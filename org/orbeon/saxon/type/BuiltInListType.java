package org.orbeon.saxon.type;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.NormalizeSpace;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.SequenceExtent;

import java.io.Serializable;

/**
 * <p>This class is used to implement the built-in
 * list types NMTOKENS, ENTITIES, IDREFS. It is also used to represent the anonymous type of the
 * xsi:schemaLocation attribute (a list of xs:anyURI values).</p>
 *
**/

public class BuiltInListType implements ListType, Serializable {

    private int fingerprint;

    /**
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     */

    public boolean isExternalType() {
        return false;
    }


    /**
     * Determine how values of this simple type are whitespace-normalized.
     *
     * @return one of {@link org.orbeon.saxon.value.Whitespace#PRESERVE}, {@link org.orbeon.saxon.value.Whitespace#COLLAPSE},
     *         {@link org.orbeon.saxon.value.Whitespace#REPLACE}.
     * @param th
     */

    public int getWhitespaceAction(TypeHierarchy th) {
        return Whitespace.COLLAPSE;
    }

    /**
     *  The SimpleType of the items in the list.
     */

    private BuiltInAtomicType itemType = null;

    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     *
     * @return the lowest common supertype of all member types
     */

    public AtomicType getCommonAtomicType() {
        return itemType;
    }

    /**
     * Create a new ListType.
     */

    public BuiltInListType(int fingerprint) {
        this.fingerprint = fingerprint;
        switch (fingerprint) {
            case StandardNames.XS_ENTITIES:
                itemType = (BuiltInAtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_ENTITY);
                break;
            case StandardNames.XS_IDREFS:
                itemType = (BuiltInAtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_IDREF);
                break;
            case StandardNames.XS_NMTOKENS:
                itemType = (BuiltInAtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_NMTOKEN);
                break;
            case StandardNames.XSI_SCHEMA_LOCATION_TYPE:
                itemType = (BuiltInAtomicType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_ANY_URI);
                break;
        }
    }

    /**
     * Get the validation status - always valid
     */
    public int getValidationStatus()  {
        return VALIDATED;
    }

   /**
     * Returns the base type that this type inherits from.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     * @return the base type.
    */

    public SchemaType getBaseType() {
        return AnySimpleType.getInstance();
    }

    /**
     * Test whether this Simple Type is an atomic type
     * @return false, this is not an atomic type
     */

    public boolean isAtomicType() {
        return false;
    }


   /**
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     */

    public boolean isListType() {
        return true;
    }

    public boolean isUnionType() {
        return false;
    }

    /**
     * Test whether this is an anonymous type
     * @return true if this SchemaType is an anonymous type
     */

    public boolean isAnonymousType() {
        return false;
    }

    public SchemaType getBuiltInBaseType() {
        return this;
    }

    public boolean isNamespaceSensitive() {
        return false;
    }

    /**
     * Get the fingerprint of the name of this type
     * @return the fingerprint. Returns an invented fingerprint for an anonymous type.
     */

    public int getFingerprint() {
        return fingerprint;
    }

    /**
     * Get the namecode of the name of this type. Because built-in types don't depend on the namePool,
     * this actually returns the fingerprint, which contains no information about the namespace prefix
     */

    public int getNameCode() {
        return fingerprint;
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return StandardNames.getDisplayName(fingerprint);
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
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link SchemaType#DERIVATION_LIST} and {@link SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public int getBlock() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
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
        return AnySimpleType.getInstance();
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example {@link SchemaType#DERIVATION_RESTRICTION}
     */

    public int getDerivationMethod() {
        return SchemaType.DERIVATION_LIST;
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
     * Get the typed value of a node that is annotated with this schema type. This method must be called
     * only for a valid type.
     *
     * @param node the node whose typed value is required
     * @return a SequenceIterator over the atomic values making up the typed value of the specified
     *         node. The objects returned by this iterator are of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    public SequenceIterator getTypedValue(NodeInfo node) throws XPathException {
        try {
            return getTypedValue(node.getStringValue(),
                    new InscopeNamespaceResolver(node),
                    node.getConfiguration().getNameChecker());
        } catch (ValidationException err) {
            throw new DynamicError("Internal error: value doesn't match its type annotation. " + err.getMessage());
        }
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

    public Value atomize(NodeInfo node) throws XPathException {
        return new SequenceExtent(getTypedValue(node)).simplify();
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return other.getFingerprint() == this.getFingerprint();
    }

    public String getDescription() {
        return getDisplayName();
    }

    /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException
     *          if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException, ValidationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Get the local name of this type
     * @return the local part of the name, or null if the type is anonymous
     */

    public String getLocalName() {
       return getDisplayName().substring(3);
    }

    /**
     * Returns the simpleType of the items in this ListType.
     * @return the simpleType of the items in this ListType.
    */

    public SimpleType getItemType() {
        return itemType;
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public String applyWhitespaceNormalization(String value) {
        return NormalizeSpace.normalize(value).toString();
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link Type#ELEMENT},
     *                   {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     * @param env
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public void analyzeContentExpression(Expression expression, int kind, StaticContext env) throws XPathException {
        BuiltInAtomicType.analyzeContentExpression(this, expression, env, kind);
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @param nameChecker
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    public ValidationException validateContent(CharSequence value, NamespaceResolver nsResolver, NameChecker nameChecker) {
        SimpleType base = getItemType();
        SequenceIterator iter = new StringTokenIterator(value.toString());
        ValidationException result = null;
        int count = 0;
        try {
            while (true) {
                StringValue val = (StringValue)iter.next();
                if (val == null) break;
                count++;
                ValidationException v = base.validateContent(val.getStringValue(), nsResolver, nameChecker);
                if (v != null) {
                    return v;
                }
            }
        } catch (ValidationException err) {
            result = err;
        } catch (XPathException err) {
            result = new ValidationException(err);
        }
        if (count == 0) {
            result = new ValidationException("The built-in list type " +
                    StandardNames.getDisplayName(fingerprint) +
                    " does not allow a zero-length list");
        }
        return result;
    }

    /**
     * Get the typed value of a given input string. This method assumes that the input value
     * is valid according to this SimpleType
     * @param value the string whose typed value is required
     * @param resolver
     * @param nameChecker
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver, NameChecker nameChecker) throws ValidationException {
        SequenceIterator iter = new StringTokenIterator(value.toString());
        ListTypeMappingFunction map = new ListTypeMappingFunction();
        map.resolver = resolver;
        map.atomicType = (AtomicType)getItemType();
        map.nameChecker = nameChecker;
        return new MappingIterator(iter, map);
    }

    private static class ListTypeMappingFunction implements MappingFunction {

        public NamespaceResolver resolver;
        public AtomicType atomicType;
        public NameChecker nameChecker;

        /**
         * The typed value of a list-valued node is obtained by tokenizing the string value and
         * applying a mapping function to the sequence of tokens.
         * This method implements the mapping function. It is for internal use only.
         * For details see {@link org.orbeon.saxon.expr.MappingFunction}
        */

        public Object map(Item item) throws XPathException {
            try {
                return atomicType.getTypedValue(item.getStringValue(), resolver, nameChecker);
            } catch (ValidationException err) {
                return new DynamicError(err);
            }
        }
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
// The Initial Developer of the Original Code is Saxonica Limited.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//


