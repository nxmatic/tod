#! /bin/sh

PATH=$JAVA_HOME/bin/:$PATH
# AGENT=../agng/libtod-agent.so
AGENT=../agent/libtod-agent15.so
#CLASSPATH=./bin:../zz.utils/bin

HOST=localhost
#HOST=syntagma.dim.uchile.cl
#HOST=padme.dcc.uchile.cl
#HOST=cluster


VMARGS=''
VMARGS="$VMARGS -agentpath:$AGENT"
#VMARGS="$VMARGS -noverify"
VMARGS="$VMARGS -Dcollector-host=$HOST -Dcollector-port=8058 -Dclient-name=tod-1"
#VMARGS="$VMARGS -Dcollector-type=socket"
VMARGS="$VMARGS -Xbootclasspath/p:../agent/bin" 
VMARGS="$VMARGS -ea" 
VMARGS="$VMARGS -server" 
VMARGS="$VMARGS -Xmx256m" 
VMARGS="$VMARGS -XX:MaxPermSize=128m"
VMARGS="$VMARGS -Dagent-verbose=0"
#VMARGS="$VMARGS -Xcheck:jni"
#VMARGS="$VMARGS -Dagent-cache-path=/home/gpothier/tmp/tod"
#VMARGS="$VMARGS -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
#VMARGS="$VMARGS -agentlib:hprof=cpu=samples,depth=3"
#VMARGS="$VMARGS -Dcom.sun.management.jmxremote=true"
#VMARGS="$VMARGS -agentpath:/home/gpothier/tmp/yjp-9.0.5/bin/linux-x86-32/libyjpagent.so"
#VMARGS="$VMARGS -verbose:jni"

#valgrind --leak-check=yes --smc-check=all \
#java $VMARGS -cp ./bin dummy.BurnTest
~/apps/eclipse-3.5.2/eclipse -data ~/devel/ws-todtest -vm ~/apps/java/jdk1.6.0_16/bin/java -consolelog -vmargs $VMARGS

# /home/gpothier/apps/java/jdk1.6.0_01/bin/java $VMARGS -jar /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar -os linux -ws gtk -arch x86 -showsplash -launcher /home/gpothier/apps/eclipse-3.3.1.1/eclipse -name Eclipse --launcher.library /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher.gtk.linux.x86_1.0.2.R331_v20071019/eclipse_1021.so -startup /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar -exitdata 2d88001 -data ~/eclipse/ws-tod -consolelog -vm /home/gpothier/apps/java/jdk1.5.0_08/bin/java -vmargs -Xmx256m -XX:MaxPermSize=128m -jar /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar

# echo "handle SIGSEGV nostop noprint
# set args $VMARGS -jar /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar -os linux -ws gtk -arch x86 -showsplash -launcher /home/gpothier/apps/eclipse-3.3.1.1/eclipse -name Eclipse --launcher.library /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher.gtk.linux.x86_1.0.2.R331_v20071019/eclipse_1021.so -startup /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar -exitdata 2d88001 -data ~/eclipse/ws-tod -consolelog -vm /home/gpothier/apps/java/jdk1.5.0_08/bin/java -vmargs -Xmx256m -XX:MaxPermSize=128m -jar /home/gpothier/apps/eclipse-3.3.1.1/plugins/org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar" > gdb.cmd
# gdb -x gdb.cmd /home/gpothier/apps/java/jdk1.5.0_08/bin/java 
#java $VMARGS -cp ./bin calls.Main

#java $VMARGS -cp ../../runtime-EclipseApplication2/AspectJTODTest/taggedbin foo.Main

#java $VMARGS -cp ./bin tod.bench.overhead.Dummy

#java $VMARGS -cp ./bin testclinit.A

#java $VMARGS -cp "../../runtime-EclipseApplication(1)/TODTest/bin/" tod.demo.format.A

#java $VMARGS -cp "../../runtime-EclipseApplication(1)/TODTest/bin:lib/zz.utils.jar" imageviewer3/ImageViewer
#Machines:
# ireul
# arael
# bardiel
# naud2
# leliel
# pilmaiquen
