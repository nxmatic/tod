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

bool startsWith(const char* aString, const char* aPrefix)
{
	int len = strlen(aPrefix);
	return strncmp(aPrefix, aString, len) == 0;
}

void test(CompoundClassSet* set, char* name, bool match)
{
	bool b = set->accept(name);
	printf("%s -> %d\n", name, b);
	if ((b != 0) != (match != 0))
	{
		printf("Fail\n");
		exit(1);
	}
}

int main(int, char**)
{
	printf("Let's go\n");
	CompoundClassSet* set;
	
	set = parseWorkingSet("[-java/io/** +java/io/yes/* -tod/agent]");
	test(set, "java/io/Tata", false);
	test(set, "java/io/blip/Titi", false);
	test(set, "java/io/yes/Tata", true);
	test(set, "java/io/yes/no/Tata", false);
	
	set = parseWorkingSet("[+tod.impl.evdbng.db.IndexSet +tod.impl.evdbng.db.SimpleIndexSet +zz.utils.cache.MRUBuffer +tod.tools.ConcurrentMRUBuffer +tod.impl.evdbng.db.IndexSet$IndexManager +tod.impl.evdbng.db.IndexSet$BTreeWrapper]");
	test(set, "tod/impl/evdbng/db/Indexes", false);
	test(set, "tod/impl/evdbng/db/IndexSet$IndexManager", true);
	test(set, "tod/impl/evdbng/db/IndexSet", true);
	
	set = parseWorkingSet("[-java.** -javax.** -sun.** -com.sun.** -org.ietf.jgss.** -org.omg.** -org.w3c.** -org.xml.** -org.jibx.**]");
	test(set, "org/xmlpull/v1/XmlPullParserFactory", true);
	test(set, "org/xmldb/api/DatabaseManager", true);
	
	set = parseWorkingSet("[]");
	test(set, "org/xmlpull/v1/XmlPullParserFactory", false);
	test(set, "org/xmldb/api/DatabaseManager", false);
	
	printf("Success\n");
}
