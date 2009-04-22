package imageviewer2008;

import java.awt.image.BufferedImage;

public aspect LazyLoad {
	
	pointcut init(): call(* ImageData.load())
		&& withincode(ImageData.new(..));
	
	
	BufferedImage around(): init() {
		return null;
	}
	
	pointcut paint(ImageData i): execution(* ImageData.paint*(..))
		&& this(i);
	
	before(ImageData i): paint(i) {
		if (i.image == null) 
		{
			i.image = i.load();
		}
	}

}
