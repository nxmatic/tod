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

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import tod.gui.IGUIManager.DialogType;
import tod.gui.IGUIManager.ErrorDialogType;
import tod.gui.IGUIManager.OkCancelDialogTYpe;
import tod.gui.IGUIManager.YesNoDialogType;

/**
 * Utility class for implementing {@link IGUIManager#showDialog(tod.gui.IGUIManager.DialogType)}
 * on Swing.
 * @author gpothier
 */
public class SwingDialogUtils
{
	public static <T> T showDialog(JComponent aParent, DialogType<T> aDialog)
	{
		if (aDialog instanceof ErrorDialogType)
		{
			ErrorDialogType theDialog = (ErrorDialogType) aDialog;
			JOptionPane.showMessageDialog(aParent, theDialog.getText(), theDialog.getTitle(), JOptionPane.ERROR_MESSAGE);
			return null;
		}
		else if (aDialog instanceof OkCancelDialogTYpe)
		{
			OkCancelDialogTYpe theDialog = (OkCancelDialogTYpe) aDialog;
			int theResult = JOptionPane.showConfirmDialog(
					aParent, 
					theDialog.getText(), 
					theDialog.getTitle(), 
					JOptionPane.OK_CANCEL_OPTION);
			
			return (T) (Boolean) (theResult == JOptionPane.OK_OPTION);
		}
		else if (aDialog instanceof YesNoDialogType)
		{
			YesNoDialogType theDialog = (YesNoDialogType) aDialog;
			int theResult = JOptionPane.showConfirmDialog(
					aParent, 
					theDialog.getText(), 
					theDialog.getTitle(), 
					JOptionPane.YES_NO_OPTION);
			
			return (T) (Boolean) (theResult == JOptionPane.YES_OPTION);
		}
		else throw new IllegalArgumentException("Not handled: "+aDialog);
	}

}
