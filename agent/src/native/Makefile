# The following variables should be defined in a file whose name 
# is passed in $(ENV_DEF). Example:
#
# PLATFORM=linux
# 
# JAVA14_HOME=/opt/java1.4
# JAVA15_HOME=/opt/java1.5
# 
# BOOST_VERSION=1_34_1
# BOOST_BASE=/usr/include/boost
# BOOST_FLAVOR=gcc41-mt
#
# MACOS_ARCH=-arch i386 -arch ppc -arch ppc64 -arch x86_64
# 
# WINBASE=

include $(ENV_DEF)

SHARED_MAGIC = -shared
STATIC_MAGIC = -Wl,-Bstatic 
STATIC_LIB_PREFIX = 

#Base name for the tod-agent library
AGENT_NAME = tod-agent
AGENT_NAME_SUFFIX = 

ifeq ($(PLATFORM), win32)
	#CXX = i586-mingw32msvc-g++

	LDFLAGS += -lws2_32 -Wl,--add-stdcall-alias
	CXXFLAGS14 += -I$(JAVA14_HOME)/include/ -I$(JAVA14_HOME)/include/win32/
	CXXFLAGS15 += -I$(JAVA15_HOME)/include/ -I$(JAVA15_HOME)/include/win32/

	LIBPREFIX =
	LIBEXT = dll
	STATIC_LIB_PREFIX = lib
endif

ifeq ($(PLATFORM), macos)
	SHARED_MAGIC = -dynamiclib
	STATIC_MAGIC = 
	
	CXXFLAGS += -Wno-long-double

	CXXFLAGS14 += -I$(JAVA14_HOME)/Headers/ 
	CXXFLAGS15 += -I$(JAVA15_HOME)/Headers/ 
	
	CXXFLAGS += $(MACOS_ARCH)
	LDFLAGS += $(MACOS_ARCH)

	LIBPREFIX = lib
	LIBEXT = dylib
endif

ifeq ($(PLATFORM), linux)
	CXXFLAGS14 += -I$(JAVA14_HOME)/include/ -I$(JAVA14_HOME)/include/linux/
	CXXFLAGS15 += -I$(JAVA15_HOME)/include/ -I$(JAVA15_HOME)/include/linux/
	LDFLAGS += -static-libgcc 

	LIBPREFIX = lib
	LIBEXT = so
endif

ifeq ($(PLATFORM), linux_x64)
	CXXFLAGS14 += -I$(JAVA14_HOME)/include/ -I$(JAVA14_HOME)/include/linux/
	CXXFLAGS15 += -I$(JAVA15_HOME)/include/ -I$(JAVA15_HOME)/include/linux/
	LDFLAGS += -static-libgcc 

	# To do a static version we would have to recompile the whole libc with -fPIC...
	# So it is dynamic for now
	STATIC_MAGIC = 

	AGENT_NAME_SUFFIX = _x64
	LIBPREFIX = lib
	LIBEXT = so
endif

CXXFLAGS += -O3 -fPIC
CXXFLAGS += -I$(BOOST_INCLUDES)

LDFLAGS += -fPIC

AGENT14_LIBNAME = $(LIBPREFIX)$(AGENT_NAME)14$(AGENT_NAME_SUFFIX).$(LIBEXT)
# AGENT14_IOLIBNAME = $(LIBPREFIX)$(AGENT_NAME)io14$(AGENT_NAME_SUFFIX).$(LIBEXT)
AGENT15_LIBNAME = $(LIBPREFIX)$(AGENT_NAME)15$(AGENT_NAME_SUFFIX).$(LIBEXT)

LDFLAGS_BOOST += \
	-L$(BOOST_LIBS) \
	$(STATIC_MAGIC) -l$(STATIC_LIB_PREFIX)boost_system-$(BOOST_FLAVOR)-$(BOOST_VERSION) \
	$(STATIC_MAGIC) -l$(STATIC_LIB_PREFIX)boost_thread-$(BOOST_FLAVOR)-$(BOOST_VERSION) \
	$(STATIC_MAGIC) -l$(STATIC_LIB_PREFIX)boost_filesystem-$(BOOST_FLAVOR)-$(BOOST_VERSION)

common-objects = \
	$(PLATFORM)/agent.o \
	$(PLATFORM)/utils.o \
	$(PLATFORM)/md5.o \
	$(PLATFORM)/jniutils.o \
	$(PLATFORM)/workingset.o \
	$(PLATFORM)/io.o


agent14-objects = $(common-objects) $(PLATFORM)/agentimpl14.o 
agent15-objects = $(common-objects) $(PLATFORM)/agentimpl15.o 

##############

all: agent15 agent14

agent14: dirs $(agent14-objects)
	$(CXX) $(SHARED_MAGIC) -o $(PLATFORM)/$(AGENT14_LIBNAME) $(agent14-objects) $(LDFLAGS) $(LDFLAGS_BOOST)
	cp $(PLATFORM)/$(AGENT14_LIBNAME) ../..
# 	$(CXX) $(SHARED_MAGIC) -o $(PLATFORM)/$(AGENT14_IOLIBNAME) $(PLATFORM)/io.o $(LDFLAGS) $(LDFLAGS_BOOST)
# 	cp $(PLATFORM)/$(AGENT14_IOLIBNAME) ../..

agent15: dirs $(agent15-objects)
	$(CXX) $(SHARED_MAGIC) -o $(PLATFORM)/$(AGENT15_LIBNAME) $(agent15-objects) $(LDFLAGS) $(LDFLAGS_BOOST)
	cp $(PLATFORM)/$(AGENT15_LIBNAME) ../..

workingsettest: dirs $(PLATFORM)/workingset.o $(PLATFORM)/workingsettest.o
	$(CXX) -g -O0 -o workingsettest $(PLATFORM)/workingset.o $(PLATFORM)/workingsettest.o $(LDFLAGS) $(LDFLAGS_BOOST)

dirs:
	mkdir -p $(PLATFORM)

push: push14 push15

push14: agent14
	./push.sh $(PLATFORM)/$(AGENT14_LIBNAME) $(AGENT14_LIBNAME)

push15: agent15
	./push.sh $(PLATFORM)/$(AGENT15_LIBNAME) $(AGENT15_LIBNAME)

clean:
	rm -f ../../*.so ../../*.dll ../../*.dylib
	rm -rf linux linux_x64 win32 macos

############## 

$(PLATFORM)/agent.o: agent.cpp agent.h agentimpl.h utils.h jniutils.h workingset.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(CXXFLAGS15) agent.cpp -o $(PLATFORM)/agent.o

$(PLATFORM)/agentimpl14.o: agentimpl-1.4.cpp agentimpl.h agent.h utils.h jniutils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(CXXFLAGS14) agentimpl-1.4.cpp -o $(PLATFORM)/agentimpl14.o

$(PLATFORM)/agentimpl15.o: agentimpl-1.5.cpp agentimpl.h agent.h utils.h jniutils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(CXXFLAGS15) agentimpl-1.5.cpp -o $(PLATFORM)/agentimpl15.o

$(PLATFORM)/utils.o: utils.cpp utils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(CXXFLAGS15) utils.cpp -o $(PLATFORM)/utils.o

$(PLATFORM)/jniutils.o: jniutils.cpp jniutils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(CXXFLAGS15) jniutils.cpp -o $(PLATFORM)/jniutils.o

$(PLATFORM)/md5.o: md5.cpp md5.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) md5.cpp -o $(PLATFORM)/md5.o

$(PLATFORM)/workingset.o: workingset.cpp workingset.h utils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(CXXFLAGS15) workingset.cpp -o $(PLATFORM)/workingset.o

$(PLATFORM)/workingsettest.o: workingsettest.cpp workingset.h utils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) workingsettest.cpp -o $(PLATFORM)/workingsettest.o

$(PLATFORM)/io.o: io.cpp
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(CXXFLAGS15) io.cpp -o $(PLATFORM)/io.o
