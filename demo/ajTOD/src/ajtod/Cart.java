package ajtod;

import java.util.ArrayList;
import java.util.List;

public class Cart {
	private List products = new ArrayList();
	
	public void add(Product p)
	{
		products.add(p);
	}
	
	public String toString() {
		return super.toString();
	}
}
