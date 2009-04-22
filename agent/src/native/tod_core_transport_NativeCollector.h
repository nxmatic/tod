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
#include <jni.h>
/* Header for class tod_core_transport_NativeCollector */

#ifndef _Included_tod_core_transport_NativeCollector
#define _Included_tod_core_transport_NativeCollector
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    init
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_init
  (JNIEnv *, jclass, jstring, jint);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    allocThreadData
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_allocThreadData
  (JNIEnv *, jclass, jint);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    behaviorExit
 * Signature: (IJSJIIZLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_behaviorExit
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jint, jint, jboolean, jobject);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    exception
 * Signature: (IJSJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_exception
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jstring, jstring, jstring, jint, jobject);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    fieldWrite
 * Signature: (IJSJIILjava/lang/Object;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_fieldWrite
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jint, jint, jobject, jobject);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    instantiation
 * Signature: (IJSJIZIILjava/lang/Object;[Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_instantiation
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jint, jboolean, jint, jint, jobject, jobjectArray);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    localWrite
 * Signature: (IJSJIILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_localWrite
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jint, jint, jobject);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    methodCall
 * Signature: (IJSJIZIILjava/lang/Object;[Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_methodCall
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jint, jboolean, jint, jint, jobject, jobjectArray);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    output
 * Signature: (IJSJLtod/core/Output;[B)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_output
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jobject, jbyteArray);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    superCall
 * Signature: (IJSJIZIILjava/lang/Object;[Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_superCall
  (JNIEnv *, jclass, jint, jlong, jshort, jlong, jint, jboolean, jint, jint, jobject, jobjectArray);

/*
 * Class:     tod_core_transport_NativeCollector
 * Method:    thread
 * Signature: (IJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_tod_core_transport_NativeCollector_thread
  (JNIEnv *, jclass, jint, jlong, jstring);

#ifdef __cplusplus
}
#endif
#endif
