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

import static tod.impl.bci.asm2.BCIUtils.CLS_INSCOPEREPLAYERFRAME;
import static tod.impl.bci.asm2.BCIUtils.CLS_LOCALSSNAPSHOT;
import static tod.impl.bci.asm2.BCIUtils.DSC_LOCALSSNAPSHOT;
import static tod.impl.bci.asm2.BCIUtils.DSC_OBJECTID;
import static tod.impl.bci.asm2.BCIUtils.TYPE_OBJECTID;

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
import tod.impl.server.BufferStream;
import zz.utils.Utils;

public class ReplayerLoader extends ClassLoader
{
	private final ClassLoader itsParent;
	private final Map<String, byte[]> itsClassesMap = new HashMap<String, byte[]>();
	
	private Constructor its1stPassReplayerCtor;
	private Constructor itsPartialReplayerCtor;

	public ReplayerLoader(ClassLoader aParent, IStructureDatabase aDatabase)
	{
		itsParent = aParent;
		modifyBaseClasses(aDatabase);
		
		try
		{
			Class the1stPassClass = loadClass("tod.impl.replay2.ThreadReplayer_1stPass");
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
					BufferStream.class);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public Object createReplayer(
			boolean a1stPass,
			int aThreadId,
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		try
		{
			Constructor theCtor = a1stPass ? its1stPassReplayerCtor : itsPartialReplayerCtor;
			return theCtor.newInstance(this, aThreadId, aConfig, aDatabase, aCollector, aTmpIdManager, aBuffer);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void addClass(String aName, byte[] aBytecode)
	{
		itsClassesMap.put(aName, aBytecode);
	}

	private boolean shouldLoad(String aName)
	{
		return aName.startsWith("tod.impl.replay2.") 
			&& ! aName.equals(getClass().getName())
			&& ! aName.equals(TmpIdManager.class.getName())
			&& ! aName.equals(EventCollector.class.getName());
	}
	
	@Override
	public Class loadClass(String aName) throws ClassNotFoundException
	{
		byte[] theBytecode = itsClassesMap.remove(aName);
		if (theBytecode == null && shouldLoad(aName)) theBytecode = getClassBytecode(aName.replace('.', '/'));
		
		if (theBytecode != null) 
		{
			return super.defineClass(aName, theBytecode, 0, theBytecode.length);
		}
		else 
		{
			return itsParent.loadClass(aName);
		}
	}
	
	public static byte[] getClassBytecode(String aClassName)
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
	
	/**
	 * Modifies the base frame classes to add all possible invokeXxxx(...) methods
	 */
	private void modifyBaseClasses(IStructureDatabase aDatabase)
	{
		Set<MethodDescriptor> theUsedDescriptors = getUsedDescriptors(aDatabase);
		
		modifyReplayerFrame(theUsedDescriptors);
		modifyUnmonitoredReplayerFrame(theUsedDescriptors);
		modifyClassloaderWrapperReplayerFrame(theUsedDescriptors);
	}
	
	private MethodNode createNode(MethodDescriptor aDescriptor)
	{
		String[] theSignature = aDescriptor.getSignature();
		
		MethodNode theMethodNode = new MethodNode();
		theMethodNode.name = theSignature[0];
		theMethodNode.desc = theSignature[1];
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
	 * Create all methods with a body that throws {@link UnsupportedOperationException}.
	 */
	private void modifyReplayerFrame(Set<MethodDescriptor> aUsedDescriptors)
	{
		ClassNode theClassNode = getOriginalClass(BCIUtils.getJvmClassName(ReplayerFrame.class));
		
		for (MethodDescriptor theDescriptor : aUsedDescriptors)
		{
			// Filter out methods already present in source
			if (theDescriptor.itsStatic
					&& theDescriptor.itsArgSorts.length == 0 
					&& theDescriptor.itsReturnSort == Type.VOID) continue;
			
			MethodNode theMethodNode = createNode(theDescriptor);
			theMethodNode.maxStack = 0;
			theMethodNode.maxLocals = 1;
			
			SList s = new SList();
			s.createUnsupportedEx("ReplayerGenerator.modifyReplayerFrame - "+theDescriptor);
			s.ATHROW();
			
			theMethodNode.instructions = s;
			theClassNode.methods.add(theMethodNode);
		}

		addClass(theClassNode);
	}
	
	private void modifyInScopeReplayerFrame()
	{
		ClassNode theClassNode = getOriginalClass(BCIUtils.getJvmClassName(InScopeReplayerFrame.class));
		modifyInScopeReplayerFrame_addSnapshotMethods(theClassNode);
		addClass(theClassNode);
	}
	
	private void modifyInScopeReplayerFrame_addSnapshotMethods(ClassNode aClassNode)
	{
		modifyInScopeReplayerFrame_addSnapshotMethod(aClassNode, "I");
		modifyInScopeReplayerFrame_addSnapshotMethod(aClassNode, "II");
		modifyInScopeReplayerFrame_addSnapshotMethod(aClassNode, "III");
		modifyInScopeReplayerFrame_addSnapshotMethod(aClassNode, "L");
		modifyInScopeReplayerFrame_addSnapshotMethod(aClassNode, "LL");
		modifyInScopeReplayerFrame_addSnapshotMethod(aClassNode, "LLL");
	}
	
	private void modifyInScopeReplayerFrame_addSnapshotMethod(ClassNode aClassNode, String aSnapshotSig)
	{
		Type[] theSignature = new Type[aSnapshotSig.length()];
		for(int i=0;i<theSignature.length;i++) theSignature[i] = getTypeForSig(aSnapshotSig.charAt(i));
		MethodNode theMethod = createSnapshotMethod(aClassNode, theSignature);
		aClassNode.methods.add(theMethod);
	}
	
	private static Type getTypeForSig(char aSig)
	{
		switch(aSig)
		{
		case 'I': return Type.INT_TYPE;
		case 'J': return Type.LONG_TYPE;
		case 'F': return Type.FLOAT_TYPE;
		case 'D': return Type.DOUBLE_TYPE;
		case 'L': return TYPE_OBJECTID;
		default: throw new RuntimeException("Not handled: "+aSig);
		}
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
	

	private void modifyUnmonitoredReplayerFrame(Set<MethodDescriptor> aUsedDescriptors)
	{
		modifyUnmonitoredReplayerFrame(aUsedDescriptors, "tod/impl/replay2/UnmonitoredReplayerFrame");
	}
	
	private void modifyClassloaderWrapperReplayerFrame(Set<MethodDescriptor> aUsedDescriptors)
	{
		modifyUnmonitoredReplayerFrame(aUsedDescriptors, "tod/impl/replay2/ClassloaderWrapperReplayerFrame");
	}
	
	/**
	 * Override all the methods so that they all invoke {@link UnmonitoredReplayerFrame#replay()} and
	 * return the appropriate result.
	 */
	private Class modifyUnmonitoredReplayerFrame(Set<MethodDescriptor> aUsedDescriptors, String aClassName)
	{
		ClassNode theClassNode = getOriginalClass(aClassName);
		
		for (MethodDescriptor theDescriptor : aUsedDescriptors)
		{
			if (theDescriptor.itsArgSorts.length == 0 && theDescriptor.itsStatic) continue; // The methods with no args are already implemented in original source.
			MethodNode theMethodNode = createNode(theDescriptor);
			theMethodNode.maxStack = 0;
			theMethodNode.maxLocals = 1;
			
			SList s = new SList();
			s.ALOAD(0);
			s.INVOKEVIRTUAL(aClassName, "replay", "()V");
			
			s.ALOAD(0);
			switch(theDescriptor.getReturnSort())
			{
			case Type.OBJECT:
			case Type.ARRAY:
				s.GETFIELD(aClassName, "itsRefResult", TYPE_OBJECTID.getDescriptor());
				s.ARETURN();
				
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
			case Type.INT: 
				s.GETFIELD(aClassName, "itsIntResult", "I");
				s.IRETURN();
				break;
			
			case Type.LONG:
				s.GETFIELD(aClassName, "itsLongResult", "J");
				s.LRETURN();
				break;
			
			case Type.DOUBLE:
				s.GETFIELD(aClassName, "itsDoubleResult", "D");
				s.DRETURN();
				break;
				
			case Type.FLOAT:
				s.GETFIELD(aClassName, "itsFloatResult", "F");
				s.FRETURN();
				break;
				
			case Type.VOID:
				s.RETURN();
				break;

			
			default: throw new RuntimeException("Unexpected type: "+theDescriptor.getReturnSort());
			}
			
			theMethodNode.instructions = s;
			theClassNode.methods.add(theMethodNode);
		}

		addClass(theClassNode);
		try
		{
			return loadClass(aClassName.replace('/', '.'));
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns a set of all descriptors that are used by methods in the given database
	 */
	private Set<MethodDescriptor> getUsedDescriptors(IStructureDatabase aDatabase)
	{
		Set<MethodDescriptor> theResult = new HashSet<MethodDescriptor>();
		IBehaviorInfo[] theBehaviors = aDatabase.getBehaviors();
		for (IBehaviorInfo theBehavior : theBehaviors)
		{
			String theSignature = theBehavior.getSignature();
			Type theReturnType = Type.getReturnType(theSignature);
			Type[] theArgumentTypes = Type.getArgumentTypes(theSignature);
			theResult.add(new MethodDescriptor(theBehavior.isStatic(), theReturnType, theArgumentTypes));
		}
		
		return theResult;
	}
	
	private ClassNode getOriginalClass(String aClassName)
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
		private final boolean itsStatic;
		private final byte[] itsArgSorts;
		private final byte itsReturnSort;
		private String[] itsSignature;
		
		public MethodDescriptor(boolean aStatic, Type aReturnType, Type... aArgTypes)
		{
			itsStatic = aStatic;
			itsReturnSort = getSort(aReturnType);
			
			itsArgSorts = new byte[aArgTypes.length];
			for(int i=0;i<aArgTypes.length;i++) itsArgSorts[i] = getSort(aArgTypes[i]);
		}
		
		public byte getReturnSort()
		{
			return itsReturnSort;
		}
		
		private static byte getSort(Type aType)
		{
			int theSort = MethodReplayerGenerator.getActualType(aType).getSort();
			assert theSort <= Byte.MAX_VALUE;
			return (byte) theSort;
		}
		
		/**
		 * Returns the signature for the generated method corresponding to this descriptor.
		 */
		public String[] getSignature()
		{
			if (itsSignature == null)
			{
				Type theReturnType = MethodReplayerGenerator.ACTUALTYPE_FOR_SORT[itsReturnSort];
				Type[] theArgTypes = new Type[itsArgSorts.length];
				for(int i=0;i<theArgTypes.length;i++) theArgTypes[i] = 
					MethodReplayerGenerator.ACTUALTYPE_FOR_SORT[itsArgSorts[i]];
				itsSignature = MethodReplayerGenerator.getInvokeMethodSignature(itsStatic, theArgTypes, theReturnType);
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
			result = prime * result + (itsStatic ? 1231 : 1237);
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
			if (itsStatic != other.itsStatic) return false;
			return true;
		}


		@Override
		public String toString()
		{
			String[] theSignature = getSignature();
			return theSignature[0]+theSignature[1];
		}
	}


}