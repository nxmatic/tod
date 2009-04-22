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
package tod.core.database.browser;

import java.util.List;

import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;

/**
 * Permits to determine the value of local variables during a method execution.
 * The inspector maintains a current event; variable values are obtained with respect
 * to the current event, ie. the inspector returns the value a given variable had
 * at the moment the current event was being executed.
 * <br/>
 * There are no uncertainties in evaluating the values of local variables as only
 * one thread accesses them.  
 * 
 * @see tod.core.database.browser.ILogBrowser#createVariablesInspector(IBehaviorEnterEvent)
 * @author gpothier
 */
public interface IVariablesInspector extends ICompoundInspector<LocalVariableInfo>
{
	/**
	 * Returns the behavior enter event that represents the method execution 
	 * analysed by this inspector.
	 */
	public IBehaviorCallEvent getBehaviorCall();
	
	/**
	 * Returns the analysed behavior.
	 */
	public IBehaviorInfo getBehavior();
	
	/**
	 * Returns a list of all the local variables available 
	 * in the analysed method
	 */
	public List<LocalVariableInfo> getVariables();
	
	/**
	 * Returns a list of all the local variables available at the specified
	 * bytecode index in the analysed method.
	 */
	public List<LocalVariableInfo> getVariables(int aBytecodeIndex);
}
