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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.server.BufferStream;

/**
 * Wraps a {@link ThreadReplayer} so as to solve the classloading issue (existing classes such
 * as {@link ReplayerFrame} are modified on the fly).
 * @author gpothier
 */
public class ReplayerWrapper
{
	private ReplayerLoader itsLoader = new ReplayerLoader(getClass().getClassLoader());
	private Object itsReplayer;
	
	public ReplayerWrapper(
			TODConfig aConfig, 
			IStructureDatabase aDatabase, 
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		try
		{
			modifyBaseClasses(aDatabase);
			
			Class theClass = itsLoader.loadClass("tod.impl.replay2.ThreadReplayer");
			Constructor theConstructor = theClass.getConstructor(
					ReplayerLoader.class, 
					TODConfig.class, 
					IStructureDatabase.class, 
					EventCollector.class,
					TmpIdManager.class, 
					BufferStream.class);
			
			itsReplayer = theConstructor.newInstance(itsLoader, aConfig, aDatabase, aCollector, aTmpIdManager, aBuffer);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}
	
	private void addClass(String aClassName)
	{
		itsLoader.addClass(aClassName.replace('/', '.'), ReplayerLoader.getClassBytecode(aClassName));
	}

	public void replay()
	{
		try
		{
			Method theMethod = itsReplayer.getClass().getMethod("replay");
			theMethod.invoke(itsReplayer);
		}
		catch (Exception e)
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
		itsLoader.addClass(theName, theBytecode);
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
			s.createUnsupportedEx("ReplayerGenerator.modifyReplayerFrame");
			s.ATHROW();
			
			theMethodNode.instructions = s;
			theClassNode.methods.add(theMethodNode);
		}

		addClass(theClassNode);
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
				s.GETFIELD(aClassName, "itsRefResult", MethodReplayerGenerator.TYPE_OBJECTID.getDescriptor());
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
			return itsLoader.loadClass(aClassName.replace('/', '.'));
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
