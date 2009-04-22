#!/bin/sh
# This script creates the symbolic links that permit to run the database
# scripts from the checkout directory

ln -s ../../TOD/lib/aspectjrt.jar ../lib/aspectjrt.jar
ln -s ../../TOD-agent/build/tod-agent15.jar ../lib/tod-agent15.jar
ln -s ../../TOD/build/tod-debugger.jar ../lib/tod-debugger.jar
ln -s ../../TOD-evdb1/build/tod-evdb1.jar ../lib/tod-evdb1.jar
ln -s ../../TOD-evdbng/build/tod-evdbng.jar ../lib/tod-evdbng.jar
ln -s ../../zz.utils/build/zz.utils.jar ../lib/zz.utils.jar

