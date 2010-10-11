package tod.impl.evdbng.db.file;

import static tod.impl.evdbng.db.file.PagedFile.PAGE_SIZE;
import tod.impl.evdbng.db.file.Page.IntSlot;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import zz.utils.bit.BitUtils;


/**
 * Succinct representation of a tree, based on the SODA'10 paper
 * of Sadakane and Navarro (http://www.dcc.uchile.cl/~gnavarro/publ.html).
 * 
 * @author gpothier
 */
public class RangeMinMaxTree
{
	/**
	 * The integer MASKS[i] has the (32-i)th bit set and all other bits are 0.
	 */
	public static final int[] MASKS = new int[32];
	
	static
	{
		for(int i=31, j=1;i>=0;i--, j<<=1) MASKS[i] = j;
	}

	private static final int BITS_PER_PAGE = PAGE_SIZE*8;
	private static final int PACKETS_PER_PAGE = BITS_PER_PAGE/32;
	private static final int MAX_LEVELS = 6;
	
	/**
	 * Size in bytes of each tuple (for all levels except 0).
	 * 3 shorts for e, m and M, and one int for page pointer.
	 */
	private static final int TUPLE_BYTES = 10;
	
	private static final int TUPLES_PER_PAGE = PAGE_SIZE/TUPLE_BYTES;

	
	private static final int TUPLE_OFFSET_SUM = 0; 
	private static final int TUPLE_OFFSET_MIN = 2; 
	private static final int TUPLE_OFFSET_MAX = 4; 
	private static final int TUPLE_OFFSET_PTR = 6; 
	
	private final PidSlot itsDirectorySlot;
	private final Directory itsDirectory;
	
	/**
	 * The first page of each level of the tree. Index 0 corresponds to the leaves.
	 */
	private PageIOStream[] itsLevels = new PageIOStream[MAX_LEVELS];
	
	private int itsCurrentPacket;
	private int itsCurrentPacketMask = MASKS[0];
	
	private long itsSize = 0;
	private int itsCurrentSum = 0;
	
	private int[] itsCurrentMin = new int[MAX_LEVELS];
	private int[] itsCurrentMax = new int[MAX_LEVELS]; 
	
	public RangeMinMaxTree(PidSlot aDirectorySlot)
	{
		itsDirectorySlot = aDirectorySlot;
		itsDirectory = new Directory(itsDirectorySlot.getPage(true));
		
		for(int i=0;i<MAX_LEVELS;i++) itsLevels[i] = itsDirectory.getStream(i, false);
		if (itsLevels[0] == null)
		{
			itsLevels[0] = itsDirectory.getStream(0, true);
			itsDirectory.getMinSlot(0).set(1);
			itsDirectory.getMaxSlot(0).set(-1);
			for(int i=1;i<MAX_LEVELS;i++) 
			{
				itsDirectory.getMinSlot(i).set(Short.MAX_VALUE);
				itsDirectory.getMaxSlot(i).set(Short.MIN_VALUE);
			}
		}
		
		for(int i=0;i<MAX_LEVELS;i++) 
		{
			itsCurrentMin[i] = itsDirectory.getMinSlot(i).get();
			itsCurrentMax[i] = itsDirectory.getMaxSlot(i).get();
		}
	}
	
	public void flush()
	{
		for(int i=0;i<MAX_LEVELS;i++) 
		{
			if (itsLevels[i] != null) itsDirectory.setStream(i, itsLevels[i]);
			itsDirectory.getMinSlot(i).set(itsCurrentMin[i]);
			itsDirectory.getMaxSlot(i).set(itsCurrentMax[i]);
		}
	}
	
	private static class Directory
	{
		private final Page itsPage;
		private PidSlot[] itsLevelSlots = new PidSlot[MAX_LEVELS];
		private IntSlot[] itsOffsetSlots = new IntSlot[MAX_LEVELS];
		private IntSlot[] itsCurrentMinSlots = new IntSlot[MAX_LEVELS];
		private IntSlot[] itsCurrentMaxSlots = new IntSlot[MAX_LEVELS];

		public Directory(Page aPage)
		{
			itsPage = aPage;
			int theOffset = 0;
			for(int i=0;i<MAX_LEVELS;i++)
			{
				itsLevelSlots[i] = new PidSlot(itsPage, theOffset);
				theOffset += PidSlot.size();
				itsOffsetSlots[i] = new IntSlot(itsPage, theOffset);
				theOffset += IntSlot.size();
				itsCurrentMinSlots[i] = new IntSlot(aPage, theOffset);
				theOffset += IntSlot.size();
				itsCurrentMaxSlots[i] = new IntSlot(aPage, theOffset);
				theOffset += IntSlot.size();
			}
		}
		
		public PageIOStream getStream(int aLevel, boolean aCreate)
		{
			Page thePage = itsLevelSlots[aLevel].getPage(aCreate);
			if (thePage == null) return null;
			int theOffset = itsOffsetSlots[aLevel].get();
			PageIOStream theStream = thePage.asIOStream();
			theStream.setPos(theOffset);
			return theStream;
		}
		
		public void setStream(int aLevel, PageIOStream aStream)
		{
			itsLevelSlots[aLevel].setPage(aStream.getPage());
			itsOffsetSlots[aLevel].set(aStream.getPos());
		}
		
		public IntSlot getMinSlot(int aLevel)
		{
			return itsCurrentMinSlots[aLevel];
		}
		
		public IntSlot getMaxSlot(int aLevel)
		{
			return itsCurrentMaxSlots[aLevel];
		}
		
		
	}
	
	private PagedFile getFile()
	{
		return itsDirectorySlot.getFile();
	}
	
	/**
	 * Returns the size, in bits, of this tree. Remember there are two bits per tree node.
	 */
	public long size()
	{
		return itsSize;
	}
	
	/**
	 * Writes an open parenthesis (start of a node)
	 */
	public void open()
	{
		itsCurrentPacket |= itsCurrentPacketMask;
		itsCurrentPacketMask >>>= 1;
		
		itsCurrentSum++;
		if (itsCurrentMax[0] < itsCurrentSum) itsCurrentMax[0] = itsCurrentSum;
		
		if (itsCurrentPacketMask == 0) writePacket();
		
		itsSize++;
	}
	
	/**
	 * Writes a close parenthesis (end of a node)
	 */
	public void close()
	{
		itsCurrentPacketMask >>>= 1;
		
		itsCurrentSum--;
		if (itsCurrentMin[0] > itsCurrentSum) itsCurrentMin[0] = itsCurrentSum;
		
		if (itsCurrentPacketMask == 0) writePacket();
		
		itsSize++;
	}
	
	public boolean isLeaf(long i)
	{
		assert isOpen(i);
		return isClose(i+1);
	}
	
	public long parent(long i)
	{
		assert isOpen(i);
		return enclose(i);
	}
	
	public long firstChild(long i)
	{
		if (isLeaf(i)) return -1;
		else return i+1;
	}
	
	public long nextSibling(long i)
	{
		assert isOpen(i);
		long s = findclose(i)+1;
		if (isClose(s)) return -1;
		else return s;
	}
	
	public long prevSibling(long i)
	{
		assert isOpen(i);
		if (isOpen(i-1)) return -1;
		else return findopen(i-1);
	}
	
	public long subtreeSize(long i)
	{
		assert isOpen(i);
		return (findclose(i)-i+1)/2;
	}
	
	private boolean isOpen(long i)
	{
		return get(i) == true;
	}
	
	private boolean isClose(long i)
	{
		return get(i) == false;
	}
	
	private long findclose(long i)
	{
		return fwdsearch_π(i, 0);
	}
	
	private long findopen(long i)
	{
		return bwdsearch_π(i, 0);
	}
	
	private long enclose(long i)
	{
		return bwdsearch_π(i, 2);
	}
	
	private void writePacket()
	{
		PageIOStream stream = itsLevels[0];
		stream.writeInt(itsCurrentPacket);
		itsCurrentPacket = 0;
		itsCurrentPacketMask = MASKS[0];

		if (stream.remaining() == 0) commitLeaf();
	}
	
	private void commitLeaf()
	{
		commitLevel(0);
	}
	
	/**
	 * Commits (= finishes) a page at the indicated level.
	 * This outputs a tuple at the above level, and recursively commits the above page if needed. 
	 */
	private void commitLevel(int l)
	{
		assert isShort(itsCurrentSum);
		assert isShort(itsCurrentMin[l]);
		assert isShort(itsCurrentMax[l]);

		if (itsCurrentMin[l+1] > itsCurrentMin[l]) itsCurrentMin[l+1] = itsCurrentMin[l];
		if (itsCurrentMax[l+1] < itsCurrentMax[l]) itsCurrentMax[l+1] = itsCurrentMax[l];
		
		PageIOStream stream = itsLevels[l+1];
		if (stream == null) 
		{
			stream = getFile().create().asIOStream();
			itsLevels[l+1] = stream;
		}
		
		stream.writeSSSI(
				(short) itsCurrentSum, 
				(short) itsCurrentMin[l], 
				(short) itsCurrentMax[l], 
				itsLevels[l].getPage().getPageId());
		if (stream.remaining() < TUPLE_BYTES) commitLevel(l+1);
		
		itsLevels[l] = getFile().create().asIOStream();
		
		itsCurrentMin[l] = itsCurrentSum+1;
		itsCurrentMax[l] = itsCurrentSum-1;
	}
	
	private static boolean isShort(int aValue)
	{
		return aValue >= Short.MIN_VALUE && aValue <= Short.MAX_VALUE;
	}
	
	private static boolean isBetween(int aValue, int aMin, int aMax)
	{
		return aValue >= aMin && aValue <= aMax;
	}
	
	/**
	 * Returns the ith bit
	 */
	public boolean get(long i)
	{
		long pageNumber = i/BITS_PER_PAGE;
		int offset = (int) (i%BITS_PER_PAGE);
		
		Page page = getNthPage(pageNumber, 0);
		int packetNumber = offset/32;
		int packetOffset = offset%32;
		
		int packet = getPacket(page, packetNumber);
		return (packet & MASKS[packetOffset]) != 0;
	}
	
	/**
	 * Retrives a packet from a leaf node.
	 * This method properly checks if the requested packet is the current packet.
	 */
	private int getPacket(Page aPage, int aNumber)
	{
		if (aPage.getPageId() == itsLevels[0].getPage().getPageId())
		{
			// Check if we are querying the current packet
			if (itsLevels[0].getPos() == aNumber*4) return itsCurrentPacket;
		}
		return aPage.readInt(aNumber*4);
	}
	
	/**
	 * Retrieves the Nth page at a given level.
	 */
	private Page getNthPage(long n, int aLevel)
	{
		PageIOStream up = itsLevels[aLevel+1];
		if (up == null)
		{
			if (n != 0) throw new RuntimeException("Requested page #"+n+" from level "+aLevel);
			return itsLevels[aLevel].getPage();
		}
		else
		{
			long parentPage = n/TUPLES_PER_PAGE;
			int offset = (int) n%TUPLES_PER_PAGE;
			Page parent = getNthPage(parentPage, aLevel+1);
			
			if (parent.getPageId() == up.getPage().getPageId())
			{
				// We might be trying to access the current page
				if (up.getPos() == offset*TUPLE_BYTES) return itsLevels[aLevel].getPage();
			}
			
			int id = parent.readInt(offset*TUPLE_BYTES + TUPLE_OFFSET_PTR);
			return getFile().get(id);
		}
	}
	
	// For unit tests
	long _test_fwdsearch_π(long i, int d)
	{
		return fwdsearch_π(i, d);
	}
	

	
	// G[i] = sum(0, i)
	// The kth leftmost leaf of the tree stores the sub-vector[lk, rk]
	// e[k] = sum(0, rk)
	// m[k] = e[k-1] + rmq(lk, rk)
	// M[k] = e[k-1] + RMQ(lk, rk)
	private long fwdsearch_π(long i, int d)
	{
		// Index of the leaf containing i
		long k = i/BITS_PER_PAGE;
		int offset = (int) (i%BITS_PER_PAGE);
		
		Page leaf = getNthPage(k, 0);
		int sum;
		
		// Search within the page (note: we perform the search regardless of the value of d
		// because we also need to compute the sum).
		int result = fwdsearch_π(leaf, offset, d);
		if (result >= 0) return (k*BITS_PER_PAGE)+result;
		else sum = (result << 1) >> 1;
		
		if (itsLevels[1] == null) return -1;
		
		// Not in the same page, we must walk the tree
		
		long parentPageNumber = k/TUPLES_PER_PAGE;
		int parentTupleNumber = (int) (k%TUPLES_PER_PAGE);
		Page page = getNthPage(parentPageNumber, 1);
		
		assert leaf.getPageId() == page.readInt(parentTupleNumber*TUPLE_BYTES + TUPLE_OFFSET_PTR);
		int e_k = page.readShort(parentTupleNumber*TUPLE_BYTES + TUPLE_OFFSET_SUM); 
		
		// Compute the global target value we seek: d' = G[i-1]+d = e[k] - sum(π, i, rk) + d
		int d_ = e_k-sum+d;
		
		// Walk up the tree
		
		int lastSum = e_k;
		
		int level = 1;
		long kInc = 1;
		k += kInc;
		up:
		while(true)
		{
			// Check the tuples of the current page
			for(int j=parentTupleNumber+1;j<TUPLES_PER_PAGE;j++)
			{
				int min = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MIN);
				int max = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MAX);
				int childId = page.readInt(j*TUPLE_BYTES + TUPLE_OFFSET_PTR);
				if (childId == 0) return -1;
				
				if (isBetween(d_, min, max)) 
				{
					level--;
					kInc /= TUPLES_PER_PAGE;
					page = getFile().get(childId);
					break up;
				}
				lastSum = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_SUM);
				k += kInc;
			}
			
			level++;
			kInc *= TUPLES_PER_PAGE;
			if (itsLevels[level] == null) return -1;
				
			long newParentNumber = parentPageNumber/TUPLES_PER_PAGE;
			int newTupleNumber = (int) (parentPageNumber%TUPLES_PER_PAGE);
			parentPageNumber = newParentNumber;
			parentTupleNumber = newTupleNumber;
			page = getNthPage(parentPageNumber, level);
		}
		
		// Walk down the tree
		while(level > 0)
		{
			for(int j=0;j<TUPLES_PER_PAGE;j++)
			{
				int min = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MIN);
				int max = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MAX);
				if (isBetween(d_, min, max)) 
				{
					level--;
					kInc /= TUPLES_PER_PAGE;
					int childId = page.readInt(j*TUPLE_BYTES + TUPLE_OFFSET_PTR);
					page = getFile().get(childId);
					break;
				}
				lastSum = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_SUM);
				k += kInc;
			}
		}
		
		// Check leaf page
		result = fwdsearch_π(page, 0, d_-lastSum);
		if (result >= 0) return (k*BITS_PER_PAGE)+result;
		else throw new RuntimeException("Internal error");
	}
	
	// For unit tests
	long _test_bwdsearch_π(long i, int d)
	{
		return bwdsearch_π(i, d);
	}

	private long bwdsearch_π(long i, int d)
	{
		// Index of the leaf containing i
		long k = i/BITS_PER_PAGE;
		int offset = (int) (i%BITS_PER_PAGE);
		
		Page leaf = getNthPage(k, 0);
		int sum;
		
		// Search within the page (note: we perform the search regardless of the value of d
		// because we also need to compute the sum).
		int result = bwdsearch_π(leaf, offset, d);
		if (result >= 0) return (k*BITS_PER_PAGE)+result;
		else sum = (result << 1) >> 1;
		
		if (itsLevels[1] == null || k == 0) return -1;
		
		// Not in the same page, we must walk the tree
		
		long km1 = k-1;
		long parentPageNumber = km1/TUPLES_PER_PAGE;
		int parentTupleNumber = (int) (km1%TUPLES_PER_PAGE);
		Page page = getNthPage(parentPageNumber, 1);
		
		int e_km1 = page.readShort(parentTupleNumber*TUPLE_BYTES + TUPLE_OFFSET_SUM); 
		
		// Compute the global target value we seek: d' = e[k-1] + sum(π, lk, i) - d
		// Note that this gives the bit just to the left of the one we seek.
		// We assume there is a "virtual" bit at position -1, and sum(-1, -1) = 0
		int d_ = e_km1+sum-d;
		
		// Walk up the tree
		
		int lastSum = e_km1;
		
		int level = 1;
		long kDec = 1;
		k -= kDec;
		up:
			while(true)
			{
				// Check the tuples of the current page
				for(int j=parentTupleNumber;j>=0;j--)
				{
					int min = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MIN);
					int max = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MAX);
					int childId = page.readInt(j*TUPLE_BYTES + TUPLE_OFFSET_PTR);
					if (childId == 0) return -1;
					
					lastSum = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_SUM);
					
					if (isBetween(d_, min, max)) 
					{
						level--;
						kDec /= TUPLES_PER_PAGE;
						page = getFile().get(childId);
						break up;
					}
					
					k -= kDec;
					if (k < 0) 
					{
						if (d_ == 0) return 0;
						else return -1;
					}
				}

				level++;
				kDec *= TUPLES_PER_PAGE;
				if (itsLevels[level] == null) return -1;
				
				long newParentNumber = (parentPageNumber-1)/TUPLES_PER_PAGE;
				int newTupleNumber = (int) ((parentPageNumber-1)%TUPLES_PER_PAGE);
				parentPageNumber = newParentNumber;
				parentTupleNumber = newTupleNumber;
				page = getNthPage(parentPageNumber, level);
			}
		
		// Walk down the tree
		while(level > 0)
		{
			for(int j=TUPLES_PER_PAGE-1;j>=0;j--)
			{
				int min = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MIN);
				int max = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_MAX);
				lastSum = page.readShort(j*TUPLE_BYTES + TUPLE_OFFSET_SUM);
				if (isBetween(d_, min, max)) 
				{
					level--;
					kDec /= TUPLES_PER_PAGE;
					int childId = page.readInt(j*TUPLE_BYTES + TUPLE_OFFSET_PTR);
					page = getFile().get(childId);
					break;
				}
				k -= kDec;
			}
		}

		if (lastSum-d_ == 0) return (k+1)*BITS_PER_PAGE;
		
		// Check leaf page
		result = bwdsearch_π(page, BITS_PER_PAGE-1, lastSum-d_);
		if (result >= 0) return (k*BITS_PER_PAGE)+result;
		else throw new RuntimeException("Internal error");
	}
	
	/**
	 * See {@link #fwdpos_π(int, int)}
	 */
	private static final byte[] TABLE_FWDPOS_π = new byte[256*8*18];
	private static final byte[] TABLE_BWDPOS_π = new byte[256*8*18];
	
	static
	{
		// 256 possible data values
		// 8 possible starting positions
		// 17 possible target values + 1 for total byte sum
		for(int v=-8;v<=9;v++) for (int s=0;s<8;s++) for (int d=0;d<256;d++)
		{
			TABLE_FWDPOS_π[fwdpos_π_tableIndex(d, s, v)] = precalculate_fwdpos_π(d, s, v);
			TABLE_BWDPOS_π[bwdpos_π_tableIndex(d, s, v)] = precalculate_bwdpos_π(d, s, v);
		}
	}
	
	private static int fwdpos_π_tableIndex(int aData, int aStart, int aValue)
	{
		int v = aValue+8;
		return (aStart*18 + v)*256 + aData;
	}
	
	private static int fwdpos_π_tableIndex0(int aData, int aValue)
	{
		int v = aValue+8;
		return v*256 + aData;
	}
	
	private static int bwdpos_π_tableIndex(int aData, int aStart, int aValue)
	{
		int v = aValue+8;
		return ((7-aStart)*18 + v)*256 + aData;
	}
	
	private static int bwdpos_π_tableIndex7(int aData, int aValue)
	{
		int v = aValue+8;
		return v*256 + aData;
	}
	
	private static byte precalculate_fwdpos_π(int aData, int aStart, int aValue)
	{
		assert isBetween(aStart, 0, 7);
		
		int sum = 0;
		int mask = 1 << (7-aStart);
		for(int i=aStart;i<8;i++)
		{
			boolean bit = (aData & mask) != 0;
			sum += bit ? 1 : -1;
			if (sum == aValue) return (byte) (i-aStart);
			mask >>>= 1;
		}
		return (byte) (0x80 | sum);
	}
	
	private static byte precalculate_bwdpos_π(int aData, int aStart, int aValue)
	{
		assert isBetween(aStart, 0, 7);
		
		int sum = 0;
		int mask = 1 << (7-aStart);
		for(int i=aStart;i>=0;i--)
		{
			boolean bit = (aData & mask) != 0;
			sum += bit ? 1 : -1;
			if (sum == aValue) return (byte) (aStart-i);
			mask <<= 1;
		}
		return (byte) (0x80 | sum);
	}
	
	/**
	 * Indicates in which position of the given data byte the sum of
	 * function π, starting at the specified position, reaches the specified value. 
	 * @param aData The data byte: 0..255
	 * @param aStart The starting position of the sum: 0..7 (0 is the MSB)
	 * @param aValue The searched value: -8..9 (9 means we ask the sum)
	 * @return Positive or 0 means the position (relative to aStart), 
	 * otherwise returns 0x80 | sum until the end of the byte
	 */
	private byte fwdpos_π(int aData, int aStart, int aValue)
	{
		return TABLE_FWDPOS_π[fwdpos_π_tableIndex(aData, aStart, aValue)];
	}
	
	private byte fwdpos_π(int aData, int aValue)
	{
		return TABLE_FWDPOS_π[fwdpos_π_tableIndex0(aData, aValue)];
	}
	
	/**
	 * Indicates in which position of the given data byte the sum of
	 * function π, starting backward at the specified position, reaches the specified value. 
	 * @param aData The data byte: 0..255
	 * @param aStart The starting position of the sum: 0..7 (0 is the MSB)
	 * @param aValue The searched value: -8..9 (9 means we ask the sum)
	 * @return Positive or 0 means the position (relative to aStart), 
	 * otherwise returns 0x80 | sum until the end of the byte.
	 */
	private byte bwdpos_π(int aData, int aStart, int aValue)
	{
		return TABLE_BWDPOS_π[bwdpos_π_tableIndex(aData, aStart, aValue)];
	}
	
	private byte bwdpos_π(int aData, int aValue)
	{
		return TABLE_BWDPOS_π[bwdpos_π_tableIndex7(aData, aValue)];
	}
	
	private static int getByte(int aData, int aByteNumber)
	{
		switch(aByteNumber)
		{
		case 0: return 0xff & (aData >>> 24);
		case 1: return 0xff & (aData >>> 16);
		case 2: return 0xff & (aData >>> 8);
		case 3: return 0xff & aData;
		default: throw new RuntimeException();
		}		
	}
	
	// For unit tests
	int _test_fwdsearch_π(Page aPage, int i, int d)
	{
		itsLevels[0].setPos(-1);
		return fwdsearch_π(aPage, i, d);
	}
	
	/**
	 * fwdsearch within a page.
	 * @return If positive or 0, the position of the answer (within the page).
	 * Otherwise, 0x80000000 | sum from i until the end of the page
	 */
	private int fwdsearch_π(Page aPage, int i, int d)
	{
		int originalTarget = d;
		
		int packetNumber = i/32;
		int packetOffset = i%32;
		
		int packet = getPacket(aPage, packetNumber);
		
		int byteNumber = packetOffset/8;
		int byteOffset = packetOffset%8;

		if (byteOffset != 0)
		{
			// Check if the answer might be in the remaining portion of the first byte
			byte pos = fwdpos_π(
					getByte(packet, byteNumber), 
					byteOffset, 
					isBetween(d, -8, 8) ? d : 9);
			
			if (pos >= 0) return i+pos;
			else
			{
				pos = (byte) ((byte) (pos << 1) >> 1);
				d -= pos;
				i += 8-byteOffset;
				byteNumber++;
			}
		}
		
		while(true)
		{
			if (byteNumber == 4)
			{
				byteNumber = 0;
				packetNumber++;
				if (packetNumber >= PACKETS_PER_PAGE) return 0x80000000 | (originalTarget-d);
				packet = getPacket(aPage, packetNumber);
			}
			
			byte pos = fwdpos_π(getByte(packet, byteNumber), isBetween(d, -8, 8) ? d : 9);
			if (pos >= 0) return i+pos;
			else
			{
				pos = (byte) ((byte) (pos << 1) >> 1);
				d -= pos;
				i += 8;
				byteNumber++;
			}
		}
	}

	// For unit tests
	int _test_bwdsearch_π(Page aPage, int i, int d)
	{
		itsLevels[0].setPos(-1);
		return bwdsearch_π(aPage, i, d);
	}
	
	/**
	 * Symmetric of fwdsearch_π
	 */
	private int bwdsearch_π(Page aPage, int i, int d)
	{
		int originalTarget = d;
		
		int packetNumber = i/32;
		int packetOffset = i%32;
		
		int packet = getPacket(aPage, packetNumber);
		
		int byteNumber = packetOffset/8;
		int byteOffset = packetOffset%8;
		
		if (byteOffset != 7)
		{
			// Check if the answer might be in the remaining portion of the first byte
			byte pos = bwdpos_π(
					getByte(packet, byteNumber), 
					byteOffset, 
					isBetween(d, -8, 8) ? d : 9);
			
			if (pos >= 0) return i-pos;
			else
			{
				pos = (byte) ((byte) (pos << 1) >> 1);
				d -= pos;
				i -= byteOffset+1;
				byteNumber--;
			}
		}
		
		while(true)
		{
			if (byteNumber == -1)
			{
				byteNumber = 3;
				packetNumber--;
				if (packetNumber < 0) return 0x80000000 | (originalTarget-d);
				packet = getPacket(aPage, packetNumber);
			}
			
			byte pos = bwdpos_π(getByte(packet, byteNumber), isBetween(d, -8, 8) ? d : 9);
			if (pos >= 0) return i-pos;
			else
			{
				pos = (byte) ((byte) (pos << 1) >> 1);
				d -= pos;
				i -= 8;
				byteNumber--;
			}
		}
	}
	
	
	private void fwdsearch_ψ()
	{
		
	}
	
	private void fwdsearch_Φ()
	{
		
	}
	
	
}
