package net.sf.saxon.type;

import net.sf.saxon.expr.*;
import net.sf.saxon.functions.NormalizeSpace;
import net.sf.saxon.om.*;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;

/**
 * <p>This class is used to implement the built-in
 * list types NMTOKENS, ENTITIES, IDREFS. In the schema-aware product it is also
 * used to support user-defined list types.</p>
 *
**/

public class BuiltInListType implements ListType, MappingFunction, Serializable {

    private int fingerprint;

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
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     */

    public boolean isListType() {
        return true;
    }

    public boolean isUnionType() {
        return false;
    }

    public SchemaType getBuiltInBaseType() throws ValidationException {
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
     *         node. The objects returned by this iterator are of type {@link net.sf.saxon.value.AtomicValue}
     */

    public SequenceIterator getTypedValue(NodeInfo node) throws XPathException {
        try {
            return getTypedValue(node.getStringValue(), new InscopeNamespaceResolver(node));
        } catch (ValidationException err) {
            throw new DynamicError("Internal error: value doesn't match its type annotation. " + err.getMessage());
        }
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

    public void isTypeDerivationOK(SchemaType type, int block) throws SchemaException, ValidationException {
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
     * @throws net.sf.saxon.trans.XPathException
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
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    public ValidationException validateContent(CharSequence value, NamespaceResolver nsResolver) {
        SimpleType base = getItemType();
        SequenceIterator iter = new StringTokenIterator(value.toString());
        ValidationException result = null;
        try {
            while (true) {
                StringValue val = (StringValue)iter.next();
                if (val == null) break;
                ValidationException v = base.validateContent(val.getStringValue(), nsResolver);
                if (v != null) {
                    return v;
                }
            }
        } catch (ValidationException err) {
            result = err;
        } catch (XPathException err) {
            result = new ValidationException(err);
        }
        return result;
    }

    /**
     * Get the typed value of a given input string. This method assumes that the input value
     * is valid according to this SimpleType
     * @param value the string whose typed value is required
     * @param resolver
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver) throws ValidationException {
        SequenceIterator iter = new StringTokenIterator(value.toString());
        Object[] data = {resolver, getItemType()};
        return new MappingIterator(iter, this, null, data);
    }

    /**
     * The typed value of a list-valued node is obtained by tokenizing the string value and
     * applying a mapping function to the sequence of tokens.
     * This method implements the mapping function. It is for internal use only.
     * For details see {@link net.sf.saxon.expr.MappingFunction}
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        try {
            NamespaceResolver resolver = (NamespaceResolver)((Object[])info)[0];
            AtomicType type = (AtomicType)((Object[])info)[1];
            return type.getTypedValue(item.getStringValue(), resolver);
        } catch (ValidationException err) {
            return new DynamicError(err);
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


