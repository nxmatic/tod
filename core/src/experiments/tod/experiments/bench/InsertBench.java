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
package tod.experiments.bench;

import java.util.Random;

import zz.utils.Utils;

public class InsertBench
{
	public static final int NUM_THREADS = 10;
	
	public static final int MAX_DEPTH = 6;
	
	public static final int MAX_OBJECTS = 10000;
	public static final int MAX_METHODS = 1000;
	public static final int MAX_FIELDS = 1000;
	public static final int MAX_VARS = 50;
	public static final int MAX_ARGS = 10;
	
	public static final int MAX_ITERATIONS = 10;
	public static final int MAX_FIELD_WRITES = 10;
	public static final int MAX_VAR_WRITES = 10;
	public static final int MAX_METHOD_CALLS = 10;
	
	
	private ISimpleLogCollector itsCollector;
	public long itsNumEvents;
	
	public class MyThread extends Thread
	{
		private Random itsRandom = new Random(0);
		
		private long itsId;
		private long itsSeq;
		
		public MyThread(long aId)
		{
			itsId = aId;
		}

		@Override
		public void run()
		{
			while (itsSeq < (itsNumEvents/NUM_THREADS)) method(0, 0, 0);
		}
		
		private int rand(int aMax)
		{
			return itsRandom.nextInt(aMax);
		}
		
		private long seq()
		{
			if (itsSeq % 1000000 == 0) System.err.println(itsId + ": " + itsSeq);
			return itsSeq++;
		}
		
		public long eventCount()
		{
			return itsSeq;
		}
		
		private void method(int aDepth, int aId, long aTarget)
		{
			if (aDepth >= MAX_DEPTH || itsSeq >= (itsNumEvents/NUM_THREADS)) return;
			
			int nArgs = aId % MAX_ARGS;
			long[] args = new long[nArgs];
			for(int i=0;i<nArgs;i++) args[i] = rand(MAX_OBJECTS);
			itsCollector.logBehaviorEnter(itsId, seq(), aId, aTarget, args);
			
			int nIterations = rand(MAX_ITERATIONS);
			
			for (int i=0;i<nIterations;i++)
			{
				int nFieldWrites = rand(MAX_FIELD_WRITES);
				int nVarWrites = rand(MAX_VAR_WRITES);
				int nMethodCalls = rand(MAX_METHOD_CALLS);
			
				for (int j=0;j<nFieldWrites;j++) 
				{
					itsCollector.logFieldWrite(itsId, seq(), rand(MAX_FIELDS), rand(MAX_OBJECTS), rand(MAX_OBJECTS));
				}
				
				for (int j=0;j<nVarWrites;j++) 
				{
					itsCollector.logVarWrite(itsId, seq(), rand(MAX_VARS), rand(MAX_OBJECTS));
				}
				
				for(int j=0;j<nMethodCalls;j++)
				{
					method(aDepth+1, rand(MAX_METHODS), rand(MAX_OBJECTS));
				}
			}
			
			itsCollector.logBehaviorExit(itsId, seq(), rand(MAX_OBJECTS));
		}
	}
	
	
	
	public InsertBench(ISimpleLogCollector aCollector, long aNumEvents) throws InterruptedException
	{
		itsCollector = aCollector;
		itsNumEvents = aNumEvents;
		
		long t0 = System.currentTimeMillis();
		
		MyThread[] threads = new MyThread[NUM_THREADS]; 
		for(int i=0;i<NUM_THREADS;i++)
		{
			threads[i] = new MyThread(i);
			threads[i].start();
		}
		
		for(int i=0;i<NUM_THREADS;i++)
		{
			threads[i].join();
		}
		
		long t1 = System.currentTimeMillis();
		
		long eventCount = 0;
		for(int i=0;i<NUM_THREADS;i++)
		{
			eventCount += threads[i].eventCount();
		}
		
		float seconds = 0.001f * (t1-t0);
		long size = itsCollector.getStoredSize();
		
		System.out.println(String.format(
				"Finished. Total time: %.1fs.\n" +
				" Event count: %,d. Rate: %,dev/s.\n" +
				" Storage: %,.1fMB. Rate: %,.3fMB/s",
				seconds,
				eventCount,
				(long)(eventCount/seconds),
				1f*size/(1024*1024),
				size/(1024*1024*seconds)));
	}



	public static void main(String[] args) throws Exception
	{
		String collectorClassName = args[0];
		long numEvents = Long.parseLong(args[1]);
		
		Class colletorClass = Class.forName(collectorClassName);
		ISimpleLogCollector collector = (ISimpleLogCollector) colletorClass.newInstance();
		
		System.out.println("Running InsertBench of "+colletorClass.getSimpleName()+" with "+numEvents+" events.");
		
		new InsertBench(collector, numEvents);
	}
	
	public static long getDirSize(String aDir)
	{
		try
		{
			Process theProcess = Runtime.getRuntime().exec("du -bs "+aDir);
			theProcess.waitFor();
			
			String theResult = Utils.readInputStream(theProcess.getInputStream());
			theResult = theResult.split("[\t ]")[0];
			return Long.parseLong(theResult);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
