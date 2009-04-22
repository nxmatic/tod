/**
 * 
 */
package imageviewer2008;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TextData extends FileData
{
	String text;

	public TextData(String aFile)
	{
		super(aFile);
		try {
			FileReader reader = new FileReader(file);
			text = new BufferedReader(reader).readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Dimension getSize()
	{
		return new Dimension(50, 50);
	}

	public void paintThumbnail(Graphics g) {
		g.setColor(Color.BLACK);
		g.drawString(text, 0, 20);
	}

	public void paint(Graphics g, Dimension dim) {
		g.setColor(Color.BLACK);
		g.drawString(text, 20, 20);
	}
	
	
}