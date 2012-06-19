package org.orbeon.saxon.trans;

import java.io.Serializable;

/**
 * This class is modelled on Java's DecimalFormatSymbols, but it allows the use of any
 * Unicode character to represent symbols such as the decimal point and the grouping
 * separator, whereas DecimalFormatSymbols restricts these to a char (1-65535). Since
 * this is essentially a data structure with no behaviour, we don't bother with getter
 * and setter methods but just expose the fields
 */
public class DecimalSymbols implements Serializable {

    public int decimalSeparator;
    public int groupingSeparator;
    public int digit;
    public int minusSign;
    public int percent;
    public int permill;
    public int zeroDigit;
    public int patternSeparator;
    public String infinity;
    public String NaN;


    public boolean equals(Object obj) {
        if (!(obj instanceof DecimalSymbols)) return false;
        DecimalSymbols o = (DecimalSymbols)obj;
        return decimalSeparator == o.decimalSeparator &&
                groupingSeparator == o.groupingSeparator &&
                digit == o.digit &&
                minusSign == o.minusSign &&
                percent == o.percent &&
                permill == o.permill &&
                zeroDigit == o.zeroDigit &&
                patternSeparator == o.patternSeparator &&
                infinity.equals(o.infinity) &&
                NaN.equals(o.NaN);
    }

    public int hashCode() {
        return decimalSeparator + (37*groupingSeparator) + (41*digit);
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

