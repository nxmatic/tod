package tod.impl.evdbng.db.file;

import static tod.impl.evdbng.db.file.PagedFile.PAGE_SIZE;
import tod.impl.evdbng.db.file.Page.PageIOStream;


/**
 * Succinct representation of a tree, based on the SODA'10 paper
 * of Sadakane and Navarro (http://www.dcc.uchile.cl/~gnavarro/publ.html).
 * 
 * This structure can store a tree of n <= w^c nodes.
 * @author gpothier
 */
public class RangeMinMaxTree
{
	private final PagedFile itsFile;
	private static final int BITS_PER_PAGE = PAGE_SIZE*8;
	private static final int MAX_LEVELS = 5;
	
	/**
	 * The integer MASKS[i] has the (32-i)th bit set and all other bits are 0.
	 */
	private static final int[] MASKS = new int[32];
	
	static
	{
		for(int i=31, j=1;i>=0;i--, j<<=1) MASKS[i] = j; 
	}
	
	/**
	 * The first page of each level of the tree. Index 0 corresponds to the leaves.
	 */
	private PageIOStream[] itsLevels = new PageIOStream[MAX_LEVELS];
	
	private int itsCurrentPacket;
	private int itsCurrentPacketMask = MASKS[0];
	
	private int itsCurrentSum = 0;
	
	private int[] itsCurrentMin = new int[MAX_LEVELS];
	private int[] itsCurrentMax = new int[MAX_LEVELS]; 
	
	public RangeMinMaxTree(PagedFile aFile)
	{
		itsFile = aFile;
		itsLevels[0] = itsFile.create().asIOStream();
	}
	
	public void open()
	{
		itsCurrentPacket |= itsCurrentPacketMask;
		itsCurrentPacketMask >>>= 1;
		
		itsCurrentSum++;
		if (itsCurrentMax[0] < itsCurrentSum) itsCurrentMax[0] = itsCurrentSum;
		
		if (itsCurrentPacketMask == 0) writePacket();
	}
	
	public void close()
	{
		itsCurrentPacketMask >>>= 1;
		
		itsCurrentSum--;
		if (itsCurrentMin[0] > itsCurrentSum) itsCurrentMin[0] = itsCurrentSum;
		
		if (itsCurrentPacketMask == 0) writePacket();
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
	
	private boolean isShort(int aValue)
	{
		return aValue >= Short.MIN_VALUE && aValue <= Short.MAX_VALUE;
	}
	
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
			stream = itsFile.create().asIOStream();
			itsLevels[l+1] = stream;
		}
		
		stream.writeSSSI((short) itsCurrentSum, (short) itsCurrentMin[l], (short) itsCurrentMax[l], itsLevels[l].getPage().getPageId());
		if (stream.remaining() < 10) commitLevel(l+1);
		
		itsLevels[l] = itsFile.create().asIOStream();
		
		itsCurrentMin[l] = itsCurrentMax[l] = 0;
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
	 * Retrives a packet from a leaf node, checking if the requested packet is the current packet
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
			long sPage = n/(PAGE_SIZE/10);
			int offset = (int) n%(PAGE_SIZE/10);
			Page parent = getNthPage(sPage, aLevel+1);
			
			if (parent.getPageId() == up.getPage().getPageId())
			{
				// We might be trying to access the current page
				if (up.getPos() == offset*10) return itsLevels[aLevel].getPage();
			}
			
			int id = parent.readInt(offset*10 + 6);
			return itsFile.get(id);
		}
	}
	
	public void fwdsearch_π()
	{
		
	}
	
	public void fwdsearch_ψ()
	{
		
	}
	
	public void fwdsearch_Φ()
	{
		
	}
	
	
}
