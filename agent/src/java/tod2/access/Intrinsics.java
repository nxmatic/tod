package tod2.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class Intrinsics
{
	private static Method itsCloneMethod;
	
	/**
	 * The {@link ClassLoader#loadClass(String)} method is sometimes called automatically
	 * by the VM, so we register it as a classloader method. But it is possible to call it
	 * directly, so we need to be able to differentiate. In-scope code calls to it are replaced
	 * by calls to this method.
	 */
	public static Class<?> ClassLoader_loadClass(ClassLoader aTarget, String aName) throws ClassNotFoundException
	{
		return aTarget.loadClass(aName);
	}
	
	public static boolean Object_equals(Object aTarget, Object aOther)
	{
		return aTarget.equals(aOther);
	}

	public static int Object_hashCode(Object aTarget)
	{
		return aTarget.hashCode();
	}
	
	public static Object Object_clone(Object aTarget)
	{
		try
		{
			if (itsCloneMethod == null)
			{
				itsCloneMethod = Object.class.getDeclaredMethod("clone");
				itsCloneMethod.setAccessible(true);
			}
			return itsCloneMethod.invoke(aTarget);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static int String_length(String aTarget)
	{
		return aTarget.length();
	}
	
	public static char String_charAt(String aTarget, int aIndex)
	{
		return aTarget.charAt(aIndex);
	}
	
	public static int String_compareTo(String aTarget, String aOther)
	{
		return aTarget.compareTo(aOther);
	}
	
	public static int String_compareToIgnoreCase(String aTarget, String aOther)
	{
		return aTarget.compareToIgnoreCase(aOther);
	}
}
