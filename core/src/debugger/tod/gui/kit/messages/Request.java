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
package tod.gui.kit.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * A message that permits to obtain a return value.
 * @author gpothier
 */
public class Request<R> extends Message
{
	private List<R> itsResults = new ArrayList<R>(); 
	
	public Request(String aId)
	{
		super(aId);
	}
	
	public void addResult(R aResult)
	{
		itsResults.add(aResult);
	}
	
	public Iterable<R> getResults()
	{
		return itsResults;
	}

	public R getResult()
	{
		if (itsResults.size() != 1) throw new RuntimeException("Expected exactly one result, got "+itsResults.size());
		return itsResults.get(0);
	}
}
