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
#ifndef _Included_utils_h
#define _Included_utils_h

#include <stdio.h>
#include <jni.h>
#include <iostream>
#include <boost/thread/recursive_mutex.hpp>


typedef boost::recursive_mutex t_mutex;
typedef boost::recursive_mutex::scoped_lock t_lock;

void fatal_error(char*);
void fatal_ioerror(char*);

void writeByte(std::ostream* f, const int i);
void writeBytes(std::ostream* f, const int n, const void* buffer);
void writeShort(std::ostream* f, const int v);
void writeInt(std::ostream* f, const int v);
void writeLong(std::ostream* f, const jlong v);
int readByte(std::istream* f);
void readBytes(std::istream* f, const int n, void* buffer);
int readShort(std::istream* f);
int readInt(std::istream* f);
void writeUTF(std::ostream* f, const char* s);
char* readUTF(std::istream* f);
void flush(std::ostream* f);

bool startsWith(const char* aString, const char* aPrefix);


#endif
