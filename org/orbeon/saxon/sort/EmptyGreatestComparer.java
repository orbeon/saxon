package org.orbeon.saxon.sort;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;

/**
 * A Comparer that modifies a base comparer by sorting empty key values and NaN values last (greatest),
 * as opposed to the default which sorts them first.
 *
 * @author Michael H. Kay
 *
 */

public class EmptyGreatestComparer implements AtomicComparer, java.io.Serializable {

    private AtomicComparer baseComparer;

    /**
     * Create an EmptyGreatestComparer
     * @param baseComparer the comparer used to compare non-empty values (which typically sorts empty
     * as least)
     */

    public EmptyGreatestComparer(AtomicComparer baseComparer) {
        this.baseComparer = baseComparer;
    }

    /**
     * Get the underlying comparer (which compares empty least)
     * @return the base comparer
     */

    public AtomicComparer getBaseComparer() {
        return baseComparer;
    }

    /**
     * Supply the dynamic context in case this is needed for the comparison
     * @param context the dynamic evaluation context
     * @return either the original AtomicComparer, or a new AtomicComparer in which the context
     * is known. The original AtomicComparer is not modified
     */

    public AtomicComparer provideContext(XPathContext context) {
        AtomicComparer newBase = baseComparer.provideContext(context);
        if (newBase != baseComparer) {
            return new EmptyGreatestComparer(newBase);
        } else {
            return this;
        }
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

    public int compareAtomicValues(AtomicValue a, AtomicValue b) throws NoDynamicContextException {
        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                return +1;
            }
        } else if (b == null) {
            return -1;
        }

        if (a.isNaN()) {
            return (b.isNaN() ? 0 : +1);
        } else if (b.isNaN()) {
            return -1;
        }

        return baseComparer.compareAtomicValues(a, b);
    }

    /**
     * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
     * values are compared by converting to the type of the other operand.
     *
     * @param a the first object to be compared.
     * @param b the second object to be compared.
     * @return true if the values are equal, false if not
     * @throws ClassCastException if the objects are not comparable
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) throws NoDynamicContextException {
        return (a==null && b==null) || baseComparer.comparesEqual(a, b);
    }

    /**
     * Get a comparison key for an object. This must satisfy the rule that if two objects are equal
     * according to the XPath eq operator, then their comparison keys are equal according to the Java
     * equals() method, and vice versa. There is no requirement that the
     * comparison keys should reflect the ordering of the underlying objects.
     */

    public ComparisonKey getComparisonKey(AtomicValue a) throws NoDynamicContextException {
        return (a==null ? new ComparisonKey(Type.EMPTY, "()") : baseComparer.getComparisonKey(a));
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