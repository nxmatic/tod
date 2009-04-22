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
package tod.tools.recording;

import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.MethodSignature;

import tod.core.DebugFlags;
import tod.core.database.browser.ICompoundInspector;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.browser.IVariablesInspector;
import tod.core.database.browser.ICompoundInspector.EntryValue;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.session.ISession;

/**
 * Records the call to database primitives so that they can be later
 * replayed (for benchmarking).
 * @author gpothier
 */
//public aspect Recorder
//{
//	pointcut recordedCall():
//		(execution(* ILogBrowser+.getSession(..)) 
//		|| execution(* ILogBrowser+.clear(..)) 
//		|| execution(* ILogBrowser+.getEvent(..)) 
//		|| execution(* ILogBrowser+.getStructureDatabase(..)) 
//		|| execution(* ILogBrowser+.getThreads(..)) 
//		|| execution(* ILogBrowser+.getHosts(..)) 
//		|| execution(* ILogBrowser+.getHost(..)) 
//		|| execution(* ILogBrowser+.create*(..)) 
//		|| execution(* ILogBrowser+.getCFlowRoot(..)) 
//		|| execution(* ILogBrowser+.searchStrings(..)) 
//		|| execution(* ILogBrowser+.exec(..)) 
//				
//		|| execution(* ISession+.getLogBrowser())
//		
//		|| execution(* IEventBrowser+.getLogBrowser(..))
//		|| execution(* IEventBrowser+.getFilter(..))
//		|| execution(* IEventBrowser+.getEventCount(..))
//		|| execution(* IEventBrowser+.getEventCounts(..))
//		|| execution(* IEventBrowser+.setNextEvent(..))
//		|| execution(* IEventBrowser+.setPreviousEvent(..))
//		|| execution(* IEventBrowser+.setNextTimestamp(..))
//		|| execution(* IEventBrowser+.setPreviousTimestamp(..))
//		|| execution(* IEventBrowser+.hasNext(..))
//		|| execution(* IEventBrowser+.hasPrevious(..))
//		|| execution(* IEventBrowser+.next(..))
//		|| execution(* IEventBrowser+.previous(..))
//		|| execution(* IEventBrowser+.createIntersection(..))
//		|| execution(* IEventBrowser+.getFirstTimestamp(..))
//		|| execution(* IEventBrowser+.getLastTimestamp(..))
//		|| execution(* IEventBrowser+.clone(..))
//		
//		|| execution(* IStructureDatabase+.getClass(..))
//		|| execution(* IStructureDatabase+.getClasses(..))
//		|| execution(* IStructureDatabase+.getType(..))
//		|| execution(* IStructureDatabase+.getArrayType(..))
//		|| execution(* IStructureDatabase+.getField(..))
//		|| execution(* IStructureDatabase+.getBehavior(..))
//		|| execution(* IStructureDatabase+.getBehaviors(..))
//		|| execution(* IStructureDatabase+.getProbeInfo(..))
//		|| execution(* IStructureDatabase+.getAdvice(..))
//		
//		|| execution(* ILogEvent+.getHost())
//		|| execution(* ILogEvent+.getThread())
//		|| execution(* ILogEvent+.getParent())
//		|| execution(* ILogEvent+.getParentPointer())
//		|| execution(* ILogEvent+.getPointer())
//		
//		|| execution(* IParentEvent+.getChildrenBrowser())
//		
//		|| execution(* IBehaviorCallEvent+.getExecutedBehavior())
//		|| execution(* IBehaviorCallEvent+.getCalledBehavior())
//		|| execution(* IBehaviorCallEvent+.getCallingBehavior())
//		|| execution(* IBehaviorCallEvent+.getExitEvent())
//		
//		|| execution(* ExternalPointer.*(..))
//
//		|| execution(* IClassInfo+.getSupertype(..))
//		|| execution(* IClassInfo+.getInterfaces(..))
//		|| execution(* IClassInfo+.getField(..))
//		|| execution(* IClassInfo+.getBehavior(..))
//		|| execution(* IClassInfo+.getFields(..))
//		|| execution(* IClassInfo+.getBehaviors(..))
//		
//		|| execution(* ICompoundInspector+.setReferenceEvent(..))
//		|| execution(* ICompoundInspector+.getReferenceEvent(..))
//		|| execution(* ICompoundInspector+.getEntryValue(..))
//		|| execution(* ICompoundInspector+.nextEntryValue(..))
//		|| execution(* ICompoundInspector+.previousEntryValue(..))
//		
//		|| execution(* IObjectInspector+.getLogBrowser(..))
//		|| execution(* IObjectInspector+.getCreationEvent(..))
//		|| execution(* IObjectInspector+.getType(..))
//		|| execution(* IObjectInspector+.getFields(..))
//		|| execution(* IObjectInspector+.getBrowser(..))
//		|| execution(* IObjectInspector+.getNewValue(..))
//		
//		|| execution(* IVariablesInspector+.getBehaviorCall(..))
//		|| execution(* IVariablesInspector+.getBehavior(..))
//		|| execution(* IVariablesInspector+.getVariables(..))
//		
//		|| execution(* ICompoundInspector.EntryValue.getSetter(..))
//		
//		) && ! (
//		execution(*.new(..))
//		|| execution(static * *.*(..))
//		);
//	
//	pointcut reenteringCall():
//		(execution(* ILogBrowser+.*(..))
//		|| execution(* ISession+.*(..))
//		|| execution(* IEventBrowser+.*(..))
//		|| execution(* IStructureDatabase+.*(..))
//		|| execution(* ILogEvent+.*(..))
//		|| execution(* IClassInfo+.*(..))
//		|| execution(* ICompoundInspector+.*(..))
//		);
//	
//	after() returning(Object r): recordedCall() && ! cflowbelow(reenteringCall())
//	{
//		if (DebugFlags.TRACE_DBCALLS)
//		{
//			MethodSignature theSignature = (MethodSignature) thisJoinPoint.getSignature();
//			RecorderHelper.getInstance().recordCall(
//					thisJoinPoint.getThis(), 
//					theSignature.getName(),
//					theSignature.getParameterTypes(),
//					thisJoinPoint.getArgs(), 
//					r,
//					""+thisJoinPoint.getSourceLocation());
//		}
//	}
//	
////	pointcut recordedConstructor():
////		call(tod.impl.dbgrid.event.BehaviorCallEvent.CallInfoBuilder.new(..));
////	
////	after() returning(Object r): recordedConstructor() 
////	{
////		if (DebugFlags.TRACE_DBCALLS)
////		{
////			ConstructorSignature theSignature = (ConstructorSignature) thisJoinPoint.getSignature();
////			RecorderHelper.getInstance().recordNew(
////					theSignature.getDeclaringTypeName(),
////					theSignature.getParameterTypes(),
////					thisJoinPoint.getArgs(), 
////					r,
////					""+thisJoinPoint.getSourceLocation());
////		}
////	}
//}
