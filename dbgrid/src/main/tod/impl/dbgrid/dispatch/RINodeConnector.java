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
package tod.impl.dbgrid.dispatch;

import java.io.Serializable;

import tod.core.config.TODConfig;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.ITypeInfo;
import tod.impl.dbgrid.IGridEventFilter;
import tod.impl.dbgrid.db.RIBufferIterator;
import tod.impl.dbgrid.db.RINodeEventIterator;
import tod.impl.dbgrid.db.ObjectsDatabase.Decodable;
import tod.tools.monitoring.RIMonitoringServerProvider;
import zz.utils.net.Server.ServerAdress;
import zz.utils.srpc.IRemote;

/**
 * Remote interface for {@link NodeConnector}
 * @author gpothier
 */
public interface RINodeConnector extends IRemote, RIMonitoringServerProvider
{
	public int getNodeId();
	
	/**
	 * Sets a new config for the node.
	 */
	public void setConfig(TODConfig aConfig);
	
	/**
	 * Tells this node to establish its incoming data connection to
	 * the grid master at the specified adress.
	 */
	public void connectEventStream(ServerAdress aAdress, IHostInfo aHostInfo);

	/**
	 * Flushes currently bufferred events.
	 * @return Number of flushed events.
	 */
	public int flush();
	
	/**
	 * Initializes or reinitializes the database.
	 */
	public void clear();
	
	/**
	 * Creates a new event iterator for the given condition.
	 */
	public RINodeEventIterator getIterator(IGridEventFilter aCondition);
	
	/**
	 * Semantic matches {@link IEventBrowser#getEventCounts(long, long, int)}
	 */
	public long[] getEventCounts(
			IGridEventFilter aCondition,
			long aT1,
			long aT2,
			int aSlotsCount, 
			boolean aForceMergeCounts);
	
	public long getEventsCount();
	public long getDroppedEventsCount();
	public long getObjectsStoreSize();
	public long getFirstTimestamp();
	public long getLastTimestamp();
	
	/**
	 * Returns an object registered by this dispatcher, or null
	 * if not found.
	 */
	public Decodable getRegisteredObject(long aId);
	
	/**
	 * Returns the type of the given object.
	 */
	public ITypeInfo getObjectType(long aId);
	
	/**
	 * Searches the strings that match the given text.
	 * Returns an iterator of object ids of matching strings, ordered
	 * by relevance.
	 */
	public RIBufferIterator<StringSearchHit[]> searchStrings(String aText);
	
	/**
	 * Returns the number of events that occurred within each given behavior.
	 */
	public long[] getEventCountAtBehaviors(int[] aBehaviorIds);

	/**
	 * Returns the number of events that occurred within each given class.
	 */
	public long[] getEventCountAtClasses(int[] aClassIds);
	
	/**
	 * Represents a search hit.
	 * @author gpothier
	 */
	public static class StringSearchHit implements Serializable
	{
		private static final long serialVersionUID = 6477792385168896074L;
		private long itsObjectId;
		private long itsScore;
		
		public StringSearchHit(long aObjectId, long aScore)
		{
			itsObjectId = aObjectId;
			itsScore = aScore;
		}

		public long getObjectId()
		{
			return itsObjectId;
		}

		public long getScore()
		{
			return itsScore;
		}
		
		@Override
		public String toString()
		{
			return "Hit: "+itsObjectId+" ("+itsScore+")";
		}
	}

	
}
