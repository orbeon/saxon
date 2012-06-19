package org.orbeon.saxon.sort;

import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.NoDynamicContextException;

import java.io.Serializable;

/**
 * Interface representing an object that can be used to compare two XPath atomic values for equality or
 * for ordering.
 */

public interface AtomicComparer extends Serializable {

    /**
     * Supply the dynamic context in case this is needed for the comparison
     * @param context the dynamic evaluation context
     * @return either the original AtomicComparer, or a new AtomicComparer in which the context
     * is known. The original AtomicComparer is not modified
     */

    public AtomicComparer provideContext(XPathContext context);

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
     * @throws NoDynamicContextException if this comparer required access to dynamic context information,
     * notably the implicit timezone, and this information is not available. In general this happens if a
     * context-dependent comparison is attempted at compile-time, and it signals the compiler to generate
     * code that tries again at run-time.
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) throws NoDynamicContextException;

    /**
    * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
    * values are compared by converting to the type of the other operand.
    * @param a the first object to be compared.
    * @param b the second object to be compared.
    * @return true if the values are equal, false if not
    * @throws ClassCastException if the objects are not comparable
    */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) throws NoDynamicContextException;

    /**
     * Get a comparison key for an object. This must satisfy the rule that if two objects are equal
     * according to the XPath eq operator, then their comparison keys are equal according to the Java
     * equals() method, and vice versa. There is no requirement that the
     * comparison keys should reflect the ordering of the underlying objects.
     */

    public ComparisonKey getComparisonKey(AtomicValue a) throws NoDynamicContextException;
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

