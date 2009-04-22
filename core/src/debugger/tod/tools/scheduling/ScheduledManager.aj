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
package tod.tools.scheduling;

import org.aspectj.lang.reflect.MethodSignature;

import tod.tools.scheduling.IJobScheduler.JobPriority;

/**
 * This aspect provides the semantics of the {@link Scheduled} annotation.
 * @author gpothier
 */
public aspect ScheduledManager
{
	pointcut scheduledExec(IJobSchedulerProvider aProvider):
		execution(@Scheduled void IJobSchedulerProvider+.*(..))
		&& this(aProvider);
	
	Object around(final IJobSchedulerProvider aProvider): scheduledExec(aProvider)
	{
		IJobScheduler theJobScheduler = aProvider.getJobScheduler();
		MethodSignature theSignature = (MethodSignature) thisJoinPointStaticPart.getSignature();
		Scheduled theAnnotation = theSignature.getMethod().getAnnotation(Scheduled.class);
		
		JobPriority thePriority = theAnnotation.value();
		if (theAnnotation.cancelOthers()) theJobScheduler.cancelAll();
		
		theJobScheduler.submit(thePriority, new Runnable()
		{
			public void run()
			{
				try
				{
					proceed(aProvider);
				}
				catch (RuntimeException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		});
		
		return null;
	}
	
	declare error: 
		execution(@Scheduled (!void) IJobSchedulerProvider+.*(..))
		: "@Scheduled cannot be applied to non-void method";

	declare error:
		execution(@Scheduled * (! IJobSchedulerProvider+).*(..))
		: "@Scheduled method must belong to a IJobSchedulerProvider";
}
