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
#include <unistd.h>
#include <string.h>

#include "agent.h"
#include "agentimpl.h"

#include "utils.h"
#include "jniutils.h"
#include "md5.h"
#include "workingset.h"

#include <vector>

#include <iostream>
#include <fstream>
#include <boost/asio.hpp>
#include <boost/thread/tss.hpp>

#ifdef __cplusplus
extern "C" {
#endif

using boost::asio::ip::tcp;

// Outgoing commands
const char EXCEPTION_GENERATED = 20;
const char INSTRUMENT_CLASS = 50;
const char REGISTER_CLASS = 51;
const char FLUSH = 99;

// Incoming commands
const char SET_CAPTURE_EXCEPTIONS = 83;
const char SET_HOST_BITS = 84;
const char CONFIG_DONE = 99;

int AGENT_STARTED = 0;
int CAPTURE_STARTED = 0;
STREAM* gSocket = 0;

// Configuration data
bool cfgIsJVM14 = false;
int cfgCaptureExceptions = 0;
int cfgHostBits = 8; // Number of bits used to encode host id.
int cfgHostId = 0; // A host id assigned by the TODServer - not the "real" host id used in events.

// System properties configuration data.
char* propHost = NULL;
char* propClientName = NULL;
char* propPort = NULL;
char* _propVerbose = NULL;
int propVerbose = 2;

// Class and method references
StaticVoidMethod* ExceptionGeneratedReceiver_exceptionGenerated;
int isInitializingExceptionMethods = 0;

StaticVoidMethod* TracedMethods_setMode;
StaticVoidMethod* TOD_enable;
StaticVoidMethod* TOD_start;

// Method IDs for methods whose exceptions are ignored
jmethodID ignoredExceptionMethods[3];

// Mutex for class load callback
t_mutex loadMutex;

// This vector holds traced methods ids for methods
// that are registered prior to VM initialization.
// The two lower order bits of each int represent 
// the monitoring mode -- the higher order bits are the method id
std::vector<int> tmpTracedMethods;

/*
Connects to the instrumenting host
host: host to connect to
hostname: name of this host, sent to the peer.
*/
void agentConnect(char* host, char* port, char* clientName)
{
	if (propVerbose >=1) printf("Connecting to %s:%s\n", host, port);
	fflush(stdout);
	gSocket = new tcp::iostream(host, port);
	if (gSocket->fail()) fatal_error("Could not connect.\n");
	
	// Send signature (defined in AgentConfig)
	writeInt(gSocket, 0x3a71be0);
	
	// Send client name & JVM14 flag 
	if (propVerbose>=1) printf("Sending client name: %s\n", clientName);
	writeUTF(gSocket, clientName);
	writeByte(gSocket, cfgIsJVM14);
	flush(gSocket);
	
	cfgHostId = readInt(gSocket);
	if (propVerbose>=1) printf("Assigned host id: %ld\n", cfgHostId);
	fflush(stdout);
}


void agentConfigure()
{
	while(true)
	{
		int cmd = readByte(gSocket);
		switch(cmd)
		{
			case SET_CAPTURE_EXCEPTIONS:
				cfgCaptureExceptions = readByte(gSocket);
				if (propVerbose >= 1) printf("Capture exceptions: %s\n", cfgCaptureExceptions ? "Yes" : "No");
				break;
				
			case SET_HOST_BITS:
				cfgHostBits = readByte(gSocket);
				if (propVerbose >= 1) printf("Host bits: %d\n", cfgHostBits);
				break;

			case CONFIG_DONE:
				// Check host id vs host bits
				if (cfgHostBits > 0)
				{
					int mask = (1 << cfgHostBits) - 1;
					if ((cfgHostId & mask) != cfgHostId) fatal_error("Host id overflow.\n");
				}
				else 
				{
					cfgHostId = 0;
				}
				
				if (propVerbose >= 1) printf("Config done.\n");
				return;
				
			default:
				printf("Config command not handled: %d\n", cmd);
		}
	}
	fflush(stdout);
}

void registerTracedMethod(JNIEnv* jni, int tracedMethod)
{
	int behaviorId = tracedMethod >> 2;
	int mode = tracedMethod & 0x3;
	
	TracedMethods_setMode->invoke(jni, behaviorId, mode);
	if (propVerbose>=3) printf("Registered traced method: %d -> %d\n", behaviorId, mode);
}

/**
Registers the traced methods that were registered in tmpTracedMethods
*/ 
void registerTmpTracedMethods(JNIEnv* jni)
{
	if (propVerbose>=1) printf("Registering %d buffered traced methods\n", tmpTracedMethods.size());
	std::vector<int>::iterator iter = tmpTracedMethods.begin();
	std::vector<int>::iterator end = tmpTracedMethods.end();
	
	while (iter != end) registerTracedMethod(jni, *iter++);
	
	tmpTracedMethods.clear();
}

void registerTracedMethods(JNIEnv* jni, int nTracedMethods, int* tracedMethods)
{
	if (AGENT_STARTED)
	{
		if (propVerbose>=1 && nTracedMethods>0) printf("Registering %d traced methods\n", nTracedMethods);
		for (int i=0;i<nTracedMethods;i++) registerTracedMethod(jni, tracedMethods[i]);
	}
	else
	{
		if (propVerbose>=1 && nTracedMethods>0) printf("Buffering %d traced methods, will register later\n", nTracedMethods);
		for (int i=0;i<nTracedMethods;i++) tmpTracedMethods.push_back(tracedMethods[i]);
	}
}

void agentClassFileLoadHook(
	JNIEnv* jni, const char* name, 
	jint class_data_len, const unsigned char* class_data,
	jint* new_class_data_len, unsigned char** new_class_data,
	void* (*malloc_f)(unsigned int)) 
{
	if (! name) return; // Don't understand why, but it happens.

	if (! CAPTURE_STARTED)
	{
		if (! startsWith(name, "java/") && ! startsWith(name, "sun/") && ! startsWith(name, "tod/"))
		{
			printf("[TOD] Starting capture (%s).\n", name);
			fflush(stdout);

			CAPTURE_STARTED = 1;
			TOD_start->invoke(jni);
		}
	}

	if (propVerbose>=3) 
	{
		printf("Checking scope (hook) name: %s, len: %d\n", name, class_data_len);
		fflush(stdout);
	}

	// Unconditionally skip agent classes
	if (startsWith(name, "java/tod/")) return;
	if (startsWith(name, "tod/agent/")) return;
	
	int* tracedMethods = NULL;
	int nTracedMethods = 0;
	
	// Compute MD5 sum
	char md5Buffer[16];
	char md5String[33];
	md5_buffer((const char *) class_data, class_data_len, md5Buffer);
	md5_sig_to_string(md5Buffer, md5String, 33);
	if (propVerbose>=3) printf("MD5 sum: %s\n", md5String);
	
	{
		t_lock lock(loadMutex);
	
		// Send command
		writeByte(gSocket, INSTRUMENT_CLASS);
		
		// Send class name
		writeUTF(gSocket, name);
		
		// Send bytecode
		writeInt(gSocket, class_data_len);
		gSocket->write((char*) class_data, class_data_len);
		flush(gSocket);
		
		int len = readInt(gSocket);
		
		if (len > 0)
		{
			if (propVerbose>=1) printf("Instrumented: %s\n", name);

			*new_class_data = (unsigned char*) malloc_f(len);
			*new_class_data_len = len;
			
			gSocket->read((char*) *new_class_data, len);
			if (gSocket->eof()) fatal_ioerror("fread");
			if (propVerbose>=2) printf("Class definition downloaded.\n");
			
			nTracedMethods = readInt(gSocket);
			tracedMethods = new int[nTracedMethods];
			for (int i=0;i<nTracedMethods;i++) tracedMethods[i] = readInt(gSocket);
			
		}
		else if (len == -1)
		{
			char* errorString = readUTF(gSocket);
			fatal_error(errorString);
		}
	}
	
	// Register traced methods
	if (tracedMethods) 
	{
		registerTracedMethods(jni, nTracedMethods, tracedMethods);
		delete tracedMethods;
	}
	fflush(stdout);
}

void ignoreMethod(JNIEnv* jni, int index, char* className, char* methodName, char* signature)
{
	if (propVerbose>=2) printf("Loading (jni-ignore) %s\n", className);
	jclass clazz = jni->FindClass(className);
	if (clazz == NULL) printf("Could not load %s\n", className);
	jmethodID method = jni->GetMethodID(clazz, methodName, signature);
	if (method == NULL) printf("Could not find %s.%s%s\n", className, methodName, signature);
	jni->DeleteLocalRef(clazz);

	ignoredExceptionMethods[index] = method;
}

void initExceptionClasses(JNIEnv* jni)
{
	// Initialize the classes and method ids that will be used
	// for exception processing
	ExceptionGeneratedReceiver_exceptionGenerated = new StaticVoidMethod(
		jni, 
		"java/tod/ExceptionGeneratedReceiver",
		"exceptionGenerated", 
		"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Throwable;)V");
	
	// init ignored methods
	int i=0;
	ignoreMethod(jni, i++, "java/lang/ClassLoader", "findBootstrapClass", "(Ljava/lang/String;)Ljava/lang/Class;");
	ignoreMethod(jni, i++, "java/net/URLClassLoader$1", "run", "()Ljava/lang/Object;");
	ignoreMethod(jni, i++, "java/net/URLClassLoader", "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
}

bool agentShouldProcessException(JNIEnv* jni, jmethodID method)
{
	if (isInitializingExceptionMethods) return false; // Check if we are in the lazy init process
	
	if (cfgCaptureExceptions == 0) return false;
	if (AGENT_STARTED == 0) return false;
	
	if (! ExceptionGeneratedReceiver_exceptionGenerated)
	{
		isInitializingExceptionMethods = true;
		initExceptionClasses(jni);
		isInitializingExceptionMethods = false;
	}
	
	for (int i=0;i<sizeof(ignoredExceptionMethods);i++)
	{
		if (method == ignoredExceptionMethods[i]) return false;
	}

	return true;
}


void agentException(
	JNIEnv* jni,
	char* methodName,
	char* methodSignature,
	jclass methodDeclaringClass,
	char* methodDeclaringClassSignature,
	jobject exception,
	int bytecodeIndex)
{
	if (propVerbose>=1) printf("Exception generated: %s, %s, %s, %d\n", methodName, methodSignature, methodDeclaringClassSignature, bytecodeIndex);
	
	ExceptionGeneratedReceiver_exceptionGenerated->invoke(
		jni,
		jni->NewStringUTF(methodName),
		jni->NewStringUTF(methodSignature),
		jni->NewStringUTF(methodDeclaringClassSignature),
		bytecodeIndex,
 		exception);

	jthrowable ex = jni->ExceptionOccurred();
	if (ex)
	{
		jni->ExceptionDescribe();
		jni->FatalError("Exception detected while processing exeception event. Exiting.\n");
	}
}


void agentStart(JNIEnv* jni)
{
	printf("[TOD] Initializing...\n");
	fflush(stdout);
	
	// Initialize the classes and method ids that will be used
	// for registering traced methods
	
	TracedMethods_setMode = new StaticVoidMethod(jni, "java/tod/TracedMethods", "setMode", "(II)V");
	TOD_enable = new StaticVoidMethod(jni, "java/tod/AgentReady", "nativeAgentLoaded", "()V");	
	TOD_start = new StaticVoidMethod(jni, "java/tod/AgentReady", "start", "()V");	

	TOD_enable->invoke(jni);
	
	if (propVerbose>=1) printf("Agent start - done\n");
	fflush(stdout);
	
	AGENT_STARTED = 1;
	
	registerTmpTracedMethods(jni);
}

void agentInit(
	char* aPropVerbose,
	char* aPropHost,
	char* aPropPort,
	char* aPropCachePath,
	char* aPropClientName)
{
	printf("Loading TOD agent - v4.0\n");

	if (aPropVerbose)
	{
		propVerbose = atoi(aPropVerbose);
		printf("Property: agent-verbose=%d\n", propVerbose);
	}
	else
	{
		propVerbose = 0;
		printf("agent-verbose property not specified, going silent.\n");
	}

	propHost = aPropHost;
	if (propVerbose>=1) printf("Property: collector-host=%s\n", propHost);

	propClientName = aPropClientName;
	if (propClientName)
	{
		if (propVerbose>=1) printf("Property: client-name=%s\n", propClientName);
	}
	else
	{
		propClientName = "no-name";
	}

	propPort = aPropPort;
	if (propVerbose>=1) printf("Property: collector-port=%s\n", propPort);
	
	agentConnect(propHost, propPort, propClientName);
	agentConfigure();

	fflush(stdout);
}

void agentStop()
{
	if (gSocket)
	{
		{
			t_lock lock(loadMutex);

			writeByte(gSocket, FLUSH);
			flush(gSocket);
			if (propVerbose>=1) printf("Sent flush\n");
		}
	}
}

/*
 * Class: tod_core_ObjectIdentity
 * Method: get
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_java_tod_ObjectIdentity_get15
	(JNIEnv * jni, jclass, jobject obj)
{
	agentimplGetObjectId(jni, obj);
}

JNIEXPORT jint JNICALL Java_java_tod__1AgConfig_getHostId
	(JNIEnv * jni, jclass)
{
	return cfgHostId;
}

JNIEXPORT jstring JNICALL Java_java_tod__1AgConfig_getCollectorHost
  (JNIEnv* jni, jclass)
{
	return jni->NewStringUTF(propHost);
}

JNIEXPORT jstring JNICALL Java_java_tod__1AgConfig_getCollectorPort
  (JNIEnv* jni, jclass)
{
	return jni->NewStringUTF(propPort);
}

JNIEXPORT jstring JNICALL Java_java_tod__1AgConfig_getClientName
  (JNIEnv* jni, jclass)
{
	return jni->NewStringUTF(propClientName);
}

#ifdef WIN32
void tss_cleanup_implemented(void)
{
	// Avoid link error in win32
	// Not that this is not a good solution and probably causes some leaks.
	// See http://boost.org/doc/html/thread/release_notes.html#thread.release_notes.boost_1_32_0.change_log.static_link
}
#endif

#ifdef __cplusplus
}
#endif
