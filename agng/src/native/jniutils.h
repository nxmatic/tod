#ifndef __jniutils_h
#define __jniutils_h

#include <jni.h>

template <class R>
class StaticMethod
{
	protected: jclass itsClass;
	protected: jmethodID itsMethod;
	
	public: StaticMethod(
		JNIEnv* jni, 
		char* aClassName, 
		char* aMethodName, 
		char* aMethodSignature);
	
	public: virtual R invoke(JNIEnv* jni, ...) =0;
	public: virtual void checkException(JNIEnv* jni);
};

class StaticVoidMethod : public StaticMethod<void>
{
	public: StaticVoidMethod(
		JNIEnv* jni, 
		char* aClassName, 
		char* aMethodName, 
		char* aMethodSignature);
	
	public: virtual void invoke(JNIEnv* jni, ...);
};

class StaticLongMethod : public StaticMethod<jlong>
{
	public: StaticLongMethod(
		JNIEnv* jni, 
		char* aClassName, 
		char* aMethodName, 
		char* aMethodSignature);
	
	public: virtual jlong invoke(JNIEnv* jni, ...);
};

class StaticIntMethod : public StaticMethod<jint>
{
	public: StaticIntMethod(
		JNIEnv* jni, 
		char* aClassName, 
		char* aMethodName, 
		char* aMethodSignature);
	
	public: virtual jint invoke(JNIEnv* jni, ...);
};



class StaticByteArrayMethod : public StaticMethod<jbyteArray>
{
	public: StaticByteArrayMethod(
		JNIEnv* jni, 
		char* aClassName, 
		char* aMethodName, 
		char* aMethodSignature);
	
	public: virtual jbyteArray invoke(JNIEnv* jni, ...);
};

#endif
