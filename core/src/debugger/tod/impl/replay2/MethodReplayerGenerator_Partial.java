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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.bci.asm2.MethodInfo.BCIFrame;

public class MethodReplayerGenerator_Partial extends MethodReplayerGenerator
{
	private int itsStartProbeVar;
	private int itsSnapshotSeqVar;
	private int itsSnapshotRetVar;
	private int itsSnapshotProbeIdVar;
	private int itsSnapshotVar;
	
	private List<Label> itsSnapshotProbes = new ArrayList<Label>();
	
	/**
	 * Maintains a mapping of local variable signature (ie. one character per live local slot indicating 
	 * its current type) to the label of a subroutine that performs a snapshot/resume for locals of this type. 
	 */
	private Map<String, Label> itsLocalsSigToLabel = new HashMap<String, Label>();
	
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
		itsSnapshotSeqVar = nextFreeVar(1);
		itsSnapshotRetVar = nextFreeVar(1);
		itsSnapshotProbeIdVar = nextFreeVar(1);
		itsSnapshotVar = nextFreeVar(1);

		SList s = new SList();

		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getSnapshotSeq", "()I");
		s.ISTORE(itsSnapshotSeqVar);
		
		Label lBadProbeId = new Label();
		
		itsSnapshotProbes.add(0, getCodeStartLabel());
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getStartProbe", "()I");
		s.DUP();
		s.ISTORE(itsStartProbeVar);
		s.TABLESWITCH(0, itsSnapshotProbes.size()-1, lBadProbeId, itsSnapshotProbes.toArray(new Label[itsSnapshotProbes.size()]));
		
		s.label(lBadProbeId);
		s.ILOAD(itsStartProbeVar);
		s.createRTExArg("No such probe: ");
		s.ATHROW();
		
		aInsns.insert(s);
	}
	
	@Override
	protected void insertSnapshotProbe(SList s, AbstractInsnNode aReferenceNode)
	{
		String theLocalsSig = getLocalsSig(aReferenceNode);
		Label lCheckSnapshot = itsLocalsSigToLabel.get(theLocalsSig);
		if (lCheckSnapshot == null)
		{
			lCheckSnapshot = makeSnapshotChecker(theLocalsSig, aReferenceNode);
			itsLocalsSigToLabel.put(theLocalsSig, lCheckSnapshot);
		}
		
		Label lNoCheck = new Label();
		Label lProbe = new Label();
		
		int theProbeIndex = itsSnapshotProbes.size();
		itsSnapshotProbes.add(lProbe);
		int theProbeId = getDatabase().addSnapshotProbe(getBehaviorId(), theProbeIndex);
		
		s.label(lProbe);
		s.LDC(theLocalsSig);
		s.POP();
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getSnapshotSeq", "()I");
		s.ILOAD(itsSnapshotSeqVar);
		s.IF_ICMPLE(lNoCheck);
		s.LDC(theProbeId);
		s.JSR(lCheckSnapshot);
		s.label(lNoCheck);
	}
	
	private Label makeSnapshotChecker(String aLocalsSig, AbstractInsnNode aReferenceNode)
	{
		BCIFrame theFrame = getMethodInfo().getFrame(aReferenceNode);
		int theLocals = theFrame.getLocals();

		Label lCheckSnapshot = new Label();
		Label lResumeFromSnapshot = new Label();
		
		SList s = new SList();
		
		s.label(lCheckSnapshot);
		s.LDC(aLocalsSig);
		s.POP();
		
		s.ASTORE(itsSnapshotRetVar); // Store return address var
		s.ISTORE(itsSnapshotProbeIdVar); // Store probe id
		
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getSnapshotForResume", "()"+DSC_LOCALSSNAPSHOT);
		s.DUP();
		s.ASTORE(itsSnapshotVar);
		s.IFNONNULL(lResumeFromSnapshot);
		
		// Take snapshot
		
		LocalsMapInfo theLocalsMapInfo = new LocalsMapInfo();
		for(int i=1;i<theLocals;i++) // First slot is the frame
		{
			Type theType = theFrame.getLocal(i).getType();
			if (theType == null) continue;
			updateLocalsMapInfo(theLocalsMapInfo, theType);
		}
		
		if (theLocalsMapInfo.isEmpty())
		{
			s.ALOAD(0);
			s.ILOAD(itsSnapshotProbeIdVar);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "registerEmptySnapshot", "(I)V");
		}
		else
		{
			s.ALOAD(0);
			s.ILOAD(itsSnapshotProbeIdVar);
			s.pushInt(theLocalsMapInfo.intValuesCount);
			s.pushInt(theLocalsMapInfo.longValuesCount);
			s.pushInt(theLocalsMapInfo.floatValuesCount);
			s.pushInt(theLocalsMapInfo.doubleValuesCount);
			s.pushInt(theLocalsMapInfo.refValuesCount);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "createSnapshot", "(IIIIII)"+DSC_LOCALSSNAPSHOT);
			s.ASTORE(itsSnapshotVar);
			
			for(int i=1;i<theLocals;i++) // First slot is the frame
			{
				Type theType = theFrame.getLocal(i).getType();
				if (theType == null) continue;
				s.ALOAD(itsSnapshotVar);
				s.ILOAD(theType, i); // TODO: adjust for big slots
				invokeSnapshotPush(s, theType);
			}
			
			s.ALOAD(0);
			s.ALOAD(itsSnapshotVar);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "registerSnapshot", "("+DSC_LOCALSSNAPSHOT+")V");
		}

		// Update seq
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getSnapshotSeq", "()I");
		s.ISTORE(itsSnapshotSeqVar);

		s.RET(itsSnapshotRetVar);
		
		// Resume from snapshot
		s.label(lResumeFromSnapshot);
		
		for(int i=1;i<theLocals;i++) // First slot is the frame
		{
			Type theType = theFrame.getLocal(i).getType();
			if (theType == null) continue;
			s.ALOAD(itsSnapshotVar);
			invokeSnapshotPop(s, theType);
			s.ISTORE(theType, i); // TODO: adjust for big slots
		}

		s.RET(itsSnapshotRetVar);
		
		addAdditionalInstructions(s);
		
		return lCheckSnapshot;
	}
	
	private String getLocalsSig(AbstractInsnNode aNode)
	{
		BCIFrame theFrame = getMethodInfo().getFrame(aNode);
		int theLocals = theFrame.getLocals();
		char[] theChars = new char[theLocals];
		theChars[0] = (char) theFrame.getStackSize();
		for(int i=1;i<theLocals;i++) // The first slot is the frame
		{
			Type theType = theFrame.getLocal(i).getType();
			theChars[i] = theType != null ? getTypeChar(theType) : (char)0;
		}
		return new String(theChars);
	}
	
	private static char getTypeChar(Type aType)
	{
		switch(aType.getSort())
		{
		case Type.ARRAY:
		case Type.OBJECT:
			return 'L';
			
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.INT:
		case Type.SHORT:
			return 'I';
			
		case Type.DOUBLE:
			return 'D';
			
		case Type.FLOAT:
			return 'F';
			
		case Type.LONG:
			return 'J';

		default:
			throw new RuntimeException("Not handled: "+aType);	
		}
	}
	
	private static void invokeSnapshotPush(SList s, Type aType)
	{
		switch(aType.getSort())
		{
		case Type.ARRAY:
		case Type.OBJECT:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "pushRef", "("+DSC_OBJECTID+")V");
			break;
			
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.INT:
		case Type.SHORT:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "pushInt", "(I)V");
			break;
			
		case Type.DOUBLE:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "pushDouble", "(D)V");
			break;
			
		case Type.FLOAT:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "pushFloat", "(F)V");
			break;
			
		case Type.LONG:
			s.INVOKEVIRTUAL(CLS_LOCALSSNAPSHOT, "pushLong", "(J)V");
			break;

		default:
			throw new RuntimeException("Not handled: "+aType);	
		}
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
	
	private static void updateLocalsMapInfo(LocalsMapInfo aInfo, Type aType)
	{
		switch(aType.getSort())
		{
		case Type.ARRAY:
		case Type.OBJECT:
			aInfo.refValuesCount++;
			break;
			
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.INT:
		case Type.SHORT:
			aInfo.intValuesCount++;
			break;
			
		case Type.DOUBLE:
			aInfo.doubleValuesCount++;
			break;
			
		case Type.FLOAT:
			aInfo.floatValuesCount++;
			break;
			
		case Type.LONG:
			aInfo.longValuesCount++;
			break;
			
		default:
			throw new RuntimeException("Not handled: "+aType);	
		}
	}
	
	private static class LocalsMapInfo
	{
		public int intValuesCount;
		public int longValuesCount;
		public int floatValuesCount;
		public int doubleValuesCount;
		public int refValuesCount;
		
		public boolean isEmpty()
		{
			return intValuesCount + longValuesCount + floatValuesCount + doubleValuesCount + refValuesCount == 0;
		}
	}

}
