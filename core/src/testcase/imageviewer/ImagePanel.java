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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ImagePanel extends JPanel
{
	private ImageData itsThumbnail;

	public ImagePanel(ImageData aThumbnail)
	{
		itsThumbnail = aThumbnail;
		
		setLayout(new BorderLayout());
		add(new JLabel(aThumbnail != null ? aThumbnail.getName() : ""), BorderLayout.NORTH);
		add(new ThumbnailPanel(itsThumbnail), BorderLayout.CENTER);
	}
	
	private static class ThumbnailPanel extends JPanel
	{
		private ImageData itsThumbnail;
		
		public ThumbnailPanel(ImageData aThumbnail)
		{
			itsThumbnail = aThumbnail;
			setPreferredSize(new Dimension(70, 70));
		}
		
		@Override
		protected void paintComponent(Graphics aG)
		{
			super.paintComponent(aG);
			itsThumbnail.paintThumbnail(aG);
		}
	}
}
