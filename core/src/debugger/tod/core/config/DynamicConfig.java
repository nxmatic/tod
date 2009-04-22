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

import java.util.EnumMap;
import java.util.Map;

import tod.agent.Output;


/**
 * @author gpothier
 */
public class DynamicConfig
{
	private static DynamicConfig INSTANCE = new DynamicConfig();

	public static DynamicConfig getInstance()
	{
		return INSTANCE;
	}

	private DynamicConfig()
	{
		setOutputBehaviour(Output.OUT, false, false, false);
		setOutputBehaviour(Output.ERR, false, false, false);
	}
	
	private Map<Output, OutputBehaviour> itsOutputBehaviours =
		new EnumMap<Output, OutputBehaviour>(Output.class);
	
	private void setOutputBehaviour (
			Output aOutput,
			boolean aCollect,
			boolean aWriteThrough,
			boolean aThreadSafe)
	{
		itsOutputBehaviours.put (
				aOutput, 
				new OutputBehaviour(aCollect, aWriteThrough, aThreadSafe));
	}

	public OutputBehaviour getOutputBehaviour (Output aOutput)
	{
		return itsOutputBehaviours.get (aOutput);
	}
	
	public static class OutputBehaviour
	{
		private boolean itsCollect;
		private boolean itsWriteThrough;
		private boolean itsThreadSafe;
		
		public OutputBehaviour(boolean aCollect, boolean aWriteThrough, boolean aThreadSafe)
		{
			itsCollect = aCollect;
			itsWriteThrough = aWriteThrough;
			itsThreadSafe = aThreadSafe;
		}
		
		
		public boolean getCollect()
		{
			return itsCollect;
		}
		
		public boolean getThreadSafe()
		{
			return itsThreadSafe;
		}
		
		public boolean getWriteThrough()
		{
			return itsWriteThrough;
		}
	}
	
}
