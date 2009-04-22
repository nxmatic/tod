/**
 * 
 */
package imageviewer2006;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageData 
{
	String file;
	Image image;
	Image thumbnail;

	public ImageData(String aFile)
	{
		file = aFile;
		image = load();
	}

	public String getName()
	{
		return file;
	}
	
	Image load()
	{
		try {
			Image img = ImageIO.read(new File(file));
			thumbnail = img.getScaledInstance(70, 50, 0);
			return img;
		} catch (IOException e) {
			return null;
		}
	}
	
	public Dimension getSize() {
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		return new Dimension(width, height);
	}

	public void paintThumbnail(Graphics g) {
		g.drawImage(thumbnail, 5, 5, null);
	}

	public void paint(Graphics g, Dimension dim) {
		Dimension size = getSize();
		g.drawImage(
				image, 
				(dim.width/2)-(size.width/2),
				(dim.height/2)-(size.height/2),
				null);
	}
	
	
}