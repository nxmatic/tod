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

import zz.utils.bit.IntBitStruct;

/**
 * A page bank stores and manages data pages, each of which identified by 
 * a unique number, or pointer.
 * @author gpothier
 */
public abstract class PageBank
{
	/**
	 * Retrieve a page given its id.
	 */
	public abstract Page get(long aId);
	
	/**
	 * Creates a new, blank page
	 */
	public abstract Page create();
	
	/**
	 * Clients can call this method if they know the given
	 * page will not be used anymore, so that
	 * its space can be reclaimed. 
	 */
	public abstract void free(Page aPage);
	
	/**
	 * Returns the number of bits necessary to represent a 
	 * page pointer.
	 */
	public abstract int getPagePointerSize();
	
	public static abstract class PageKey
	{
		private final PageBank itsBank;
		private final long itsPageId;
		
		public PageKey(PageBank aBank, long aPageId)
		{
			itsBank = aBank;
			itsPageId = aPageId;
		}

		public long getPageId()
		{
			return itsPageId;
		}

		public PageBank getBank()
		{
			return itsBank;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((itsBank == null) ? 0 : itsBank.hashCode());
			result = prime * result + (int) (itsPageId ^ (itsPageId >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final PageKey other = (PageKey) obj;
			if (itsBank == null)
			{
				if (other.itsBank != null) return false;
			}
			else if (!itsBank.equals(other.itsBank)) return false;
			if (itsPageId != other.itsPageId) return false;
			return true;
		}
		
		@Override
		public String toString()
		{
			return "PageKey ["+itsPageId+" on "+itsBank+"]";
		}
	}
	
	public static abstract class Page
	{
		private PageKey itsKey;

		public Page(PageKey aKey)
		{
			itsKey = aKey;
		}
		
		public PageKey getKey()
		{
			return itsKey;
		}
		
		public long getPageId()
		{
			return getKey().getPageId();
		}

		/**
		 * Marks this page as modified.
		 */
		abstract void modified();

		/**
		 * Returns the size of this page, in bytes.
		 */
		public abstract int getSize();
		
		public abstract PageBitStruct asBitStruct();

		/**
		 * Notifies the system that this page is being used.
		 * This is an optional operation.
		 */
		public void use()
		{
		}
	}
	
	public static abstract class PageBitStruct extends IntBitStruct
	{
		private Page itsPage;

		public PageBitStruct(int aOffset, int aSize, Page aPage)
		{
			super(null, aOffset, aSize);
			itsPage = aPage;
		}

		public Page getPage()
		{
			return itsPage;
		}
		
		@Override
		protected void grow(int aMinSize)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected abstract int[] getData();
		
		@Override
		protected void setData(int[] aData)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeBoolean(boolean aValue)
		{
			synchronized (getPage())
			{
				super.writeBoolean(aValue);
				getPage().modified();
			}
		}

		@Override
		public void writeBytes(byte[] aBytes, int aBitCount)
		{
			synchronized (getPage())
			{
				super.writeBytes(aBytes, aBitCount);
				getPage().modified();
			}
		}

		@Override
		public void writeBytes(byte[] aBytes)
		{
			synchronized (getPage())
			{
				super.writeBytes(aBytes);
				getPage().modified();
			}
		}

		@Override
		public void writeInt(int aValue, int aBitCount)
		{
			synchronized (getPage())
			{
				super.writeInt(aValue, aBitCount);
				getPage().modified();
			}
		}

		@Override
		public void writeLong(long aValue, int aBitCount)
		{
			synchronized (getPage())
			{
				super.writeLong(aValue, aBitCount);
				getPage().modified();
			}
		}
	}
}
