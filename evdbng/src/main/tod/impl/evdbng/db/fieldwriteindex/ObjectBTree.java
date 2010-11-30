package tod.impl.evdbng.db.fieldwriteindex;

import tod.impl.evdbng.db.Stats;
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
		super(aName, Stats.ACC_OBJECTS, aRootSlot);
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
		ObjectPageSlot theSlot = new ObjectPageSlot(theStream);
		if (Stats.COLLECT)
		{
			if (theSlot.isNull()) Stats.OBJECT_TREE_ENTRIES++;
		}
		return theSlot;
	}
	
	public int get(long aKey)
	{
		IntTuple theTuple = getTupleAt(aKey, true);
		return theTuple != null ? theTuple.getData() : 0;
	}
}
