package net.sf.saxon.type;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.value.UntypedAtomicValue;

import java.io.Serializable;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anyType,
 * also known as the urtype.
 */

public final class AnyType implements ComplexType, ValidSchemaType, Serializable {

    private static AnyType theInstance = new AnyType();

    /**
     * Private constructor
     */
    private AnyType() {
        super();
    }

    /**
     * Get the singular instance of this class
     * @return the singular object representing xs:anyType
     */

    public static AnyType getInstance() {
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
     * @return null (this is the root of the type hierarchy)
     */

    public SchemaType getBaseType() {
        return null;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    public SchemaType getKnownBaseType() throws IllegalStateException {
        return null;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example
     * {@link org.w3c.dom.TypeInfo#DERIVATION_RESTRICTION}
     */

    public int getDerivationMethod() {
        return 0;
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
     * Test whether this ComplexType has been marked as abstract.
     * @return false: this class is not abstract.
     */

    public boolean isAbstract() {
        return false;
    }

    /**
     * Test whether this SchemaType is a complex type
     *
     * @return true if this SchemaType is a complex type
     */

    public boolean isComplexType() {
        return true;
    }

    /**
     * Test whether this SchemaType is a simple type
     * @return true if this SchemaType is a simple type
     */

    public boolean isSimpleType() {
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
     * Test whether this complex type has complex content
     * @return true: this complex type has complex content
     */
    public boolean isComplexContent() {
        return true;
    }

    /**
     * Test whether this complex type has simple content
     * @return false: this complex type has complex content
     */

    public boolean isSimpleContent() {
        return false;
    }

    /**
     * Test whether this complex type has "all" content, that is, a content model
     * using an xs:all compositor
     * @return false: this complex type does not use an "all" compositor
     */

    public boolean isAllContent() {
        return false;
    }

    /**
     * For a complex type with simple content, return the simple type of the content.
     * Otherwise, return null.
     * @return null: this complex type does not have simple content
     */

    public SimpleType getSimpleContentType() {
        return null;
    }

    /**
     * Test whether this complex type is derived by restriction
     * @return false: this type is not a restriction
     */
    public boolean isRestricted() {
        return false;
    }

    /**
     * Test whether the content type of this complex type is empty
     * @return false: the content model is not empty
     */

    public boolean isEmptyContent() {
        return false;
    }

    /**
     * Test whether the content model of this complexType allows empty content
     * @return true: the content is allowed to be empty
     */

    public boolean isEmptiable() {
        return true;
    }

    /**
     * Test whether this complex type allows mixed content
     * @return true: mixed content is allowed
     */

    public boolean isMixedContent() {
        return true;
    }

    /**
     * Get the fingerprint of the name of this type
     * @return the fingerprint.
     */

    public int getFingerprint() {
        return StandardNames.XS_ANY_TYPE;
    }

    /**
     * Get a description of this type for use in diagnostics
     * @return the string "xs:anyType"
     */

    public String getDescription() {
        return "xs:anyType";
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return "xs:anyType";
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return (other instanceof AnyType);
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link net.sf.saxon.type.Type.ELEMENT},
          *                   {@link net.sf.saxon.type.Type.ATTRIBUTE}, or {@link net.sf.saxon.type.Type.DOCUMENT}
     * @param env

     */

    public void analyzeContentExpression(Expression expression, int kind, StaticContext env) {
        return;
    }

    /**
     * Get the typed value of a node that is annotated with this schema type
     * @param node the node whose typed value is required
     * @return an iterator returning a single untyped atomic value, equivalent to the string value of the node. This
     * follows the standard rules for elements with mixed content.
     */

    public SequenceIterator getTypedValue(NodeInfo node) {
        return SingletonIterator.makeIterator(new UntypedAtomicValue(node.getStringValue()));
    }

    /**
     * Test whether this complex type subsumes another complex type. The algorithm
     * used is as published by Thompson and Tobin, XML Europe 2003.
     * @param sub the other type (the type that is derived by restriction, validly or otherwise)
     * @return null indicating that this type does indeed subsume the other; or a string indicating
     * why it doesn't.
     */

    public String subsumes(ComplexType sub) {
        return null;
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
        throw new SchemaException("Cannot derive xs:anyType from another type");
    }

    /**
     * The name of a type declared for the associated element or attribute,
     * or <code>null</code> if unknown.
     */
    public String getTypeName() {
        return "anyType";
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
        return false;
    }

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the schema type associated with that element particle.
     * If there is no such particle, return null. If the fingerprint matches an element wildcard,
     * return the type of the global element declaration with the given name if one exists, or AnyType
     * if none exists and lax validation is permitted by the wildcard.
     *
     * @param fingerprint Identifies the name of the child element within this content model
     */

    public SchemaType getElementParticleType(int fingerprint) {
        return this;
    }

    /**
     * Find an element particle within this complex type definition having a given element name
     * (identified by fingerprint), and return the cardinality associated with that element particle,
     * that is, the number of times the element can occur within this complex type. The value is one of
     * {@link net.sf.saxon.expr.StaticProperty.EXACTLY_ONE}, {@link net.sf.saxon.expr.StaticProperty.ALLOWS_ZERO_OR_ONE},
     * {@link net.sf.saxon.expr.StaticProperty.ALLOWS_ZERO_OR_MORE}, {@link net.sf.saxon.expr.StaticProperty.ALLOWS_ONE_OR_MORE},
     * If there is no such particle, return zero.
     *
     * @param fingerprint Identifies the name of the child element within this content model
     */

    public int getElementParticleCardinality(int fingerprint) {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Find an attribute use within this complex type definition having a given attribute name
     * (identified by fingerprint), and return the schema type associated with that attribute.
     * If there is no such attribute use, return null. If the fingerprint matches an attribute wildcard,
     * return the type of the global attribute declaration with the given name if one exists, or AnySimpleType
     * if none exists and lax validation is permitted by the wildcard.
     *
     * @param fingerprint Identifies the name of the child element within this content model
     */

    public SchemaType getAttributeUseType(int fingerprint) {
        return AnySimpleType.getInstance();
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