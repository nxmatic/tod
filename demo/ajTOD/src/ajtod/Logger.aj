package ajtod;

import org.aspectj.lang.Signature;

import java.io.*;

public aspect Logger {
//	pointcut logged(): call(* *.*(..)) && !within(Logger);
//	
//	before(): logged()
//	{
//		Signature sig = thisJoinPointStaticPart.getSignature();
//		System.out.println("Calling: "+sig);
//	}
//	
//	after(): logged()
//	{
//		Signature sig = thisJoinPointStaticPart.getSignature();
//		System.out.println("Called: "+sig);
//	}
	
	pointcut logCart(): execution(* Cart.*(..));
	
	before(): logCart()
	{
		System.out.println(thisJoinPoint.getThis());
	}

}
