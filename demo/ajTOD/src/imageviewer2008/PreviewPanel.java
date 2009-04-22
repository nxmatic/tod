package imageviewer2008;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class PreviewPanel extends JPanel {
	private FileData image;

	public FileData getImage() {
		return image;
	}

	public void setImage(FileData image) {
		this.image = image;
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) image.paint(g, getSize());
	}
}
