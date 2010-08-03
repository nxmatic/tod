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
package tod.impl.evdbng.db.file;

import static tod.impl.evdbng.DebuggerGridConfigNG.DB_MAX_INDEX_LEVELS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import tod.core.DebugFlags;
import tod.impl.evdbng.db.file.Page.ChainedPageIOStream;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.TupleFinder.Match;
import tod.impl.evdbng.db.file.TupleFinder.NoMatch;

public abstract class StaticBTree<T extends Tuple>
{
	/**
	 * The name of this btree (the index it represents).
	 */
	private final String itsName;
	private final PagedFile itsFile;
	
	private Page itsRootPage;
	
	/**
	 * The level of the root page. If the root page is also a leaf page, the level is 0.
	 */
	private int itsRootLevel;
	private int itsFirstLeafPageId;
	
	/**
	 * The Page chains for each btree level. The first slot corresponds to the leaves.
	 */
	private MyChainedPageIOStream<T>[] itsChains = new MyChainedPageIOStream[DB_MAX_INDEX_LEVELS];
	
	/**
	 * Last written key for each level.
	 */
	private long[] itsLastKeys = new long[DB_MAX_INDEX_LEVELS];
	
	/**
	 * The keys currently being written for each level.
	 */
	private long[] itsCurrentKeys = new long[DB_MAX_INDEX_LEVELS];

	/**
	 * Number of tuples before the beginning of the current page for each level.
	 */
	private long[] itsTupleCount = new long[DB_MAX_INDEX_LEVELS];
	
	private long itsFirstKey = -1;
	private long itsLastKey=0;
	
	/**
	 * Number of pages per level
	 */
//	private int[] itsPagesCount = new int[DB_MAX_INDEX_LEVELS];
	
	/**
	 * Total number of tuples in leaf nodes.
	 */
	private long itsLeafTupleCount = 0;
	
	/**
	 * Size of data associated to each tuple.
	 */
	private final TupleBufferFactory<T> itsTupleBufferFactory = getTupleBufferFactory();
	
	private static PrintWriter itsLogWriter;
	
	static
	{
		if (DebugFlags.DB_LOG_DIR != null) 
		{
			try
			{
				itsLogWriter = new PrintWriter(new File(DebugFlags.DB_LOG_DIR+"/btree.log"));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Write everything to the same file otherwise we can get too many open files.
	 */
	private static void log(String aName, long aLeafTupleCount, long aKey, String aExtradata)
	{
		if (DebugFlags.DB_LOG_DIR != null)
		{
			synchronized(StaticBTree.class)
			{
				itsLogWriter.print(aName+": "+aLeafTupleCount+" - "+aKey);
				if (aExtradata != null) itsLogWriter.print(" "+aExtradata);
				itsLogWriter.println();
				itsLogWriter.flush();
			}
		}
	}
	
	public StaticBTree(String aName, PagedFile aFile)
	{
		itsName = aName;
		itsFile = aFile;

		// Init pages
		itsChains[0] = new MyChainedPageIOStream<T>(itsFile, this, 0);
		startLeafPage();
		itsRootPage = itsChains[0].getCurrentPage();
		itsFirstLeafPageId = itsRootPage.getPageId();
		itsRootLevel = 0;
	}
	
	/**
	 * Reconstructs a previously-written tree from the given struct.
	 */
	public StaticBTree(String aName, PagedFile aFile, PageIOStream aStream)
	{
		itsName = aName;
		itsFile = aFile;

		int theRootPageId = aStream.readPagePointer();
		itsRootPage = getFile().get(theRootPageId);
		
		itsFirstLeafPageId = aStream.readPagePointer();
		itsFirstKey = aStream.readLong();
		itsLastKey = aStream.readLong();
		itsLeafTupleCount = aStream.readTupleCount();
		itsRootLevel = aStream.readByte();
		
		for (int i=0;i<=itsRootLevel;i++)
		{
			// chains
			int thePageId = aStream.readPagePointer();
			int thePos = aStream.readPageOffset();
			itsChains[i] = new MyChainedPageIOStream<T>(getFile(), thePageId, thePos, this, i);
			itsChains[i].getCurrentStream().setPos(thePos);
			
			// last keys
			itsLastKeys[i] = aStream.readLong();
			
			// tuple count
			itsTupleCount[i] = aStream.readTupleCount();
		}
	}
	
	protected void logLeafTuple(long aKey, String aExtradata)
	{
		log(itsName, itsLeafTupleCount, aKey, aExtradata);
	}
	
	/**
	 * Writes this index to the given struct so that it can be reloaded
	 * later.
	 */
	public void writeTo(PageIOStream aStream)
	{
		aStream.writePagePointer(itsRootPage.getPageId());
		aStream.writePagePointer(itsFirstLeafPageId);
		aStream.writeLong(itsFirstKey);
		aStream.writeLong(itsLastKey);
		aStream.writeTupleCount(itsLeafTupleCount);
		aStream.writeByte(itsRootLevel);
		
		for (int i=0;i<=itsRootLevel;i++)
		{
			MyChainedPageIOStream<T> theChain = itsChains[i];
			aStream.writePagePointer(theChain.getCurrentPage().getPageId());
			aStream.writePageOffset(theChain.getCurrentStream().getPos());
			
			aStream.writeLong(itsLastKeys[i]);
			
			aStream.writeTupleCount(itsTupleCount[i]);
		}
	}
	
	/**
	 * Returns the maximum serialized size of an index.
	 */
	public static int getSerializedSize()
	{
		int theResult = 0;

		theResult += PageIOStream.pagePointerSize()*2;
		theResult += PageIOStream.longSize()*2;
		theResult += PageIOStream.tupleCountSize();
		theResult += PageIOStream.byteSize();
		
		theResult += DB_MAX_INDEX_LEVELS * (
				PageIOStream.pagePointerSize()
				+PageIOStream.pageOffsetSize()
				+PageIOStream.longSize()
				+PageIOStream.tupleCountSize());

		return theResult;
	}
	

	
	public PagedFile getFile()
	{
		return itsFile;
	}

	/**
	 * Returns the tuple buffer factory for this btree's leaf nodes.
	 * Note that this method is called only once, during the initialization
	 * of the BTree. It should return a constant. 
	 */
	protected abstract TupleBufferFactory<T> getTupleBufferFactory();
	
	/**
	 * Writes a tuple at the leaf level.
	 * @return The {@link PageIOStream} to which extra data can be written
	 */
	protected PageIOStream addLeafKey(long aKey)
	{
		if (itsFirstKey == -1) itsFirstKey = aKey;
		assert itsLastKey <= aKey : String.format("last: %d, key: %d", itsLastKey, aKey);
		
		itsLastKey = aKey;
		PageIOStream theStream = addKey(aKey, itsTupleBufferFactory.getDataSize(), 0);
		itsLeafTupleCount++;
		
		return theStream;
	} 
	
	/**
	 * Adds an internal tuple at the specified level.
	 * @param aKey The tuple key
	 * @param aPageId Tuple data
	 * @param aTupleCount Tuple data
	 * @param aLevel The level to which the tuple is added
	 */
	protected void addInternalTuple(long aKey, int aPageId, long aTupleCount, int aLevel)
	{
		assert aLevel > 0;
		
		addKey(aKey, PageIOStream.internalTupleDataSize(), aLevel);
		itsChains[aLevel].writeInternalTupleData(aPageId, aTupleCount);
	}
	
	/**
	 * Adds a key to the current page of the given level.
	 * The caller is responsible of writing additional tuple data. This method only
	 * ensures that space is available for adding this data.
	 * @param aKey The key to add
	 * @param aDataSpace Additional space for tuple data.
	 */
	protected PageIOStream addKey(long aKey, int aDataSpace, int aLevel)
	{
		long theDelta = aKey - itsLastKeys[aLevel];
		assert theDelta >= 0 : "aKey: "+aKey+"theDelta: "+theDelta;
		assert itsChains[aLevel] != null : ""+aLevel;
		
		// Set the currently written key so that index tuples in higher 
		// levels are properly set up
		itsCurrentKeys[aLevel] = aKey;
		
		// We put the MSB to 1 for bytes so as to recognize a 0 byte
		// at the end of a page as empty space
		if (theDelta < 0x7f)
		{
			itsChains[aLevel].writeByte((byte) (theDelta | 0x80), aDataSpace);
		}
//		else if (theDelta < 0x1fff)
//		{
//			int d = (int) theDelta;
//			int b1 = (d >>> 8) | 0x00;
//			int b2 = d & 0xff;
//			itsChains[aLevel].writeBB(b1, b2, aDataSpace);
//		}
		else if (theDelta < 0x1fffff)
		{
			int d = (int) theDelta;
			int b = (d >>> 16) | 0x20;
			int s = d & 0xffff;
			itsChains[aLevel].writeBS(b, s, aDataSpace);
		}
		else if (theDelta < 0x1fffffffffL)
		{
			long d = theDelta;
			int b = (int) (d >>> 32) | 0x40;
			int i = (int) d;
			itsChains[aLevel].writeBI(b, i, aDataSpace);
		}
		else
		{
			itsChains[aLevel].writeBL(0x60, aKey, aDataSpace);
		}

		// We update this after writing to the chains, as the last key of the previous page
		// is written at the beginning of each new page
		itsLastKeys[aLevel] = aKey;

		return itsChains[aLevel].getCurrentStream();
	}
	
	/**
	 * Decompresses the keys of the specified page into the given buffer.
	 * @param aLevel The level of the page.
	 */
	private TupleBuffer<?> decompress(Page aPage, int aLevel)
	{
		PageIOStream theStream = aPage.asIOStream();
		
		theStream.setPos(PagedFile.PAGE_SIZE-PageIOStream.pagePointerSize()*2);
		int thePreviousPagePointer = theStream.readPagePointer();
		int theNextPagePointer = theStream.readPagePointer();
		
		theStream.setPos(0);
		
		TupleBuffer<?> theBuffer;
		long theLastKey = theStream.readLong(); // Last key of the previous page.
		
		if (aLevel == 0)
		{
			long theTupleCount = theStream.readLong();
			theBuffer = itsTupleBufferFactory.create(
					PagedFile.PAGE_SIZE, 
					thePreviousPagePointer, 
					theNextPagePointer);
			//TODO set the tuple count (tuple count is accessible from parent 
			theBuffer.setTupleCount(theTupleCount);
		}
		else
		{
			// Internal tuples record the page pointer and the number of tuples below (for counts)
			theBuffer = TupleBufferFactory.INTERNAL.create(
					PagedFile.PAGE_SIZE, 
					thePreviousPagePointer, 
					theNextPagePointer);
		}

		while(true)
		{
			long theDelta;

			if (theStream.remaining() <= 2*PageIOStream.pagePointerSize()) break;
			int b = theStream.readByte();
			if (b == 0) break;
			
			if ((b & 0x80) != 0) theDelta = b & 0x7f;
			else 
			{
				int t = b & 0x60;
				int r = b & 0x1f;
				
				switch (t)
				{
//				case 0x00:
//					theDelta = (theStream.readByte() & 0xff) | (r << 8);
//					break;
//					
				case 0x20:
					theDelta = (theStream.readShort() & 0xffff)| (r << 16);
					break;
					
				case 0x40:
					theDelta = (theStream.readInt() & 0xffffffffL) | ((long)r << 32);
					break;							
					
				case 0x60:
					theDelta = 0;
					theLastKey = theStream.readLong();
					break;
					
				default: throw new RuntimeException("1 == 2");
				}
			}
			theLastKey += theDelta;
			theBuffer.read(theLastKey, theStream);
		}
		return theBuffer;
	}
	
	/**
	 * Initializes the current leaf page. This method is called before any data
	 * is written to the page.
	 */
	protected void startLeafPage()
	{
		startPage(0);
		itsChains[0].writeLong(itsLeafTupleCount , 0);
	}

	/**
	 * Initializes the new page. This method is called before any data
	 * is written to the page.
	 */
	protected void startPage(int aLevel)
	{
		// Write the last key of the previous page
		itsChains[aLevel].writeLong(itsLastKeys[aLevel], 0);
	}
	
	
	/**
	 * Called when a new page is created for the given level.
	 * @param aLevel The level at which the page was created.
	 * @param aOldPageId The id of the finished page.
	 * @param aNewPageId The id of the new page.
	 */
	protected void newPageHook(int aLevel, int aOldPageId, int aNewPageId)
	{
		// Initialize the new page
		if (aLevel == 0) startLeafPage();
		else startPage(aLevel);
		
		MyChainedPageIOStream<T> theChain = itsChains[aLevel+1];
		if (theChain == null)
		{
			// We need one more level
			assert itsRootLevel == aLevel;
			itsRootLevel++;
			theChain = new MyChainedPageIOStream<T>(itsFile, this, itsRootLevel);
			itsRootPage = theChain.getCurrentPage();
			itsChains[itsRootLevel] = theChain;
			
			//add the key to the first page of the new level : its always the first key
			itsChains[itsRootLevel].writeLong(itsFirstKey, 0);
			itsLastKeys[itsRootLevel] = itsFirstKey;
			
			//create the first internal tuple of the level 
			//having as key the first key of the leaves
			addInternalTuple(itsFirstKey, aOldPageId, (short) 0, itsRootLevel);
		}
		
		//create a new page with the key value to be written 
		//the LeafTupleCount includes the key which is going to be inserted...
		long theOldPageCount = itsLeafTupleCount-itsTupleCount[aLevel];
		addInternalTuple(itsCurrentKeys[aLevel], aNewPageId, theOldPageCount, aLevel+1);
		
		itsTupleCount[aLevel] = itsLeafTupleCount;
	}

	/**
	 * Returns the cached tuple buffer of the given page, or recreates it.
	 */
	public TupleBuffer getPageTupleBuffer(int aPageId, int aLevel)
	{
		Page thePage = itsFile.get(aPageId);
		return getPageTupleBuffer(thePage, aLevel);
	}
	
	/**
	 * Returns the cached tuple buffer of the given page, or recreates it.
	 */
	public TupleBuffer getPageTupleBuffer(Page aPage, int aLevel)
	{
		TupleBuffer theTupleBuffer = (TupleBuffer) aPage.getDecodedPage();
		if (theTupleBuffer != null) return theTupleBuffer;
		
		theTupleBuffer = decompress(aPage, aLevel);
		aPage.setDecodedPage(theTupleBuffer);
		
		return theTupleBuffer;
	}
	
	private TupleIterator<T> gotoFirstTuple()
	{
		Page theFirstPage = getFile().get(itsFirstLeafPageId);
		return new TupleIterator<T>(
				this,
				getPageTupleBuffer(theFirstPage, 0),
				0);
	}
	
	/**
	 * Returns an {@link TupleIterator} that returns all tuples whose key
	 * is greater than or equal to the specified key.
	 * @param aKey Requested first key, or 0 to start
	 * at the beginning of the list.
	 */
	public TupleIterator<T> getTupleIterator(long aKey)
	{
		return getTupleIterator(aKey, NoMatch.AFTER);
	}
	
	public TupleIterator<T> getTupleIterator(long aKey, NoMatch aNoMatch)
	{
		if (aKey <= itsFirstKey) return gotoFirstTuple();
		else
		{
			int theLevel = itsRootLevel;
			Page thePage = itsRootPage;
			TupleBuffer theBuffer = getPageTupleBuffer(thePage, theLevel);
			while (theLevel > 0)
			{
//				System.out.println("Level: "+theLevel);
				int thePosition = 
					TupleFinder.findTupleIndex(theBuffer, aKey, Match.FIRST, NoMatch.BEFORE);
				
				if (thePosition == -1) 
				{
					// The first tuple of this index is after the specified key
					return gotoFirstTuple();
				}
				
				InternalTuple theTuple = (InternalTuple) theBuffer.getTuple(thePosition);
				
				theLevel--;
				
				thePage = getFile().get(theTuple.getPageId());
				theBuffer = getPageTupleBuffer(thePage, theLevel);
			}
			
			if (theBuffer.getSize() == 0) return new TupleIterator<T>(this);
			int theIndex = TupleFinder.findTupleIndex(theBuffer, aKey, Match.FIRST, aNoMatch);
			
			if (theIndex < 0) 
			{
				// The last tuple is before the requested key.
				// The index of the last tuple is -index-1
				// We want an iterator that is past the last tuple.
				theIndex = -theIndex;
			}

			TupleIterator<T> theIterator = new TupleIterator<T>(this, theBuffer, theIndex);
			
			if (aNoMatch == NoMatch.AFTER)
			{
				//next test may be useful for index < 0
				T theTuple = theIterator.peekNext();
				if (theIterator.hasNext() && theTuple.getKey() < aKey)
					theIterator.next();
			}
			else
			{
				T theTuple = theIterator.peekPrevious();
				if (theIterator.hasPrevious() && theTuple.getKey() > aKey)
					theIterator.previous();
			}
			
			return theIterator;
		}
	}
	
	/**
	 * Returns the first tuple that has a key greater or equal
	 * than the specified key, if any.
	 * @param aNoMatch If null, only a tuple with exactly the specified
	 * key is returned. Otherwise, the previous or next tuple is returned.
	 * @return A matching tuple, or null if none is found.
	 */
	public T getTupleAt(long aKey, NoMatch aNoMatch)
	{
		TupleIterator<T> theIterator = getTupleIterator(
				aKey, 
				aNoMatch != null ? aNoMatch : NoMatch.AFTER);
		
		if (! theIterator.hasNext()) return null;
		T theTuple = theIterator.next();
		if (aNoMatch == null && theTuple.getKey() != aKey) return null;
		else return theTuple;
	}

	/**
	 * Counts the tuples between keys k1 (inclusive) and k2 (exclusive).
	 */
	public long countTuples(long aK1, long aK2)
	{
		TupleIterator<T> i1 = getTupleIterator(aK1);
		TupleIterator<T> i2 = getTupleIterator(aK2);
		
		return i2.getNextTupleIndex() - i1.getNextTupleIndex();
	}
	

	/**
	 * Chained PBS for the BTree. When a new page is created the hierarchy is updated.
	 * @author gpothier
	 */
	private static class MyChainedPageIOStream<T extends Tuple> extends ChainedPageIOStream
	{
		private final StaticBTree<T> itsTree;
		private final int itsLevel;
		
		public MyChainedPageIOStream(PagedFile aFile, StaticBTree<T> aTree, int aLevel)
		{
			super(aFile);
			itsTree = aTree;
			itsLevel = aLevel;
		}
		
		public MyChainedPageIOStream(PagedFile aFile, int aPageId, int aPosition, StaticBTree<T> aTree, int aLevel)
		{
			super(aFile, aPageId, aPosition);
			itsTree = aTree;
			itsLevel = aLevel;
		}

		@Override
		protected void newPageHook(int aOldPageId, int aNewPageId)
		{
			itsTree.newPageHook(itsLevel, aOldPageId, aNewPageId);
		}
	}
	
}
