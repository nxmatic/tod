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
#include <boost/filesystem/convenience.hpp>
#include <boost/thread/tss.hpp>

#ifdef __cplusplus
extern "C" {
#endif

using boost::asio::ip::tcp;
namespace fs = boost::filesystem;

// Outgoing commands
const char EXCEPTION_GENERATED = 20;
const char INSTRUMENT_CLASS = 50;
const char REGISTER_CLASS = 51;
const char FLUSH = 99;

// Incoming commands
const char SET_SKIP_CORE_CLASSES = 81;
const char SET_CAPTURE_EXCEPTIONS = 83;
const char SET_HOST_BITS = 84;
const char SET_WORKINGSET = 85;
const char SET_STRUCTDB_ID = 86;
const char SET_SPECIALCASE_WORKINGSET = 87;
const char CONFIG_DONE = 99;

int AGENT_STARTED = 0;
int CAPTURE_STARTED = 0;
STREAM* gSocket = 0;

// Configuration data
bool cfgIsJVM14 = false;
int cfgSkipCoreClasses = 0;
int cfgCaptureExceptions = 0;
int cfgHostBits = 8; // Number of bits used to encode host id.
int cfgHostId = 0; // A host id assigned by the TODServer - not official.
char* cfgWorkingSet = "undefined"; // The current working set of instrumentation
char* cfgSpecialCaseWorkingSet = "undefined"; // The special case working set
char* cfgStructDbId = "undefined"; // The id of the structure database used by the peer
int cfgObfuscation = 0; // set to 1 ofuscated version of the agent: package tod.agentX instead of tod.agent

// System properties configuration data.
char* propHost = NULL;
char* propClientName = NULL;
char* propPort = NULL;
char* propCachePath = NULL;
char* _propVerbose = NULL;
int propVerbose = 2;

// directory prefix for the class cache. It is the MD5 sum of (working set, struct db id).
char* classCachePrefix = "bad";

// Working set
CompoundClassSet* workingSet = NULL;

// Working set for classes that should be included even if not in scope
CompoundClassSet* specialCaseWorkingSet = NULL;

// Class and method references
StaticVoidMethod* ExceptionGeneratedReceiver_exceptionGenerated;
int isInitializingExceptionMethods = 0;

StaticVoidMethod* TracedMethods_setTraced;
StaticVoidMethod* TOD_enable;
StaticVoidMethod* TOD_start;

// Method IDs for methods whose exceptions are ignored
jmethodID ignoredExceptionMethods[3];

// Mutex for class load callback
t_mutex loadMutex;

// This vector holds traced methods ids for methods
// that are registered prior to VM initialization.
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


/*
* Tries to create all the directories denoted by the given name.
*/
int mkdirs(fs::path& p)
{
	try
	{
		fs::create_directories(p.branch_path());
		return 1;
	}
	catch(...)
	{
		return 0;
	}
}

void agentConfigure()
{
	while(true)
	{
		int cmd = readByte(gSocket);
		switch(cmd)
		{
			case SET_SKIP_CORE_CLASSES:
				cfgSkipCoreClasses = readByte(gSocket);
				if (propVerbose >= 1) printf("Skipping core classes: %s\n", cfgSkipCoreClasses ? "Yes" : "No");
				break;

			case SET_CAPTURE_EXCEPTIONS:
				cfgCaptureExceptions = readByte(gSocket);
				if (propVerbose >= 1) printf("Capture exceptions: %s\n", cfgCaptureExceptions ? "Yes" : "No");
				break;
				
			case SET_HOST_BITS:
				cfgHostBits = readByte(gSocket);
				if (propVerbose >= 1) printf("Host bits: %d\n", cfgHostBits);
				break;

			case SET_WORKINGSET:
				cfgWorkingSet = readUTF(gSocket);
				if (propVerbose >= 1) printf("Working set: %s\n", cfgWorkingSet);
				break;

			case SET_SPECIALCASE_WORKINGSET:
				cfgSpecialCaseWorkingSet = readUTF(gSocket);
				if (propVerbose >= 2) printf("Special cases working set: %s\n", cfgSpecialCaseWorkingSet);
				break;

			case SET_STRUCTDB_ID:
				cfgStructDbId = readUTF(gSocket);
				if (propVerbose >= 1) printf("Structure database id.: %s\n", cfgStructDbId);
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
				
				// Compute class cache prefix
				int len = strlen(cfgWorkingSet)+strlen(cfgStructDbId)+2;
				char* sigSrc = (char*) malloc(len);
				snprintf(sigSrc, len, "%s/%s", cfgWorkingSet, cfgStructDbId);
				if (propVerbose >= 1) printf("Computing class cache prefix from: %s\n", sigSrc);
				
				char md5Buffer[16];
				classCachePrefix = (char*) malloc(33);
				md5_buffer(sigSrc, strlen(sigSrc), md5Buffer);
				md5_sig_to_string(md5Buffer, classCachePrefix, 33);
				free(sigSrc);

				if (propVerbose >= 1) printf("Class cache prefix: %s\n", classCachePrefix);
				
				// Parse working sets
				workingSet = parseWorkingSet(cfgWorkingSet);
				specialCaseWorkingSet = parseWorkingSet(cfgSpecialCaseWorkingSet);
				
				if (propVerbose >= 1) printf("Config done.\n");
				return;
		}
	}
	fflush(stdout);
}

void registerTracedMethod(JNIEnv* jni, int tracedMethod)
{
	TracedMethods_setTraced->invoke(jni, tracedMethod);
	if (propVerbose>=3) printf("Registered traced method: %d\n", tracedMethod);
}

/**
Registers the traced methods that were registered in tmpTracedMethods
*/ 
void registerTmpTracedMethods(JNIEnv* jni)
{
	if (propVerbose>=1) printf("Registering %d buffered traced methods\n", tmpTracedMethods.size());
	std::vector<int>::iterator iter = tmpTracedMethods.begin();
	std::vector<int>::iterator end = tmpTracedMethods.end();
	
	while (iter != end)
	{
		registerTracedMethod(jni, *iter++);
	}
	
	tmpTracedMethods.clear();
}

void registerTracedMethods(JNIEnv* jni, int nTracedMethods, int* tracedMethods)
{
	if (AGENT_STARTED)
	{
		if (propVerbose>=1 && nTracedMethods>0) printf("Registering %d traced methods\n", nTracedMethods);
		for (int i=0;i<nTracedMethods;i++)
		{
			registerTracedMethod(jni, tracedMethods[i]);
		}
	}
	else
	{
		if (propVerbose>=1 && nTracedMethods>0) printf("Buffering %d traced methods, will register later\n", nTracedMethods);
		for (int i=0;i<nTracedMethods;i++)
		{
			tmpTracedMethods.push_back(tracedMethods[i]);
		}
	}
	if (tracedMethods) delete tracedMethods;
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
	if (cfgObfuscation)
	{
		if (startsWith(name, "java/todX/")) return;
		if (startsWith(name, "tod/agentX/")) return;
	}
	else
	{
		if (startsWith(name, "java/tod/")) return;
		if (startsWith(name, "tod/agent/")) return;
	}
	
	// Check scope
	bool skip = false;
	if (cfgSkipCoreClasses 
		&& (
			startsWith(name, "java/")
			|| startsWith(name, "sun/")
// 			|| startsWith(name, "javax/")
			|| startsWith(name, "com/sun/")
			|| startsWith(name, "net/sourceforge/retroweaver/")
		)) skip = true;
	
	if (! workingSet->accept(name)) 
	{
		if (propVerbose>=2) 
		{
			printf("Rejected by working set: %s\n", name);
			fflush(stdout);
		}
		skip = true;
	}
	
	// Check special cases
	if (specialCaseWorkingSet->accept(name))
	{
		if (propVerbose>=2) 
		{
			printf("Accepted as special case: %s\n", name);
			fflush(stdout);
		}
		skip = false;
	}
	
	if (skip)
	{
		if (!gSocket)
		{
			printf("[TOD] Unable to register %s\n", name);
			fflush(stdout);
			return;
		}
		
		{
			t_lock lock(loadMutex);
		
			if (propVerbose>=1) printf("Registering %s\n", name);
			
			// Send command
			writeByte(gSocket, REGISTER_CLASS);
			
			// Send class name
			writeUTF(gSocket, name);
			
			// Send bytecode
			writeInt(gSocket, class_data_len);
			gSocket->write((char*) class_data, class_data_len);
		}
		
		return;
	}
	
	int* tracedMethods = NULL;
	int nTracedMethods = 0;
	
	// Compute MD5 sum
	char md5Buffer[16];
	char md5String[33];
	md5_buffer((const char *) class_data, class_data_len, md5Buffer);
	md5_sig_to_string(md5Buffer, md5String, 33);
	if (propVerbose>=3) printf("MD5 sum: %s\n", md5String);
	
	// Compute cache file paths	
	fs::path cacheFilePath;
	fs::path tracedCacheFilePath;
	
	if (propCachePath != NULL)
	{
		char cacheFileName[2000];
		char tracedCacheFileName[2000];
		cacheFileName[0] = 0;
		tracedCacheFileName[0] = 0;
		
		// Escape the class name, as all characters allowed for class names are
		// not necessarily allowed for files on all platforms.
		int l = strlen(name);
		char escapedName[l+1];
		strcpy(escapedName, name);
		for(int i=0;i<l;i++) if (escapedName[i] == '$') escapedName[i] = '_';
		
		snprintf(cacheFileName, sizeof(cacheFileName), "%s/%s/%s.%s.class", 
			propCachePath, classCachePrefix, escapedName, md5String);
			
		snprintf(tracedCacheFileName, sizeof(tracedCacheFileName), "%s/%s/%s.%s.tm", 
			propCachePath, classCachePrefix, escapedName, md5String);
		
		cacheFilePath = fs::path(cacheFileName);
		tracedCacheFilePath = fs::path(tracedCacheFileName);
		fflush(stdout);
	}

	// Check if we have a cached version
	if (propCachePath != NULL)
	{
		if (propVerbose>=2) 
		{
			printf ("Looking for %s\n", cacheFilePath.native_file_string().c_str());
			fflush(stdout);
		}
		
		// Check if length is 0
		if (fs::exists(cacheFilePath.native_file_string()))
		{
			int len = fs::file_size(cacheFilePath);
			
			if (len == 0)
			{
				if (propVerbose>=2) printf ("Using original\n");
			}
			else
			{
				std::fstream f;
				
				// Read class definition
				f.open(cacheFilePath.native_file_string().c_str(), std::ios_base::in | std::ios_base::binary);
				if (f.fail()) fatal_error("Could not open class file");
				
				if (propVerbose>=1) printf("Instrumented: %s\n", name);

				*new_class_data = (unsigned char*) malloc_f(len);
				*new_class_data_len = len;
		
				f.read((char*) *new_class_data, len);
				if (f.eof()) fatal_ioerror("EOF on read from class file");
				if (propVerbose>=2) printf("Class definition uploaded from cache.\n");
				f.close();
				
				// Read traced methods array
				f.open(tracedCacheFilePath.native_file_string().c_str(), std::ios_base::in | std::ios_base::binary);
				if (f.fail()) fatal_error("Could not open traced methods file");
				nTracedMethods = readInt(&f);
				tracedMethods = new int[nTracedMethods];
				for (int i=0;i<nTracedMethods;i++) tracedMethods[i] = readInt(&f);
				f.close();
			}
			
			// Register traced methods
			registerTracedMethods(jni, nTracedMethods, tracedMethods);
			
			fflush(stdout);
			return;
		}
		else
		{
			if (propVerbose>=2) 
			{
				printf ("Class not found in cache.\n");
				fflush(stdout);
			}
		}
	}
	
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
			if (propVerbose>=2) printf("Class definition uploaded.\n");
			
			nTracedMethods = readInt(gSocket);
			tracedMethods = new int[nTracedMethods];
			for (int i=0;i<nTracedMethods;i++) tracedMethods[i] = readInt(gSocket);
			
			// Cache class
			if (propCachePath != NULL)
			{
				if (propVerbose>=2) printf("Caching %s\n", cacheFilePath.native_file_string().c_str());
				if (! mkdirs(cacheFilePath)) fatal_ioerror("Error in mkdirs");
		
				std::fstream f;
				
				// Cache bytecode
				f.open(cacheFilePath.native_file_string().c_str(), std::ios_base::out | std::ios_base::binary | std::ios_base::trunc);
				if (f.fail()) fatal_ioerror("Opening cache class file for output");
				f.write((char*) *new_class_data, len);
				if (f.bad()) fatal_ioerror("Writing cached class");
				
				f.flush();
				f.close();
				
				// Cache traced methods
				f.open(tracedCacheFilePath.native_file_string().c_str(), std::ios_base::out | std::ios_base::binary | std::ios_base::trunc);
				if (f.fail()) fatal_ioerror("Opening cache traced methods file for output");
				writeInt(&f, nTracedMethods);
				for (int i=0;i<nTracedMethods;i++) writeInt(&f, tracedMethods[i]);
				f.flush();
				f.close();
				
				if (propVerbose>=2) printf("Cached.\n");
			}
		}
		else if (len == 0 && propCachePath != NULL)
		{
			// Mark class as not instrumented.
			if (propVerbose>=2) printf("Caching empty: %s\n", cacheFilePath.native_file_string().c_str());
			if (! mkdirs(cacheFilePath)) fatal_ioerror("Error in mkdirs");
			
			std::fstream f (cacheFilePath.native_file_string().c_str(), std::ios_base::out | std::ios_base::binary | std::ios_base::trunc);
			if (f.fail()) fatal_ioerror("Opening cache class file for output");
			f.flush();
			f.close();
			if (propVerbose>=2) printf("Cached empty.\n");
		}
		else if (len == -1)
		{
			char* errorString = readUTF(gSocket);
			fatal_error(errorString);
		}
	}
	
	// Register traced methods
	registerTracedMethods(jni, nTracedMethods, tracedMethods);
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
	
	if (cfgObfuscation ==1 ) 
	ExceptionGeneratedReceiver_exceptionGenerated = new StaticVoidMethod(
		jni, 
		"java/todX/ExceptionGeneratedReceiver",
		"exceptionGenerated", 
		"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Throwable;)V");
	else 	ExceptionGeneratedReceiver_exceptionGenerated = new StaticVoidMethod(
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
	
	if (cfgObfuscation == 1)	
	{
		TracedMethods_setTraced = new StaticVoidMethod(jni, "java/todX/TracedMethods", "setTraced", "(I)V");
		TOD_enable = new StaticVoidMethod(jni, "java/todX/AgentReady", "nativeAgentLoaded", "()V");
		TOD_start = new StaticVoidMethod(jni, "java/todX/AgentReady", "start", "()V");
	}
	else 
	{
		TracedMethods_setTraced = new StaticVoidMethod(jni, "java/tod/TracedMethods", "setTraced", "(I)V");
		TOD_enable = new StaticVoidMethod(jni, "java/tod/AgentReady", "nativeAgentLoaded", "()V");	
		TOD_start = new StaticVoidMethod(jni, "java/tod/AgentReady", "start", "()V");	
	}

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
	fs::path::default_name_check(fs::no_check);

	printf("Loading TOD agent - v4.0\n");
	if (cfgObfuscation == 1) printf(">>>>WARNING obfuscation form agent package to agentX is considered \n");
	fflush(stdout);

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

	propCachePath = aPropCachePath;
	if (propCachePath && propVerbose>=1) printf("Property: agent-cache-path=%s\n", propCachePath);

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

JNIEXPORT jlong JNICALL Java_java_todX_ObjectIdentity_get15
	(JNIEnv * jni, jclass cls, jobject obj)
{
	return Java_java_tod_ObjectIdentity_get15(jni, cls, obj);
}

JNIEXPORT jint JNICALL Java_java_todX__1AgConfig_getHostId
	(JNIEnv * jni, jclass cls)
{
	return Java_java_tod__1AgConfig_getHostId(jni, cls);
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
