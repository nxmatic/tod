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
package tod.core.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import tod.impl.server.JavaTODServerFactory2;
import tod.utils.ConfigUtils;
import tod2.agent.AgentConfig;
import zz.utils.PublicCloneable;

/**
 * Instances of this class contain configuration options for a TOD session.
 * 
 * @author gpothier
 */
public class TODConfig extends PublicCloneable implements Serializable
{
	public static int TOD_VERBOSE = ConfigUtils.readInt("tod-verbose", 0);
	{
		System.out.println("Verbosity = " + TOD_VERBOSE);
	}

	public static boolean DB_SCOPE_CHECK = ConfigUtils.readBoolean("db.scope.check", true);
	{
		System.out.println("scope-checking is " + DB_SCOPE_CHECK);
	}

	private static final long serialVersionUID = 4959079097346687404L;

	private static final String HOME = System.getProperty("user.home");

	/**
	 * Defines levels of "detail" for configuration options.
	 * 
	 * @author gpothier
	 */
	public static enum ConfigLevel
	{
		NORMAL(1), ADVANCED(2), DEBUG(3), NEVER(Integer.MAX_VALUE);

		private int itsValue;

		private ConfigLevel(int aValue)
		{
			itsValue = aValue;
		}

		/**
		 * Whether this level also includes the specified level. eg.
		 * {@link #DEBUG} includes {@link #NORMAL} but the opposite is false.
		 */
		public boolean accept(ConfigLevel aLevel)
		{
			return aLevel.itsValue <= itsValue;
		}
	}

	public static final IntegerItem AGENT_VERBOSE =
			new IntegerItem(
					ConfigLevel.ADVANCED,
					"agent-verbose",
					"Agent - verbose",
					"Defines the verbosity level of the native agent. "
							+ "0 means minimal verbosity, greater values increase verbosity.",
					0);

	public static final StringItem CLASS_CACHE_PATH =
			new StringItem(
					ConfigLevel.NORMAL,
					"class-cache-path",
					"Instrumenter - class cache path",
					"Defines the path where the instrumenter stores instrumented classes.",
					HOME + File.separatorChar + "tmp" + File.separatorChar + "tod" + File.separatorChar + "classCache");

	public static final StringItem BAD_CLASSES_PATH =
		new StringItem(
				ConfigLevel.NORMAL,
				"bas-classes-path",
				"Instrumenter - bad classes path",
				"Defines the path where the instrumenter stores classes that could not be instrumented.",
				HOME + File.separatorChar + "tmp" + File.separatorChar + "tod" + File.separatorChar + "badClasses");
	
	public static final BooleanItem AGENT_CAPTURE_EXCEPTIONS =
			new BooleanItem(
					ConfigLevel.DEBUG,
					"agent-captureExceptions",
					"Agent - capture exceptions",
					"If true, the native agent sets up a callback that captures " + "exceptions.",
					true);

	public static final BooleanItem AGENT_CAPTURE_AT_START =
			new BooleanItem(
					ConfigLevel.ADVANCED,
					AgentConfig.PARAM_CAPTURE_AT_START,
					"Agent - capture trace at start",
					"Whether trace capture should be enabled when the agent starts.",
					true);

	public static final StringItem STRUCTURE_DATABASE_LOCATION =
			new StringItem(
					ConfigLevel.NORMAL,
					"structure-db-loc",
					"Structure database - location",
					"Directory where the structure database is stored.",
					HOME + "/tmp/tod/locations");

	public static final StringItem SCOPE_GLOBAL_FILTER =
			new StringItem(
					ConfigLevel.DEBUG,
					"scope-globalFilter",
					"Scope - global filter",
					"Global class filter for instrumentation. "
							+ "Used mainly to shield TOD agent classes from instrumentation. "
							+ "Classes that do no pass this filter are not touched by any kind "
							+ "of instrumentation and are not registered in the trace database. "
							+ "There should not be any reason to modify it.",
					ConfigUtils.readString("scope-globalFilter", "[-tod.agent.**]"));

	public static final StringItem SCOPE_TRACE_FILTER =
			new StringItem(
					ConfigLevel.NORMAL,
					"trace-filter",
					"Scope - trace filter",
					"Tracing class filter for instrumentation. "
							+ "Classes that do no pass this filter are not instrumented "
							+ "but are registered in the structure database.",
					ConfigUtils.readString("trace-filter", "[-java.** -javax.** -sun.** -com.sun.** -org.ietf.jgss.** -org.omg.** -org.w3c.** -org.xml.**]"));

	public static final StringItem SCOPE_ID_FILTER =
		new StringItem(
				ConfigLevel.ADVANCED,
				"id-filter",
				"Scope - object id filter",
				"Filter for classes to which TOD can add an Object Id field. "
				+ "Classes that do no pass this filter are not structurally modified.",
				ConfigUtils.readString("id-filter", "[-java.lang.Object -java.lang.String -java.lang.Number -java.lang.Boolean]"));
	
	public static final StringItem CLIENT_NAME =
			new StringItem(
					ConfigLevel.NORMAL,
					AgentConfig.PARAM_CLIENT_NAME,
					"Client - name",
					"Name given to the debugged program's JVM.",
					"tod-1");

	public static final StringItem COLLECTOR_HOST =
			new StringItem(
					ConfigLevel.DEBUG,
					"collector-host",
					"Collector - host",
					"Host to which the debugged program should send events.",
					"localhost");

	public static final IntegerItem COLLECTOR_PORT =
			new IntegerItem(
					ConfigLevel.DEBUG,
					"collector-port",
					"Collector - listening port",
					"Port to which the TOD agent should connect.",
					8058);

	public static final String SESSION_MEMORY = "memory";
	public static final String SESSION_LOCAL = "local";
	public static final String SESSION_REMOTE = "remote";
	public static final String SESSION_COUNT = "count";

	public static final StringItem SESSION_TYPE =
			new StringItem(
					ConfigLevel.NORMAL,
					"session-type",
					"Session type",
					"Specifies the type of database to use for the debugging "
							+ "session. One of:\n" + " - " + SESSION_MEMORY
							+ ": Events are stored in memory, " + "in the Eclipse process. "
							+ "This is the less scalable option. The maximum number "
							+ "of events depends on the amount of heap memory allocated "
							+ "the the JVM that runs Eclipse.\n" + " - " + SESSION_LOCAL
							+ ": Events are stored on the hard disk. "
							+ "This option provides good scalability but "
							+ "performance may be a problem for large traces.\n" + " - "
							+ SESSION_REMOTE + ": Events are stored in a dedicated "
							+ "distributed database. "
							+ "This option provides good scalability and performance "
							+ "(depending on the size of the database cluster). "
							+ "The database cluster must be set up and the "
							+ "'Collector host' option must indicate the name of "
							+ "the grid master.\n",
					ConfigUtils.readString("session-type", SESSION_LOCAL));

	public static final SizeItem LOCAL_SESSION_HEAP =
			new SizeItem(
					ConfigLevel.NORMAL,
					"localSessionHeap",
					"Local session heap size",
					"Specifies the amount of heap memory to allocate for local debugging session.",
					"256m");

	public static final BooleanItem INDEX_STRINGS =
			new BooleanItem(
					ConfigLevel.NORMAL,
					"index-strings",
					"Index strings",
					"Whether strings should be indexed by the database. "
							+ "This has an impact on overall recording performance.",
					false);

	public static final IntegerItem MASTER_TIMEOUT =
			new IntegerItem(
					ConfigLevel.ADVANCED,
					"master-timeout",
					"Master timeout",
					"The time (in seconds) the database should wait for clients to connect"
							+ "before exiting. A value of 0 means no timeout.",
					0);

	public static final IntegerItem DB_PROCESS_TIMEOUT =
			new IntegerItem(
					ConfigLevel.ADVANCED,
					"db-process-timeout",
					"Database process timeout",
					"The time (in seconds) the debugger should wait for the database process to start.",
					30);

	public static final IntegerItem DB_AUTOFLUSH_DELAY =
			new IntegerItem(
					ConfigLevel.ADVANCED,
					"db-autoflush-delay",
					"Database autoflush delay (seconds)",
					"The delay, in seconds, between automatic database flushes. Setting it to 0 disables "
							+ "automatic flushes, which is sometimes needed to avoid 'Out of order events' errors.",
					2);

	public static final BooleanItem WITH_ASPECTS =
			new BooleanItem(
					ConfigLevel.ADVANCED,
					"with-aspects",
					"Include aspect info",
					"Whether information used by the TOD extension for AspectJ, such as "
							+ "TagMap, should be included in behavior infos.",
					false);

	public static final BooleanItem WITH_BYTECODE =
			new BooleanItem(
					ConfigLevel.ADVANCED,
					"with-bytecode",
					"Store behavior bytecode",
					"Store behavior bytecode so as to provide disassembled views.",
					true);

	public static final StringItem SERVER_TYPE =
			new StringItem(
					ConfigLevel.ADVANCED,
					"server-type",
					"Type of server interface",
					"Class name of the TOD server factory.",
					JavaTODServerFactory2.class.getName());

	public static final BooleanItem BCI_PRELOAD_CLASSES =
			new BooleanItem(
					ConfigLevel.ADVANCED,
					"bci-preload-classes",
					"Class preloading in instrumented code",
					"Whether instrumented code should preload classes on which methods are about to be called. "
							+ "This permits to optimize the information sent to the database in some cases.",
					true);

	public static final BooleanItem DB_TWOPHASES =
			new BooleanItem(
					ConfigLevel.ADVANCED,
					"db-twophases",
					"Split trace recording into two phases",
					"If set to true, raw events emitted by the program are first written out to disk "
							+ "without any processing; when the program terminates, they are indexed. This permits "
							+ "to reduce the runtime overhead to the minimum.",
					false);

	public static final StringItem DB_RAW_EVENTS_DIR =
			new StringItem(
					ConfigLevel.ADVANCED,
					"db-raw-events-dir",
					"Directory to store raw events file",
					"Defines the path where where the raw events file is stored (only valid "
							+ "if two-phases recording is enabled). If possible, do not use the same disk as the"
							+ "database files.",
					HOME + File.separatorChar + "tmp" + File.separatorChar + "tod");

	public static final BooleanItem ALLOW_HOMONYM_CLASSES =
		new BooleanItem(
				ConfigLevel.ADVANCED,
				"allow-homonym-classes",
				"Allow homonym classes",
				"Allow various classes to have the same (fully-qualified) name. This uses "
						+" more memory.",
				false);
	
	/**
	 * Contains all available configuration items.
	 */
	public static final Item[] ITEMS = getItems();

	private static Item[] getItems()
	{
		try
		{
			List<Item> theItems = new ArrayList<Item>();
			Field[] theFields = TODConfig.class.getDeclaredFields();

			for (Field theField : theFields)
			{
				if (Item.class.isAssignableFrom(theField.getType()))
				{
					Item theItem = (Item) theField.get(null);
					theItems.add(theItem);
				}
			}

			return theItems.toArray(new Item[theItems.size()]);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private HashMap<String, String> itsMap = new HashMap<String, String>();

	/**
	 * Sets the value for an option item
	 */
	public <T> void set(Item<T> aItem, T aValue)
	{
		itsMap.put(aItem.getKey(), aItem.getOptionString(aValue));
	}

	/**
	 * Retrieves the value for an option item.
	 */
	public <T> T get(Item<T> aItem)
	{
		String theString = itsMap.get(aItem.getKey());
		return theString != null ? aItem.getOptionValue(theString) : aItem.getDefault();
	}

	@Override
	public TODConfig clone()
	{
		TODConfig theClone = (TODConfig) super.clone();
		theClone.itsMap = (HashMap<String, String>) itsMap.clone();
		return theClone;
	}

	public static abstract class ItemType<T>
	{
		public static final ItemType<String> ITEM_TYPE_STRING = new ItemType<String>()
		{
			@Override
			public String getName()
			{
				return "string";
			}

			@Override
			public String getString(String aValue)
			{
				return aValue;
			}

			@Override
			public String getValue(String aString)
			{
				return aString;
			}
		};

		public static final ItemType<Integer> ITEM_TYPE_INTEGER = new ItemType<Integer>()
		{
			@Override
			public String getName()
			{
				return "integer";
			}

			@Override
			public String getString(Integer aValue)
			{
				return Integer.toString(aValue);
			}

			@Override
			public Integer getValue(String aString)
			{
				return Integer.parseInt(aString);
			}
		};

		public static final ItemType<Boolean> ITEM_TYPE_BOOLEAN = new ItemType<Boolean>()
		{
			@Override
			public String getName()
			{
				return "boolean";
			}

			@Override
			public String getString(Boolean aValue)
			{
				return Boolean.toString(aValue);
			}

			@Override
			public Boolean getValue(String aString)
			{
				return Boolean.parseBoolean(aString);
			}
		};

		public static final ItemType<Long> ITEM_TYPE_SIZE = new ItemType<Long>()
		{
			@Override
			public String getName()
			{
				return "boolean";
			}

			@Override
			public String getString(Long aValue)
			{
				return Long.toString(aValue);
			}

			@Override
			public Long getValue(String aString)
			{
				return ConfigUtils.readSize(aString);
			}
		};

		public abstract String getName();

		/**
		 * Transforms an option value to a string.
		 */
		public abstract String getString(T aValue);

		/**
		 * Transforms a string to an option value
		 */
		public abstract T getValue(String aString);

	}

	public static class Item<T>
	{
		private final ConfigLevel itsLevel;
		private final ItemType<T> itsType;
		private final String itsName;
		private final String itsDescription;
		private final T itsDefault;
		private final String itsKey;

		public Item(
				ConfigLevel aLevel,
				ItemType<T> aType,
				String aKey,
				String aName,
				String aDescription,
				T aDefault)
		{
			itsLevel = aLevel;
			itsType = aType;
			itsKey = aKey;
			itsName = aName;
			itsDescription = aDescription;
			itsDefault = aDefault;
		}

		public ItemType<T> getType()
		{
			return itsType;
		}

		public String getKey()
		{
			return itsKey;
		}

		public String getName()
		{
			return itsName;
		}

		public String getDescription()
		{
			return itsDescription;
		}

		public T getDefault()
		{
			return itsDefault;
		}

		/**
		 * Transforms an option value to a string.
		 */
		public String getOptionString(T aValue)
		{
			return itsType.getString(aValue);
		}

		/**
		 * Transforms a string to an option value
		 */
		public T getOptionValue(String aString)
		{
			return itsType.getValue(aString);
		}

		/**
		 * Returns a java option string representing the specified value. Eg.:
		 * -Dxxx=yyy
		 */
		public String javaOpt(T aValue)
		{
			return "-D" + getKey() + "=" + getOptionString(aValue);
		}

		/**
		 * Creates a java option representing the value of this item in the
		 * specified config. Eg.: -Dxxx=yyy
		 */
		public String javaOpt(TODConfig aConfig)
		{
			return javaOpt(aConfig.get(this));
		}

	}

	public static class BooleanItem extends Item<Boolean>
	{
		public BooleanItem(
				ConfigLevel aLevel,
				String aKey,
				String aName,
				String aDescription,
				Boolean aDefault)
		{
			super(aLevel, ItemType.ITEM_TYPE_BOOLEAN, aKey, aName, aDescription, ConfigUtils
					.readBoolean(aKey, aDefault));
		}
	}

	public static class StringItem extends Item<String>
	{
		public StringItem(
				ConfigLevel aLevel,
				String aKey,
				String aName,
				String aDescription,
				String aDefault)
		{
			super(aLevel, ItemType.ITEM_TYPE_STRING, aKey, aName, aDescription, ConfigUtils
					.readString(aKey, aDefault));
		}
	}

	public static class IntegerItem extends Item<Integer>
	{
		public IntegerItem(
				ConfigLevel aLevel,
				String aKey,
				String aName,
				String aDescription,
				Integer aDefault)
		{
			super(aLevel, ItemType.ITEM_TYPE_INTEGER, aKey, aName, aDescription, ConfigUtils
					.readInt(aKey, aDefault));
		}
	}

	public static class SizeItem extends Item<Long>
	{
		public SizeItem(
				ConfigLevel aLevel,
				String aKey,
				String aName,
				String aDescription,
				String aDefault)
		{
			super(aLevel, ItemType.ITEM_TYPE_SIZE, aKey, aName, aDescription, ConfigUtils.readSize(
					aKey,
					aDefault));
		}
	}

	/**
	 * Reads a {@link TODConfig} from a properties file.
	 */
	public static TODConfig fromProperties(File aFile)
	{
		try
		{
			Properties theProperties = new Properties();
			theProperties.load(new FileInputStream(aFile));
			return fromProperties(theProperties);
		}
		catch (FileNotFoundException e)
		{
			System.err.println("File not found: " + aFile + ", using default configuration.");
			return new TODConfig();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads a {@link TODConfig} from a properties file.
	 */
	public static TODConfig fromProperties(Properties aProperties)
	{
		TODConfig theConfig = new TODConfig();
		for (Item theItem : ITEMS)
		{
			String theValue = aProperties.getProperty(theItem.getKey());
			if (theValue != null) theConfig.set(theItem, theItem.getOptionValue(theValue));
		}
		return theConfig;
	}

	/**
	 * This is a hack to allow debugging the database with TOD.
	 * Without this, the collector port passed to the native agent
	 * is always the same as the one the server tries to listen to.
	 */
	public int getPort()
	{
		return ConfigUtils.readInt("debug-server-port", get(TODConfig.COLLECTOR_PORT));
	}
	

}
