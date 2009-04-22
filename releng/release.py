# -*- coding: utf-8 -*-
import os
import sys
import shutil
import urllib

AGENT_LIBS_URL = "http://pleiad.dcc.uchile.cl/files/tod/tod-agent/"

def release(version):
	zzuMod = useSVN('zz.utils', 'http://pleiad.dcc.uchile.cl/svn/zz/devel/zz.utils')
	zzeuMod = useSVN('zz.eclipse.utils', 'http://pleiad.dcc.uchile.cl/svn/zz/devel/zz.eclipse.utils/')
	zzji = useSVN('zz.jinterp', 'http://pleiad.dcc.uchile.cl/svn/zz/devel/zz.jinterp')

	tpMod = useSVN('tod.plugin', 'http://pleiad.dcc.uchile.cl/svn/tod/tod.plugin/trunk/')
	tpaMod = useSVN('tod.plugin.ajdt', 'http://pleiad.dcc.uchile.cl/svn/tod/tod.plugin.ajdt/')
	tpwMod = useSVN('tod.plugin.wst', 'http://pleiad.dcc.uchile.cl/svn/tod/tod.plugin.wst/trunk')
	#tppMod = useSVN('tod.plugin.pytod', 'http://pleiad.dcc.uchile.cl/svn/tod/tod.plugin.pytod/trunk')
	todMod = useSVN('TOD', 'http://pleiad.dcc.uchile.cl/svn/tod/core/trunk/')
	taMod = useSVN('TOD-agent', 'http://pleiad.dcc.uchile.cl/svn/tod/agent/trunk/')
	toddbgridMod = useSVN('TOD-dbgrid', 'http://pleiad.dcc.uchile.cl/svn/tod/dbgrid/trunk/')
	todevdb1Mod = useSVN('TOD-evdb1', 'http://pleiad.dcc.uchile.cl/svn/tod/evdb1/trunk/')
	todevdbngMod = useSVN('TOD-evdbng', 'http://pleiad.dcc.uchile.cl/svn/tod/evdbng/trunk/')

	#pytodDbMod = useSVN('TOD-pytod-db', 'http://pleiad.dcc.uchile.cl/svn/tod/pytod/trunk/java-proyect/')
	#pytodCoreMod = useSVN('python-project', 'http://pleiad.dcc.uchile.cl/svn/tod/pytod/trunk/python-project/')

	
	print '############################################################'
	print 'Checking and downloading native library builds...'
	print '############################################################'
	print ''

	cwd = os.getcwd()
	os.chdir(taMod.path + '/src/native')

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
	
	shutil.copy('ant.settings', tpMod.path)

	print '############################################################'
	print 'Cleaning...'
	print '############################################################'
	print ''

	antBuild('TOD', 'build.xml', 'clean')
	antBuild('TOD-agent', 'build.xml', 'clean')
	antBuild('TOD-dbgrid', 'build.xml', 'clean')
	antBuild('TOD-evdb1', 'build.xml', 'clean')
	antBuild('TOD-evdbng', 'build.xml', 'clean')
	#antBuild('TOD-pytod-db', 'build.xml', 'clean')
	antBuild('zz.utils', 'build.xml', 'clean')
	antBuild('zz.eclipse.utils', 'build-plugin.xml', 'clean')
	antBuild('zz.jinterp', 'build.xml', 'clean')
	antBuild('tod.plugin', 'build-plugin.xml', 'clean')
	antBuild('tod.plugin.ajdt', 'build-plugin.xml', 'clean')
	antBuild('tod.plugin.wst', 'build-plugin.xml', 'clean')
	#antBuild('tod.plugin.pytod', 'build-plugin.xml', 'clean')
	
	print '############################################################'
	print 'Building plugins and dependencies...'
	print '############################################################'
	print ''

	antBuild('zz.utils', 'build.xml', 'jar')
	antBuild('zz.eclipse.utils', 'build-plugin.xml', 'plugin')
	antBuild('zz.jinterp', 'build.xml', 'jar')
	antBuild('TOD-agent', 'build.xml', 'jar')
	antBuild('TOD', 'build.xml', 'jar')
	antBuild('TOD-dbgrid', 'build.xml', 'jar')
	antBuild('TOD-evdb1', 'build.xml', 'jar')
	antBuild('TOD-evdbng', 'build.xml', 'jar')
	#antBuild('TOD-pytod-db', 'build.xml', 'jar')

	setEclipsePluginVersion('tod.plugin', version)
	antBuild('tod.plugin', 'build-plugin.xml', 'plugin')
	
	setEclipsePluginVersion('tod.plugin.ajdt', version)
	antBuild('tod.plugin.ajdt', 'build-plugin.xml', 'plugin')
	
	setEclipsePluginVersion('tod.plugin.wst', version)
	antBuild('tod.plugin.wst', 'build-plugin.xml', 'plugin')
	
	#setEclipsePluginVersion('tod.plugin.pytod', version)
	#antBuild('tod.plugin.pytod', 'build-plugin.xml', 'plugin')
	
	print '############################################################'
	print 'Packaging plugins...'
	print '############################################################'
	print ''

	os.mkdir('release/plugins')
	shutil.copytree(tpMod.path + '/build/tod.plugin', 'release/plugins/tod.plugin_' + version)
	shutil.copytree(tpaMod.path + '/build/tod.plugin.ajdt', 'release/plugins/tod.plugin.ajdt_' + version)
	shutil.copytree(tpwMod.path + '/build/tod.plugin.wst', 'release/plugins/tod.plugin.wst_' + version)
	#shutil.copytree(tppMod.path + '/build/tod.plugin.pytod', 'release/plugins/tod.plugin.pytod_' + version)
	shutil.copytree(zzeuMod.path + '/build/zz.eclipse.utils', 'release/plugins/zz.eclipse.utils_1.0.0')

	os.chdir('release')
	ret = os.system('zip -rm tod.plugin_' + version + '.zip plugins')
	os.chdir(cwd)
	
	print '############################################################'
	print 'Packaging standalone database...'
	print '############################################################'
	print ''

	antBuild('TOD-dbgrid', 'build.xml', 'release-db')
	shutil.copy(toddbgridMod.path + '/build/tod-db.zip', 'release/tod-db_' + version + '.zip')
	shutil.copy(toddbgridMod.path + '/build/tod-db.tar.gz', 'release/tod-db_' + version + '.tar.gz')
	
	print '############################################################'
        print 'Done.'
        print '############################################################'
        print ''
	
	print "Missing libs: " + missingLibs

				
