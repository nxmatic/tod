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
package tod.impl.database.structure.standard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.core.database.structure.IBehaviorInfo.BytecodeTagType;
import zz.utils.Utils;

/**
 * Manages the association between bytecode and tags.
 * There are various types of tags (see {@link BytecodeTagType}),
 * and each bytecode can have a tag for each type.
 * @author gpothier
 */
public class TagMap implements Serializable
{
	private static final long serialVersionUID = 1045425445284384491L;
	
	private final Map<String, List> itsTagsMap = new HashMap<String, List>();

	/**
	 * Returns the tag associated to a given bytecode.
	 */
	public <T> T getTag(BytecodeTagType<T> aType, int aBytecodeIndex)
	{
		List<T> theTags = itsTagsMap.get(aType.getName());
		return theTags != null ? 
				Utils.listGet(theTags, aBytecodeIndex)
				: null;
	}

	private <T> List<T> getTags(BytecodeTagType<T> aType)
	{
		List<T> theTags = itsTagsMap.get(aType.getName());
		if (theTags == null)
		{
			theTags = new ArrayList<T>();
			itsTagsMap.put(aType.getName(), theTags);
		}

		return theTags;
	}
	
	/**
	 * Adds a tag to a specific bytecode.
	 */
	public <T> void putTag(BytecodeTagType<T> aType, T aTag, int aBytecodeIndex)
	{
		Utils.listSet(getTags(aType), aBytecodeIndex, aTag);
	}
	
	/**
	 * Adds a tag to a range of bytecode.
	 * @param aType The tag type for which the tag is being set.
	 * @param aTag The tag value
	 * @param aStart The start of the range, inclusive.
	 * @param aEnd The end of the range, exclusive.
	 */
	public <T> void putTagRange(BytecodeTagType<T> aType, T aTag, int aStart, int aEnd)
	{
		List<T> theTags = getTags(aType);
		for(int i=aStart;i<aEnd;i++) Utils.listSet(theTags, i, aTag);
	}

}
