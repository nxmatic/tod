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
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod2.access.TODAccessor;
import tod2.agent.Message;
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
	
	private static final String CLSID_FIELD = "$tod$clsId";
	private static final String CLSID_GETTER = "$tod$getClsId";
	
	private static final String STRING_GETCHARS = "$tod$getChars";
	private static final String STRING_GETOFFSET = "$tod$getOffset";
	private static final String STRING_GETCOUNT = "$tod$getCount";
	
	
	private final ASMInstrumenter2 itsInstrumenter;
	private final String itsName;
	private final byte[] itsOriginal;
	private final ClassNode itsNode;
	private final boolean itsUseJava14;
	
	private boolean itsInterface;
	private IMutableClassInfo itsClassInfo;
	private IClassInfo itsSuperclass;
	private IClassInfo[] itsInterfaces;
	
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
		
		IClassInfo[] theInterfaces = new IClassInfo[itsNode.interfaces != null ? itsNode.interfaces.size() : 0];
		if (itsNode.interfaces != null) for (int i = 0; i < itsNode.interfaces.size(); i++)
		{
			String theInterface = (String) itsNode.interfaces.get(i);
			theInterfaces[i] = getDatabase().getNewClass(Util.jvmToScreen(theInterface));
		}
		
		itsClassInfo.setup(
				itsInterface, 
				getDatabase().isInScope(itsName), 
				Utils.md5String(aBytecode), 
				theInterfaces, 
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
		
		if (BCIUtils.CLS_OBJECT.equals(getNode().name)) addGetIdMethod_Root();
		if (BCIUtils.CLS_CLASS.equals(getNode().name)) addGetClsIdMethod();
		if (BCIUtils.CLS_STRING.equals(getNode().name)) addStringRawAccess();
		
		// Output the modified class
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		itsNode.accept(theWriter);
		
		byte[] theBytecode = theWriter.toByteArray();

		try
		{
			BCIUtils.checkClass(theBytecode);
			for(MethodNode theNode : (List<MethodNode>) itsNode.methods) BCIUtils.checkMethod(getNode(), theNode);
		}
		catch(Exception e)
		{
			BCIUtils.writeClass(getInstrumenter().getConfig().get(TODConfig.CLASS_CACHE_PATH)+"/err", getNode(), theBytecode);
			System.err.println("Class "+getNode().name+" failed check. Writing out bytecode.");
			e.printStackTrace();
		}
		
		itsClassInfo.setBytecode(theBytecode, itsOriginal);
		
		return new InstrumentedClass(
				theBytecode, 
				getInstrumenter().getModeChangesAndReset());
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
		
		// Ensure all the fields are added
		for(FieldNode theNode : (List<FieldNode>) itsNode.fields) 
		{
			ITypeInfo theType = getDatabase().getNewType(theNode.desc);
			itsClassInfo.getNewField(theNode.name, theType, BCIUtils.isStatic(theNode.access));
		}

		// Add infrastructure
		if (! itsInterface
				&& BCIUtils.CLS_OBJECT.equals(getNode().superName) 
				&& getDatabase().isInIdScope(getNode().name)) 
		{
			addGetIdMethod_InScope();
		}
	}
	
	private void processMethod(MethodNode aNode)
	{
		IMutableBehaviorInfo theBehavior = itsClassInfo.getNewBehavior(
				aNode.name,
				aNode.desc, 
				BCIUtils.isStatic(aNode.access));
		
		if (BCIUtils.CLS_CLASSLOADER.equals(getNode().name) && "loadClassInternal".equals(aNode.name)) 
		{
			new MethodInstrumenter_loadClassInternal(this, aNode, theBehavior).proceed();
		}
		else if (getDatabase().isInScope(itsName)) new MethodInstrumenter_InScope(this, aNode, theBehavior).proceed();
		else new MethodInstrumenter_OutOfScope(this, aNode, theBehavior).proceed();
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
		s.IRETURN(theType);
		
		aNode.instructions = s;
		aNode.maxStack = theType.getSize();
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
	


	
	private MethodNode createMethod(String aName, String aDesc, int aAccess)
	{
		MethodNode theNode = new MethodNode();
		getNode().methods.add(theNode);
		theNode.access = aAccess;
		theNode.name = aName;
		theNode.desc = aDesc;
		theNode.tryCatchBlocks = Collections.EMPTY_LIST;
		theNode.exceptions = Collections.EMPTY_LIST;
		
		return theNode;
	}
}
