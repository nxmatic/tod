.class public tod/experiments/Verifier
.super java/lang/Object

.method public <init>()V
	.limit stack 1
	.limit locals 1
	aload 0
	invokespecial java/lang/Object/<init>()V
	return
.end method

.method public foo(Ljava/lang/Object;)V
	.limit stack 7
	.limit locals 8

	aload 1
	checkcast java/lang/String
	invokevirtual java/lang/String/length()I
	
	return
.end method

