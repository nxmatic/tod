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
	private int itsStartProbeIdVar;
	private int itsSnapshotVar;
	private final LocalsSnapshot itsSnapshot;
	private final SnapshotProbeInfo itsSnapshotProbeInfo;
	
	private int itsProbeIndex = 0;
	private Label itsStartProbe;
	
	public MethodReplayerGenerator_Partial(
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			ReplayerGenerator aGenerator,
			int aBehaviorId,
			ClassNode aClassNode,
			MethodNode aMethodNode,
			LocalsSnapshot aSnapshot)
	{
		super(aConfig, aDatabase, aGenerator, aBehaviorId, aClassNode, aMethodNode);
		itsSnapshot = aSnapshot;
		itsSnapshotProbeInfo = getDatabase().getSnapshotProbeInfo(itsSnapshot.getProbeId());
	}

	@Override
	protected String getClassDumpSubpath()
	{
		return "partial";
	}

	@Override
	protected void allocVars()
	{
		super.allocVars();
		itsStartProbeIdVar = nextFreeVar(1);
		itsSnapshotVar = nextFreeVar(1);
	}
	
	@Override
	protected void addSnapshotSetup(InsnList aInsns)
	{
		if (getBehaviorId() != itsSnapshotProbeInfo.behaviorId) return;
		
		SList s = new SList();
		s.GOTO(itsStartProbe);
		aInsns.insert(s);
	}
	
	@Override
	protected void insertSnapshotProbe(SList s, AbstractInsnNode aReferenceNode, boolean aSaveStack)
	{
		if (getBehaviorId() != itsSnapshotProbeInfo.behaviorId) return;
		itsProbeIndex++;
		if (itsProbeIndex != itsSnapshotProbeInfo.probeIndex) return;
		
		BCIFrame theFrame = getMethodInfo().getFrame(aReferenceNode.getNext());
		String theLocalsSig = BCIUtils.getSnapshotSig(theFrame, aSaveStack);

		Label lProbe = new Label();

		itsStartProbe = lProbe;
		
		s.label(lProbe);
		
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
	}
	
	private static void invokeSnapshotPop(SList s, Type aType)
	{
		switch(aType.getSort())
		{
		case Type.ARRAY:
		case Type.OBJECT:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "popRef", "()"+DSC_OBJECTID);
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
