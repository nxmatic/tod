package tod.impl.evdbng.db;

import tod.impl.evdbng.db.file.LongInsertableBTree;
import tod.impl.evdbng.db.file.Page.ChainedPageIOStream;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;

/**
 * An index that stores arbitrary-length strings identified by a long id.
 * @author gpothier
 */
public class StringIndex extends LongInsertableBTree
{
	private ChainedPageIOStream itsDataStream;
	
	public StringIndex(String aName, PidSlot aRootSlot)
	{
		super(aName, aRootSlot);
		// TODO: we should have a directory
		itsDataStream = new ChainedPageIOStream(getFile());
	}

	public void addString(long aId, String aValue)
	{
		PageIOStream theCurrentStream = itsDataStream.getCurrentStream();
		int thePid = theCurrentStream.getPage().getPageId();
		int theOffset = theCurrentStream.getPos();
		assert (theOffset & ~0xffff) == 0;
		long theValue = (thePid << 16) | theOffset;
		add(aId, theValue);
		itsDataStream.writeString(aValue);
	}
}
