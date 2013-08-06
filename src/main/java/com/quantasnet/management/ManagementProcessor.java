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

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for registering DynamicMBeans
 *
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
        catch (JMException e)
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
     *
     * @param obj  Instance of the object to be managed
     * @param desc Description of the MBean
     */
    public static void register(final Object obj, final String desc)
    {
        LOG.info("Registering MBean: " + obj.getClass().getSimpleName());
        Object instance = null;

        try
        {
            if (obj instanceof DynamicMBean)
            {
                instance = obj;
            }
            else
            {
                final DynamicManagementMBean mbean = new DynamicManagementMBean(obj, desc);
                if (mbean.getMBeanInfo() != null)
                {
                    instance = mbean;
                }
            }

            if (instance != null)
            {
                final ObjectName objName = constructObjectName(obj.getClass());

                ManagementFactory.getPlatformMBeanServer().registerMBean(instance, objName);

                synchronized (REGISTERED_OBJS)
                {
                    REGISTERED_OBJS.add(objName);
                }
            }
        }
        catch (Exception e)
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

            synchronized (REGISTERED_OBJS)
            {
                ObjectName toRemove = null;
                for (final ObjectName oName : REGISTERED_OBJS)
                {
                    if (oName.getCanonicalName().equals(objName.getCanonicalName()))
                    {
                        toRemove = oName;
                    }
                }

                if (toRemove != null)
                {
                    REGISTERED_OBJS.remove(toRemove);
                }
            }
        }
        catch (JMException e)
        {
            LOG.error("Error Unregistering the DynamicManagementMBean with the Factory", e);
        }
    }
}