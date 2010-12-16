/*
 * Created on Apr 23, 2009
 */
package tod2.agent;

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
	public static final byte ARRAY_LENGTH = 4;
	public static final byte NEW_ARRAY = 5;
	public static final byte CONSTANT = 6;
	
	/**
	 * Emitted after an unmonitored instantiation.
	 * This is needed because we cannot get the object id before the constructor is called 
	 * (not allowed by the verifier). 
	 */
	public static final byte OBJECT_INITIALIZED = 7;
	public static final byte EXCEPTION = 8;
	public static final byte HANDLER_REACHED = 9;
	
	/**
	 * Entering into an in-scope behavior
	 */
	public static final byte INSCOPE_BEHAVIOR_ENTER_FROM_SCOPE = 10;
	
	/**
	 * Entering into an in-scope <clinit> behavior.
	 * This is different from {@link #INSCOPE_BEHAVIOR_ENTER} so that the replayer code is simper.
	 */
	public static final byte INSCOPE_CLINIT_ENTER_FROM_SCOPE = 11;
	
	/**
	 * Entering into {@link ClassLoader#loadClassInternal} or similar methods.
	 * This makes the replayer code simpler.
	 */
	public static final byte CLASSLOADER_ENTER = 12;
	
	public static final byte CLASSLOADER_EXIT_NORMAL = 13;
	public static final byte CLASSLOADER_EXIT_EXCEPTION = 14;
	
	/**
	 * Same as {@link #INSCOPE_BEHAVIOR_ENTER} but behavior id is a delta relative to previous
	 */
	public static final byte INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_SCOPE = 15;
	
	/**
	 * Behavior arguments, in the case the behavior was called from non-instrumented code.
	 * Must be sent right after {@link #BEHAVIOR_ENTER}
	 */
	public static final byte BEHAVIOR_ENTER_ARGS = 16;
	
	/**
	 * Target of constructor call, in the case the behavior was called from non-instrumented code.
	 * This is sent after the constructor chaining is finished.
	 */
	public static final byte CONSTRUCTOR_TARGET = 17;
	
	public static final byte INSCOPE_BEHAVIOR_EXIT_NORMAL = 18;
	public static final byte INSCOPE_BEHAVIOR_EXIT_EXCEPTION = 19;
	
	/**
	 * Entering into an out-of-scope behavior
	 */
	public static final byte OUTOFSCOPE_BEHAVIOR_ENTER = 20;
	
	public static final byte OUTOFSCOPE_CLINIT_ENTER = 21;
	
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL = 22;
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION = 23;
	
	public static final byte INSTANCEOF_OUTCOME = 24;
	
	public static final byte REGISTER_OBJECT = 25;
	public static final byte REGISTER_OBJECT_DELTA = 26;
	public static final byte REGISTER_REFOBJECT = 27;
	public static final byte REGISTER_REFOBJECT_DELTA = 28;
	public static final byte REGISTER_CLASS = 29;
	public static final byte REGISTER_CLASSLOADER = 30;
	public static final byte REGISTER_THREAD = 31;
	
	/**
	 * Args: timestamp
	 */
	public static final byte SYNC = 32;
	
	public static final byte INSCOPE_BEHAVIOR_ENTER_FROM_OUTOFSCOPE = 33;
	public static final byte INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_OUTOFSCOPE = 34;
	public static final byte INSCOPE_CLINIT_ENTER_FROM_OUTOFSCOPE = 35;
	

	
	public static final int MSG_COUNT = 35;
	
	public static final String[] _NAMES = 
	{
		"0",
		"FIELD_READ",
		"FIELD_READ_SAME",
		"ARRAY_READ",
		"ARRAY_LENGTH",
		"NEW_ARRAY",
		"CONSTANT",
		"OBJECT_INITIALIZED",
		"EXCEPTION",
		"HANDLER_REACHED",
		"INSCOPE_BEHAVIOR_ENTER_FROM_SCOPE",
		"INSCOPE_CLINIT_ENTER_FROM_SCOPE",
		"CLASSLOADER_ENTER",
		"CLASSLOADER_EXIT_NORMAL",
		"CLASSLOADER_EXIT_EXCEPTION",
		"INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_SCOPE",
		"BEHAVIOR_ENTER_ARGS",
		"CONSTRUCTOR_TARGET",
		"INSCOPE_BEHAVIOR_EXIT_NORMAL",
		"INSCOPE_BEHAVIOR_EXIT_EXCEPTION",
		"OUTOFSCOPE_BEHAVIOR_ENTER",
		"OUTOFSCOPE_CLINIT_ENTER",
		"OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL",
		"OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION",
		"INSTANCEOF_OUTCOME",
		"REGISTER_OBJECT",
		"REGISTER_OBJECT_DELTA",
		"REGISTER_REFOBJECT",
		"REGISTER_REFOBJECT_DELTA",
		"REGISTER_CLASS",
		"REGISTER_CLASSLOADER",
		"REGISTER_THREAD",
		"SYNC",
		"INSCOPE_BEHAVIOR_ENTER_FROM_OUTOFSCOPE",
		"INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_OUTOFSCOPE",
		"INSCOPE_CLINIT_ENTER_FROM_OUTOFSCOPE"
	};
}
