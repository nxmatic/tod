/**
 * 
 */
package imageviewer2008;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

class ThumbnailPanel extends JPanel
{
	ImageViewer imageViewer;
	FileData image;

	public ThumbnailPanel(ImageViewer viewer, String file) {
		this.imageViewer = viewer;
		image = ThumbnailFactory.create(file);
		setPreferredSize(new Dimension(80, 60));
		
		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e) {
				imageViewer.select(image);
			}
		});
	}
	
	public FileData getImage() {
		return image;
	}
	
	protected void paintComponent(Graphics g)
	{
		setBackground(imageViewer.getSelected() == image ? Color.BLUE : Color.LIGHT_GRAY);
			
		super.paintComponent(g);
		image.paintThumbnail(g);
	}
}