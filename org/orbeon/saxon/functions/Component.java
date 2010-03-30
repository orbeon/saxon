package org.orbeon.saxon.functions;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;

/**
 * This class supports the get_X_from_Y functions defined in XPath 2.0
 */

public class Component extends SystemFunction {

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOURS = 4;
    public static final int MINUTES = 5;
    public static final int SECONDS = 6;
    public static final int TIMEZONE = 7;
    public static final int LOCALNAME = 8;
    public static final int NAMESPACE = 9;
    public static final int PREFIX = 10;
    public static final int MICROSECONDS = 11;   // internal use only
    public static final int WHOLE_SECONDS = 12;  // internal use only
    public static final int YEAR_ALLOWING_ZERO = 13;  // internal use only

    int component;

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        component = (operation >> 16) & 0xffff;
        return super.simplify(visitor);
    }

    /**
     * Get the required component
     */

    public int getRequiredComponent() {
        return component;
    }

    /**
     * Get the required component name as a string
     */

    public String getRequiredComponentAsString() {
        String[] components = {"", "YEAR", "MONTH", "DAY", "HOURS", "MINUTES", "SECONDS",
                               "TIMEZONE", "LOCALNAME", "NAMESPACE", "PREFIX", "MICROSECONDS",
                               "WHOLE_SECONDS", "YEAR_ALLOWING_ZERO"};
        return components[component];
    }

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg = (AtomicValue)argument[0].evaluateItem(context);

        if (arg == null) {
            return null;
        }

        return arg.getComponent(component);

    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Component c = (Component)super.copy();
        c.component = (c.operation >> 16) & 0xffff;
        return c;
    }
}


//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
