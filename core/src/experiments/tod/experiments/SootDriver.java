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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.ClassProvider;
import soot.ClassSource;
import soot.CoffiClassSource;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.options.Options;
import soot.shimple.Shimple;
import soot.shimple.ShimpleBody;

public class SootDriver
{
	public static void main(String[] args) throws Exception
	{
		Options.v().set_soot_classpath("/home/gpothier/apps/java/jdk1.6.0_01/jre/lib/rt.jar");

		File f = new File("bin/tod/experiments/SootDummyClass.class");
		byte[] theBytecode = new byte[(int) f.length()];
		DataInputStream theStream = new DataInputStream(new FileInputStream(f));
		theStream.readFully(theBytecode);
		
		SourceLocator.v().setClassProviders((List) Arrays.asList(
				new MyClassProvider("tod.experiments.SootDummyClass", theBytecode),
				new PlatformClassProvider(
						"java.lang.Object"/*,
						"java.lang.String",
						"java.lang.Comparable",
						"java.lang.Comparable",
						"java.lang.CharSequence",
						"java.io.Serializable"*/)
				));
		
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_verbose(true);
		Scene.v().setPhantomRefs(true);
		SootClass theClass = Scene.v().loadClassAndSupport("tod.experiments.SootDummyClass");
		System.out.println(theClass);
		List<SootMethod> theMethods = theClass.getMethods();
		System.out.println(theMethods);
		
		for (SootMethod theMethod : theMethods)
		{
			Scene.v().setPhantomRefs(true);
			ShimpleBody theBody = Shimple.v().newBody(theMethod);
			Body theActiveBody = theMethod.retrieveActiveBody();
			theBody.importBodyContentsFrom(theActiveBody);
			theBody.rebuild();
			System.out.println(theBody);
		}
	}
	
	public static class MyClassProvider implements ClassProvider
	{
		private String itsName;
		private byte[] itsBytecode;

		public MyClassProvider(String aName, byte[] aBytecode)
		{
			itsName = aName;
			itsBytecode = aBytecode;
		}

		public ClassSource find(String aClassName)
		{
			if (itsName.equals(aClassName)) 
			{
				return new CoffiClassSource(itsName, new ByteArrayInputStream(itsBytecode));
			}
			else return null;
		}
	}
	
	public static class PlatformClassProvider implements ClassProvider
	{
		private Set<String> itsAllowedClasses;
		
		public PlatformClassProvider(String... aAllowedClasses)
		{
			itsAllowedClasses = new HashSet<String>();
			for (String theClass : aAllowedClasses) itsAllowedClasses.add(theClass);
		}

		public ClassSource find(String aClassName)
		{
			if (! itsAllowedClasses.contains(aClassName)) return null;
			
	        String fileName = aClassName.replace('.', '/') + ".class";
	        SourceLocator.FoundFile file = 
	            SourceLocator.v().lookupInClassPath(fileName);
	        if( file == null ) return null;
	        return new CoffiClassSource(aClassName, file.inputStream());
		}
		
	}
}
