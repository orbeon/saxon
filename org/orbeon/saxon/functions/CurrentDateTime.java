package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.DateValue;
import net.sf.saxon.value.SecondsDurationValue;
import net.sf.saxon.value.TimeValue;

/**
* This class implements the XPath 2.0 functions
 * current-date(), current-time(), and current-dateTime(), as
 * well as the function implicit-timezone(). The value that is required
 * is inferred from the type of result required.
*/


public class CurrentDateTime extends SystemFunction {

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the value of the expression depends on the runtime context)
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        // current date/time is part of the context, but it is fixed for a transformation, so
        // we don't need to manage it as a dependency: expressions using it can be freely
        // rearranged
       return 0;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        DateTimeValue dt = DateTimeValue.getCurrentDateTime(context);
        int targetType = getItemType().getPrimitiveType();
        switch (targetType) {
            case Type.DATE_TIME:
                return dt;
            case Type.DATE:
                return (DateValue)dt.convert(Type.DATE);
            case Type.TIME:
                return (TimeValue)dt.convert(Type.TIME);
            case Type.DAY_TIME_DURATION:
            case Type.DURATION:
                return dt.getComponent(Component.TIMEZONE);
            default:
                throw new IllegalArgumentException("Wrong target type for current date/time");
        }
    }

    /**
     * Get the implicit timezone
     */

    public static SecondsDurationValue getImplicitTimezone(XPathContext context) throws XPathException {
        DateTimeValue dt = DateTimeValue.getCurrentDateTime(context);
        return (SecondsDurationValue)dt.getComponent(Component.TIMEZONE);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
