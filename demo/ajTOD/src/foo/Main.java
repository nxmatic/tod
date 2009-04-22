package foo;

public class Main {
	
	public static void main(String[] args) {
		Canvas c = new Canvas();
		c.draw(getShape());
	}
	
	private static Shape getShape() {
		return new Rectangle();
	}
}
