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
package java.tod;


/**
 * All possible low level event types. Low level events are those that are directly generated 
 * by instrumented code.
 * @author gpothier
 */
public enum _LowLevelEventType 
{
	CLINIT_ENTER,
	BEHAVIOR_ENTER,
	CLINIT_EXIT,
	BEHAVIOR_EXIT,
	BEHAVIOR_EXIT_EXCEPTION,
	EXCEPTION_GENERATED,
	FIELD_WRITE,
	NEW_ARRAY,
	ARRAY_WRITE,
	LOCAL_VARIABLE_WRITE,
	INSTANCEOF,
	BEFORE_CALL_DRY,
	BEFORE_CALL,
	AFTER_CALL_DRY,
	AFTER_CALL,
	AFTER_CALL_EXCEPTION,
	OUTPUT,
	
	
	// Registering
	REGISTER_OBJECT,
	REGISTER_THREAD,
	REGISTER_REFOBJECT,
	REGISTER_CLASS,
	REGISTER_CLASSLOADER;

	
	/**
	 * Cached values; call to values() is costly. 
	 */
	public static final _LowLevelEventType[] VALUES = values();
}
