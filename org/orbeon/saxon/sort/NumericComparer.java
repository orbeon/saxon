package org.orbeon.saxon.sort;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.xpath.XPathException;
import java.util.Comparator;

/**
 * A Comparer used for comparing sort keys when data-type="number". The items to be
 * compared are converted to numbers, and the numbers are then compared directly
 *
 * @author Michael H. Kay
 *
 */

public class NumericComparer implements Comparator, java.io.Serializable {

    public NumericComparer() {
    }

    /**
    * Compare two Items by converting them to numbers and comparing the numeric values. If either
    * value cannot be converted to a number, it is treated as NaN, and compares less that the other
    * (two NaN values compare equal).
    * @param a the first Item to be compared.
    * @param b the second Item to be compared.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not Items
    */

    public int compare(Object a, Object b) {


        double d1, d2;

        if (a instanceof NumericValue) {
            d1 = ((NumericValue)a).getDoubleValue();
        } else {
            try {
                String s1 = (a instanceof String ? (String)a : ((Item)a).getStringValue());
                d1 = Value.stringToNumber(s1);
            } catch (XPathException err) {
                d1 = Double.NaN;
            } catch (NumberFormatException err) {
                d1 = Double.NaN;
            }
        }

        if (b instanceof NumericValue) {
            d2 = ((NumericValue)b).getDoubleValue();
        } else {
            try {
                String s2 = (b instanceof String ? (String)b : ((Item)b).getStringValue());
                d2 = Value.stringToNumber(s2);
            } catch (XPathException err) {
                d2 = Double.NaN;
            } catch (NumberFormatException err) {
                d2 = Double.NaN;
            }
        }

        if (Double.isNaN(d1)) {
            if (Double.isNaN(d2)) {
                return 0;
            } else {
                return -1;
            }
        }
        if (Double.isNaN(d2)) {
            return +1;
        }
        if (d1 < d2) return -1;
        if (d1 > d2) return +1;
        return 0;

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
// The Initial Developer of this module is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//