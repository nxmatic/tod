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

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import zz.utils.Utils;

public class PostgresCollectorLight extends AbstractSQLCollector
{
	public PostgresCollectorLight()
	{
		super(false, false);
		
	}

	@Override
	protected Connection connect()
	{
		try
		{
			Process theProcess = Runtime.getRuntime().exec("/usr/bin/psql -p 5433 -d tod");
			Utils.pipe(new FileInputStream("doc/cc55a/db-postgres.sql"), theProcess.getOutputStream());
			theProcess.getOutputStream().close();
			Utils.pipe(theProcess.getInputStream(), System.err);
			Utils.pipe(theProcess.getErrorStream(), System.err);
			theProcess.waitFor();
			
			Class.forName("org.postgresql.Driver");
			return DriverManager.getConnection("jdbc:postgresql://localhost:5433/tod", "gpothier", "");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public long getStoredSize()
	{
		return 0;
	}
}
