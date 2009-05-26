/*
 * Created on Apr 23, 2009
 */
package tod.agent;

/**
 * Contains constants used to build messages.
 * @author gpothier
 */
public class Message
{
	public static final byte FIELD_READ = 1;
	public static final byte FIELD_READ_SAME = 2;
	public static final byte ARRAY_READ = 3;
	public static final byte NEW = 4;
	public static final byte OBJECT_INITIALIZED = 5;
	public static final byte EXCEPTION = 6;
	public static final byte HANDLER_REACHED = 7;
	
	/**
	 * Entering into an in-scope behavior
	 */
	public static final byte INSCOPE_BEHAVIOR_ENTER = 8;
	
	/**
	 * Behavior arguments, in the case the behavior was called from non-instrumented code.
	 * Must be sent right after {@link #BEHAVIOR_ENTER}
	 */
	public static final byte BEHAVIOR_ENTER_ARGS = 9;
	
	/**
	 * Target of constructor call, in the case the behavior was called from non-instrumented code.
	 * This is sent after the constructor chaining is finished.
	 */
	public static final byte CONSTRUCTOR_TARGET = 10;
	
	public static final byte INSCOPE_BEHAVIOR_EXIT_NORMAL = 11;
	public static final byte INSCOPE_BEHAVIOR_EXIT_EXCEPTION = 12;
	
	/**
	 * Entering into an out-of-scope behavior
	 */
	public static final byte OUTOFSCOPE_BEHAVIOR_ENTER = 13;
	
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL = 14;
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION = 15;
	
	/**
	 * An unmonitored method call returned normally 
	 */
	public static final byte UNMONITORED_BEHAVIOR_CALL_RESULT = 16;
	
	public static final byte REGISTER_OBJECT = 20;
	public static final byte REGISTER_REFOBJECT = 21;
	public static final byte REGISTER_CLASS = 22;
	public static final byte REGISTER_CLASSLOADER = 23;
	public static final byte REGISTER_THREAD = 24;
	
	/**
	 * Args: timestamp
	 */
	public static final byte SYNC = 100;
	
	public static final String[] _NAMES = 
	{
		"0",
		"FIELD_READ",
		"FIELD_READ_SAME",
		"ARRAY_READ",
		"NEW",
		"OBJECT_INITIALIZED",
		"EXCEPTION",
		"HANDLER_REACHED",
		"INSCOPE_BEHAVIOR_ENTER",
		"BEHAVIOR_ENTER_ARGS",
		"CONSTRUCTOR_TARGET",
		"INSCOPE_BEHAVIOR_EXIT_NORMAL",
		"INSCOPE_BEHAVIOR_EXIT_EXCEPTION",
		"OUTOFSCOPE_BEHAVIOR_ENTER",
		"OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL",
		"OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION",
		"UNMONITORED_BEHAVIOR_CALL_RESULT",
		"17", "18", "19",
		"REGISTER_OBJECT",
		"REGISTER_REFOBJECT",
		"REGISTER_CLASS",
		"REGISTER_CLASSLOADER",
		"REGISTER_THREAD"
	};
}
