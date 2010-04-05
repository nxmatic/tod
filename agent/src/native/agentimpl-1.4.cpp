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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <jni.h>
#include <jvmdi.h>
#include <jvmpi.h>

#include "agentimpl.h"
#include "agent.h"

#include "utils.h"
#include "io.h"

#ifdef __cplusplus
extern "C" {
#endif

// Global JVMPI and JVMDI environments, to be able to use them in pure JNI calls
JVMPI_Interface *gJvmpi;
JVMDI_Interface_1 *gJvmdi;

#define CONSTANT_Class 		7
#define CONSTANT_Fieldref 	9
#define CONSTANT_Methodref 	10
#define CONSTANT_InterfaceMethodref 	11
#define CONSTANT_String 	8
#define CONSTANT_Integer 	3
#define CONSTANT_Float 		4
#define CONSTANT_Long 		5
#define CONSTANT_Double 	6
#define CONSTANT_NameAndType 	12
#define CONSTANT_Utf8 		1

#define ERRCASE(N) case N: errmsg = "N"; break;

void check_jvmpi_error(jint errnum, const char *str)
{
	if (errnum != JVMPI_SUCCESS) 
	{
		char *errmsg = NULL;
		switch(errnum)
		{
			ERRCASE(JVMPI_FAIL)
			ERRCASE(JVMPI_NOT_AVAILABLE)
		}
		
		printf("ERROR: JVMPI: %d(%s): %s\n", errnum, 
			(errmsg == NULL ? "Unknown" : errmsg),
			(str == NULL ? "" : str));

		fflush(stdout);
	}
}

void check_jvmdi_error(jint errnum, const char *str)
{
	if (errnum != JVMDI_ERROR_NONE) 
	{
		char *errmsg = NULL;
		switch(errnum)
		{
			ERRCASE(JVMDI_ERROR_OUT_OF_MEMORY)
			ERRCASE(JVMDI_ERROR_ACCESS_DENIED)
			ERRCASE(JVMDI_ERROR_UNATTACHED_THREAD)
			ERRCASE(JVMDI_ERROR_VM_DEAD)
			ERRCASE(JVMDI_ERROR_INTERNAL )
		}
		
		printf("ERROR: JVMPI: %d(%s): %s\n", errnum, 
			(errmsg == NULL ? "Unknown" : errmsg),
			(str == NULL ? "" : str));

		fflush(stdout);
	}
}



uint8_t u1(unsigned char* data)
{
	return data[0];
}

uint16_t u2(unsigned char* data)
{
	return (data[0] << 8) + data[1];
}

/**
Retrieves the address of the Nth item in the constant pool.
*/
unsigned char* getConstant(unsigned char* pool, int index)
{
	int len = 0;
	for(int i=0;i<index;i++)
	{
		uint8_t tag = u1(pool);
// 		printf("tag: %d\n", tag);
		pool++;
		
		switch(tag)
		{
			case CONSTANT_Class:
			case CONSTANT_String:
				pool += 2;
				break;
				
			case CONSTANT_Fieldref:
			case CONSTANT_Methodref:
			case CONSTANT_InterfaceMethodref:
			case CONSTANT_Integer:
			case CONSTANT_Float:
			case CONSTANT_NameAndType:
				pool += 4;
				break;
				
			case CONSTANT_Long:
			case CONSTANT_Double:
				pool += 8;
				i++;
				break;
				
			case CONSTANT_Utf8:
				len = u2(pool);
				pool += 2+len;
				break;
				
			default:
				printf("ERROR: unknown entry type: %d\n", tag);
				fflush(stdout);
				break;
		}
	}
	
	return pool;
}

bool getClassName(unsigned char* data, char* buffer, int bsize)
{
// 	for(int i=0;i<event->u.class_load_hook.class_data_len;i++)
// 	{
// 		printf("%02x ", data[i]);
// 		if (i % 16 == 15) printf("\n");
// 	}
// 	printf("\n");

	// Find class name
	int pool_size = u2(data+8);
// 	printf("pool_size: %d\n", pool_size);
	unsigned char* pool = data+10;
	unsigned char* after_pool = getConstant(pool, pool_size-1);
	int cls_index = u2(after_pool+2);
	
	unsigned char* cls_const = getConstant(pool, cls_index-1);
	int clsname_index = u2(cls_const+1);
	
	unsigned char* clsname_const = getConstant(pool, clsname_index-1);
	int clsname_size = u2(clsname_const+1);
	
	if (clsname_size+1 > bsize) return false;
	memcpy(buffer, clsname_const+3, clsname_size);
	buffer[clsname_size] = 0;

	return true;
}


void cbClassLoadHook(JVMPI_Event* event) 
{
	unsigned char* data = event->u.class_load_hook.class_data;
	jint len = event->u.class_load_hook.class_data_len;
	
	event->u.class_load_hook.new_class_data = data;
	event->u.class_load_hook.new_class_data_len = len;
	
	char clsName[2048];
	if (! getClassName(data, clsName, sizeof(clsName)))
	{
		fprintf(stderr, "ERROR: class name too big\n");
		return;
	}
	
// 	printf("Hook: %s\n", clsName);
// 	fflush(stdout);
	
	agentClassFileLoadHook(
		event->env_id,
		clsName, 
		event->u.class_load_hook.class_data_len, 
		event->u.class_load_hook.class_data, 
		&event->u.class_load_hook.new_class_data_len, 
		&event->u.class_load_hook.new_class_data,
		event->u.class_load_hook.malloc_f);
}

jclass loadClass(JNIEnv* jni, char* aName)
{
	jclass cls = jni->FindClass(aName);
	if (cls == NULL) printf("Could not load %s!\n", aName);
	return cls;
}

void registerNative(JNIEnv* jni, jclass aClass, char* aName, char* aSig, void* aPtr)
{
	JNINativeMethod m;
	m.name = aName;
	m.signature = aSig;
	m.fnPtr = aPtr;
	int res = jni->RegisterNatives(aClass, &m, 1);
	
	if (res != 0)
	{
		fprintf(stderr, "Failed to register %s %s\n", aName, aSig);
		fflush(stderr);
	}
}


void cbJvmInitDone(JVMPI_Event *event)
{
	JNIEnv* jni = event->env_id;
	
	// Register native methods
	printf("Registering native methods...\n");
	fflush(stdout);
	
	jclass cls_IO = loadClass(jni, "java/tod/io/_IO");
	registerNative(jni, cls_IO, "out", "(Ljava/lang/String;)V", (void*) Java_java_tod_io__1IO_out);
	registerNative(jni, cls_IO, "err", "(Ljava/lang/String;)V", (void*) Java_java_tod_io__1IO_err);

	jclass cls_AgConfig = loadClass(jni, "java/tod/_AgConfig");
	registerNative(jni, cls_AgConfig, "getHostId", "()I", (void*) Java_java_tod__1AgConfig_getHostId);
	registerNative(jni, cls_AgConfig, "getCollectorHost", "()Ljava/lang/String;", (void*) Java_java_tod__1AgConfig_getCollectorHost);
	registerNative(jni, cls_AgConfig, "getCollectorPort", "()Ljava/lang/String;", (void*) Java_java_tod__1AgConfig_getCollectorPort);
	registerNative(jni, cls_AgConfig, "getClientName", "()Ljava/lang/String;", (void*) Java_java_tod__1AgConfig_getClientName);

	jclass cls_SocketChannel = loadClass(jni, "java/tod/io/_SocketChannel");
	registerNative(jni, cls_SocketChannel, "open0", "(Ljava/lang/String;I)I", (void*) Java_java_tod_io__1SocketChannel_open0);
	registerNative(jni, cls_SocketChannel, "flush0", "(I)I", (void*) Java_java_tod_io__1SocketChannel_flush0);
	registerNative(jni, cls_SocketChannel, "close0", "(I)I", (void*) Java_java_tod_io__1SocketChannel_close0);
	registerNative(jni, cls_SocketChannel, "write0", "(I[BII)I", (void*) Java_java_tod_io__1SocketChannel_write0);
	registerNative(jni, cls_SocketChannel, "read0", "(I[BII)I", (void*) Java_java_tod_io__1SocketChannel_read0);
	registerNative(jni, cls_SocketChannel, "in_avail0", "(I)I", (void*) Java_java_tod_io__1SocketChannel_in_1avail0);

	printf("Registered native methods.\n");
	fflush(stdout);

	agentStart(jni);
}

void cbJVMPIEvent(JVMPI_Event *event)
{
	switch(event->event_type)
	{
		case JVMPI_EVENT_CLASS_LOAD_HOOK:
			cbClassLoadHook(event);
			break;
		case JVMPI_EVENT_JVM_INIT_DONE:
			cbJvmInitDone(event);
			break;
// 		case JVMPI_EVENT_JVM_SHUT_DOWN:
// 			cbJvmShutDown(event);
// 			break;
		default:
			fprintf(stderr, "ERROR: unknown JVMPI event type: %d\n", event->event_type);
			break;
	}
}

void cbException(JNIEnv* jni, JVMDI_Event* event)
{
	JVMDI_exception_event_data& ev = event->u.exception;
	
	if (! agentShouldProcessException(jni, ev.method)) return;

	char* methodName;
	char* methodSignature;
	jclass methodDeclaringClass;
	char* methodDeclaringClassSignature;
 
	int bytecodeIndex = (int) ev.location;
	
	// Obtain method information
	gJvmdi->GetMethodName(ev.clazz, ev.method, &methodName, &methodSignature);
	gJvmdi->GetMethodDeclaringClass(ev.clazz, ev.method, &methodDeclaringClass);
	gJvmdi->GetClassSignature(methodDeclaringClass, &methodDeclaringClassSignature);
	
	agentException(
		jni, 
		methodName, 
		methodSignature, 
		methodDeclaringClass, 
		methodDeclaringClassSignature, 
		ev.exception, 
		bytecodeIndex);
	
	// Free buffers
	gJvmdi->Deallocate((jbyte*) methodName);
	gJvmdi->Deallocate((jbyte*) methodSignature);
	gJvmdi->Deallocate((jbyte*) methodDeclaringClassSignature);

}

void cbVMDeath(JNIEnv* jni, JVMDI_Event* event)
{
//  	agentStop();
}

void cbJVMDIEvent(JNIEnv* jni, JVMDI_Event* event)
{
	switch(event->kind)
	{
		case JVMDI_EVENT_EXCEPTION:
			cbException(jni, event);
			break;
		case JVMDI_EVENT_VM_DEATH:
			cbVMDeath(jni, event);
			break;
	}
}

JNIEXPORT jint JNICALL JVM_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
	// Get environments 
	int res = jvm->GetEnv((void **) &gJvmpi, JVMPI_VERSION_1);
	if (res < 0) {
		fprintf(stderr, "ERROR: Unable to get jvmpi, GetEnv failed, error=%d\n", res);
		return JNI_ERR;
	}

	res = jvm->GetEnv((void **) &gJvmdi, JVMDI_VERSION_1);
	if (res < 0) {
		fprintf(stderr, "ERROR: Unable to get jvmdi, GetEnv failed, error=%d\n", res);
		return JNI_ERR;
	}

	// Enable JVMPI events
	gJvmpi->NotifyEvent = cbJVMPIEvent;
	res = gJvmpi->EnableEvent(JVMPI_EVENT_CLASS_LOAD_HOOK, NULL);
	check_jvmpi_error(res, "Enable JVMPI_EVENT_CLASS_LOAD_HOOK") ;
	
	res = gJvmpi->EnableEvent(JVMPI_EVENT_JVM_INIT_DONE, NULL);
	check_jvmpi_error(res, "Enable JVMPI_EVENT_JVM_INIT_DONE");
	
// 	res = gJvmpi->EnableEvent(JVMPI_EVENT_JVM_SHUT_DOWN, NULL);
// 	check_jvmpi_error(res, "Enable JVMPI_EVENT_JVM_SHUT_DOWN");
	
	// Enable JVMDI events
	res = gJvmdi->SetEventHook(cbJVMDIEvent);
	check_jvmdi_error(res, "SetEventHook");
	
// 	res = gJvmdi->SetEventNotificationMode(JVMDI_ENABLE, JVMDI_EVENT_EXCEPTION, NULL);
// 	check_jvmdi_error(res, "Enable JVMDI_EVENT_EXCEPTION");

	// Parse options
	int optLen = strlen(options);
	char* buffer = (char*) malloc(optLen+1);
	strncpy(buffer, options, optLen+1);
	
	char* propVerbose = NULL;
	char* propHost = NULL;
	char* propPort = NULL;
	char* propCachePath = NULL;
	char* propClientName = NULL;
	
	char** props[5] = {&propVerbose, &propHost, &propPort, &propCachePath, &propClientName};
	
	int i=0;
	int p;
	for(p=0;p<5 && i < optLen;p++)
	{
		*props[p] = &buffer[i];
		while (i < optLen)
		{
			if (buffer[i] == ',') break;
			i++;
		}
		
		buffer[i] = 0;
		i++;
	}
	
	if (p != 5)
	{
		fprintf(stderr, "ERROR: Could not parse options (%d)\n", p);
		return -1;
	}
	
// 	printf("[TOD] Agent options: %s, %s, %s, %s, %s\n", propVerbose, propHost, propPort, propCachePath, propClientName);
// 	fflush(stdout);
	
	cfgIsJVM14 = true;

	agentInit(propVerbose, propHost, propPort, propCachePath, propClientName);

	return JNI_OK;
}

//************************************************************************************


jlong agentimplGetObjectId(JNIEnv* jni, jobject obj)
{
	// This is implemented in Java, so this method is never called.
	return 0;
}

#ifdef __cplusplus
}
#endif
