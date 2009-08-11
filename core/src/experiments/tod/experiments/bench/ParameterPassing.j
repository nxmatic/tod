.class public tod/experiments/bench/ParameterPassing
.super java/lang/Object

.method public <init>()V
	.limit stack 1
	.limit locals 1
	aload 0
	invokespecial java/lang/Object/<init>()V
	return
.end method

.method public invoke0(Ltod/experiments/bench/FalseFrame;)V
	.limit stack 7
	.limit locals 8

	aload 1
	aconst_null
	iconst_1
	iconst_2
	lconst_0
	ldc "hey"
	invokevirtual tod/experiments/bench/FalseFrame/invoke(Ljava/lang/Object;IIJLjava/lang/Object;)V
	
	return
.end method

.method public invoke1(Ltod/experiments/bench/FalseFrame;)V
	.limit stack 7	
	.limit locals 2
	aconst_null
	iconst_1
	iconst_2
	lconst_0
	ldc "hey"
	; Here was the original invoke
	
	aload 1
	invokestatic tod/experiments/bench/FalseFrame/setTarget(Ltod/experiments/bench/FalseFrame;)V
	
	invokestatic tod/experiments/bench/FalseFrame/s_invoke(Ljava/lang/Object;IIJLjava/lang/Object;)V
	
	return
	
.end method

.method public invoke2(Ltod/experiments/bench/FalseFrame;)V
	.limit stack 7
	.limit locals 8

	aconst_null
	iconst_1
	iconst_2
	lconst_0
	ldc "hey"
	; Here was the original invoke
	
	astore 2
	lstore 3
	istore 5
	istore 6
	astore 7
	
	aload 1
	aload 7
	iload 6
	iload 5
	lload 3
	aload 2
	invokevirtual tod/experiments/bench/FalseFrame/invoke(Ljava/lang/Object;IIJLjava/lang/Object;)V
	
	return
.end method
