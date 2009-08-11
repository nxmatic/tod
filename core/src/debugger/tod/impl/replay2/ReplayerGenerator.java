/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.replay2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Type;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase;

public class ReplayerGenerator
{
	
	/**
	 * Returns a set of all descriptors that are used by methods in the given database
	 */
	public Set<MethodDescriptor> getUsedDescriptors(IStructureDatabase aDatabase)
	{
		Set<MethodDescriptor> theResult = new HashSet<MethodDescriptor>();
		IBehaviorInfo[] theBehaviors = aDatabase.getBehaviors();
		for (IBehaviorInfo theBehavior : theBehaviors)
		{
			String theSignature = theBehavior.getSignature();
			Type[] theArgumentTypes = Type.getArgumentTypes(theSignature);
			theResult.add(new MethodDescriptor(theArgumentTypes));
		}
		return theResult;
	}
	
	/**
	 * Represents a method descriptor (argument types).
	 * Properly implements equals and hashCode.
	 * @author gpothier
	 */
	private static class MethodDescriptor
	{
		private byte[] itsSorts;
		
		public MethodDescriptor(Type... aTypes)
		{
			itsSorts = new byte[aTypes.length];
			for(int i=0;i<aTypes.length;i++) 
			{
				int theSort = aTypes[i].getSort();
				if (theSort == Type.ARRAY) theSort = Type.OBJECT;
				assert theSort <= Byte.MAX_VALUE;
				itsSorts[i] = (byte) theSort;
			}
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(itsSorts);
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MethodDescriptor other = (MethodDescriptor) obj;
			if (!Arrays.equals(itsSorts, other.itsSorts)) return false;
			return true;
		}
	}
}
