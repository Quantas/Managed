package com.quantasnet.managed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes the object passed in and parses all the @Managed annotations for the constructors/methods/fields<br />
 * Once all the annotations are found it will then build the MBeanInfo object containing the methods/fields/constructors<br />
 * This class should not be used directly, but invoked through the ManagementProcessor's register method, which will call<br />
 * this class to create the object then register it with the PlatformMBeanServer.<br />
 * <br />
 * 
 * @author Quantas
 */
/*package*/ final class DynamicManagementMBean implements DynamicMBean
{
	private static final Logger LOG = LoggerFactory.getLogger(DynamicManagementMBean.class);
	
	private Object objInstance;
	private Class<?> objClass;
	private MBeanInfo info;
	
	private MBeanAttributeInfo[] attributes;
	private MBeanOperationInfo[] operations;
	private MBeanConstructorInfo[] mgmtConstructors;
	private MBeanNotificationInfo[] notifications;
	
	/**
	 * 
	 * @param objInstance The instance of the object to be Managed
	 * @param description Description of the object for JMX
	 * @throws InvalidManagementAnnotationException
	 */
	public DynamicManagementMBean(final Object objInstance, final String description)
	{
		this.objInstance = objInstance;
		
		objClass = objInstance.getClass();
		
		final Method[] methods = objClass.getDeclaredMethods();
		final Field[] fields = objClass.getDeclaredFields();
		final Constructor<?>[] constructors = objClass.getDeclaredConstructors();
		
		attributes = null;
		operations = null;
		mgmtConstructors = null;
		notifications = null;	//NYI

		createMBeans(methods, fields, constructors);
		
		info = new MBeanInfo(this.objClass.getName(), description, attributes, mgmtConstructors, operations, notifications);
	}
	
	private void createMBeans(final Method[] methods, final Field[] fields, final Constructor<?>[] constructors)
	{
		final ArrayList<MBeanAttributeInfo> attrList = new ArrayList<MBeanAttributeInfo>();
		final ArrayList<MBeanOperationInfo> operList = new ArrayList<MBeanOperationInfo>();
		final ArrayList<MBeanConstructorInfo> consList = new ArrayList<MBeanConstructorInfo>();
		
		//Parse the annotations for all the methods
		for(final Method method : methods)
		{
			final Managed mgmt = method.getAnnotation(Managed.class);			
			if(mgmt != null)
			{				
				operList.add(new MBeanOperationInfo(mgmt.description(), method));
			}
		}
		
		//Parse the annotations for all the fields
		for(final Field field : fields)
		{
			final Managed mgmt = field.getAnnotation(Managed.class);
			if(mgmt != null)
			{
				attrList.add(new MBeanAttributeInfo(field.getName(), field.getType().getName(), mgmt.description(), mgmt.readable(), mgmt.writable(), false));
			}
		}
		
		//Parse the annotations for all the constructors
		for(final Constructor<?> constructor : constructors)
		{
			final Managed mgmt = constructor.getAnnotation(Managed.class);
			if(mgmt != null)
			{
				consList.add(new MBeanConstructorInfo(mgmt.description(), constructor));
			}
		}
		
		if(!attrList.isEmpty())
		{
			attributes = new MBeanAttributeInfo[attrList.size()];
			attrList.toArray(attributes);
		}
		
		if(!operList.isEmpty())
		{
			operations = new MBeanOperationInfo[operList.size()];
			operList.toArray(operations);
		}
		
		if(!consList.isEmpty())
		{
			mgmtConstructors = new MBeanConstructorInfo[consList.size()];
			consList.toArray(mgmtConstructors);
		}
	}
	
	public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
	{
		Object value = null;
        
		try 
		{
			boolean foundAttribute = false;
			
			for(final MBeanAttributeInfo mbeanAttr : attributes)
			{
				if(mbeanAttr.getName().equals(attribute))
				{
					foundAttribute = true;
					
					if(mbeanAttr.isReadable())
					{
						final Field field = objClass.getDeclaredField(attribute);
						
						final boolean isAccessible = field.isAccessible();
						if(!isAccessible)
						{
							field.setAccessible(true);
						}
						
						value = field.get(objInstance);
						
						field.setAccessible(isAccessible);
					}
					else
					{
						throw new Exception("Attribute not readable: " + attribute);
					}
				}
			}
			
			if(!foundAttribute)
			{
				throw new AttributeNotFoundException();
			}
		} 
		catch (Exception e) 
		{
			throw new MBeanException(e);
		}
		
        return value;
	}

	public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException 
	{
		try 
		{
			boolean foundAttribute = false;
			
			for(final MBeanAttributeInfo mbeanAttr : attributes)
			{
				if(mbeanAttr.getName().equals(attribute.getName()))
				{
					foundAttribute = true;
					
					if(mbeanAttr.isWritable())
					{
						final Field field = objClass.getDeclaredField(attribute.getName());
						final boolean isAccessible = field.isAccessible();
						if(!isAccessible)
						{
							field.setAccessible(true);
						}
						
						field.set(objInstance, attribute.getValue());
						
						field.setAccessible(isAccessible);
					}
					else
					{
						throw new Exception("Attribute not writable: " + attribute.getName());
					}
				}
			}
			
			if(!foundAttribute)
			{
				throw new AttributeNotFoundException();
			}
		} 
		catch (Exception e) 
		{
			LOG.error("Error setting Attribute", e);
			throw new MBeanException(e);
		} 
	}

	public AttributeList getAttributes(final String[] attributes) 
	{
		AttributeList values = new AttributeList();
        for (final String attribute : attributes)
        {
        	final String name = attribute;
        	
        	try
        	{
	            final Object value = getAttribute(name);
	            final Attribute attr = new Attribute(name, value);
	            
	            values.add(attr);
        	}
        	catch(Exception e)
        	{
        		LOG.error("Error getting attribute: " + attribute, e);
        	}
        }
        
        return values;
	}

	public AttributeList setAttributes(final AttributeList attributes) 
	{
		final AttributeList retList = new AttributeList();
		
		for(final Object attr : attributes)
		{
			try
			{
				setAttribute((Attribute)attr);
				retList.add(getAttribute(((Attribute)attr).getName()));
			}
			catch(Exception e)
			{
				LOG.error("Error setting attribute: " + attr, e);
			}
		}
		
		
		return retList;
	}

	public Object invoke(final String actionName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException 
	{
		Object retVal = null;
		
		boolean foundMethod = false;
		
		for(final MBeanOperationInfo mbOperInfo : operations)
		{
			if(mbOperInfo.getName().equals(actionName))
			{
				foundMethod = true;
				
				try
				{
					int paramCount = 0;
					
					final Class<?>[] paramClazzes = new Class<?>[params.length];
					
					for(final String param : signature)
					{
						if("boolean".equals(param))
						{
							paramClazzes[paramCount] = Boolean.TYPE;
						}
						else if("int".equals(param))
						{
							paramClazzes[paramCount] = Integer.TYPE;
						}
						else if("char".equals(param))
						{
							paramClazzes[paramCount] = Character.TYPE;
						}
						else if("long".equals(param))
						{
							paramClazzes[paramCount] = Long.TYPE;
						}
						else if("double".equals(param))
						{
							paramClazzes[paramCount] = Double.TYPE;
						}
						else if("float".equals(param))
						{
							paramClazzes[paramCount] = Float.TYPE;
						}
						else if("byte".equals(param))
						{
							paramClazzes[paramCount] = Byte.TYPE;
						}
						else
						{
							paramClazzes[paramCount] = Class.forName(param);
						}
						
						paramCount++;
					}
					
					final Method method = objClass.getDeclaredMethod(actionName, paramClazzes);
					
					method.setAccessible(true);
					
					retVal = method.invoke(objInstance, params);
					
					method.setAccessible(false);
				}
				catch(Exception e)
				{
					LOG.error("Error invoking " + actionName, e);
					throw new MBeanException(e, "Error invoking " + actionName);
				}
			}
		}
		
		if(!foundMethod)
		{
			final Exception exc = new Exception("No such method known to JMX: " + actionName);
			LOG.error("No such method known to JMX: " + actionName, exc);
			
			throw new MBeanException(exc);
		}
		
		return retVal;
	}

	public MBeanInfo getMBeanInfo() 
	{
		return info;
	}
}