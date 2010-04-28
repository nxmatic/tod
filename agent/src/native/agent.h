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
#ifndef _Included_agent_h
#define _Included_agent_h

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
This file declares the API of the abstract agent functions.
These functions are called by the actual agent implementations.
*/

extern int cfgHostBits;
extern int cfgHostId;
extern bool cfgIsJVM14;

void agentClassFileLoadHook(
	JNIEnv* jni, const char* name, 
	jint class_data_len, const unsigned char* class_data,
	jint* new_class_data_len, unsigned char** new_class_data,
	void* (*malloc_f)(unsigned int));
	
bool agentShouldProcessException(JNIEnv* jni, jmethodID method);

void agentException(
	JNIEnv* jni,
	char* methodName,
	char* methodSignature,
	jclass methodDeclaringClass,
	char* methodDeclaringClassSignature,
	jobject exception,
	int bytecodeIndex);
	
void agentStart(JNIEnv* jni);

void agentInit(
	char* aPropVerbose,
	char* aPropHost,
	char* aPropPort,
	char* aPropCachePath,
	char* aPropClientName);
	
void agentStop();

JNIEXPORT jint JNICALL Java_java_tod__1AgConfig_getHostId
	(JNIEnv * jni, jclass);

JNIEXPORT jstring JNICALL Java_java_tod__1AgConfig_getCollectorHost
  (JNIEnv* jni, jclass);

JNIEXPORT jstring JNICALL Java_java_tod__1AgConfig_getCollectorPort
  (JNIEnv* jni, jclass);

JNIEXPORT jstring JNICALL Java_java_tod__1AgConfig_getClientName
  (JNIEnv* jni, jclass);

jlong nextObjectId(JNIEnv* jni);

#ifdef __cplusplus
}
#endif

#endif
