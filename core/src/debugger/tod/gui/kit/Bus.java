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
package tod.gui.kit;

import java.awt.Component;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.gui.kit.messages.Message;
import zz.utils.ListMap;
import zz.utils.properties.IProperty;
import zz.utils.properties.IRWProperty;

public class Bus
{
	public static final Bus ROOT_BUS = new Bus(null);
	
	/**
	 * Returns the bus that should be used by the given component.
	 */
	public static Bus get(Component aComponent)
	{
		while(true)
		{
			if (aComponent == null) return ROOT_BUS;
			else if (aComponent instanceof IBusOwner)
			{
				IBusOwner theBusOwner = (IBusOwner) aComponent;
				return theBusOwner.getBus();
			}
			else aComponent = aComponent.getParent();
		} 
	}
	
	private Component itsOwner;
	
	private ListMap<String, IBusListener> itsListeners = new ListMap<String, IBusListener>();
	private Map<Class, AbstractServiceInfo> itsServices = new HashMap<Class, AbstractServiceInfo>();
	private Map<PropertyId, PropertyHolder> itsProperties = new HashMap<PropertyId, PropertyHolder>();
	
	public Bus(Component aOwner)
	{
		itsOwner = aOwner;
	}
	
	/**
	 * Posts a message on this bus. All the listeners that are registered
	 * for the proper message id will receive this message.
	 */
	public void postMessage(Message aMessage)
	{
		List<IBusListener> theListeners = itsListeners.get(aMessage.getId());
		if (theListeners != null) for (IBusListener theListener : theListeners)
		{
			if (theListener.processMessage(aMessage)) return;
		}
			
		
		if (itsOwner != null) get(itsOwner.getParent()).postMessage(aMessage);
	}
	
	/**
	 * Subscribes a bus listener to messages of the given id.
	 */
	public void subscribe(String aId, IBusListener aListener)
	{
		itsListeners.add(aId, aListener);
	}
	
	public void unsubscribe(String aId, IBusListener aListener)
	{
		itsListeners.remove(aId, aListener);
	}
	
	/**
	 * Puts a property in this bus.
	 * @param aId An identifier for the property
	 * @param aProperty The property
	 * @param aRw Whether other bus clients can obtain a {@link IRWProperty} for the
	 * property, or only a {@link IProperty}.
	 */
	public <T> void putProperty(PropertyId<T> aId, IRWProperty<T> aProperty, boolean aRw)
	{
		PropertyHolder theOld = itsProperties.put(aId, new PropertyHolder(aProperty, aRw));
		if (theOld != null) throw new RuntimeException("Property already present: "+aId);
	}
	
	public <T> void removeProperty(PropertyId<T> aId)
	{
		itsProperties.remove(aId);
	}
	
	/**
	 * Returns a previously registered property.
	 * @see #putProperty(String, IRWProperty, boolean)
	 */
	public <T> IProperty<T> getProperty(PropertyId<T> aId)
	{
		PropertyHolder theHolder = itsProperties.get(aId);
		return theHolder != null ? theHolder.property : null;
	}
	
	/**
	 * Returns a previously registered property. This method fails if the property was
	 * not registered as RW.
	 * @see #putProperty(String, IRWProperty, boolean)
	 */	
	public <T> IRWProperty<T> getRWProperty(PropertyId<T> aId)
	{
		PropertyHolder theHolder = itsProperties.get(aId);
		if (theHolder == null) return null;
		if (! theHolder.rw) throw new RuntimeException("Property is not writable");
		return theHolder.property;
	}
	
	/**
	 * Registers a service on this bus. Services can be looked up by class
	 * using {@link #getService(Class)}.
	 * @param aService The service to register.
	 * @param aExclusive If true, only one service of this kind is allowed. If a service
	 * of this kind is already registered, this method fails. If an attempt is made to
	 * register another service of this kind later, the attempt will fail.
	 */
	public <T> void registerService(Class<T> aClass, T aService, boolean aExclusive)
	{
		AbstractServiceInfo<T> theServiceInfo = itsServices.get(aClass);
		if (theServiceInfo == null)
		{
			theServiceInfo = aExclusive ? 
					new ExclusiveServiceInfo<T>(aService)
					: new MultipleServiceInfo<T>(aClass, aService);
					
			itsServices.put(aClass, theServiceInfo);
		}
		else
		{
			if (aExclusive) throw new IllegalStateException("There is already a service registered for: "+aClass);
			if (theServiceInfo.isExclusive()) throw new IllegalStateException("There is already an exclusive service registered for: "+aClass);
			
			MultipleServiceInfo<T> theMultipleServiceInfo = (MultipleServiceInfo<T>) theServiceInfo;
			theMultipleServiceInfo.addService(aService);
		}
	}
	
	public <T> void unregisterService(Class<T> aClass, T aService)
	{
		AbstractServiceInfo<T> theServiceInfo = itsServices.get(aClass);
		if (theServiceInfo == null) return;
		if (theServiceInfo.isExclusive())
		{
			if (theServiceInfo.getProxy() == aService) itsServices.remove(aClass);
		}
		else
		{
			MultipleServiceInfo<T> theMultipleServiceInfo = (MultipleServiceInfo<T>) theServiceInfo;
			theMultipleServiceInfo.removeService(aService);
			if (theMultipleServiceInfo.isEmpty()) itsServices.remove(aClass);
		}
	}
	
	/**
	 * Returns a service of the given interface.
	 * In the case of non-exclusive services, the returned object is a proxy
	 * that forwards any call to all corresponding registered services. 
	 * @return A registered service, or null if none found.
	 */
	public <T> T getService(Class<T> aClass)
	{
		AbstractServiceInfo<T> theServiceInfo = itsServices.get(aClass);
		return theServiceInfo != null ? theServiceInfo.getProxy() : null;
	}
	
	private static abstract class AbstractServiceInfo<T>
	{
		public abstract T getProxy();
		public abstract boolean isExclusive();
	}
	
	private static class ExclusiveServiceInfo<T> extends AbstractServiceInfo<T>
	{
		private T itsService;

		public ExclusiveServiceInfo(T aService)
		{
			itsService = aService;
		}
		
		@Override
		public boolean isExclusive()
		{
			return true;
		}
		
		@Override
		public T getProxy()
		{
			return itsService;
		}
	}
	
	private static class MultipleServiceInfo<T> extends AbstractServiceInfo<T>
	implements InvocationHandler
	{
		private List<T> itsServices = new ArrayList<T>();
		private T itsProxy;
		
		public MultipleServiceInfo(Class<T> aClass, T aInitialService)
		{
			itsServices.add(aInitialService);
			itsProxy = (T) Proxy.newProxyInstance(
					aClass.getClassLoader(), 
					new Class[] {aClass}, 
					this);
		}
		
		public void addService(T aService)
		{
			itsServices.add(aService);
		}
		
		public void removeService(T aService)
		{
			itsServices.remove(aService);
		}
		
		@Override
		public boolean isExclusive()
		{
			return false;
		}
		
		public boolean isEmpty()
		{
			return itsServices.isEmpty();
		}

		public Object invoke(Object aProxy, Method aMethod, Object[] args) throws Throwable
		{
			for (T theService : itsServices)
			{
				aMethod.invoke(theService, args);
			}
			return null;
		}

		@Override
		public T getProxy()
		{
			return itsProxy;
		}
	}
	
	private static class PropertyHolder
	{
		public final IRWProperty property;
		public final boolean rw;
		
		public PropertyHolder(IRWProperty aProperty, boolean aRw)
		{
			property = aProperty;
			rw = aRw;
		}
	}
}
