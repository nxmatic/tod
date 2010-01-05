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
package tod.experiments;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JFrame;

import tod.core.config.TODConfig;
import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.IEventPredicate;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.browser.IVariablesInspector;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.AbstractSession;
import tod.core.session.ISession;
import tod.gui.IExtensionPoints;
import tod.gui.IGUIManager;
import tod.gui.MinerUI;
import tod.gui.activities.structure.StructureSeed;
import tod.impl.bci.asm2.ASMInstrumenter2;
import tod.impl.database.IBidiIterator;
import tod.impl.database.structure.standard.StructureDatabase;
import zz.utils.Utils;
import zz.utils.properties.IRWProperty;

public class ABCTags
{
	public static void main(String[] args) throws Exception
	{
		TODConfig theConfig = new TODConfig();
		StructureDatabase theStructureDatabase = StructureDatabase.create(theConfig);
		theConfig.set(TODConfig.SCOPE_TRACE_FILTER, "[+xys]");
		ASMInstrumenter2 theInstrumenter = new ASMInstrumenter2(theConfig, theStructureDatabase);
		
		byte[] theBytecode = Utils.readInputStream_byte(new FileInputStream(args[0]));
		theInstrumenter.instrumentClass("x", theBytecode, false);

		ILogBrowser theLogBrowser = new MyLogBrowser(theStructureDatabase);
		
		MinerUI theUI = new MyMinerUI(theConfig, theLogBrowser);
		
		JFrame frame = new JFrame("ABC test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(theUI);
		frame.setVisible(true);
		frame.setSize(1300, 500);
		
		StructureSeed theSeed = new StructureSeed(theLogBrowser);
		theUI.openSeed(theSeed, false);
		
		System.out.println("ready");
	}
	
	private static class MyMinerUI extends MinerUI
	{
		public MyMinerUI(TODConfig aConfig, ILogBrowser aLogBrowser)
		{
			setSession(new DummySession(this, aConfig, URI.create("dummy:dummy"), aLogBrowser));
		}
		
		public void gotoSource(ILocationInfo aLocation)
		{
			throw new UnsupportedOperationException();
		}

		public void gotoSource(ProbeInfo aProbe)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T showDialog(DialogType<T> aDialog)
		{
			throw new UnsupportedOperationException();
		}

		public void showPostIt(JComponent aComponent, Dimension aSize)
		{
			throw new UnsupportedOperationException();
		}

		public IExtensionPoints getExtensionPoints()
		{
			return null;
		}
	}
	
	private static class DummySession extends AbstractSession
	{
		private final ILogBrowser itsLogBrowser;

		public DummySession(IGUIManager aGUIManager, TODConfig aConfig, URI aUri, ILogBrowser aLogBrowser)
		{
			super(aGUIManager, aUri, aConfig);
			itsLogBrowser = aLogBrowser;
		}

		public JComponent createConsole()
		{
			throw new UnsupportedOperationException();
		}

		public void disconnect()
		{
			throw new UnsupportedOperationException();
		}

		public void flush()
		{
			throw new UnsupportedOperationException();
		}

		public ILogBrowser getLogBrowser()
		{
			return itsLogBrowser;
		}

		public boolean isAlive()
		{
			throw new UnsupportedOperationException();
		}

		public IRWProperty<Boolean> pCaptureEnabled()
		{
			throw new UnsupportedOperationException();
		}
	}
	
	private static class MyLogBrowser implements ILogBrowser
	{
		private final IStructureDatabase itsStructureDatabase;
		
		public MyLogBrowser(IStructureDatabase aStructureDatabase)
		{
			itsStructureDatabase = aStructureDatabase;
		}

		public ISession getSession()
		{
			throw new UnsupportedOperationException();
		}

		public IStructureDatabase getStructureDatabase()
		{
			return itsStructureDatabase;
		}

		public void clear()
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createAdviceSourceIdFilter(int aAdviceSourceId)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createArgumentFilter(ObjectId aId)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createArgumentFilter(ObjectId aId, int aPosition)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createArrayWriteFilter()
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createArrayWriteFilter(int aIndex)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createBehaviorCallFilter()
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createBehaviorCallFilter(IBehaviorInfo aBehavior)
		{
			throw new UnsupportedOperationException();
		}

		public IEventBrowser createBrowser()
		{
			throw new UnsupportedOperationException();
		}

		public IEventBrowser createBrowser(IEventFilter aFilter)
		{
			throw new UnsupportedOperationException();
		}

		public IObjectInspector createClassInspector(IClassInfo aClass)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createDepthFilter(int aDepth)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createEventFilter(ILogEvent aEvent)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createExceptionGeneratedFilter()
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createFieldFilter(IFieldInfo aField)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createFieldWriteFilter()
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createHostFilter(IHostInfo aHost)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createInstantiationFilter(ObjectId aObjectId)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createInstantiationsFilter()
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createInstantiationsFilter(ITypeInfo aType)
		{
			throw new UnsupportedOperationException();
		}

		public ICompoundFilter createIntersectionFilter(IEventFilter... aFilters)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createOperationLocationFilter(IBehaviorInfo aBehavior, int aBytecodeIndex)
		{
			throw new UnsupportedOperationException();
		}
		
		public IEventFilter createOperationLocationFilter(ProbeInfo aProbe)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createOperationLocationFilter(IBehaviorInfo aBehavior)
		{
			throw new UnsupportedOperationException();
		}
		
		public IEventFilter createObjectFilter(ObjectId aId)
		{
			throw new UnsupportedOperationException();
		}

		public IObjectInspector createObjectInspector(ObjectId aObjectId)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createResultFilter(ObjectId aId)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createTargetFilter(ObjectId aId)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createThreadFilter(IThreadInfo aThread)
		{
			throw new UnsupportedOperationException();
		}

		public ICompoundFilter createUnionFilter(IEventFilter... aFilters)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createValueFilter(ObjectId aId)
		{
			throw new UnsupportedOperationException();
		}

		public IVariablesInspector createVariablesInspector(IBehaviorCallEvent aEvent)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createVariableWriteFilter()
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createVariableWriteFilter(LocalVariableInfo aVariable)
		{
			throw new UnsupportedOperationException();
		}

		public <O> O exec(Query<O> aQuery)
		{
			throw new UnsupportedOperationException();
		}

		public IParentEvent getCFlowRoot(IThreadInfo aThread)
		{
			throw new UnsupportedOperationException();
		}

		public ILogEvent getEvent(ExternalPointer aPointer)
		{
			throw new UnsupportedOperationException();
		}

		public long getEventsCount()
		{
			return 0;
		}

		public long getDroppedEventsCount()
		{
			return 0;
		}

		public long getFirstTimestamp()
		{
			return 0;
		}

		public IHostInfo getHost(String aName)
		{
			throw new UnsupportedOperationException();
		}

		public Iterable<IHostInfo> getHosts()
		{
			throw new UnsupportedOperationException();
		}

		public long getLastTimestamp()
		{
			return 0;
		}

		public Object getRegistered(ObjectId aId)
		{
			throw new UnsupportedOperationException();
		}

		public Iterable<IThreadInfo> getThreads()
		{
			return Collections.EMPTY_LIST;
		}

		public IBidiIterator<Long> searchStrings(String aSearchText)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createAdviceCFlowFilter(int aAdviceSourceId)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createRoleFilter(BytecodeRole aRole)
		{
			throw new UnsupportedOperationException();
		}

		public IEventFilter createPredicateFilter(
				IEventPredicate aPredicate,
				IEventFilter aBaseFilter)
		{
			throw new UnsupportedOperationException();
		}
		
		public IEventFilter createBehaviorCallFilter(
				IBehaviorInfo aCalledBehavior,
				IBehaviorInfo aExecutedBehavior)
		{
			throw new UnsupportedOperationException();
		}

		public long[] getEventCounts(IBehaviorInfo[] aBehaviors)
		{
			throw new UnsupportedOperationException();
		}
		
		public long[] getEventCounts(IClassInfo[] aClasses)
		{
			throw new UnsupportedOperationException();
		}
	}
}
