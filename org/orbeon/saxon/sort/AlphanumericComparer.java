package org.orbeon.saxon.sort;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A Comparer that treats strings as an alternating sequence of alpha parts and numeric parts. The
 * alpha parts are compared using a base collation supplied as a parameter; the numeric parts are
 * compared numerically. "Numeric" here means a sequence of consecutive ASCII digits 0-9.
 * <p>
 * Note: this Comparator produces an ordering that is not compatible with equals().
 * </p>
 */

public class AlphanumericComparer implements Comparator, java.io.Serializable {

    private Comparator baseComparer;
    private static Pattern pattern = Pattern.compile("\\d+");

    public AlphanumericComparer(Comparator base) {
        baseComparer = base;
    }

    /**
    * Compare two objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    */

    public int compare(Object a1, Object a2) {
        String s1 = a1.toString();
        String s2 = a2.toString();
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

            int c = baseComparer.compare(s1.substring(pos1, m1start), s2.substring(pos2, m2start));
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