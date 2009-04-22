package imageviewer2008;



import java.awt.Dimension;
import java.awt.Graphics;
import java.io.File;

public abstract class FileData {
	String file;
	
	public FileData(String aFile)
	{
		file = aFile;
	}
	
	public String getName()
	{
		return file;
	}
	
	public abstract void paintThumbnail(Graphics g);
	public abstract void paint(Graphics g, Dimension dim);
	
	public abstract Dimension getSize();
}
