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
package tod;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMemberInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.gui.IGUIManager;
import tod.gui.formatter.CustomFormatterRegistry;
import tod.utils.ConfigUtils;
import zz.utils.srpc.RIRegistry;
import zz.utils.srpc.SRPCRegistry;
import zz.utils.srpc.SRPCServer;

/**
 * @author gpothier
 */
public class Util
{
	public static final int TOD_SRPC_PORT = 
		ConfigUtils.readInt("debug-server-port", ConfigUtils.readInt("client-port", 8068)-1)+1;
	
	private static SRPCRegistry SRPC_REGISTRY;
	
	/**
	 * Path to the development eclipse workspace.
	 * It is used during development to avoid rebuilding jars. 
	 * If null, the development workspace is not available.
	 */
	public static final String workspacePath = System.getProperty("dev.path");
	
	public static void ensureSize (List<?> aList, int aSize)
	{
		while (aList.size() <= aSize) aList.add (null);
	}
	
	/**
	 * Retrieves the package name of the given class
	 */
	public static String getPackageName(String aFullyQualifiedName)
	{
		int theIndex = aFullyQualifiedName.lastIndexOf('.');
		if (theIndex == -1) return "";
		else return aFullyQualifiedName.substring(0, theIndex);
	}
	
	/**
	 * Strips the package name from the given class.
	 */
	public static String getSimpleName(String aFullyQualifiedName)
	{
		int theIndex = aFullyQualifiedName.lastIndexOf('.');
		
		String theName = theIndex == -1 ?
				aFullyQualifiedName
				: aFullyQualifiedName.substring(theIndex+1);
		
		return theName.replace('$', '.');
	}
	
	/**
	 * Transforms a JVM class descriptor into a normal, source-level class name.
	 */
	public static String jvmToScreen(String aName)
	{
		return aName.replace('/', '.');
	}
	
	public static String getSimpleInnermostName(String aFullyQualifiedName)
	{
		int theIndex = Math.max(
				aFullyQualifiedName.lastIndexOf('.'),
				Math.max(
						aFullyQualifiedName.lastIndexOf('/'),
						aFullyQualifiedName.lastIndexOf('$')));
		
		String theName = theIndex == -1 ?
				aFullyQualifiedName
				: aFullyQualifiedName.substring(theIndex+1);
		
		return theName;
	}
	
	/**
	 * Returns the name of the class,
	 * with the '$' changed to a '.' in the case
	 * of an inner class.
	 */
	public static String getPrettyName(String aFullyQualifiedName)
	{
		return aFullyQualifiedName.replace('$', '.');
	}
	
	public static SRPCRegistry getLocalSRPCRegistry()
	{
		if (SRPC_REGISTRY == null)
		{
			SRPCServer theServer = new SRPCServer(TOD_SRPC_PORT, false);
			SRPC_REGISTRY = theServer.getRegistry();
		}
		return SRPC_REGISTRY;
	}
	
	public static RIRegistry getRemoteSRPCRegistry(String aHost, int aPort)
	{
		try
		{
			return SRPCServer.connectTo(aHost, aPort);
		}
		catch (UnknownHostException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns the name of the behavior plus its arguments.
	 */
	public static String getFullName(IBehaviorInfo aBehavior)
	{
		String theName = aBehavior.getName();
		if ("<init>".equals(theName)) theName = getSimpleInnermostName(aBehavior.getDeclaringType().getName());
		
		StringBuilder theBuilder = new StringBuilder(theName);
		theBuilder.append('(');
		boolean theFirst = true;
		for (ITypeInfo theType : aBehavior.getArgumentTypes())
		{
			if (theFirst) theFirst = false;
			else theBuilder.append(", ");
			theBuilder.append(theType.getName());
		}
		theBuilder.append(')');
		return theBuilder.toString();
	}
	
	/**
	 * Returns the full name of the given member, including the parameters if
	 * it is a behavior. 
	 */
	public static String getFullName(IMemberInfo aMember)
	{
		if (aMember instanceof IBehaviorInfo)
		{
			IBehaviorInfo theBehavior = (IBehaviorInfo) aMember;
			return getFullName(theBehavior);
		}
		else if (aMember instanceof IFieldInfo)
		{
			IFieldInfo theField = (IFieldInfo) aMember;
			return theField.getName();
		}
		else throw new RuntimeException("Not handled: "+aMember);
	}
	
	/**
	 * Returns a suitable name for the given object, using formatters if necessary.
	 * @param aCurrentObject The current "this" object, if any.
	 */
	public static String getObjectName(
			IGUIManager aGUIManager, 
			ObjectId aObject, 
			Object aCurrentObject,
			ILogEvent aRefEvent)
	{
		ILogBrowser theLogBrowser = aGUIManager.getSession().getLogBrowser();

		String theText;
		if (aCurrentObject != null && aCurrentObject.equals(aObject)) theText = "this";
		else 
		{
			IObjectInspector theInspector = theLogBrowser.createObjectInspector(aObject);
			theInspector.setReferenceEvent(aRefEvent);
			theText = CustomFormatterRegistry.formatObjectShort(
					aGUIManager, 
					theInspector, 
					false);
		}
		
		return theText;
	}
	

}
