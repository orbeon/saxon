package net.sf.saxon.type;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.om.*;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.value.Whitespace;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anySimpleType
 */

public final class AnySimpleType implements SimpleType, ValidSchemaType {

    private static AnySimpleType theInstance = new AnySimpleType();

    /**
     * Private constructor
     */
    private AnySimpleType() {
    }

    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     * @return the lowest common supertype of all member types
     */

    public AtomicType getCommonAtomicType() {
        return Type.ANY_ATOMIC_TYPE;
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
     * Test whether this ComplexType has been marked as abstract.
     * @return false: this class is not abstract.
     */

//    public boolean isAbstract() {
//        return false;
//    }

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
        return SingletonIterator.makeIterator(new UntypedAtomicValue(node.getStringValue()));
    }

     /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException
     *          if the derivation is not allowed
     */

    public void checkDerivation(SchemaType type, int block) throws SchemaException {
        throw new SchemaException("Cannot derive xs:anySimpleType from another type");
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
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     *         returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue}
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver) {
        return new UntypedAtomicValue(value).iterate(null);
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     *
     * @param value      the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     *                   is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     *                   content will throw an UnsupportedOperationException.
     *                                       if the content is invalid
     */

    public void validateContent(CharSequence value, NamespaceResolver nsResolver) {
        return;
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
     * integer with fields such as {@link org.w3c.dom.TypeInfo#DERIVATION_LIST} and {@link org.w3c.dom.TypeInfo#DERIVATION_EXTENSION}
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
     * @return a numeric code representing the derivation method, for example {@link org.w3c.dom.TypeInfo#DERIVATION_RESTRICTION}
     */

    public int getDerivationMethod() {
        return org.w3c.dom.TypeInfo.DERIVATION_RESTRICTION;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link org.w3c.dom.TypeInfo#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public boolean allowsDerivation(int derivation) {
        return true;
    }

    /**
     * Determine how values of this simple type are whitespace-normalized.
     *
     * @return one of {@link net.sf.saxon.value.Whitespace#PRESERVE}, {@link net.sf.saxon.value.Whitespace#COLLAPSE},
     *         {@link net.sf.saxon.value.Whitespace#REPLACE}.
     */

    public int getWhitespaceAction() {
        return Whitespace.COLLAPSE;
    }

    /**
     * The name of a type declared for the associated element or attribute,
     * or <code>null</code> if unknown.
     */
    public String getTypeName() {
        return "anySimpleType";
    }

    /**
     * The namespace of the type declared for the associated element or
     * attribute or <code>null</code> if the element does not have
     * declaration or if no namespace information is available.
     */
    public String getTypeNamespace() {
        return NamespaceConstant.SCHEMA;
    }

    /**
     * This method returns if there is a derivation between the reference
     * type definition, i.e. the <code>TypeInfo</code> on which the method
     * is being called, and the other type definition, i.e. the one passed
     * as parameters.
     *
     * @param typeNamespaceArg the namespace of the other type definition.
     * @param typeNameArg      the name of the other type definition.
     * @param derivationMethod the type of derivation and conditions applied
     *                         between two types, as described in the list of constants provided
     *                         in this interface.
     * @return If the document's schema is a DTD or no schema is associated
     *         with the document, this method will always return <code>false</code>
     *         .  If the document's schema is an XML Schema, the method will
     *         <code>true</code> if the reference type definition is derived from
     *         the other type definition according to the derivation parameter. If
     *         the value of the parameter is <code>0</code> (no bit is set to
     *         <code>1</code> for the <code>derivationMethod</code> parameter),
     *         the method will return <code>true</code> if the other type
     *         definition can be reached by recursing any combination of {base
     *         type definition}, {item type definition}, or {member type
     *         definitions} from the reference type definition.
     */
    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
        return (typeNamespaceArg.equals(NamespaceConstant.SCHEMA) &&
                typeNameArg.equals("anyType"));
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link net.sf.saxon.type.Type.ELEMENT},
     *                   {@link net.sf.saxon.type.Type.ATTRIBUTE}, or {@link net.sf.saxon.type.Type.DOCUMENT}
     * @param env
     */

    public void analyzeContentExpression(Expression expression, int kind, StaticContext env) {
        return;
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