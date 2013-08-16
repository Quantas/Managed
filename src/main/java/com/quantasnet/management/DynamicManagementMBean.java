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
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class takes the object passed in and parses all the @Managed annotations for the constructors/methods/fields<br />
 * Once all the annotations are found it will then build the MBeanInfo object containing the methods/fields/constructors<br />
 * This class should not be used directly, but invoked through the ManagementProcessor's register method, which will call<br />
 * this class to create the object then register it with the PlatformMBeanServer.<br />
 * <br />
 * <p/>
 * TODO class needs some refactoring
 *
 * @author Quantas
 */
/*package*/ final class DynamicManagementMBean implements DynamicMBean
{
    private static final Logger LOG = LoggerFactory.getLogger(DynamicManagementMBean.class);

    private static final String SET = "set";
    private static final String GET = "get";
    private static final String IS = "is";

    private Object objInstance;
    private Class<?> objClass;
    private MBeanInfo info;

    private MBeanAttributeInfo[] attributes;
    private MBeanOperationInfo[] operations;
    private MBeanConstructorInfo[] mgmtConstructors;

    // Notifications are Not Implemented at this time
    //private MBeanNotificationInfo[] notifications;

    private final Map<String, AttributeWithMethods> attributeMethodMap = new HashMap<String, AttributeWithMethods>();

    private final Map<Method, String> methodMap = new HashMap<Method, String>();

    /**
     * @param objInstance The instance of the object to be Managed
     * @param description Description of the object for JMX
     */
    public DynamicManagementMBean(final Object objInstance, final String description)
    {
        this.objInstance = objInstance;

        objClass = objInstance.getClass();

        final List<Method> methods = getMethods(objClass);

        final Field[] fields = objClass.getDeclaredFields();
        final Constructor<?>[] constructors = objClass.getDeclaredConstructors();

        attributes = null;
        operations = null;
        mgmtConstructors = null;

        createMBeans(methods, fields, constructors);

        info = new MBeanInfo(this.objClass.getName(), description, attributes, mgmtConstructors, operations, null/*notifications*/);
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
                        for (final Map.Entry<String, AttributeWithMethods> entry : attributeMethodMap.entrySet())
                        {
                            final String entryName = entry.getKey();
                            final AttributeWithMethods attributeWithMethods = entry.getValue();

                            if (entryName.equals(attribute))
                            {
                                final Method method = attributeWithMethods.getGetOrIsMethod();
                                final boolean isAccessible = method.isAccessible();
                                if (!isAccessible)
                                {
                                    method.setAccessible(true);
                                }

                                value = method.invoke(objInstance, null);

                                method.setAccessible(isAccessible);

                                return value;
                            }
                        }

                        //TODO maybe remove the extra reflection here
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

            boolean alreadySet = false;

            for (final MBeanAttributeInfo mbeanAttr : attributes)
            {
                if (mbeanAttr.getName().equals(attribute.getName()))
                {
                    foundAttribute = true;

                    if (mbeanAttr.isWritable())
                    {
                        for (final Map.Entry<String, AttributeWithMethods> entry : attributeMethodMap.entrySet())
                        {
                            final String entryName = entry.getKey();
                            final AttributeWithMethods attributeWithMethods = entry.getValue();

                            if (entryName.equals(attribute.getName()))
                            {
                                final Method method = attributeWithMethods.getSetMethod();
                                final boolean isAccessible = method.isAccessible();
                                if (!isAccessible)
                                {
                                    method.setAccessible(true);
                                }

                                method.invoke(objInstance, attribute.getValue());

                                method.setAccessible(isAccessible);

                                alreadySet = true;

                                break;
                            }
                        }

                        if (!alreadySet)
                        {
                            final Field field = objClass.getDeclaredField(attribute.getName());
                            final boolean isAccessible = field.isAccessible();
                            if (!isAccessible)
                            {
                                field.setAccessible(true);
                            }

                            //TODO maybe remove the extra reflection here
                            field.set(objInstance, attribute.getValue());

                            field.setAccessible(isAccessible);
                        }
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

                    final Class<?>[] paramClazzes = getParamClasses(params, signature, paramCount);

                    Method method = null;

                    for (final Map.Entry<Method, String> entry : methodMap.entrySet())
                    {
                        final Method mapMethod = entry.getKey();
                        final Class<?>[] mapMethodParams = mapMethod.getParameterTypes();
                        if (actionName.equals(entry.getValue()) && mapMethodParams.length == paramClazzes.length)
                        {
                            boolean correctMethod = true;
                            // we found a method with same name and same param count
                            // double check it is the correct method
                            for (int i = 0; i < mapMethodParams.length; i++)
                            {
                                if (!(mapMethodParams[i] == paramClazzes[i]))
                                {
                                    correctMethod = false;
                                    break;
                                }
                            }

                            if (correctMethod)
                            {
                                method = mapMethod;
                            }
                        }
                    }

                    if (method == null)
                    {
                        throw new Exception("Could not find method " + actionName);
                    }
                    else
                    {
                        method.setAccessible(true);
                        retVal = method.invoke(objInstance, params);
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

    ////////////////////////////////////////////////////////
    //  Private Methods
    ////////////////////////////////////////////////////////

    /**
     * TODO this method needs refactoring
     *
     * @param methods
     * @param fields
     * @param constructors
     */
    private void createMBeans(final List<Method> methods, final Field[] fields, final Constructor<?>[] constructors)
    {
        final List<MBeanAttributeInfo> attrList = new ArrayList<MBeanAttributeInfo>();
        final List<MBeanOperationInfo> operList = new ArrayList<MBeanOperationInfo>();
        final List<MBeanConstructorInfo> consList = new ArrayList<MBeanConstructorInfo>();

        final List<String> methodAttr = new ArrayList<String>();

        //Parse the annotations for all the methods
        for (final Method method : methods)
        {
            final Managed mgmt = method.getAnnotation(Managed.class);
            if (mgmt != null)
            {
                if (checkGetSetIs(method))
                {
                    final String attributeName = getAttributeNameFromMethod(method);

                    if (!methodAttr.contains(attributeName))
                    {
                        Method first = null;
                        final Method other = findOtherMethod(attributeName, method, methods);

                        final String methodName = method.getName();

                        boolean firstGetter = false;

                        if (methodName.startsWith(GET) || methodName.startsWith(IS) && mgmt.readable())
                        {
                            firstGetter = true;
                            first = method;
                        }
                        else if (methodName.startsWith(SET))
                        {
                            first = method;
                        }

                        if (other != null && (other.getName().startsWith(GET) || other.getName().startsWith(IS)))
                        {
                            firstGetter = false;
                        }

                        try
                        {
                            MBeanAttributeInfo attrInfo = null;
                            AttributeWithMethods attributeWithMethods = null;

                            if (firstGetter)
                            {
                                attrInfo = new MBeanAttributeInfo(attributeName, mgmt.description(), first, other);
                                attributeWithMethods = new AttributeWithMethods(attrInfo, first, other);
                            }
                            else
                            {
                                attrInfo = new MBeanAttributeInfo(attributeName, mgmt.description(), other, first);
                                attributeWithMethods = new AttributeWithMethods(attrInfo, other, first);
                            }

                            attributeMethodMap.put(attributeName, attributeWithMethods);
                            attrList.add(attrInfo);
                            methodAttr.add(attributeName);
                        }
                        catch (IntrospectionException ie)
                        {
                            LOG.error("Error creating attribute from get/set/is methods for " + attributeName, ie);
                        }
                    }
                }
                else
                {
                    operList.add(new MBeanOperationInfo(mgmt.description(), method));
                    methodMap.put(method, method.getName());
                }
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

    /**
     * Find a matching method for a getter/setter/is method, ie, if the first method was a get, find the set.
     *
     * @param attributeName Name of the attribute to search for
     * @param method        Original Method
     * @param methods       List of all the available Methods
     * @return Method to match first method, may be null
     */
    private Method findOtherMethod(final String attributeName, final Method method, final List<Method> methods)
    {
        final String methodName = method.getName();

        boolean needSet = methodName.startsWith(GET) || methodName.startsWith(IS);

        for (final Method listMethod : methods)
        {
            final Managed mgmt = listMethod.getAnnotation(Managed.class);

            if (mgmt != null)
            {
                final String listMethodName = listMethod.getName();

                boolean foundCorrectMethod = false;

                if (needSet && listMethodName.startsWith(SET))
                {
                    foundCorrectMethod = true;
                }
                else if (!needSet && (listMethodName.startsWith(GET) || listMethodName.startsWith(IS)))
                {
                    foundCorrectMethod = true;
                }

                if (foundCorrectMethod)
                {
                    final String listAttributeName = getAttributeNameFromMethod(listMethod);
                    if (listAttributeName.equals(attributeName))
                    {
                        // do some real logic, lol
                        if ((listMethodName.startsWith(GET) || listMethodName.startsWith(IS)) && mgmt.readable())
                        {
                            return listMethod;
                        }
                        else if ((listMethodName.startsWith(SET)))
                        {
                            return listMethod;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Return the attribute name from a getter/setter/is method
     *
     * @param method Method to check
     * @return String attribute name
     */
    private String getAttributeNameFromMethod(final Method method)
    {
        final String methodName = method.getName();

        String retString;

        if (methodName.startsWith(GET) || methodName.startsWith(SET))
        {
            retString = methodName.substring(3);
        }
        else
        {
            // assume startsWith "is"
            retString = methodName.substring(2);
        }

        return retString.substring(0, 1).toLowerCase() + retString.substring(1);
    }

    private boolean checkGetSetIs(final Method method)
    {
        final String methodName = method.getName();
        return methodName.startsWith(GET) || methodName.startsWith(SET) || methodName.startsWith(IS);
    }

    /**
     * Get the parameter classes, including support for primitives
     *
     * @param params
     * @param signature
     * @param paramCount
     * @return
     * @throws ClassNotFoundException
     */
    private Class<?>[] getParamClasses(Object[] params, String[] signature, int paramCount) throws ClassNotFoundException
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

    /**
     * We also want methods from parent classes that may be annotated,
     * so we recurse through the class's hierarchy until we are at the top,
     * meaning Object
     *
     * @param objClass
     * @return
     */
    private List<Method> getMethods(final Class<?> objClass)
    {
        final List<Method> retMethods = new ArrayList<Method>();

        if (!(objClass == Object.class))
        {
            retMethods.addAll(Arrays.asList(objClass.getDeclaredMethods()));

            // recurse until objClass == Object.class
            retMethods.addAll(getMethods(objClass.getSuperclass()));
        }

        return retMethods;
    }
}