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
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase.SnapshotProbeInfo;
import tod.impl.bci.asm2.MethodInfo.BCIFrame;

public class MethodReplayerGenerator_Partial extends MethodReplayerGenerator
{
	private int itsSnapshotVar;
	private final SnapshotProbeInfo itsResumeSnapshotProbeInfo;
	
	private int itsProbeIndex = 0;
	private Label itsStartProbe;
	private InsnList itsResumeCode;
	
	public MethodReplayerGenerator_Partial(
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			IBehaviorInfo aBehavior,
			String aClassName,
			MethodNode aMethodNode,
			SnapshotProbeInfo aResumeSnapshotProbeInfo)
	{
		super(aConfig, aDatabase, aBehavior, aClassName, aMethodNode);
		itsResumeSnapshotProbeInfo = aResumeSnapshotProbeInfo;
	}
	
	@Override
	protected String getClassName()
	{
		return itsResumeSnapshotProbeInfo != null ? 
				super.getClassName() + "_" + itsResumeSnapshotProbeInfo.id
				: super.getClassName();
	}

	@Override
	protected boolean sendAllEvents()
	{
		return true;
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
		itsSnapshotVar = nextFreeVar(1);
	}
	
	@Override
	protected void addSnapshotSetup(InsnList aInsns)
	{
		if (itsResumeSnapshotProbeInfo == null || getBehaviorId() != itsResumeSnapshotProbeInfo.behaviorId) return;
		
		SList s = new SList();
		s.add(itsResumeCode);
		s.GOTO(itsStartProbe);
		aInsns.insert(s);
	}
	
	@Override
	protected void insertSnapshotProbe(SList s, AbstractInsnNode aReferenceNode, boolean aSaveStack)
	{
		boolean theResumeProbe = true;
		if (itsResumeSnapshotProbeInfo == null || getBehaviorId() != itsResumeSnapshotProbeInfo.behaviorId) theResumeProbe = false;
		else
		{
			itsProbeIndex++;
			if (itsProbeIndex != itsResumeSnapshotProbeInfo.probeIndex) theResumeProbe = false;
		}
		
		if (theResumeProbe)
		{
			BCIFrame theFrame = getMethodInfo().getFrame(aReferenceNode.getNext());

			itsStartProbe = new Label();
			s.label(itsStartProbe);
			
			s = new SList();
			
			s.ALOAD(getThreadReplayerSlot());
			s.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getSnapshotForResume", "()"+DSC_LOCALSSNAPSHOT);
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
				s.ISTORE(theType, transformSlot(i));
			}
			
			itsResumeCode = s;
		}
		else
		{
			s.ALOAD(getThreadReplayerSlot());
			s.INVOKEVIRTUAL(CLS_THREADREPLAYER, "checkSnapshotKill", "()V");
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
