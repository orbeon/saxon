package org.orbeon.saxon.sort;
import org.orbeon.saxon.om.FastStringBuffer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Comparer that treats strings as an alternating sequence of alpha parts and numeric parts. The
 * alpha parts are compared using a base collation supplied as a parameter; the numeric parts are
 * compared numerically. "Numeric" here means a sequence of consecutive ASCII digits 0-9.
 * <p>
 * Note: this StringCollator produces an ordering that is not compatible with equals().
 * </p>
 */

public class AlphanumericCollator implements StringCollator, java.io.Serializable {

    private StringCollator baseCollator;
    private static Pattern pattern = Pattern.compile("\\d+");

    /**
     * Create an alphanumeric collation
     * @param base the collation used to compare the alphabetic parts of the string
     */

    public AlphanumericCollator(StringCollator base) {
        baseCollator = base;
    }

    /**
    * Compare two objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    */

    public int compareStrings(String s1, String s2) {
        int pos1 = 0;
        int pos2 = 0;
        Matcher m1 = pattern.matcher(s1);
        Matcher m2 = pattern.matcher(s2);
        while (true) {

            // find the next number in each string

            boolean b1 = m1.find(pos1);
            boolean b2 = m2.find(pos2);
            int m1start = (b1 ? m1.start() : s1.length());
            int m2start = (b2 ? m2.start() : s2.length());

            // compare an alphabetic pair (even if zero-length)

            int c = baseCollator.compareStrings(s1.substring(pos1, m1start), s2.substring(pos2, m2start));
            if (c != 0) {
                return c;
            }

            // if one match found a number and the other didn't, exit accordingly

            if (b1 && !b2) {
                return +1;
            } else if (b2 && !b1) {
                return -1;
            } else if (!b1 && !b2) {
                return 0;
            }

            // a number was found in each of the strings: compare the numbers

            int n1 = Integer.parseInt(s1.substring(m1start, m1.end()));
            int n2 = Integer.parseInt(s2.substring(m2start, m2.end()));
            if (n1 != n2) {
                return (n1 - n2);
            }

            // the numbers are equal: move on to the next part of the string

            pos1 = m1.end();
            pos2 = m2.end();
        }
    }

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     */

    public Object getCollationKey(String s) {
        // The string is normalized by removing leading zeros in a numeric component
        FastStringBuffer sb = new FastStringBuffer(s.length()*2);
        int pos1 = 0;
        Matcher m1 = pattern.matcher(s);
        while (true) {

            // find the next number in the string

            boolean b1 = m1.find(pos1);
            int m1start = (b1 ? m1.start() : s.length());

            // handle an alphabetic part (even if zero-length)

            sb.append(s.substring(pos1, m1start));

            // reached end?

            if (!b1) {
                return sb.toString();
            }

            // handle a numeric part

            int n1 = Integer.parseInt(s.substring(m1start, m1.end()));
            sb.append(n1 + "");

            // move on to the next part of the string

            pos1 = m1.end();
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
// Contributor(s): none
//