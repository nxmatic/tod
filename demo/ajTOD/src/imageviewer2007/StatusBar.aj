package imageviewer2007;

import java.awt.BorderLayout;

import javax.swing.JLabel;

public aspect StatusBar {
	JLabel ImageViewer.statusBar = new JLabel("Status bar");
	
	pointcut create(ImageViewer v): execution(ImageViewer.new(..)) && this(v);
	
	after(ImageViewer v): create(v) {
		v.add(v.statusBar, BorderLayout.SOUTH);
	}
	
	pointcut select(ImageViewer v, ImageData i): 
		execution(* ImageViewer.select(..))
		&& this(v) && args(i);
	
	before(ImageViewer v, ImageData i): select(v, i) {
		v.statusBar.setText(i.getName());
	}
}
