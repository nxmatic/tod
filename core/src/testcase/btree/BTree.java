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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


/*
 * Created on May 1, 2006
 */

public class BTree implements Dict
{
	private SimplePageManager itsManager;
	private Node itsRoot;
	
	/**
	 * The anchor is a "false" root of the tree: it is always located
	 * at page 0 and points to the real root.
	 */
	private Node itsAnchor;

	public BTree(File aFile, boolean aWriteCache)
	{
		boolean theFileExists = aFile.exists();
		
		itsManager = new SimplePageManager(aFile, aWriteCache);
		
		if (theFileExists)
		{
			itsAnchor = Node.read(this, 0);
			itsRoot = itsAnchor.readChild(0);
		}
		else
		{
			int theId = itsManager.alloc();
			assert theId == 0;
			itsAnchor = Node.create(this, 0, false);
			
			setRoot(createNode(true));
		}
	}
	
	public Node createNode(boolean aLeaf)
	{
		int theId = getManager().alloc();
		return Node.create(this, theId, aLeaf);
	}
	
	public SimplePageManager getManager()
	{
		return itsManager;
	}
	
	private void setRoot(Node aRoot)
	{
		if (itsRoot != null) itsRoot.setRoot(false);
		itsAnchor.setChildId(0, aRoot.getPageId());
		itsAnchor.write();
		itsRoot = aRoot;
		itsRoot.setRoot(true);
	}
	
	public Long get(long aKey)
	{
		Node theNode = itsRoot;
		while (theNode != null)
		{
			int theLocation = theNode.search(aKey);
			if (theLocation >= 0)
			{
				// We found the key
				return theNode.getValue(theLocation);
			}
			else
			{
				// The key is not in the node, we descend into the appropriate child
				int theChildIndex = -theLocation-1;
				theNode = theNode.readChild(theChildIndex);
			}
		}
		
		return null;
	}
	
	public boolean put(long aKey, long aValue)
	{
		Node theNode = itsRoot;
		
		// 1. Check if we must split the root
		if (theNode.getCount() == Config.maxKeys())
		{
			Node theNewRoot = createNode(false);
			theNewRoot.setChildId(0, itsRoot.getPageId());
			theNewRoot.split(0, itsRoot);
			setRoot(theNewRoot);
			theNode = theNewRoot;
		}
		
		// 2. Search insertion point
		while (! theNode.isLeaf())
		{
			int theLocation = theNode.search(aKey);
			if (theLocation >= 0)
			{
				// The key already exists: we simply replace the value
				theNode.setValue(theLocation, aValue);
				theNode.write();
				return false;
			}
			else
			{
				int theChildIndex = -theLocation-1;
				Node theChildNode = theNode.readChild(theChildIndex);
				if (theChildNode.getCount() == Config.maxKeys())
				{
					// If the child node is full we must split it.
					Node theNewNode = theNode.split(theChildIndex, theChildNode);
					long theUpKey = theNode.getKey(theChildIndex);
					if (aKey == theUpKey)
					{
						// Check if the key that went up is our key
						theNode.setValue(theChildIndex, aValue);
						theNode.write();
						return false;						
					}
					else if (aKey > theUpKey) theChildNode = theNewNode;
				}
				theNode = theChildNode;
			}
		}
		
		// 3. Insert the key into the (non full) leaf
		int theLocation = theNode.search(aKey);
		if (theLocation >= 0)
		{
			// The key already exists: we simply replace the value
			theNode.setValue(theLocation, aValue);
			theNode.write();
			return false;
		}
		else
		{
			int theChildIndex = -theLocation-1;
			theNode.insert(theChildIndex, aKey, aValue);
			theNode.write();
			return true;
		}
	}
	
	public boolean remove(long aKey)
	{
		Remove theHelper = new Remove(aKey);
		return theHelper.remove();
	}
	
	@Override
	public String toString()
	{
		LinkedList<Node> theQueue = new LinkedList<Node>();
		theQueue.addLast(itsRoot);
		
		StringBuilder theBuilder = new StringBuilder();
		while (! theQueue.isEmpty())
		{
			Node theNode = theQueue.removeFirst();
			if (theNode == null)
			{
				theBuilder.append("\r\n");
			}
			else
			{
				theBuilder.append(theNode);
				theBuilder.append(" | ");
				
				if (! theNode.isLeaf()) 
				{
					theQueue.addLast(null);
					for (int i=0;i<theNode.getCount()+1;i++)
					{
						theQueue.addLast(theNode.readChild(i));
					}
				}
			}
		}
		
		return theBuilder.toString();
	}
	
	private static class NodeMarker
	{
		private Set<Node> itsModifiedNodes = new HashSet<Node>();
		
		/**
		 * Marks a node as needing to be saved.
		 */
		public void markNode(Node aNode)
		{
			aNode.check();
			itsModifiedNodes.add(aNode);
		}
		
		/**
		 * Saves all previously marked nodes.
		 */
		public void saveNodes()
		{
			for (Node theNode : itsModifiedNodes)
			{
				theNode.write();
			}
		}
		
	}
	
	/**
	 * A class that permits to walk down the tree maintaining the invariant that the 
	 * current node always has at least one more key than the minimum.
	 * @author gpothier
	 */
	private class RemoveWalker
	{
		private NodeMarker itsMarker;
		private Node itsNode;
		
		public RemoveWalker(NodeMarker aMarker, Node aNode)
		{
			itsMarker = aMarker;
			itsNode = aNode;
		}

		public Node getNode()
		{
			return itsNode;
		}

		/**
		 * Descends into a specific child of the current node,
		 * maintaining the invariant that the current node always
		 * has at least one more key than the minimum.
		 */
		public void descend(int aChildIndex)
		{
			assert ! itsNode.isLeaf();
			
			Node theChild = itsNode.readChild(aChildIndex);
			if (theChild.getCount() > Config.minKeys())
			{
				// The child has at least one more key than the minimum,
				// we can directly recurse into it
				itsNode = theChild;
			}
			else
			{
				// The child only has the minimum number of keys,
				// we must add it one key.
				Node theLeftNeighbour = aChildIndex > 0 ?
						itsNode.readChild(aChildIndex-1)
						: null;
						
				Node theRightNeighbour = aChildIndex < itsNode.getCount() ?
						itsNode.readChild(aChildIndex+1)
						: null;
				
				int theShiftResult = shiftKey(
						itsNode, 
						aChildIndex, 
						theChild, 
						theLeftNeighbour, 
						theRightNeighbour);
				
				if (theShiftResult != 0)
				{
					itsNode = theChild;
				}
				else if (theShiftResult == 0)
				{
					if (theLeftNeighbour != null) 
					{
						itsNode.mergeLeft(aChildIndex, theChild, theLeftNeighbour);
						theChild.free();
						itsMarker.markNode(theLeftNeighbour);
						if (itsNode.getCount() == 0)
						{
							assert itsNode == itsRoot;
							setRoot(theLeftNeighbour);
						}
						else
						{
							itsMarker.markNode(itsNode);
						}
						itsNode = theLeftNeighbour;
					}
					else
					{
						itsNode.mergeRight(aChildIndex, theChild, theRightNeighbour);
						theRightNeighbour.free();
						itsMarker.markNode(theChild);
						if (itsNode.getCount() == 0)
						{
							assert itsNode == itsRoot;
							setRoot(theChild);
						}
						else
						{
							itsMarker.markNode(itsNode);
						}
						itsNode = theChild;
					}
				}
			}
		}
		
		/**
		 * Tries to shift one key from a neighbour of a child.
		 * @return -1 if the child from which the key was taken was the left neighbout,
		 * +1 if it was the right neighbour, 0 if the neighbours didn't have enough keys
		 * to allow a shift 
		 */
		private int shiftKey(
				Node aParent, 
				int aChildIndex, 
				Node aChild, 
				Node aLeftNeighbour,
				Node aRightNeighbour)
		{
			assert (aLeftNeighbour == null || aChild.isLeaf() == aLeftNeighbour.isLeaf())
				&& (aRightNeighbour == null || aChild.isLeaf() == aRightNeighbour.isLeaf());
			
			if (aLeftNeighbour != null && aLeftNeighbour.getCount() > Config.minKeys())
			{
				long theDownKey = aParent.getKey(aChildIndex-1);
				long theDownValue = aParent.getValue(aChildIndex-1);
				
				long theUpKey = aLeftNeighbour.getKey(aLeftNeighbour.getCount()-1);
				long theUpValue= aLeftNeighbour.getValue(aLeftNeighbour.getCount()-1);
				
				int theShiftedChild = aLeftNeighbour.getChildId(aLeftNeighbour.getCount());
				
				aChild.insertHead(theShiftedChild, theDownKey, theDownValue);
				itsMarker.markNode(aChild);
				
				aParent.setKey(aChildIndex-1, theUpKey);
				aParent.setValue(aChildIndex-1, theUpValue);
				itsMarker.markNode(aParent);
				
				aLeftNeighbour.truncate(aLeftNeighbour.getCount()-1);
				itsMarker.markNode(aLeftNeighbour);
				
				return -1;
			}
			else if (aRightNeighbour != null && aRightNeighbour.getCount() > Config.minKeys())
			{
				long theDownKey = aParent.getKey(aChildIndex);
				long theDownValue = aParent.getValue(aChildIndex);
				
				long theUpKey = aRightNeighbour.getKey(0);
				long theUpValue = aRightNeighbour.getValue(0);
				
				int theShiftedChild = aRightNeighbour.getChildId(0);
				
				aChild.insertTail(theShiftedChild, theDownKey, theDownValue);
				itsMarker.markNode(aChild);
				
				aParent.setKey(aChildIndex, theUpKey);
				aParent.setValue(aChildIndex, theUpValue);
				itsMarker.markNode(aParent);

				aRightNeighbour.removeHead();
				itsMarker.markNode(aRightNeighbour);
				
				return 1;
			}
			else return 0;
		}

		

	}
	
	/**
	 * We create a special class to handle the deletion because we need to maintain 
	 * a lot of state during the operation and we don't want to implement it
	 * in a single method. 
	 * @author gpothier
	 */
	private class Remove
	{
		private NodeMarker itsMarker = new NodeMarker();
		private RemoveWalker itsWalker;
		private long itsKey;
		
		public Remove(long aKey)
		{
			itsKey = aKey;
			itsWalker = new RemoveWalker(itsMarker, itsRoot);
		}


		public boolean remove()
		{
			while (itsWalker.getNode() != null)
			{
				Node theNode = itsWalker.getNode();
				int theLocation = theNode.search(itsKey);
				if (theLocation >= 0)
				{
					// We found the key to delete in the current node
					if (theNode.isLeaf())
					{
						// Just delete
						theNode.removeFromLeaf(theLocation);
						itsMarker.markNode(theNode);
						itsMarker.saveNodes();
						return true;
					}
					else
					{
						Node thePreviousChild = theNode.readChild(theLocation);
						Node theNextChild = theNode.readChild(theLocation+1);
						
						if (thePreviousChild.getCount() == Config.minKeys()
								&& theNextChild.getCount() == Config.minKeys())
						{
							// We can merge the children at each side of the key.
							theNode.mergeRight(theLocation, thePreviousChild, theNextChild);
							itsMarker.markNode(theNode);
							itsMarker.markNode(thePreviousChild);
							theNextChild.free();
							theNode = thePreviousChild;
						}
						else
						{
							Entry theEntry;
							
							if (thePreviousChild.getCount() > Config.minKeys())
							{
								theEntry = deletePredecessor(thePreviousChild);
							}
							else if (theNextChild.getCount() > Config.minKeys())
							{
								theEntry = deleteSuccessor(theNextChild);
							}
							else throw new RuntimeException("Cannot happen");
							
							theNode.setKey(theLocation, theEntry.key);
							theNode.setValue(theLocation, theEntry.value);
							itsMarker.markNode(theNode);
							itsMarker.saveNodes();
							return true;
						}
					}
				}
				else
				{
					// The key to delete is not in the current node
					if (theNode.isLeaf()) break;
					int theChildIndex = -theLocation-1;
					itsWalker.descend(theChildIndex);
				}
			}
			
			itsMarker.saveNodes();
			return false;
		}
		
		/**
		 * Deletes the successor of the key to delete, and returns it.
		 * @param aRoot The root of the subtree that contains the successor
		 * @return The 
		 */
		private Entry deleteSuccessor(Node aNode)
		{
			RemoveWalker theWalker = new RemoveWalker(itsMarker, aNode);
			while (! theWalker.getNode().isLeaf()) theWalker.descend(0);

			Node theNode = theWalker.getNode();
			Entry theEntry = new Entry(
								theNode.getKey(0),
								theNode.getValue(0));
			
			theNode.removeFromLeaf(0);
			itsMarker.markNode(theNode);
			
			return theEntry;
		}

		/**
		 * Symmetric of {@link #deleteSuccessor(Node)}
		 */
		private Entry deletePredecessor(Node aNode)
		{
			RemoveWalker theWalker = new RemoveWalker(itsMarker, aNode);
			while (! theWalker.getNode().isLeaf()) theWalker.descend(theWalker.getNode().getCount());
			
			Node theNode = theWalker.getNode();
			Entry theEntry = new Entry(
								theNode.getKey(theNode.getCount()-1),
								theNode.getValue(theNode.getCount()-1));
			
			theNode.removeFromLeaf(theNode.getCount()-1);
			itsMarker.markNode(theNode);
			
			return theEntry;
		}
	}
	
	private static class Entry
	{
		public final long key;
		public final long value;
		
		public Entry(long aKey, long aValue)
		{
			key = aKey;
			value = aValue;
		}
	}
}
