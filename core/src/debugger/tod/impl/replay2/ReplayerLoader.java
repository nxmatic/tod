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
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.replay2.MethodReplayerGenerator.MethodSignature;
import tod.impl.server.BufferStream;
import zz.utils.ListMap;
import zz.utils.Utils;
import zz.utils.bit.BitUtils;

public class ReplayerLoader extends ClassLoader
{
	private static final int OOSDISPATCH_BITS = 8; 
	private static final int OOSDISPATCH_N = BitUtils.pow2i(OOSDISPATCH_BITS);
	private static final int OOSDISPATCH_MASK = OOSDISPATCH_N-1;
	
	private final ClassLoader itsParent;
	private TODConfig itsConfig;
	private final IMutableStructureDatabase itsDatabase;
	private final Map<String, byte[]> itsClassesMap = new HashMap<String, byte[]>();
	
	private boolean itsFirstPass;
	private Constructor itsReplayerCtor;
	
	private Object itsGenerator;
	
	private Map<IClassInfo, ClassNode> itsClassNodeCache = new HashMap<IClassInfo, ClassNode>();

	private Set<String> itsAddedSnapshotSigs = new HashSet<String>();

	public ReplayerLoader(
			ClassLoader aParent, 
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase, 
			boolean aFirstPass)
	{
		itsParent = aParent;
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsFirstPass = aFirstPass;
		modifyBaseClasses(aDatabase);
		
		try
		{
			if (itsFirstPass)
			{
				Class the1stPassClass = loadClass("tod.impl.replay2.ThreadReplayer_FirstPass");
				itsReplayerCtor = the1stPassClass.getConstructor(
						ReplayerLoader.class, 
						int.class,
						TODConfig.class, 
						IMutableStructureDatabase.class, 
						EventCollector.class,
						TmpIdManager.class, 
						BufferStream.class);
			}
			else
			{
				Class thePartialClass = loadClass("tod.impl.replay2.ThreadReplayer_Partial");
				itsReplayerCtor = thePartialClass.getConstructor(
						ReplayerLoader.class, 
						int.class,
						TODConfig.class, 
						IMutableStructureDatabase.class, 
						EventCollector.class,
						TmpIdManager.class, 
						BufferStream.class,
						LocalsSnapshot.class);
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public Object createReplayer(
			LocalsSnapshot aSnapshot,
			int aThreadId,
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		try
		{
			if (itsFirstPass)
			{
				assert aSnapshot == null;
				return itsReplayerCtor.newInstance(this, aThreadId, itsConfig, itsDatabase, aCollector, aTmpIdManager, aBuffer);
			}
			else
			{
				assert aSnapshot != null;
				return itsReplayerCtor.newInstance(this, aThreadId, itsConfig, itsDatabase, aCollector, aTmpIdManager, aBuffer, aSnapshot);
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
			&& ! aName.equals(TmpObjectId.class.getName())
			&& ! aName.equals(EventCollector.class.getName());
	}
	
	private ClassNode getClassNode(IClassInfo aClass)
	{
		ClassNode theClassNode = itsClassNodeCache.get(aClass);
		if (theClassNode == null)
		{
			byte[] theClassBytecode = aClass.getBytecode().original;
			theClassNode = new ClassNode();
			ClassReader theReader = new ClassReader(theClassBytecode);
			theReader.accept(theClassNode, 0);
			
			itsClassNodeCache.put(aClass, theClassNode);
		}
		return theClassNode;
	}
	
	private MethodNode findMethodNode(ClassNode aClassNode, IBehaviorInfo aBehavior)
	{
		return findMethodNode(aClassNode, aBehavior.getName(), aBehavior.getDescriptor());
	}
	
	private MethodNode findMethodNode(ClassNode aClassNode, String aName, String aDescriptor)
	{
		for (MethodNode theMethodNode : (List<MethodNode>) aClassNode.methods)
		{
			if (theMethodNode.name.equals(aName)
					&& theMethodNode.desc.equals(aDescriptor)) return theMethodNode;
		}
		return null;
	}
	
	private byte[] createFirstPassReplayerClass(int aBehaviorId)
	{
		IBehaviorInfo theBehavior = itsDatabase.getBehavior(aBehaviorId, true);
		ClassNode theClassNode = getClassNode(theBehavior.getDeclaringType());
		MethodNode theMethodNode = findMethodNode(theClassNode, theBehavior);
		
		return new MethodReplayerGenerator_FirstPass(
				itsConfig, 
				itsDatabase, 
				itsDatabase.getBehavior(aBehaviorId, true), 
				theClassNode.name, 
				theMethodNode).generate();
	}
	
	private byte[] createPartialReplayerClass(int aBehaviorId, int aSnapshotProbeId)
	{
		IBehaviorInfo theBehavior = itsDatabase.getBehavior(aBehaviorId, true);
		ClassNode theClassNode = getClassNode(theBehavior.getDeclaringType());
		MethodNode theMethodNode = findMethodNode(theClassNode, theBehavior);
		
		// We can instrument the same method twice (starting point and regular), so clone it. 
		theMethodNode = BCIUtils.cloneMethod(theMethodNode); 
		
		return new MethodReplayerGenerator_Partial(
				itsConfig, 
				itsDatabase, 
				itsDatabase.getBehavior(aBehaviorId, true), 
				theClassNode.name, 
				theMethodNode,
				aSnapshotProbeId > 0 ? itsDatabase.getSnapshotProbeInfo(aSnapshotProbeId) : null).generate();
	}
	
	@Override
	public Class loadClass(String aName) throws ClassNotFoundException
	{
		byte[] theBytecode;
		if (aName.startsWith(MethodReplayerGenerator.REPLAY_CLASS_PREFIX))
		{
			String theName = aName.substring(MethodReplayerGenerator.REPLAY_CLASS_PREFIX.length());
			if (itsFirstPass)
			{
				int id = Integer.parseInt(theName);
				theBytecode = createFirstPassReplayerClass(id);
			}
			else
			{
				String[] theParts = theName.split("_");
				if (theParts.length == 1)
				{
					// Non-startup replayer
					int theBehaviorId = Integer.parseInt(theParts[0]);
					theBytecode = createPartialReplayerClass(theBehaviorId, 0);
				}
				else
				{
					// Startup replayer
					int theBehaviorId = Integer.parseInt(theParts[0]);
					int theSnapshotProbeId = Integer.parseInt(theParts[1]);
					theBytecode = createPartialReplayerClass(theBehaviorId, theSnapshotProbeId);
				}
			}
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
	
	private MethodNode createMethodNode(MethodSignature aSignature)
	{
		MethodNode theMethodNode = new MethodNode();
		theMethodNode.name = aSignature.name;
		theMethodNode.desc = aSignature.descriptor;
		theMethodNode.exceptions = Collections.EMPTY_LIST;
		theMethodNode.access = Opcodes.ACC_PUBLIC;
		theMethodNode.tryCatchBlocks = new ArrayList();

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
		
		ListMap<MethodDescriptor,IBehaviorInfo> theSignatureGroups = getDescriptorGroups();
		createInScopeDispatchMethods(theClassNode, theSignatureGroups);
		createOutOfScopeDispatchMethods(theClassNode, theSignatureGroups);
		
		// Create snapshot methods
		for (String theSignature : itsDatabase.getRegisteredSnapshotSignatures())
		{
			modifyThreadReplayer_addSnapshotMethod(theClassNode, theSignature);
			
			// Add extra signatures that takes an extra object
			// This is because of the NEW/INVOKE mechanism that stores a tmp id in a local
			modifyThreadReplayer_addSnapshotMethod(theClassNode, theSignature+"L");
			modifyThreadReplayer_addSnapshotMethod(theClassNode, theSignature+"LL");
		}
		
		addClass(theClassNode);
	}
	
	private ListMap<MethodDescriptor, IBehaviorInfo> getDescriptorGroups()
	{
		ListMap<MethodDescriptor, IBehaviorInfo> theMap = new ListMap<MethodDescriptor, IBehaviorInfo>();

		// Split behaviors into groups of the same signature
		IBehaviorInfo[] theBehaviors = itsDatabase.getBehaviors();
		for (IBehaviorInfo theBehavior : theBehaviors)
		{
			// TODO: check if in scope
			MethodDescriptor theDescriptor = getDescriptor(theBehavior);
			theMap.add(theDescriptor, theBehavior);
		}

		return theMap;
	}
	
	/**
	 * In-scope dispatch methods receive their arguments from the caller.
	 * There is one dispatch methods per distinct signature.
	 */
	private void createInScopeDispatchMethods(ClassNode aClassNode, ListMap<MethodDescriptor, IBehaviorInfo> aSignatureGroups)
	{
		// Create one dispatch method per group
		for(Map.Entry<MethodDescriptor, List<IBehaviorInfo>> theEntry : aSignatureGroups.entrySet())
		{
			MethodNode theMethod = createInScopeDispatcher(theEntry.getKey(), theEntry.getValue());
			BCIUtils.checkMethod(aClassNode, theMethod, new ReplayerVerifier(), false);
			aClassNode.methods.add(theMethod);
		}
	}
	
	/**
	 * The in-scope dispatcher is called by in-scope code.
	 * It can dispatch to either in- or out-of-scope methods.
	 */
	private MethodNode createInScopeDispatcher(MethodDescriptor aDescriptor, List<IBehaviorInfo> aBehaviors)
	{
		MethodSignature theDispatchSignature = aDescriptor.getDispatchSignature();
		byte[] theArgSorts = aDescriptor.getArgSorts();
		Type theReturnType = getType(aDescriptor.getReturnSort(), TYPE_OBJECTID);
		
		// Compute thread replayer and behavior id slots
		int theSlot = 0; 
		if (! aDescriptor.isStatic()) theSlot++;
		for(int theSort : theArgSorts)
		{
			Type theType = getType(theSort, TYPE_OBJECTID);
			theSlot += theType.getSize();
		}
		int theThreadReplayerSlot = theSlot++; 
		int theCollectorSlot = theSlot++; 
		int theBehaviorIdSlot = theSlot++;
		int theLastSlot = theSlot;
		
		MethodNode theMethod = createMethodNode(theDispatchSignature);
		theMethod.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

		Label lBegin = new Label();
		Label lEnd = new Label();
		Label lExit = new Label();
		Label lHandler = new Label();
		
		SList sw = new SList();

		int n = aBehaviors.size();
		
		Label[] theLabels = new Label[n+1]; // +1 because we need the out-of-scope case
		int[] theKeys = new int[n+1];
		for(int i=0;i<n+1;i++) theLabels[i] = new Label();
		Label lDefault = new Label();
		
		sw.label(lBegin);

		// Push args
		theSlot = 0; 
		if (! aDescriptor.isStatic()) sw.ALOAD(theSlot++);
		for(int theSort : theArgSorts)
		{
			Type theType = getType(theSort, TYPE_OBJECTID);
			sw.ILOAD(theType, theSlot);
			theSlot += theType.getSize();
		}
		sw.ALOAD(theThreadReplayerSlot); // Push the ThreadReplayer
		
		SList cases = new SList();
		{
			// Prepare cases
			for(int i=0;i<n;i++)
			{
				IBehaviorInfo theBehavior = aBehaviors.get(i);
				theLabels[i+1] = new Label();
				theKeys[i+1] = theBehavior.getId();
				
				cases.label(theLabels[i+1]);
				MethodSignature theReplaySignature = MethodReplayerGenerator.getReplayMethodSignature(theBehavior);
				cases.INVOKESTATIC(
						MethodReplayerGenerator.REPLAY_CLASS_PREFIX + theBehavior.getId(), 
						theReplaySignature.name, 
						theReplaySignature.descriptor);
				
				cases.GOTO(lExit);
			}
			
			// Add case for out of scope
			theLabels[0] = new Label();
			theKeys[0] = -1;
			cases.label(theLabels[0]);
	
			// Pop all args
			cases.POP(); // ThreadReplayer
			
			for(int i = theArgSorts.length-1;i>=0;i--)
			{
				int theSort = theArgSorts[i];
				Type theType = getType(theSort, TYPE_OBJECTID);
				cases.POP(theType);
			}
			if (! aDescriptor.isStatic()) cases.POP();
	
			cases.ALOAD(theThreadReplayerSlot);
			cases.INVOKEVIRTUAL(
					CLS_THREADREPLAYER,
					"dispatch_OOS_"+getCompleteSigForSort(theReturnType.getSort()), 
					"()"+theReturnType.getDescriptor());
			
			cases.GOTO(lExit);
			
			// Add default
			cases.label(lDefault);
			cases.ILOAD(theBehaviorIdSlot);
			cases.createRTExArg("Bad method id");
			cases.ATHROW();
		}		

		
		// Get dispatch target
		sw.ALOAD(theThreadReplayerSlot);
		sw.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getDispatchTarget", "()I");
		sw.DUP();
		sw.ISTORE(theBehaviorIdSlot);
		
		// Send event
		if (itsFirstPass)
		{
			sw.ALOAD(theThreadReplayerSlot);
			sw.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getCollector", "()"+DSC_EVENTCOLLECTOR_REPLAY);
			sw.ILOAD(theBehaviorIdSlot);
			sw.pushInt(0);
			sw.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR_REPLAY, "enter", "(II)V");
		}
		else
		{
			int theArgCount = theArgSorts.length;
			if (! aDescriptor.isStatic()) theArgCount++;
			
			sw.ALOAD(theThreadReplayerSlot);
			sw.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getCollector", "()"+DSC_EVENTCOLLECTOR_REPLAY);
			sw.DUP();
			sw.ASTORE(theCollectorSlot);
			
			sw.ILOAD(theBehaviorIdSlot);
			sw.pushInt(theArgCount);
			sw.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR_REPLAY, "enter", "(II)V");

			
			theSlot = 0; 
			if (! aDescriptor.isStatic()) 
			{
				sw.ALOAD(theCollectorSlot);
				sw.ALOAD(0);
				MethodReplayerGenerator.invokeValue(sw, TYPE_OBJECT);
				theSlot++;
			}
			for(int theSort : theArgSorts)
			{
				Type theType = getType(theSort, TYPE_OBJECTID);
				sw.ALOAD(theCollectorSlot);
				sw.ILOAD(theType, theSlot);
				MethodReplayerGenerator.invokeValue(sw, theType);
				theSlot += theType.getSize();
			}
		}

		// Create switch
		sw.LOOKUPSWITCH(lDefault, theKeys, theLabels);
		sw.add(cases);
		
		sw.label(lEnd);
		
		sw.label(lExit);
		sw.ALOAD(theThreadReplayerSlot);
		sw.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getCollector", "()"+DSC_EVENTCOLLECTOR_REPLAY);
		sw.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR_REPLAY, "exit", "()V");
		sw.IRETURN(theReturnType);

		// Catch BehaviorExitException
		sw.label(lHandler);
		sw.ALOAD(theThreadReplayerSlot);
		sw.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getCollector", "()"+DSC_EVENTCOLLECTOR_REPLAY);
		sw.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR_REPLAY, "exitException", "()V");
		sw.POP();
		sw.ALOAD(theThreadReplayerSlot);
		sw.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectException", "("+DSC_THREADREPLAYER+")V");
		sw.createRTEx("Shouldn't be reached");
		sw.ATHROW();
		
		theMethod.visitTryCatchBlock(lBegin, lEnd, lHandler, CLS_BEHAVIOREXITEXCEPTION);
		
		theMethod.instructions = sw;
		theMethod.maxLocals = theLastSlot;
		theMethod.maxStack = theLastSlot+2;
		
		return theMethod;
	}

	/**
	 * Out-of-scope dispatch methods read their arguments from the stream.
	 * They actually dispatch TO in-scope methods, but FROM OOS code.
	 */
	private void createOutOfScopeDispatchMethods(ClassNode aClassNode, ListMap<MethodDescriptor, IBehaviorInfo> aSignatureGroups)
	{
		// Create one dispatch method per group
		for(Map.Entry<MethodDescriptor, List<IBehaviorInfo>> theEntry : aSignatureGroups.entrySet())
		{
			MethodNode theMethod = createOutOfScopeDispatcher(theEntry.getKey(), theEntry.getValue());
			BCIUtils.checkMethod(aClassNode, theMethod, new ReplayerVerifier(), false);
			aClassNode.methods.add(theMethod);
		}

		IBehaviorInfo[] theBehaviors = itsDatabase.getBehaviors();
		int theLastId = theBehaviors[theBehaviors.length-1].getId();
		createHierarchicalOutOfScopeDispatcher(aClassNode, theLastId);
	}
	
	private void createHierarchicalOutOfScopeDispatcher(ClassNode aClassNode, int aLastId)
	{
		MethodNode theMethod = findMethodNode(aClassNode, "dispatch_inscope", "(I"+DSC_THREADREPLAYER+")V");
		
		SList s = new SList();
		
		int theCases = (aLastId + OOSDISPATCH_N-1) >>> OOSDISPATCH_BITS;
		Label[] theLabels = new Label[theCases];
		for(int i=0;i<theCases;i++) theLabels[i] = new Label();
		Label lDefault = new Label();
		
		// Push args
		s.ILOAD(0);
		s.ALOAD(1);
		
		// Switch
		s.ILOAD(0);
		s.pushInt(OOSDISPATCH_BITS);
		s.IUSHR();
		s.TABLESWITCH(0, theCases-1, lDefault, theLabels);
		
		for(int i=0;i<theCases;i++)
		{
			s.label(theLabels[i]);
			
			s.INVOKESTATIC(CLS_THREADREPLAYER, "dispatch_OOS_"+i, "(I"+DSC_THREADREPLAYER+")V");
			s.RETURN();
			
			aClassNode.methods.add(createLevel2OutOfScopeDispatcher(
					aClassNode, 
					i,
					Math.min(OOSDISPATCH_N, aLastId-(i*OOSDISPATCH_N))));
		}
		
		// Add default
		s.label(lDefault);
		s.ILOAD(0);
		s.createRTExArg("Bad method id");
		s.ATHROW();
		
		theMethod.instructions = s;
		theMethod.maxLocals = 2;
		theMethod.maxStack = 4;
		
		BCIUtils.checkMethod(aClassNode, theMethod, new ReplayerVerifier(), false);
	}

	private MethodNode createLevel2OutOfScopeDispatcher(ClassNode aClassNode, int aHighOrderId, int aSubcases)
	{
		MethodNode theMethod = createMethodNode(new MethodSignature("dispatch_OOS_"+aHighOrderId, "(I"+DSC_THREADREPLAYER+")V"));
		theMethod.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
		
		SList s = new SList();
		
		int theCases = aSubcases;
		Label[] theLabels = new Label[theCases];
		for(int i=0;i<theCases;i++) theLabels[i] = new Label();
		Label lDefault = new Label();
		
		// Push args
		s.ILOAD(0);
		s.ALOAD(1);
		
		// Switch
		s.ILOAD(0);
		s.pushInt(OOSDISPATCH_MASK);
		s.IAND();
		s.TABLESWITCH(0, theCases-1, lDefault, theLabels);
		
		for(int i=0;i<theCases;i++)
		{
			s.label(theLabels[i]);
			
			int theId = aHighOrderId*OOSDISPATCH_N + i;
			if (theId > 0)
			{
				IMutableBehaviorInfo theBehavior = itsDatabase.getBehavior(theId, true);
				MethodDescriptor theDescriptor = getDescriptor(theBehavior);
				MethodSignature theSignature = theDescriptor.getOOSDispatchSignature();
				
				s.INVOKESTATIC(CLS_THREADREPLAYER, theSignature.name, theSignature.descriptor);
			}

			s.RETURN();
		}
		
		// Add default
		s.label(lDefault);
		s.ILOAD(0);
		s.createRTExArg("Bad method id");
		s.ATHROW();
		
		theMethod.instructions = s;
		return theMethod;
	}
	
	/**
	 * Creates an out-of-scope dispatcher for a given method signature.
	 * Reads the arguments from the stream.
	 * The arguments of the method are (behaviod id, ThreadReplayer)
	 */
	private MethodNode createOutOfScopeDispatcher(MethodDescriptor aDescriptor, List<IBehaviorInfo> aBehaviors)
	{
		MethodSignature theDispatchSignature = aDescriptor.getOOSDispatchSignature();
		byte[] theArgSorts = aDescriptor.getArgSorts();
		Type theReturnType = getType(aDescriptor.getReturnSort(), TYPE_OBJECTID);
		
		MethodNode theMethod = createMethodNode(theDispatchSignature);
		theMethod.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

		SList sw = new SList();
		
		int n = aBehaviors.size();
		
		Label[] theLabels = new Label[n]; 
		int[] theKeys = new int[n];
		for(int i=0;i<n;i++) theLabels[i] = new Label();
		
		Label lDefault = new Label();
		
		// Push args
		int theSlot = 0;
		if (! aDescriptor.isStatic())
		{
			if (! aDescriptor.isConstructor())
			{
				sw.invokeRead(TYPE_OBJECTID, 1);
			}
			else
			{
				sw.ALOAD(1);
				sw.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "nextTmpId", "("+DSC_THREADREPLAYER+")"+BCIUtils.DSC_TMPOBJECTID);
			}
			theSlot++;
		}
		for(int theSort : theArgSorts)
		{
			Type theType = getType(theSort, TYPE_OBJECTID);
			sw.invokeRead(theType, 1);
			theSlot += theType.getSize();
		}
		sw.ALOAD(1); // Push the ThreadReplayer

		SList cases = new SList();
		{
			for(int i=0;i<n;i++)
			{
				IBehaviorInfo theBehavior = aBehaviors.get(i);
				theLabels[i] = new Label();
				theKeys[i] = theBehavior.getId();
				
				cases.label(theLabels[i]);
				MethodSignature theReplaySignature = MethodReplayerGenerator.getReplayMethodSignature(theBehavior);
				cases.INVOKESTATIC(
						MethodReplayerGenerator.REPLAY_CLASS_PREFIX + theBehavior.getId(), 
						theReplaySignature.name, 
						theReplaySignature.descriptor);
				
				cases.POP(theReturnType);
				cases.RETURN();
			}
			
			// Add default
			cases.label(lDefault);
			cases.ILOAD(0);
			cases.createRTExArg("Bad method id");
			cases.ATHROW();

		}
		// Create switch
		sw.ILOAD(0); // Push behavior id
		sw.LOOKUPSWITCH(lDefault, theKeys, theLabels);
		sw.add(cases);
		
		theMethod.instructions = sw;
		theMethod.maxLocals = 2;
		theMethod.maxStack = theSlot+3;
		
		return theMethod;
	}


	
	private void modifyThreadReplayer_addSnapshotMethod(ClassNode aClassNode, String aSnapshotSig)
	{
		if (! itsAddedSnapshotSigs.add(aSnapshotSig)) return;
		Type[] theSignature = new Type[aSnapshotSig.length()];
		for(int i=0;i<theSignature.length;i++) theSignature[i] = BCIUtils.getTypeForSig(aSnapshotSig.charAt(i));
		modifyThreadReplayer_addSnapshotMethod(aClassNode, theSignature);
	}
	
	private void modifyThreadReplayer_addSnapshotMethod(ClassNode aClassNode, Type[] aSignature)
	{
		MethodNode theMethod = createSnapshotMethod(aClassNode, aSignature);
		BCIUtils.checkMethod(aClassNode, theMethod, new ReplayerVerifier(), false);
		aClassNode.methods.add(theMethod);
	}
	
	private MethodNode createSnapshotMethod(ClassNode aClassNode, Type[] aSnapshotSignature)
	{
		MethodNode theMethodNode = new MethodNode();
		theMethodNode.name = MethodReplayerGenerator.SNAPSHOT_METHOD_NAME;
		theMethodNode.maxLocals = 3;
		theMethodNode.maxStack = 9;

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
		s.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getSnapshotSeq", "()I");
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
		s.INVOKEVIRTUAL(CLS_THREADREPLAYER, "createSnapshot", "(IIIIII)"+DSC_LOCALSSNAPSHOT);
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
		s.INVOKEVIRTUAL(CLS_THREADREPLAYER, "registerSnapshot", "("+DSC_LOCALSSNAPSHOT+")V");
		
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
		private MethodSignature itsOOSSignature;
		
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
		
		public boolean isConstructor()
		{
			return itsBehavior.isConstructor();
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
			if (theSort == Type.ARRAY) theSort = Type.OBJECT;
			assert theSort <= Byte.MAX_VALUE;
			return (byte) theSort;
		}
		
		/**
		 * Returns the signature for the dispatcher method corresponding to this descriptor.
		 */
		public MethodSignature getDispatchSignature()
		{
			if (itsSignature == null)
				itsSignature = MethodReplayerGenerator.getDispatchMethodSignature(
						itsBehavior.getDescriptor(), 
						itsBehavior.isStatic(),
						itsBehavior.isConstructor());
			return itsSignature;
		}

		/**
		 * Returns the signature for the out-of-scope dispatcher method corresponding to this descriptor.
		 */
		public MethodSignature getOOSDispatchSignature()
		{
			if (itsOOSSignature == null)
				itsOOSSignature = MethodReplayerGenerator.getOOSDispatchMethodSignature(itsBehavior);
			return itsOOSSignature;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(itsArgSorts);
			result = prime * result + itsReturnSort;
			result = prime * result + (isStatic() ? 1231 : 1237);
			result = prime * result + (isConstructor() ? 1231 : 1237);
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
			if (isConstructor() != other.isConstructor()) return false;
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