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
package tod.impl.dbgrid.bench;


public class GridDispatch
{
	public static void main(String[] args) throws Exception
	{
		throw new UnsupportedOperationException();
//		Registry theRegistry = LocateRegistry.createRegistry(1099);
//		
//		int theExpectedNodes;
//		int theEventsCount;
//		theExpectedNodes = Integer.parseInt(args[0]);
//		theEventsCount = Integer.parseInt(args[1]);
//		
//		final GridMaster theMaster = TODUtils.setupMaster(theRegistry, theExpectedNodes);
//		final AbstractEventDispatcher theDispatcher = theMaster._getDispatcher();
//		final EventGenerator theGenerator = BenchEventDatabase.createGenerator();
//
//		final int n = theEventsCount;
//		
//		BenchResults theGenResults = BenchBase.benchmark(new Runnable()
//		{
//			public void run()
//			{
//				for (int i=0;i<n;i++) theGenerator.next();
//			}
//		});
//		
//		System.out.println("Gen: "+theGenResults);
//		
//		BenchResults theDispatchResults = BenchBase.benchmark(new Runnable()
//		{
//			public void run()
//			{
//				GridEvent theEvent = theGenerator.next();
//				
//				for (int i=0;i<n;i++)
//				{
//					theDispatcher.dispatchEvent(theGenerator.next());
//				}
//			}
//		});
//		
//		System.out.println("Dispatch: "+theDispatchResults);
//		
//		float dt = (theDispatchResults.totalTime-theGenResults.totalTime)/1000f;
//		System.out.println("DeltaT: "+dt);
//		
//		float theEpS = n/dt;
//		System.out.println("events/s: "+theEpS);
//		System.exit(0);
	}
}
