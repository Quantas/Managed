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

import javax.management.MBeanAttributeInfo;
import java.lang.reflect.Method;

/**
 * Holding class for internal information
 */
/*package*/ class AttributeWithMethods
{
    final MBeanAttributeInfo mbeanAttribute;
    final Method getOrIsMethod;
    final Method setMethod;

    public AttributeWithMethods(final MBeanAttributeInfo mbeanAttribute, final Method getOrIsMethod, final Method setMethod)
    {
        this.mbeanAttribute = mbeanAttribute;

        if(getOrIsMethod == null)
        {
            this.getOrIsMethod = null;
        }
        else
        {
            this.getOrIsMethod = getOrIsMethod;
        }

        if(setMethod == null)
        {
            this.setMethod = null;
        }
        else
        {
            this.setMethod = setMethod;
        }
    }

    public MBeanAttributeInfo getMbeanAttribute()
    {
        return mbeanAttribute;
    }

    public Method getSetMethod()
    {
        return setMethod;
    }

    public Method getGetOrIsMethod()
    {
        return getOrIsMethod;
    }
}
