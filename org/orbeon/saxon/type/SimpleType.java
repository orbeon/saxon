package org.orbeon.saxon.type;

import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.om.SequenceIterator;

/**
 * This interface represents a simple type, which may be a built-in simple type, or
 * a user-defined simple type.
 */

public interface SimpleType extends SchemaType {

    /**
     * Test whether this Simple Type is a list type
     * @return true if this is a list type
     */
    boolean isListType();

   /**
     * Test whether this Simple Type is a union type
     * @return true if this is a union type
     */

    boolean isUnionType();

    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     * @return the lowest common supertype of all member types
     */

    AtomicType getCommonAtomicType();

    SchemaType getBuiltInBaseType() throws ValidationException;

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     * @param value the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     * in the content of values. Can supply null, in which case any namespace-sensitive content
     * will be rejected.
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     * returned by this SequenceIterator will all be of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver)
            throws ValidationException;

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @return null if validation succeeds; return a ValidationException describing the validation failure
     * if validation fails. Note that the exception is returned rather than being thrown.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    ValidationException validateContent(CharSequence value, NamespaceResolver nsResolver);

    /**
     * Test whether this type is namespace sensitive, that is, if a namespace context is needed
     * to translate between the lexical space and the value space. This is true for types derived
     * from, or containing, QNames and NOTATIONs
     * @return true if the type is namespace-sensitive
     */

    boolean isNamespaceSensitive();

    /**
     * Determine how values of this simple type are whitespace-normalized.
     * @return one of {@link org.orbeon.saxon.value.Whitespace#PRESERVE}, {@link org.orbeon.saxon.value.Whitespace#COLLAPSE},
     * {@link org.orbeon.saxon.value.Whitespace#REPLACE}.
     */

    public int getWhitespaceAction();
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

