package com.quantasnet.managed;

import java.lang.management.ManagementFactory;

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
	
	private ManagementProcessor()
	{
		// no-op
	}
	
	public static ObjectName constructObjectName(Class<?> clazz)
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
	 * @throws InvalidManagementAnnotationException
	 */
	public static void register(Object obj, String desc)
	{
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
				ManagementFactory.getPlatformMBeanServer().registerMBean(instance, constructObjectName(obj.getClass()));
			}
		}
		catch(Exception e)
		{
			LOG.error("Error Registering the DynamicManagementMBean with the Factory", e);
		}
	}
	
	public static void unregister(Object obj)
	{
		try
		{
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(constructObjectName(obj.getClass()));
		}
		catch(JMException e)
		{
			LOG.error("Error Unregistering the DynamicManagementMBean with the Factory", e);
		}
	}
}