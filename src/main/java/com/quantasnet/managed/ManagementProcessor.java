package com.quantasnet.managed;

import java.lang.management.ManagementFactory;

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

/**
 * Factory class for registering DynamicMBeans
 * @author Quantas
 */
public class ManagementProcessor 
{
	private static final Logger LOG = Logger.getLogger(ManagementProcessor.class);
	
	public static ObjectName constructObjectName(Object obj)
	{
		ObjectName name = null;
		
		try
		{
			name = new ObjectName(obj.getClass().getPackage().getName() + ":type=" + obj.getClass().getSimpleName());
		}
		catch(JMException e)
		{
			
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
				DynamicManagementMBean mbean = new DynamicManagementMBean(obj, desc);
				if(mbean.getMBeanInfo() != null)
				{
					instance = mbean;
				}
			}
			
			if(instance != null)
			{
				ManagementFactory.getPlatformMBeanServer().registerMBean(instance, constructObjectName(obj));
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
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(constructObjectName(obj));
		}
		catch(JMException e)
		{
			LOG.error("Error Unregistering the DynamicManagementMBean with the Factory", e);
		}
	}

}