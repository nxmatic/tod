/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.local.event;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.IParentEvent;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;

/**
 * Serves as a fake event containing control flow roots.
 * This version uses a {@link IEventBrowser} to access the children.
 * @see ListRootEvent
 * @author gpothier
 */
public class BrowserRootEvent implements IParentEvent
{
	private final ILogBrowser itsLogBrowser;
	private final IThreadInfo itsThread;

	public BrowserRootEvent(ILogBrowser aLogBrowser, IThreadInfo aThread)
	{
		itsLogBrowser = aLogBrowser;
		itsThread = aThread;
	}

	public IEventBrowser getChildrenBrowser()
	{
		IEventFilter theFilter = itsLogBrowser.createIntersectionFilter(
				itsLogBrowser.createThreadFilter(itsThread),
				itsLogBrowser.createDepthFilter(1));

		return itsLogBrowser.createBrowser(theFilter);
	}
	
	public IEventBrowser getCFlowBrowser()
	{
		IEventFilter theFilter = itsLogBrowser.createThreadFilter(itsThread);
		return itsLogBrowser.createBrowser(theFilter);
	}

	public long getFirstTimestamp()
	{
		return itsLogBrowser.getFirstTimestamp();
	}

	public long getLastTimestamp()
	{
		return itsLogBrowser.getLastTimestamp();
	}

	public boolean hasRealChildren()
	{
		return true;
	}

	public boolean isDirectParent()
	{
		return true;
	}

	public int[] getAdviceCFlow()
	{
		return null;
	}

	public int getDepth()
	{
		return 0;
	}

	public IHostInfo getHost()
	{
		return itsThread.getHost();
	}

	public IBehaviorCallEvent getParent()
	{
		return null;
	}

	public ExternalPointer getParentPointer()
	{
		return null;
	}

	public ExternalPointer getPointer()
	{
		throw new UnsupportedOperationException();
	}

	public IThreadInfo getThread()
	{
		return itsThread;
	}

	public long getTimestamp()
	{
		return 0;
	}

}
