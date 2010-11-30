package tod.impl.evdbng.db.file;

import java.io.File;

import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.Stats.Account;
import tod.impl.evdbng.db.file.classic.ClassicPagedFile;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;

public abstract class PagedFile
{
	public static final int PAGE_SIZE = DebuggerGridConfigNG.DB_PAGE_SIZE;
	
	private long itsReadCount = 0;
	private long itsWriteCount = 0;
	private long itsLastAccessedPage = -1;
	private long itsPageScattering = 0;
	private long itsScatteringCount = 0;

	public static PagedFile create(File aFile, boolean aTruncate)
	{
		return ClassicPagedFile.create(aFile, aTruncate);
	}
	
	public PagedFile()
	{
		Monitor.getInstance().register(this);
	}
	
	public void dispose()
	{
		Monitor.getInstance().unregister(this);
	}
	
	/**
	 * Returns the number of allocated pages.
	 */
	@Probe(key = "file page count", aggr = AggregationType.SUM)
	public abstract long getPagesCount();

	/**
	 * Returns the amount of storage, in bytes, occupied by this file.
	 */
	@Probe(key = "file storage", aggr = AggregationType.SUM)
	public long getStorageSpace()
	{
		return getPagesCount() * PAGE_SIZE;
	}
	
	/**
	 * Returns the actual file size.
	 */
	@Probe(key = "file size", aggr = AggregationType.SUM)
	public abstract long getFileSize();
	
	@Probe(key = "file written bytes", aggr = AggregationType.SUM)
	public long getWrittenBytes()
	{
		return itsWriteCount * PAGE_SIZE;
	}

	@Probe(key = "file read bytes", aggr = AggregationType.SUM)
	public long getReadBytes()
	{
		return itsReadCount * PAGE_SIZE;
	}
	
	@Probe(key = "file written pages", aggr = AggregationType.SUM)
	public long getWrittenPages()
	{
		return itsWriteCount;
	}
	
	@Probe(key = "file read pages", aggr = AggregationType.SUM)
	public long getReadPages()
	{
		return itsReadCount;
	}
	
	@Probe(key = "read/write ratio (%)", aggr = AggregationType.AVG)
	public float getRatio()
	{
		return 100f*itsReadCount/itsWriteCount;
	}
	
	@Probe(key = "page access scattering", aggr = AggregationType.SUM)
	public float getScattering()
	{
		return 1f * itsPageScattering / itsScatteringCount;
	}

	protected void updateScattering(long aId)
	{
		if (itsLastAccessedPage >= 0)
		{
			long theDistance = Math.abs(itsLastAccessedPage - aId);
			itsPageScattering += theDistance;
			itsScatteringCount++;
		}
		itsLastAccessedPage = aId;
	}
	
	protected void incWrite()
	{
		itsWriteCount++;
	}
	
	protected void incRead()
	{
		itsReadCount++;
	}
	
	/**
	 * Returns the page with the specified id.
	 */
	public abstract Page get(int aPageId);
	
	/**
	 * Creates a new page.
	 * @param aAccount The account the charge for the page creation.
	 */
	public abstract Page create(Account aAccount);
	
	/**
	 * Flushes all dirty buffers to disk
	 */
	public abstract void flush();
	
	/**
	 * Clears this paged file.
	 */
	public abstract void clear();
	
	/**
	 * Indicates to the page manager that this page is not going to be used anymore.
	 * This is optional, not calling it has no adverse effects, and the effect of calling
	 * it is a potiential increase in efficiency.
	 */
	public abstract void free(Page aPage);
}
