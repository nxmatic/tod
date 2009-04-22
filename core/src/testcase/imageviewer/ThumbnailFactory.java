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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

import javax.imageio.ImageIO;

public class ThumbnailFactory
{

	/**
	 * Creates a thumbnail of the specified file.
	 */
	public static ImageData create(File file)
	{
		String name = file.getName();

		if (name.endsWith(".jpg") || name.endsWith(".png"))
		{
			try
			{
				return new ImageThumbnail(file, ImageIO.read(file));
			}
			catch (IOException e)
			{
				return null;
			}
		}
		else if (name.endsWith(".txt"))
		{
			try
			{
				FileReader reader = new FileReader(file);
				String line = new BufferedReader(reader).readLine();
				return new TextThumbnail(file, line);
			}
			catch (IOException e)
			{
				return null;
			}
		}
		else return null;
	}

	private static class ImageThumbnail extends ImageData
	{
		private BufferedImage image;

		public ImageThumbnail(File aFile, BufferedImage image)
		{
			super(aFile);
			this.image = image;
		}

		public Dimension getSize()
		{
			int width = image.getWidth();
			int height = image.getHeight();
			return new Dimension(width, height);
		}

		public void paintThumbnail(Graphics g)
		{
			g.drawImage(image, 0, 0, null);
		}
	}

	private static class TextThumbnail extends ImageData
	{
		String text;

		public TextThumbnail(File aFile, String text)
		{
			super(aFile);
			this.text = text;
		}

		public Dimension getSize()
		{
			return new Dimension(50, 50);
		}

		@Override
		public void paintThumbnail(Graphics g)
		{
			g.setColor(Color.BLACK);
			g.drawString(text, 0, 20);
		}
	}
}
