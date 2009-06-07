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
	public static final byte PACKET_TYPE_THREAD = 1;
	public static final byte PACKET_TYPE_STRING = 2;
	
	public static final byte FIELD_READ = 1;
	public static final byte FIELD_READ_SAME = 2;
	public static final byte ARRAY_READ = 3;
	public static final byte NEW_ARRAY = 4;
	public static final byte CONSTANT = 5;
	public static final byte OBJECT_INITIALIZED = 6;
	public static final byte EXCEPTION = 7;
	public static final byte HANDLER_REACHED = 8;
	
	/**
	 * Entering into an in-scope behavior
	 */
	public static final byte INSCOPE_BEHAVIOR_ENTER = 9;
	
	/**
	 * Same as {@link #INSCOPE_BEHAVIOR_ENTER} but behavior id is a delta relative to previous
	 */
	public static final byte INSCOPE_BEHAVIOR_ENTER_DELTA = 10;
	
	/**
	 * Behavior arguments, in the case the behavior was called from non-instrumented code.
	 * Must be sent right after {@link #BEHAVIOR_ENTER}
	 */
	public static final byte BEHAVIOR_ENTER_ARGS = 11;
	
	/**
	 * Target of constructor call, in the case the behavior was called from non-instrumented code.
	 * This is sent after the constructor chaining is finished.
	 */
	public static final byte CONSTRUCTOR_TARGET = 12;
	
	public static final byte INSCOPE_BEHAVIOR_EXIT_NORMAL = 13;
	public static final byte INSCOPE_BEHAVIOR_EXIT_EXCEPTION = 14;
	
	/**
	 * Entering into an out-of-scope behavior
	 */
	public static final byte OUTOFSCOPE_BEHAVIOR_ENTER = 15;
	
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL = 16;
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION = 17;
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_RESULT = 18;
	
	public static final byte UNMONITORED_BEHAVIOR_CALL = 19;
	
	/**
	 * An unmonitored method call returned normally 
	 */
	public static final byte UNMONITORED_BEHAVIOR_CALL_RESULT = 20;
	
	public static final byte UNMONITORED_BEHAVIOR_CALL_EXCEPTION = 21;
	
	public static final byte REGISTER_OBJECT = 22;
	public static final byte REGISTER_OBJECT_DELTA = 23;
	public static final byte REGISTER_REFOBJECT = 24;
	public static final byte REGISTER_REFOBJECT_DELTA = 25;
	public static final byte REGISTER_CLASS = 26;
	public static final byte REGISTER_CLASSLOADER = 27;
	public static final byte REGISTER_THREAD = 28;
	
	/**
	 * Args: timestamp
	 */
	public static final byte SYNC = 29;
	
	public static final int MSG_COUNT = 29;
	
	public static final String[] _NAMES = 
	{
		"0",
		"FIELD_READ",
		"FIELD_READ_SAME",
		"ARRAY_READ",
		"NEW_ARRAY",
		"CONSTANT",
		"OBJECT_INITIALIZED",
		"EXCEPTION",
		"HANDLER_REACHED",
		"INSCOPE_BEHAVIOR_ENTER",
		"INSCOPE_BEHAVIOR_ENTER_DELTA",
		"BEHAVIOR_ENTER_ARGS",
		"CONSTRUCTOR_TARGET",
		"INSCOPE_BEHAVIOR_EXIT_NORMAL",
		"INSCOPE_BEHAVIOR_EXIT_EXCEPTION",
		"OUTOFSCOPE_BEHAVIOR_ENTER",
		"OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL",
		"OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION",
		"OUTOFSCOPE_BEHAVIOR_EXIT_RESULT",
		"UNMONITORED_BEHAVIOR_CALL",
		"UNMONITORED_BEHAVIOR_CALL_RESULT",
		"UNMONITORED_BEHAVIOR_CALL_EXCEPTION",
		"REGISTER_OBJECT",
		"REGISTER_OBJECT_DELTA",
		"REGISTER_REFOBJECT",
		"REGISTER_REFOBJECT_DELTA",
		"REGISTER_CLASS",
		"REGISTER_CLASSLOADER",
		"REGISTER_THREAD"
	};
}
