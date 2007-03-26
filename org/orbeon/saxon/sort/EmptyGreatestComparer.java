package org.orbeon.saxon.sort;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.NumericValue;

import java.util.Comparator;

/**
 * A Comparer that modifies a base comparer by sorting empty key values and NaN values last (greatest),
 * as opposed to the default which sorts them first.
 *
 * @author Michael H. Kay
 *
 */

public class EmptyGreatestComparer implements Comparator, java.io.Serializable {

    private Comparator baseComparer;

    public EmptyGreatestComparer(AtomicComparer baseComparer) {
        this.baseComparer = baseComparer;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should normally be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compare(Object a, Object b) {
        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                return +1;
            }
        } else if (b == null) {
            return -1;
        }

        a = ((AtomicValue)a).getPrimitiveValue();
        b = ((AtomicValue)b).getPrimitiveValue();

        if (a instanceof NumericValue && ((NumericValue)a).isNaN()) {
            if (b instanceof NumericValue && ((NumericValue)b).isNaN()) {
                return 0;
            } else {
                return +1;
            }
        } else if (b instanceof NumericValue && ((NumericValue)b).isNaN()) {
            return -1;
        }

        return baseComparer.compare(a, b);
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