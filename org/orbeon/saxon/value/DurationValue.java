package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.util.StringTokenizer;

/**
* A value of type xs:duration
*/

public class DurationValue extends AtomicValue implements Comparable {

    protected boolean negative = false;
    protected int years = 0;
    protected int months = 0;
    protected int days = 0;
    protected int hours = 0;
    protected int minutes = 0;
    protected int seconds = 0;     // used to represent xdt:dayTimeDuration
    protected int milliseconds = 0;

    /**
    * Private constructor for internal use
    */

    protected DurationValue() {
    }

    /**
    * Constructor: create a duration value from a supplied string, in
    * ISO 8601 format [+|-]PnYnMnDTnHnMnS
    */

    public DurationValue(CharSequence s) throws XPathException {
        // TODO: use regular expressions instead
        StringTokenizer tok = new StringTokenizer(trimWhitespace(s).toString(), "-+.PYMDTHS", true);
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
                    case 'D':
                        if (state > 2) badDuration("D is out of sequence", s);
                        days = value;
                        state = 3;
                        break;
                    case 'H':
                        if (state != 4) badDuration("H is out of sequence", s);
                        hours = value;
                        state = 5;
                        break;
                    case '.':
                        if (state > 6) badDuration("misplaced decimal point", s);
                        seconds = value;
                        state = 7;
                        break;
                    case 'S':
                        if (state > 7) badDuration("S is out of sequence", s);
                        if (state==7) {
                            while (part.length() < 3) part += "0";
                            if (part.length() > 3) part = part.substring(0, 3);
                            milliseconds = Integer.parseInt(part);
                        } else {
                            seconds = value;
                        }
                        state = 8;
                        break;
                   default:
                        badDuration("misplaced " + delim, s);
                }
            }


        } catch (NumberFormatException err) {
            badDuration("non-numeric component", s);
        }
    }

    protected void badDuration(String msg, CharSequence s) throws XPathException {
        DynamicError err = new DynamicError("Invalid duration value '" + s + "' (" + msg + ")");
        err.setErrorCode("FORG0001");
        throw err;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type
    * @throws XPathException if the conversion is not possible
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        //System.err.println("Convert duration " + getClass() + " to " + Type.getTypeName(requiredType));
        switch(requiredType) {
        case Type.DURATION:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;
        case Type.STRING:
            return new StringValue(getStringValue());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        case Type.YEAR_MONTH_DURATION:
            if (days!=0 || hours!=0 || minutes!=0 || seconds!=0 || milliseconds!=0) {
                DynamicError err = new DynamicError(
                        "Cannot convert to yearMonthDuration because some components are non-zero");
                err.setXPathContext(context);
                err.setErrorCode("FORG0001");
                throw err;
            } else {
                return MonthDurationValue.fromMonths((years*12 + months) * (negative ? -1 : +1));
            }
        case Type.DAY_TIME_DURATION:
            if (years!=0 || months!=0) {
                DynamicError err = new DynamicError(
                        "Cannot convert to dayTimeDuration because some components are non-zero");
                err.setXPathContext(context);
                err.setErrorCode("FORG0001");
                throw err;
            } else {
                return SecondsDurationValue.fromSeconds(getLengthInSeconds());
            }
        default:
            DynamicError err = new DynamicError("Cannot convert duration to " +
                                     StandardNames.getDisplayName(requiredType));
            err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public String getStringValue() {

        // Note, Schema does not define a canonical representation. We output all components.

        StringBuffer sb = new StringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append('P');
        sb.append(years);
        sb.append('Y');
        sb.append(months);
        sb.append('M');
        sb.append(days);
        sb.append('D');
        sb.append('T');
        sb.append(hours);
        sb.append('H');
        sb.append(minutes);
        sb.append('M');
        sb.append(seconds);
        if (milliseconds!=0) {
            sb.append('.');
            DateTimeValue.appendString(sb, milliseconds, 3);
        }
        sb.append('S');
        return sb.toString();

    }

    /**
    * Normalize the value, for example 90M becomes 1H30M
    */

//    public void normalize() {
//        if (milliseconds >= 1000) {
//            seconds += (milliseconds / 1000);
//            milliseconds = milliseconds % 1000;
//        }
//        if (seconds >= 60) {
//            minutes += (seconds / 60);
//            seconds = seconds % 60;
//        }
//        if (minutes >= 60) {
//            hours += (minutes / 60);
//            minutes = minutes % 60;
//        }
//        if (hours >= 24) {
//            days += (hours / 24);
//            hours = hours % 24;
//        }
//        if (months >= 12) {
//            years += (months / 12);
//            months = months % 12;
//        }
//    }

    /**
    * Get length of duration in seconds, assuming an average length of month. (Note, this defines a total
    * ordering on durations which is different from the partial order defined in XML Schema; XPath 2.0
    * currently avoids defining an ordering at all. But the ordering here is consistent with the ordering
    * of the two duration subtypes in XPath 2.0.)
    */

    public double getLengthInSeconds() {
        double a = years;
        a = a*12 + months;
        a = a*(365.242199/12.0) + days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a = a + ((double)milliseconds / 1000);
        return (negative ? -a : a);
    }

    /**
    * Determine the data type of the exprssion
    * @return Type.DURATION,
    */

    public ItemType getItemType() {
        return Type.DURATION_TYPE;
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
            Object o = super.convertToJava(target, config, context);
            if (o == null) {
                DynamicError err = new DynamicError("Conversion of duration to " + target.getName() +
                        " is not supported");
                err.setXPathContext(context);
                err.setErrorCode("SAXON:0000");
            }
            return o;
        }
    }

    /**
    * Get a component of the value
    */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.YEAR:
            return new IntegerValue((negative?-years:years));
        case Component.MONTH:
            return new IntegerValue((negative?-months:months));
        case Component.DAY:
            return new IntegerValue((negative?-days:days));
        case Component.HOURS:
            return new IntegerValue((negative?-hours:hours));
        case Component.MINUTES:
            return new IntegerValue((negative?-minutes:minutes));
        case Component.SECONDS:
            StringBuffer sb = new StringBuffer(16);
            String ms = ("000" + milliseconds);
            ms = ms.substring(ms.length()-3);
            sb.append((negative?"-":"") + seconds+'.'+ms);
            return new DecimalValue(sb);
        default:
            throw new IllegalArgumentException("Unknown component for duration: " + component);
        }
    }


    /**
    * Compare the value to another duration value
    * @param other The other dateTime value
    * @return negative value if this one is the shorter duration, 0 if they are equal,
    * positive value if this one is the longer duration. For this purpose, a year is considered
    * to be equal to 365.242199 days.
    * @throws ClassCastException if the other value is not a DurationValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        if (!(other instanceof DurationValue)) {
            throw new ClassCastException("Duration values are not comparable to " + other.getClass());
        }
        double s1 = this.getLengthInSeconds();
        double s2 = ((DurationValue)other).getLengthInSeconds();
        if (s1==s2) return 0;
        if (s1<s2) return -1;
        return +1;
    }

    /**
    * Test if the two durations are of equal length. Note: this function is defined
    * in XPath 2.0, but its semantics are currently unclear.
    */

    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        return new Double(getLengthInSeconds()).hashCode();
    }

    /**
    * Add two durations
    */

    public DurationValue add(DurationValue other, XPathContext context) throws XPathException {
        throw new DynamicError("Only subtypes of xs:duration can be added");
    }

    /**
    * Subtract two durations
    */

    public DurationValue subtract(DurationValue other, XPathContext context) throws XPathException{
        throw new DynamicError("Only subtypes of xs:duration can be subtracted");
    }

    /**
    * Multiply a duration by a number
    */

    public DurationValue multiply(double factor, XPathContext context) throws XPathException {
        throw new DynamicError("Only subtypes of xs:duration can be multiplied by a number");
    }

    /**
    * Divide a duration by a number
    */

    public DoubleValue divide(DurationValue other, XPathContext context) throws XPathException {
        throw new DynamicError("Only subtypes of xs:duration can be divided by another duration");
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

