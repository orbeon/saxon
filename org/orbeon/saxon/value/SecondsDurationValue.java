package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.Configuration;

import java.util.StringTokenizer;
import java.io.PrintStream;

/**
* A value of type xsd:dayTimeDuration
*/

public final class SecondsDurationValue extends DurationValue {

    /**
    * Private constructor for internal use
    */

    private SecondsDurationValue() {
    }

    /**
    * Constructor: create a duration value from a supplied string, in
    * ISO 8601 format [+|-]PnDTnHnMnS
    */

    public SecondsDurationValue(CharSequence s) throws XPathException {

        StringTokenizer tok = new StringTokenizer(trimWhitespace(s).toString(), "-+.PDTHMS", true);
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
                    case 'M':
                        if (state < 4 || state > 5) badDuration("M is out of sequence", s);
                        minutes = value;
                        state = 6;
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

            normalize();

        } catch (NumberFormatException err) {
            badDuration("non-numeric or out-of-range component", s);
        }
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public String getStringValue() {

        // We need to normalize the representation

        double length = getLengthInSeconds();
        if (length<0) length = -length;

        long secs = (long)Math.floor(length);
        int millis = (int)((length % 1.0) * 1000);

        long s = secs % 60;
        long m = secs / 60;
        long h = m / 60;
        m = m % 60;
        long d = h / 24;
        h = h % 24;

        StringBuffer sb = new StringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append('P');
        if (d != 0) {
            sb.append(d);
            sb.append('D');
        }
        if ( d==0 || h!=0 || m!=0 || s!=0 || millis!=0) {
            sb.append('T');
        }
        if (h != 0) {
            sb.append(h);
            sb.append('H');
        }
        if (m != 0) {
            sb.append(m);
            sb.append('M');
        }
        if (s != 0 || millis != 0 || (d==0 && m==0 && h==0)) {
            sb.append(s);
            if (milliseconds!=0) {
                sb.append('.');
                DateTimeValue.appendString(sb, millis, 3);
            }
            sb.append('S');
        }
        return sb.toString();

    }

    /**
    * Normalize the value, for example 90M becomes 1H30M
    */

    public void normalize() throws DynamicError {
        long seconds2 = seconds;
        long minutes2 = minutes;
        long hours2 = hours;
        long days2 = days;
        if (milliseconds >= 1000) {
            seconds2 += (milliseconds / 1000);
            milliseconds = milliseconds % 1000;
        }
        if (seconds >= 60) {
            minutes2 += (seconds2 / 60);
            seconds2 = (int)(seconds2 % 60);
        }
        if (minutes2 >= 60) {
            hours2 += (minutes2 / 60);
            minutes2 = (int)(minutes2 % 60);
        }
        if (hours2 >= 24) {
            days2 += (hours2 / 24);
            if (days2 > Integer.MAX_VALUE || days2 < Integer.MIN_VALUE) {
                throw new DynamicError("Duration exceeds implementation-defined limits");
            }
            hours2 = (int)(hours2 % 24);
        }
        days = (int)days2;
        hours = (int)hours2;
        minutes = (int)minutes2;
        seconds = (int)seconds2;
    }

    /**
    * Get length of duration in seconds
    */

    public double getLengthInSeconds() {
        double a = days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a += ((double)milliseconds / 1000);
        // System.err.println("Duration length " + days + "/" + hours + "/" + minutes + "/" + seconds + " is " + a);
        return (negative ? -a : a);
    }

    /**
     * Get length of duration in milliseconds, as a long
     */

    public long getLengthInMilliseconds() {
        long a = days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a = a*1000 + milliseconds;
        return (negative ? -a : a);
    }


    /**
    * Construct a duration value as a number of seconds.
    */

    public static SecondsDurationValue fromSeconds(double seconds) throws XPathException {
        SecondsDurationValue sdv = new SecondsDurationValue();
        sdv.negative = (seconds<0);
        sdv.seconds = (int)(seconds<0 ? -seconds : seconds);
        sdv.milliseconds = (int)((seconds % 1.0) * 1000);
        sdv.normalize();
        return sdv;
    }

    /**
    * Construct a duration value as a number of milliseconds.
    */

    public static SecondsDurationValue fromMilliseconds(long milliseconds) throws XPathException {
        SecondsDurationValue sdv = new SecondsDurationValue();
        sdv.negative = (milliseconds<0);
        long seconds = Math.abs(milliseconds)/1000;
        sdv.days = (int)(seconds / (3600*24));
        sdv.seconds = (int)(seconds % (3600*24));
        sdv.milliseconds = (int)(milliseconds % 1000);
        sdv.normalize();
        return sdv;
    }


    /**
    * Multiply duration by a number
    */

    public DurationValue multiply(double n, XPathContext context) throws XPathException {
        return fromMilliseconds((long)(getLengthInMilliseconds() * n));
    }

    /**
     * Find the ratio between two durations
     * @param other the dividend
     * @return the ratio, as a double
     * @throws XPathException
     */
    public DoubleValue divide(DurationValue other, XPathContext context) throws XPathException {
        if (other instanceof SecondsDurationValue) {
            long v1 = this.getLengthInMilliseconds();
            long v2 = ((SecondsDurationValue)other).getLengthInMilliseconds();
            long mask = (long)1;
            while ((v1&mask) == 0 && ((v2&mask) == 0 && (v1 != 0) && (v2 != 0))) {
                v1 = v1/2;
                v2 = v2/2;
            }
            return new DoubleValue(
                    ((double)v1) / (double)v2);
        } else {
            throw new DynamicError("Cannot divide two durations of different type");
        }
    }

    /**
    * Add two dayTimeDurations
    */

    public DurationValue add(DurationValue other, XPathContext context) throws XPathException {
        if (other instanceof SecondsDurationValue) {
            return fromMilliseconds(this.getLengthInMilliseconds() +
                    ((SecondsDurationValue)other).getLengthInMilliseconds());
        } else {
            throw new DynamicError("Cannot add two durations of different type");
        }
    }

    /**
    * Subtract two dayTime-durations
    */

    public DurationValue subtract(DurationValue other, XPathContext context) throws XPathException {
        if (other instanceof SecondsDurationValue) {
            return fromMilliseconds(this.getLengthInMilliseconds() -
                    ((SecondsDurationValue)other).getLengthInMilliseconds());
        } else {
            throw new DynamicError("Cannot add two durations of different type");
        }
    }


    /**
    * Determine the data type of the exprssion
    * @return Type.DAY_TIME_DURATION,
    */

    public ItemType getItemType() {
        return Type.DAY_TIME_DURATION_TYPE;
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
            throw new DynamicError("Conversion of dayTimeDuration to " + target.getName() +
                        " is not supported");
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

