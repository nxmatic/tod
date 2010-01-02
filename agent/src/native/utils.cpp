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

#include <vector>


#include "utils.h"

void fatal_error(char* message)
{
	fprintf(stderr, "FATAL ERROR, ABORTING: ");
	fprintf(stderr, message);
	fprintf(stderr, "\n");
	exit(-1);
}

void fatal_ioerror(char* message)
{
	fprintf(stderr, "FATAL IO ERROR, ABORTING: ");
	perror(message);
	exit(-1);
}

void writeByte(std::ostream* f, const int i)
{
	f->put((char) (i & 0xff));
}

void writeBytes(std::ostream* f, const int n, const void* buffer)
{
	f->write((const char*)buffer, n);
}

void writeShort(std::ostream* f, const int v)
{
	char buf[2];
	buf[0] = 0xff & (v >> 8);
	buf[1] = 0xff & v;
	f->write(buf, 2);
}

void writeInt(std::ostream* f, const int v)
{
	char buf[4];
	buf[0] = 0xff & (v >> 24);
	buf[1] = 0xff & (v >> 16);
	buf[2] = 0xff & (v >> 8);
	buf[3] = 0xff & v;
	f->write(buf, 4);
}

void writeLong(std::ostream* f, const jlong v)
{
	char buf[8];
	buf[0] = 0xff & (v >> 56);
	buf[1] = 0xff & (v >> 48);
	buf[2] = 0xff & (v >> 40);
	buf[3] = 0xff & (v >> 32);
	buf[4] = 0xff & (v >> 24);
	buf[5] = 0xff & (v >> 16);
	buf[6] = 0xff & (v >> 8);
	buf[7] = 0xff & v;
	f->write(buf, 8);
}

int readByte(std::istream* f)
{
	return f->get();
}

void readBytes(std::istream* f, const int n, void* buffer)
{
	f->read((char*) buffer, n);
}


int readShort(std::istream* f)
{
	char buf[2];
	f->read(buf, 2);
	
	return (((buf[0] & 0xff) << 8) | (buf[1] & 0xff));
}

int readInt(std::istream* f)
{
	char buf[4];
	f->read(buf, 4);
	
	return (((buf[0] & 0xff) << 24) 
		| ((buf[1] & 0xff) << 16) 
		| ((buf[2] & 0xff) << 8) 
		| (buf[3] & 0xff));
}

void writeUTF(std::ostream* f, const char* s)
{
	int len = strlen(s);
	writeShort(f, len);
	f->write(s, len);
}

char* readUTF(std::istream* f)
{
	int len = readShort(f);
	char* s = (char*) malloc(len+1);
	f->read(s, len);
	s[len] = 0;
	
	return s;
}

void flush(std::ostream* f)
{
	f->flush();
}

bool startsWith(const char* aString, const char* aPrefix)
{
	int len = strlen(aPrefix);
	return strncmp(aPrefix, aString, len) == 0;
}

