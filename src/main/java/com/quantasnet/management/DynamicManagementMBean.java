/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Andrew Landsverk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.quantasnet.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * @param objInstance The instance of the object to be Managed
     * @param description Description of the object for JMX
     */
    public DynamicManagementMBean(final Object objInstance, final String description)
    {
        this.objInstance = objInstance;

        objClass = objInstance.getClass();

        // We also want methods from parent classes that may be annotated
        final List<Method> methods = getMethods(objClass);

        final Field[] fields = objClass.getDeclaredFields();
        final Constructor<?>[] constructors = objClass.getDeclaredConstructors();

        attributes = null;
        operations = null;
        mgmtConstructors = null;
        notifications = null;    //NYI

        createMBeans(methods, fields, constructors);

        info = new MBeanInfo(this.objClass.getName(), description, attributes, mgmtConstructors, operations, notifications);
    }

    private void createMBeans(final List<Method> methods, final Field[] fields, final Constructor<?>[] constructors)
    {
        final List<MBeanAttributeInfo> attrList = new ArrayList<MBeanAttributeInfo>();
        final List<MBeanOperationInfo> operList = new ArrayList<MBeanOperationInfo>();
        final List<MBeanConstructorInfo> consList = new ArrayList<MBeanConstructorInfo>();

        //Parse the annotations for all the methods
        for (final Method method : methods)
        {
            final Managed mgmt = method.getAnnotation(Managed.class);
            if (mgmt != null)
            {
                operList.add(new MBeanOperationInfo(mgmt.description(), method));
            }
        }

        //Parse the annotations for all the fields
        for (final Field field : fields)
        {
            final Managed mgmt = field.getAnnotation(Managed.class);
            if (mgmt != null)
            {
                attrList.add(new MBeanAttributeInfo(field.getName(), field.getType().getName(), mgmt.description(), mgmt.readable(), mgmt.writable(), false));
            }
        }

        //Parse the annotations for all the constructors
        for (final Constructor<?> constructor : constructors)
        {
            final Managed mgmt = constructor.getAnnotation(Managed.class);
            if (mgmt != null)
            {
                consList.add(new MBeanConstructorInfo(mgmt.description(), constructor));
            }
        }

        if (!attrList.isEmpty())
        {
            attributes = new MBeanAttributeInfo[attrList.size()];
            attrList.toArray(attributes);
        }

        if (!operList.isEmpty())
        {
            operations = new MBeanOperationInfo[operList.size()];
            operList.toArray(operations);
        }

        if (!consList.isEmpty())
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

            for (final MBeanAttributeInfo mbeanAttr : attributes)
            {
                if (mbeanAttr.getName().equals(attribute))
                {
                    foundAttribute = true;

                    if (mbeanAttr.isReadable())
                    {
                        final Field field = objClass.getDeclaredField(attribute);

                        final boolean isAccessible = field.isAccessible();
                        if (!isAccessible)
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

            if (!foundAttribute)
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

            for (final MBeanAttributeInfo mbeanAttr : attributes)
            {
                if (mbeanAttr.getName().equals(attribute.getName()))
                {
                    foundAttribute = true;

                    if (mbeanAttr.isWritable())
                    {
                        final Field field = objClass.getDeclaredField(attribute.getName());
                        final boolean isAccessible = field.isAccessible();
                        if (!isAccessible)
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

            if (!foundAttribute)
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
        final AttributeList values = new AttributeList();

        for (final String attribute : attributes)
        {
            try
            {
                final Object value = getAttribute(attribute);
                final Attribute attr = new Attribute(attribute, value);

                values.add(attr);
            }
            catch (Exception e)
            {
                LOG.error("Error getting attribute: " + attribute, e);
            }
        }

        return values;
    }

    public AttributeList setAttributes(final AttributeList attributes)
    {
        final AttributeList retList = new AttributeList();

        for (final Object attr : attributes)
        {
            try
            {
                setAttribute((Attribute) attr);
                retList.add(getAttribute(((Attribute) attr).getName()));
            }
            catch (Exception e)
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

        for (final MBeanOperationInfo mbOperInfo : operations)
        {
            if (mbOperInfo.getName().equals(actionName))
            {
                foundMethod = true;

                try
                {
                    int paramCount = 0;

                    final Class<?>[] paramClazzes = getClasses(params, signature, paramCount);

                    Method method = null;
                    try
                    {
                        method = objClass.getMethod(actionName, paramClazzes);
                    }
                    catch (NoSuchMethodException nsme)
                    {
                        // nothing, move on
                    }

                    if(method == null)
                    {
                        method = findMethod(objClass, actionName, paramClazzes);
                    }

                    if(method == null)
                    {
                        throw new Exception("Could not find method " + actionName);
                    }
                    else
                    {
                        method.setAccessible(true);

                        retVal = method.invoke(objInstance, params);

                        method.setAccessible(false);
                    }
                }
                catch (Exception e)
                {
                    final String errorText = "Error invoking " + actionName;

                    LOG.error(errorText, e);
                    throw new MBeanException(e, errorText);
                }
            }
        }

        if (!foundMethod)
        {
            final String errorText = "No such method known to JMX: " + actionName;

            final Exception exc = new Exception(errorText);
            LOG.error(errorText, exc);

            throw new MBeanException(exc);
        }

        return retVal;
    }

    public MBeanInfo getMBeanInfo()
    {
        return info;
    }

    private Class<?>[] getClasses(Object[] params, String[] signature, int paramCount) throws ClassNotFoundException
    {
        final Class<?>[] paramClazzes = new Class<?>[params.length];

        for (final String param : signature)
        {
            if ("boolean".equals(param))
            {
                paramClazzes[paramCount] = Boolean.TYPE;
            }
            else if ("int".equals(param))
            {
                paramClazzes[paramCount] = Integer.TYPE;
            }
            else if ("char".equals(param))
            {
                paramClazzes[paramCount] = Character.TYPE;
            }
            else if ("long".equals(param))
            {
                paramClazzes[paramCount] = Long.TYPE;
            }
            else if ("double".equals(param))
            {
                paramClazzes[paramCount] = Double.TYPE;
            }
            else if ("float".equals(param))
            {
                paramClazzes[paramCount] = Float.TYPE;
            }
            else if ("byte".equals(param))
            {
                paramClazzes[paramCount] = Byte.TYPE;
            }
            else
            {
                paramClazzes[paramCount] = Class.forName(param);
            }

            paramCount++;
        }

        return paramClazzes;
    }

    private List<Method> getMethods(final Class<?> objClass)
    {
        final List<Method> retMethods = new ArrayList<Method>();

        if(!(objClass == Object.class))
        {
            retMethods.addAll(Arrays.asList(objClass.getDeclaredMethods()));

            // recurse until objClass == Object.class
            retMethods.addAll(getMethods(objClass.getSuperclass()));
        }

        return retMethods;
    }

    private Method findMethod(final Class<?> objClass, final String methodName, final Class<?>[] paramClazzes)
    {
        Method retMethod = null;

        if(!(objClass == Object.class))
        {
            try
            {
                retMethod = objClass.getDeclaredMethod(methodName, paramClazzes);
            }
            catch (Exception e)
            {
                // nothing
            }

            if(retMethod == null)
            {
                // recurse until we are at Object.class
                retMethod = findMethod(objClass.getSuperclass(), methodName, paramClazzes);
            }
        }

        return retMethod;
    }
}