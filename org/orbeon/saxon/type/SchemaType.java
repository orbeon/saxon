package org.orbeon.saxon.type;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.xpath.XPathException;
import org.w3c.dom.TypeInfo;

/**
 * SchemaType is an interface implemented by all schema types: simple and complex types, built-in and
 * user-defined types.
 *
 * <p>There is a hierarchy of interfaces that extend SchemaType, representing the top levels of the schema
 * type system: SimpleType and ComplexType, with SimpleType further subdivided into List, Union, and Atomic
 * types.</p>
 *
 * <p>The implementations of these interfaces are organized into a different hierarchy: on the one side,
 * built-in types such as AnyType, AnySimpleType, and the built-in atomic types and list types; on the other
 * side, user-defined types defined in a schema.</p>
 */

public interface SchemaType extends SchemaComponent, TypeInfo {
    /**
     * Get the fingerprint of the name of this type
     * @return the fingerprint. Returns an invented fingerprint for an anonymous type.
     */

    int getFingerprint();

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     * @return a lexical QName identifying the type
     */

    String getDisplayName();

    /**
     * Test whether this SchemaType is a complex type
     * @return true if this SchemaType is a complex type
     */

    boolean isComplexType();

    /**
     * Test whether this SchemaType is a simple type
     * @return true if this SchemaType is a simple type
     */

    boolean isSimpleType();

    /**
	 * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link TypeInfo#DERIVATION_LIST} and {@link TypeInfo#DERIVATION_EXTENSION}
     * @return the value of the 'block' attribute for this type
	 */

    int getBlock();

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
    */

    SchemaType getBaseType() throws UnresolvedReferenceException;

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
    */

    SchemaType getKnownBaseType() throws IllegalStateException;

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     * @return a numeric code representing the derivation method, for example {@link TypeInfo#DERIVATION_RESTRICTION}
     */

    int getDerivationMethod();

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     * @param derivation the kind of derivation, for example {@link TypeInfo#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    boolean allowsDerivation(int derivation);

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     * @param expression the expression that delivers the content
     * @param kind the node kind whose content is being delivered: {@link Type.ELEMENT},
     * {@link Type.ATTRIBUTE}, or {@link Type.DOCUMENT}
     * @param env The static evaluation context for the query or stylesheet
     * @throws XPathException if the expression will never deliver a value of the correct type
     */

    void analyzeContentExpression(Expression expression, int kind, StaticContext env) throws XPathException;

    /**
     * Get the typed value of a node that is annotated with this schema type. This method must be called
     * only for a valid type.
     * @param node the node whose typed value is required
     * @return a SequenceIterator over the atomic values making up the typed value of the specified
     * node. The objects returned by this iterator are of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    SequenceIterator getTypedValue(NodeInfo node) throws XPathException;

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    boolean isSameType(SchemaType other);

    /**
     * Get a description of this type for use in error messages. This is the same as the display name
     * in the case of named types; for anonymous types it identifies the type by its position in a source
     * schema document.
     * @return text identifing the type, for use in a phrase such as "the type XXXX".
     */

    String getDescription();

    /**
     * Check that this type is validly derived from a given type
     * @param type the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkDerivation(SchemaType type, int block) throws SchemaException, ValidationException;
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
