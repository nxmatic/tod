/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.dbgrid;

import java.net.URI;

import javax.swing.JComponent;

import tod.core.config.TODConfig;
import tod.core.database.browser.ILogBrowser;
import tod.core.session.AbstractSession;
import tod.gui.IGUIManager;
import tod.impl.dbgrid.gui.GridConsole;
import zz.utils.Utils;
import zz.utils.monitoring.Monitor.MonitorData;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

public abstract class AbstractGridSession extends AbstractSession
{
	private RIGridMaster itsMaster;
	private GridLogBrowser itsBrowser;

	private boolean itsUpdatingCapture = false;
	
	private final IRWProperty<Boolean> pCaptureEnabled = 
		new SimpleRWProperty<Boolean>(getConfig().get(TODConfig.AGENT_CAPTURE_AT_START))
		{
			@Override
			protected Object canChange(Boolean aOldValue, Boolean aNewValue)
			{
				if (getMaster() == null) return aNewValue == null ? ACCEPT : REJECT;
				else return aNewValue != null ? ACCEPT : REJECT;
			}
			
			@Override
			protected void changed(Boolean aOldValue, Boolean aNewValue)
			{
				if (itsUpdatingCapture) return;
				if (getMaster() == null) return;
				else getMaster().sendEnableCapture(aNewValue);
			}
		};
		
	private MasterListener itsMasterListener; 

	public AbstractGridSession(IGUIManager aGUIManager, URI aUri, TODConfig aConfig)
	{
		super(aGUIManager, aUri, aConfig);
	}

	protected RIGridMaster getMaster()
	{
		return itsMaster;
	}
	
	public ILogBrowser getLogBrowser()
	{
		return itsBrowser;
	}
	
	protected void setMaster(RIGridMaster aMaster) 
	{
		if (itsMasterListener == null) itsMasterListener = new MasterListener();
		
		if (itsMaster != null) itsMaster.removeListener(itsMasterListener);
		itsMaster = aMaster;
		if (itsMaster != null) itsMaster.addListener(itsMasterListener);
		
		itsBrowser = itsMaster != null ?
				DebuggerGridConfig.createRemoteLogBrowser(this, itsMaster)
				: null;
	}

	public IRWProperty<Boolean> pCaptureEnabled()
	{
		return pCaptureEnabled;
	}

	public JComponent createConsole()
	{
		return new GridConsole(itsMaster);
	}

	public void flush()
	{
		itsMaster.flush();
	}

	private class MasterListener implements RIGridMasterListener
	{
		public void eventsReceived() 
		{
			itsBrowser.clearStats();
		}

		public void exception(Throwable aThrowable) 
		{
			if (getGUIManager() != null)
			{
				getGUIManager().showDialog(new IGUIManager.ErrorDialogType(
						"Database error",
						"An error occurred in the database: "+Utils.getRootCause(aThrowable).getMessage()));
			}
			
			aThrowable.printStackTrace();
		}

		public void monitorData(int aNodeId, MonitorData aData) 
		{
		}

		public void captureEnabled(boolean aEnabled) 
		{
			itsUpdatingCapture = true;
			pCaptureEnabled().set(aEnabled);
			itsUpdatingCapture = false;
		}
	}
}
