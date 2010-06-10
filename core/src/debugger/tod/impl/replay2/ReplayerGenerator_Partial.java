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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.bci.asm2.BCIUtils;
import zz.utils.Utils;

public class ReplayerGenerator_Partial extends ReplayerGenerator
{
	private final LocalsSnapshot itsSnapshot;
	
	private boolean itsGeneratingInitialFrame = false;
	
	/**
	 * The cached replayer class factories, indexed by behavior id.
	 */
	private List<InScopeReplayerFrame.Factory> itsInitialFrameFactories = new ArrayList<InScopeReplayerFrame.Factory>();


	public ReplayerGenerator_Partial(
			ReplayerLoader aLoader,
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			LocalsSnapshot aSnapshot)
	{
		super(aLoader, aConfig, aDatabase);
		itsSnapshot = aSnapshot;
	}

	@Override
	protected MethodReplayerGenerator createGenerator(
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			int aBehaviorId,
			ClassNode aClassNode,
			MethodNode aMethodNode)
	{
		return new MethodReplayerGenerator_Partial(
				aConfig, 
				aDatabase, 
				this, 
				aBehaviorId, 
				aClassNode, 
				aMethodNode, 
				itsGeneratingInitialFrame ? itsSnapshot : null);
	}

	@Override
	protected String getReplayerClassName(
			String aJvmClassName,
			String aJvmMethodName,
			String aDesc)
	{
		String thePrefix = itsGeneratingInitialFrame ? "_i_" : "_p_";
		return makeReplayerClassName(thePrefix+aJvmClassName, aJvmMethodName, aDesc);
	}
	
	public InScopeReplayerFrame createInitialFrame(int aBehaviorId)
	{
		IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL) System.out.println("ReplayerGenerator.createInitialFrame(): "+theBehavior);
		InScopeReplayerFrame.Factory theFactory = getInitialReplayerFactory(aBehaviorId);
		return theFactory.create();
	}
	
	private InScopeReplayerFrame.Factory getInitialReplayerFactory(int aBehaviorId)
	{
		itsGeneratingInitialFrame = true;
		InScopeReplayerFrame.Factory theFactory = Utils.listGet(itsInitialFrameFactories, aBehaviorId);
		if (theFactory != null) return theFactory;

		// Replayer class for this behavior not found 
		// Create replayers for all the behaviors in the class.
		IClassInfo theClass = getDatabase().getBehavior(aBehaviorId, true).getDeclaringType();

		byte[] theClassBytecode = theClass.getBytecode().original;
		ClassNode theClassNode = new ClassNode();
		ClassReader theReader = new ClassReader(theClassBytecode);
		theReader.accept(theClassNode, 0);

		for (MethodNode theMethodNode : (List<MethodNode>) theClassNode.methods)
		{
			if (BCIUtils.isAbstract(theMethodNode.access) || BCIUtils.isNative(theMethodNode.access)) continue;

			IBehaviorInfo theBehavior = LocationUtils.getBehavior(getDatabase(), theClass, theMethodNode.name, theMethodNode.desc, false);
			if (theBehavior.getId() != aBehaviorId) continue;

			theFactory = createReplayerFactory(theClass, theBehavior, theClassNode, theMethodNode);
			Utils.listSet(itsInitialFrameFactories, theFactory.getBehaviorId(), theFactory);
			break;
		}
		
		itsGeneratingInitialFrame = false;
		return Utils.listGet(itsInitialFrameFactories, aBehaviorId);
	}

}