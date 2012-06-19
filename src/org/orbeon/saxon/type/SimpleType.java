package org.orbeon.saxon.type;

import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.om.SequenceIterator;

/**
 * This interface represents a simple type, which may be a built-in simple type, or
 * a user-defined simple type.
 */

public interface SimpleType extends SchemaType {

    /**
     * Test whether this Simple Type is an atomic type
     * @return true if this is an atomic type
     */

    boolean isAtomicType();

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
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     * @return true if this is an external type
     */

    boolean isExternalType();

    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     * @return the lowest common supertype of all member types
     */

    AtomicType getCommonAtomicType();

    /**
     * Determine whether this is a built-in type or a user-defined type
     * @return true if this is a built-in type
     */

    boolean isBuiltInType();

    /**
     * Get the built-in type from which this type is derived by restriction
     * @return the built-in type from which this type is derived by restriction. This will not necessarily
     * be a primitive type.
     */

    SchemaType getBuiltInBaseType();

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     * @param value the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     * in the content of values. Can supply null, in which case any namespace-sensitive content
     * will be rejected.
     * @param nameChecker a NameChecker used in the case of types that are defined in terms of the
     * XML NCName syntax: this is used to check conformance to XML 1.0 or XML 1.1 naming rules, as
     * appropriate
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     * returned by this SequenceIterator will all be of type {@link org.orbeon.saxon.value.AtomicValue},
     * The next() method on the iterator throws no checked exceptions, although it is not actually
     * declared as an UnfailingIterator.
     * @throws ValidationException if the supplied value is not in the lexical space of the data type
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver, NameChecker nameChecker)
            throws ValidationException;

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @param nameChecker XML 1.0 or 1.1 name checker, needed when types such as xs:NCName are used
     * @return null if validation succeeds; return a ValidationFailure describing the validation failure
     * if validation fails. Note that the exception is returned rather than being thrown.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    ValidationFailure validateContent(CharSequence value, NamespaceResolver nsResolver, NameChecker nameChecker);

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
     * @param th the type hierarchy cache. Not needed in the case of a built-in type
     */

    public int getWhitespaceAction(TypeHierarchy th);
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

