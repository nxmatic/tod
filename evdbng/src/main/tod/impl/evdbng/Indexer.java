package tod.impl.evdbng;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.evdbng.db.cflowindex.CFlowIndex;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline.PerThreadIndex;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.mapped.MappedPagedFile;
import tod.impl.replay2.EventCollector;
import tod.impl.server.DBSideIOThread;
import zz.utils.Utils;

public class Indexer 
{
	private final PagedFile itsFile;
	private final Page itsDirectoryPage;
	
	private final Pipeline itsFieldWritePipeline;
	private List<Collector> itsCollectors = new ArrayList<Indexer.Collector>();
	
	private int itsDirectoryOffset = 0;
	
	public Indexer(PagedFile aFile)
	{
		itsFile = aFile;
		if (itsFile.getPagesCount() == 0) itsDirectoryPage = itsFile.create();
		else itsDirectoryPage = itsFile.get(1);
		
		itsFieldWritePipeline = new Pipeline(new PidSlot(itsDirectoryPage, itsDirectoryOffset));
		itsDirectoryOffset += PidSlot.size();
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
		itsCollectors.add(theCollector);
		return theCollector;
	}
	
	private void flush()
	{
		itsFieldWritePipeline.flush();
		for (Collector theCollector : itsCollectors) theCollector.flush();
	}
	
	private class Collector extends EventCollector
	{
		private final int itsThreadId;
		private final PerThreadIndex itsFieldsIndex;
		private final CFlowIndex itsCFlowIndex;
		private long itsLastSync;
		
		public Collector(int aThreadId)
		{
			itsThreadId = aThreadId;
			itsFieldsIndex = itsFieldWritePipeline.getIndex(itsThreadId);
			itsCFlowIndex = new CFlowIndex(createPidSlot());
		}

		@Override
		public void fieldWrite(ObjectId aTarget, int aFieldId)
		{
			long theObjectId = aTarget != null ? aTarget.getId() : 0;
			assert theObjectId < 0xffffffffffffL;
			assert aFieldId < 0xffff;
			long theId = (theObjectId << 16) | aFieldId;
			itsFieldsIndex.registerAccess(theId);
		}
		
		@Override
		public void arrayWrite(ObjectId aTarget, int aIndex)
		{
			long theObjectId = aTarget != null ? aTarget.getId() : 0;
			assert theObjectId < 0xffffffffffffL;
			assert aIndex < 0xffff;
			long theId = (theObjectId << 16) | aIndex;
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
		
		/**
		 * Waits until all the processes associated with this collector are finished.
		 */
		public void flush()
		{
			itsCFlowIndex.flush();
		}
	}

//	public static void main(String[] args) throws InterruptedException
//	{
//		try
//		{
//			TODConfig theConfig = new TODConfig();
//			PagedFile theIndexFile = MappedPagedFile.create(new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/index.tod"), true);
//			final Indexer theIndexer = new Indexer(theIndexFile);
//			
//			final ArrayList<Collector> theCollectors = new ArrayList<Collector>();
//		
//			long t0 = System.currentTimeMillis();
//			ObjectWriteSerializeCollector.replay(0, new OWSReplayer()
//			{
//				@Override
//				public EventCollector getCollector(int aThreadId)
//				{
//					Collector theCollector = Utils.listGet(theCollectors, aThreadId);
//					if (theCollector == null)
//					{
//						theCollector = theIndexer.createCollector(aThreadId);
//						Utils.listSet(theCollectors, aThreadId, theCollector);
//					}
//					return theCollector;
//				}
//			});
//			
//			theIndexer.flush();
//			
//			long t1 = System.currentTimeMillis();
//			Utils.println("Wall time: %.2fs", 0.001f*(t1-t0));
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace();
//		}
//		Thread.sleep(1000);
//		System.err.println("END");
//	}
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
			final Indexer theIndexer = new Indexer(theIndexFile);
			
			final Map<Integer, EventCollector> theCollectors = new HashMap<Integer, EventCollector>();  
			
			DBSideIOThread theIOThread = new DBSideIOThread(theConfig, theDatabase, new FileInputStream(theEventsFile), null)
			{
				@Override
				protected EventCollector createCollector(int aThreadId)
				{
					EventCollector theCollector = theIndexer.createCollector(aThreadId);
					theCollectors.put(aThreadId, theCollector);
					return theCollector;
				}
			};
			theIOThread.run();
			
			theIndexer.flush();
			
			System.out.println("Collectors:");
			for(Map.Entry<Integer, EventCollector> theEntry : theCollectors.entrySet())
			{
				System.out.println(theEntry.getKey() + ": " + theEntry.getValue());
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		Thread.sleep(1000);
		System.err.println("END");
	}
}
