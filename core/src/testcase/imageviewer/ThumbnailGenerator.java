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
package imageviewer;




import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ThumbnailGenerator {
	
	public static List<ImageData> generate (File base)
	{
		List<ImageData> theThumbnails = new ArrayList<ImageData>();
		String[] fileNames = base.list();
		Arrays.sort(fileNames);
		
		for (String fileName : fileNames) 
		{
			File file = new File(base, fileName);
			if (file.isDirectory()) continue;
			else theThumbnails.add(ThumbnailFactory.create(file));
		}
		
		return theThumbnails;
	}
}
