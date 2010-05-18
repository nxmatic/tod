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
#include "io.h"

#include <stdio.h>
#include <stdlib.h>
#include <boost/asio.hpp>
#include <vector>

typedef std::iostream STREAM;
using boost::asio::ip::tcp;

// Maps file descriptors to streams
std::vector<STREAM*> fds;

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_open0
  (JNIEnv* jni, jclass, jstring jHost, jint port)
{
	int len = jni->GetStringUTFLength(jHost);
	const char* bHost = jni->GetStringUTFChars(jHost, NULL);
	char* host = (char*) malloc(len+1);
	memcpy(host, bHost, len);
	host[len] = 0;
	
	char sPort[12];
	sprintf(sPort, "%d", port);

	STREAM* s = new tcp::iostream(host, sPort);
	int fd;
	if (! s->fail())
	{
		fd = fds.size();
		fds.push_back(s);
	}
	else
	{
		fd = -1;
		printf("Cannot connect to %s:%s\n", host, sPort);
		fflush(stdout);
	}

	free(host);
	jni->ReleaseStringUTFChars(jHost, bHost);

	return fd;
}

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_flush0
  (JNIEnv *, jclass, jint fd)
{
	if (fd >= fds.size()) return -10;
	STREAM* s = fds[fd];
	if (! s->good()) return -1;
	s->flush();
	if (! s->good()) return -2;
	return 0;
}

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_close0
  (JNIEnv *, jclass, jint)
{
	return 0;
}

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_write0
  (JNIEnv* jni, jclass, jint fd, jbyteArray bytes, jint pos, jint len)
{
	if (fd >= fds.size()) return -10;
	STREAM* s = fds[fd];
	int result;
	char* carray = (char*) jni->GetPrimitiveArrayCritical(bytes, NULL);
	
	if (! s->good()) result = -1;
	else
	{
		s->write(carray+pos, len);
		if (! s->good()) result = -1;
		else result = len;
	}
	
	jni->ReleasePrimitiveArrayCritical(bytes, carray, JNI_ABORT);
	
	return result;
}

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_writeStringPacket0
  (JNIEnv* jni, jclass, jint fd, jlong id, jstring str)
{
	if (fd >= fds.size()) return -10;
	STREAM* s = fds[fd];
	int result;
	jboolean isCopy;
	
	jsize len = jni->GetStringLength(str);
	const jchar* chars = jni->GetStringChars(str, &isCopy);
	
	if (! s->good()) result = -1;
	else
	{
		s->write((char*) &id, 8);
		s->write((char*) &len, 4);
		s->write((char*) chars, len*2);
		if (! s->good()) result = -1;
		else result = 0;
	}
	
	jni->ReleaseStringChars(str, chars);
	
	return result;
}

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_read0
  (JNIEnv* jni, jclass, jint fd, jbyteArray bytes, jint pos, jint len)
{
	if (fd >= fds.size()) return -10;
	STREAM* s = fds[fd];
	int result;
	char* carray = (char*) jni->GetPrimitiveArrayCritical(bytes, NULL);
	
	if (! s->good()) result = -2;
	else
	{
		s->read(carray+pos, len);
		if (s->bad() || s->fail()) result = -3;
		else
		{
			result = s->gcount();
			if (result == 0 && s->eof()) result = -1;
		}
	}
	
	jni->ReleasePrimitiveArrayCritical(bytes, carray, 0);
	
	return result;
}

JNIEXPORT jint JNICALL Java_java_tod_io__1SocketChannel_in_1avail0
  (JNIEnv* jni, jclass, jint fd)
{
	if (fd >= fds.size()) return -10;
	STREAM* s = fds[fd];
	return s->rdbuf()->in_avail();
}

void ioPrint0(const char* format, JNIEnv* jni, jstring str, FILE* o)
{
	int len = jni->GetStringUTFLength(str);
	char* b = (char*) jni->GetStringUTFChars(str, NULL);
	char* c = (char*) malloc(len+1);
	memcpy(c, b, len);
	c[len] = 0;
	
	fprintf(o, format, c);
	fflush(o);
	
	free(c);
	jni->ReleaseStringUTFChars(str, b);
}

void ioPrintln(JNIEnv* jni, jstring str, FILE* o)
{
	ioPrint0("%s\n", jni, str, o);
}

void ioPrint(JNIEnv* jni, jstring str, FILE* o)
{
	ioPrint0("%s", jni, str, o);
}

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_out
  (JNIEnv* jni, jclass, jstring str)
{
	ioPrintln(jni, str, stdout);
}

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_err
  (JNIEnv* jni, jclass, jstring str)
{
	ioPrintln(jni, str, stderr);
}

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_outi
  (JNIEnv* jni, jclass, jstring str, jintArray v)
{
	ioPrint(jni, str, stdout);
	jsize len = jni->GetArrayLength(v);
	jint* elem = jni->GetIntArrayElements(v, NULL);
	
	for(int i=0;i<len;i++) printf("%d ", elem[i]);
	printf("\n");
	fflush(stdout);
	
	jni->ReleaseIntArrayElements(v, elem, JNI_ABORT);
}

JNIEXPORT jint JNICALL Java_java_tod_io__1IO_outb
  (JNIEnv* jni, jclass, jstring str, jbooleanArray v)
{
	ioPrint(jni, str, stdout);
	jsize len = jni->GetArrayLength(v);
	jboolean* elem = jni->GetBooleanArrayElements(v, NULL);
	
	for(int i=0;i<len;i++) printf("%d ", elem[i]);
	printf("\n");
	fflush(stdout);
	
	jni->ReleaseBooleanArrayElements(v, elem, JNI_ABORT);
}


#ifdef __cplusplus
}
#endif
