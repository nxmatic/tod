package tod.impl.evdbng.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import tod.impl.evdbng.db.file.LongInsertableBTree;
import tod.impl.evdbng.db.file.LongInsertableBTree.LongTuple;
import tod.impl.evdbng.db.file.Page.ChainedPageIOStream;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.replay2.LocalsSnapshot;

/**
 * An index that stores arbitrary-length strings identified by a long id.
 * @author gpothier
 */
public class SnapshotIndex extends LongInsertableBTree
{
	private ChainedPageIOStream itsDataStream;
	
	public SnapshotIndex(String aName, PidSlot aRootSlot)
	{
		super(aName, aRootSlot);
		// TODO: we should have a directory
		itsDataStream = new ChainedPageIOStream(getFile());
	}

	public void addSnapshot(long aId, LocalsSnapshot aSnapshot)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(aSnapshot);
			oos.flush();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		byte[] theData = baos.toByteArray();
		
		PageIOStream theCurrentStream = itsDataStream.getCurrentStream();
		int thePid = theCurrentStream.getPage().getPageId();
		int theOffset = theCurrentStream.getPos();
		assert (theOffset & ~0xffff) == 0;
		long theValue = (thePid << 16) | theOffset;
		add(aId, theValue);
		itsDataStream.writeInt(theData.length, 0);
		itsDataStream.writeBytes(theData, 0, theData.length);
	}
	
	public LocalsSnapshot getSnapshot(long aId)
	{
		LongTuple theTuple = getTupleAt(aId);
		if (theTuple == null) return null;
		
		long theValue = theTuple.getData();
		int thePid = (int) (theValue >>> 16);
		int theOffset = (int) (theValue & 0xffff);
		
		PageIOStream theStream = new PageIOStream(getFile().get(thePid), theOffset);
		int theSize = theStream.readInt();
		byte[] theData = new byte[theSize];
		theStream.readBytes(theData, 0, theSize);
		
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(theData);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return (LocalsSnapshot) ois.readObject();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
