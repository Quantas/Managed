package com.quantasnet.managed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

/**
 * This class takes the object passed in and parses all the @Managed annotations for the constructors/methods/fields<br />
 * Once all the annotations are found it will then build the MBeanInfo object containing the methods/fields/constructors<br />
 * This class should not be used directly, but invoked through the ManagementProcessor's register method, which will call<br />
 * this class to create the object then register it with the PlatformMBeanServer.<br />
 * <br />
 * *NOTE* If using invoke with a param that is the Class representation of a primitive type it will not work, ie using Boolean instead of boolean<br />
 * <br />
 * TODO clean up error handling
 * @author Quantas
 */
class DynamicManagementMBean implements DynamicMBean
{
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
	public DynamicManagementMBean(Object objInstance, String description) throws InvalidManagementAnnotationException
	{
		this.objInstance = objInstance;
		objClass = objInstance.getClass();
		Method[] methods = objClass.getDeclaredMethods();
		Field[] fields = objClass.getDeclaredFields();
		Constructor<?>[] constructors = objClass.getDeclaredConstructors();
		
		attributes = null;
		operations = null;
		mgmtConstructors = null;
		notifications = null;	//NYI

		try
		{
			createMBeans(methods, fields, constructors);
			
			if(!(attributes == null && operations == null && mgmtConstructors == null && notifications == null))
			{
				info = new MBeanInfo(this.objClass.getName(), description, attributes, mgmtConstructors, operations, notifications);
			}
		} 
		catch (IntrospectionException e) 
		{
			throw new InvalidManagementAnnotationException("Error Registering the DynamicManagementMBean: ", e);
		}
	}
	
	private void createMBeans(Method[] methods, Field[] fields, Constructor<?>[] constructors) throws InvalidManagementAnnotationException, IntrospectionException
	{
		ArrayList<MBeanAttributeInfo> attrList = new ArrayList<MBeanAttributeInfo>();
		ArrayList<MBeanOperationInfo> operList = new ArrayList<MBeanOperationInfo>();
		ArrayList<MBeanConstructorInfo> consList = new ArrayList<MBeanConstructorInfo>();
		
		//Parse the annotations for all the methods
		for(int i=0; i<methods.length; i++)
		{
			Managed mgmt = methods[i].getAnnotation(Managed.class);			
			if(mgmt != null)
			{				
				operList.add(new MBeanOperationInfo(mgmt.description(), methods[i]));
			}
		}
		
		//Parse the annotations for all the fields
		for(int i=0; i<fields.length; i++)
		{
			Managed mgmt = fields[i].getAnnotation(Managed.class);
			if(mgmt != null)
			{
				attrList.add(new MBeanAttributeInfo(fields[i].getName(), fields[i].getType().getName(), mgmt.description(), mgmt.readable(), mgmt.writable(), false));
			}
		}
		
		//Parse the annotations for all the constructors
		for(int i=0; i<constructors.length; i++)
		{
			Managed mgmt = constructors[i].getAnnotation(Managed.class);
			if(mgmt != null)
			{
				consList.add(new MBeanConstructorInfo(mgmt.description(), constructors[i]));
			}
		}
		
		if(attrList.size() > 0)
		{
			attributes = new MBeanAttributeInfo[attrList.size()];
			attrList.toArray(attributes);
		}
		
		if(operList.size() > 0)
		{
			operations = new MBeanOperationInfo[operList.size()];
			operList.toArray(operations);
		}
		
		if(consList.size() > 0)
		{
			mgmtConstructors = new MBeanConstructorInfo[consList.size()];
			consList.toArray(mgmtConstructors);
		}
	}
	
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
	{
		Object value = null;
        
		try 
		{
			Field field = objClass.getDeclaredField(attribute);
			field.setAccessible(true);
			value = field.get(objInstance);
			field.setAccessible(false);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
        return value;
	}

	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException 
	{
		try 
		{
			Field field = objClass.getDeclaredField(attribute.getName());
			field.setAccessible(true);
			field.set(objInstance, attribute.getValue());
			field.setAccessible(false);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
	}

	public AttributeList getAttributes(String[] attributes) 
	{
		AttributeList values = new AttributeList();
        for (int a = 0; a < attributes.length; a++) 
        {
        	String name = attributes[a];
        	try
        	{
	            Object value = getAttribute(name);
	            Attribute attr = new Attribute(name, value);
	            values.add(attr);
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        }
        
        return values;
	}

	public AttributeList setAttributes(AttributeList attributes) 
	{
		AttributeList retList = new AttributeList();
		try
		{
			for(Object attr : attributes)
			{
				setAttribute((Attribute)attr);
				retList.add(getAttribute(((Attribute)attr).getName()));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return retList;
	}

	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException 
	{
		Object retVal = null;
		
		for(MBeanOperationInfo mbOperInfo : operations)
		{
			if(mbOperInfo.getName().equals(actionName))
			{
				try
				{
					int paramCount = 0;
					
					Class<?>[] paramClazzes = new Class<?>[params.length];
					
					for(Object param : params)
					{
						if(param instanceof Boolean)
							paramClazzes[paramCount] = Boolean.TYPE;
						else if(param instanceof Integer)
							paramClazzes[paramCount] = Integer.TYPE;
						else if(param instanceof Character)
							paramClazzes[paramCount] = Character.TYPE;
						else if(param instanceof Short)
							paramClazzes[paramCount] = Short.TYPE;
						else if(param instanceof Long)
							paramClazzes[paramCount] = Long.TYPE;	
						else if(param instanceof Double)
							paramClazzes[paramCount] = Double.TYPE;
						else if(param instanceof Float)
							paramClazzes[paramCount] = Float.TYPE;
						else if(param instanceof Byte)
							paramClazzes[paramCount] = Byte.TYPE;
						else
							paramClazzes[paramCount] = param.getClass();
						
						paramCount++;
					}
					
					Method method = objClass.getDeclaredMethod(actionName, paramClazzes);
					method.setAccessible(true);
					retVal = method.invoke(objInstance, params);
					method.setAccessible(false);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		return retVal;
	}

	public MBeanInfo getMBeanInfo() 
	{
		return info;
	}
	
}