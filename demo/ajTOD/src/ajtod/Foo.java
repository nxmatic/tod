package ajtod;

public class Foo 
{
	public int faz()
	{
		return 0;
	}

	public void foo(Foo f, int a, int b, int c)
	{
		f.bar("1", b, faz());
	}
	
	public void bar(String a, int b, int c)
	{
		System.out.println("Foo.bar()");
	}
	
	public static void main(String[] args) 
	{
		System.out.println("Foo.main()");
		Foo foo = new Foo();
		foo.foo(foo, 0, 1, 2);
	}
}
