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
package tod.impl.evdb1.db.file;

import tod.impl.evdb1.db.file.HardPagedFile.Page;
import zz.utils.bit.BitUtils;
import zz.utils.bit.IntBitStruct;

/**
 * Similar to {@link HardPagedFile} but allows pages of different sizes.
 * @author gpothier
 */
public class SoftPagedFile
{
	private HardPagedFile itsFile;
	
	/**
	 * Minimum page size, in array slots.
	 */
	private int itsMinPageSize;
	
	/**
	 * Logarithm of the minimum page size.
	 */
	private int itsMinLog;
	
	private int itsPagePointerSize;
	private int itsPageIdBits;
	private int itsPageSizeBits;
	private int itsPageIndexBits;
	
	private PageData[] itsPagesData;
	
	public SoftPagedFile(HardPagedFile aFile, int aMinPageSize)
	{
		if (aMinPageSize < 4) throw new IllegalArgumentException("Minimum size too small: "+aMinPageSize);
		if (aMinPageSize % 4 != 0) throw new IllegalArgumentException("Minimum size must be multiple of 4: "+aMinPageSize);
		
		itsFile = aFile;
		itsMinPageSize = aMinPageSize/4;
		
		int theMaxPageSize = itsFile.getPageSize();
		if (theMaxPageSize % 4 != 0) throw new IllegalArgumentException("File page size must be multiple of 4: "+theMaxPageSize);
		theMaxPageSize /= 4;
		
		itsMinLog = BitUtils.log2ex(itsMinPageSize);
		int theMaxLog = BitUtils.log2ex(theMaxPageSize);
		
		if (itsMinLog == -1) throw new IllegalArgumentException("Bad minimum size: "+itsMinPageSize);
		if (theMaxLog == -1) throw new IllegalArgumentException("Bad maximum size: "+theMaxPageSize);
		
		int theSizesCount = theMaxLog - itsMinLog + 1;
		itsPagesData = new PageData[theSizesCount];
		for(int i=0;i<theSizesCount;i++) itsPagesData[i] = new PageData(i);
		
		// Calculate page pointer size:
		// base page pointer + page size + soft page index
		int theMaxSoftPagesCount = theMaxPageSize/itsMinPageSize;
		itsPageIdBits = itsFile.getPagePointerSize();
		itsPageSizeBits = BitUtils.log2ceil(theSizesCount);
		itsPageIndexBits = BitUtils.log2ceil(theMaxSoftPagesCount);
		
		itsPagePointerSize = itsPageIdBits + itsPageSizeBits + itsPageIndexBits;
	}
	
	public int getPagePointerSize()
	{
		return itsPagePointerSize;
	}
	
	/**
	 * Returns the minimum page size, in bytes, supported by this file.
	 */
	public int getMinPageSize()
	{
		return itsMinPageSize*4;
	}

	/**
	 * Returns the maximum page size supported by this file.
	 */
	public int getMaxPageSize()
	{
		return itsFile.getPageSize();
	}

	public SoftPage get(long aId)
	{
		long thePageId;
		int thePageSizeIndex;
		int thePageIndex;
		
		long theId = aId;
		
		long thePageIdMask = BitUtils.pow2(itsPageIdBits)-1;
		thePageId = theId & thePageIdMask;
		theId >>>= itsPageIdBits;
		
		long thePageSizeMask = BitUtils.pow2(itsPageSizeBits)-1;
		thePageSizeIndex = (int) (theId & thePageSizeMask);
		theId >>>= itsPageSizeBits;
		
		long thePageIndexMask = BitUtils.pow2(itsPageIndexBits)-1;
		thePageIndex = (int) (theId & thePageIndexMask);
		theId >>>= itsPageIndexBits;
		
		if (theId != 0) throw new IllegalArgumentException("Id overflow: "+aId);
		
		int thePageSize = getPageSize(thePageSizeIndex);
		
		return new SoftPage(
				aId, 
				itsFile.get(thePageId),
				thePageIndex*thePageSize, 
				thePageSize);
	}
	
	/**
	 * Creates a new page of the given size
	 * @param aSize Desired page size, in bytes. Must be a power of 2.
	 */
	public SoftPage create(int aSize)
	{
		PageData thePageData = itsPagesData[getPageSizeIndex(aSize)];
		return thePageData.createPage();
	}
	
	/**
	 * Returns the size index corresponding to the given size.
	 */
	private int getPageSizeIndex(int aSize)
	{
		if (aSize % 4 != 0) throw new IllegalArgumentException("Size must be multiple of 4: "+aSize);
		int theSize = aSize/4;
		
		if (theSize < itsMinPageSize) throw new IllegalArgumentException("Size too small: "+aSize);
		int theLog = BitUtils.log2ex(theSize);
		if (theLog == -1) throw new IllegalArgumentException("Bad size: "+aSize);
		
		return theLog-itsMinLog;
	}
	
	/**
	 * Returns the actual page size, in array slots, corresponding to
	 * the given size index.
	 */
	private int getPageSize(int aPageSizeIndex)
	{
		return BitUtils.pow2i(itsMinLog+aPageSizeIndex);
	}
	
	/**
	 * Creates a soft page id.
	 */
	private long makeId(long aPageId, int aPageSizeIndex, int aPageIndex)
	{
		long thePageIdMask = BitUtils.pow2(itsPageIdBits)-1;
		long thePageSizeMask = BitUtils.pow2(itsPageSizeBits)-1;
		long thePageIndexMask = BitUtils.pow2(itsPageIndexBits)-1;
		
		if ((aPageId & ~thePageIdMask) != 0) throw new IllegalArgumentException("Page id overflow");
		if ((aPageSizeIndex & ~thePageSizeMask) != 0) throw new IllegalArgumentException("Page size index overflow");
		if ((aPageIndex & ~thePageIndexMask) != 0) throw new IllegalArgumentException("Page index overflow");
		assert aPageId <= itsFile.getPagesCount();
		
		long theId = 0;
		
		theId |= aPageIndex;
		
		theId <<= itsPageSizeBits;
		theId |= aPageSizeIndex;
		
		theId <<= itsPageIdBits;
		theId |= aPageId;
		
		return theId;
	}

	/**
	 * Per-size page data. Maintains the current hard page and index
	 * for a given page size.
	 * @author gpothier
	 */
	private class PageData
	{
		/**
		 * Index of the page size in the page data array.
		 */
		private int itsPageSizeIndex;
		
		/**
		 * Actual page size, in array slots.
		 */
		private int itsPageSize;
		
		private Page itsCurrentPage = null;
		
		private int itsCurrentIndex;
		
		public PageData(int aSizeIndex)
		{
			itsPageSizeIndex = aSizeIndex;
			itsPageSize = getPageSize(itsPageSizeIndex);
		}

		public SoftPage createPage()
		{
			int theFilePageSize = itsFile.getPageSize()/4;
			
			if (itsCurrentPage == null || itsCurrentIndex * itsPageSize >= theFilePageSize)
			{
				// We must use a new page
				itsCurrentPage = itsFile.create();
				itsCurrentIndex = 0;
			}
			
			long theId = makeId(
					itsCurrentPage.getPageId(), 
					itsPageSizeIndex, 
					itsCurrentIndex);
			
			SoftPage thePage = new SoftPage(
					theId, 
					itsCurrentPage, 
					itsCurrentIndex * itsPageSize, 
					itsPageSize);
			
			itsCurrentIndex++;
			
			return thePage;
		}
	}
	
	public static class SoftPage extends PageBank.Page
	{
		private Page itsPage;
		
		/**
		 * Page offset, in array slots.
		 */
		private int itsOffset;
		
		/**
		 * Page size, in array slots
		 */
		private int itsSize;
		
		public SoftPage(long aId, Page aPage, int aOffset, int aSize)
		{
			super(null); //TODO: BROKEN, should be a page key but it seems soft pages are not needed anymore.
			itsPage = aPage;
			itsOffset = aOffset;
			itsSize = aSize;
		}

		public SoftPageBitStruct asBitStruct()
		{
			return new SoftPageBitStruct(this, itsOffset, itsSize);
		}
		
		@Override
		public int getSize()
		{
			return itsSize*4;
		}
		
		private int[] getData()
		{
			return itsPage.getData();
		}

		@Override
		void modified()
		{
			itsPage.modified();
		}
	}
	
	public static class SoftPageBitStruct extends PageBank.PageBitStruct
	{
		public SoftPageBitStruct(SoftPage aPage, int aOffset, int aSize)
		{
			super(aOffset, aSize, aPage);
		}
		
		@Override
		protected int[] getData()
		{
			return getPage().getData();
		}

		public SoftPage getPage()
		{
			return (SoftPage) super.getPage();
		}
		
	}

}
