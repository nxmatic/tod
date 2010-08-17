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
package tod.impl.bci.asm2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import tod.Util;
import tod.core.bci.IInstrumenter.InstrumentedClass;
import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.utils.GrowingByteBuffer;
import tod2.access.TODAccessor;
import zz.utils.Utils;

/**
 * Handles the instrumentation of a single class
 * @author gpothier
 */
public class ClassInstrumenter
{
	/**
	 * Enables a few safety checks for debugging the instrumentation 
	 */
	private static final boolean ENABLE_CHECKS = true;
	
	private static final String TODACCESSOR_CLASSNAME = TODAccessor.class.getName().replace('.', '/');
	
	private static final String OBJID_FIELD = "$tod$id";
	private static final String OBJID_GETTER = "$tod$getId";
	public static final String OBJID_RESET = "$tod$resetId";
	
	public static final String BOOTSTRAP_FLAG = "$tod$bootstrap";
	
	private static final String CLSID_FIELD = "$tod$clsId";
	private static final String CLSID_GETTER = "$tod$getClsId";
	
	private static final String STRING_GETCHARS = "$tod$getChars";
	private static final String STRING_GETOFFSET = "$tod$getOffset";
	private static final String STRING_GETCOUNT = "$tod$getCount";
	
	private static final String THREAD_THREADDATAFIELD = "$tod$threadData";
	private static final String THREAD_GETID = "$tod$getId";
	private static final String THREAD_GETNAME = "$tod$getName";
	
	private static final String NATIVE_METHOD_PREFIX = "$todwrap$";
	
	private final ASMInstrumenter2 itsInstrumenter;
	private final String itsName;
	private final byte[] itsOriginal;
	private final ClassNode itsNode;
	private final boolean itsUseJava14;
	
	private boolean itsInterface;
	private IMutableClassInfo itsClassInfo;
	private IClassInfo itsSuperclass;
	private IClassInfo[] itsInterfaces;
	
	private List<IBehaviorInfo> itsBehaviors = new ArrayList<IBehaviorInfo>();
	
	private List<MethodNode> itsExtraMethods = new ArrayList<MethodNode>();
	
	private boolean itsModified = false;
	
	public ClassInstrumenter(ASMInstrumenter2 aInstrumenter, String aName, byte[] aBytecode, boolean aUseJava14)
	{
		itsInstrumenter = aInstrumenter;
		itsName = aName;
		itsOriginal = aBytecode;
		itsUseJava14 = aUseJava14;

		itsNode = new ClassNode();
		ClassReader theReader = new ClassReader(aBytecode);
		theReader.accept(itsNode, 0);
		
		if (! itsName.equals(itsNode.name)) Utils.rtex("Internal error - %s != %s", itsName, itsNode.name);
		
		itsClassInfo = getDatabase().getNewClass(Util.jvmToScreen(aName));
		itsSuperclass = itsInterface || itsNode.superName == null ? 
				null
				: getDatabase().getNewClass(Util.jvmToScreen(itsNode.superName));
		
		itsInterfaces = new IClassInfo[itsNode.interfaces != null ? itsNode.interfaces.size() : 0];
		if (itsNode.interfaces != null) for (int i = 0; i < itsNode.interfaces.size(); i++)
		{
			String theInterface = (String) itsNode.interfaces.get(i);
			itsInterfaces[i] = getDatabase().getNewClass(Util.jvmToScreen(theInterface));
		}
		
		itsClassInfo.setup(
				itsInterface, 
				getDatabase().isInScope(itsName), 
				Utils.md5String(aBytecode), 
				itsInterfaces, 
				itsSuperclass);
	}
	
	public ASMInstrumenter2 getInstrumenter()
	{
		return itsInstrumenter;
	}
	
	private IMutableStructureDatabase getDatabase()
	{
		return itsInstrumenter.getStructureDatabase();
	}
	
	public ClassNode getNode()
	{
		return itsNode;
	}
	
	public InstrumentedClass proceed()
	{
		if (TODACCESSOR_CLASSNAME.equals(getNode().name)) processTODAccessor();
		else processNormalClass();
		
		if (BCIUtils.CLS_OBJECT.equals(getNode().name)) 
		{
			addGetIdMethod_Root();
			addResetIdMethod_Root();
			addBootstrapField();
		}
		else if (BCIUtils.CLS_CLASS.equals(getNode().name)) addGetClsIdMethod();
		else if (BCIUtils.CLS_STRING.equals(getNode().name)) addStringRawAccess();
		else if (BCIUtils.CLS_THREAD.equals(getNode().name)) addThreadAccess();
		
		// Output the modified class
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		itsNode.accept(theWriter);
		
		byte[] theBytecode = theWriter.toByteArray();

		try
		{
			BCIUtils.checkClass(theBytecode);
			for(MethodNode theNode : (List<MethodNode>) itsNode.methods) 
				if ((theNode.access & Opcodes.ACC_NATIVE) == 0) BCIUtils.checkMethod(getNode(), theNode);
		}
		catch(Exception e)
		{
			BCIUtils.writeClass(getInstrumenter().getConfig().get(TODConfig.CLASS_CACHE_PATH)+"/err", getNode(), theBytecode);
			System.err.println("Class "+getNode().name+" failed check. Writing out bytecode.");
			e.printStackTrace();
		}
				
		itsClassInfo.setBytecode(theBytecode, itsOriginal);
		
		return new InstrumentedClass(
				itsClassInfo.getId(),
				theBytecode, 
				createClassInfo());
	}
	
	private byte[] createClassInfo()
	{
		GrowingByteBuffer b = GrowingByteBuffer.allocate(1024);
		
		// Superclass
		b.putInt(itsSuperclass != null ? itsSuperclass.getId() : 0);
		
		// Interfaces
		b.putShort((short) itsInterfaces.length);
		for(IClassInfo theInterface : itsInterfaces) b.putInt(theInterface.getId());
		
		// Methods
		b.putShort((short) itsBehaviors.size());
		for(IBehaviorInfo theBehavior : itsBehaviors) 
		{
			b.putInt(theBehavior.getId());
			b.putInt(getDatabase().getBehaviorSignatureId(theBehavior));
			b.putBoolean(theBehavior.isNative() || StructureDatabase.isSkipped(itsNode.name));
			b.putBoolean(theBehavior.isStaticInit());
			b.putBoolean(getDatabase().isInScope(itsNode.name));
		}
		
		return b.toArray();
	}
	
	private void processNormalClass()
	{
		// Get classes and interfaces from the structure database
		itsInterface = BCIUtils.isInterface(itsNode.access);
		itsClassInfo = getDatabase().getNewClass(Util.jvmToScreen(itsNode.name));
		itsSuperclass = itsInterface || itsNode.superName == null ? 
				null
				: getDatabase().getNewClass(Util.jvmToScreen(itsNode.superName));
		
		IClassInfo[] theInterfaces = new IClassInfo[itsNode.interfaces != null ? itsNode.interfaces.size() : 0];
		if (itsNode.interfaces != null) for (int i = 0; i < itsNode.interfaces.size(); i++)
		{
			String theInterface = (String) itsNode.interfaces.get(i);
			theInterfaces[i] = getDatabase().getNewClass(Util.jvmToScreen(theInterface));
		}
		
		// Process each method
		for(MethodNode theNode : (List<MethodNode>) itsNode.methods) processMethod(theNode);
		
		itsNode.methods.addAll(itsExtraMethods);
		
		// Ensure all the fields are added
		for(FieldNode theNode : (List<FieldNode>) itsNode.fields) 
		{
			ITypeInfo theType = getDatabase().getNewType(theNode.desc);
			itsClassInfo.getNewField(theNode.name, theType, theNode.access);
		}

		// Add infrastructure
		if (! itsInterface
				&& BCIUtils.CLS_OBJECT.equals(getNode().superName) 
				&& getDatabase().isInIdScope(getNode().name)) 
		{
			addGetIdMethod_InScope();
			addResetIdMethod_InScope();
		}
	}
	
	private void processMethod(MethodNode aNode)
	{
		IMutableBehaviorInfo theBehavior = itsClassInfo.getNewBehavior(aNode.name, aNode.desc, aNode.access);
		itsBehaviors.add(theBehavior);
		
		if (BCIUtils.CLS_CLASSLOADER.equals(getNode().name) 
				&& ("loadClassInternal".equals(aNode.name) || "checkPackageAccess".equals(aNode.name))) 
		{
			new MethodInstrumenter_ClassLoader(this, aNode, theBehavior).proceed();
		}
		else if (getDatabase().isInScope(itsName)) 
		{
			new MethodInstrumenter_InScope(this, aNode, theBehavior).proceed();
		}
		else 
		{
			new MethodInstrumenter_OutOfScope(this, aNode, theBehavior).proceed();
		}

		if (BCIUtils.isNative(aNode.access)
				&& !BCIUtils.isPrivate(aNode.access)
				&& !BCIUtils.CLS_OBJECT.equals(getNode().name)
				&& !BCIUtils.CLS_THREAD.equals(getNode().name)
				&& !BCIUtils.CLS_THROWABLE.equals(getNode().name)
//				&& !"java/security/AccessController".equals(getNode().name)
				&& !"java/lang/ClassLoader$NativeLibrary".equals(getNode().name)) wrapNative(aNode);
	}
	
	
	/**
	 * Wraps a native method (based on JVMTI's SetNativeMethodPrefix)
	 * @param aNode
	 */
	private void wrapNative(MethodNode aNode)
	{
//		System.out.println("Wrapping native: "+itsName+"."+aNode.name+aNode.desc);
		MethodNode theWrapper = createMethod(itsExtraMethods, aNode.name, aNode.desc, aNode.access & ~Opcodes.ACC_NATIVE);
		
		aNode.name = NATIVE_METHOD_PREFIX + aNode.name;
		aNode.access = (aNode.access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE;
		
		boolean theStatic = BCIUtils.isStatic(aNode.access);
		
		SyntaxInsnList s = new SyntaxInsnList();
		
		// Call original native method
		Type[] theTypes = Type.getArgumentTypes(aNode.desc);
		int theIndex = 0;
		if (! theStatic) s.ALOAD(theIndex++);
		for (int i = 0; i < theTypes.length; i++)
		{
			Type theType = theTypes[i];
			s.ILOAD(theType, theIndex);
			theIndex += theType.getSize();
		}
		
		if (theStatic) s.INVOKESTATIC(itsNode.name, aNode.name, aNode.desc);
		else s.INVOKESPECIAL(itsNode.name, aNode.name, aNode.desc);
		
		s.IRETURN(Type.getReturnType(aNode.desc));
		
		theWrapper.maxLocals = theIndex;
		theWrapper.maxStack = theIndex + 2; // +2 to account for the return value, not optimal but easy...
		theWrapper.instructions = s;
		
		// Insert enveloppe instrumentation
//		IMutableBehaviorInfo theBehavior = itsClassInfo.getNewBehavior(theWrapper.name, theWrapper.desc, theWrapper.access);
//		new MethodInstrumenter_OutOfScope(this, aNode, theBehavior).proceed();
	}

	/**
	 * Replaces the body of {@link TODAccessor#getId(Object)} so that
	 * it calls the generated Object.$tod$getId method
	 */
	private void processTODAccessor()
	{
		for(MethodNode theNode : (List<MethodNode>) itsNode.methods) 
		{
			if ("getObjectId".equals(theNode.name))
				makeAccessor(theNode, BCIUtils.CLS_OBJECT, OBJID_GETTER, "J");
			else if ("getStringChars".equals(theNode.name))
				makeAccessor(theNode, BCIUtils.CLS_STRING, STRING_GETCHARS, "[C");
			else if ("getStringOffset".equals(theNode.name))
				makeAccessor(theNode, BCIUtils.CLS_STRING, STRING_GETOFFSET, "I");
			else if ("getStringCount".equals(theNode.name))
				makeAccessor(theNode, BCIUtils.CLS_STRING, STRING_GETCOUNT, "I");
			else if ("getClassId".equals(theNode.name))
				makeAccessor(theNode, BCIUtils.CLS_CLASS, CLSID_GETTER, "I");
			else if ("getThreadData".equals(theNode.name))
				makeGetter(theNode, BCIUtils.CLS_THREAD, THREAD_THREADDATAFIELD, BCIUtils.DSC_THREADDATA);
			else if ("setThreadData".equals(theNode.name))
				makeSetThreadData(theNode);
			else if ("getThreadName".equals(theNode.name))
				makeAccessor(theNode, BCIUtils.CLS_THREAD, THREAD_GETNAME, "[C");
			else if ("getThreadId".equals(theNode.name))
				makeAccessor(theNode, BCIUtils.CLS_THREAD, THREAD_GETID, "J");
			else if ("setBootstrapFlag".equals(theNode.name))
				makeSetBootstrapFlag(theNode);
			else if ("getBootstrapFlag".equals(theNode.name))
				makeGetBootstrapFlag(theNode);
		}
	}
	
	/**
	 * Transforms the given method into a delegate accessor that calls the given
	 * method on the object passed as a parameter.
	 */
	private void makeAccessor(MethodNode aNode, String aOwner, String aName, String aDescriptor)
	{
		Type theType = Type.getType(aDescriptor);
		
		SyntaxInsnList s = new SyntaxInsnList();
		s.ALOAD(0);
		s.INVOKEVIRTUAL(aOwner, aName, "()"+theType.getDescriptor());
		s.RETURN(theType);
		
		aNode.instructions = s;
		aNode.maxStack = theType.getSize();
	}
	
	private void makeGetter(MethodNode aNode, String aOwner, String aName, String aDescriptor)
	{
		Type theType = Type.getType(aDescriptor);
		
		SyntaxInsnList s = new SyntaxInsnList();
		s.ALOAD(0);
		s.GETFIELD(aOwner, aName, aDescriptor);
		s.RETURN(theType);
		
		aNode.instructions = s;
		aNode.maxStack = theType.getSize();
	}
	
	private void makeSetThreadData(MethodNode aNode)
	{
		Type theType = Type.getType(BCIUtils.DSC_THREADDATA);
		
		SyntaxInsnList s = new SyntaxInsnList();
		s.ALOAD(0);
		s.ALOAD(1);
		s.PUTFIELD(BCIUtils.CLS_THREAD, THREAD_THREADDATAFIELD, BCIUtils.DSC_THREADDATA);
		s.RETURN();
		
		aNode.instructions = s;
		aNode.maxStack = theType.getSize()*2;
	}
	
	private void makeSetBootstrapFlag(MethodNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		s.ILOAD(0);
		s.PUTSTATIC(BCIUtils.CLS_OBJECT, BOOTSTRAP_FLAG, "Z");
		s.RETURN();
		
		aNode.instructions = s;
		aNode.maxStack = 1;
	}
	
	private void makeGetBootstrapFlag(MethodNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		s.GETSTATIC(BCIUtils.CLS_OBJECT, BOOTSTRAP_FLAG, "Z");
		s.IRETURN();
		
		aNode.instructions = s;
		aNode.maxStack = 1;
	}
	
	/**
	 * Adds the $tod$getId method to java.lang.Object
	 */
	private void addGetIdMethod_Root()
	{
		MethodNode theGetter = createMethod(OBJID_GETTER, "()J", Opcodes.ACC_PUBLIC);
		theGetter.maxStack = 2;
		theGetter.maxLocals = 1;
		
		SyntaxInsnList s = new SyntaxInsnList();
		
		s.ALOAD(0);
		s.INVOKESTATIC("java/tod/ObjectIdentity", "get", "("+BCIUtils.DSC_OBJECT+")J");
		s.LRETURN();
		
		theGetter.instructions = s;
	}
	
	/**
	 * Adds the $tod$resetId method to java.lang.Object
	 */
	private void addResetIdMethod_Root()
	{
		MethodNode theGetter = createMethod(OBJID_RESET, "()V", Opcodes.ACC_PUBLIC);
		theGetter.maxStack = 0;
		theGetter.maxLocals = 1;
		
		SyntaxInsnList s = new SyntaxInsnList();
		s.RETURN();
		theGetter.instructions = s;
	}
	
	private void addBootstrapField()
	{
		FieldNode theField = new FieldNode(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, BOOTSTRAP_FLAG, "Z", null, false);
		itsNode.fields.add(theField);
	}
	
	/**
	 * Overrides the $tod$getId method for objects that are in id scope 
	 * @see {@link TODConfig#SCOPE_ID_FILTER}
	 */
	private void addGetIdMethod_InScope()
	{
		// Add field
		getNode().fields.add(new FieldNode(
				Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT | Opcodes.ACC_VOLATILE, 
				OBJID_FIELD, 
				"J", 
				null, 
				null));
		
		// Add getter
		MethodNode theGetter = createMethod(
				OBJID_GETTER, 
				"()J", 
				Opcodes.ACC_PUBLIC);
		
		theGetter.maxStack = 6;
		theGetter.maxLocals = 1;
		
		SyntaxInsnList s = new SyntaxInsnList();
		Label lReturn = new Label();
		Label lUnlock = new Label();
		
		s.ALOAD(0);
		s.GETFIELD(getNode().name, OBJID_FIELD, "J");
		s.DUP2();
		s.pushLong(0);
		s.LCMP();
		s.IFNE(lReturn);
		
		// Doesn't have an id
		s.POP2();
		
		// Double-checked locking (this works under Java5 if the field is volatile)
		s.GETSTATIC("java/tod/ObjectIdentity", "MON", BCIUtils.DSC_OBJECT);
		s.MONITORENTER();
		
		s.ALOAD(0);
		s.GETFIELD(getNode().name, OBJID_FIELD, "J");
		s.DUP2();
		s.pushLong(0);
		s.LCMP();
		s.IFNE(lUnlock);
		
		// Still doesn't have an id
		s.POP2();
		
		s.ALOAD(0);
		s.INVOKESTATIC("java/tod/ObjectIdentity", "nextId", "()J");
		s.DUP2_X1();
		s.PUTFIELD(getNode().name, OBJID_FIELD, "J");
		
		s.LNEG();
		
		s.label(lUnlock);
		
		s.GETSTATIC("java/tod/ObjectIdentity", "MON", BCIUtils.DSC_OBJECT);
		s.MONITOREXIT();
		
		s.label(lReturn);
		s.LRETURN();

		theGetter.instructions = s;
	}
	
	/**
	 * Overrides the $tod$resetId method for objects that are in id scope 
	 * @see {@link TODConfig#SCOPE_ID_FILTER}
	 */
	private void addResetIdMethod_InScope()
	{
		MethodNode theMethod = createMethod(
				OBJID_RESET, 
				"()V", 
				Opcodes.ACC_PUBLIC);
		
		theMethod.maxStack = 3;
		theMethod.maxLocals = 1;
		
		SyntaxInsnList s = new SyntaxInsnList();
		
		s.ALOAD(0);
		s.pushLong(0);
		s.PUTFIELD(getNode().name, OBJID_FIELD, "J");
		s.RETURN();
		
		theMethod.instructions = s;
	}
	
	/**
	 * Creates the $tod$getClsId method. 
	 */
	private void addGetClsIdMethod()
	{
		// Add field
		getNode().fields.add(new FieldNode(
				Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT | Opcodes.ACC_VOLATILE, 
				CLSID_FIELD, 
				"I", 
				null, 
				null));
		
		// Add getter
		MethodNode theGetter = createMethod(
				CLSID_GETTER, 
				"()I", 
				Opcodes.ACC_PUBLIC);
		
		theGetter.maxStack = 6;
		theGetter.maxLocals = 1;
		
		SyntaxInsnList s = new SyntaxInsnList();
		Label lReturn = new Label();
		Label lUnlock = new Label();
		
		s.ALOAD(0);
		s.GETFIELD(getNode().name, CLSID_FIELD, "I");
		s.DUP();
		s.IFfalse(lReturn);
		
		// Doesn't have an id
		s.POP();
		
		// Double-checked locking (this works under Java5 if the field is volatile)
		s.GETSTATIC("java/tod/ObjectIdentity", "MON", BCIUtils.DSC_OBJECT);
		s.MONITORENTER();
		
		s.ALOAD(0);
		s.GETFIELD(getNode().name, CLSID_FIELD, "I");
		s.DUP();
		s.IFfalse(lUnlock);
		
		// Still doesn't have an id
		s.POP();
		
		s.ALOAD(0);
		s.INVOKESTATIC("java/tod/ObjectIdentity", "nextClassId", "()I");
		s.DUP_X1();
		s.PUTFIELD(getNode().name, CLSID_FIELD, "I");
		
		s.INEG();
		
		s.label(lUnlock);
		
		s.GETSTATIC("java/tod/ObjectIdentity", "MON", BCIUtils.DSC_OBJECT);
		s.MONITOREXIT();
		
		s.label(lReturn);
		s.IRETURN();
		
		theGetter.instructions = s;
	}
	
	/**
	 * Adds the raw access methods to java.lang.String
	 */
	private void addStringRawAccess()
	{
		createGetter(BCIUtils.CLS_STRING, STRING_GETCHARS, "value", "[C");
		createGetter(BCIUtils.CLS_STRING, STRING_GETOFFSET, "offset", "I");
		createGetter(BCIUtils.CLS_STRING, STRING_GETCOUNT, "count", "I");
	}
	
	private void createGetter(String aOwner, String aGetterName, String aFieldName, String aValueDesc)
	{
		Type theType = Type.getType(aValueDesc);
		
		MethodNode theGetter = createMethod(aGetterName, "()"+aValueDesc, Opcodes.ACC_PUBLIC);
		theGetter.maxStack = theType.getSize();
		theGetter.maxLocals = 1;
		
		SyntaxInsnList s = new SyntaxInsnList();
		
		s.ALOAD(0);
		s.GETFIELD(aOwner, aFieldName, aValueDesc);
		s.IRETURN(theType);
		
		theGetter.instructions = s;
	}
	
	private void addThreadAccess()
	{
		// Add field for ThreadData access
		getNode().fields.add(new FieldNode(
				Opcodes.ACC_PUBLIC | Opcodes.ACC_TRANSIENT, 
				THREAD_THREADDATAFIELD, 
				BCIUtils.DSC_THREADDATA, 
				null, 
				null));
		
		
		// Add accessor for id access
		createGetter(BCIUtils.CLS_THREAD, THREAD_GETID, "tid", "J");
		createGetter(BCIUtils.CLS_THREAD, THREAD_GETNAME, "name", "[C");
	}
	
	private MethodNode createMethod(String aName, String aDesc, int aAccess)
	{
		return createMethod(getNode().methods, aName, aDesc, aAccess);
	}
	
	private MethodNode createMethod(List<MethodNode> aTarget, String aName, String aDesc, int aAccess)
	{
		MethodNode theNode = new MethodNode();
		aTarget.add(theNode);
		theNode.access = aAccess;
		theNode.name = aName;
		theNode.desc = aDesc;
		theNode.tryCatchBlocks = Collections.EMPTY_LIST;
		theNode.exceptions = Collections.EMPTY_LIST;
		
		return theNode;
	}
}
