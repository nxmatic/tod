#! /usr/bin/python

import pysvn
import os
import os.path
import sys
import imp
import shutil
import re
import datetime

versions = None
modules = None

class SCMModule:
	"""Represents a Source Code Management module, ie. typically 
	a source code module obtained from CVS or SVN"""
	
	def __init__(self, name, url, lastRev, checkout):
		self.name = name
		self.url = url
		self.path = 'checkouts/' + self.name
		if checkout: self.rev = self.checkout()
		else: self.rev = self.update()
		self.lastRev = lastRev
	
	def checkout():
		pass

	def log(self):
		"""
		Returns the commit messages of this module
		between the last revision and the current one.
		"""
		pass
		
class SVNModule(SCMModule):
	client = pysvn.Client()
	
	def checkout(self):
		print('Checking out '+self.name+'...')
		
		self.client.checkout(self.url, self.path)
		info = self.client.info(self.path)
		rev = info.revision.number
		print('Checked out rev. %d' % rev)
		
		return str(rev)
	
	def update(self):
		print('Updating '+self.name+'...')
		
		self.client.update(self.path)
		info = self.client.info(self.path)
		rev = info.revision.number
		print('Updated to rev. %d' % rev)
		
		return str(rev)
	
	def log(self):
		if self.lastRev == self.rev: return ''
		startRev = str(int(self.lastRev) + 1)
		endRev = self.rev
		
		print('Retrieving logs of %s: %s-%s' % (self.name, startRev, endRev))
		logs = self.client.log(
			self.path, 
			pysvn.Revision(pysvn.opt_revision_kind.number, startRev), 
			pysvn.Revision(pysvn.opt_revision_kind.number, endRev))
		
		result = ''
		
		for log in logs: 
			lines = log.message.splitlines()
			for line in lines:
				line = line.strip()
				if (len(line) == 0): continue
				result += line + '\n'
					
		return result
		
def countStars(s):
	count = 0
	started = False
	
	for c in s:
		if c == '*':
			started = True
			count+=1
		elif not started:
			if c.isspace(): continue
			else: break
		elif started: break
	
	return count
	
def useSVN_CO(name, url):
	return useSVN(name, url, True)

def useSVN_UP(name, url):
	return useSVN(name, url, False)

def useSVN(name, url, checkout):
	lastRev = 0
	if name in versions: lastRev = versions[name]
	module = SVNModule(name, url, lastRev, checkout)
	modules[name] = module
	return module
	
def setEclipsePluginVersion(moduleName, version):
	module = getModule(moduleName)
	dir = 'checkouts/'+module.name
	f = open(dir + '/plugin.xml', 'r+')
	text = f.read()
	text = re.sub(r'(<plugin[^>]*version=")[^"]+"', '\g<1>' + version + '"', text, 1)
	f.seek(0)
	f.write(text)
	f.truncate()
	
def antBuild(moduleName, buildFileName, targetName):
	print '***********************************************************'
	print 'Building ' + moduleName + ' (' + targetName + ')...'
	module = getModule(moduleName)
	cwd = os.getcwd()
	dir = 'checkouts/'+module.name
	os.chdir(dir)
	ret = os.system('ant -f '+buildFileName+' '+targetName)
	os.chdir(cwd)
	
	if (ret != 0): raise Exception('Build failed')
	print ''
	
def mkcleandir(path):	
	if os.path.exists(path):
		shutil.rmtree(path)
	
	os.mkdir(path)
	
def readInt(fileName):
	if not os.path.exists(fileName):
		return 0
	
	f = open(fileName, 'r')
	i = int(f.read())
	f.close()
	
	return i

def getModule(moduleName):
	return modules[moduleName]

def loadProps(fileName):
	props = dict()
	
	if (os.path.exists(fileName)):
		f = open(fileName, 'r')
		lines = f.readlines()
		f.close()
		
		for line in lines:
			(key, value) = line.split(':')
			props[key.strip()] = value.strip()
		
	return props
	
def writeProps(fileName, props):
	f = open(fileName, 'w+')
	for (key, value) in props.iteritems():
		f.write(key + ':' + value + '\n')
	f.close()
	
def stripLog(log, detailLevel):
	"""
	Strips the logs from lines that have a detail level 
	(number of prefix stars) lower than the specified detail level.
	"""
	result = ''
	for line in log.splitlines():
		if countStars(line) >= detailLevel:
			result += line + '\n'

	return result


def build(version, checkout):
	"""
	Performs the full build.
	checkout: if true, a full checkout is performed, otherwise update only.
	"""
	global modules
	global versions
	
	modules = dict()	
	
	if (checkout): mkcleandir('checkouts')
	mkcleandir('release')
	
	versions = loadProps('versions.last')
	
	# Setup target module
	tm = imp.load_source('target', 'release.py')
	tm.useSVN = (useSVN_UP, useSVN_CO)[checkout]
	tm.antBuild = antBuild
	tm.setEclipsePluginVersion = setEclipsePluginVersion
	tm.getModule = getModule
	tm.mkcleandir = mkcleandir
	
	tm.release(version)
	
	# Write logs & update versions properties
	lastVersion = '0'
	if '_version_' in versions: lastVersion = versions['_version_']
	
	versions['_version_'] = version
	fChanges = open('release/changes_' + version + '.txt', 'w+')
	fLogs = open('release/logs_' + version + '.txt', 'w+')

	fChanges.write(datetime.datetime.today().strftime("%Y-%m-%d %H:%M:%S\n"))
	fChanges.write('Changes between ' + lastVersion + ' and ' + version + '\n\n')
	fLogs.write('Commit logs between ' + lastVersion + ' and ' + version + '\n\n')
	
	for module in modules.itervalues():
		log = module.log()
		versions[module.name] = module.rev
		
		header = 'Changes for %s (rev. %s to %s)' % (module.name, module.lastRev, module.rev)
		
		changesText = stripLog(log, 1)
		if (len(changesText.strip()) > 0): 
			fChanges.write(header + '\n')
			fChanges.write('-'*len(header) + '\n')
			fChanges.write(changesText.encode('utf-8'))
			fChanges.write('\n')
			
		logsText = stripLog(log, 0)
		if (len(logsText.strip()) > 0): 
			fLogs.write(header + '\n')
			fLogs.write('-'*len(header) + '\n')
			fLogs.write(logsText.encode('utf-8'))
			fLogs.write('\n')
		
	fChanges.close()
	fLogs.close()
	
	writeProps('versions.current', versions)
	
	print('Done.')


command = sys.argv[1]

if command == '--build' or command == '-b':
	version = sys.argv[2]
	build(version, False)
	
elif command == '--rebuild' or command == '-r':
	version = sys.argv[2]
	build(version, True)
	
elif command == '--commit' or command == '-c':
	
	os.rename('versions.current', 'versions.last')
	versions = loadProps('versions.last')
	version = versions['_version_']

	client = pysvn.Client()
	client.checkin('.', 'Version ' + version + '\n')
	
	print 'Done.'

else:
	print 'Unknown command: ' + command