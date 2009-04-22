package imageviewer2006;

import java.awt.Graphics;
import javax.swing.JPanel;

public class PreviewPanel extends JPanel {
	private ImageData image;

	public ImageData getImage() {
		return image;
	}

	public void setImage(ImageData image) {
		this.image = image;
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) image.paint(g, getSize());
	}
}
