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
#include <boost/filesystem.hpp>
#include <boost/filesystem/fstream.hpp>
//#include <boost/thread/tss.hpp>

#ifdef __cplusplus
extern "C" {
#endif

using boost::asio::ip::tcp;
namespace fs = boost::filesystem;

// Outgoing commands
const char EXCEPTION_GENERATED = 20;
const char INSTRUMENT_CLASS = 50;
const char SYNC_CACHE_IDS = 51;
const char USE_CACHED_CLASS = 52;
const char FLUSH = 99;

// Incoming commands
const char SET_CAPTURE_EXCEPTIONS = 83;
const char SET_HOST_BITS = 84;
const char SET_CACHE_PATH = 85;
const char CONFIG_DONE = 99;

int AGENT_STARTED = 0;
int CAPTURE_STARTED = 0;
std::iostream* gSocket = 0;

// Configuration data
bool cfgIsJVM14 = false;
int cfgCaptureExceptions = 0;
int cfgHostBits = 8; // Number of bits used to encode host id.
int cfgHostId = 0; // A host id assigned by the TODServer - not the "real" host id used in events.
char* cfgCachePath = 0;

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

struct TracedMethodInfo
{
	jint behaviorId;
	jbyte instrumentationMode;
	jbyte callMode;
};

// This vector holds traced methods ids for methods
// that are registered prior to VM initialization.
// The two lower order bits of each int represent 
// the monitoring mode -- the higher order bits are the method id
std::vector<TracedMethodInfo> tmpTracedMethods;

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

void syncCache()
{
	if (propVerbose >= 1) printf("Synchronizing class cache\n");
	
	// Read last ids from cache
	fs::path cachePath(cfgCachePath);
	fs::path idsPath(cachePath / "ids.bin");
	if (fs::exists(idsPath))
	{
		fs::ifstream idsFile(idsPath);
		int lastClassId = readInt(&idsFile);
		int lastBehaviorId = readInt(&idsFile);
		int lastFieldId = readInt(&idsFile);
		idsFile.close();
		
		if (propVerbose >= 2) printf("Ids: %d, %d, %d\n", lastClassId, lastBehaviorId, lastFieldId);
		writeByte(gSocket, SYNC_CACHE_IDS);
		writeInt(gSocket, lastClassId);
		writeInt(gSocket, lastBehaviorId);
		writeInt(gSocket, lastFieldId);
		flush(gSocket);
	}
	
	fflush(stdout);
}

void writeIds(int classId, int behaviorId, int fieldId)
{
	fs::path cachePath(cfgCachePath);
	fs::path idsPath(cachePath / "ids.bin");
	fs::ofstream idsFile(idsPath);
	
	writeInt(&idsFile, classId);
	writeInt(&idsFile, behaviorId);
	writeInt(&idsFile, fieldId);
	
	idsFile.close();
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
				
			case SET_CACHE_PATH:
				cfgCachePath = readUTF(gSocket);
				if (propVerbose >= 1) printf("Cache path: %s\n", cfgCachePath);
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
				
				// Synchronize class cache
				if (cfgCachePath) syncCache();
				
				if (propVerbose >= 1) printf("Config done.\n");
				return;
				
			default:
				printf("Config command not handled: %d\n", cmd);
		}
		fflush(stdout);
	}
}

void registerTracedMethod(JNIEnv* jni, TracedMethodInfo& info)
{
	if (propVerbose>=2) printf(
		"Trying: %d -> (%d, %d)\n", 
		info.behaviorId, 
		info.instrumentationMode, 
		info.callMode);
	TracedMethods_setMode->invoke(jni, info.behaviorId, (jint)info.instrumentationMode, (jint)info.callMode);
	if (propVerbose>=2) printf(
		"Registered traced method: %d -> (%d, %d)\n", 
		info.behaviorId, 
		info.instrumentationMode, 
		info.callMode);
}

/**
Registers the traced methods that were registered in tmpTracedMethods
*/ 
void registerTmpTracedMethods(JNIEnv* jni)
{
	if (propVerbose>=1) printf("Registering %d buffered traced methods\n", tmpTracedMethods.size());
	std::vector<TracedMethodInfo>::iterator iter = tmpTracedMethods.begin();
	std::vector<TracedMethodInfo>::iterator end = tmpTracedMethods.end();
	
	if (propVerbose>=1) printf("Yeah, now starting\n");
	while (iter != end) registerTracedMethod(jni, *iter++);
	
	tmpTracedMethods.clear();
}

void registerTracedMethods(JNIEnv* jni, int nTracedMethods, TracedMethodInfo* tracedMethods)
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

struct ClassInfo
{
	jint id;
	
	unsigned char* data;
	jint dataLen;
	
	TracedMethodInfo* tracedMethods;
	int nTracedMethods;
};

void readTracedMethodInfo(std::istream* file, TracedMethodInfo& info)
{
	info.behaviorId = readInt(file);
	info.instrumentationMode = readByte(file);
	info.callMode = readByte(file);
}

void writeTracedMethodInfo(std::ostream* file, TracedMethodInfo& info)
{
	writeInt(file, info.behaviorId);
	writeByte(file, info.instrumentationMode);
	writeByte(file, info.callMode);
}

ClassInfo* checkCacheInfo(
	const char* name, 
	char md5Buffer_in[16],
	void* (*malloc_f)(unsigned int))
{
	fs::path cachePath(cfgCachePath);
	
	fs::path classPath(cachePath / name / "class");
	fs::path infoPath(cachePath / name / "info");

	if (fs::exists(infoPath))
	{
		fs::ifstream infoFile(infoPath);
		char md5Buffer_cached[16];
		readBytes(&infoFile, 16, md5Buffer_cached);
		
		if (memcmp(md5Buffer_cached, md5Buffer_in, 16) == 0)
		{
			if (propVerbose>=1) printf("Found valid in cache: %s\n", name);
			
			jint id = readInt(&infoFile);
			
			int nTracedMethods = readInt(&infoFile);
			TracedMethodInfo* tracedMethods = new TracedMethodInfo[nTracedMethods];
			for (int i=0;i<nTracedMethods;i++) readTracedMethodInfo(&infoFile, tracedMethods[i]);
			
			fs::ifstream classFile(classPath);
			jint len = fs::file_size(classPath);
			unsigned char* data = (unsigned char*) malloc_f(len);
			readBytes(&classFile, len, data);
			
			return new ClassInfo { id, data, len, tracedMethods, nTracedMethods };
		}
	}
	
	return NULL;
}

ClassInfo* requestInstrumentation(
	const char* name, 
	const char md5Buffer_in[16],
	const unsigned char* data,
	const jint len,
	void* (*malloc_f)(unsigned int))
{
	// Send command
	writeByte(gSocket, INSTRUMENT_CLASS);
	
	// Send class name
	writeUTF(gSocket, name);
	
	// Send bytecode
	writeInt(gSocket, len);
	writeBytes(gSocket, len, data);
	flush(gSocket);
	
	jint instrLen = readInt(gSocket);
	
	if (instrLen > 0)
	{
		if (propVerbose>=1) printf("Instrumented: %s\n", name);

		unsigned char* instrData = (unsigned char*) malloc_f(instrLen);
		
		readBytes(gSocket, instrLen, instrData);

		if (gSocket->eof()) fatal_ioerror("fread");
		if (propVerbose>=2) printf("Class definition downloaded.\n");
		
		jint id = readInt(gSocket);
		
		int nTracedMethods = readInt(gSocket);
		TracedMethodInfo* tracedMethods = new TracedMethodInfo[nTracedMethods];
		for (int i=0;i<nTracedMethods;i++) readTracedMethodInfo(gSocket, tracedMethods[i]);
		
		int lastClassId = readInt(gSocket);
		int lastBehaviorId = readInt(gSocket);
		int lastFieldId = readInt(gSocket);
		writeIds(lastClassId, lastBehaviorId, lastFieldId);
		
		// Write cache
		fs::path cachePath(cfgCachePath);
		fs::create_directories(cachePath / name);
		fs::path classPath(cachePath / name / "class");
		fs::path infoPath(cachePath / name / "info");
		
		fs::ofstream classFile(classPath);
		writeBytes(&classFile, instrLen, instrData);
		classFile.close();
		
		fs::ofstream infoFile(infoPath);
		writeBytes(&infoFile, 16, md5Buffer_in);
		writeInt(&infoFile, id);
		writeInt(&infoFile, nTracedMethods);
		for (int i=0;i<nTracedMethods;i++) writeTracedMethodInfo(&infoFile, tracedMethods[i]);
		infoFile.close();
		
		if (propVerbose>=2) std::cout << "Stored cache: " << infoPath << std::endl;


		return new ClassInfo { id, instrData, instrLen, tracedMethods, nTracedMethods };
	}
	else if (instrLen == -1)
	{
		char* errorString = readUTF(gSocket);
		fatal_error(errorString);
	}
	else return NULL;
}


void agentClassFileLoadHook(
	JNIEnv* jni, const char* name, 
	jint class_data_len, const unsigned char* class_data,
	jint* new_class_data_len, unsigned char** new_class_data,
	void* (*malloc_f)(unsigned int)) 
{
	if (! name) return; // Don't understand why, but it happens.

	// Unconditionally skip agent classes
	if (startsWith(name, "java/tod/")) return;
	if (startsWith(name, "tod2/agent/")) return;
	if (startsWith(name, "com/yourkit/")) return;
	
	if (! CAPTURE_STARTED)
	{
		if (! startsWith(name, "java/") 
			&& ! startsWith(name, "javax/")
			&& ! startsWith(name, "sun/") 
			&& ! startsWith(name, "tod/")
			&& ! startsWith(name, "tod2/"))
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
		
		ClassInfo* info = checkCacheInfo(name, md5Buffer, malloc_f);
		if (info == NULL) info = requestInstrumentation(name, md5Buffer, class_data, class_data_len, malloc_f);
		else
		{
			// Notify server of class loaded from cache
			writeByte(gSocket, USE_CACHED_CLASS);
			writeInt(gSocket, info->id);
		}
	
		if (info != NULL)
		{
			*new_class_data = info->data;
			*new_class_data_len = info->dataLen;
				
			// Register traced methods
			registerTracedMethods(jni, info->nTracedMethods, info->tracedMethods);
			delete info->tracedMethods;
			delete info;
		}
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
	
	TracedMethods_setMode = new StaticVoidMethod(jni, "java/tod/TracedMethods", "setMode", "(III)V");
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
	printf("Loading TOD agent - v4.0.5\n");

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

/*
#ifdef WIN32
void tss_cleanup_implemented(void)
{
	// Avoid link error in win32
	// Not that this is not a good solution and probably causes some leaks.
	// See http://boost.org/doc/html/thread/release_notes.html#thread.release_notes.boost_1_32_0.change_log.static_link
}
#endif
*/

#ifdef __cplusplus
}
#endif
