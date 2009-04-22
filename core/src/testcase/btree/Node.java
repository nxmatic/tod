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
package btree;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


/*
 * Created on May 1, 2006
 */

public class Node
{
	/**
	 * The tree that owns this node.
	 */
	private BTree itsTree;
	
	private int itsPageId;
	
	/**
	 * Current number of used keys in this node.
	 */
	private int itsCount;
	private boolean itsLeaf;
	private boolean itsRoot;
	
	private long[] itsKeys = new long[Config.maxKeys()];
	private long[] itsValues = new long[Config.maxKeys()];
	
	private int[] itsChildrenIds = new int[Config.maxChildren()];

	public Node(BTree aTree, int aPageId, boolean aLeaf)
	{
		itsTree = aTree;
		itsPageId = aPageId;
		itsLeaf = aLeaf;
		if (itsLeaf) itsChildrenIds[0] = -1;
	}

	/**
	 * For testing only
	 */
	public Node(int aPageId, int aCount, long[] aKeys, long[] aValues, int[] aChildrenIds)
	{
		itsLeaf = false;
		itsPageId = aPageId;
		itsCount = aCount;
		itsKeys = aKeys;
		itsValues = aValues;
		itsChildrenIds = aChildrenIds;
		check();
	}

	/**
	 * Verifies that the node has the specified characteristics.
	 */
	public void assertNode(int aPageId, int aCount, long[] aKeys, long[] aValues, int[] aChildrenIds)
	{
		assert itsPageId == aPageId;
		assert itsCount == aCount;
		assert Arrays.equals(itsKeys, aKeys);
		assert Arrays.equals(itsValues, aValues);
		assert Arrays.equals(itsChildrenIds, aChildrenIds);
	}
	

	/**
	 * Loads a node from an array of bytes.
	 * @param aBytes
	 * @return
	 */
	public static Node read(BTree aTree, int aPageId)
	{
		byte[] theData = aTree.getManager().read(aPageId);
		DataInputStream theStream = new DataInputStream(new ByteArrayInputStream(theData));
		
		Node theNode = new Node(aTree, aPageId, false);
		
		try
		{
			theNode.itsCount = theStream.readInt();
			theNode.itsLeaf = theStream.readBoolean();
			
			for (int i=0;i<Config.maxKeys();i++)
			{
				theNode.itsKeys[i] = theStream.readLong();
				theNode.itsValues[i] = theStream.readLong();
			}

			for (int i=0;i<Config.maxChildren();i++)
			{
				theNode.itsChildrenIds[i] = theStream.readInt();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		theNode.check();
		return theNode;
	}
	
	public static Node create(BTree aTree, int aPageId, boolean aLeaf)
	{
		Node theNode = new Node(aTree, aPageId, aLeaf);
		theNode.itsCount = Config.maxKeys();
		theNode.truncate(0);
		return theNode;
	}
	
	public void write()
	{
		check();
		
		ByteArrayOutputStream theByteStream = new ByteArrayOutputStream();
		DataOutputStream theStream = new DataOutputStream(theByteStream);
		
		try
		{
			theStream.writeInt(itsCount);
			theStream.writeBoolean(itsLeaf);

			for (int i=0;i<Config.maxKeys();i++)
			{
				theStream.writeLong(itsKeys[i]);
				theStream.writeLong(itsValues[i]);
			}

			for (int i=0;i<Config.maxChildren();i++)
			{
				theStream.writeInt(itsChildrenIds[i]);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		byte[] theData = theByteStream.toByteArray();
		assert theData.length == Config.pageSize();
		
		itsTree.getManager().write(itsPageId, theData);
	}
	
	public void free()
	{
		itsTree.getManager().free(getPageId());
		itsPageId = -1;
	}
	
	/**
	 * Checks the integrity of this node
	 */
	public void check()
	{
		assert itsCount <= Config.maxKeys();
		
		long thelastKey = Long.MIN_VALUE;
		for (int i=0;i<itsCount;i++)
		{ 
			assert itsKeys[i] > thelastKey;
			thelastKey = itsKeys[i];
		}
		
		for (int i=itsCount;i<Config.maxKeys();i++)
		{
			assert itsKeys[i] == -1;
			assert itsValues[i] == -1;
		}
		
		for (int i=0;i<Config.maxChildren();i++)
		{
			assert i >= itsCount+1 || isLeaf() ? itsChildrenIds[i] == -1 : itsChildrenIds[i] >= 0;
		}
	}
	
	public int getPageId()
	{
		return itsPageId;
	}

	public int getChildId(int aIndex)
	{
		if (isLeaf()) return -1;
		else return itsChildrenIds[aIndex];
	}
	
	public Node readChild(int aIndex)
	{
		if (isLeaf()) return null;
		else return read(itsTree, getChildId(aIndex));
	}
	
	public void setChildId(int aIndex, int aId)
	{
		assert ! isLeaf();
		itsChildrenIds[aIndex] = aId;
	}
	
	public long getKey(int aIndex)
	{
		return itsKeys[aIndex];
	}
	
	public void setKey(int aIndex, long aKey)
	{
		itsKeys[aIndex] = aKey;
	}
	
	public long getValue(int aIndex)
	{
		return itsValues[aIndex];
	}
	
	public void setValue(int aIndex, long aValue)
	{
		itsValues[aIndex] = aValue;
	}
	
	public boolean isLeaf()
	{
		return itsLeaf;
	}
	
	

	public boolean isRoot()
	{
		return itsRoot;
	}

	public void setRoot(boolean aRoot)
	{
		itsRoot = aRoot;
	}

	public int getCount()
	{
		return itsCount;
	}
	
	public void truncate(int aNewCount)
	{
		assert aNewCount < itsCount;
		for (int i=aNewCount;i<itsCount;i++)
		{
			itsKeys[i] = -1;
			itsValues[i] = -1;
			itsChildrenIds[i+1] = -1;
		}
		itsCount = aNewCount;
	}

	/**
	 * Searches for a key in this node. If the key is found its index
	 * is returned. Otherwise, the opposite minus one of the index of the subtree that should
	 * contain the key is returned.
	 */
	public int search(long aKey)
	{
		for (int i=0;i<itsCount;i++)
		{
			long theKey = itsKeys[i];
			if (theKey == aKey) return i;
			if (theKey > aKey) return -i-1;
		}
		return -itsCount-1;
	}
	
	/**
	 * Splits the child of this node at the specified index. The child node must be full
	 * and this node must not be full.
	 * @param aChildIndex The index of the node to split
	 * @param aChild The node to split (provided here because it is already loaded
	 * when this method is called).
	 * @return Returns the newly created node.
	 */
	public Node split(int aChildIndex, Node aChild)
	{
		assert itsCount < Config.maxKeys(); // must not be full
		assert aChild.itsCount == Config.maxKeys(); // child must be full
		
		Node theNewNode = itsTree.createNode(aChild.isLeaf());
		theNewNode.itsCount = Config.minKeys();
		
		// Copy the tail of the splited node to the new node
		System.arraycopy(
				aChild.itsKeys, Config.maxKeys()-Config.minKeys(), 
				theNewNode.itsKeys, 0, 
				Config.minKeys());
		
		System.arraycopy(
				aChild.itsValues, Config.maxKeys()-Config.minKeys(), 
				theNewNode.itsValues, 0, 
				Config.minKeys());
		
		if (! aChild.isLeaf())
		{
			System.arraycopy(
					aChild.itsChildrenIds, Config.maxChildren()-Config.minChildren(), 
					theNewNode.itsChildrenIds, 0, 
					Config.minChildren());
		}
		
		// Shift children, keys and values of this node by one.
		itsCount++;
		
		if (itsCount-aChildIndex > 1)
		{
			System.arraycopy(
					itsChildrenIds, aChildIndex+1, 
					itsChildrenIds, aChildIndex+2, 
					itsCount-aChildIndex-1);
			
			System.arraycopy(
					itsKeys, aChildIndex, 
					itsKeys, aChildIndex+1,
					itsCount-aChildIndex-1);
			
			System.arraycopy(
					itsValues, aChildIndex, 
					itsValues, aChildIndex+1,
					itsCount-aChildIndex-1);
		}

		// Place new child and middle key
		itsChildrenIds[aChildIndex+1] = theNewNode.itsPageId;
		itsKeys[aChildIndex] = aChild.getKey(Config.maxKeys()-Config.minKeys()-1);
		itsValues[aChildIndex] = aChild.getValue(Config.maxKeys()-Config.minKeys()-1);
		aChild.truncate(Config.minKeys());
		
		// Write out all modified nodes
		write();
		aChild.write();
		theNewNode.write();
		
		return theNewNode;
	}
	
	/**
	 * Merges a child of this node with its left neighbour. After calling this method
	 * the left neighbour and this node must be saved, and the child can be freed.
	 */
	public void mergeLeft(int aChildIndex, Node aChild, Node aLeftNeighbour)
	{
		assert isRoot() || itsCount > Config.minKeys();
		assert aChild.itsCount == Config.minKeys();
		assert aLeftNeighbour.itsCount == Config.minKeys();
		
		long theDownKey = getKey(aChildIndex-1);
		long theDownValue = getValue(aChildIndex-1);
		
		aLeftNeighbour.itsCount = Config.maxKeys();
		aLeftNeighbour.itsKeys[Config.minKeys()] = theDownKey;
		aLeftNeighbour.itsValues[Config.minKeys()] = theDownValue;
		
		System.arraycopy(aChild.itsKeys, 0, aLeftNeighbour.itsKeys, Config.minKeys()+1, Config.minKeys());
		System.arraycopy(aChild.itsValues, 0, aLeftNeighbour.itsValues, Config.minKeys()+1, Config.minKeys());
		System.arraycopy(aChild.itsChildrenIds, 0, aLeftNeighbour.itsChildrenIds, Config.minChildren(), Config.minChildren());
		
		System.arraycopy(itsKeys, aChildIndex, itsKeys, aChildIndex-1, itsCount-aChildIndex);
		System.arraycopy(itsValues, aChildIndex, itsValues, aChildIndex-1, itsCount-aChildIndex);
		System.arraycopy(itsChildrenIds, aChildIndex+1, itsChildrenIds, aChildIndex, itsCount-aChildIndex);
		
		truncate(itsCount-1);
	}
	
	/**
	 * Merges a child of this node with its right neighbour. After calling this method
	 * the child and this node must be saved, and the neighbour can be freed.
	 */
	public void mergeRight(int aChildIndex, Node aChild, Node aRightNeighbour)
	{
		assert isRoot() || itsCount > Config.minKeys();
		assert aChild.itsCount == Config.minKeys();
		assert aRightNeighbour.itsCount == Config.minKeys();
		
		long theDownKey = getKey(aChildIndex);
		long theDownValue = getValue(aChildIndex);
		
		aChild.itsCount = Config.maxKeys();
		aChild.itsKeys[Config.minKeys()] = theDownKey;
		aChild.itsValues[Config.minKeys()] = theDownValue;
		
		System.arraycopy(aRightNeighbour.itsKeys, 0, aChild.itsKeys, Config.minKeys()+1, Config.minKeys());
		System.arraycopy(aRightNeighbour.itsValues, 0, aChild.itsValues, Config.minKeys()+1, Config.minKeys());
		System.arraycopy(aRightNeighbour.itsChildrenIds, 0, aChild.itsChildrenIds, Config.minChildren(), Config.minChildren());
		
		System.arraycopy(itsKeys, aChildIndex+1, itsKeys, aChildIndex, itsCount-aChildIndex-1);
		System.arraycopy(itsValues, aChildIndex+1, itsValues, aChildIndex, itsCount-aChildIndex-1);
		System.arraycopy(itsChildrenIds, aChildIndex+2, itsChildrenIds, aChildIndex+1, itsCount-aChildIndex-1);

		truncate(itsCount-1);
	}
	
	public void insert (int aIndex, long aKey, long aValue)
	{
		assert isLeaf();
		assert itsCount < Config.maxKeys();
		
		itsCount++;
		System.arraycopy(itsKeys, aIndex, itsKeys, aIndex+1, itsCount-aIndex-1);
		System.arraycopy(itsValues, aIndex, itsValues, aIndex+1, itsCount-aIndex-1);
		itsKeys[aIndex] = aKey;
		itsValues[aIndex] = aValue;
	}
	
	public void insertHead(int aChildId, long aKey, long aValue)
	{
		assert itsCount < Config.maxKeys();
		
		itsCount++;
		System.arraycopy(itsKeys, 0, itsKeys, 1, itsCount-1);
		System.arraycopy(itsValues, 0, itsValues, 1, itsCount-1);
		System.arraycopy(itsChildrenIds, 0, itsChildrenIds, 1, itsCount);
		
		itsKeys[0] = aKey;
		itsValues[0] = aValue;
		itsChildrenIds[0] = aChildId;
	}
	
	public void insertTail(int aChildId, long aKey, long aValue)
	{
		assert itsCount < Config.maxKeys();

		itsCount++;
		itsKeys[itsCount-1] = aKey;
		itsValues[itsCount-1] = aValue;
		itsChildrenIds[itsCount] = aChildId;		
	}
	
	/**
	 * Removes the first key, value and child from this node.
	 */
	public void removeHead()
	{
		System.arraycopy(itsKeys, 1, itsKeys, 0, itsCount-1);
		System.arraycopy(itsValues, 1, itsValues, 0, itsCount-1);
		System.arraycopy(itsChildrenIds, 1, itsChildrenIds, 0, itsCount);
		truncate(itsCount-1);
	}
	
	/**
	 * Removes the key and value at the specified index from this node.
	 * This node must be a leaf.
	 */
	public void removeFromLeaf(int aIndex)
	{
		assert isLeaf();
		System.arraycopy(itsKeys, aIndex+1, itsKeys, aIndex, itsCount-aIndex-1);
		System.arraycopy(itsValues, aIndex+1, itsValues, aIndex, itsCount-aIndex-1);
		
		truncate(itsCount-1);
	}
	
	@Override
	public String toString()
	{
		return (isLeaf() ? "Leaf" : "Node") +" (pid: "+itsPageId+", n: "+itsCount+", k: "+toString(itsKeys)+", v: "+toString(itsValues)+", c: "+toString(itsChildrenIds)+")";
	}
	
	private static String toString (long[] aArray)
	{
		StringBuilder theBuilder = new StringBuilder();
		for (long theValue : aArray)
		{
			theBuilder.append(theValue+", ");
		}
		return theBuilder.toString();
	}

	private static String toString (int[] aArray)
	{
		StringBuilder theBuilder = new StringBuilder();
		for (int theValue : aArray)
		{
			theBuilder.append(theValue+", ");
		}
		return theBuilder.toString();
	}
}
