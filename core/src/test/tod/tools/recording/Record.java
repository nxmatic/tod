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
package tod.tools.recording;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import zz.utils.Utils;

/**
 * An history record of database access.
 * @author gpothier
 */
public abstract class Record implements Serializable
{
	private static final long serialVersionUID = 2484097571786422998L;
	
	private int itsThreadId;
	private ProxyObject itsTarget;
	private MethodSignature itsMethod;
	private Object[] itsArgs;
	private Object itsResult;
	private String itsLocation;
	
	public Record(
			int aThreadId,
			ProxyObject aTarget,
			MethodSignature aMethod,
			Object[] aArgs,
			Object aResult,
			String aLocation)
	{
		itsThreadId = aThreadId;
		itsTarget = aTarget;
		itsMethod = aMethod;
		itsArgs = aArgs;
		itsResult = aResult;
		itsLocation = aLocation;
	}

	public ProxyObject getTarget()
	{
		return itsTarget;
	}

	public MethodSignature getMethod()
	{
		return itsMethod;
	}

	public Object[] getArgs()
	{
		return itsArgs;
	}

	public Object getResult()
	{
		return itsResult;
	}

	protected Object resolve(final Map<Integer, Object> aObjectsMap, Object aObject)
	{
		return map(aObject, new IMapper()
		{
			public Object map(Object aSource)
			{
				if (aSource instanceof ProxyObject)
				{
					ProxyObject theProxy = (ProxyObject) aSource;
					return theProxy.resolve(aObjectsMap);
				}
				else return aSource;
			}
		});
	}
	
	/**
	 * "Fixes" the argument so that arrays are of the proper type.
	 */
	protected Object fixArg(Object aArg, Class aExpectedType)
	{
		if (aExpectedType.isArray())
		{
			int theLength = Array.getLength(aArg);
			Object theResult = Array.newInstance(aExpectedType.getComponentType(), theLength);
			for(int i=0;i<theLength;i++) Array.set(theResult, i, Array.get(aArg, i));
			return theResult;
		}
		else return aArg;
	}
	
	protected Object[] fixArgs(Object[] aArgs, Class<?>[] aParameterTypes)
	{
		Object[] theResult = new Object[aArgs.length];
		
		for (int i=0;i<theResult.length;i++)
		{
			theResult[i] = fixArg(aArgs[i], aParameterTypes[i]); 
		}
		
		return theResult;
	}
	
	public abstract void process(Map<Integer, Object> aObjectsMap) throws Exception;
	
	protected void registerResult(
			Map<Integer, Object> aObjectsMap, 
			Object aFormalResult, 
			Object aActualResult)
	{
		if (aFormalResult == null) return;
		else if (aFormalResult instanceof ProxyObject)
		{
			ProxyObject theProxy = (ProxyObject) aFormalResult;
			aObjectsMap.put(theProxy.getId(), aActualResult);
		}
		else if (aFormalResult.getClass().getComponentType() != null
				&& ! aFormalResult.getClass().getComponentType().isPrimitive())
		{
			int theLength = Array.getLength(aFormalResult);
			Object[] theActualArray = toArray(aActualResult);
			for(int i=0;i<theLength;i++)
			{
				registerResult(
						aObjectsMap, 
						Array.get(aFormalResult, i), 
						Array.get(theActualArray, i));
			}
		}
	}
	
	protected Object[] toArray(Object aObject)
	{
		if (aObject == null) return null;
		else if (aObject.getClass().isArray()) return (Object[]) aObject;
		else if (aObject instanceof Iterable)
		{
			Iterable theIterable = (Iterable) aObject;
			List theList = new ArrayList();
			Utils.fillCollection(theList, theIterable);
			return theList.toArray();
		}
		else throw new RuntimeException("not handled: "+aObject);
	}
	
	protected String format(Object aObject)
	{
		if (aObject == null) return "null";
		else if (aObject.getClass().isArray())
		{
			StringBuilder theBuilder = new StringBuilder("[");
			int theLength = Array.getLength(aObject);
			for(int i=0;i<theLength;i++)
			{
				theBuilder.append(format(Array.get(aObject, i)));
				theBuilder.append(", ");
			}
			
			return theBuilder.toString();
		}
		else return ""+aObject;
	}
	
	@Override
	public String toString()
	{
		return String.format(
				"Record [th: %d, tgt: %d, m: %s, args: %s] -> %s - loc: %s",
				itsThreadId,
				itsTarget != null ? itsTarget.getId() : null, 
				itsMethod,
				Arrays.asList(itsArgs),
				format(itsResult),
				itsLocation);
	}
	
	public Object map(Object aSource, IMapper aMapper)
	{
		if (aSource == null) return null;
		else if (aSource.getClass().isArray())
		{
			if (aSource.getClass().getComponentType().isPrimitive()) return aSource;
			else
			{
				int theLength = Array.getLength(aSource);
				Object theArray = Array.newInstance(aSource.getClass().getComponentType(), theLength);
				for(int i=0;i<theLength;i++)
				{
					Array.set(theArray, i, map(Array.get(aSource, i), aMapper));
				}
				
				return theArray;
			}
		}
		else return aMapper.map(aSource);
	}

	public static class Call extends Record
	{
		private static final long serialVersionUID = 3691035497463713498L;
		
		public Call(
				int aThreadId,
				ProxyObject aTarget,
				MethodSignature aMethod,
				Object[] aArgs,
				Object aResult,
				String aLocation)
		{
			super(aThreadId, aTarget, aMethod, aArgs, aResult, aLocation);
		}
		
		public void process(Map<Integer, Object> aObjectsMap) throws Exception
		{
			Object theTarget = resolve(aObjectsMap, getTarget());
			Class<?> theClass = theTarget.getClass();
			Method theMethod = getMethod().findMethod(theClass);
			Object theResult = null;
			try
			{
				theMethod.setAccessible(true);
				theResult = theMethod.invoke(
						theTarget, 
						fixArgs((Object[]) resolve(aObjectsMap, getArgs()), theMethod.getParameterTypes()));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}

			registerResult(aObjectsMap, getResult(), theResult);
		}
	}
	
	public static class New extends Record
	{
		private static final long serialVersionUID = 87635235091283764L;

		public New(
				int aThreadId,
				MethodSignature aMethod,
				Object[] aArgs,
				Object aResult,
				String aLocation)
		{
			super(aThreadId, null, aMethod, aArgs, aResult, aLocation);
		}

		public void process(Map<Integer, Object> aObjectsMap) throws Exception
		{
			Constructor theConstructor = getMethod().findConstructor();
			Object theResult = null;
			try
			{
				theConstructor.setAccessible(true);
				theResult = theConstructor.newInstance(
						fixArgs((Object[]) resolve(aObjectsMap, getArgs()), theConstructor.getParameterTypes()));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}

			registerResult(aObjectsMap, getResult(), theResult);
		}
	}
	
	public interface IMapper
	{
		public Object map(Object aSource);
	}
	
	/**
	 * An object that is not yet known but that should be
	 * known by the time it is used.
	 * @author gpothier
	 */
	public static class ProxyObject implements Serializable
	{
		private static final long serialVersionUID = 558471669354712358L;
		private int itsId;
		
		public ProxyObject(int aId)
		{
			itsId = aId;
		}

		public Object resolve(Map<Integer, Object> aObjectsMap)
		{
			Object theObject = aObjectsMap.get(itsId);
			assert theObject != null : "No object for id "+itsId;
			return theObject;
		}
		
		public int getId()
		{
			return itsId;
		}
		
		@Override
		public String toString()
		{
			return "Proxy ["+itsId+"]";
		}
	}

	public static class MethodSignature implements Serializable
	{
		private static final long serialVersionUID = 1500258777457112475L;
		private String itsMethodName;
		private String[] itsArgTypeNames;
		
		public MethodSignature(String aMethodName, Class[] aArgTypes)
		{
			itsMethodName = aMethodName;
			itsArgTypeNames = new String[aArgTypes.length];
			for(int i=0;i<itsArgTypeNames.length;i++)
			{
				itsArgTypeNames[i] = aArgTypes[i].getName();
			}
		}
		
		public MethodSignature(String aMethodName, String[] aArgTypes)
		{
			itsMethodName = aMethodName;
			itsArgTypeNames = aArgTypes;
		}
		
		public Method findMethod(Class aClass) throws Exception
		{
			Class[] theArgClasses = new Class[itsArgTypeNames.length];
			for(int i=0;i<theArgClasses.length;i++)
			{
				theArgClasses[i] = forName(itsArgTypeNames[i]);
			}
			
			return aClass.getMethod(itsMethodName, theArgClasses);
		}
		
		public Constructor findConstructor() throws Exception
		{
			Class theClass = Class.forName(itsMethodName);
			Class[] theArgClasses = new Class[itsArgTypeNames.length];
			for(int i=0;i<theArgClasses.length;i++)
			{
				theArgClasses[i] = forName(itsArgTypeNames[i]);
			}
			
			return theClass.getConstructor(theArgClasses);
		}
		
		private Class forName(String aName) throws ClassNotFoundException
		{
			if ("long".equals(aName)) return long.class;
			else if ("int".equals(aName)) return int.class;
			else if ("boolean".equals(aName)) return boolean.class;
			else return Class.forName(aName);
		}
		
		@Override
		public String toString()
		{
			return ""+itsMethodName+Arrays.asList(itsArgTypeNames);
		}
	}
}