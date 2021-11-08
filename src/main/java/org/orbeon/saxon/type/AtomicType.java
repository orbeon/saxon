package org.orbeon.saxon.type;

import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.value.AtomicValue;

/**
 * Interface for atomic types (these are either built-in atomic types
 * or user-defined atomic types). An AtomicType is both an ItemType (a possible type
 * for items in a sequence) and a SchemaType (a possible type for validating and
 * annotating nodes).
 */
public interface AtomicType extends SimpleType, ItemType {

    /**
     * Validate that a primitive atomic value is a valid instance of a type derived from the
     * same primitive type.
     * @param primValue the value in the value space of the primitive type.
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
     * is used. This value is checked against the pattern facet (if any)
     * @param checker Used for checking names against XML 1.0 or XML 1.1 rules
     * @return null if the value is valid; otherwise, a ValidationFailure object indicating
     * the nature of the error.
     * @throws UnsupportedOperationException in the case of an external object type
     */

    public ValidationFailure validate(AtomicValue primValue, CharSequence lexicalValue, NameChecker checker);

    /**
     * Determine whether the atomic type is ordered, that is, whether less-than and greater-than comparisons
     * are permitted
     * @return true if ordering operations are permitted
     */

    public boolean isOrdered();

    /**
     * Determine whether the type is abstract, that is, whether it cannot have instances that are not also
     * instances of some concrete subtype
     */

    public boolean isAbstract();

    /**
     * Determine whether the atomic type is a primitive type.  The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; and all supertypes of these (xs:anyAtomicType, xs:numeric, ...)
     * @return true if the type is considered primitive under the above rules
     */

    public boolean isPrimitiveType();

    /**
     * Determine whether the atomic type is a built-in type. The built-in atomic types are the 41 atomic types
     * defined in XML Schema, plus xs:dayTimeDuration and xs:yearMonthDuration,
     * xs:untypedAtomic, and all supertypes of these (xs:anyAtomicType, xs:numeric, ...)
     */

    public boolean isBuiltInType();
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

