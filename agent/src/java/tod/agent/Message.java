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
	public static final byte ARRAY_READ = 2;
	public static final byte NEW = 3;
	public static final byte OBJECT_INITIALIZED = 4;
	public static final byte EXCEPTION = 5;
	public static final byte HANDLER_REACHED = 6;
	
	/**
	 * Entering into an in-scope behavior
	 */
	public static final byte INSCOPE_BEHAVIOR_ENTER = 7;
	
	/**
	 * Behavior arguments, in the case the behavior was called from non-instrumented code.
	 * Must be sent right after {@link #BEHAVIOR_ENTER}
	 */
	public static final byte BEHAVIOR_ENTER_ARGS = 8;
	
	/**
	 * Target of constructor call, in the case the behavior was called from non-instrumented code.
	 * This is sent after the constructor chaining is finished.
	 */
	public static final byte CONSTRUCTOR_TARGET = 9;
	
	public static final byte INSCOPE_BEHAVIOR_EXIT_NORMAL = 10;
	public static final byte INSCOPE_BEHAVIOR_EXIT_EXCEPTION = 11;
	
	/**
	 * Entering into an out-of-scope behavior
	 */
	public static final byte OUTOFSCOPE_BEHAVIOR_ENTER = 12;
	
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL = 13;
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION = 14;
	
	/**
	 * An unmonitored method call returned normally 
	 */
	public static final byte UNMONITORED_BEHAVIOR_CALL_RESULT = 15;
	
	public static final byte REGISTER_OBJECT = 20;
	public static final byte REGISTER_REFOBJECT = 21;
	public static final byte REGISTER_CLASS = 22;
	public static final byte REGISTER_CLASSLOADER = 23;
	public static final byte REGISTER_THREAD = 24;
	
	/**
	 * Args: timestamp
	 */
	public static final byte SYNC = 100;
	
}
