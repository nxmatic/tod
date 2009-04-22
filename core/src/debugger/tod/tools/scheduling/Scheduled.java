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
package tod.tools.scheduling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tod.tools.scheduling.IJobScheduler.JobPriority;

/**
 * This annotation permits to transparently execute the method they
 * apply to through a scheduler.
 * The class that declares the method must implement {@link IJobSchedulerProvider}
 * so that the correct scheduler can be used.
 * Only methods that return void can be marked by this annotation. 
 * @author gpothier
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled
{
	JobPriority value() default JobPriority.DEFAULT;
	
	/**
	 * Indicates if other jobs should be cancelled prior to submitting the new job.
	 * @return
	 */
	boolean cancelOthers() default false;
}
