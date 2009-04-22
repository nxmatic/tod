package imageviewer2008;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;


public class ThumbnailFactory
{

	/**
	 * Creates a thumbnail of the specified file.
	 */
	public static FileData create(String file)
	{
		file = new File(ImageViewer.root, file).getPath();

		
		if (file.endsWith(".jpg") || file.endsWith(".png"))
		{
			return new ImageData(file);
		}
		else if (file.endsWith(".txt"))
		{
			return new TextData(file);
		}
		else return null;
	}
}
