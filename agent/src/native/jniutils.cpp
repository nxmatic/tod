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
#include <string.h>
#include <stdarg.h>

#include "jniutils.h"

extern int propVerbose;

StaticMethod::StaticMethod(
	JNIEnv* jni,
	char* aClassName, 
	char* aMethodName, 
	char* aMethodSignature)
{
	if (propVerbose>=2) printf("Loading (jni) %s\n", aClassName);
	jclass theClass = jni->FindClass(aClassName);
	if (theClass == NULL) printf("Could not load %s!\n", aClassName);
	itsClass = (jclass) jni->NewGlobalRef(theClass);
	
	itsMethod = jni->GetStaticMethodID(itsClass, aMethodName, aMethodSignature);
	if (itsMethod == NULL) printf("Could not find method %s %s!\n", aMethodName, aMethodSignature);	
}

StaticVoidMethod::StaticVoidMethod(
	JNIEnv* jni,
	char* aClassName, 
	char* aMethodName, 
	char* aMethodSignature) : StaticMethod(jni, aClassName, aMethodName, aMethodSignature)
{
}

void StaticVoidMethod::invoke(JNIEnv* jni, ...)
{
	va_list args;
	va_start(args, jni);
	jni->CallStaticVoidMethodV(itsClass, itsMethod, args);
}

StaticLongMethod::StaticLongMethod(
	JNIEnv* jni,
	char* aClassName, 
	char* aMethodName, 
	char* aMethodSignature) : StaticMethod(jni, aClassName, aMethodName, aMethodSignature)
{
}

jlong StaticLongMethod::invoke(JNIEnv* jni, ...)
{
	va_list args;
	va_start(args, jni);
	return jni->CallStaticLongMethodV(itsClass, itsMethod, args);
}
