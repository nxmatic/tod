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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.replay2.MethodReplayerGenerator.MethodSignature;
import tod.impl.server.BufferStream;
import zz.utils.ListMap;
import zz.utils.Utils;

public class ReplayerLoader extends ClassLoader
{
	private final ClassLoader itsParent;
	private final IStructureDatabase itsDatabase;
	private final Map<String, byte[]> itsClassesMap = new HashMap<String, byte[]>();
	
	private Constructor its1stPassReplayerCtor;
	private Constructor itsPartialReplayerCtor;
	
	private Object itsGenerator;

	public ReplayerLoader(ClassLoader aParent, IStructureDatabase aDatabase)
	{
		itsParent = aParent;
		itsDatabase = aDatabase;
		modifyBaseClasses(aDatabase);
		
		try
		{
			Class the1stPassClass = loadClass("tod.impl.replay2.ThreadReplayer_FirstPass");
			its1stPassReplayerCtor = the1stPassClass.getConstructor(
					ReplayerLoader.class, 
					int.class,
					TODConfig.class, 
					IMutableStructureDatabase.class, 
					EventCollector.class,
					TmpIdManager.class, 
					BufferStream.class);
			
			Class thePartialClass = loadClass("tod.impl.replay2.ThreadReplayer_Partial");
			itsPartialReplayerCtor = thePartialClass.getConstructor(
					ReplayerLoader.class, 
					int.class,
					TODConfig.class, 
					IMutableStructureDatabase.class, 
					EventCollector.class,
					TmpIdManager.class, 
					BufferStream.class,
					LocalsSnapshot.class);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public Object createReplayer(
			LocalsSnapshot aSnapshot,
			int aThreadId,
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		try
		{
			if (aSnapshot == null)
			{
				return its1stPassReplayerCtor.newInstance(this, aThreadId, aConfig, aDatabase, aCollector, aTmpIdManager, aBuffer);
			}
			else
			{
				return itsPartialReplayerCtor.newInstance(this, aThreadId, aConfig, aDatabase, aCollector, aTmpIdManager, aBuffer, aSnapshot);
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public Object getGenerator()
	{
		return itsGenerator;
	}
	
	public void setGenerator(Object aGenerator)
	{
		assert itsGenerator == null;
		itsGenerator = aGenerator;
	}
	
	public void addClass(String aName, byte[] aBytecode)
	{
		itsClassesMap.put(aName, aBytecode);
	}

	private boolean shouldLoadSourceClass(String aName)
	{
		return aName.startsWith("tod.impl.replay2.") 
			&& ! aName.equals(getClass().getName())
			&& ! aName.equals(TmpIdManager.class.getName())
			&& ! aName.equals(LocalsSnapshot.class.getName())
			&& ! aName.equals(EventCollector.class.getName());
	}
	
	private byte[] createReplayerClass(int aBehaviorId)
	{
		
	}
	
	@Override
	public Class loadClass(String aName) throws ClassNotFoundException
	{
		byte[] theBytecode;
		if (aName.startsWith(MethodReplayerGenerator.REPLAY_CLASS_PREFIX))
		{
			int id = Integer.parseInt(aName.substring(MethodReplayerGenerator.REPLAY_CLASS_PREFIX.length()));
			theBytecode = createReplayerClass(id);
		}
		else
		{
			theBytecode = itsClassesMap.remove(aName);
			if (theBytecode == null && shouldLoadSourceClass(aName)) theBytecode = getSourceClassBytecode(aName.replace('.', '/'));
		}
		
		if (theBytecode != null) 
		{
			return super.defineClass(aName, theBytecode, 0, theBytecode.length);
		}
		else 
		{
			return itsParent.loadClass(aName);
		}
	}
	
	public static byte[] getSourceClassBytecode(String aClassName)
	{
		try
		{
			InputStream theStream = ReplayerLoader.class.getResourceAsStream("/"+aClassName+".class");
			return Utils.readInputStream_byte(theStream);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private ClassNode getSourceClassNode(String aClassName)
	{
		try
		{
			InputStream theStream = getClass().getResourceAsStream("/"+aClassName+".class");
			ClassNode theClassNode = new ClassNode();
			ClassReader theReader = new ClassReader(theStream);
			theReader.accept(theClassNode, 0);
			
			return theClassNode;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	

	
	/**
	 * Modifies the base frame classes to add all possible invokeXxxx(...) methods
	 */
	private void modifyBaseClasses(IStructureDatabase aDatabase)
	{
		Set<MethodDescriptor> theUsedDescriptors = getUsedDescriptors(aDatabase);
		modifyThreadReplayer(theUsedDescriptors);
	}
	
	private MethodNode createNode(MethodDescriptor aDescriptor)
	{
		MethodSignature theSignature = aDescriptor.getDispatchSignature();
		
		MethodNode theMethodNode = new MethodNode();
		theMethodNode.name = theSignature.name;
		theMethodNode.desc = theSignature.descriptor;
		theMethodNode.exceptions = Collections.EMPTY_LIST;
		theMethodNode.access = Opcodes.ACC_PUBLIC;
		theMethodNode.tryCatchBlocks = Collections.EMPTY_LIST;

		return theMethodNode;
	}
	
	private void addClass(ClassNode aClassNode)
	{
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		aClassNode.accept(theWriter);
		byte[] theBytecode = theWriter.toByteArray();
		try
		{
			File f = new File("/home/gpothier/tmp/tod/gen/"+aClassNode.name+".class");
			f.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(theBytecode);
			fos.flush();
			fos.close();
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		String theName = aClassNode.name.replace('/', '.');
		addClass(theName, theBytecode);
	}
	
	/**
	 * Creates the dispatcher and snapshot class that is used to dispatch method calls.
	 */
	private void modifyThreadReplayer(Set<MethodDescriptor> aUsedDescriptors)
	{
		ClassNode theClassNode = getSourceClassNode(BCIUtils.getJvmClassName(ThreadReplayer.class));
		ListMap<MethodDescriptor, IBehaviorInfo> theMap = new ListMap<MethodDescriptor, IBehaviorInfo>();
		
		// Split behaviors into groups of the same signature
		IBehaviorInfo[] theBehaviors = itsDatabase.getBehaviors();
		for (IBehaviorInfo theBehavior : theBehaviors)
		{
			// TODO: check if in scope
			MethodDescriptor theDescriptor = getDescriptor(theBehavior);
			theMap.add(theDescriptor, theBehavior);
		}

		// Create one dispatch method per group
		for(Map.Entry<MethodDescriptor, List<IBehaviorInfo>> theEntry : theMap.entrySet())
		{
			MethodNode theMethod  = createDispatcher(theEntry.getKey(), theEntry.getValue());
			theClassNode.methods.add(theMethod);
		}
		
		// Create snapshot methods
		for (String theSignature : itsDatabase.getRegisteredSnapshotSignatures())
		{
			modifyThreadReplayer_addSnapshotMethod(theClassNode, theSignature);
		}

	}
	
	private MethodNode createDispatcher(MethodDescriptor aDescriptor, List<IBehaviorInfo> aBehaviors)
	{
		MethodSignature theDispatchSignature = aDescriptor.getDispatchSignature();
		
		MethodNode theMethod = new MethodNode();
		theMethod.name = theDispatchSignature.name;
		theMethod.desc = theDispatchSignature.descriptor;
		theMethod.exceptions = Collections.EMPTY_LIST;
		theMethod.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
		theMethod.tryCatchBlocks = Collections.EMPTY_LIST;

		SList s = new SList();
		
		int n = aBehaviors.size();
		
		Label[] theLabels = new Label[n];
		int[] theKeys = new int[n];
		for(int i=0;i<n;i++) theLabels[i] = new Label();
		
		Label lDefault = new Label();
		
		// Push args
		s.ALOAD(0); // ThreadReplayer (this)
		
		int theSlot = 2; // Slot 1 is behavior id
		if (! aDescriptor.isStatic()) s.ALOAD(theSlot++);
		for(int theSort : aDescriptor.getArgSorts())
		{
			Type theType = getType(theSort, TYPE_OBJECTID);
			s.ILOAD(theType, theSlot);
			theSlot += theType.getSize();
		}
		
		// Create switch
		s.ILOAD(1); // Behavior id
		s.LOOKUPSWITCH(lDefault, theKeys, theLabels);
		
		for(int i=0;i<n;i++)
		{
			IBehaviorInfo theBehavior = aBehaviors.get(i);
			theLabels[i] = new Label();
			theKeys[i] = theBehavior.getId();
			
			s.label(theLabels[i]);
			String theDescriptor = theBehavior.getDescriptor();
			Type theReturnType = Type.getReturnType(theDescriptor);
			MethodSignature theReplaySignature = 
				MethodReplayerGenerator.getReplayMethodSignature(theBehavior);
			s.INVOKESTATIC(MethodReplayerGenerator.REPLAY_CLASS_PREFIX + theBehavior.getId(), theReplaySignature.name, theReplaySignature.descriptor);
			
			s.IRETURN(theReturnType);
		}
		
		s.label(lDefault);
		s.ILOAD(1);
		s.createRTExArg("Bad method id");
		s.ATHROW();
		
		theMethod.instructions = s;
		return theMethod;
	}

	private void modifyThreadReplayer_addSnapshotMethod(ClassNode aClassNode, String aSnapshotSig)
	{
		Type[] theSignature = new Type[aSnapshotSig.length()];
		for(int i=0;i<theSignature.length;i++) theSignature[i] = BCIUtils.getTypeForSig(aSnapshotSig.charAt(i));
		MethodNode theMethod = createSnapshotMethod(aClassNode, theSignature);
		aClassNode.methods.add(theMethod);
	}
	
	private MethodNode createSnapshotMethod(ClassNode aClassNode, Type[] aSnapshotSignature)
	{
		MethodNode theMethodNode = new MethodNode();
		theMethodNode.name = ReplayerGenerator.SNAPSHOT_METHOD_NAME;
		theMethodNode.maxLocals = 3;

		LocalsMapInfo theLocalsMapInfo = new LocalsMapInfo();
		List<Type> theArgTypes = new ArrayList<Type>();
		theArgTypes.add(Type.INT_TYPE); // Current snapshot seq 
		theArgTypes.add(Type.INT_TYPE); // Probe id
		for (Type theType : aSnapshotSignature) 
		{
			theMethodNode.maxLocals += theType.getSize();
			theArgTypes.add(theType);
			theLocalsMapInfo.add(theType);
		}
		
		theMethodNode.desc = Type.getMethodDescriptor(Type.INT_TYPE, theArgTypes.toArray(new Type[theArgTypes.size()]));
		theMethodNode.exceptions = Collections.EMPTY_LIST;
		theMethodNode.access = Opcodes.ACC_PUBLIC;
		theMethodNode.tryCatchBlocks = Collections.EMPTY_LIST;
		
		int vSnapshotSeq = theMethodNode.maxLocals++;
		int vSnapshot = theMethodNode.maxLocals++;

		Label lNoSnapshot = new Label();
		SList s = new SList();
		
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getSnapshotSeq", "()I");
		s.DUP();
		s.ISTORE(vSnapshotSeq);
		s.ILOAD(1); // Passed seq
		s.IF_ICMPLE(lNoSnapshot);
		
		// Perform snapshot
		s.ALOAD(0);
		s.ILOAD(2);
		s.pushInt(theLocalsMapInfo.intValuesCount);
		s.pushInt(theLocalsMapInfo.longValuesCount);
		s.pushInt(theLocalsMapInfo.floatValuesCount);
		s.pushInt(theLocalsMapInfo.doubleValuesCount);
		s.pushInt(theLocalsMapInfo.refValuesCount);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "createSnapshot", "(IIIIII)"+DSC_LOCALSSNAPSHOT);
		s.ASTORE(vSnapshot);
		
		int theSlot = 3;
		for (Type theType : aSnapshotSignature) 
		{
			s.ALOAD(vSnapshot);
			s.ILOAD(theType, theSlot);
			invokeSnapshotPush(s, theType);
			theSlot += theType.getSize();
		}
		
		s.ALOAD(0);
		s.ALOAD(vSnapshot);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "registerSnapshot", "("+DSC_LOCALSSNAPSHOT+")V");
		
		s.label(lNoSnapshot);
		s.ILOAD(vSnapshotSeq);
		s.IRETURN();
		
		theMethodNode.instructions = s;
		
		return theMethodNode;
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
	
	private static MethodDescriptor getDescriptor(IBehaviorInfo aBehavior)
	{
		String theDescriptor = aBehavior.getDescriptor();
		Type theReturnType = Type.getReturnType(theDescriptor);
		Type[] theArgumentTypes = Type.getArgumentTypes(theDescriptor);
		return new MethodDescriptor(aBehavior, theReturnType, theArgumentTypes);
	}
	
	/**
	 * Returns a set of all descriptors that are used by methods in the given database
	 */
	private Set<MethodDescriptor> getUsedDescriptors(IStructureDatabase aDatabase)
	{
		Set<MethodDescriptor> theResult = new HashSet<MethodDescriptor>();
		IBehaviorInfo[] theBehaviors = aDatabase.getBehaviors();
		for (IBehaviorInfo theBehavior : theBehaviors) theResult.add(getDescriptor(theBehavior));
		
		return theResult;
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
		
		public void add(Type aType)
		{
			switch(aType.getSort())
			{
			case Type.ARRAY:
			case Type.OBJECT:
				refValuesCount++;
				break;
				
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.INT:
			case Type.SHORT:
				intValuesCount++;
				break;
				
			case Type.DOUBLE:
				doubleValuesCount++;
				break;
				
			case Type.FLOAT:
				floatValuesCount++;
				break;
				
			case Type.LONG:
				longValuesCount++;
				break;
				
			default:
				throw new RuntimeException("Not handled: "+aType);	
			}
		}
		

	}


	/**
	 * Represents a method descriptor (argument types).
	 * Properly implements equals and hashCode.
	 * NOTE: the class is public because of the classloading.
	 * @author gpothier
	 */
	public static class MethodDescriptor
	{
		private final IBehaviorInfo itsBehavior;
		private final byte[] itsArgSorts;
		private final byte itsReturnSort;
		private MethodSignature itsSignature;
		
		public MethodDescriptor(IBehaviorInfo aBehavior, Type aReturnType, Type... aArgTypes)
		{
			itsBehavior = aBehavior;
			itsReturnSort = getSort(aReturnType);
			
			itsArgSorts = new byte[aArgTypes.length];
			for(int i=0;i<aArgTypes.length;i++) itsArgSorts[i] = getSort(aArgTypes[i]);
		}
		
		public boolean isStatic()
		{
			return itsBehavior.isStatic();
		}
		
		public byte getReturnSort()
		{
			return itsReturnSort;
		}
		
		public byte[] getArgSorts()
		{
			return itsArgSorts;
		}
		
		private static byte getSort(Type aType)
		{
//			int theSort = BCIUtils.getActualReplayType(aType).getSort();
			int theSort = aType.getSort();
			assert theSort <= Byte.MAX_VALUE;
			return (byte) theSort;
		}
		
		/**
		 * Returns the signature for the dispatcher method corresponding to this descriptor.
		 */
		public MethodSignature getDispatchSignature()
		{
			if (itsSignature == null)
			{
				itsSignature = MethodReplayerGenerator.getDispatchMethodSignature(itsBehavior);
			}
			return itsSignature;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(itsArgSorts);
			result = prime * result + itsReturnSort;
			result = prime * result + (isStatic() ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MethodDescriptor other = (MethodDescriptor) obj;
			if (!Arrays.equals(itsArgSorts, other.itsArgSorts)) return false;
			if (itsReturnSort != other.itsReturnSort) return false;
			if (isStatic() != other.isStatic()) return false;
			return true;
		}


		@Override
		public String toString()
		{
			MethodSignature theSignature = getDispatchSignature();
			return theSignature.name+theSignature.descriptor;
		}
	}


}