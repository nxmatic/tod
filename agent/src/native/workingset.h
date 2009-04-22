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
#ifndef _Included_workingset_h
#define _Included_workingset_h

class AbstractClassSet
{
public:
	virtual bool accept(const char* name)=0;
};

class SingleClassSet : public AbstractClassSet
{
public:
	SingleClassSet(char* aName);
	bool accept(const char* name);
	
private:
	char* name;
};

class AbstractPackageSet : public AbstractClassSet
{
public:
	AbstractPackageSet(char* aName);
	bool accept(const char* name);
	
protected:
	virtual bool acceptPackage(char* ref, char* name)=0;
	
private:
	char* name;
};

class RecursivePackageSet : public AbstractPackageSet
{
public:
	RecursivePackageSet(char* aName);
		
protected:
	bool acceptPackage(char* ref, char* name);
};

class SinglePackageSet : public AbstractPackageSet
{
public:
	SinglePackageSet(char* aName);
		
protected:
	bool acceptPackage(char* ref, char* name);
};

class SetOperation
{
public:
	SetOperation(bool aInclude, AbstractClassSet* aSet) ;
	
	bool include;
	AbstractClassSet* set;
};

class CompoundClassSet
{
public:
	bool accept(const char* name);
	
	AbstractClassSet* parseSet(char* ws, int& i);
	void parse(char* ws, int& i);
	
private:
	std::vector<SetOperation*> components;
};

CompoundClassSet* parseWorkingSet(char* ws);


#endif
