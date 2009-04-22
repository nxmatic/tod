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

#include <vector>

#include "workingset.h"

#ifndef TEST
#include "utils.h"
#endif

#define MAX_NAME_LENGTH 4096

#ifdef TEST
bool startsWith(const char* aString, const char* aPrefix)
{
	int len = strlen(aPrefix);
	return strncmp(aPrefix, aString, len) == 0;
}
#endif


SingleClassSet::SingleClassSet(char* aName)
	: name(aName) {};

bool SingleClassSet::accept(const char* name)
{
	return strcmp(this->name, name) == 0;
}

AbstractPackageSet::AbstractPackageSet(char* aName)
	: name(aName) {};

bool AbstractPackageSet::accept(const char* name)
{
	char buffer[MAX_NAME_LENGTH];
	char* last = 0;
	int i=0;
	while(name[i])
	{
		buffer[i] = name[i];
		if (buffer[i] == '/') last = &buffer[i];
		i++;
	}
	
	if (last)
	{
		*last = 0;
		return acceptPackage(this->name, buffer);
	}
	else
	{
		return acceptPackage(this->name, "");
	}
}

RecursivePackageSet::RecursivePackageSet(char* aName)
	: AbstractPackageSet(aName) {};

bool RecursivePackageSet::acceptPackage(char* ref, char* name)
{
	return startsWith(name, ref) && (name[strlen(ref)] == '/' || name[strlen(ref)] == 0);
}

SinglePackageSet::SinglePackageSet(char* aName)
	: AbstractPackageSet(aName) {};

bool SinglePackageSet::acceptPackage(char* ref, char* name)
{
	return strcmp(ref, name) == 0;
}

SetOperation::SetOperation(bool aInclude, AbstractClassSet* aSet) 
	: include(aInclude), set(aSet) {};

bool isNameChar(char c)
{
	return 
		(c >= 'a' && c <= 'z') 
		|| (c >= 'A' && c <= 'Z') 
		|| (c >= '0' && c <= '9')
		|| c == '_' || c == '$' 
		|| c == '/';
}

bool CompoundClassSet::accept(const char* name)
{
	if (components.size() == 0) return false;
	
	for (int i=components.size()-1;i>=0;i--)
	{
		SetOperation* op = components[i];
		if (op->set->accept(name)) return op->include;
	}
	
	return ! components[0]->include;
}

AbstractClassSet* CompoundClassSet::parseSet(char* ws, int& i)
{
	char buffer[MAX_NAME_LENGTH];
	int j=0;
	
	bool inName = true;
	int stars = 0;
	while(ws[i])
	{
		char c = ws[i++];
		if (inName)
		{
			if (isNameChar(c)) buffer[j++] = c;
			else if (c == ' ' || c == ']') break;
			else if (c == '*')
			{
				stars++;
				inName = false;
			}
			else throw "Cannot parse";
		}
		else
		{
			if (c == ' ' || c == ']') break;
			else if (c == '*') stars++;
			else throw "Cannot parse";
		}
	}
	
	if (buffer[j-1] == '/') buffer[j-1] = 0;
	else buffer[j] = 0;
	char* name = new char[j+1];
	strcpy(name, buffer);
	
	switch(stars)
	{
		case 0: return new SingleClassSet(name);
		case 1: return new SinglePackageSet(name);
		case 2: return new RecursivePackageSet(name);
		default: throw "Cannot parse: too many stars";
	}
}

void CompoundClassSet::parse(char* ws, int& i)
{
	while(ws[i])
	{
		switch(ws[i++])
		{
			case '+':
				components.push_back(new SetOperation(true, parseSet(ws, i)));
				break;
				
			case '-':
				components.push_back(new SetOperation(false, parseSet(ws, i)));
				break;
			
			case ']':
				return;
			
			case ' ':
				break;
				
			default:
				throw "Cannot parse: invalid token";
		}
	}
}

CompoundClassSet* parseWorkingSet(char* ws)
{
	int len = strlen(ws);
	char buffer[len+1];
	for(int i=0;i<=len;i++) buffer[i] = ws[i] == '.' ? '/' : ws[i];
	
	if (ws[0] == '[')
	{
		CompoundClassSet* set = new CompoundClassSet();
		int i=1;
		set->parse(buffer, i);
		return set;
	}
	else throw "Cannot parse: should start by '['";
}

#ifdef TEST
void test(CompoundClassSet* set, char* name)
{
	printf("%s -> %d\n", name, set->accept(name));
}

int main(int, char**)
{
	printf("Let's go\n");
	CompoundClassSet* set = parseWorkingSet("[-java/io/** +java/io/yes/* -tod/agent]");
	test(set, "java/io/Tata");
	test(set, "java/io/blip/Titi");
	test(set, "java/io/yes/Tata");
	test(set, "java/io/yes/no/Tata");
}
#endif
