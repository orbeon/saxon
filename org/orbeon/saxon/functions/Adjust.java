package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.value.DateTimeValue;
import org.orbeon.saxon.value.DateValue;
import org.orbeon.saxon.value.TimeValue;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.CalendarValue;
import org.orbeon.saxon.value.SecondsDurationValue;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.XPathException;

/**
* This class implements the XPath 2.0 functions
 * adjust-date-to-timezone(), adjust-time-timezone(), and adjust-dateTime-timezone().
*/


public class Adjust extends SystemFunction {

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the implicit timezone is not known statically)
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(c);
        if (av1==null) {
            return null;
        }
        CalendarValue in = (CalendarValue)av1.getPrimitiveValue();

        int nargs = argument.length;
        SecondsDurationValue tz = null;
        if (nargs==1) {
            // use the implicit timezone
            tz = (SecondsDurationValue)new DateTimeValue(c).getComponent(Component.TIMEZONE);
            return in.setTimezone(tz);
        } else {
            AtomicValue av2 = (AtomicValue)argument[1].evaluateItem(c);
            if (av2==null) {
                return in.removeTimezone();
            }
            tz = (SecondsDurationValue)av2.getPrimitiveValue();
            return in.setTimezone(tz);
        }
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
