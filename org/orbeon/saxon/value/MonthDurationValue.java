package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.util.StringTokenizer;

/**
* A value of type xsd:yearMonthDuration
*/

public final class MonthDurationValue extends DurationValue implements Comparable {

    /**
    * Private constructor for internal use
    */

    private MonthDurationValue() {
    }

    /**
    * Constructor: create a duration value from a supplied string, in
    * ISO 8601 format [+|-]PnYnM
    */

    public MonthDurationValue(CharSequence s) throws XPathException {

        StringTokenizer tok = new StringTokenizer(trimWhitespace(s).toString(), "-+PYM", true);
        try {
            if (!tok.hasMoreElements()) badDuration("empty string", s);
            String part = (String)tok.nextElement();
            if ("+".equals(part)) {
                part = (String)tok.nextElement();
            } else if ("-".equals(part)) {
                negative = true;
                part = (String)tok.nextElement();
            }
            if (!"P".equals(part)) badDuration("missing 'P'", s);
            int state = 0;
            while (tok.hasMoreElements()) {
                part = (String)tok.nextElement();
                if ("T".equals(part)) {
                    state = 4;
                    part = (String)tok.nextElement();
                }
                int value = Integer.parseInt(part);
                if (!tok.hasMoreElements()) badDuration("missing unit letter at end", s);
                char delim = ((String)tok.nextElement()).charAt(0);
                switch (delim) {
                    case 'Y':
                        if (state > 0) badDuration("Y is out of sequence", s);
                        years = value;
                        state = 1;
                        break;
                    case 'M':
                        if (state == 4 || state==5) {
                            minutes = value;
                            state = 6;
                            break;
                        } else if (state == 0 || state==1) {
                            months = value;
                            state = 2;
                            break;
                        } else {
                            badDuration("M is out of sequence", s);
                        }

                   default:
                        badDuration("misplaced " + delim, s);
                }
            }
            normalize();

        } catch (NumberFormatException err) {
            badDuration("non-numeric component", s);
        }
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public String getStringValue() {

        // The canonical representation has months in the range 0-11

        int mm = years*12 + months;
        int y = mm / 12;
        int m = mm % 12;

        StringBuffer sb = new StringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append('P');
        if (y!=0) {
            sb.append(y);
            sb.append('Y');
        }
        if (m!=0 || y==0) {
            sb.append(m);
            sb.append('M');
        }
        return sb.toString();

    }

    /**
    * Normalize the value, for example 90M becomes 1H30M
    */

    public void normalize() {
        if (months >= 12) {
            years += (months / 12);
            months = months % 12;
        }
    }

    /**
    * Get the number of months in the duration
    */

    public int getLengthInMonths() {
        return (years*12 + months) * (negative ? -1 : +1);
    }

    /**
    * Construct a duration value as a number of months.
    */

    public static MonthDurationValue fromMonths(int months) {
        MonthDurationValue mdv = new MonthDurationValue();
        mdv.negative = (months<0);
        mdv.months = (months<0 ? -months : months);
        mdv.normalize();
        return mdv;
    }

    /**
    * Multiply duration by a number
    */

    public DurationValue multiply(double n, XPathContext context) {
        return fromMonths((int)(getLengthInMonths() * n));
    }

    /**
     * Find the ratio between two durations
     * @param other the dividend
     * @return the ratio, as a double
     * @throws XPathException
     */

    public DoubleValue divide(DurationValue other, XPathContext context) throws XPathException {
        if (other instanceof MonthDurationValue) {
            return new DoubleValue(
                    (double)this.getLengthInMonths() / (double)((MonthDurationValue)other).getLengthInMonths());
        } else {
            throw new DynamicError("Cannot divide two durations of different type");
        }
    }

    /**
    * Add two year-month-durations
    */

    public DurationValue add(DurationValue other, XPathContext context) throws XPathException {
        if (other instanceof MonthDurationValue) {
            return fromMonths(this.getLengthInMonths() +
                    ((MonthDurationValue)other).getLengthInMonths());
        } else {
            throw new DynamicError("Cannot add two durations of different type");
        }
    }

    /**
    * Subtract two year-month-durations
    */

    public DurationValue subtract(DurationValue other, XPathContext context) throws XPathException {
        if (other instanceof MonthDurationValue) {
            return fromMonths(this.getLengthInMonths() -
                    ((MonthDurationValue)other).getLengthInMonths());
        } else {
            throw new DynamicError("Cannot subtract two durations of different type");
        }
    }

    /**
    * Determine the data type of the exprssion
    * @return Type.YEAR_MONTH_DURATION,
    */

    public ItemType getItemType() {
        return Type.YEAR_MONTH_DURATION_TYPE;
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(DurationValue.class)) {
            return this;
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            throw new DynamicError("Conversion of yearMonthDuration to " + target.getName() +
                        " is not supported");
        }
    }


    /**
    * Compare the value to another duration value
    * @param other The other dateTime value
    * @return negative value if this one is the earler, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be UTC values (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        if (other instanceof MonthDurationValue) {
            return this.getLengthInMonths() - ((MonthDurationValue)other).getLengthInMonths();
        } else {
            return super.compareTo(other);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

