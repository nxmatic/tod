/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
#ifndef __jniutils_h
#define __jniutils_h

#include <jni.h>

class StaticMethod
{
	protected: jclass itsClass;
	protected: jmethodID itsMethod;

	public: StaticMethod(
		JNIEnv* jni, 
		const char* aClassName, 
		const char* aMethodName, 
		const char* aMethodSignature);
};

class StaticVoidMethod : StaticMethod
{
	public: StaticVoidMethod(
		JNIEnv* jni, 
		const char* aClassName, 
		const char* aMethodName, 
		const char* aMethodSignature) : StaticMethod(jni, aClassName, aMethodName, aMethodSignature) {};
	
	public: void invoke(JNIEnv* jni, ...);
};

class StaticLongMethod : StaticMethod
{
	public: StaticLongMethod(
		JNIEnv* jni, 
		const char* aClassName, 
		const char* aMethodName, 
		const char* aMethodSignature) : StaticMethod(jni, aClassName, aMethodName, aMethodSignature) {};
	
	public: jlong invoke(JNIEnv* jni, ...);
};

class StaticObjectMethod : StaticMethod
{
	public: StaticObjectMethod(
		JNIEnv* jni, 
		const char* aClassName, 
		const char* aMethodName, 
		const char* aMethodSignature) : StaticMethod(jni, aClassName, aMethodName, aMethodSignature) {};
	
	public: jobject invoke(JNIEnv* jni, ...);
};

#endif
