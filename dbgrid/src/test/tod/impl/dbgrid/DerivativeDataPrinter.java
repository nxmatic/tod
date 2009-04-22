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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DerivativeDataPrinter
{
	private PrintWriter itsWriter;
	
	private double itsFirstX;
	private double itsLastX;
	private double itsLastY;
	
	private int i;
	
	public DerivativeDataPrinter(
			PrintWriter aWriter,
			String aXLabel,
			String aYLabel)
	{
		itsWriter = aWriter;
		
		itsWriter.println("# Col. 1: "+aXLabel);
		itsWriter.println("# Col. 2: "+aYLabel);
		itsWriter.println("# Col. 3: dy/dx");
		itsWriter.println("# Col. 4: avg");
	}
	
	public DerivativeDataPrinter(
			File aFile,
			String aXLabel,
			String aYLabel) throws IOException
	{
		this(new PrintWriter(new FileWriter(aFile)), aXLabel, aYLabel);
	}
	
	public void addPoint(double aX, double aY)
	{
		if (itsFirstX == 0) itsFirstX = aX;
		
		double theDX = aX-itsLastX;
		double theDY = aY-itsLastY;
		
		double theD = theDY/theDX;
		double theAvg = aY / (aX-itsFirstX);
		
		itsWriter.println(aX+" "+aY+" "+theD+" "+theAvg);

		if (i % 10 == 0) itsWriter.flush();
		i++;
		
		itsLastX = aX;
		itsLastY = aY;
	}
	
	public void close()
	{
		itsWriter.close();
	}
}
