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
package tod.impl.evdb1.db;

import static tod.impl.evdb1.DebuggerGridConfig1.DB_MAX_INDEX_LEVELS;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_POINTER_BITS;
import static tod.impl.evdb1.DebuggerGridConfig1.EVENT_TIMESTAMP_BITS;
import tod.agent.AgentUtils;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.db.file.IndexTuple;
import tod.impl.evdb1.db.file.IndexTupleCodec;
import tod.impl.evdb1.db.file.TupleCodec;
import tod.impl.evdb1.db.file.TupleFinder;
import tod.impl.evdb1.db.file.TupleIterator;
import tod.impl.evdb1.db.file.TupleWriter;
import tod.impl.evdb1.db.file.PageBank.Page;
import tod.impl.evdb1.db.file.PageBank.PageBitStruct;
import tod.impl.evdb1.db.file.TupleFinder.Match;
import tod.impl.evdb1.db.file.TupleFinder.NoMatch;
import zz.utils.ArrayStack;
import zz.utils.Stack;
import zz.utils.bit.BitStruct;
import zz.utils.bit.BitUtils;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Probe;

/**
 * Implementation of a hierarchical index on an attribute value,
 * for instance a particular behavior id.
 * @author gpothier
 */
public class HierarchicalIndex<T extends IndexTuple>
{
	private final String itsName;
	private Page itsRootPage;
	private long itsFirstLeafPageId;
	private int itsRootLevel;
	private MyTupleWriter[] itsTupleWriters = new MyTupleWriter[DB_MAX_INDEX_LEVELS];
	
	private final TupleCodec<T> itsTupleCodec;
	private final HardPagedFile itsFile;
	
	/**
	 * The timestamp of the last added tuple
	 */
	private long itsLastKey = 0;
	
	/**
	 * Number of pages per level
	 */
//	private int[] itsPagesCount = new int[DB_MAX_INDEX_LEVELS];
	
	private long itsLeafTupleCount = 0;

	public HierarchicalIndex(String aName, TupleCodec<T> aTupleCodec, HardPagedFile aFile) 
	{
		itsName = aName;
		itsTupleCodec = aTupleCodec;
		itsFile = aFile;
		
		// Init pages
		itsTupleWriters[0] = new MyTupleWriter<T>(getFile(), getTupleCodec(), 0);
		itsRootPage = itsTupleWriters[0].getCurrentPage();
		itsFirstLeafPageId = itsRootPage.getPageId();
		itsRootLevel = 0;
	}
	
	/**
	 * Reconstructs a previously-written index from the given struct.
	 */
	public HierarchicalIndex(String aName, TupleCodec<T> aTupleCodec, HardPagedFile aFile, BitStruct aStoredIndexStruct)
	{
		itsName = aName;
		itsTupleCodec = aTupleCodec;
		itsFile = aFile;

		int thePagePointerSize = getFile().getPagePointerSize();
		long theRootPageId = aStoredIndexStruct.readLong(thePagePointerSize);
		itsRootPage = getFile().get(theRootPageId);
		
		itsFirstLeafPageId = aStoredIndexStruct.readLong(thePagePointerSize);
		itsLastKey = aStoredIndexStruct.readLong(64);
		itsLeafTupleCount = aStoredIndexStruct.readLong(32);
		itsRootLevel = aStoredIndexStruct.readInt(BitUtils.log2ceil(DB_MAX_INDEX_LEVELS));
		
		for (int i=0;i<DB_MAX_INDEX_LEVELS;i++)
		{
			long thePageId = aStoredIndexStruct.readLong(thePagePointerSize);
			if (thePageId != 0)
			{
				int thePos = aStoredIndexStruct.readInt(BitUtils.log2ceil(getFile().getPageSize()*8));
				itsTupleWriters[i] = new MyTupleWriter(
						getFile(),
						i == 0 ? getTupleCodec() : InternalTupleCodec.getInstance(),
						i,
						getFile().get(thePageId-1),
						thePos);
			}
		}
	}

	private TupleCodec<T> getTupleCodec()
	{
		return itsTupleCodec;
	}
	
	private HardPagedFile getFile()
	{
		return itsFile;
	}
	
	
	/**
	 * Writes this index to the given struct so that it can be reloaded
	 * later.
	 */
	public void writeTo(BitStruct aBitStruct)
	{
		int thePagePointerSize = getFile().getPagePointerSize();
		aBitStruct.writeLong(itsRootPage.getPageId(), thePagePointerSize);
		aBitStruct.writeLong(itsFirstLeafPageId, thePagePointerSize);
		aBitStruct.writeLong(itsLastKey, 64);
		aBitStruct.writeLong(itsLeafTupleCount, 32);
		aBitStruct.writeInt(itsRootLevel, BitUtils.log2ceil(DB_MAX_INDEX_LEVELS));
		
		for (int i=0;i<DB_MAX_INDEX_LEVELS;i++)
		{
			MyTupleWriter theWriter = itsTupleWriters[i];
			PageBitStruct theStruct = theWriter != null ? theWriter.getCurrentStruct() : null;
			
			aBitStruct.writeLong(
					theStruct != null ? theStruct.getPage().getPageId()+1 : 0, 
					thePagePointerSize);
			
			aBitStruct.writeInt(
					theStruct != null ? theStruct.getPos() : 0, 
					BitUtils.log2ceil(getFile().getPageSize()*8));
		}
	}
	
	/**
	 * Returns the size, in bits of the serialized data for a {@link HierarchicalIndex}
	 */
	public static int getSerializedSize(HardPagedFile aFile)
	{
		int thePagePointerSize = aFile.getPagePointerSize();
		int theResult = 0;

		theResult += thePagePointerSize;
		theResult += thePagePointerSize;
		theResult += 64;
		theResult += 32;
		theResult += BitUtils.log2ceil(DB_MAX_INDEX_LEVELS);
		
		theResult += DB_MAX_INDEX_LEVELS * (thePagePointerSize+BitUtils.log2ceil(aFile.getPageSize()*8));

		return theResult;
	}
	
	/**
	 * Returns the level number that corresponds to the root page.
	 * This is equivalent to the height of the index.
	 */
	public int getRootLevel()
	{
		return itsRootLevel;
	}

	/**
	 * Returns the first tuple that has a key greater or equal
	 * than the specified key, if any.
	 * @param aExact If true, only a tuple with exactly the specified
	 * key is returned.
	 * @return A matching tuple, or null if none is found.
	 */
	public T getTupleAt(long aKey, boolean aExact)
	{
		TupleIterator<T> theIterator = getTupleIterator(aKey);
		if (! theIterator.hasNext()) return null;
		T theTuple = theIterator.next();
		if (aExact && theTuple.getKey() != aKey) return null;
		else return theTuple;
	}
	
	/**
	 * Returns an iterator that returns all tuples whose key
	 * is greater than or equal to the specified key.
	 * @param aKey Requested first key, or 0 to start
	 * at the beginning of the list.
	 */
	public TupleIterator<T> getTupleIterator(long aKey)
	{
//		System.out.println("Get    "+aTimestamp);
		
		// The tuplefinder considers empty tuples to have key Long.MAX_VALUE
		if (aKey == Long.MAX_VALUE) aKey--;
		
		if (aKey == 0)
		{
			PageBitStruct theBitStruct = getFile().get(itsFirstLeafPageId).asBitStruct();
			return new TupleIterator<T>(
					this,
					getFile(), 
					getTupleCodec(), 
					theBitStruct);
		}
		else
		{
			int theLevel = itsRootLevel;
			Page thePage = itsRootPage;
			while (theLevel > 0)
			{
//				System.out.println("Level: "+theLevel);
				InternalTuple theTuple = TupleFinder.findTuple(
						thePage.asBitStruct(), 
						DB_PAGE_POINTER_BITS,
						aKey, 
						InternalTupleCodec.getInstance(),
						Match.FIRST,
						NoMatch.BEFORE);
				
				if (theTuple == null) 
				{
					// The first tuple of this index is after the specified key
					thePage = getFile().get(itsFirstLeafPageId);
					PageBitStruct theBitStruct = thePage.asBitStruct();
					return new TupleIterator<T>(
							this,
							getFile(), 
							getTupleCodec(), 
							theBitStruct);
				}
				
				thePage = getFile().get(theTuple.getPagePointer());
				theLevel--;
			}
			
			PageBitStruct theBitStruct = thePage.asBitStruct();
			int theIndex = TupleFinder.findTupleIndex(
					theBitStruct,
					DB_PAGE_POINTER_BITS,
					aKey, 
					getTupleCodec(),
					Match.FIRST,
					NoMatch.AFTER);
			
			if (theIndex < 0) 
			{
				// The last tuple is before the requested key.
				// The index of the last tuple is -index-1
				// We want an iterator that is past the last tuple.
				theIndex = -theIndex;
			}

			theBitStruct.setPos(theIndex * getTupleCodec().getTupleSize());
			TupleIterator<T> theIterator = new TupleIterator<T>(
					this,
					getFile(), 
					getTupleCodec(), 
					theBitStruct);
			
			T theTuple = theIterator.peekNext();
			if (theIterator.hasNext() && theTuple.getKey() < aKey)
				theIterator.next();
			
			return theIterator;
		}
	}
	
	/**
	 * Adds a tuple to this index.
	 */
	public void add(T aTuple)
	{
		assert checkTimestamp(aTuple);
		add(aTuple, 0, getTupleCodec());
		for (MyTupleWriter theTupleWriter : itsTupleWriters)
		{
			if (theTupleWriter != null) theTupleWriter.getCurrentPage().use();
		}
		
		itsLeafTupleCount++;
	}
	
	/**
	 * Checks that the newly added tuple's key is greater than
	 * the last key.
	 */
	private boolean checkTimestamp(T aTuple)
	{
		long theKey = aTuple.getKey();
		assert theKey >= itsLastKey : ""+theKey +" "+  itsLastKey + " -dif="+(theKey-itsLastKey);
		itsLastKey = theKey;
		return true;
	}
	
	private <T1 extends IndexTuple> void add(T1 aTuple, int aLevel, TupleCodec<T1> aCodec)
	{
		MyTupleWriter<T1> theWriter = itsTupleWriters[aLevel];
		if (theWriter == null)
		{
			assert aLevel == itsRootLevel+1;
			itsRootLevel = aLevel;
			theWriter = new MyTupleWriter<T1>(getFile(), aCodec, aLevel);
			itsTupleWriters[aLevel] = theWriter;
			itsRootPage = itsTupleWriters[aLevel].getCurrentPage();
		}
		
		theWriter.add(aTuple);
	}
	
	private TupleIterator<? extends IndexTuple> createTupleIterator(PageBitStruct aPage, int aLevel)
	{
		return aLevel > 0 ?
				new TupleIterator<InternalTuple>(this, getFile(), InternalTupleCodec.getInstance(), aPage)
				: new TupleIterator<T>(this, getFile(), getTupleCodec(), aPage);
	}
	
	private String printIndex()
	{
		StringBuilder theBuilder = new StringBuilder();
		int theLevel = itsRootLevel;
		Page theCurrentPage = itsRootPage;
		Page theFirstChildPage = null;
		
		while (theLevel > 0)
		{
			theBuilder.append("Level "+theLevel+"\n");
			
			TupleIterator<InternalTuple> theIterator = new TupleIterator<InternalTuple>(
					this,
					getFile(), 
					InternalTupleCodec.getInstance(), 
					theCurrentPage.asBitStruct());
			
			int i = 0;
			while (theIterator.hasNext())
			{
				InternalTuple theTuple = theIterator.next();
				if (theFirstChildPage == null)
				{
					theFirstChildPage = getFile().get(theTuple.getPagePointer());
				}
				
				theBuilder.append(AgentUtils.formatTimestamp(theTuple.getKey()));
				theBuilder.append('\n');
				i++;
			}
			theBuilder.append(""+i+" entries\n");
			
			theCurrentPage = theFirstChildPage;
			theFirstChildPage = null;
			theLevel--;
		}
		
		return theBuilder.toString();
	}
	
	/**
	 * Realizes a fast counting of the tuples of this index, using
	 * upper-level indexes when possible.
	 */
	public long[] fastCountTuples(
			long aT1, 
			long aT2, 
			int aSlotsCount) 
	{
		return new TupleCounter(aT1, aT2, aSlotsCount).count();
	}
	
	/**
	 * Data structure used by {@link HierarchicalIndex#fastCountTuples(long, long, int)}.
	 * @author gpothier
	 */
	private static class LevelData
	{
		public TupleIterator<? extends IndexTuple> iterator;
		public IndexTuple lastTuple;
		
		/**
		 * Minimum number of tuples to read at this level.
		 */
		public int remaining;
		
		public LevelData(TupleIterator< ? extends IndexTuple> aIterator, int aRemaining)
		{
			iterator = aIterator;
			remaining = aRemaining;
		}

		public IndexTuple next()
		{
			lastTuple = iterator.next();
			return lastTuple;
		}
	}

	
	/**
	 * Returns the total number of pages occupied by this index
	 */
	@Probe(key = "index pages", aggr = AggregationType.SUM)	
	public int getTotalPageCount()
	{
		int theCount = 0;
		for (TupleWriter theWriter : itsTupleWriters) theCount += theWriter.getPagesCount();
		return theCount;
	}
	
	@Probe(key = "leaf index tuples", aggr = AggregationType.SUM)
	public long getLeafTupleCount()
	{
		return itsLeafTupleCount;
	}
	
	public int getPageSize()
	{
		return getFile().getPageSize();
	}
	
	@Probe(key = "index storage", aggr = AggregationType.SUM)
	public long getStorageSpace()
	{
		return getTotalPageCount() * getPageSize();
	}
	
	@Override
	public String toString()
	{
		return getClass().getName()+":"+itsName;
	}

	/**
	 * Codec for {@link InternalTuple}.
	 * @author gpothier
	 */
	private static class InternalTupleCodec extends IndexTupleCodec<InternalTuple>
	{
		private static InternalTupleCodec INSTANCE = new InternalTupleCodec();

		public static InternalTupleCodec getInstance()
		{
			return INSTANCE;
		}

		private InternalTupleCodec()
		{
		}
		
		@Override
		public int getTupleSize()
		{
			return super.getTupleSize() + DB_PAGE_POINTER_BITS;
		}

		@Override
		public InternalTuple read(BitStruct aBitStruct)
		{
			return new InternalTuple(aBitStruct);
		}
	}
	
	/**
	 * Tuple for internal index nodes.
	 */
	private static class InternalTuple extends IndexTuple
	{
		/**
		 * Page pointer
		 */
		private long itsPagePointer;

		public InternalTuple(long aKey, long aPagePointer)
		{
			super(aKey);
			itsPagePointer = aPagePointer;
		}
		
		public InternalTuple(BitStruct aBitStruct)
		{
			super(aBitStruct);
			itsPagePointer = aBitStruct.readLong(DB_PAGE_POINTER_BITS);
		}

		@Override
		public void writeTo(BitStruct aBitStruct)
		{
			super.writeTo(aBitStruct);
			aBitStruct.writeLong(getPagePointer(), DB_PAGE_POINTER_BITS);
		}
		
		@Override
		public int getBitCount()
		{
			return super.getBitCount() + DB_PAGE_POINTER_BITS;
		}
		
		public long getPagePointer()
		{
			return itsPagePointer;
		}
		
		@Override
		public String toString()
		{
			return String.format("%s: k=%d p=%d",
					getClass().getSimpleName(),
					getKey(),
					getPagePointer());
		}
	}
	
	private class MyTupleWriter<T extends IndexTuple> extends TupleWriter<T>
	{
		private final int itsLevel;
		
		public MyTupleWriter(HardPagedFile aFile, TupleCodec<T> aTupleCodec, int aLevel)
		{
			this(aFile, aTupleCodec, aLevel, aFile.create(), 0);
		}
		
		public MyTupleWriter(
				HardPagedFile aFile,
				TupleCodec<T> aTupleCodec, 
				int aLevel,
				Page aPage,
				int aPos)
		{
			super(aFile, aTupleCodec, aPage, aPos);
			itsLevel = aLevel;
		}


		@Override
		protected void newPageHook(PageBitStruct aStruct, long aNewPageId)
		{
			// If this is the first time we finish a page at this level,
			// we must update upper level index.
			if (itsRootLevel == itsLevel)
			{
				// Read timestamp of first tuple
				aStruct.setPos(0);
				long theTimestamp = aStruct.readLong(EVENT_TIMESTAMP_BITS);
				
				HierarchicalIndex.this.add(
						new InternalTuple(theTimestamp, aStruct.getPage().getPageId()),
						itsLevel+1, 
						InternalTupleCodec.getInstance());
			}
		}

		@Override
		protected void startPageHook(PageBitStruct aStruct, T aTuple)
		{
			if (itsRootLevel > itsLevel)
			{
				// When we write the first tuple of a page we also update indexes.
				long theKey = aTuple.getKey();
				HierarchicalIndex.this.add(
						new InternalTuple(theKey, aStruct.getPage().getPageId()),
						itsLevel+1, 
						InternalTupleCodec.getInstance());
			}
		}
		
	}

	/**
	 * Implementation of fast tuple counting. 
	 * @author gpothier
	 */
	private class TupleCounter
	{
		private final long itsK1;
		private final long itsK2;
		private final int itsSlotsCount;
		
		/**
		 * k2-k1
		 */
		private long itsDK;
		
		private Stack<LevelData> itsStack = new ArrayStack<LevelData>();

		private float[] itsCounts;

		private int[] itsTuplesBetweenPairs;
		private int itsTuplesPerPage0;
		private int itsTuplesPerPageU;
		private LevelData itsCurrentLevel;
		private IndexTuple itsLastTuple;
		private int itsCurrentHeight;
		
		public TupleCounter(long aK1, long aK2, int aSlotsCount)
		{
			if (aK1 >= aK2) throw new IllegalArgumentException();
			itsK1 = aK1;
			itsK2 = aK2;
			itsSlotsCount = aSlotsCount;
			itsDK = (aK2-aK1)/aSlotsCount;
			
			precomputeTuplesBetweenPairs();
			itsCounts = new float[aSlotsCount];

			itsStack.push(new LevelData(
					createTupleIterator(itsRootPage.asBitStruct(), itsRootLevel), 
					0));

		}

		/**
		 * Compute the number of level-0 tuples between each pair of
		 * level-i tuples, for each level.
		 */
		private void precomputeTuplesBetweenPairs()
		{
			itsTuplesBetweenPairs = new int[itsRootLevel+1];
			
			itsTuplesPerPage0 = TupleFinder.getTuplesPerPage(
					getFile().getPageSize()*8,
					DB_PAGE_POINTER_BITS,
					getTupleCodec());
			
			itsTuplesPerPageU = TupleFinder.getTuplesPerPage(
					getFile().getPageSize()*8,
					DB_PAGE_POINTER_BITS,
					InternalTupleCodec.getInstance());
			
			for (int i=0;i<=itsRootLevel;i++)
			{
				itsTuplesBetweenPairs[i] = i > 0 ?
						itsTuplesPerPage0 * BitUtils.powi(itsTuplesPerPageU, i-1)
						: 1;
			}
		}
		
		private void drillDown()
		{
			InternalTuple theTuple = (InternalTuple) itsLastTuple;
			Page theChildPage = getFile().get(theTuple.getPagePointer());
			
			itsStack.push(new LevelData(
					createTupleIterator(theChildPage.asBitStruct(), itsCurrentHeight-1),
					itsCurrentHeight > 1 ? itsTuplesPerPageU : itsTuplesPerPage0));
		}

		public long[] count() 
		{
//			System.out.println(printIndex());

//			System.out.println("dt: "+AgentUtils.formatTimestampU(dt));
			
			long t = itsK1;
			
			boolean theFinished = false;
			
			while(! theFinished)
			{
				itsCurrentLevel = itsStack.peek();
				itsCurrentHeight = itsRootLevel - itsStack.size() + 1;
				long theStart;
				long theEnd;

				itsLastTuple = itsCurrentLevel.lastTuple;
				if (itsCurrentLevel.iterator.hasNext()) 
				{
					IndexTuple theCurrent = itsCurrentLevel.next();
					theEnd = theCurrent.getKey();
				}
				else if (itsCurrentHeight > 0)
				{
					drillDown();
					continue;
				}
				else
				{
					theFinished = true;
					theEnd = itsLastKey;
				}
				
				if (itsLastTuple == null || theEnd < t) continue;
				theStart = itsLastTuple.getKey();

				itsCurrentHeight = itsRootLevel - itsStack.size() + 1;
				long dtPair = theEnd-theStart;
//				System.out.println("dtPair: "+AgentUtils.formatTimestampU(dtPair));
				
				if (itsCurrentHeight > 0 && dtPair > itsDK/2)
				{
					drillDown();
					continue;
				}
				
				t = theStart;
				int theSlot = (int)(((t - itsK1) * itsSlotsCount) / (itsK2 - itsK1));
				if (theSlot < 0) continue;
				if (theSlot >= itsSlotsCount) break;
				
				if (itsCurrentHeight == 0)
				{
					itsCounts[theSlot] += 1;
				}
				else
				{
					int theCount = itsTuplesBetweenPairs[itsCurrentHeight];
					
					distributeCounts(theCount, theStart, theEnd, theSlot);
				}

				itsCurrentLevel.remaining--;
				if (itsCurrentLevel.remaining == 0)
				{
					itsStack.pop();
				}
			}
			
			long[] theResult = new long[itsSlotsCount];
			for (int i = 0; i < itsCounts.length; i++)
			{
				float f = itsCounts[i];
				theResult[i] = (long) f;
			}
			
			return theResult;
		}

		/**
		 * Distribute a number of events across one or several slots.
		 * @param theCount The number of events to distribute
		 * @param theStart Beginning of the interval in which the events occurred
		 * @param theEnd End of the interval
		 * @param theSlot The main receiving slot
		 */
		private void distributeCounts(
				int theCount, 
				long theStart, 
				long theEnd, 
				int theSlot)
		{
			long theSlotStart = itsK1 + theSlot * (itsK2 - itsK1) / itsSlotsCount;
			long theSlotEnd = theSlotStart + itsDK;
			
			long dtPair = theEnd-theStart;
			
			if (theStart < theSlotStart)
			{
				// We overflow before
				long theBefore = theSlotStart-theStart;
				float theRatio = 1f * theBefore / dtPair;
				if (theSlot > 0) itsCounts[theSlot-1] += theRatio * theCount;
				itsCounts[theSlot] += (1f-theRatio) * theCount;
			}
			else if (theEnd > theSlotEnd)
			{
				// We overflow after
				long theAfter = theEnd-theSlotEnd;
				float theRatio = 1f * theAfter / dtPair;
				if (theSlot < itsCounts.length-1) itsCounts[theSlot+1] += theRatio * theCount;
				itsCounts[theSlot] += (1f-theRatio) * theCount;
			}
			else
			{
				// No overflow - note the invariant dtPair > dt/2
				itsCounts[theSlot] += theCount;
			}
		}
		
	}
}
