package org.orbeon.saxon.sort;
import org.orbeon.saxon.value.DerivedAtomicValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.UntypedAtomicValue;

import java.util.Comparator;
import java.text.Collator;

/**
 * A Comparator used for comparing atomic values of arbitrary item types. It encapsulates
 * a Collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * @author Michael H. Kay
 *
 */

public class AtomicComparer implements Comparator, java.io.Serializable {

    private Comparator collator;

    public AtomicComparer(Comparator collator) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compare(Object a, Object b) {

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);

        if (a instanceof DerivedAtomicValue) {
            a = ((DerivedAtomicValue)a).getPrimitiveValue();
        }
        if (b instanceof DerivedAtomicValue) {
            b = ((DerivedAtomicValue)b).getPrimitiveValue();
        }

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator);
        } else if (a instanceof Comparable) {
            return ((Comparable)a).compareTo(b);
        } else if (a instanceof StringValue) {
            return collator.compare(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue());
        } else {
            throw new ClassCastException("Objects are not comparable (" + a.getClass() + ", " + b.getClass() + ')');
        }
    }

    /**
    * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
    * values are compared by converting to the type of the other operand.
    * @param a the first object to be compared. It is intended that this should be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the equals() method.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public boolean comparesEqual(Object a, Object b) {
        // System.err.println("Comparing " + a.getClass() + ": " + a + " with " + b.getClass() + ": " + b);

        if (a instanceof DerivedAtomicValue) {
            a = ((DerivedAtomicValue)a).getPrimitiveValue();
        }
        if (b instanceof DerivedAtomicValue) {
            b = ((DerivedAtomicValue)b).getPrimitiveValue();
        }

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator) == 0;
        } else if (b instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)b).compareTo(a, collator) == 0;
        } else if (a instanceof StringValue) {
            return collator.compare(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue()) == 0;
        } else if (a instanceof String) {
            return collator.compare(a, b) == 0;
        } else {
            return a.equals(b);
        }
    }

    /**
    * Get a comparison key for an object. This must satisfy the rule that if two objects are equal,
    * then their comparison keys are equal, and vice versa. There is no requirement that the
    * comparison keys should reflect the ordering of the underlying objects.
    */

    public Object getComparisonKey(Object a) {

        if (a instanceof DerivedAtomicValue) {
            a = ((DerivedAtomicValue)a).getPrimitiveValue();
        }

        if (a instanceof StringValue) {
            if (collator instanceof Collator) {
                return ((Collator)collator).getCollationKey(((StringValue)a).getStringValue());
            } else {
                return ((StringValue)a).getStringValue();
            }
        } else {
            return a;
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