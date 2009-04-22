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

import tod.impl.evdb1.db.file.PageBank.Page;
import tod.impl.evdb1.db.file.PageBank.PageBitStruct;


/**
 * Writes out {@link Tuple}s in a linked list of {@link Page}s.
 * See {@link TupleIterator} for a description of page format.
 * @author gpothier
 */
public class TupleWriter<T>
{
	private PageBank itsBank;
	private TupleCodec<T> itsTupleCodec;
	private Page itsCurrentPage;
	private PageBitStruct itsCurrentStruct;
	private int itsPagesCount;

	/**
	 * Creates a tuple writer that resumes writing at the specified page.
	 */
	public TupleWriter(PageBank aBank, TupleCodec<T> aTupleCodec, Page aPage, int aPos)
	{
		itsBank = aBank;
		itsTupleCodec = aTupleCodec;
		setCurrentPage(aPage, aPos);
	}
	
	/**
	 * Creates an uninitialized tuple writer. Use {@link #setCurrentPage(Page, int)} to
	 * properly initialize.
	 */
	protected TupleWriter(PageBank aBank, TupleCodec<T> aTupleCodec)
	{
		itsBank = aBank;
		itsTupleCodec = aTupleCodec;
	}

	private int getPagePointerSize()
	{
		return itsBank.getPagePointerSize();
	}

	private int getTupleSize()
	{
		return itsTupleCodec.getTupleSize();
	}
	
	/**
	 * Writes a tuple to the file.
	 */
	public void add(T aTuple)
	{
		if (itsCurrentStruct.getRemainingBits() - 2*getPagePointerSize() < getTupleSize())
		{
			Page theNextPage = itsBank.create();
			itsPagesCount++;
			
			PageBitStruct theNextStruct = theNextPage.asBitStruct();
			long theNextPageId = theNextStruct.getPage().getPageId();
			long theCurrentPageId = itsCurrentStruct.getPage().getPageId();
			
			// Write next page id (+1: 0 means no next page).
			itsCurrentStruct.setPos(itsCurrentStruct.getTotalBits()-getPagePointerSize());
			itsCurrentStruct.writeLong(theNextPageId+1, getPagePointerSize());
			
			// Write previous page id on next page
			theNextStruct.setPos(itsCurrentStruct.getTotalBits()-2*getPagePointerSize());
			theNextStruct.writeLong(theCurrentPageId+1, getPagePointerSize());
			theNextStruct.setPos(0);
			
			newPageHook(itsCurrentStruct, theNextPageId);
			
			itsBank.free(itsCurrentPage);
			
			itsCurrentStruct = theNextStruct;
			itsCurrentPage = theNextPage;
		}

		if (itsCurrentStruct.getPos() == 0) startPageHook(itsCurrentStruct, aTuple);
		
//		itsCurrentStruct.getPage().use();
		itsTupleCodec.write(itsCurrentStruct, aTuple);
	}
	
	/**
	 * A hook method that is called whenever a new page is started.
	 * The method does nothing by default.
	 * @param aStruct The struct of the page that is being finished.
	 */
	protected void newPageHook(PageBitStruct aStruct, long aNewPageId)
	{
	}

	/**
	 * A hook method that is called whenever a page is about to receive
	 * its first tuple.
	 * The method does nothing by default.
	 * @param aStruct The struct of the page that is being started
	 * @param aTuple The tuple that is being written
	 */
	protected void startPageHook(PageBitStruct aStruct, T aTuple)
	{
	}

	/**
	 * Returns the number of pages used by this writer.
	 */
	public int getPagesCount()
	{
		return itsPagesCount;
	}

	/**
	 * The currently used page.
	 */
	public Page getCurrentPage()
	{
		return itsCurrentPage;
	}
	
	public PageBank getBank()
	{
		return itsBank;
	}

	public TupleCodec<T> getTupleCodec()
	{
		return itsTupleCodec;
	}

	public PageBitStruct getCurrentStruct()
	{
		return itsCurrentStruct;
	}

	/**
	 * Sets the current page and moves the internal pointer to the given
	 * position (in bits).
	 */
	protected void setCurrentPage(Page aPage, int aPos)
	{
		itsCurrentPage = aPage;
		itsCurrentStruct = itsCurrentPage.asBitStruct();
		itsCurrentStruct.setPos(aPos);
		
		// We mark the page as modified to ensure that it is properly written
		// the the disk, even if it is blank.
		itsCurrentPage.modified();
	}
	
}
