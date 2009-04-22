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
package tod.gui.formatter;



import tod.Util;
import tod.agent.ObjectValue;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.IArrayWriteEvent;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.IBehaviorExitEvent;
import tod.core.database.event.IExceptionGeneratedEvent;
import tod.core.database.event.IFieldWriteEvent;
import tod.core.database.event.IInstantiationEvent;
import tod.core.database.event.ILocalVariableWriteEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IOutputEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ILocationInfo;
import zz.utils.AbstractFormatter;

/**
 * Formatter for {@link ILogEvent}s
 * @author gpothier
 */
public class EventFormatter extends AbstractFormatter<ILogEvent>
{
	private ILogBrowser itsLogBrowser;
	private ObjectFormatter itsObjectFormatter;
	
	public EventFormatter(ILogBrowser aLogBrowser)
	{
		itsLogBrowser = aLogBrowser;
		itsObjectFormatter = new ObjectFormatter(itsLogBrowser);
	}

	@Override
	protected String getText(ILogEvent aEvent, boolean aHtml)
	{
		if (aEvent instanceof IInstantiationEvent)
		{
			return formatInstantiation((IInstantiationEvent) aEvent);
		}
		else if (aEvent instanceof IBehaviorCallEvent)
		{
			return formatBehaviorCall((IBehaviorCallEvent) aEvent);
		}
		else if (aEvent instanceof IBehaviorExitEvent)
		{
			return formatBehaviorExit((IBehaviorExitEvent) aEvent);
		}
		else if (aEvent instanceof IFieldWriteEvent)
		{
			return formatFieldWrite((IFieldWriteEvent) aEvent);
		}
        else if (aEvent instanceof ILocalVariableWriteEvent)
		{
			return formatLocalWrite((ILocalVariableWriteEvent) aEvent);
		}
		else if (aEvent instanceof IOutputEvent)
		{
			return formatOutput((IOutputEvent) aEvent);
		}
		else if (aEvent instanceof IExceptionGeneratedEvent)
		{
			return formatException((IExceptionGeneratedEvent) aEvent);
		}
		else if (aEvent instanceof IArrayWriteEvent)
		{
			return formatArrayWrite((IArrayWriteEvent) aEvent);
		}
		else return ""+aEvent;
	}

	private String formatArrayWrite(IArrayWriteEvent theEvent)
	{
		return String.format(
				"%s[%d] = %s",
				formatObject(theEvent.getTarget()),
				theEvent.getIndex(),
				formatObject(theEvent.getValue()));
	}

	private String formatException(IExceptionGeneratedEvent theEvent)
	{
		IBehaviorInfo theBehavior = theEvent.getOperationBehavior();
		String theBehaviorName = theBehavior != null ? 
				Util.getSimpleName(theBehavior.getDeclaringType().getName()) + "." + theBehavior.getName() 
				: "<unknown>";
				
		String theExceptionText;
		
		Object theException = theEvent.getException();
		if (theException instanceof ObjectValue)
		{
			ObjectValue theValue = (ObjectValue) theException;
			theExceptionText = theValue.getClassName();
		}
		else
		{
			theExceptionText = ""+theException;
		}
		
		return "Exception thrown in "+theBehaviorName+": "+theExceptionText;
	}

	private String formatOutput(IOutputEvent theEvent)
	{
		return "Output ("+theEvent.getOutput()+"): "+theEvent.getData();
	}

	private String formatLocalWrite(ILocalVariableWriteEvent theEvent)
	{
		return String.format(
				"%s = %s",
				theEvent.getVariable().getVariableName(),
				formatObject(theEvent.getValue()));
	}

	private String formatFieldWrite(IFieldWriteEvent theEvent)
	{
		return String.format(
				"%s.%s = %s",
				Util.getPrettyName(theEvent.getField().getDeclaringType().getName()),
				theEvent.getField().getName(),
				formatObject(theEvent.getValue()));
	}

	private String formatBehaviorExit(IBehaviorExitEvent theEvent)
	{
		IBehaviorCallEvent theParent = theEvent.getParent();
		
		if (theParent != null)
		{
			IBehaviorInfo theBehavior = theParent.getExecutedBehavior();
			if (theBehavior == null) theBehavior = theParent.getCalledBehavior();

			return String.format(
					"Return from %s.%s -> %s",
					Util.getPrettyName(theBehavior.getDeclaringType().getName()),
		            theBehavior.getName(),
		            theEvent.getResult());
		}
		else
		{
			return String.format(
					"Return from ? -> %s",
		            theEvent.getResult());
		}
	}

	private String formatBehaviorCall(IBehaviorCallEvent theEvent)
	{
		IBehaviorInfo theBehavior = theEvent.getExecutedBehavior();
		if (theBehavior == null) theBehavior = theEvent.getCalledBehavior();
		
		return String.format(
				"%s.%s (%s)",
				Util.getPrettyName(theBehavior.getDeclaringType().getName()),
		        theBehavior.getName(),
		        formatArgs(theEvent.getArguments()));
	}

	private String formatInstantiation(IInstantiationEvent theEvent)
	{
		return String.format(
				"%s (%s)",
				Util.getPrettyName(theEvent.getType().getName()),
		        formatArgs(theEvent.getArguments()));
	}
	
	public String formatObject (Object aObject)
	{
		return itsObjectFormatter.getPlainText(aObject);
	}
	
	private String formatLocation (ILocationInfo aLocationInfo)
	{
		return LocationFormatter.getInstance().getPlainText(aLocationInfo);
	}
	
	private String formatArgs (Object[] aArguments)
	{
		StringBuffer theBuffer = new StringBuffer();
		
		boolean theFirst = true;
		if (aArguments != null) for (Object theArgument : aArguments)
		{
			if (! theFirst) theBuffer.append(", ");
			else theFirst = false;
			
			theBuffer.append(formatObject(theArgument));
		}
		
		return theBuffer.toString();
	}

	/**
	 * Formats the given event.
	 */
	public static String formatEvent(ILogBrowser aLogBrowser, ILogEvent aEvent)
	{
		return new EventFormatter(aLogBrowser).getText(aEvent, false);
	}
}
