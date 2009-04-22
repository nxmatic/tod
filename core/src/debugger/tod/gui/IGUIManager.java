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
package tod.gui;

import java.awt.Dimension;

import javax.swing.JComponent;

import tod.core.IBookmarks;
import tod.core.database.browser.IEventFilter;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;
import tod.gui.activities.ActivitySeed;
import tod.gui.kit.IBusOwner;
import tod.gui.settings.GUISettings;
import tod.tools.scheduling.JobScheduler;

/**
 * This interface permits to access the basic functionalities
 * of the UI, such as setting a new view, etc.
 * All interactive UI components should have a reference to
 * a GUI manager
 * @author gpothier
 */
public interface IGUIManager extends IBusOwner
{
	/**
	 * Sets the currently viewed seed.
	 * @param aNewTab If false, the viewer for the seed will replace the
	 * currently displayed viewer. If true, a new tab will be opened.
	 */
	public void openSeed (ActivitySeed aSeed, boolean aNewTab);
	
	/**
	 * Shows the location of the specified probe in the source code.
	 */
	public void gotoSource (ProbeInfo aProbe);
	
	/**
	 * Shows the location of the specified location in the source code.
	 */
	public void gotoSource (ILocationInfo aLocation);
	
	/**
	 * Returns a global job scheduler.
	 */
	public JobScheduler getJobScheduler();
	
	/**
	 * Returns the debugging session currently associated with this GUI manager.
	 */
	public ISession getSession();
	
	/**
	 * Shows the next event that occurred at a specific line, relative to
	 * the currently selected event.
	 * @param aBehavior The behavior that contains the line
	 * @param aInCFlow If true, only events that belong to the control flow of the 
	 * parent behavior of the currently selected event. Otherwise, considers the whole trace. 
	 */
	public void showNextEventForLine(IBehaviorInfo aBehavior, int aLine, boolean aInCFlow);

	/**
	 * Shows the previous event that occurred at a specific line, relative to
	 * the currently selected event.
	 * @param aBehavior The behavior that contains the line
	 * @param aInCFlow If true, only events that belong to the control flow of the 
	 * parent behavior of the currently selected event. Otherwise, considers the whole trace. 
	 */
	public void showPreviousEventForLine(IBehaviorInfo aBehavior, int aLine, boolean aInCFlow);
	
	/**
	 * Whether the "Show next event for line" action should be enabled.
	 */
	public boolean canShowNextEventForLine();

	/**
	 * Whether the "Show previous event for line" action should be enabled.
	 */
	public boolean canShowPreviousEventForLine();

	/**
	 * Shows a list of all the events that occurred at the specified line.
	 * @param aFilter An optional additional filter.
	 */
	public void showEventsForLine(IBehaviorInfo aBehavior, int aLine, IEventFilter aFilter);
	
	/**
	 * Returns a settings manager for this gui.
	 */
	public GUISettings getSettings();
	
	/**
	 * Returns the global bookmarks model.
	 */
	public IBookmarks getBookmarks();
	
	/**
	 * Shows a message/question to the user.
	 * See {@link SwingDialogUtils} for an example implementation.
	 */
	public <T> T showDialog(DialogType<T> aDialog);
	
	/**
	 * Displays the given component as a floating post-it note.
	 * @param aSize The requested size of the postit. 
	 */
	public void showPostIt(JComponent aComponent, Dimension aSize);
	
	/**
	 * Returns an object that can be used to extend the TOD GUI.
	 */
	public IExtensionPoints getExtensionPoints();
	
	/**
	 * Models a dialog presented to the user.
	 * @param <R> The type of the response (Boolean for YES/NO, Void for OK...)
	 * @author gpothier
	 */
	public static abstract class DialogType<R>
	{
		private String itsTitle;
		private String itsText;

		public DialogType(String aTitle, String aText)
		{
			itsTitle = aTitle;
			itsText = aText;
		}

		public String getTitle()
		{
			return itsTitle;
		}

		public String getText()
		{
			return itsText;
		}
	}
	
	public static class YesNoDialogType extends DialogType<Boolean>
	{
		public YesNoDialogType(String aTitle, String aText)
		{
			super(aTitle, aText);
		}
	}
	
	public static class OkCancelDialogTYpe extends DialogType<Boolean>
	{
		public OkCancelDialogTYpe(String aTitle, String aText)
		{
			super(aTitle, aText);
		}
	}
	
	public static class ErrorDialogType extends DialogType<Void>
	{
		public ErrorDialogType(String aTitle, String aText)
		{
			super(aTitle, aText);
		}
	}
}
