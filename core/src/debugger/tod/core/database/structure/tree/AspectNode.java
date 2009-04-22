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
package tod.core.database.structure.tree;

import java.util.Collections;

import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.ILocationInfo;
import zz.utils.tree.SimpleTree;

public class AspectNode extends LocationNode
{
	private final boolean itsShowAdvices;

	public AspectNode(
			SimpleTree<ILocationInfo> aTree, 
			IAspectInfo aAspect, 
			boolean aShowAdvices)
	{
		super(aTree, ! aShowAdvices, aAspect);
		itsShowAdvices = aShowAdvices;
	}

	public IAspectInfo getAspectInfo()
	{
		return (IAspectInfo) getLocation();
	}
	
	@Override
	protected void init()
	{
		System.out.println("Init for "+getAspectInfo());
		
		if (itsShowAdvices) for(IAdviceInfo theAdvice : getAspectInfo().getAdvices())
			addAdviceNode(theAdvice);
	}
	
	/**
	 * Adds a new advice node
	 */
	public AdviceNode addAdviceNode(IAdviceInfo aAdvice)
	{
		int theIndex = Collections.binarySearch(
				pChildren().get(), 
				aAdvice.getName(),
				AdviceComparator.ADVICE);
		
		if (theIndex >= 0) throw new RuntimeException("Advice already exists: "+aAdvice); 
		AdviceNode theNode = new AdviceNode(getTree(), aAdvice);

		pChildren().add(-theIndex-1, theNode);
		return theNode;
	}

}