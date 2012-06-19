package org.orbeon.saxon.sort;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.NoDynamicContextException;

/**
 * A Comparer used for comparing descending keys. This simply returns the inverse of the result
 * delivered by the base comparer.
 */

public class DescendingComparer implements AtomicComparer, java.io.Serializable {

    private AtomicComparer baseComparer;

    public DescendingComparer(AtomicComparer base) {
        baseComparer = base;
    }

    /**
     * Get the underlying (ascending) comparer
     * @return the underlying (ascending) comparer
     */

    public AtomicComparer getBaseComparer() {
        return baseComparer;
    }

    /**
     * Supply the dynamic context in case this is needed for the comparison
     * @param context the dynamic evaluation context
     * @return either the original AtomicComparer, or a new AtomicComparer in which the context
     * is known. The original AtomicComparer is not modified
     * @throws NoDynamicContextException if the context is an "early evaluation" (compile-time) context
     */

    public AtomicComparer provideContext(XPathContext context) {
        AtomicComparer newBase = baseComparer.provideContext(context);
        if (newBase != baseComparer) {
            return new DescendingComparer(newBase);
        } else {
            return this;
        }
    }

    /**
    * Compare two objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are of the wrong type for this Comparer
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) throws NoDynamicContextException {
        return 0 - baseComparer.compareAtomicValues(a, b);
    }

    /**
     * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
     * values are compared by converting to the type of the other operand.
     *
     * @param a the first object to be compared. It is intended that this should be an instance
     *          of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
     *          collator is used to compare the values, otherwise the value must implement the equals() method.
     * @param b the second object to be compared. This must be comparable with the first object: for
     *          example, if one is a string, they must both be strings.
     * @return true if the values are equal, false if not
     * @throws ClassCastException if the objects are not comparable
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) throws NoDynamicContextException {
        return baseComparer.comparesEqual(a, b);
    }

    /**
     * Get a comparison key for an object. This must satisfy the rule that if two objects are equal
     * according to the XPath eq operator, then their comparison keys are equal according to the Java
     * equals() method, and vice versa. There is no requirement that the
     * comparison keys should reflect the ordering of the underlying objects.
     */

    public ComparisonKey getComparisonKey(AtomicValue a) throws NoDynamicContextException {
        return baseComparer.getComparisonKey(a);
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