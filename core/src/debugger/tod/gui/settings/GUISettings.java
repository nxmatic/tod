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
package tod.gui.settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

import tod.gui.IGUIManager;
import tod.gui.MinerUI;
import tod.gui.formatter.CustomFormatterRegistry;
import tod.gui.kit.IOptionsOwner;
import tod.gui.kit.Options;
import zz.utils.Base64;

/**
 * Manages the persistence of TOD GUI settings.
 * @author gpothier
 */
public class GUISettings
implements IOptionsOwner
{
	private static final String PROPERTY_REGISTRY = "todGUI.customFormatterRegistry";
	private static final String PROPERTY_INTIMACY = "todGUI.intimacySettings";

	private final IGUIManager itsGUIManager;
	private final Properties itsProperties = new Properties();
	private final Options itsRootOptions = new Options(this, "root", null);
	private CustomFormatterRegistry itsCustomFormatterRegistry;
	private IntimacySettings itsIntimacySettings;

	public GUISettings(IGUIManager aManager)
	{
		itsGUIManager = aManager;
		
		loadProperties();

		itsCustomFormatterRegistry = (CustomFormatterRegistry) getObjectProperty(PROPERTY_REGISTRY, null);
		if (itsCustomFormatterRegistry == null) itsCustomFormatterRegistry = new CustomFormatterRegistry();
		
//		itsIntimacySettings = (IntimacySettings) getObjectProperty(PROPERTY_INTIMACY, null);
		if (itsIntimacySettings == null) itsIntimacySettings = new IntimacySettings();
	}

	public void save()
	{
		saveFormatters();
		saveProperties();
	}
	
	/**
	 * Returns the registry of custom object formatters.
	 */
	public CustomFormatterRegistry getCustomFormatterRegistry()
	{
		return itsCustomFormatterRegistry;
	}

	private void saveFormatters()
	{
		setObjectProperty(PROPERTY_REGISTRY, itsCustomFormatterRegistry);
	}


	
	/**
	 * Stores a persistent property, which can be retrieved
	 * with {@link #getProperty(String)}.
	 */
	public void setProperty(String aKey, String aValue)
	{
		itsProperties.setProperty(aKey, aValue);
	}
	
	/**
	 * Retrieves a persistent property previously stored with 
	 * {@link #setProperty(String, String)}.
	 * @see MinerUI#getIntProperty(IGUIManager, String, int)
	 * @see MinerUI#getBooleanProperty(IGUIManager, String, boolean)
	 */
	public String getProperty(String aKey)
	{
		return itsProperties.getProperty(aKey);
	}
	
	/**
	 * Returns the global GUI options of this GUI manager.
	 */
	public Options getOptions()
	{
		return itsRootOptions;
	}
	
	
	
	/**
	 * Loads stored properties and place them in the given properties map.
	 */
	public void loadProperties()
	{
		try
		{
			File theFile = new File("tod-properties.txt");
			if (theFile.exists()) itsProperties.load(new FileInputStream(theFile));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Saves the given properties map.
	 */
	private void saveProperties()
	{
		try
		{
			File theFile = new File("tod-properties.txt");
			itsProperties.store(new FileOutputStream(theFile), "TOD configuration");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Utility method for {@link #getProperty(String)}
	 */
	public boolean getBooleanProperty (String aPropertyName, boolean aDefault)
	{
		String theString = getProperty(aPropertyName);
		return theString != null ? Boolean.parseBoolean(theString) : aDefault;
	}
	
	/**
	 * Utility method for {@link #getProperty(String)}
	 */
	public int getIntProperty (String aPropertyName, int aDefault)
	{
		String theString = getProperty(aPropertyName);
		return theString != null ? Integer.parseInt(theString) : aDefault;
	}
	
	/**
	 * Utility method for {@link #getProperty(String)}
	 */
	public String getStringProperty (String aPropertyName, String aDefault)
	{
		String theString = getProperty(aPropertyName);
		return theString != null ? theString : aDefault;
	}
	
	/**
	 * Retrieves a serialized object.
	 */
	public Object getObjectProperty(String aPropertyName, Object aDefault)
	{
		try
		{
			String theString = getProperty(aPropertyName);
			if (theString == null) return aDefault;
			
			byte[] theByteArray = Base64.decode(theString);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(theByteArray));
			return ois.readObject();
		}
		catch (Exception e)
		{
			// avoid throwing new exception in case of new object format
			//throw new RuntimeException(e);
			System.err.println("---- Problem while loading GUI properties "+aPropertyName);
			//e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Saves a serialized object into a property.
	 */
	public void setObjectProperty(String aPropertyName, Object aValue)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(aValue);
			oos.flush();
			
			byte[] theByteArray = baos.toByteArray();
			String theString = Base64.encodeBytes(theByteArray);
			
			setProperty(aPropertyName, theString);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the settings for aspect/advice intimacy level.
	 */
	public IntimacySettings getIntimacySettings()
	{
		return itsIntimacySettings;
	}

}
