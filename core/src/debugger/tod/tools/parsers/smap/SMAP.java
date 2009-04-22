/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.tools.parsers.smap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.tools.parsers.smap.LineSection.LineInfo;
import zz.utils.Utils;

public class SMAP
{
	private final String itsOutputFileName;
	private final String itsDefaultStratumId;
	
	private final Map<String, StratumInfo> itsStrataMap = new HashMap<String, StratumInfo>();
	private StratumInfo itsCurrentStratum = null;
	
	public SMAP(String aOutputFileName, String aDefaultStratumId, List<AbstractSection> aSections)
	{
		itsOutputFileName = aOutputFileName;
		itsDefaultStratumId = aDefaultStratumId;
		
		for (AbstractSection theSection : aSections) theSection.addTo(this);
	}
	
	void addStratum(String aId)
	{
		if (itsStrataMap.containsKey(aId)) throw new RuntimeException("Stratum already defined: "+aId);
		itsCurrentStratum = new StratumInfo(aId);
		itsStrataMap.put(aId, itsCurrentStratum);
	}
	
	void addFile(FileInfo aInfo)
	{
		if (itsCurrentStratum == null) throw new RuntimeException("No current stratum");
		itsCurrentStratum.addFile(aInfo);
	}
	
	void startLineSection()
	{
		if (itsCurrentStratum == null) throw new RuntimeException("No current stratum");
		itsCurrentStratum.startLineSection();
	}
	
	void addLine(LineInfo aInfo)
	{
		if (itsCurrentStratum == null) throw new RuntimeException("No current stratum");
		itsCurrentStratum.addLine(aInfo);
	}
	
	/**
	 * Retrieves the source location for the given output line in the default stratum.
	 */
	public SourceLoc getSourceLoc(int aOutputLine)
	{
		return getSourceLoc(itsDefaultStratumId, aOutputLine);
	}

	/**
	 * Retrieves the source location for the given output line in the given stratum.
	 */
	public SourceLoc getSourceLoc(String aStratumId, int aOutputLine)
	{
		StratumInfo theStratumInfo = itsStrataMap.get(aStratumId);
		if (theStratumInfo == null) throw new RuntimeException("No such stratum: "+aStratumId);
		return theStratumInfo.getSourceLoc(aOutputLine);
	}
	
	/**
	 * A source location for a given output location for a given stratum.
	 * @author gpothier
	 */
	public static class SourceLoc
	{
		public final FileInfo fileInfo;
		public final int lineNumber;

		public SourceLoc(FileInfo aFileInfo, int aLineNumber)
		{
			fileInfo = aFileInfo;
			lineNumber = aLineNumber;
		}
	}
	
	private static class StratumInfo
	{
		/**
		 * Stratum id
		 */
		private final String itsId;
		private final Map<Integer, FileInfo> itsFilesMap = new HashMap<Integer, FileInfo>();
		
		/**
		 * Maps output lines to source locations. This list is indexed by output line 
		 * (-1, line 1 is at index 0).
		 */
		private List<SourceLoc> itsLocationsMap = new ArrayList<SourceLoc>();
		
		private int itsCurrentFileId;

		public StratumInfo(String aId)
		{
			itsId = aId;
		}

		void addFile(FileInfo aInfo)
		{
			itsFilesMap.put(aInfo.id, aInfo);
		}
		
		void startLineSection()
		{
			itsCurrentFileId = 0;
		}
		
		void addLine(LineInfo aInfo)
		{
			for(int i=0;i<aInfo.in.repeatCount;i++)
			{
				int outLine = aInfo.out.startLine.number + i*aInfo.out.increment;
				int inLine = aInfo.in.startLine.number + i;
				int theFileId = aInfo.in.startLine.fileId;
				if (theFileId != -1) itsCurrentFileId = theFileId;
				FileInfo theFile = itsFilesMap.get(itsCurrentFileId);
				for(int j=0;j<aInfo.out.increment;j++)
				{
					Utils.listSet(itsLocationsMap, outLine+j-1, new SourceLoc(theFile, inLine));
				}
			}
		}
		
		/**
		 * Retrieves the source location for the given output line in the default stratum.
		 */
		public SourceLoc getSourceLoc(int aOutputLine)
		{
			return itsLocationsMap.get(aOutputLine-1);
		}

	}
}
