/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.gui.kit;

import java.awt.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import tod.gui.settings.GUISettings;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * Contains a set of options.
 * @author gpothier
 */
public class Options
{
	private final GUISettings itsGUISettings;
	
	/**
	 * The name of this options set, for persistence.
	 */
	private final String itsName;
	private final Options itsParent;
	
	/**
	 * The set of all available options.
	 */
	private Set<OptionDef> itsOptions = new HashSet<OptionDef>();
	
	/**
	 * The user cannot change the options from this set using the standard
	 * options management dialogs.
	 */
	private Set<OptionDef> itsHiddenOptions = new HashSet<OptionDef>();
	
	private Map<OptionDef, IRWProperty> itsValues = 
		new HashMap<OptionDef, IRWProperty>();
	
	public Options(GUISettings aGUISettings, String aName, Options aParent)
	{
		itsGUISettings = aGUISettings;
		itsName = aName;
		itsParent = aParent;
	}

	protected Options getParent()
	{
		return itsParent;
	}
	
	/**
	 * Creates a component that permits to inspect and modify current options.
	 */
	public JComponent createComponent()
	{
		return null;
	}
	
	/**
	 * Returns the option key (used for persistence) for the given option.
	 */
	private <T> String getKey(OptionDef<T> aDef)
	{
		return "[Options]/"+itsName+"/"+aDef.getName();
	}
	
	private <T> void save(OptionDef<T> aDef, T aValue)
	{
		itsGUISettings.setProperty(getKey(aDef), aDef.marshall(aValue));
	}
	
	private <T> T load(OptionDef<T> aDef)
	{
		String theProperty = itsGUISettings.getProperty(getKey(aDef));
		return theProperty != null ? aDef.unmarshall(theProperty) : null;
	}
	
	private <T> boolean addOption0(OptionDef<T> aDef, T aDefault)
	{
		if (hasOption(aDef))
		{
			IRWProperty<T> theProperty = itsValues.get(aDef);
			if (theProperty == null) 
			{
				T theValue = load(aDef);
				if (theValue == null) 
				{
					theValue = aDefault;
					save(aDef, theValue);
				}
				
				theProperty = new SimpleRWProperty<T>(theValue);
				itsValues.put(aDef, theProperty);
			}
			
			return true;
		}
		else return false;
	}
	
	/**
	 * Adds an option to this option manager, if not already present in parent.
	 */
	public <T> void addOption(OptionDef<T> aDef, T aDefault)
	{
		Options theParent = getParent();
		if (theParent != null)
		{
			if (theParent.addOption0(aDef, aDefault)) return;
		}
		
		itsOptions.add(aDef);
		addOption0(aDef, aDefault);
	}
	
	/**
	 * Retrieves the value of the given option for the given target.
	 */
	public <T> T get(OptionDef<T> aDef)
	{
		IRWProperty<T> theProperty = getProperty(aDef);
		return theProperty != null ? theProperty.get() : null;
	}
	
	private <T> boolean hasOption(OptionDef<T> aDef)
	{
		return itsOptions.contains(aDef);
	}
	
	/**
	 * Returns the {@link Options} object responsible of the given option def.
	 */
	private <T> Options getHandler(OptionDef<T> aDef)
	{
		Options theHandler = this;
		do
		{
			if (theHandler.hasOption(aDef)) return theHandler;
			theHandler = theHandler.getParent();
		} while (theHandler != null);
		
		return null;
	}
	
	private <T> IRWProperty<T> getProperty0(OptionDef<T> aDef)
	{
		return itsValues.get(aDef);
	}
	
	/**
	 * Retrives the property object that holds the value of the given
	 * option for the given target.
	 */
	public <T> IRWProperty<T> getProperty(OptionDef<T> aDef)
	{
		Options theHandler = getHandler(aDef);
		if (theHandler == null) throw new RuntimeException("Option unknown: "+aDef);
		
		return theHandler.getProperty0(aDef);
	}
	
	/**
	 * Returns the option manager for the specified component.
	 */
	public static Options get(Component aComponent)
	{
		while(true)
		{
			if (aComponent == null) return null;
			else if (aComponent instanceof IOptionsOwner)
			{
				IOptionsOwner theOwner = (IOptionsOwner) aComponent;
				return theOwner.getOptions();
			}
			else aComponent = aComponent.getParent();
		} 
	}
	
	public static abstract class OptionDef<T>
	{
		private String itsName;
		
		public OptionDef(String aName)
		{
			itsName = aName;
		}
		
		public String getName()
		{
			return itsName;
		}

		public abstract String marshall(T aValue);
		public abstract T unmarshall(String aString);
	}
	
	public static class BooleanOptionDef extends OptionDef<Boolean>
	{
		public BooleanOptionDef(String aName)
		{
			super(aName);
		}

		@Override
		public String marshall(Boolean aValue)
		{
			return ""+aValue;
		}

		@Override
		public Boolean unmarshall(String aString)
		{
			return Boolean.parseBoolean(aString);
		}
	}
	
	public static class IntOptionDef extends OptionDef<Integer>
	{
		public IntOptionDef(String aName)
		{
			super(aName);
		}

		@Override
		public String marshall(Integer aValue)
		{
			return ""+aValue;
		}

		@Override
		public Integer unmarshall(String aString)
		{
			return Integer.parseInt(aString);
		}
	}
}
