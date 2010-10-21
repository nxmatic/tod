package tod.impl.evdbng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.evdbng.db.SnapshotIndex;
import tod.impl.evdbng.db.StringIndex;
import tod.impl.evdbng.db.cflowindex.CFlowIndex;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline.PerThreadIndex;
import tod.impl.evdbng.db.file.DeltaBTree;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.mapped.MappedPagedFile;
import tod.impl.replay2.EventCollector;
import tod.impl.replay2.LocalsSnapshot;
import tod.impl.replay2.ReifyEventCollector;
import tod.impl.replay2.ReifyEventCollector.SyncEvent;
import tod.impl.replay2.ReplayerLoader;
import tod.impl.replay2.ReifyEventCollector.Event;
import tod.impl.server.DBSideIOThread;
import zz.utils.Utils;

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
	
	private int itsDirectoryOffset = 0;
	
	private ReplayerLoader itsPartialReplayerLoader;
	
	public Indexer(TODConfig aConfig, IMutableStructureDatabase aDatabase, File aEventsFile, PagedFile aIndexFile)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsEventsFile = aEventsFile;
		itsIndexFile = aIndexFile;
		if (itsIndexFile.getPagesCount() == 0) itsDirectoryPage = itsIndexFile.create();
		else itsDirectoryPage = itsIndexFile.get(1);
		
		itsFieldWritePipeline = new Pipeline(createPidSlot());
		itsStringIndex = new StringIndex("strings", createPidSlot());
	}
	
	private PidSlot createPidSlot()
	{
		PidSlot theSlot = new PidSlot(itsDirectoryPage, itsDirectoryOffset);
		itsDirectoryOffset += PidSlot.size();
		return theSlot;
	}
	
	private Collector createCollector(int aThreadId)
	{
		Collector theCollector = new Collector(aThreadId);
		Utils.listSet(itsCollectors, aThreadId, theCollector);
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
		try
		{
			DBSideIOThread theIOThread = new DBSideIOThread(itsConfig, itsDatabase, new FileInputStream(itsEventsFile), null)
			{
				@Override
				protected EventCollector createCollector(int aThreadId)
				{
					return createCollector(aThreadId);
				}
			};
			theIOThread.run();
			
			flush();
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void replaySnapshot(LocalsSnapshot aSnapshot, List<Event> aEvents) throws IOException
	{
		FileInputStream fis = new FileInputStream(itsEventsFile);
		long theBytesToSkip = aSnapshot.getPacketStartOffset();
		while(theBytesToSkip > 0) theBytesToSkip -= fis.skip(theBytesToSkip);
		
		final ReifyEventCollector theCollector = new ReifyEventCollector(aEvents);
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
	
	public List<Event> partialReplay(int aThreadId, long aBlockId)
	{
		if (itsPartialReplayerLoader == null)
		{
			itsPartialReplayerLoader = new ReplayerLoader(
					DBSideIOThread.class.getClassLoader(), 
					itsConfig, 
					itsDatabase, false);
		}
		
		Collector theCollector = Utils.listGet(itsCollectors, aThreadId);
		SnapshotIndex theIndex = theCollector.itsSnapshotIndex;
		LocalsSnapshot theSnapshot = theIndex.getSnapshot(aBlockId++);
		List<Event> theEvents = new ArrayList<ReifyEventCollector.Event>();
		try
		{
			while(theSnapshot != null)
			{
				replaySnapshot(theSnapshot, theEvents);
				theSnapshot = theIndex.getSnapshot(aBlockId++);
			}
			assert theEvents.get(theEvents.size()-1) instanceof SyncEvent;
			return theEvents;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
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
			itsFieldsIndex.startBlock(aTimestamp);
			itsCFlowIndex.sync(aTimestamp);
		}
		
		@Override
		public void localsSnapshot(LocalsSnapshot aSnapshot)
		{
			itsSnapshotIndex.addSnapshot(itsLastSync, aSnapshot);
			// There can be several snapshots for the same block 
			// (because of mandatory snapshots after method calls).
			// So we must differentiate them.
			itsLastSync++; 
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
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		Thread.sleep(1000);
		System.err.println("END");
	}
}
