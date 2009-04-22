/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.experiments;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFrame;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import tod.core.bci.IInstrumenter.InstrumentedClass;
import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.gui.activities.structure.DisassemblyPanel;
import tod.impl.bci.asm.ASMDebuggerConfig;
import tod.impl.bci.asm.ASMInstrumenter;
import tod.impl.database.structure.standard.StructureDatabase;
import zz.utils.Utils;

public class Instrument
{
	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		String theClassFile = args[0];
		byte[] theClassData = Utils.readInputStream_byte(new FileInputStream(theClassFile));
		
		ClassReader cr = new ClassReader(theClassData);
		ClassNode theClassNode = new ClassNode();
		cr.accept(theClassNode, 0);
		
		String theName = theClassNode.name.replace('/', '.');
		System.out.println(theName);
		
		TODConfig theConfig = new TODConfig();
		ASMDebuggerConfig theDebuggerConfig = new ASMDebuggerConfig(theConfig);
		StructureDatabase theStructureDatabase = StructureDatabase.create(theConfig, "test");
		ASMInstrumenter theInstrumenter = new ASMInstrumenter(theStructureDatabase, theDebuggerConfig);
		
		InstrumentedClass theInstrumentedClass = theInstrumenter.instrumentClass(theName, theClassData, false);

		IClassInfo theClass = theStructureDatabase.getClass(theName, true);
		String theSig = "()Ltod/impl/evdbng/db/file/Tuple;";
		IBehaviorInfo theBehavior = theClass.getBehavior(
				"fetchNext", 
				LocationUtils.getArgumentTypes(theStructureDatabase, theSig), 
				LocationUtils.getReturnType(theStructureDatabase, theSig));
		
		JFrame theFrame = new JFrame("TOD - Instrument");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		theFrame.setContentPane(new DisassemblyPanel(theBehavior));
		theFrame.pack();
		theFrame.setVisible(true);
	}
	
}
