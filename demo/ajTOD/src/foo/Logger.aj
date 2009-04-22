package foo;

public aspect Logger {
	
	before(Rectangle r): call(* Canvas.draw(Shape))
		&& args(r) {
		System.out.println("MyAspect.before()");
	};
}