package imageviewer2007;

import java.awt.image.BufferedImage;

public aspect LazyLoad {
	
	pointcut load(): call(* ImageData.load())
		&& withincode(ImageData.new(..));
	
	BufferedImage around(): load() {
		System.out.println("Lazy: skip");
		return null;
	}
	
	pointcut paint(ImageData i): 
		execution(* ImageData.paintThumbnail(..)) && this(i);
	
	before(ImageData i): paint(i) {
		if (i.image == null) 
		{
			System.out.println("Lazy: load");
			i.image = i.load();
		}
	}

}
