package tod.impl.evdbng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.evdbng.db.SnapshotIndex;
import tod.impl.evdbng.db.Stats;
import tod.impl.evdbng.db.StringIndex;
import tod.impl.evdbng.db.cflowindex.CFlowIndex;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline.PerThreadIndex;
import tod.impl.evdbng.db.file.LongInsertableBTree.LongTuple;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.mapped.MappedPagedFile;
import tod.impl.replay2.EventCollector;
import tod.impl.replay2.LocalsSnapshot;
import tod.impl.replay2.ReifyEventCollector;
import tod.impl.replay2.ReifyEventCollector.EventList;
import tod.impl.replay2.ReifyEventCollector.EventList.FieldReadEvent;
import tod.impl.replay2.ReplayerLoader;
import tod.impl.server.DBSideIOThread;
import zz.utils.Utils;
import zz.utils.cache.MRUBuffer;

public class Indexer 
{
	private final TODConfig itsConfig;
	private final IMutableStructureDatabase itsDatabase;
	private final File itsEventsFile;
	private final PagedFile itsIndexFile;
	private final Page itsDirectoryPage;
	
	private final Pipeline itsFieldWritePipeline;
	private final StringIndex itsStringIndex;
	private List<Collector> itsCollectors = new ArrayList<Collector>();
	private BlocksBuffer itsBlocksBuffer = new BlocksBuffer();
	
	private int itsDirectoryOffset = 0;
	
	private ReplayerLoader itsPartialReplayerLoader;
	
	public Indexer(TODConfig aConfig, IMutableStructureDatabase aDatabase, File aEventsFile, PagedFile aIndexFile)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsEventsFile = aEventsFile;
		itsIndexFile = aIndexFile;
		if (itsIndexFile.getPagesCount() == 0) itsDirectoryPage = itsIndexFile.create(Stats.ACC_MISC);
		else itsDirectoryPage = itsIndexFile.get(1);
		
		itsFieldWritePipeline = new Pipeline(createPidSlot());
		itsStringIndex = new StringIndex("strings", createPidSlot());
	}
	
	private PidSlot createPidSlot()
	{
		PidSlot theSlot = new PidSlot(Stats.ACC_MISC, itsDirectoryPage, itsDirectoryOffset);
		itsDirectoryOffset += PidSlot.size();
		return theSlot;
	}
	
	private Collector createCollector(int aThreadId)
	{
		Collector theCollector = new Collector(aThreadId);
		if (aThreadId >= 0) Utils.listSet(itsCollectors, aThreadId, theCollector);
		return theCollector;
	}
	
	private void registerString(ObjectId aId, String aString)
	{
		itsStringIndex.addString(aId.getId(), aString);
	}
	
	private void flush()
	{
		itsFieldWritePipeline.flush();
		for (Collector theCollector : itsCollectors) if (theCollector != null) theCollector.flush();
	}
	
	public void indexTrace() 
	{
		DBSideIOThread theIOThread;
		try
		{
			theIOThread = new DBSideIOThread(itsConfig, itsDatabase, new FileInputStream(itsEventsFile), null)
			{
				@Override
				protected EventCollector createCollector(int aThreadId)
				{
					return Indexer.this.createCollector(aThreadId);
				}
			};
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		
		try
		{
			theIOThread.run();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
			
		flush();
		Stats.print();
	}
	
	private void replaySnapshot(LocalsSnapshot aSnapshot, EventList aEventList) throws IOException
	{
		FileInputStream fis = new FileInputStream(itsEventsFile);
		long theBytesToSkip = aSnapshot.getPacketStartOffset();
		while(theBytesToSkip > 0) theBytesToSkip -= fis.skip(theBytesToSkip);
		
		final ReifyEventCollector theCollector = new ReifyEventCollector(aEventList);
		final boolean[] theCollectorCreated = {false};
		
		DBSideIOThread theIOThread = new DBSideIOThread(itsConfig, itsDatabase, fis, aSnapshot, itsPartialReplayerLoader)
		{
			@Override
			protected EventCollector createCollector(int aThreadId)
			{
				if (theCollectorCreated[0]) throw new RuntimeException(); // Only one thread
				theCollectorCreated[0] = true;
				return theCollector;
			}
		};
		
		theIOThread.setInitialSkip(aSnapshot.getPacketOffset());
		theIOThread.run();
	}
	
	public Block getBlock(int aThreadId, long aBlockId)
	{
		return itsBlocksBuffer.get(new BlockKey(aThreadId, aBlockId));
	}
	
	private static void timedGc()
	{
		long t0 = System.currentTimeMillis();
		System.gc();
		long t1 = System.currentTimeMillis();
		Utils.println("GC: %dms", t1-t0);
	}
	
	private EventList partialReplay(int aThreadId, long aBlockId)
	{
		if (itsPartialReplayerLoader == null)
		{
			itsPartialReplayerLoader = new ReplayerLoader(
					DBSideIOThread.class.getClassLoader(), 
					itsConfig, 
					itsDatabase, false);
		}
		
		Utils.println("Replaying: %s %s", aThreadId, aBlockId);
		long t0 = System.currentTimeMillis();
		Collector theCollector = Utils.listGet(itsCollectors, aThreadId);
		SnapshotIndex theIndex = theCollector.itsSnapshotIndex;
		LocalsSnapshot theSnapshot = theIndex.getSnapshot(aBlockId++);
		EventList theEventList = new EventList(aThreadId, aBlockId);
		try
		{
			while(theSnapshot != null)
			{
				replaySnapshot(theSnapshot, theEventList);
				theSnapshot = theIndex.getSnapshot(aBlockId++);
			}
			long t1 = System.currentTimeMillis();
			Utils.println("Replay took %dms, yielded %d events", t1-t0, theEventList.size());
			return theEventList;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private final class Block
	{
		private final BlockKey itsKey;
		private final EventList itsEvents;
		
		public Block(BlockKey aKey, EventList aEvents)
		{
			itsKey = aKey;
			itsEvents = aEvents;
		}
		
		public BlockKey getKey()
		{
			return itsKey;
		}
		
		public EventList getEvents()
		{
			return itsEvents;
		}
		
	}
	
	private final class BlockKey
	{
		private final int itsThreadId;
		private final long itsBlockId;
		
		public BlockKey(int aThreadId, long aBlockId)
		{
			itsThreadId = aThreadId;
			itsBlockId = aBlockId;
		}
		
		public int getThreadId()
		{
			return itsThreadId;
		}
		
		public long getBlockId()
		{
			return itsBlockId;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (itsBlockId ^ (itsBlockId >>> 32));
			result = prime * result + itsThreadId;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			BlockKey other = (BlockKey) obj;
			if (!getOuterType().equals(other.getOuterType())) return false;
			if (itsBlockId != other.itsBlockId) return false;
			if (itsThreadId != other.itsThreadId) return false;
			return true;
		}

		private Indexer getOuterType()
		{
			return Indexer.this;
		}
	}
	
	private class BlocksBuffer extends MRUBuffer<BlockKey, Block>
	{
		public BlocksBuffer()
		{
			super(16);
		}

		@Override
		protected BlockKey getKey(Block aValue)
		{
			return aValue.getKey();
		}

		@Override
		protected Block fetch(BlockKey aKey)
		{
			EventList theEvents = partialReplay(aKey.getThreadId(), aKey.getBlockId());
			return new Block(aKey, theEvents);
		}
	}
	
	public static class EventRef
	{
		public final int threadId;
		public final long blockId;
		public final int positionInBlock;
		
		public EventRef(int aThreadId, long aBlockId, int aPositionInBlock)
		{
			threadId = aThreadId;
			blockId = aBlockId;
			positionInBlock = aPositionInBlock;
		}
	}
	
	/**
	 * Event collector for a given thread.
	 * @author gpothier
	 */
	private class Collector extends EventCollector
	{
		private final int itsThreadId;
		private final PerThreadIndex itsFieldsIndex;
		private final CFlowIndex itsCFlowIndex;
		private long itsLastSync;
		private int itsSnapshotSeq = 0;
		private final SnapshotIndex itsSnapshotIndex;
		
		public Collector(int aThreadId)
		{
			itsThreadId = aThreadId;
			if (aThreadId > 0)
			{
				itsFieldsIndex = itsFieldWritePipeline.getIndex(itsThreadId);
				itsCFlowIndex = new CFlowIndex(createPidSlot());
				itsSnapshotIndex = new SnapshotIndex("snapshots", createPidSlot());
			}
			else
			{
				itsFieldsIndex = null;
				itsCFlowIndex = null;
				itsSnapshotIndex = null;
			}
		}

		@Override
		public void fieldWrite(ObjectId aTarget, int aFieldSlotIndex)
		{
			// Must keep the id odd because of temp ids.
			long theId = aTarget != null ? aTarget.getId() + 2*aFieldSlotIndex : 1; 
			itsFieldsIndex.registerAccess(theId);
		}
		
		@Override
		public void arrayWrite(ObjectId aTarget, int aIndex)
		{
			// Must keep the id odd because of temp ids.
			long theId = aTarget.getId() + 2*aIndex; 
			itsFieldsIndex.registerAccess(theId);
		}
		
		@Override
		public void sync(long aTimestamp)
		{
			assert aTimestamp > itsLastSync : "last: "+itsLastSync+", current: "+aTimestamp;
			itsLastSync = aTimestamp;
			itsSnapshotSeq = 0;
			itsFieldsIndex.startBlock(aTimestamp);
		}
		
		@Override
		public void localsSnapshot(LocalsSnapshot aSnapshot)
		{
			if (itsSnapshotSeq == 0) itsCFlowIndex.snapshot(itsLastSync);
			itsSnapshotIndex.addSnapshot(itsLastSync+itsSnapshotSeq, aSnapshot);
			// There can be several snapshots for the same block 
			// (because of mandatory snapshots after method calls).
			// So we must differentiate them.
			itsSnapshotSeq++; 
		}
		
		@Override
		public void enter(int aBehaviorId, int aArgsCount)
		{
			itsCFlowIndex.enter();
		}
		
		@Override
		public void exit()
		{
			itsCFlowIndex.exit();
		}
		
		@Override
		public void exitException()
		{
			itsCFlowIndex.exit();
		}
		
		@Override
		public void registerString(ObjectId aId, String aString)
		{
			// Only one collector receives these events so no synchronization issues
			Indexer.this.registerString(aId, aString);
		}
		
		/**
		 * Waits until all the processes associated with this collector are finished.
		 */
		public void flush()
		{
			if (itsCFlowIndex != null) itsCFlowIndex.flush();
		}
	}
	
	private static class InspectionBenchData
	{
		private long itsTotalTime;
		private long itsMaxTime;
		private long itsTotalSkip;
		private long itsMaxSkip;
		private int itsOperations;
		
		public void operation(long aTime, long aSkip)
		{
			assert aTime >= 0;
			assert aSkip >= 0;
			itsTotalTime += aTime;
			itsTotalSkip += aSkip;
			if (aTime > itsMaxTime) itsMaxTime = aTime;
			if (aSkip > itsMaxSkip) itsMaxSkip = aSkip;
			itsOperations++;
		}
		
		public void print()
		{
			Utils.println(
					"Avg time: %f, Avg skip: %d, max time: %d, max skip: %d, operations: %d", 
					1f*itsTotalTime/itsOperations, 
					itsTotalSkip/itsOperations,
					itsMaxTime,
					itsMaxSkip,
					itsOperations);
		}
	}

	private static class StepBenchData
	{
		private long itsTotalTime;
		private long itsMaxTime;
		private long itsTotalSkip;
		private long itsMaxSkip;
		private int itsOperations;
		
		public void operation(long aTime, long aSkip)
		{
			assert aTime >= 0;
			assert aSkip >= 0;
			itsTotalTime += aTime;
			itsTotalSkip += aSkip;
			if (aTime > itsMaxTime) itsMaxTime = aTime;
			if (aSkip > itsMaxSkip) itsMaxSkip = aSkip;
			itsOperations++;
		}
		
		public void print()
		{
			Utils.println(
					"Avg time: %f, Avg skip: %d, max time: %d, max skip: %d, operations: %d", 
					1f*itsTotalTime/itsOperations, 
					itsTotalSkip/itsOperations,
					itsMaxTime,
					itsMaxSkip,
					itsOperations);
		}
	}
	
	private InspectionBenchData itsInspectionBenchData;
	
	private static class FieldData
	{
		public final ObjectId itsObjectId;
		public final int itsFieldSlotIndex;
		
		public FieldData(ObjectId aObjectId, int aFieldSlotIndex)
		{
			itsObjectId = aObjectId;
			itsFieldSlotIndex = aFieldSlotIndex;
		}
	}
	
	private void benchInspection()
	{
		// 1. Collect a few fields
		Set<FieldData> theAllFields = new HashSet<FieldData>();
		
		System.out.println("Collecting fields...");
		for(int i=0;i<itsCollectors.size();i++)
		{
			Collector theCollector = itsCollectors.get(i);
			if (theCollector == null) continue;
			
			Utils.println("Processing thread %d", i);

			CFlowIndex theIndex = theCollector.itsCFlowIndex;
			
			long thePosition = 0;
			
			Utils.println("Index size: %d", theIndex.size());
			long theDelta = Math.max(theIndex.size()/100, 1);
			while(thePosition < theIndex.size())
			{
				thePosition += theDelta;
				
				Set<FieldData> theFields = new HashSet<Indexer.FieldData>();
				
				long theLocalPos = thePosition;
				threshold:
				while(theLocalPos < theIndex.size())
				{
					EventList theEvents = cflow_positionToBlock(i, theLocalPos);
					
					for(int j=0;j<theEvents.size();j++)
					{
						if (theEvents.getEventType(j) == EventList.FieldReadEvent.TYPE)
						{
							FieldReadEvent theEvent = (FieldReadEvent) theEvents.getEvent(j);
							FieldData theFieldData = new FieldData(theEvent.getObjectId(), theEvent.getFieldId());
							theFields.add(theFieldData);
							if (theFields.size() == 100) break threshold;
						}
					}
					
					EventRef theLastEvent = new EventRef(theEvents.getThreadId(), theEvents.getBlockId(), theEvents.size()-1);
					theLocalPos = cflow_eventToPosition(theLastEvent)+1;
				}

				theAllFields.addAll(theFields);
			}
		}
		
		// 2. Measure inspection
		System.out.println("Measuring inspection...");
		itsInspectionBenchData = new InspectionBenchData();
		
		for(int i=0;i<itsCollectors.size();i++)
		{
			Collector theCollector = itsCollectors.get(i);
			if (theCollector == null) continue;
			
			Utils.println("Processing thread %d", i);

			CFlowIndex theIndex = theCollector.itsCFlowIndex;
			
			long thePosition = 0;
			
			Utils.println("Index size: %d", theIndex.size());
			while(thePosition < theIndex.size())
			{
				thePosition += theIndex.size()/100;
				
				EventRef theEvent = cflow_positionToEvent(i, thePosition);

				for (FieldData theFieldData : theAllFields)
				{
					
				}
			}
		}

		itsInspectionBenchData.print();

	}
	
	private void inspect(ObjectId aObjectId, int aFieldId, EventRef aReferenceEventRef)
	{
		
	}
	
	private StepBenchData itsStepBenchData;
	
	private void benchStepping()
	{
		itsStepBenchData = new StepBenchData();
		
		for(int i=0;i<itsCollectors.size();i++)
		{
			Collector theCollector = itsCollectors.get(i);
			if (theCollector == null) continue;
			
			Utils.println("Processing thread %d", i);

			CFlowIndex theIndex = theCollector.itsCFlowIndex;
			
			long thePosition = 0;
			
			Utils.println("Index size: %d", theIndex.size());
			// Choose a call event
			long theDelta = Math.max(theIndex.size()/100, 1);
			while(thePosition < theIndex.size())
			{
				thePosition += theDelta;

				while(thePosition < theIndex.size() && ! theIndex.isOpen(thePosition)) thePosition++;
				if (thePosition >= theIndex.size()) break;
				
				benchStepOver(i, thePosition);
			}
		}

		itsStepBenchData.print();
	}
	
	private void benchStepOver(int aThreadId, long aPosition)
	{
		try
		{
			EventRef theEventRef = cflow_positionToEvent(aThreadId, aPosition);
			cflow_findReturn(theEventRef);
			while(theEventRef != null) 
			{
				theEventRef = cflow_findParent(theEventRef);
				cflow_findReturn(theEventRef);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getMessage());
		}			
	}
	
	private EventRef cflow_findReturn(EventRef aEventRef)
	{
		long t0 = System.currentTimeMillis();

		Collector theCollector = Utils.listGet(itsCollectors, aEventRef.threadId);
		CFlowIndex theIndex = theCollector.itsCFlowIndex;

		long theCallPosition = cflow_eventToPosition(aEventRef);
		long theReturnPosition = theIndex.getClose(theCallPosition);
		EventRef theEventRef = theReturnPosition >= 0 ? cflow_positionToEvent(aEventRef.threadId, theReturnPosition) : null;
		
		long t1 = System.currentTimeMillis();
		
		itsStepBenchData.operation(t1-t0, theReturnPosition >= 0 ? theReturnPosition-theCallPosition : 0);

		return theEventRef;
	}
	
	private EventRef cflow_findParent(EventRef aEventRef)
	{
		long t0 = System.currentTimeMillis();

		Collector theCollector = Utils.listGet(itsCollectors, aEventRef.threadId);
		CFlowIndex theIndex = theCollector.itsCFlowIndex;

		long theEventPosition = cflow_eventToPosition(aEventRef);
		long theParentPosition = theIndex.getParent(theEventPosition);
		EventRef theEventRef = theParentPosition >= 0 ? cflow_positionToEvent(aEventRef.threadId, theParentPosition) : null;
		
		long t1 = System.currentTimeMillis();
		
		itsStepBenchData.operation(t1-t0, theEventPosition-theParentPosition);

		return theEventRef;
	}
	
	private long cflow_eventToPosition(EventRef aEventRef)
	{
		Collector theCollector = Utils.listGet(itsCollectors, aEventRef.threadId);
		CFlowIndex theIndex = theCollector.itsCFlowIndex;

		long thePosition = theIndex.getBlockStartPosition(aEventRef.blockId);
		Block theBlock = getBlock(aEventRef.threadId, aEventRef.blockId);
		EventList theEvents = theBlock.getEvents();
		
		for(int i=0;i<aEventRef.positionInBlock;i++)
		{
			if (theEvents.isCFlowEvent(i)) thePosition++;
		}
		
		return thePosition;
	}
	
	private EventRef cflow_positionToEvent(int aThreadId, long aPosition)
	{
		Collector theCollector = Utils.listGet(itsCollectors, aThreadId);
		CFlowIndex theIndex = theCollector.itsCFlowIndex;

		LongTuple theTuple = theIndex.getContainingBlock(aPosition);
		long theBlockId = theTuple.getData();
		long theStartPosition = theTuple.getKey();
		
		Block theBlock = getBlock(aThreadId, theBlockId);
		EventList theEvents = theBlock.getEvents();
		
		int thePositionInBlock = 0;
		while (theStartPosition < aPosition)
		{
			if (theEvents.isCFlowEvent(thePositionInBlock)) theStartPosition++;
			thePositionInBlock++;
		}

		return new EventRef(aThreadId, theBlockId, thePositionInBlock);
	}
	
	private EventList cflow_positionToBlock(int aThreadId, long aPosition)
	{
		Collector theCollector = Utils.listGet(itsCollectors, aThreadId);
		CFlowIndex theIndex = theCollector.itsCFlowIndex;
		
		LongTuple theTuple = theIndex.getContainingBlock(aPosition);
		long theBlockId = theTuple.getData();
		
		Block theBlock = getBlock(aThreadId, theBlockId);
		return theBlock.getEvents();
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		try
		{
			TODConfig theConfig = new TODConfig();
			File theEventsFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/events.raw");
			
			String theScopeMD5 = Utils.md5String(theConfig.get(TODConfig.SCOPE_TRACE_FILTER).getBytes());
			File theDbFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/db-"+theScopeMD5+".raw");
			
			IMutableStructureDatabase theDatabase = StructureDatabase.create(theConfig, theDbFile, true);
			
			PagedFile theIndexFile = MappedPagedFile.create(new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/index.tod"), true);
			final Indexer theIndexer = new Indexer(theConfig, theDatabase, theEventsFile, theIndexFile);

			theIndexer.indexTrace();
			theIndexer.benchStepping();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		Thread.sleep(1000);
		System.err.println("END");
	}
}
