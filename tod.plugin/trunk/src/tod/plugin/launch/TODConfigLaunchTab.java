/*
TOD plugin - Eclipse pluging for TOD
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.plugin.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import tod.core.config.TODConfig;
import tod.core.config.TODConfig.Item;
import tod.core.config.TODConfig.ItemType;
import zz.eclipse.utils.launcher.options.AbstractItemControl;
import zz.eclipse.utils.launcher.options.BooleanControl;
import zz.eclipse.utils.launcher.options.IntegerControl;
import zz.eclipse.utils.launcher.options.OptionsControl;
import zz.eclipse.utils.launcher.options.OptionsTab;
import zz.eclipse.utils.launcher.options.TextControl;

public class TODConfigLaunchTab extends OptionsTab<TODConfig.Item>
{
	private static final String MAP_NAME = "tod.plugin.launch.OPTIONS_MAP"; //$NON-NLS-1$
	
	private static final Map<ItemType, ItemTypeHandler> HANDLERS = initHandlers();
	
	private TODConfig.Item[] itsExcludedItems;
	
	public TODConfigLaunchTab(TODConfig.Item... aExcludedItems) 
	{
		itsExcludedItems = aExcludedItems;
	}
	
	public String getName()
	{
		return "TOD options";
	}
	
	@Override
	protected OptionsControl<Item> createOptionsControl(Composite aParent)
	{
		MyOptionsControl theOptionsControl = new MyOptionsControl(aParent);
		return theOptionsControl;
	}
	
	@Override
	protected String getMapName()
	{
		return MAP_NAME;
	}


	private static Map<ItemType, ItemTypeHandler> initHandlers()
	{
		Map<ItemType, ItemTypeHandler> theMap = new HashMap<ItemType, ItemTypeHandler>();
		
		theMap.put(TODConfig.ItemType.ITEM_TYPE_STRING, new ItemTypeHandler()
		{
			@Override
			public AbstractItemControl<Item> createControl(OptionsControl<Item> aOptionsTab, Composite aParent, Item aItem)
			{
				return new TextControl<Item>(aParent, aOptionsTab, aItem);
			}
		});
		
		theMap.put(TODConfig.ItemType.ITEM_TYPE_SIZE, new ItemTypeHandler()
		{
			@Override
			public AbstractItemControl<Item> createControl(OptionsControl<Item> aOptionsTab, Composite aParent, Item aItem)
			{
				return new TextControl<Item>(aParent, aOptionsTab, aItem);
			}
		});
		
		theMap.put(TODConfig.ItemType.ITEM_TYPE_INTEGER, new ItemTypeHandler()
		{
			@Override
			public AbstractItemControl<Item> createControl(OptionsControl<Item> aOptionsTab, Composite aParent, Item aItem)
			{
				return new IntegerControl<Item>(aParent, aOptionsTab, aItem);
			}
		});
		
		theMap.put(TODConfig.ItemType.ITEM_TYPE_BOOLEAN, new ItemTypeHandler()
		{
			@Override
			public AbstractItemControl<Item> createControl(OptionsControl<Item> aOptionsTab, Composite aParent, Item aItem)
			{
				return new BooleanControl<Item>(aParent, aOptionsTab, aItem);
			}
		});
		
		return theMap;
	}
	
	private static ItemTypeHandler getHandler(Item aItem)
	{
		ItemTypeHandler theHandler = HANDLERS.get(aItem.getType());
		if (theHandler == null) throw new RuntimeException("Handler not found for "+aItem+" ("+aItem.getType()+")");
		return theHandler;
	}
	
	private TODConfig.Item[] getItems()
	{
		List<TODConfig.Item> theItems = new ArrayList<Item>();
		for (Item theItem : TODConfig.ITEMS)
		{
			if (! isExcluded(theItem)) theItems.add(theItem);
		}
		return theItems.toArray(new TODConfig.Item[theItems.size()]);
	}
	
	private boolean isExcluded(TODConfig.Item aItem)
	{
		for (TODConfig.Item theItem : itsExcludedItems)
		{
			if (aItem == theItem) return true;
		}
		return false;
	}
	
	private class MyOptionsControl extends OptionsControl<TODConfig.Item>
	{
		public MyOptionsControl(Composite aParent)
		{
			super(aParent, 0, getItems());
		}


		@Override
		public String getCaption(Item aItem)
		{
			return aItem.getName();
		}

		@Override
		protected String getDefault(Item aItem)
		{
			return aItem.getOptionString(aItem.getDefault());
		}

		@Override
		public String getDescription(Item aItem)
		{
			return aItem.getDescription();
		}

		@Override
		protected String getKey(Item aItem)
		{
			return aItem.getKey();
		}

		@Override
		protected AbstractItemControl<Item> createControl(
				Composite aParent, 
				Item aItem)
		{
			return getHandler(aItem).createControl(this, aParent, aItem);
		}
		
		@Override
		protected void contentChanged()
		{
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}
	
	/**
	 * Bridge between {@link ItemType} and SWT controls.
	 * @author gpothier
	 */
	private static abstract class ItemTypeHandler
	{
		public abstract AbstractItemControl<Item> createControl(
				OptionsControl<Item> aOptionsTab,
				Composite aParent, 
				Item aItem);
	}
	
	/**
	 * Reads the TOD config options from the given launch configuration. 
	 */
	public static TODConfig readConfig(ILaunchConfiguration aLaunchConfiguration) 
	throws CoreException
	{
		try
		{
			return readConfig0(aLaunchConfiguration);
		}
		catch (InvalidLaunchConfiguration e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static TODConfig readConfig0(ILaunchConfiguration aLaunchConfiguration) 
	throws CoreException, InvalidLaunchConfiguration
	{
		Map<String, String> theOptionsMap = OptionsTab.loadOptionsMap(aLaunchConfiguration, MAP_NAME);
		
		TODConfig theConfig = new TODConfig();
		for (Item theItem : TODConfig.ITEMS)
		{
			String theString = theOptionsMap.get(theItem.getKey());
			try
			{
				Object theValue = theString != null ?
						theItem.getOptionValue(theString)
						: theItem.getDefault();
						
				theConfig.set(theItem, theValue);
			}
			catch (Exception e)
			{
				throw new InvalidLaunchConfiguration(
						"Invalid value for '"
						+theItem.getName()+"'");
			}
		}
		
		return theConfig;
	}
	
	
	
	
	@Override
	protected void checkValid(ILaunchConfiguration aLaunchConfiguration) 
	throws InvalidLaunchConfiguration
	{
		super.checkValid(aLaunchConfiguration);
		try
		{
			readConfig0(aLaunchConfiguration);
		}
		catch (CoreException e)
		{
			throw new InvalidLaunchConfiguration(e);
		}
	}
}
