package tod.impl.evdbng.db.file;

import java.util.ArrayList;

import gnu.trove.TLongHashSet;
import gnu.trove.TLongIntHashMap;
import gnu.trove.TLongIntProcedure;


public class ObjectAccessIndex
{
	private static final int MAX_MEMORY = 64*1024*1024;
	
	/**
	 * A span is a replayable range. It corresponds to a document in Managing Gigabytes.
	 */
	private long itsCurrentSpan = 0;
	
	/**
	 * For each object, stores the number of occurrences of the object in the current chunk.
	 */
	private MyLongIntHashMap itsChunkObjectCounts = new MyLongIntHashMap();
	
	/**
	 * Stores the data of finished spans within the chunk.
	 */
	private ArrayList<SpanData> itsChunkSpanDatas = new ArrayList<SpanData>();
	
	private int itsSpanDatasMemory = 0;
	
	/**
	 * The set of objects accessed in the current span.
	 */
	private TLongHashSet itsSpanObjects = new TLongHashSet();
	
	public void startSpan()
	{
		if (itsCurrentSpan > 0) closeSpan();
		itsCurrentSpan++;
	}
	
	private void closeSpan()
	{
		itsChunkSpanDatas.add(new SpanData(itsCurrentSpan, itsSpanObjects.toArray()));
		itsSpanDatasMemory += 4+8+8+4+(8*itsSpanObjects.size()); // pointer + oops overhead + span number + array pointer + array
		itsSpanObjects.clear();
		
		// check if we must close the current chunk
		if (itsSpanDatasMemory + itsChunkObjectCounts.getMemory() > MAX_MEMORY) closeChunk();
	}
	
	private void closeChunk()
	{
		// Obtain the arrays of ids and corresponding counts, and sort them on id.
		int theSize = itsChunkObjectCounts.size();
		long[] theIds = itsChunkObjectCounts.keys();
		int[] theCounts = itsChunkObjectCounts.getValues(); 
		new LongIntSorter(theIds, theCounts).sort();
		
		// Setup lexicon (running sum of the counts array)
		int theCurrentCount = 0;
		int[] theStartPositions = new int[theSize]; // Starting slot of each term (aka object id)
		int[] theCurrentPositions = new int[theSize]; // Current insertion slot of each term
		
		for(int i=0;i<theSize;i++)
		{
			theStartPositions[i] = theCurrentCount;
			theCurrentPositions[i] = theCurrentCount;
			itsChunkObjectCounts.put(theIds[i], i); // Make the map associate ids to their sorted index
			theCurrentCount += theCounts[i];
		}
		
		// Allocate the array. It stores deltas between span numbers (the delta is
		// bounded by the number of spans that can occur within a chunk).
		short[] theSpanDeltas = new short[theCurrentCount];
		long[] theStartSpans = new long[theSize]; // The first span number for each id
		long[] theLastSpans = new long[theSize]; // The last (current) span number for each id
		int theSpanCount = itsChunkSpanDatas.size(); 
		for(int i=0;i<theSpanCount;i++)
		{
			SpanData theSpanData = itsChunkSpanDatas.get(i);
			long theSpan = theSpanData.itsSpan;
			long[] theSpanIds = theSpanData.itsIds;
			int theIdsCount = theSpanIds.length;
			
			for(int j=0;j<theIdsCount;j++)
			{
				long theId = theSpanIds[j];
				int theIndex = itsChunkObjectCounts.get(theId);
				long theDelta;
				if (theStartSpans[theIndex] == 0) 
				{
					theStartSpans[theIndex] = theSpan;
					theLastSpans[theIndex] = theSpan;
					theDelta = 0;
				}
				else
				{
					theDelta = theSpan - theLastSpans[theIndex];
					theLastSpans[theIndex] = theSpan;
					assert theDelta >= Short.MIN_VALUE && theDelta <= Short.MAX_VALUE : theDelta;
				}
				
				theSpanDeltas[theCurrentPositions[theIndex]++] = (short) theDelta;
			}
		}
		
		itsChunkObjectCounts.clear();
		itsChunkSpanDatas.clear();
		itsSpanDatasMemory = 0;
	}
	
	public void registerAccess(long aId)
	{
		itsSpanObjects.add(aId);
		itsChunkObjectCounts.adjustOrPutValue(aId, 1, 1);
	}
	
	/**
	 * Stores the data about the objects that were accessed during a span
	 * @author gpothier
	 */
	private static class SpanData
	{
		private final long itsSpan;
		private final long[] itsIds;

		public SpanData(long aSpan, long[] aIds)
		{
			itsSpan = aSpan;
			itsIds = aIds;
		}
	}
	
	private static class MyLongIntHashMap extends TLongIntHashMap
	{
		/**
		 * Returns the approximate amount of memory used by this map (in bytes). 
		 */
		public int getMemory()
		{
			// TPrimitiveHash uses one byte per entry
			// TLongHash uses one long per entry
			// TLongIntHashMap uses one int per entry
			return capacity()*(1+8+4); 
		}
	}
	
	private static class LongIntSorter extends Sorter
	{
		private final long[] itsLongs;
		private final int[] itsInts;

		public LongIntSorter(long[] aLongs, int[] aInts)
		{
			itsLongs = aLongs;
			itsInts = aInts;
		}
		
		public void sort()
		{
			sort(itsLongs);
		}

		@Override
		protected void swap(int a, int b)
		{
			swap(itsInts, a, b);
		}
		
	}
}
