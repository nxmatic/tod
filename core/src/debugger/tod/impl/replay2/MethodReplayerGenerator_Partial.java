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

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase.SnapshotProbeInfo;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.bci.asm2.MethodInfo.BCIFrame;

public class MethodReplayerGenerator_Partial extends MethodReplayerGenerator
{
	private int itsSnapshotRetVar;
	private int itsStartProbeIdVar;
	private int itsSnapshotVar;
	private int itsStartedVar;
	
	private List<Label> itsSnapshotProbes = new ArrayList<Label>();
	
	public MethodReplayerGenerator_Partial(
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			ReplayerGenerator aGenerator,
			int aBehaviorId,
			ClassNode aClassNode,
			MethodNode aMethodNode)
	{
		super(aConfig, aDatabase, aGenerator, aBehaviorId, aClassNode, aMethodNode);
	}

	@Override
	protected void addSnapshotSetup(InsnList aInsns)
	{
		itsSnapshotRetVar = nextFreeVar(1);
		itsStartProbeIdVar = nextFreeVar(1);
		itsSnapshotVar = nextFreeVar(1);
		itsStartedVar = nextFreeVar(1);

		SList s = new SList();

		Label lBadProbeId = new Label();

		s.pushInt(0);
		s.ISTORE(itsStartedVar); // Mark replay as not started yet
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getStartProbe", "()I");
		s.DUP();
		s.ISTORE(itsStartProbeIdVar);
		itsSnapshotProbes.add(0, getCodeStartLabel());
		s.TABLESWITCH(0, itsSnapshotProbes.size()-1, lBadProbeId, itsSnapshotProbes.toArray(new Label[itsSnapshotProbes.size()]));
		
		s.label(lBadProbeId);
		s.ILOAD(itsStartProbeIdVar);
		s.createRTExArg("No such probe: ");
		s.ATHROW();
		
		aInsns.insert(s);
	}
	
	@Override
	protected void insertSnapshotProbe(SList s, AbstractInsnNode aReferenceNode, boolean aSaveStack)
	{
		BCIFrame theFrame = getMethodInfo().getFrame(aReferenceNode.getNext());
		String theLocalsSig = BCIUtils.getSnapshotSig(theFrame, aSaveStack);

		Label lNoCheck = new Label();
		Label lProbe = new Label();
		
		int theProbeIndex = itsSnapshotProbes.size();
		itsSnapshotProbes.add(lProbe);
		SnapshotProbeInfo theProbe = getDatabase().getNewSnapshotProbe(getBehaviorId(), theProbeIndex, theLocalsSig);
		
		s.label(lProbe);
		s.ILOAD(itsStartedVar);
		s.IFtrue(lNoCheck);
		
		s.pushInt(1);
		s.ISTORE(itsStartedVar); // mark replay as started
		
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getSnapshotForResume", "()"+DSC_LOCALSSNAPSHOT);
		s.ASTORE(itsSnapshotVar);
		
		if (aSaveStack)
		{
			Type[] theStackTypes = getStackTypes(theFrame);
			for(Type theType : theStackTypes) 
			{
				s.ALOAD(itsSnapshotVar);
				invokeSnapshotPop(s, theType);
			}
		}

		int theLocals = theFrame.getLocals();
		for(int i=theLocals-1;i>=0;i--) 
		{
			Type theType = theFrame.getLocal(i).getType();
			if (theType == null) continue;

			s.ALOAD(itsSnapshotVar);
			invokeSnapshotPop(s, theType);
			s.ISTORE(theType, i+1);
		}
	
		s.label(lNoCheck);
	}
	
	private static void invokeSnapshotPop(SList s, Type aType)
	{
		switch(aType.getSort())
		{
		case Type.ARRAY:
		case Type.OBJECT:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "popRef", "()"+DSC_LOCALSSNAPSHOT);
			break;
			
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.INT:
		case Type.SHORT:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "popInt", "()I");
			break;
			
		case Type.DOUBLE:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "popDouble", "()D");
			break;
			
		case Type.FLOAT:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "popFloat", "()F");
			break;
			
		case Type.LONG:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "popLong", "()J");
			break;
			
		default:
			throw new RuntimeException("Not handled: "+aType);	
		}
	}
	
}
