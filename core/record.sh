#! /bin/sh

PATH=$JAVA_HOME/bin/:$PATH
WS="../.."
ECLIPSE_HOME="/home/gpothier/apps/eclipse-3.5.2"

VMARGS=''
VMARGS="$VMARGS -server "
VMARGS="$VMARGS -Xmx192m "
VMARGS="$VMARGS -DXtrace-filter=[+plop.**] "
VMARGS="$VMARGS -Dinstrumenter-log=false "
VMARGS="$VMARGS -Dfile.encoding=UTF-8 "
#VMARGS="$VMARGS -Xbootclasspath:/usr/lib/jvm/java-6-openjdk/jre/lib/resources.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/rt.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/jsse.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/jce.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/charsets.jar:/usr/lib/jvm/java-6-openjdk/jre/lib/rhino.jar"
VMARGS="$VMARGS -classpath $WS/TOD/core/bin:$WS/zz.utils/bin:$WS/TOD/agent/bin:$WS/TOD/core/lib/trove-2.1.0.jar:$WS/asm3-svn/output/eclipse:$WS/TOD/core/generated"

java $VMARGS  tod.impl.database.Recorder
