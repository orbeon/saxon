package net.sf.saxon.type;

import net.sf.saxon.value.AtomicValue;

/**
 * Marker interface for atomic types (these are either built-in atomic types
 * or user-defined atomic types). An AtomicType is both an ItemType (a possible type
 * for items in a sequence) and a SchemaType (a possible type for validating and
 * annotating nodes).
 */
public interface AtomicType extends SimpleType, ItemType {

    /**
     * Factory method to create values of a derived atomic type. This method
     * is not used to create values of a built-in type, even one that is not
     * primitive.
     * @param primValue the value in the value space of the primitive type
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
     * is used. This value is checked against the pattern facet (if any)
     * @param validate     true if the value is to be validated against the facets of the derived
     *                     type; false if the caller knows that the value is already valid.
     * @return the derived atomic value if validation succeeds, or an ErrorValue otherwise. The ErrorValue
     * encapsulates the exception that occurred; it is the caller's responsibility to check for this.
     */

    public AtomicValue makeDerivedValue(AtomicValue primValue, CharSequence lexicalValue, boolean validate);

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

