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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import zz.utils.ui.ScrollablePanel;
import zz.utils.ui.StackLayout;

public class ImageViewer extends JPanel
{
	private List<ImageData> itsImages;
	
	public ImageViewer(File aFile)
	{
		itsImages = ThumbnailGenerator.generate(aFile);

		JPanel thePanel = new ScrollablePanel(new GridLayout(0, 4, 5, 5))
		{
			@Override
			public boolean getScrollableTracksViewportWidth()
			{
				return true;
			}
		};
		for (ImageData theImageData : itsImages)
		{
			thePanel.add(new ImagePanel(theImageData));
		}

		JScrollPane theScrollPane = new JScrollPane(thePanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		theScrollPane.setPreferredSize(new Dimension(400, 300));
		
		setLayout(new StackLayout());
		add(theScrollPane);
	}

	public static void main(String[] args)
	{
		JFrame theFrame = new JFrame("Image Viewer");
		theFrame.setContentPane(new ImageViewer(new File(args[0])));
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		theFrame.pack();
		theFrame.setVisible(true);
	}
	
}
