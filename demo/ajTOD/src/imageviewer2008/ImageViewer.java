/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package imageviewer2008;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

public class ImageViewer extends JPanel
{
	public static File root;
	
	PreviewPanel previewPanel;
	List imagePanels = new ArrayList();
	int selectedIndex;
	
	public ImageViewer(File aFile)
	{
		JPanel thePanel = new ScrollablePanel(new GridLayout(0, 2, 5, 5))
		{
			public boolean getScrollableTracksViewportWidth()
			{
				return true;
			}
		};
	
		root = aFile;
		
		String[] fileNames = aFile.list();
		Arrays.sort(fileNames);
		
		for (int i=0;i<fileNames.length;i++) 
		{
			ThumbnailPanel panel = new ThumbnailPanel(this, fileNames[i]);
			imagePanels.add(panel);
			thePanel.add(panel);
		}

		JScrollPane theScrollPane = new JScrollPane(thePanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		theScrollPane.setPreferredSize(new Dimension(600, 300));
		
		previewPanel = new PreviewPanel();
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
		
		setLayout(new BorderLayout());
		add(splitPane);
		
		splitPane.setLeftComponent(theScrollPane);
		splitPane.setRightComponent(previewPanel);
		
		splitPane.setDividerLocation(200);
		
		// add support for space-key browsing
		addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					if(selectedIndex < imagePanels.size() - 1){
						ThumbnailPanel panel = (ThumbnailPanel) imagePanels.get(++selectedIndex);
						select(panel.getImage());	
					}
				}
			}
		});
	}
	
	public void addNotify() {
		super.addNotify();
		grabFocus();
	}
	
	private void findIndex(FileData image) {
		for(int i=0;i<imagePanels.size();i++) {
			ThumbnailPanel panel = (ThumbnailPanel) imagePanels.get(i);
			if (panel.getImage() == image) {
				selectedIndex = i;
				break;
			}
		}
	}
	
	public void select(FileData image)	{
		findIndex(image);
		previewPanel.setImage(image);
		repaint();
	}

	public FileData getSelected() {
		return previewPanel.getImage();
	}
	
	public static void main(String[] args)
	{
		System.out.println("CP: "+System.getProperty("java.class.path"));
		JFrame theFrame = new JFrame("Image Viewer");
		theFrame.setContentPane(new ImageViewer(new File(args[0])));
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		theFrame.pack();
		theFrame.setVisible(true);
	}
	
}
