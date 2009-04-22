/*
 TOD - Trace Oriented Debugger.
 Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 version 2 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 Parts of this work rely on the MD5 algorithm "derived from the 
 RSA Data Security, Inc. MD5 Message-Digest Algorithm".
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include <jni.h>
#include <jvmti.h>

#include "jniutils.h"

int cfgSkipCoreClasses = 1;

// System properties configuration data.
char* propHost = NULL;
char* propHostName = NULL;
char* propCachePath = NULL;
char* propPort = NULL;
int propVerbose = 2;

bool inExceptionHook = false;
int isInitializingExceptionMethods = 0;
bool vmInitialized = false;
bool exceptionClassLoaded = false;
bool loadingExceptionClass = false;
bool hostIdRegistered = false;
jint hostID;


StaticByteArrayMethod *TodAgent_agClassLoadHook;
StaticVoidMethod *TodAgent_agExceptionGenerated;
StaticLongMethod *TodAgent_agGetNextOid;
StaticIntMethod *TodAgent_agGetHostId;



extern "C" {

// Pointer to our JVMTI environment, to be able to use it in pure JNI calls
jvmtiEnv *globalJvmti;

/* Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str) {
	if (errnum != JVMTI_ERROR_NONE) {
		char *errnum_str;

		errnum_str = NULL;
		(void)jvmti->GetErrorName(errnum, &errnum_str);

		printf("ERROR: JVMTI: %d(%s): %s\n", errnum,
				(errnum_str==NULL ? "Unknown" : errnum_str), (str==NULL ? ""
						: str));

	}
}


void jvmtiAllocate(int aLen, unsigned char** mem_ptr) {
	jvmtiError err = globalJvmti->Allocate(aLen, mem_ptr);
	check_jvmti_error(globalJvmti, err, "Allocate");
}

bool startsWith(const char* aString, const char* aPrefix)
{
	int len = strlen(aPrefix);
	return strncmp(aString, aPrefix, len) == 0;int isInitializingExceptionMethods = 0;
}

void JNICALL cbClassFileLoadHook(
		jvmtiEnv *jvmti, JNIEnv* jni,
		jclass class_being_redefined, jobject loader,
		const char* name, jobject protection_domain,
		jint class_data_len, const unsigned char* class_data,
		jint* new_class_data_len, unsigned char** new_class_data)
{
	if (startsWith(name, "tod/agent/")
		|| startsWith(name, "tod/tools/")
		|| startsWith(name, "zz/utils/")
	    || startsWith(name, "tod/core/config/")) return;
		
	if (cfgSkipCoreClasses)
	{
		if (startsWith(name, "java/")
			|| startsWith(name, "sun/")
			|| startsWith(name, "com/sun/")) return;
	}

	if (propVerbose>=1) printf("Loading (hook) %s\n", name);
	
	//manage jni call to cpy byte array
	jbyteArray original = jni->NewByteArray(class_data_len);
	jni->SetByteArrayRegion( original, 0, class_data_len, (jbyte*) class_data);

	jbyteArray instrumented = TodAgent_agClassLoadHook->invoke(
			jni,
			name ? jni->NewStringUTF(name) : NULL,
			original
	);

	if (instrumented)
	{
		int len = jni->GetArrayLength( instrumented);
		if (propVerbose >= 3) printf("Redefining %s (%d bytes)...\n", name, len);
	
		jvmtiAllocate(len, new_class_data);
		*new_class_data_len = len;
		jni->GetByteArrayRegion( instrumented, 0, len, (jbyte*) *new_class_data);   //TODO  release jbyteArray
	}
	else
	{
		if (propVerbose >= 3) printf("Not redefining %s\n", name);
	}
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
// Uncomment in order to avoid concurrent call
//	if (inExceptionHook) 
//	{ 
//		printf("recursive or concurrent Exception call - skip hook\n");
//		return;
//	}
	inExceptionHook =true;
	if (propVerbose>=2) printf("enter exception hook\n");
	
	if (!vmInitialized) {
		if (propVerbose>=1)  printf("VM not yet initialized: exit exception hook\n");
		inExceptionHook =false;
		return;
	}
	
	if (loadingExceptionClass) 
	{
		inExceptionHook =false;
			return;
	}

	if (! exceptionClassLoaded)
	{
		// Initialize the classes and method ids that will be used
		// for exception processing
		// exception may be generated before vmstart
		loadingExceptionClass = true;
		TodAgent_agExceptionGenerated= new StaticVoidMethod(
						jni,
						"tod/agent/TodAgent",
						"agExceptionGenerated",
						"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Throwable;)V");
		
		
		loadingExceptionClass = false;
		exceptionClassLoaded = true;
	}

	jvmtiJlocationFormat locationFormat;
	jvmti->GetJLocationFormat(&locationFormat);
	int bytecodeIndex = -1;
	if (locationFormat == JVMTI_JLOCATION_JVMBCI) bytecodeIndex = (int) location;
	
	char* methodName;
	char* methodSignature;
	jclass methodDeclaringClass;
	char* methodDeclaringClassSignature;

	// Obtain method information
	globalJvmti->GetMethodName((jmethodID) method, &methodName, &methodSignature, NULL);
	globalJvmti->GetMethodDeclaringClass((jmethodID) method, &methodDeclaringClass);
	globalJvmti->GetClassSignature(methodDeclaringClass, &methodDeclaringClassSignature, NULL);

	if (propVerbose>=1) printf("Exception generated: %s, %s, %s, %d\n", methodName, methodSignature, methodDeclaringClassSignature, bytecodeIndex);
	
	TodAgent_agExceptionGenerated->invoke(
			jni,
			methodName ? jni->NewStringUTF(methodName) : NULL,
			methodSignature? jni->NewStringUTF(methodSignature) : NULL,
			methodDeclaringClassSignature? jni->NewStringUTF(methodDeclaringClassSignature): NULL,
			//bytecodeIndex, 
			(int)location, 
			exception);
		
		jvmti->Deallocate((unsigned char*) methodName);
			jvmti->Deallocate((unsigned char*) methodSignature);
			jvmti->Deallocate((unsigned char*) methodDeclaringClassSignature);
		
	if (propVerbose>=2) printf("exit exception hook\n");
	inExceptionHook = false;
}

void JNICALL cbVMStart(
		jvmtiEnv *jvmti,
		JNIEnv* jni)
{
}

void JNICALL
cbVMInit(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread)
{
	printf("cbVMInit\n");
	
	StaticVoidMethod agVMInit(
			jni,
			"tod/agent/TodAgent",
			"agVMInit",
			"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
	
	agVMInit.invoke(
			jni,
			propHost ? jni->NewStringUTF(propHost) : NULL,
			propHostName ? jni->NewStringUTF(propHostName): NULL,
			propPort ? jni->NewStringUTF(propPort): NULL,
			propCachePath ? jni->NewStringUTF(propCachePath): NULL,
			propVerbose);
	
	printf("After agVMInit\n");
	
	// Init JNI methods
	TodAgent_agClassLoadHook = new StaticByteArrayMethod(
			jni,
			"tod/agent/TodAgent",
			"agClassLoadHook",
			"(Ljava/lang/String;[B)[B");
		
	TodAgent_agGetNextOid = new StaticLongMethod(jni, "tod/agent/TodAgent","agGetNextOid","()J");
		
	TodAgent_agGetHostId = new StaticIntMethod(jni, "tod/agent/TodAgent","agGetHostId","()I");

	
	StaticVoidMethod TOD_enable(jni, "tod/agent/AgentReady", "nativeAgentLoaded", "()V");
	TOD_enable.invoke(jni);
	
	printf("cbVMInit - done.\n");
	
	vmInitialized = true;

}


void enable_event(jvmtiEnv *jvmti, jvmtiEvent event) {
	jvmtiError err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, event, NULL);
	check_jvmti_error(jvmti, err, "SetEventNotificationMode");
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

	printf("TOD agent - v3.1\n");
	fflush(stdout);

	// do not modify the following line used during obfuscation
	printf("Standard version\n");
	
	// Get JVMTI environment 
	rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION);
	if (rc != JNI_OK) {
		fprintf(stderr, "ERROR: Unable to create jvmtiEnv, GetEnv failed, error=%d\n", rc);
		return -1;
	}
	globalJvmti = jvmti;


	// Retrieve system properties
	char* _propVerbose = NULL;
	err = jvmti->GetSystemProperty("agent-verbose", &_propVerbose);
	if (err != JVMTI_ERROR_NOT_AVAILABLE)
	{
		check_jvmti_error(jvmti, err, "GetSystemProperty (agent-verbose)");
		propVerbose = atoi(_propVerbose);
		printf("Property: agent-verbose=%d\n", propVerbose);
	}
	else
	{
		propVerbose = 0;
		printf("agent-verbose property not specified, going silent.\n");
	}

	err = jvmti->GetSystemProperty("collector-host", &propHost);
	check_jvmti_error(jvmti, err, "GetSystemProperty (collector-host)");
	if (propVerbose>=1) printf("Property: collectent::agGetNextOid();or-host=%s\n", propHost);
	
	err = jvmti->GetSystemProperty("collector-port", &propPort);
	check_jvmti_error(jvmti, err, "GetSystemProperty (collector-port)");
	if (propVerbose>=1) printf("Property: colllector-port=%s\n", propPort);

	err = jvmti->GetSystemProperty("agent-cache-path", &propCachePath);
	if (err != JVMTI_ERROR_NOT_AVAILABLE)
	{
		check_jvmti_error(jvmti, err, "GetSystemProperty (agent-cache-path)");
		if (propVerbose>=1) printf("Property: agent-cache-path=%s\n", propCachePath);
	}

	err = jvmti->GetSystemProperty("client-hostname", &propHostName);
	if (err != JVMTI_ERROR_NOT_AVAILABLE)
	{
		check_jvmti_error(jvmti, err, "GetSystemProperty (client-hostname)");
		if (propVerbose>=1) printf("Property: client-hostname=%s\n", propHostName);
	}
	else
	{
		propHostName = "no-name";
	}

	

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
// 	callbacks.VMStart = &cbVMStart;
	callbacks.VMInit = &cbVMInit;

	err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
	check_jvmti_error(jvmti, err, "SetEventCallbacks");

	// Enable events
	enable_event(jvmti, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK);
	enable_event(jvmti, JVMTI_EVENT_EXCEPTION);
// 	enable_event(jvmti, JVMTI_EVENT_VM_START);
	enable_event(jvmti, JVMTI_EVENT_VM_INIT);

	fflush(stdout);

	return JNI_OK;
}

JNIEXPORT void JNICALL
Agent_OnUnload(JavaVM *vm)
{
	JNIEnv *jni;
	// Get JVMTI environment 
	jint rc = vm->GetEnv((void **)&jni, JNI_VERSION_1_4);
	if (rc != JNI_OK) {
		fprintf(stderr, "ERROR: Unable to create jniEnv, GetEnv failed, error=%d\n", rc);
	}
	else
	{
			StaticVoidMethod agOnUnload(jni, "tod/agent/TodAgent", "agOnUnload", "()V");
			agOnUnload.invoke(jni);
	}
}

//************************************************************************************

/*
 * Class: tod_core_ObjectIdentity
 * Method: get
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_tod_agent_ObjectIdentity_get
(JNIEnv * jni, jclass, jobject obj)
{
	jvmtiError err;
	jvmtiEnv *jvmti = globalJvmti;
	jlong tag;

	err = jvmti->GetTag(obj, &tag);
	check_jvmti_error(jvmti, err, "GetTag");

	if (tag != 0) return tag;

	// Not tagged yet, assign an oid.
	tag = TodAgent_agGetNextOid->invoke(jni);
	
	err = jvmti->SetTag(obj, tag);
	check_jvmti_error(jvmti, err, "SetTag");

	return -tag;
}


JNIEXPORT jint JNICALL Java_tod_agent_EventInterpreter_getHostId(JNIEnv * jni, jclass)
{
	//TODO move to pure java code
	if (!hostIdRegistered) {
		hostID =  TodAgent_agGetHostId->invoke(jni);
		hostIdRegistered = true;
	}
	return hostID;
}


JNIEXPORT jint JNICALL Java_tod_agent_EventCollector_getHostId
	(JNIEnv * jni, jclass)
{
	if (!hostIdRegistered) {
			hostID =  TodAgent_agGetHostId->invoke(jni);
			hostIdRegistered = true;
		}
	return hostID;
}

}
// extern "C"
