/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.evdb1.bench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tod.BenchBase;
import tod.Util;
import tod.BenchBase.BenchResults;
import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.browser.Stepper;
import tod.core.database.browser.IObjectInspector.IEntryInfo;
import tod.core.database.event.ICreationEvent;
import tod.core.database.event.IFieldWriteEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.dbgrid.DBGridUtils;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.GridMaster;
import tod.impl.dbgrid.RIGridMaster;
import tod.impl.evdb1.DebuggerGridConfig1;
import tod.impl.evdb1.GridLogBrowser1;
import tod.utils.ConfigUtils;
import zz.utils.ITask;
import zz.utils.Utils;
import zz.utils.srpc.SRPCRegistry;

public class GridQuery
{
	public static final String STORE_EVENTS_FILE =
		ConfigUtils.readString("events-file", "events-raw.bin");

	public static void main(String[] args) throws Exception
	{
		SRPCRegistry theRegistry = Util.getLocalSRPCRegistry();
		
		String theFileName = STORE_EVENTS_FILE;
		final File theFile = new File(theFileName);
		
		final GridMaster theMaster = DBGridUtils.setupMaster(theRegistry, args);
		
		final long[] theEventsCount = new long[1];
		BenchResults theReplayTime = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				try
				{
					throw new UnsupportedOperationException("Reimplement");
//					theEventsCount[0] = Fixtures.replay(theFile, theMaster);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		System.out.println(theReplayTime);
		float theEpS = 1000f*theEventsCount[0]/theReplayTime.totalTime;
		System.out.println("Replayed "+theEventsCount[0]+" events: "+theEpS+"ev/s");

		System.out.println("Looking up master in registry");
		RIGridMaster theRemoteMaster = (RIGridMaster) theRegistry.lookup(GridMaster.SRPC_ID);
		
		final GridLogBrowser theBrowser = GridLogBrowser1.createRemote(null, theRemoteMaster);
		
//		findObjects(theBrowser);
		benchTrueQueries(theBrowser);
		
		long theFirstTimestamp = theBrowser.getFirstTimestamp();
		long theLastTimestamp = theBrowser.getLastTimestamp();
		
		
//		System.out.println("\nCreating objects plot\n");
//		createObjectPlot(theBrowser);

//		System.out.println("\nPerforming count benchmarks --- pass #1\n");
//		
//		int theSlots = 1000;
//		
//		long[] theFastCounts = benchCounts(theBrowser, theFirstTimestamp, theLastTimestamp, theSlots, false);
//		long[] theMergeCounts = benchCounts(theBrowser, theFirstTimestamp, theLastTimestamp, theSlots, true);
//		
//		System.out.println("\nPerforming count benchmarks --- pass #2\n");
//		System.out.println("(skipped)");
//		
////		theFastCounts = benchCounts(theBrowser, theFirstTimestamp, theLastTimestamp, theSlots, false);
////		theMergeCounts = benchCounts(theBrowser, theFirstTimestamp, theLastTimestamp, theSlots, true);
//		
//		printDistortion(theMergeCounts, theFastCounts);
		
//		benchCursors(theBrowser, 1, 1000);
//		benchCursors(theBrowser, 1000, 100);
		
//		benchSplitIndex(theBrowser, 1, 100);
//		benchSplitIndex(theBrowser, 100, 10);
		
		
		System.out.println(" *** Done ***");
		System.exit(0);
	}
	
	private static void printDistortion(long[] c1, long[] c2)
	{
		assert c1.length == c2.length;
		
		long theAbsSum = 0;
		long t1 = 0;
		long t2 = 0;
		float theAvgSum = 0;
		
		for (int i=0;i<c1.length;i++)
		{
			long theAbs = Math.abs(c1[i]-c2[i]);
			theAbsSum += theAbs;
			t1 += c1[i];
			t2 += c2[i];
			
			long theRef = Math.min(c1[i], c2[i]);
			float theAvg = theRef != 0 ? 
					1f * theAbs / theRef 
					: (theAbs != 0 ? 1 : 0);
			theAvgSum += theAvg;
		}
		
		System.out.println(String.format(
				"Distortion - abs. diff: %d, t1: %d, t2: %d, %%: %.2f, avg.: %f",
				theAbsSum,
				t1,
				t2,
				100f*theAbsSum/Math.min(t1, t2),
				theAvgSum/c1.length));
	}
	
	private static <T> List<T> list(Iterable<T> aIterable)
	{
		List<T> theList = new ArrayList<T>();
		Utils.fillCollection(theList, aIterable);
		return theList;
	}
	
	private static long[] benchCounts(
			final ILogBrowser aBrowser, 
			final long aT1,
			final long aT2, 
			final int aSlots,
			final boolean aForceMergeCounts)
	{
		System.out.println("\nCount benchmarks (force merge: "+aForceMergeCounts+")");
//		System.out.println("t1: "+AgentUtils.formatTimestamp(aT1));
//		System.out.println("t2: "+AgentUtils.formatTimestamp(aT2));
//		System.out.println("Slots: "+aSlots);
		
		final long[] theCounts = new long[aSlots];
		
		BenchResults theQueryTime = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				for (IThreadInfo theThread: aBrowser.getThreads())
				{
					System.out.println("Retrieving counts for thread "+theThread.getId()+" of host "+theThread.getHost().getId());

					long t0 = System.currentTimeMillis();
					IEventFilter theFilter = aBrowser.createThreadFilter(theThread);
					IEventBrowser theEventBrowser = aBrowser.createBrowser(theFilter);
					
					long[] theThreadCounts = theEventBrowser.getEventCounts(
							aT1, 
							aT2, 
							aSlots,
							aForceMergeCounts);
					
					long theCount = 0;
					for (int i = 0; i < theThreadCounts.length; i++)
					{
						long l = theThreadCounts[i];
						theCount += l;
						theCounts[i] += l;
					}
	
					long t1 = System.currentTimeMillis();
					long t = t1-t0;
					
					System.out.println("  Event count: "+theCount+", time: "+t+"ms");
				}
			}
		});
		
		System.out.println(theQueryTime);
		
		return theCounts;
	}
	
	private static void benchCursors(
			final ILogBrowser aBrowser, 
			final int aBulk, 
			final int aCount)
	{
		System.out.println("Benchmark "+aBulk+"-cursors: "+aCount);
		
//		final Map<ObjectId, IFieldInfo> theFields = createValidFields(aBrowser, aCount);
		final List<IThreadInfo> theThreads = list(aBrowser.getThreads());
		
		benchCursors(aBrowser, aBulk, aCount, new ITask<Random, IEventFilter>()
				{
					public IEventFilter run(Random aRandom)
					{
						IThreadInfo theThread = theThreads.get(aRandom.nextInt(theThreads.size()));
						return aBrowser.createIntersectionFilter(
								aBrowser.createThreadFilter(theThread),
								aBrowser.createDepthFilter(3+aRandom.nextInt(20)));
					}
				});
	}
	
	private static void benchSplitIndex(
			final ILogBrowser aBrowser, 
			final int aBulk, 
			final int aCount)
	{
		System.out.println("Benchmark of split index. "+aBulk+"-cursors: "+aCount);
		
		System.out.println("1- Depth");
		BenchResults theDepth = benchCursors(aBrowser, aBulk, aCount, new ITask<Random, IEventFilter>()
				{
					public IEventFilter run(Random aRandom)
					{
						return aBrowser.createDepthFilter(3+aRandom.nextInt(200));
					}
				});
		
		System.out.println("2- Oid");
		BenchResults theOid = benchCursors(aBrowser, aBulk, aCount, new ITask<Random, IEventFilter>()
				{
					public IEventFilter run(Random aRandom)
					{
						return aBrowser.createObjectFilter(new ObjectId(1+aRandom.nextInt(8000000)));
					}
				});
		
		
	}
	
	
	
	private static BenchResults benchCursors(
			final ILogBrowser aBrowser, 
			final int aBulk, 
			final int aCount,
			final ITask<Random, IEventFilter> aFilterGenerator)
	{
		final long theFirstTimestamp = aBrowser.getFirstTimestamp();
		final long theLastTimestamp = aBrowser.getLastTimestamp();
		final long theTimeSpan = theLastTimestamp-theFirstTimestamp;
		
		final long[] theCount = new long[1];
		
		BenchResults theQueryTime = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				Random theRandom = new Random(0);
				for (int i=0;i<aCount;i++)
				{
					IEventFilter theFilter = aFilterGenerator.run(theRandom);
					
					long theTimestamp = theRandom.nextLong();
					theTimestamp = theFirstTimestamp + theTimestamp % theTimeSpan; 
					
					long t0 = System.currentTimeMillis();
					theCount[0] += benchCursor(aBrowser, theFilter, theTimestamp, aBulk);
					long t1 = System.currentTimeMillis();
					long t = t1-t0;
					System.out.println(String.format("%d - %s - Took %dms", i, theFilter, t));
				}
//				
//				for(Map.Entry<ObjectId, IFieldInfo> theEntry : theFields.entrySet())
//				{
//					benchCursor(aBrowser, theEntry.getKey(), null, aBulk);
//				}
			}
		});
		
		System.out.println(theQueryTime);
		float theQpS = 1000f * aCount / theQueryTime.totalTime;
		System.out.println("Queries/s: "+theQpS+", retrieved events: "+theCount[0]);
		
		return theQueryTime;
	}
	
	private static Map<ObjectId, IFieldInfo> createValidFields(ILogBrowser aBrowser, int aCount)
	{
		Map<ObjectId, IFieldInfo> theMap = new HashMap<ObjectId, IFieldInfo>();
		
		Random theRandom = new Random(0);
		while(theMap.size() < aCount)
		{
			ObjectId theId = new ObjectId(theRandom.nextInt(10000));
			IFieldInfo theField = getValidField(aBrowser, theId);
			if (theField == null) continue;
			
			theMap.put(theId, theField);
		}
		
		return theMap;
	}
	
	/**
	 * Returns a field id that corresponds to a field of the given object
	 */
	private static IFieldInfo getValidField(ILogBrowser aBrowser, ObjectId aObjectId)
	{
		IEventFilter theFilter = aBrowser.createIntersectionFilter(
				aBrowser.createTargetFilter(aObjectId),
				aBrowser.createFieldWriteFilter());
		
		IEventBrowser theBrowser = aBrowser.createBrowser(theFilter);
		
		if (theBrowser.hasNext())
		{
			IFieldWriteEvent theEvent = (IFieldWriteEvent) theBrowser.next();
			return theEvent.getField();
		}
		else return null;
	}
	
	private static void benchCursor(
			ILogBrowser aBrowser, 
			long aTimestamp,
			ObjectId aObjectId, 
			IFieldInfo aField, 
			int aCount)
	{
		ICompoundFilter theFilter = aBrowser.createIntersectionFilter(
				aBrowser.createTargetFilter(aObjectId),
				aField != null ? 
						aBrowser.createFieldFilter(aField)
						: aBrowser.createFieldWriteFilter());
		
		benchCursor(aBrowser, theFilter, aTimestamp, aCount);
	}
	
	private static void benchCursor(
			ILogBrowser aBrowser,
			long aTimestamp,
			ObjectId aObjectId, 
			IBehaviorInfo aBehavior, 
			int aCount)
	{
		ICompoundFilter theFilter = aBrowser.createIntersectionFilter(
				aBrowser.createTargetFilter(aObjectId),
				aBehavior != null ? 
						aBrowser.createBehaviorCallFilter(aBehavior)
						: aBrowser.createBehaviorCallFilter());
		
		benchCursor(aBrowser, theFilter, aTimestamp, aCount);
	}
	
	private static int benchCursor(
			ILogBrowser aBrowser,
			IEventFilter aFilter,
			long aTimestamp,
			int aCount)
	{
		IEventBrowser theBrowser = aBrowser.createBrowser(aFilter);
		theBrowser.setNextTimestamp(aTimestamp);
		
		int i = 0;
		while (theBrowser.hasNext() && i < aCount)
		{
			theBrowser.next();
			i++;
		}
		
//		if (i < aCount) System.out.println(i);
		return i;
	}
	
	/**
	 * Creates a data file containing the timestamps of references
	 * to each object.
	 */
	private static final void createObjectPlot(ILogBrowser aBrowser)
	{
		try
		{
			PrintWriter theWriter = new PrintWriter(new FileWriter("objects-refs.txt"));
			for (long i=1;i<DebuggerGridConfig1.STRUCTURE_OBJECT_COUNT;i++)
			{
				ObjectId theId = new ObjectId(i);
				IEventFilter theFilter = aBrowser.createObjectFilter(theId);
				IEventBrowser theBrowser = aBrowser.createBrowser(theFilter);
				
				long[] theCounts = theBrowser.getEventCounts(
						aBrowser.getFirstTimestamp(), 
						aBrowser.getLastTimestamp(), 
						1000, 
						false);
				
				StringBuilder theBuilder = new StringBuilder(i + " ");
				for (long theCount : theCounts)
				{
					theBuilder.append(theCount);
					theBuilder.append('\t');
				}
				
				theWriter.println(theBuilder.toString());
				
				if (i % 1000 == 0) System.out.println(i);
			}
			
			theWriter.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		

	}
	
	private static void findObjects(ILogBrowser aBrowser)
	{
		IEventBrowser theBrowser = aBrowser.createBrowser(aBrowser.createFieldWriteFilter());
		for (int i=0;i<100;i++)
		{
			IFieldWriteEvent theEvent = (IFieldWriteEvent) theBrowser.next();
			Object theTarget = theEvent.getTarget();
			if (theTarget instanceof ObjectId)
			{
				ObjectId theObjectId = (ObjectId) theTarget;
				System.out.println(Long.toHexString(theObjectId.getId()));
			}
		}
	}
	
	private static void benchTrueQueries(ILogBrowser aBrowser)
	{
		benchObjectInspector(aBrowser);
		benchStepOver(aBrowser);
		benchStepInto(aBrowser);
	}
	
	private static void benchStepInto(ILogBrowser aBrowser)
	{
		System.out.println("Bench Step Into...");
		benchStep(aBrowser, new ITask<Stepper, Object>()
				{
					public Object run(Stepper aStepper)
					{
						aStepper.forwardStepInto();
						return null;
					}
				});
	}
	
	private static void benchStepOver(ILogBrowser aBrowser)
	{
		System.out.println("Bench Step Over...");
		benchStep(aBrowser, new ITask<Stepper, Object>()
				{
					public Object run(Stepper aStepper)
					{
						aStepper.forwardStepOver();
						return null;
					}
				});		
	}
	
	private static void benchStep(ILogBrowser aBrowser, ITask<Stepper, ?> aStepperDriver)
	{
		final long theFirstTimestamp = aBrowser.getFirstTimestamp();
		final long theLastTimestamp = aBrowser.getLastTimestamp();
		final long theTimeSpan = theLastTimestamp-theFirstTimestamp;
		
		// Take a sample of the threads
		Iterable<IThreadInfo> theThreads0 = aBrowser.getThreads();
		List<IThreadInfo> theThreads1 = new ArrayList<IThreadInfo>();
		
		// first reorder the threads to ensure consistent results.
		for (IThreadInfo theThread : theThreads0)
		{
			Utils.listSet(theThreads1, theThread.getId(), theThread);
		}
		
		List<IThreadInfo> theThreads = new ArrayList<IThreadInfo>();
		for (int i=1;i<theThreads1.size();i+=10) theThreads.add(theThreads1.get(i));
		

		SerieResult theTimePerStepSerie = new SerieResult();
		Random theRandom = new Random(150);
		int theCount = 0;
		long t0 = System.currentTimeMillis();
		for (IThreadInfo theThread : theThreads)
		{
			long theThreadFirst = getFirstTimestamp(aBrowser, theThread);
			long theThreadLast = getLastTimestamp(aBrowser, theThread);
			if (theThreadFirst == 0 || theThreadLast == 0)
			{
				System.out.println("  Skipping thread: "+theThread);
				continue;
			}
			
			assert theFirstTimestamp <= theThreadFirst;
			assert theLastTimestamp >= theThreadLast;
			
			long theThreadSpan = theThreadLast - theThreadFirst;
			
			long theTimestamp = Math.abs(theRandom.nextLong());
			theTimestamp = theThreadFirst + (theTimestamp % (theThreadSpan/2));
			
			assert theThreadFirst <= theTimestamp;
			assert theThreadLast >= theTimestamp;
			
			float p = 100f*(theTimestamp-theFirstTimestamp)/theTimeSpan;
			
			System.out.println("  Bench Step: "+p+"% - thread "+theThread.getId());

			StepBenchResult theResult = benchStep(aBrowser, theTimestamp, theThread, aStepperDriver);
			if (theResult != null)
			{
				theCount += theResult.steps;
				if (theResult.steps > 0)
				{
					long theTimePerStep = theResult.time/theResult.steps;
					theTimePerStepSerie.add(theTimePerStep);
				}
			}
		}
		long t1 = System.currentTimeMillis();
		long t = t1-t0;
		float stps = 1000f * theCount / t;
		
		System.out.println("Bench Step: "+t+"ms, step/s: "+stps);
		System.out.println("Time per step: "+theTimePerStepSerie);
	}
	
	private static long getFirstTimestamp(ILogBrowser aBrowser, IThreadInfo aThread)
	{
		IEventBrowser theBrowser = aBrowser.createBrowser(aBrowser.createThreadFilter(aThread));
		theBrowser.setNextTimestamp(0);
		if (theBrowser.hasNext()) return theBrowser.next().getTimestamp();
		else return 0;
	}
	
	private static long getLastTimestamp(ILogBrowser aBrowser, IThreadInfo aThread)
	{
		IEventBrowser theBrowser = aBrowser.createBrowser(aBrowser.createThreadFilter(aThread));
		theBrowser.setPreviousTimestamp(Long.MAX_VALUE);
		if (theBrowser.hasPrevious()) return theBrowser.previous().getTimestamp();
		else return 0;
	}
	
	
	
	private static StepBenchResult benchStep(
			ILogBrowser aBrowser, 
			long aTimestamp, 
			IThreadInfo aThread,
			ITask<Stepper, ?> aStepperDriver)
	{
		IEventBrowser theEventBrowser = aBrowser.createBrowser(aBrowser.createThreadFilter(aThread));
		theEventBrowser.setNextTimestamp(aTimestamp);
		if (! theEventBrowser.hasNext()) return null;
		ILogEvent theEvent = theEventBrowser.next();
		
		Stepper theStepper = new Stepper(aBrowser);
		theStepper.setCurrentEvent(theEvent);
		
		int n = 0;
		long t0 = System.currentTimeMillis();
		for (int i=0;i<100;i++)
		{
			aStepperDriver.run(theStepper);
			if (theStepper.getCurrentEvent() == null) break;
			n++;
		}
		long t1 = System.currentTimeMillis();
		long t = t1-t0;
		System.out.println("  Bench step: "+t+"ms - "+n+" events.");
		
		return new StepBenchResult(t, n);
	}

	private static void benchObjectInspector(ILogBrowser aBrowser)
	{
		Random theRandom = new Random(0);
		
		SerieResult theFieldSerie = new SerieResult();
		SerieResult theTimePerField = new SerieResult();
		SerieResult theTimePerObject = new SerieResult();
		System.out.println("Bench object inspectors");
		long t0 = System.currentTimeMillis();
		for (int i=0;i<30;i++)
		{
			long theId = theRandom.nextInt(8000000);
			ObjectInspectorResult theResult = benchObjectInspector(aBrowser, theId);
			if (theResult == null) 
			{
				i--;
				continue;
			}
			int theFieldsCount = theResult.getFieldsCount();
			theFieldSerie.add(theFieldsCount);
			theTimePerField.add(theResult.getAvg()/theFieldsCount);
			theTimePerObject.add(theResult.getAvg());
		}
		
		long t1 = System.currentTimeMillis();
		long t = t1-t0;
		
		System.out.println("Bench object inspectors: "+t+"ms");
		System.out.println("Fields: "+theFieldSerie);
		System.out.println("ms/field: "+theTimePerField);
		System.out.println("ms/obj: "+theTimePerObject);
	}
	
	private static ObjectInspectorResult benchObjectInspector(ILogBrowser aBrowser, long aId)
	{
		IObjectInspector theInspector = aBrowser.createObjectInspector(new ObjectId(aId));
		long t0 = System.currentTimeMillis();
		
		ICreationEvent theCreationEventEvent = theInspector.getCreationEvent();
		if (theCreationEventEvent == null)
		{
			System.out.println("  Instantiation event not found for: "+aId);
			return null;
		}
		ITypeInfo theType = theInspector.getType();
		List<IEntryInfo> theEntries = theInspector.getEntries(0, Integer.MAX_VALUE);
		if (theEntries.size() == 0)
		{
			System.out.println("  No fields found for object: "+aId);
			return null;
		}
		
		long t1 = System.currentTimeMillis();
		long t = t1-t0;
		
		System.out.println("  Retrieved fields for obj "+aId+"("+theType+"): "+t+"ms, "+theEntries.size()+" fields");
		
		final long theFirstTimestamp = theCreationEventEvent.getTimestamp();
		final long theLastTimestamp = aBrowser.getLastTimestamp();
		final long theTimeSpan = theLastTimestamp-theFirstTimestamp;
		
		Random theRandom = new Random(aId);
		ObjectInspectorResult theResult = new ObjectInspectorResult(theEntries.size());
		for (int i=0;i<20;i++)
		{
			long theTimestamp = theRandom.nextLong();
			theTimestamp = theFirstTimestamp + theTimestamp % theTimeSpan; 

			long theTime = benchObjectInspector(theInspector, theTimestamp);
			theResult.add(theTime);
		}

		System.out.println("Object inspected - took(ms): "+theResult);
		return theResult;
	}
	
	private static long benchObjectInspector(IObjectInspector aInspector, long aTimestamp)
	{
		long t0 = System.currentTimeMillis();
		
		IEventBrowser theBrowser = aInspector.getLogBrowser().createBrowser();
		theBrowser.setNextTimestamp(aTimestamp);
		ILogEvent theEvent = theBrowser.next();
		
		aInspector.setReferenceEvent(theEvent);
		List<IEntryInfo> theEntries = aInspector.getEntries(0, Integer.MAX_VALUE);
		
		for (IEntryInfo theEntry : theEntries)
		{
			aInspector.getEntryValue(theEntry);
		}
		
		long t1 = System.currentTimeMillis();
		long t = t1-t0;
		
		return t;
	}
	
	private static class SerieResult
	{
		private long itsMin;
		private long itsMax;
		private long itsSum;
		private long itsCount;
		
		public void add(long aValue)
		{
			itsMin = Math.min(itsMin, aValue);
			itsMax = Math.max(itsMax, aValue);
			itsSum += aValue;
			itsCount++;
		}

		public long getMax()
		{
			return itsMax;
		}

		public long getMin()
		{
			return itsMin;
		}
		
		public long getAvg()
		{
			return itsSum/itsCount;
		}
		
		@Override
		public String toString()
		{
			return String.format(
					"min: %d max: %d avg: %d sum: %d count: %d",
					getMin(),
					getMax(),
					getAvg(),
					itsSum,
					itsCount);
		}
	}
	
	private static class ObjectInspectorResult extends SerieResult
	{
		private int itsFieldsCount;

		public ObjectInspectorResult(int aFieldsCount)
		{
			itsFieldsCount = aFieldsCount;
		}

		public int getFieldsCount()
		{
			return itsFieldsCount;
		}
	}
	
	private static class StepBenchResult
	{
		public final long time;
		public final int steps;
		
		public StepBenchResult(final long aTime, final int aSteps)
		{
			time = aTime;
			steps = aSteps;
		}
	}
	
}
