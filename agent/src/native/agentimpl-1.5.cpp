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

#include <jni.h>
#include <jvmti.h>

#include "agentimpl.h"
#include "agent.h"

#include "utils.h"

#ifdef __cplusplus
extern "C" {
#endif

// Pointer to our JVMTI environment, to be able to use it in pure JNI calls
jvmtiEnv *gJvmti;

// Object Id mutex and current id value
t_mutex oidMutex;
jlong oidCurrent = 1;

/* Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str)
{
	if ( errnum != JVMTI_ERROR_NONE ) 
	{
		char *errnum_str;
		
		errnum_str = NULL;
		(void)jvmti->GetErrorName(errnum, &errnum_str);
		
		printf("ERROR: JVMTI: %d(%s): %s\n", errnum, 
			(errnum_str==NULL?"Unknown":errnum_str),
			(str==NULL?"":str));

		fflush(stdout);
	}
}

void* agentimplAlloc(unsigned int size)
{
	unsigned char* mem_ptr;
	jvmtiError err = gJvmti->Allocate(size, &mem_ptr);
	check_jvmti_error(gJvmti, err, "Allocate");
	return mem_ptr;
}

void enable_event(jvmtiEnv *jvmti, jvmtiEvent event)
{
	jvmtiError err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, event, NULL);
	check_jvmti_error(jvmti, err, "SetEventNotificationMode");
}



void JNICALL cbClassFileLoadHook(
	jvmtiEnv *jvmti, JNIEnv* jni,
	jclass class_being_redefined, jobject loader,
	const char* name, jobject protection_domain,
	jint class_data_len, const unsigned char* class_data,
	jint* new_class_data_len, unsigned char** new_class_data) 
{
	agentClassFileLoadHook(
		jni, 
		name, 
		class_data_len, 
		class_data, 
		new_class_data_len, 
		new_class_data, 
		agentimplAlloc);
}


void JNICALL cbException(
	jvmtiEnv *jvmti,
	JNIEnv* jni,
	jthread thread,
	jmethodID method,
	jlocation location,
	jobject exception,
	jmethodID catch_method,
	jlocation catch_location)
{
	if (! agentShouldProcessException(jni, method)) return;

	char* methodName;
	char* methodSignature;
	jclass methodDeclaringClass;
	char* methodDeclaringClassSignature;
 
	jvmtiJlocationFormat locationFormat;
	int bytecodeIndex = -1;
	
	// Obtain method information
	jvmti->GetMethodName(method, &methodName, &methodSignature, NULL);
	jvmti->GetMethodDeclaringClass(method, &methodDeclaringClass);
	jvmti->GetClassSignature(methodDeclaringClass, &methodDeclaringClassSignature, NULL);
	
	// Obtain location information
	jvmti->GetJLocationFormat(&locationFormat);
	if (locationFormat == JVMTI_JLOCATION_JVMBCI) bytecodeIndex = (int) location;

	agentException(
		jni, 
		methodName, 
		methodSignature, 
		methodDeclaringClass, 
		methodDeclaringClassSignature, 
		exception, 
		bytecodeIndex);
	
	// Free buffers
	jvmti->Deallocate((unsigned char*) methodName);
	jvmti->Deallocate((unsigned char*) methodSignature);
	jvmti->Deallocate((unsigned char*) methodDeclaringClassSignature);
}


void JNICALL cbVMStart(
	jvmtiEnv *jvmti,
	JNIEnv* jni)
{
	agentStart(jni);
}

/**
 * JVMTI initialization
 */
JNIEXPORT jint JNICALL 
Agent_OnLoad(JavaVM *vm, char *options, void *reserved) 
{
	jint rc;
	jvmtiError err;
	jvmtiEventCallbacks callbacks;
	jvmtiCapabilities capabilities;
	jvmtiEnv *jvmti;
	
	// Get JVMTI environment 
	rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION);
	if (rc != JNI_OK) {
		fprintf(stderr, "ERROR: Unable to create jvmtiEnv, GetEnv failed, error=%d\n", rc);
		return -1;
	}
	
	gJvmti = jvmti;
	
	// Retrieve system properties
	char* propVerbose = NULL;
	char* propHost = NULL;
	char* propPort = NULL;
	char* propCachePath = NULL;
	char* propClientName = NULL;

	err = jvmti->GetSystemProperty("agent-verbose", &propVerbose);
	if (err != JVMTI_ERROR_NOT_AVAILABLE) check_jvmti_error(jvmti, err, "GetSystemProperty (agent-verbose)");
	
	err = jvmti->GetSystemProperty("collector-host", &propHost);
	check_jvmti_error(jvmti, err, "GetSystemProperty (collector-host)");
	
	err = jvmti->GetSystemProperty("agent-cache-path", &propCachePath);
	if (err != JVMTI_ERROR_NOT_AVAILABLE) check_jvmti_error(jvmti, err, "GetSystemProperty (agent-cache-path)");
	
	err = jvmti->GetSystemProperty("client-name", &propClientName);
	if (err != JVMTI_ERROR_NOT_AVAILABLE) check_jvmti_error(jvmti, err, "GetSystemProperty (client-name)");
	
	err = jvmti->GetSystemProperty("collector-port", &propPort);
	check_jvmti_error(jvmti, err, "GetSystemProperty (collector-port)");
	
	// Set capabilities
	err = jvmti->GetCapabilities(&capabilities);
	check_jvmti_error(jvmti, err, "GetCapabilities");
	
	capabilities.can_generate_all_class_hook_events = 1;
	capabilities.can_generate_exception_events = 1;
	capabilities.can_tag_objects = 1;
	err = jvmti->AddCapabilities(&capabilities);
	check_jvmti_error(jvmti, err, "AddCapabilities");

	// Set callbacks and enable event notifications 
	memset(&callbacks, 0, sizeof(callbacks));
	callbacks.ClassFileLoadHook = &cbClassFileLoadHook;
	callbacks.Exception = &cbException;
	callbacks.VMStart = &cbVMStart;
	
	err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
	check_jvmti_error(jvmti, err, "SetEventCallbacks");
	
	// Enable events
 	enable_event(jvmti, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK);
	enable_event(jvmti, JVMTI_EVENT_EXCEPTION);
	enable_event(jvmti, JVMTI_EVENT_VM_START);
	
	cfgIsJVM14 = false;

	agentInit(propVerbose, propHost, propPort, propCachePath, propClientName);

	return JNI_OK;
}

JNIEXPORT void JNICALL 
Agent_OnUnload(JavaVM *vm)
{
	agentStop();
}

//************************************************************************************

/*
Returns the next free oid value.
Thread-safe.
*/
jlong getNextOid()
{
	jlong val;
	{
		t_lock lock(oidMutex);
		val = oidCurrent++;
	}
	
	// Include host id
	val = (val << cfgHostBits) | cfgHostId; 
	
	// We cannot use the 64th bit.
	if (val >> 63 != 0) fatal_error("OID overflow");
	return val;
}

jlong agentimplGetObjectId(JNIEnv* jni, jobject obj)
{
	jvmtiError err;
	jvmtiEnv *jvmti = gJvmti;
	jlong tag;
	
	err = jvmti->GetTag(obj, &tag);
	check_jvmti_error(jvmti, err, "GetTag");
	
	if (tag != 0) return tag;
	
	// Not tagged yet, assign an oid.
	tag = getNextOid();
	
	err = jvmti->SetTag(obj, tag);
	check_jvmti_error(jvmti, err, "SetTag");
	
	return -tag;
}



#ifdef __cplusplus
}
#endif
