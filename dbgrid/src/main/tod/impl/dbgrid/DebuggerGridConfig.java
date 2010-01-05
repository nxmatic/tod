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
package tod.impl.dbgrid;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tod.core.session.ISession;
import tod.utils.ConfigUtils;
import zz.utils.srpc.SRPCRemoteException;

public class DebuggerGridConfig
{
	private static final String HOME = System.getProperty("user.home");

	
	/**
	 * Port at which database nodes connect to the master.
	 */
	public static final int MASTER_NODE_PORT = 8060;
	
	/**
	 * Number of array slots in the master's event buffer.
	 */
	public static final int MASTER_EVENT_BUFFER_SIZE = 4096;
	
	/**
	 * Size of the {@link DatabaseNode} reordering event buffer
	 */
	public static int DB_REORDER_BUFFER_SIZE = 
		ConfigUtils.readInt("reorder-buffer-size", 100000);
	
	public static int DB_PERTHREAD_REORDER_BUFFER_SIZE = 
		ConfigUtils.readInt("perthread-reorder-buffer-size", DB_REORDER_BUFFER_SIZE);
	
	/**
	 * Size of the object reordering buffer for {@link ObjectsDatabase}.
	 */
	public static final int DB_OBJECTS_BUFFER_SIZE = 1000;
	
	
	/**
	 * Number of events to fetch at a time 
	 */
	public static final int QUERY_ITERATOR_BUFFER_SIZE = 1;
	
	/**
	 * Number of consecutive packets to send to children in the dispatch 
	 * round-robin scheme.
	 */
	public static final int DISPATCH_BATCH_SIZE =
		ConfigUtils.readInt("dispatch-batch-size", 128);
	
	public static final String MASTER_HOST =
		ConfigUtils.readString("master-host", "localhost");
	
	public static final String NODE_DATA_DIR =
		ConfigUtils.readString("node-data-dir", HOME+"/tmp/tod");
	
	/**
	 * Whether the grid master should prevent multiple database nodes
	 * on the same host.
	 */
	public static final boolean CHECK_SAME_HOST = 
		ConfigUtils.readBoolean("check-same-host", true);
	
	public static final boolean LOAD_BALANCING =
		ConfigUtils.readBoolean("load-balancing", false);
	
	/**
	 * Name of the database implementation. Can be evdb1 or evdbng.
	 */
	public static final String DBIMPL = ConfigUtils.readString("dbimpl", "evdbng");
	
	private static final String DATABASENODE_BASE = "db.DatabaseNode";
	private static final String GRIDLOGBROWSER_BASE = "GridLogBrowser";
	
	public static enum DbImpl
	{
		EVDB1("tod.impl.evdb1", "1"),
		EVDBNG("tod.impl.evdbng", "NG");
		
		public final String prefix;
		public final String suffix;

		private DbImpl(String aPrefix, String aSuffix)
		{
			prefix = aPrefix;
			suffix = aSuffix;
		}
		
		public Class getClass(String aBase)
		{
			try
			{
				return Class.forName(prefix+"."+aBase+suffix);
			}
			catch (ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	public static DbImpl getDbImpl()
	{
		if ("evdb1".equals(DBIMPL)) return DbImpl.EVDB1;
		else if ("evdbng".equals(DBIMPL)) return DbImpl.EVDBNG;
		else throw new RuntimeException("Not handled: "+DBIMPL);
	}
	
//	/**
//	 * Creates the proper instance of {@link DatabaseNode} according to
//	 * the selected database implementation ({@link #DBIMPL}).
//	 */
//	public static DatabaseNode createDatabaseNode()
//	{
//		try
//		{
//			return (DatabaseNode) getDbImpl().getClass(DATABASENODE_BASE).newInstance();
//		}
//		catch (Exception e)
//		{
//			throw new RuntimeException(e);
//		}
//	}
//	
//	public static GridLogBrowser createLocalLogBrowser(ISession aSession, GridMaster aMaster)
//	{
//		try
//		{
//			Method theMethod = getDbImpl().getClass(GRIDLOGBROWSER_BASE).getDeclaredMethod(
//					"createLocal", 
//					ISession.class, 
//					GridMaster.class);
//			
//			return (GridLogBrowser) theMethod.invoke(null, aSession, aMaster);
//		}
//		catch (Exception e)
//		{
//			throw new RuntimeException(e);
//		}
//	}
	
	public static GridLogBrowser createRemoteLogBrowser(ISession aSession, RIGridMaster aMaster) 
	{
		try
		{
			Method theMethod = getDbImpl().getClass(GRIDLOGBROWSER_BASE).getDeclaredMethod(
					"createRemote", 
					ISession.class, 
					RIGridMaster.class);
				
			return (GridLogBrowser) theMethod.invoke(null, aSession, aMaster);
		}
		catch (InvocationTargetException e)
		{
			if (e.getCause() instanceof SRPCRemoteException)
			{
				throw (SRPCRemoteException) e.getCause();
			}
			else throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
}
