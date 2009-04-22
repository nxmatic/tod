package ajtod;

import java.io.*;

public aspect Test {
	pointcut t1(Grocery g): execution(* Cart.add(Product)) && args(g);
	
	before(Grocery g): t1(g)
	{
		System.out.println("added grocery: "+g);
	}
}
