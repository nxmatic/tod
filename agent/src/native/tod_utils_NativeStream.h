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
/* Header for class tod_utils_NativeStream */

#ifndef _Included_tod_utils_NativeStream
#define _Included_tod_utils_NativeStream
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     tod_utils_NativeStream
 * Method:    fileno
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_fileno
  (JNIEnv *, jclass, jlong);

/*
 * Class:     tod_utils_NativeStream
 * Method:    fdopen
 * Signature: (Ljava/io/FileDescriptor;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_tod_utils_NativeStream_fdopen
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     tod_utils_NativeStream
 * Method:    setFD
 * Signature: (Ljava/io/FileDescriptor;I)V
 */
JNIEXPORT void JNICALL Java_tod_utils_NativeStream_setFD
  (JNIEnv *, jclass, jobject, jint);

/*
 * Class:     tod_utils_NativeStream
 * Method:    getFD
 * Signature: (Ljava/io/FileDescriptor;)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_getFD
  (JNIEnv *, jclass, jobject);

/*
 * Class:     tod_utils_NativeStream
 * Method:    fwrite
 * Signature: (J[III)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_fwrite
  (JNIEnv *, jclass, jlong, jintArray, jint, jint);

/*
 * Class:     tod_utils_NativeStream
 * Method:    fread
 * Signature: (J[III)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_fread
  (JNIEnv *, jclass, jlong, jintArray, jint, jint);

/*
 * Class:     tod_utils_NativeStream
 * Method:    fflush
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_fflush
  (JNIEnv *, jclass, jlong);

/*
 * Class:     tod_utils_NativeStream
 * Method:    fseek
 * Signature: (JJI)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_fseek
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     tod_utils_NativeStream
 * Method:    feof
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_feof
  (JNIEnv *, jclass, jlong);

/*
 * Class:     tod_utils_NativeStream
 * Method:    fopen
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_tod_utils_NativeStream_fopen
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     tod_utils_NativeStream
 * Method:    fclose
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_fclose
  (JNIEnv *, jclass, jlong);

/*
 * Class:     tod_utils_NativeStream
 * Method:    recv
 * Signature: (I[II)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_recv
  (JNIEnv *, jclass, jint, jintArray, jint);

/*
 * Class:     tod_utils_NativeStream
 * Method:    send
 * Signature: (I[II)I
 */
JNIEXPORT jint JNICALL Java_tod_utils_NativeStream_send
  (JNIEnv *, jclass, jint, jintArray, jint);

/*
 * Class:     tod_utils_NativeStream
 * Method:    b2i
 * Signature: ([B[I)V
 */
JNIEXPORT void JNICALL Java_tod_utils_NativeStream_b2i
  (JNIEnv *, jclass, jbyteArray, jintArray);

/*
 * Class:     tod_utils_NativeStream
 * Method:    i2b
 * Signature: ([I[B)V
 */
JNIEXPORT void JNICALL Java_tod_utils_NativeStream_i2b
  (JNIEnv *, jclass, jintArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
