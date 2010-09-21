package tod.impl.evdbng;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline;
import tod.impl.evdbng.db.fieldwriteindex.Pipeline.PerThreadIndex;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.replay2.EventCollector;
import tod.impl.server.DBSideIOThread;
import tod.impl.server.ObjectAccessDistributionEventCollector;
import zz.utils.Utils;

public class Indexer 
{
	private final PagedFile itsFile;
	private final Page itsDirectoryPage;
	
	private final Pipeline itsFieldWritePipeline;
	
	public Indexer(PagedFile aFile)
	{
		itsFile = aFile;
		if (itsFile.getPagesCount() == 0) itsDirectoryPage = itsFile.create();
		else itsDirectoryPage = itsFile.get(1);
		
		itsFieldWritePipeline = new Pipeline(new PidSlot(itsDirectoryPage, 0));
	}
	
	private Collector createCollector(int aThreadId)
	{
		return new Collector(aThreadId);
	}
	
	private class Collector extends EventCollector
	{
		private final int itsThreadId;
		private final PerThreadIndex itsIndex;
		
		public Collector(int aThreadId)
		{
			itsThreadId = aThreadId;
			itsIndex = itsFieldWritePipeline.getIndex(itsThreadId);
		}

		@Override
		public void fieldWrite(ObjectId aTarget, int aFieldId)
		{
			long theObjectId = aTarget != null ? aTarget.getId() : 0;
			assert theObjectId < 0xffffffffffffL;
			assert aFieldId < 0xffff;
			long theId = (theObjectId << 16) | aFieldId;
			itsIndex.registerAccess(theId);
		}
		
		@Override
		public void sync(long aTimestamp)
		{
			itsIndex.startBlock(aTimestamp);
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

			PagedFile theIndexFile = PagedFile.create(new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/index.tod"), true);
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
