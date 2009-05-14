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
package java.tod;

import java.tod.io._IO;


/**
 * This class provides a method that is called by the JNI side when
 * an exception is generated.
 * @author gpothier
 */
public class ExceptionGeneratedReceiver
{
	// Ensures the collector is loaded
	private static EventCollector COLLECTOR = EventCollector.INSTANCE;
	
	private static final ThreadLocal<Boolean> processingExceptions = 
		new ThreadLocal<Boolean>()
		{
			@Override
			protected Boolean initialValue()
			{
				return false;
			}
		};
	
	private static final ThreadLocal<Boolean> ignoreExceptions = 
		new ThreadLocal<Boolean>()
		{
			@Override
			protected Boolean initialValue()
			{
				return false;
			}
		};
			
	/**
	 * Sets the ignore next exception flag of the current thread.
	 * This is called by instrumented classes.
	 */
	public static void ignoreNextException()
	{
		if (AgentReady.COLLECTOR_READY) COLLECTOR.ignoreNextException();
	}
	
	/**
	 * Used to avoid processing exceptions while registered objects are sent.
	 */
	public static void setIgnoreExceptions(boolean aIgnore)
	{
		ignoreExceptions.set(aIgnore);
	}
	
	public static void exceptionGenerated(
			String aMethodName,
			String aMethodSignature,
			String aMethodDeclaringClassSignature,
			int aOperationBytecodeIndex,
			Throwable aThrowable)
	{
		try
		{
			if (ignoreExceptions.get()) return;
			if (! AgentReady.COLLECTOR_READY) return;
			if (! AgentReady.CAPTURE_ENABLED) return;
			
			if (processingExceptions.get()) 
			{
				_IO.err("[TOD] Recursive exception, probably because we got disconnected from the database.");
				System.exit(1);
			}
			processingExceptions.set(true);
			
//			_IO.out(String.format("Exception generated: %s.%s, %d", aMethodDeclaringClassSignature, aMethodName, aOperationBytecodeIndex));
			COLLECTOR.getThreadData().evExceptionGenerated(
					aMethodName,
					aMethodSignature, 
					aMethodDeclaringClassSignature,
					aOperationBytecodeIndex, 
					aThrowable);
			
			processingExceptions.set(false);
		}
		catch (Throwable e)
		{
			_IO.err("[TOD] Exception in ExceptionGeneratedReceiver.exceptionGenerated:");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
