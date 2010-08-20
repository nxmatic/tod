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

import static tod.impl.bci.asm2.BCIUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.bci.asm2.MethodInfo.BCIFrame;
import zz.utils.Utils;

public class MethodReplayerGenerator_FirstPass extends MethodReplayerGenerator
{
	private int itsSnapshotSeqVar;
	
	private int itsProbeIndex = 0;

	public MethodReplayerGenerator_FirstPass(
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			IBehaviorInfo aBehavior,
			ClassNode aClassNode,
			MethodNode aMethodNode)
	{
		super(aConfig, aDatabase, aBehavior, aClassNode, aMethodNode);
	}
	
	@Override
	protected String getClassDumpSubpath()
	{
		return "first";
	}


	@Override
	protected void allocVars()
	{
		super.allocVars();
		itsSnapshotSeqVar = nextFreeVar(1);
	}
	
	@Override
	protected void addSnapshotSetup(InsnList aInsns)
	{
		SList s = new SList();

		s.ALOAD(getThreadReplayerSlot());
		s.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getSnapshotSeq", "()I");
		s.ISTORE(itsSnapshotSeqVar);
		
		aInsns.insert(s);
	}
	
	@Override
	protected void insertSnapshotProbe(SList s, AbstractInsnNode aReferenceNode, boolean aSaveStack)
	{
		BCIFrame theFrame = getMethodInfo().getFrame(aReferenceNode.getNext());
		String theLocalsSig = BCIUtils.getSnapshotSig(theFrame, aSaveStack);
		List<Type> theArgTypes = new ArrayList<Type>();
		theArgTypes.add(Type.INT_TYPE); // Snapshot seq
		theArgTypes.add(Type.INT_TYPE); // Probe id
		Type[] theStackTypes = aSaveStack ? getStackTypes(theFrame) : null;
		
		if (aSaveStack) genSaveStack(s, theStackTypes);

		s.ALOAD(0);
		s.ILOAD(itsSnapshotSeqVar);
		int theProbeId = getDatabase().getNewSnapshotProbe(getBehaviorId(), ++itsProbeIndex, theLocalsSig).id;
		s.pushInt(theProbeId);
		
		int theLocals = theFrame.getLocals();
		for(int i=0;i<theLocals;i++) 
		{
			Type theType = theFrame.getLocal(i).getType();
			if (theType == null) continue;

			theArgTypes.add(BCIUtils.getActualReplayType(theType));
			s.ILOAD(theType, i+1);
		}

		if (aSaveStack) 
		{
			genReverseLoadStack(s, theStackTypes);
			for(Type theType : theStackTypes) theArgTypes.add(theType);
		}

		String theDesc = Type.getMethodDescriptor(Type.INT_TYPE, theArgTypes.toArray(new Type[theArgTypes.size()]));

		s.INVOKEVIRTUAL(CLS_THREADREPLAYER, MethodReplayerGenerator.SNAPSHOT_METHOD_NAME, theDesc);
		s.ISTORE(itsSnapshotSeqVar);
		
		if (aSaveStack) genLoadStack(s, theStackTypes);
	}
}
