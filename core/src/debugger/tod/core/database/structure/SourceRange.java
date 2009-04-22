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
package tod.core.database.structure;

import java.io.Serializable;

/**
 * Denotes a range of code in a source file.
 * Corresponds to Soot's Position class
 * @author gpothier
 */
public class SourceRange implements Serializable
{
	private static final long serialVersionUID = 3583352414851419066L;

	public final String typeName;
	public final String sourceFile;
	public final int startLine;
	public final int startColumn;
	public final int endLine;
	public final int endColumn;
	
	/**
	 * Creates a source range for a single line
	 */
	public SourceRange(String aTypeName, String aSourceFile, int aLine)
	{
		this(aTypeName, aSourceFile, aLine, 1, aLine, 1);
	}
	
	public SourceRange(String aTypeName, String aSourceFile, int aStartLine, int aStartColumn, int aEndLine, int aEndColumn)
	{
		typeName = aTypeName;
		sourceFile = aSourceFile;
		startLine = aStartLine;
		startColumn = aStartColumn;
		endLine = aEndLine;
		endColumn = aEndColumn;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s:%d (%d,%d-%d,%d)", sourceFile, startLine, startLine, startColumn, endLine, endColumn);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + endColumn;
		result = prime * result + endLine;
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		result = prime * result + startColumn;
		result = prime * result + startLine;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final SourceRange other = (SourceRange) obj;
		if (endColumn != other.endColumn) return false;
		if (endLine != other.endLine) return false;
		if (sourceFile == null)
		{
			if (other.sourceFile != null) return false;
		}
		else if (!sourceFile.equals(other.sourceFile)) return false;
		if (startColumn != other.startColumn) return false;
		if (startLine != other.startLine) return false;
		return true;
	}
}