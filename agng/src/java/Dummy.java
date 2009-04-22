import java.util.HashSet;
/*
 * Created on Dec 15, 2007
 */



public class Dummy
{
	public static void main(String[] args)
	{
		HashSet<String> theSet = new HashSet<String>();
		theSet.add("bip");
		theSet.add("bop");
		theSet.add("bap");
		new Fooish().foo();
		for(int i =0; i<50;i++){System.out.println("bououuo");}
		
		throw new RuntimeException("ha!");
	}
}
