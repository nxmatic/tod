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
	/**
	 * Args: value
	 */
	public static final byte FIELD_READ = 1;
	
	/**
	 * Args: value
	 */
	public static final byte ARRAY_READ = 2;
	
	/**
	 * Args: value
	 */
	public static final byte NEW = 3;
	
	/**
	 * Args: name, sig, type, bytecode index, value
	 */
	public static final byte EXCEPTION = 4;
	
	/**
	 * Args: loc
	 */
	public static final byte HANDLER_REACHED = 5;
	
	/**
	 * Entering into an in-scope behavior
	 * Args: method id, [args] (if from out of scope, see {@link #BEHAVIOR_ENTER_ARGS})
	 */
	public static final byte INSCOPE_BEHAVIOR_ENTER = 6;
	
	/**
	 * Behavior arguments, in the case the behavior was called from non-instrumented code.
	 * Args: count, args
	 * Must be sent right after {@link #BEHAVIOR_ENTER}
	 */
	public static final byte BEHAVIOR_ENTER_ARGS = 7;
	
	/**
	 * Target of constructor call, in the case the behavior was called from non-instrumented code.
	 * This is sent after the constructor chaining is finished.
	 * Args: target
	 */
	public static final byte CONSTRUCTOR_TARGET = 8;
	
	/**
	 * Args: 
	 */
	public static final byte INSCOPE_BEHAVIOR_EXIT_NORMAL = 9;
	
	/**
	 * Args: 
	 */
	public static final byte INSCOPE_BEHAVIOR_EXIT_EXCEPTION = 10;
	
	/**
	 * Entering into an out-of-scope behavior
	 * Args: 
	 */
	public static final byte OUTOFSCOPE_BEHAVIOR_ENTER = 11;
	
	/**
	 * Args: result
	 */
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL = 12;
	
	/**
	 * Args: 
	 */
	public static final byte OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION = 13;
	
	/**
	 * An unmonitored method call returned normally 
	 * Args: 
	 */
	public static final byte UNMONITORED_BEHAVIOR_CALL_RESULT = 14;
	
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
