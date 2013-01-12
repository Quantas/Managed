package com.quantasnet.managed;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for registering DynamicMBeans
 * @author Quantas
 */
public final class ManagementProcessor 
{
	private static final Logger LOG = LoggerFactory.getLogger(ManagementProcessor.class);
	
	@Managed
	private static final List<ObjectName> REGISTERED_OBJS = new ArrayList<ObjectName>();
	
	private static ManagementProcessor instance;
	
	static
	{
		instance = new ManagementProcessor();
		ManagementProcessor.register(instance, "Objects registered with the @Managed annotation");
	}
	
	private ManagementProcessor()
	{
		// no-op
	}
	
	public static ObjectName constructObjectName(final Class<?> clazz)
	{
		ObjectName name = null;
		
		try
		{
			name = new ObjectName(clazz.getPackage().getName() + ":type=" + clazz.getSimpleName());
		}
		catch(JMException e)
		{
			LOG.error("Error creating ObjectName", e);
		}
		
		return name;
	}
	
	public static void register(Object obj)
	{
		ManagementProcessor.register(obj, "");
	}
	
	/**
	 * Register a new DynamicMBean
	 * @param obj Instance of the object to be managed
	 * @param desc Description of the MBean
	 */
	public static void register(final Object obj, final String desc)
	{
		LOG.info("Registering MBean: " + obj.getClass().getSimpleName());
		Object instance = null;
		
		try
		{
			if(obj instanceof DynamicMBean)
			{
				instance = obj;
			}
			else
			{
				final DynamicManagementMBean mbean = new DynamicManagementMBean(obj, desc);
				if(mbean.getMBeanInfo() != null)
				{
					instance = mbean;
				}
			}
			
			if(instance != null)
			{
				final ObjectName objName = constructObjectName(obj.getClass());
				
				ManagementFactory.getPlatformMBeanServer().registerMBean(instance, objName);
				
				synchronized(REGISTERED_OBJS)
				{
					REGISTERED_OBJS.add(objName);
				}
			}
		}
		catch(Exception e)
		{
			LOG.error("Error Registering the DynamicManagementMBean with the Factory", e);
		}
	}
	
	public static void unregister(final Object obj)
	{
		try
		{
			final ObjectName objName = constructObjectName(obj.getClass());
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(objName);
			
			synchronized(REGISTERED_OBJS)
			{
				ObjectName toRemove = null;
				for (final ObjectName oName : REGISTERED_OBJS)
				{
					if (oName.getCanonicalName().equals(objName.getCanonicalName()))
					{
						toRemove = oName;
					}
				}
				
				if(toRemove != null)
				{
					REGISTERED_OBJS.remove(toRemove);
				}
			}
		}
		catch(JMException e)
		{
			LOG.error("Error Unregistering the DynamicManagementMBean with the Factory", e);
		}
	}
}