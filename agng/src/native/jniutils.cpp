#include <string.h>
#include <stdarg.h>
#include <stdlib.h>

#include "jniutils.h"

extern int propVerbose;

template <class R>
StaticMethod<R>::StaticMethod(
	JNIEnv* jni, 
	char* aClassName,
	char* aMethodName, 
	char* aMethodSignature)
{
	if (propVerbose>=2) printf("Loading (jni) %s\n", aClassName);
	jclass theClass = jni->FindClass(aClassName);
	if (theClass == NULL) 
	{
		fprintf(stderr, "Fatal: could not load %s!\n", aClassName);
		exit(1);
	}
	itsClass = (jclass) jni->NewGlobalRef(theClass);
	
	itsMethod = jni->GetStaticMethodID(itsClass, aMethodName, aMethodSignature);
	if (itsMethod == NULL) 
	{
		fprintf(stderr, "Fatal: could not find method %s %s!\n", aMethodName, aMethodSignature);	
		exit(1);
	}
}


template <class R>
void StaticMethod<R>::checkException(JNIEnv* jni)
{
	jthrowable exc; 	
	exc = jni->ExceptionOccurred();
	if (exc) 
	{
		// we print a debug message for the exception and clear it
		printf("Exception in invoke.\n");
		jni->ExceptionDescribe();
		jni->ExceptionClear();
		fflush(stdout);
	}
}

StaticVoidMethod::StaticVoidMethod(
	JNIEnv* jni,
	char* aClassName, 
	char* aMethodName, 
	char* aMethodSignature)
: StaticMethod<void>::StaticMethod(jni, aClassName, aMethodName, aMethodSignature)
{
}

void StaticVoidMethod::invoke(JNIEnv* jni, ...)
{
	va_list args;
	va_start(args, jni);
	jni->CallStaticVoidMethodV(itsClass, itsMethod, args);
	checkException(jni);
}

StaticByteArrayMethod::StaticByteArrayMethod(
	JNIEnv* jni,
	char* aClassName, 
	char* aMethodName, 
	char* aMethodSignature)
: StaticMethod<jbyteArray>::StaticMethod(jni, aClassName, aMethodName, aMethodSignature)
{
}

jbyteArray StaticByteArrayMethod::invoke(JNIEnv* jni, ...)
{
	va_list args;
	va_start(args, jni);
	jbyteArray theArray;	
	theArray=(jbyteArray)jni->CallStaticObjectMethodV(itsClass, itsMethod, args);
	checkException(jni);
	return theArray;
}

StaticLongMethod::StaticLongMethod(
	JNIEnv* jni,
	char* aClassName, 
	char* aMethodName, 
	char* aMethodSignature)
: StaticMethod<jlong>::StaticMethod(jni, aClassName, aMethodName, aMethodSignature)
{
}

jlong StaticLongMethod::invoke(JNIEnv* jni, ...)
{
	va_list args;
	va_start(args, jni);
	jlong theLong;	
	theLong=(jlong)jni->CallStaticLongMethodV(itsClass, itsMethod, args);
	checkException(jni);
	return theLong;
}

StaticIntMethod::StaticIntMethod(
	JNIEnv* jni,
	char* aClassName, 
	char* aMethodName, 
	char* aMethodSignature)
: StaticMethod<jint>::StaticMethod(jni, aClassName, aMethodName, aMethodSignature)
{
}

jint StaticIntMethod::invoke(JNIEnv* jni, ...)
{
	va_list args;
	va_start(args, jni);
	jint theInt;	
	theInt=(jint)jni->CallStaticIntMethodV(itsClass, itsMethod, args);
	checkException(jni);
	return theInt;
}

