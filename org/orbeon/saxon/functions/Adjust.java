package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Container;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.CalendarValue;
import org.orbeon.saxon.value.SecondsDurationValue;

/**
* This class implements the XPath 2.0 functions
 * adjust-date-to-timezone(), adjust-time-timezone(), and adjust-dateTime-timezone().
*/


public class Adjust extends SystemFunction {

    int implicitTimezone; // implicit timezone as offset from UTC in minutes

    /**
    * Simplify and validate.
    */

    public Expression simplify(StaticContext env) throws XPathException {
        implicitTimezone = env.getConfiguration().getImplicitTimezone();
        return super.simplify(env);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        if (av1==null) {
            return null;
        }
        CalendarValue in = (CalendarValue)av1.getPrimitiveValue();

        int nargs = argument.length;
        SecondsDurationValue tz;
        if (nargs==1) {
            // use the implicit timezone
//            if (in.hasTimezone()) {
                return in.adjustTimezone(implicitTimezone);
//            } else {
//                in = in.copy();
//                in.setTimezoneInMinutes(implicitTimezone);
//                return in;
//            }
        } else {
            AtomicValue av2 = (AtomicValue)argument[1].evaluateItem(context);
            if (av2==null) {
                return in.removeTimezone();
            }
            tz = (SecondsDurationValue)av2.getPrimitiveValue();
            long microseconds = tz.getLengthInMicroseconds();
            if (microseconds%60000000 != 0) {
                DynamicError err = new DynamicError("Timezone is not an integral number of minutes");
                err.setErrorCode("FODT0003");
                err.setLocator(this);
                err.setXPathContext(context);
                throw err;
            }
            int tzminutes = (int)(microseconds / 60000000);
            if (Math.abs(tzminutes) > 14*60) {
                DynamicError err = new DynamicError("Timezone out of range (-14:00 to +14:00)");
                err.setErrorCode("FODT0003");
                err.setLocator(this);
                err.setXPathContext(context);
                throw err;
            }
//            if (in.hasTimezone()) {
                return in.adjustTimezone(tzminutes);
//            } else {
//                in = in.copy();
//                in.setTimezoneInMinutes(tzminutes);
//                return in;
//            }
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
