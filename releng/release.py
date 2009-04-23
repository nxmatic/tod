# -*- coding: utf-8 -*-
import os
import sys
import shutil
import urllib

AGENT_LIBS_URL = "http://pleiad.cl/files/tod/tod-agent/"

def release(version):
	zzuMod = useSVN('zz.utils', 'http://pleiad.cl/svn/zz/devel/zz.utils')
	zzeuMod = useSVN('zz.eclipse.utils', 'http://pleiad.cl/svn/zz/devel/zz.eclipse.utils/')
	zzjiMod = useSVN('zz.jinterp', 'http://pleiad.cl/svn/zz/devel/zz.jinterp')
	todMod = useSVN('TOD', 'http://pleiad.cl/svn/tod/trunk/')
	
	print '############################################################'
	print 'Checking and downloading native library builds...'
	print '############################################################'
	print ''

	cwd = os.getcwd()
	os.chdir(todMod.path + '/agent/src/native')

	libs = ["libtod-agent15.so", "libtod-agent15_x64.so", "libtod-agent15.dylib", "tod-agent15.dll", "libtod-agent14.so", "libtod-agent14_x64.so", "tod-agent14.dll"]
	missingLibs = ""

	for lib in libs:
		# Get local signature
		lsig = os.popen('./svn-sig.sh').read()

		# Get remote signature
		rsig = urllib.urlopen(AGENT_LIBS_URL + lib + "-sig.txt").read()

		if lsig != rsig:
			print "SVN revisions do not match for " + lib
			missingLibs += lib + " "
			#sys.exit(-1)
		else:
			urllib.urlretrieve(AGENT_LIBS_URL + lib, "../../" + lib)

	os.chdir(cwd)
	print "Done."
	#sys.exit(0)
	
	shutil.copy('ant.settings', todMod.path + '/tod.plugin/')

	print '############################################################'
	print 'Cleaning...'
	print '############################################################'
	print ''

	antBuild(todMod.path + '/core', 'build.xml', 'clean')
	antBuild(todMod.path + '/agent', 'build.xml', 'clean')
	antBuild(todMod.path + '/dbgrid', 'build.xml', 'clean')
	antBuild(todMod.path + '/evdb1', 'build.xml', 'clean')
	antBuild(todMod.path + '/evdbng', 'build.xml', 'clean')
	#antBuild(todMod.path + '/pytod-db', 'build.xml', 'clean')
	antBuild(zzuMod.path, 'build.xml', 'clean')
	antBuild(zzeuMod.path, 'build-plugin.xml', 'clean')
	antBuild(zzjiMod.path, 'build.xml', 'clean')
	antBuild(todMod.path + '/tod.plugin', 'build-plugin.xml', 'clean')
	antBuild(todMod.path + '/tod.plugin.ajdt', 'build-plugin.xml', 'clean')
	antBuild(todMod.path + '/tod.plugin.wst', 'build-plugin.xml', 'clean')
	#antBuild(todMod.path + '/tod.plugin.pytod', 'build-plugin.xml', 'clean')
	
	print '############################################################'
	print 'Building plugins and dependencies...'
	print '############################################################'
	print ''

	antBuild(zzuMod.path, 'build.xml', 'jar')
	antBuild(zzeuMod.path, 'build-plugin.xml', 'plugin')
	antBuild(zzjiMod.path, 'build.xml', 'jar')
	antBuild(todMod.path + '/agent', 'build.xml', 'jar')
	antBuild(todMod.path + '/core', 'build.xml', 'jar')
	antBuild(todMod.path + '/dbgrid', 'build.xml', 'jar')
	antBuild(todMod.path + '/evdb1', 'build.xml', 'jar')
	antBuild(todMod.path + '/evdbng', 'build.xml', 'jar')
	#antBuild(todMod.path + '/pytod-db', 'build.xml', 'jar')

	setEclipsePluginVersion(todMod.path + '/tod.plugin', version)
	antBuild(todMod.path + '/tod.plugin', 'build-plugin.xml', 'plugin')
	
	setEclipsePluginVersion(todMod.path + '/tod.plugin.ajdt', version)
	antBuild(todMod.path + '/tod.plugin.ajdt', 'build-plugin.xml', 'plugin')
	
	setEclipsePluginVersion(todMod.path + '/tod.plugin.wst', version)
	antBuild(todMod.path + '/tod.plugin.wst', 'build-plugin.xml', 'plugin')
	
	#setEclipsePluginVersion(todMod.path + '/tod.plugin.pytod', version)
	#antBuild(todMod.path + '/tod.plugin.pytod', 'build-plugin.xml', 'plugin')
	
	print '############################################################'
	print 'Packaging plugins...'
	print '############################################################'
	print ''

	os.mkdir('release/plugins')
	shutil.copytree(todMod.path + '/tod.plugin/build/tod.plugin', 'release/plugins/tod.plugin_' + version)
	shutil.copytree(todMod.path + '/tod.plugin.ajdt/build/tod.plugin.ajdt', 'release/plugins/tod.plugin.ajdt_' + version)
	shutil.copytree(todMod.path + '/tod.plugin.wst/build/tod.plugin.wst', 'release/plugins/tod.plugin.wst_' + version)
	#shutil.copytree(todMod.path + '/tod.plugin.pytod/build/tod.plugin.pytod', 'release/plugins/tod.plugin.pytod_' + version)
	shutil.copytree(zzeuMod.path + '/build/zz.eclipse.utils', 'release/plugins/zz.eclipse.utils_1.0.0')

	os.chdir('release')
	ret = os.system('zip -rm tod.plugin_' + version + '.zip plugins')
	os.chdir(cwd)
	
	print '############################################################'
	print 'Packaging standalone database...'
	print '############################################################'
	print ''

	antBuild(todMod.path + '/dbgrid', 'build.xml', 'release-db')
	shutil.copy(todMod.path + '/dbgrid/build/tod-db.zip', 'release/tod-db_' + version + '.zip')
	shutil.copy(todMod.path + '/dbgrid/build/tod-db.tar.gz', 'release/tod-db_' + version + '.tar.gz')
	
	print '############################################################'
	print 'Done.'
	print '############################################################'
	print ''
	
	print "Missing libs: " + missingLibs

