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
package tod.experiments.bench;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public abstract class AbstractSQLCollector extends ISimpleLogCollector
{
	private Connection itsConnection;
	
	private PreparedStatement itsEventsInsertStmt;
	private PreparedStatement itsBehaviorEnterInsertStmt;
	private PreparedStatement itsBehaviorExitInsertStmt;
	private PreparedStatement itsFieldWriteInsertStmt;
	private PreparedStatement itsVarWriteInsertStmt;
	private PreparedStatement itsArgsInsertStmt;
	
	private long itsArgsId = 0;
	
	private boolean itsInsertSubclasses;
	private boolean itsInsertArgs;

	public AbstractSQLCollector(boolean aInsertSubclasses, boolean aInsertArgs)
	{
		itsInsertSubclasses = aInsertSubclasses;
		itsInsertArgs = aInsertArgs;

		try
		{
			itsConnection = connect();

			itsEventsInsertStmt = itsConnection.prepareStatement("INSERT INTO Events VALUES (?, ?, ?, ?, ?, ?)");
			itsFieldWriteInsertStmt = itsConnection.prepareStatement("INSERT INTO FieldWrites VALUES (?, ?, ?, ?, ?)");
			itsVarWriteInsertStmt = itsConnection.prepareStatement("INSERT INTO VarWrites VALUES (?, ?, ?, ?)");
			itsBehaviorEnterInsertStmt = itsConnection.prepareStatement("INSERT INTO BEnters VALUES (?, ?, ?, ?, ?)");
			itsBehaviorExitInsertStmt = itsConnection.prepareStatement("INSERT INTO BExits VALUES (?, ?, ?)");
			itsArgsInsertStmt = itsConnection.prepareStatement("INSERT INTO Args VALUES (?, ?, ?)");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected abstract Connection connect() throws Exception;

	public abstract long getStoredSize();
	

	
	private void insertEvent(long aTid, long aSeq, EventType aType, long aParentTid, long aParentSeq) throws SQLException
	{
		itsEventsInsertStmt.setLong(1, aTid);
		itsEventsInsertStmt.setLong(2, aSeq);
		itsEventsInsertStmt.setLong(3, time());
		itsEventsInsertStmt.setShort(4, (short) aType.ordinal());
		
		if (aParentTid >= 0)
		{
			itsEventsInsertStmt.setLong(5, aParentTid);
			itsEventsInsertStmt.setLong(6, aParentSeq);
		}
		else
		{
			itsEventsInsertStmt.setNull(5, Types.BIGINT);
			itsEventsInsertStmt.setNull(6, Types.BIGINT);
		}
		
		itsEventsInsertStmt.executeUpdate();
	}

	public synchronized void logBehaviorEnter(long aTid, long aSeq, int aBehaviorId, long aTarget, long[] args)
	{
		try
		{
			insertEvent(aTid, aSeq, EventType.BEHAVIOR_ENTER, -1, -1);
			
			long id = itsArgsId++;
			
			if (itsInsertArgs)
			{
				for (int i = 0; i < args.length; i++)
				{
					long arg = args[i];
	
					itsArgsInsertStmt.setLong(1, id);
					itsArgsInsertStmt.setInt(2, i);
					itsArgsInsertStmt.setLong(3, arg);
					
					itsArgsInsertStmt.executeUpdate();
				}
			}
			
			if (itsInsertSubclasses)
			{
				itsBehaviorEnterInsertStmt.setLong(1, aTid);
				itsBehaviorEnterInsertStmt.setLong(2, aSeq);
				itsBehaviorEnterInsertStmt.setInt(3, aBehaviorId);
				itsBehaviorEnterInsertStmt.setLong(4, aTarget);
				itsBehaviorEnterInsertStmt.setLong(5, id);
				
				itsBehaviorEnterInsertStmt.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	public synchronized void logBehaviorExit(long aTid, long aSeq, long aRetValue)
	{
		try
		{
			insertEvent(aTid, aSeq, EventType.BEHAVIOR_EXIT, -1, -1);

			if (itsInsertSubclasses)
			{
				itsBehaviorExitInsertStmt.setLong(1, aTid);
				itsBehaviorExitInsertStmt.setLong(2, aSeq);
				itsBehaviorExitInsertStmt.setLong(3, aRetValue);
				
				itsBehaviorExitInsertStmt.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	public synchronized void logFieldWrite(long aTid, long aSeq, int aFieldId, long aTarget, long aValue)
	{
		try
		{
			insertEvent(aTid, aSeq, EventType.FIELD_WRITE, -1, -1);
			
			if (itsInsertSubclasses)
			{
				itsFieldWriteInsertStmt.setLong(1, aTid);
				itsFieldWriteInsertStmt.setLong(2, aSeq);
				itsFieldWriteInsertStmt.setInt(3, aFieldId);
				itsFieldWriteInsertStmt.setLong(4, aTarget);
				itsFieldWriteInsertStmt.setLong(5, aValue);
				
				itsFieldWriteInsertStmt.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	public synchronized void logVarWrite(long aTid, long aSeq, int aVarId, long aValue)
	{
		try
		{
			insertEvent(aTid, aSeq, EventType.VAR_WRITE, -1, -1);
			
			if (itsInsertSubclasses)
			{
				itsVarWriteInsertStmt.setLong(1, aTid);
				itsVarWriteInsertStmt.setLong(2, aSeq);
				itsVarWriteInsertStmt.setInt(3, aVarId);
				itsVarWriteInsertStmt.setLong(4, aValue);
				
				itsVarWriteInsertStmt.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

}
