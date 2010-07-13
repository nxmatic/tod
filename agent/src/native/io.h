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
#ifndef _Included_io_h
#define _Included_io_h

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_open0
  (JNIEnv* jni, jclass, jstring jHost, jint port);

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_flush0
  (JNIEnv *, jclass, jint fd);

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_close0
  (JNIEnv *, jclass, jint);

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_write0
  (JNIEnv* jni, jclass, jint fd, jbyteArray bytes, jint pos, jint len);

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_writeStringPacket0
  (JNIEnv *, jclass, jint, jlong, jstring);

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_read0
  (JNIEnv* jni, jclass, jint fd, jbyteArray bytes, jint pos, jint len);

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_in_1avail0
  (JNIEnv* jni, jclass, jint fd);

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_out
  (JNIEnv* jni, jclass, jstring str);

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_err
  (JNIEnv* jni, jclass, jstring str);

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_outi
  (JNIEnv* jni, jclass, jstring str, jintArray v);

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_outb
  (JNIEnv* jni, jclass, jstring str, jbooleanArray v);

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_exit
  (JNIEnv* jni, jclass);


#ifdef __cplusplus
}
#endif

#endif