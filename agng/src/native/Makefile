SHARED_MAGIC = -shared
STATIC_MAGIC = -Wl,-Bstatic
OBF = obf
OBF_PLATFORM = $(OBF)/$(PLATFORM)

ifeq ($(PLATFORM), win32)
	WINBASE = ./winbase
	CXX = i586-mingw32msvc-g++

	LDFLAGS += -lws2_32 -Wl,--add-stdcall-alias

	JAVA_HOME = $(WINBASE)/java
	JNI_INC = -I $(JAVA_HOME)/include/ -I $(JAVA_HOME)/include/win32/

	LIBPREFIX =
	LIBEXT = dll
endif

ifeq ($(PLATFORM), macos)
	SHARED_MAGIC = -dynamiclib
	STATIC_MAGIC = 
	
	CXXFLAGS += -Wno-long-double

	JNI_INC = -I $(JAVA_HOME)/Headers/ 
	CXXFLAGS += -arch ppc -arch i386 
	#CXXFLAGS += -arch ppc64 -arch x86_64

	LDFLAGS += -arch ppc -arch i386 
	#LDFLAGS += -arch ppc64 -arch x86_64

	LIBPREFIX = lib
	LIBEXT = dylib
endif

ifeq ($(PLATFORM), linux)
	JNI_INC = -I $(JAVA_HOME)/include/ -I $(JAVA_HOME)/include/linux/

	LDFLAGS += -static-libgcc 
	#LDFLAGS += -lstdc++

	LIBPREFIX = lib
	LIBEXT = so
endif

CXXFLAGS += -O2      ### flag optimization  03 hard

tod-agent-objects = \
	$(PLATFORM)/tod-agent-skel.o \
	$(PLATFORM)/jniutils.o \
	
obf_tod-agent-objects = \
	$(OBF_PLATFORM)/tod-agent-skel.o \
	$(OBF_PLATFORM)/jniutils.o \

##############

all: tod-agent

tod-agent: dirs $(tod-agent-objects) $(obf_tod-agent-objects)
	$(CXX) $(SHARED_MAGIC) $(CXXFLAGS) -o $(PLATFORM)/$(LIBPREFIX)tod-agent.$(LIBEXT) $(tod-agent-objects) $(LDFLAGS)
	$(CXX) $(SHARED_MAGIC) $(CXXFLAGS) -o $(OBF_PLATFORM)/$(LIBPREFIX)tod-agent.$(LIBEXT) $(obf_tod-agent-objects) $(LDFLAGS)
	#cp $(PLATFORM)/$(LIBPREFIX)tod-agent.$(LIBEXT) ../..

dirs:
	mkdir -p $(PLATFORM)
	mkdir -p $(OBF_PLATFORM)

clean:
	rm -f ../../libtod-agent.so ../../tod-agent.dll 
	rm -rf linux win32 macos obf

############## 

$(PLATFORM)/tod-agent-skel.o: tod-agent-skel.cpp tod-agent-skel.h  jniutils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(JNI_INC) tod-agent-skel.cpp -o $(PLATFORM)/tod-agent-skel.o

$(PLATFORM)/jniutils.o: jniutils.cpp jniutils.h
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(JNI_INC) jniutils.cpp -o $(PLATFORM)/jniutils.o

$(OBF_PLATFORM)/tod-agent-skel.o: $(PLATFORM)/tod-agent-skel.o $(OBF_PLATFORM)/jniutils.o mkObfuscatedAgent.pl
	cp tod-agent-skel.h $(OBF)/.
	cp tod-agent-skel.cpp $(OBF)/.
	./mkObfuscatedAgent.pl
	$(CXX) $(SHARED_MAGIC) -c $(CXXFLAGS) $(JNI_INC) $(OBF)/tod-agent-skel.cpp -o $(OBF_PLATFORM)/tod-agent-skel.o

$(OBF_PLATFORM)/jniutils.o: $(PLATFORM)/jniutils.o
	cp jniutils.h $(OBF)/.
	cp jniutils.cpp $(OBF)/.
	cp $(PLATFORM)/jniutils.o $(OBF_PLATFORM)/.	