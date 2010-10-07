package tod.impl.evdbng.db.fieldwriteindex;

import tod.impl.evdbng.db.fieldwriteindex.OnDiskIndex.ObjectPageSlot;
import tod.impl.evdbng.db.file.InsertableBTree;
import tod.impl.evdbng.db.file.IntInsertableBTree;
import tod.impl.evdbng.db.file.IntInsertableBTree.IntTuple;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.TupleBufferFactory;

public class ObjectBTree extends InsertableBTree<IntTuple>
{
	
	public ObjectBTree(String aName, PidSlot aRootSlot)
	{
		super(aName, aRootSlot);
	}
	
	@Override
	protected TupleBufferFactory<IntTuple> getTupleBufferFactory()
	{
		return IntInsertableBTree.INT_TUPLEFACTORY;
	}

	/**
	 * Returns a slot that can store a pointer to an object page.
	 * This method insert an entry for the given key if there is none.
	 */
	public ObjectPageSlot getSlot(long aKey)
	{
		PageIOStream theStream = insertLeafKey(aKey, true);
		return new ObjectPageSlot(theStream);
	}
	
	public int get(long aKey)
	{
		IntTuple theTuple = getTupleAt(aKey);
		return theTuple != null ? theTuple.getData() : 0;
	}
}
