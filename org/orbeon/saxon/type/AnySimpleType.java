package org.orbeon.saxon.type;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.Whitespace;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anySimpleType
 */

public final class AnySimpleType implements SimpleType {

    private static AnySimpleType theInstance = new AnySimpleType();

    /**
     * Private constructor
     */
    private AnySimpleType() {
    }

    /**
     * Get the local name of this type
     *
     * @return the local name of this type definition, if it has one. Return null in the case of an
     *         anonymous type.
     */

    public String getName() {
        return "anySimpleType";
    }

    /**
     * Get the target namespace of this type
     *
     * @return the target namespace of this type definition, if it has one. Return null in the case
     *         of an anonymous type, and in the case of a global type defined in a no-namespace schema.
     */

    public String getTargetNamespace() {
        return NamespaceConstant.SCHEMA;
    }

    /**
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     */

    public boolean isExternalType() {
        return false;
    }

    /**
     * Determine whether this is a built-in type or a user-defined type
     */

    public boolean isBuiltInType() {
        return true;
    }

    /**
     * Get the URI of the schema document containing the definition of this type
     * @return null for a built-in type
     */

    public String getSystemId() {
        return null;
    }

    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     * @return the lowest common supertype of all member types
     */

    public AtomicType getCommonAtomicType() {
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
     * Get the singular instance of this class
     * @return the singular object representing xs:anyType
     */

    public static AnySimpleType getInstance() {
        return theInstance;
    }

    /**
     * Get the validation status - always valid
     */
    public int getValidationStatus()  {
        return VALIDATED;
    }

    /**
     * Get the base type
     * @return AnyType
     */

    public SchemaType getBaseType() {
        return AnyType.getInstance();
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * @return the base type.
     */

    public SchemaType getKnownBaseType() throws IllegalStateException {
        return getBaseType();
    }

    /**
     * Test whether this SchemaType is a complex type
     *
     * @return true if this SchemaType is a complex type
     */

    public boolean isComplexType() {
        return false;
    }

    /**
     * Test whether this SchemaType is a simple type
     * @return true if this SchemaType is a simple type
     */

    public boolean isSimpleType() {
        return true;
    }

    /**
     * Get the fingerprint of the name of this type
     * @return the fingerprint.
     */

    public int getFingerprint() {
        return StandardNames.XS_ANY_SIMPLE_TYPE;
    }

    /**
     * Get the namecode of the name of this type. This includes the prefix from the original
     * type declaration: in the case of built-in types, there may be a conventional prefix
     * or there may be no prefix.
     */

    public int getNameCode() {
        return StandardNames.XS_ANY_SIMPLE_TYPE;
    }

    /**
     * Get a description of this type for use in diagnostics
     * @return the string "xs:anyType"
     */

    public String getDescription() {
        return "xs:anySimpleType";
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return "xs:anySimpleType";
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return (other instanceof AnySimpleType);
    }

    /**
     * Get the typed value of a node that is annotated with this schema type. This shouldn't happen: nodes
     * are never annotated as xs:anySimpleType; but if it does happen, we treat it as if it were
     * untypedAtomic.
     * @param node the node whose typed value is required
     * @return an iterator returning a single untyped atomic value, equivalent to the string value of the node.
     */

    public SequenceIterator getTypedValue(NodeInfo node) {
        return SingletonIterator.makeIterator(new UntypedAtomicValue(node.getStringValueCS()));
    }

    /**
     * Get the typed value of a node that is annotated with this schema type. The result of this method will always be consistent with the method
     * {@link #getTypedValue}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @param node the node whose typed value is required
     * @return the typed value.
     * @since 8.5
     */

    public Value atomize(NodeInfo node) {
        return new UntypedAtomicValue(node.getStringValueCS());
    }

     /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException
     *          if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException {
        if (type == this) {
            return;
        }
        throw new SchemaException("Cannot derive xs:anySimpleType from another type");
    }

    /**
     * Test whether this Simple Type is an atomic type
     * @return false, this is not (necessarily) an atomic type
     */

    public boolean isAtomicType() {
        return false;
    }

    public boolean isAnonymousType() {
        return false;
    }


    /**
     * Determine whether this is a list type
     * @return false (it isn't a list type)
     */
    public boolean isListType() {
        return false;
    }

    /**
     * Determin whether this is a union type
     * @return false (it isn't a union type)
     */
    public boolean isUnionType() {
        return false;
    }

    /**
     * Get the built-in ancestor of this type in the type hierarchy
     * @return this type itself
     */
    public SchemaType getBuiltInBaseType() {
        return this;
    }

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     *
     * @param value    the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     *                 in the content of values. Can supply null, in which case any namespace-sensitive content
     *                 will be rejected.
     * @param nameChecker
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     *         returned by this SequenceIterator will all be of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver, NameChecker nameChecker) {
        return SingletonIterator.makeIterator(new UntypedAtomicValue(value));
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @param nameChecker
     * @return null if validation succeeds (which it always does for this implementation)
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */
    public ValidationFailure validateContent(CharSequence value, NamespaceResolver nsResolver, NameChecker nameChecker) {
        return null;
    }

    /**
     * Test whether this type represents namespace-sensitive content
     * @return false
     */
    public boolean isNamespaceSensitive() {
        return false;
    }

    /**
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link SchemaType#DERIVATION_LIST} and {@link SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public int getBlock() {
        return 0;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example {@link SchemaType#DERIVATION_RESTRICTION}
     */

    public int getDerivationMethod() {
        return SchemaType.DERIVATION_RESTRICTION;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public boolean allowsDerivation(int derivation) {
        return true;
    }

    /**
     * Determine how values of this simple type are whitespace-normalized.
     *
     * @return one of {@link org.orbeon.saxon.value.Whitespace#PRESERVE}, {@link org.orbeon.saxon.value.Whitespace#COLLAPSE},
     *         {@link org.orbeon.saxon.value.Whitespace#REPLACE}.
     * @param th the type hierarchy cache
     */

    public int getWhitespaceAction(TypeHierarchy th) {
        return Whitespace.COLLAPSE;
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link org.orbeon.saxon.type.Type#ELEMENT},
     *                   {@link org.orbeon.saxon.type.Type#ATTRIBUTE}, or {@link org.orbeon.saxon.type.Type#DOCUMENT}
     * @param env
     */

    public void analyzeContentExpression(Expression expression, int kind, StaticContext env) {
        //return;
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//