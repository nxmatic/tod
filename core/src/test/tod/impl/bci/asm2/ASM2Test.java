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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import tod.agent.MonitoringMode;
import tod.core.bci.IInstrumenter;
import tod.core.bci.IInstrumenter.BehaviorMonitoringMode;
import tod.core.bci.IInstrumenter.InstrumentedClass;
import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.database.structure.standard.StructureDatabase;
import zz.utils.Utils;

public class ASM2Test
{
	@Test
	public void test() throws FileNotFoundException, IOException
	{
		TODConfig theConfig = new TODConfig();
		theConfig.set(TODConfig.SCOPE_TRACE_FILTER, "[+tod.test.bci.asm2.*]");
		StructureDatabase theDatabase = StructureDatabase.create(theConfig);
		ASMInstrumenter2 theInstrumenter = new ASMInstrumenter2(theConfig, theDatabase);
		
		instrument(theDatabase, theInstrumenter, "bin/tod/test/bci/asm2/Fun1.class");
		instrument(theDatabase, theInstrumenter, "bin/tod/test/bci/asm2/FunNumber.class");
		instrument(theDatabase, theInstrumenter, "bin/tod/test/bci/asm2/IFunStupid.class");
		instrument(theDatabase, theInstrumenter, "bin/tod/test/bci/asm2/outofscope/IFunStupid2.class");
		instrument(theDatabase, theInstrumenter, "bin/tod/test/bci/asm2/Fun2.class");
		instrument(theDatabase, theInstrumenter, "bin/tod/test/bci/asm2/outofscope/FunList.class");
		instrument(theDatabase, theInstrumenter, "bin/tod/test/bci/asm2/outofscope/Fun3.class");
	}
	
	private void instrument(IStructureDatabase aDatabase, IInstrumenter aInstrumenter, String aName) throws FileNotFoundException, IOException
	{
		System.out.println("Trying: "+aName);
		byte[] theClassData = Utils.readInputStream_byte(new FileInputStream(aName));
		
		ClassReader cr = new ClassReader(theClassData);
		ClassNode theClassNode = new ClassNode();
		cr.accept(theClassNode, 0);
		
		String theName = theClassNode.name;
		System.out.println("  Found class: "+theName);
		
		InstrumentedClass theInstrumentedClass = aInstrumenter.instrumentClass(theName, theClassData, false);
		
		for(BehaviorMonitoringMode theMode : theInstrumentedClass.modeChanges)
		{
			IBehaviorInfo theBehavior = aDatabase.getBehavior(theMode.behaviorId, true);
			Utils.println("  %s -> %s", LocationUtils.toString(theBehavior), MonitoringMode.toString(theMode.mode));
		}
		
		System.out.println("  Done.");
	}
}
