package net.sf.saxon.type;

import net.sf.saxon.Err;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.functions.NormalizeSpace;
import net.sf.saxon.instruct.ValueOf;
import net.sf.saxon.om.*;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;

import java.io.Serializable;

/**
 * This class represents a built-in atomic type, which may be either a primitive type
 * (such as xs:decimal or xs:anyURI) or a derived type (such as xs:ID or xdt:dayTimeDuration).
 */

public class BuiltInAtomicType implements AtomicType, Serializable {

    int fingerprint;
    int baseFingerprint = -1;

    public BuiltInAtomicType() {}

    public BuiltInAtomicType(int fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     * @return the lowest common supertype of all member types
     */

    public AtomicType getCommonAtomicType() {
        return this;
    }

    /**
     * Get the validation status - always valid
     */
    public final int getValidationStatus()  {
        return VALIDATED;
    }
    
    /**
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link SchemaType#DERIVATION_LIST} and {@link SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public final int getBlock() {
        return 0;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example {@link SchemaType#DERIVATION_RESTRICTION}
     */

    public final int getDerivationMethod() {
        return SchemaType.DERIVATION_RESTRICTION;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public final boolean allowsDerivation(int derivation) {
        return true;
    }

    public final void setBaseTypeFingerprint(int baseFingerprint) {
        this.baseFingerprint = baseFingerprint;
    }

    /**
     * Get the fingerprint of the name of this type
     *
     * @return the fingerprint. Returns an invented fingerprint for an anonymous type.
     */

    public int getFingerprint() {
        return fingerprint;
    }

    /**
     * Get the namecode of the name of this type. This includes the prefix from the original
     * type declaration: in the case of built-in types, there may be a conventional prefix
     * or there may be no prefix.
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

    public final boolean isComplexType() {
        return false;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    public final SchemaType getBaseType() {
        if (baseFingerprint == -1) {
            return null;
        } else {
            return BuiltInSchemaFactory.getSchemaType(baseFingerprint);
        }
    }

    /**
     * Test whether a given item conforms to this type
     *
     * @param item The item to be tested
     * @return true if the item is an instance of this type; false otherwise
     */

    public boolean matchesItem(Item item) {
        if (item instanceof AtomicValue) {
            AtomicValue value = (AtomicValue)item;
            AtomicType type = (AtomicType)value.getItemType();
            if (type.getFingerprint()==this.getFingerprint()) {
                // note, with compiled stylesheets one can have two objects representing
                // the same type, so comparing identity is not safe
                return true;
            }
            return Type.isSubType(type, this);
        } else {
            return false;
        }
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xdt:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     *
     * @return the supertype, or null if this type is item()
     */

    public ItemType getSuperType() {
        SchemaType base = getBaseType();
        if (base instanceof AnySimpleType) {
            return AnyItemType.getInstance();
        } else {
            return (ItemType)base;
        }
    }

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public ItemType getPrimitiveItemType() {
        if (Type.isPrimitiveType(getFingerprint())) {
             return this;
         } else {
             ItemType s = (ItemType)getBaseType();
             if (s instanceof AtomicType) {
                 return s.getPrimitiveItemType();
             } else {
                 return this;
             }
         }
    }

    /**
     * Get the primitive type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public int getPrimitiveType() {
        int x = getFingerprint();
        if (Type.isPrimitiveType(x)) {
            return x;
        } else {
            SchemaType s = getBaseType();
            if (s instanceof AtomicType) {
                return ((AtomicType)s).getPrimitiveType();
            } else {
                return this.getFingerprint();
            }
        }
    }

    /**
     * Produce a representation of this type name for use in error messages.
     * Where this is a QName, it will use conventional prefixes
     */

    public String toString(NamePool pool) {
        return getDisplayName();
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public AtomicType getAtomizedItemType() {
        return this;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    public SchemaType getKnownBaseType() {
        return getBaseType();
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return (other.getFingerprint() == this.getFingerprint());
    }

    public String getDescription() {
        return getDisplayName();
    }

    public String toString() {
        return getDisplayName();
    }

    /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void isTypeDerivationOK(SchemaType type, int block) throws SchemaException, ValidationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns true if this SchemaType is a SimpleType
     *
     * @return true (always)
     */

    public final boolean isSimpleType() {
        return true;
    }

    /**
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     *
     * @return true if this is a list type
     */

    public boolean isListType() {
        return false;
    }

    /**
     * Return true if this type is a union type (that is, if its variety is union)
     *
     * @return true for a union type
     */

    public boolean isUnionType() {
        return false;
    }

    /**
     * Determine the whitespace normalization required for values of this type
     *
     * @return one of PRESERVE, REPLACE, COLLAPSE
     */

    public int getWhitespaceAction() {
        if (getPrimitiveType() == Type.STRING) {
            if (Type.isSubType(this,
                    (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_TOKEN))) {
                return Whitespace.COLLAPSE;
            } else if (Type.isSubType(this,
                    (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_NORMALIZED_STRING))) {
                return Whitespace.REPLACE;
            } else {
                return Whitespace.PRESERVE;
            }
        } else {
            return Whitespace.COLLAPSE;
        }
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     *
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public CharSequence applyWhitespaceNormalization(CharSequence value) throws ValidationException {
        int action = getWhitespaceAction();
        switch (action) {
            case Whitespace.PRESERVE:
                return value;
            case Whitespace.REPLACE:
                FastStringBuffer sb = new FastStringBuffer(value.length());
                for (int i = 0; i < value.length(); i++) {
                    if ("\n\r\t".indexOf(value.charAt(i)) >= 0) {
                        sb.append(' ');
                    } else {
                        sb.append(value.charAt(i));
                    }
                }
                return sb;
            case Whitespace.COLLAPSE:
                return NormalizeSpace.normalize(value.toString());
            default:
                throw new IllegalArgumentException("Unknown whitespace facet value");
        }
    }

    /**
     * Returns the built-in base type this type is derived from.
     *
     * @return the first built-in type found when searching up the type hierarchy
     */
    public SchemaType getBuiltInBaseType() throws ValidationException {
        BuiltInAtomicType base = this;
        while ((base != null) && (base.getFingerprint() > 1023)) {
            base = (BuiltInAtomicType)base.getBaseType();
        }
        return base;
    }

    /**
     * Test whether this simple type is namespace-sensitive, that is, whether
     * it is derived from xs:QName or xs:NOTATION
     *
     * @return true if this type is derived from xs:QName or xs:NOTATION
     */

    public boolean isNamespaceSensitive() {
        BuiltInAtomicType base = this;
        int fp = base.getFingerprint();
        while (fp > 1023) {
            base = (BuiltInAtomicType)base.getBaseType();
            fp = base.getFingerprint();
        }

        if (fp == StandardNames.XS_QNAME || fp == StandardNames.XS_NOTATION) {
            return true;
        }
        return false;
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     *
     * @param value      the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     *                   is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     *                   content will throw an UnsupportedOperationException.
     * @return XPathException if the value is invalid. Note that the exception is returned rather than being thrown.
     * Returns null if the value is valid.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     *                                       resolver is supplied
     */

    public ValidationException validateContent(CharSequence value, NamespaceResolver nsResolver) {
        int f = getFingerprint();
        if (f==StandardNames.XS_STRING ||
                f==StandardNames.XS_ANY_SIMPLE_TYPE ||
                f==StandardNames.XDT_UNTYPED_ATOMIC ||
                f==StandardNames.XDT_ANY_ATOMIC_TYPE) {
            return null;
        }
        ValidationException result = null;
        if (isNamespaceSensitive()) {
            if (nsResolver == null) {
                throw new UnsupportedOperationException("Cannot validate a QName without a namespace resolver");
            }
            try {
                String[] parts = Name.getQNameParts(value.toString());
                String uri = nsResolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    result = new ValidationException("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                new QNameValue(parts[0], uri, parts[1]);
            } catch (QNameException err) {
                result = new ValidationException("Invalid lexical QName " + Err.wrap(value));
            } catch (XPathException err) {
                result = new ValidationException(err.getMessage());
            }
        } else {
            Value v = new StringValue(value).convert(this, null, true);
            if (v instanceof ErrorValue) {
                result = new ValidationException("Value " + Err.wrap(value, Err.VALUE) + " is invalid for type "
                        + getDisplayName() + ". " + ((ErrorValue)v).getException().getMessage());
            }
        }
        return result;
    }

    /**
     * Get the typed value of a node that is annotated with this schema type
     *
     * @param node the node whose typed value is required
     * @return an iterator over the items making up the typed value of this node. The objects
     *         returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue}
     */

    public final SequenceIterator getTypedValue(NodeInfo node)
            throws XPathException {
        try {
            return getTypedValue(node.getStringValue(), new InscopeNamespaceResolver(node));
        } catch (ValidationException err) {
            throw new DynamicError("Internal error: value doesn't match its type annotation. " + err.getMessage());
        }
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

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver)
            throws ValidationException {
        // Fast path for common cases
        if (fingerprint == StandardNames.XS_STRING) {
            return SingletonIterator.makeIterator(new StringValue(value));
        } else if (fingerprint == StandardNames.XDT_UNTYPED_ATOMIC) {
            return SingletonIterator.makeIterator(new UntypedAtomicValue(value));
        }
        AtomicValue val = new StringValue(value).convert(this, null, true);
        if (val instanceof ErrorValue) {
            throw ((ErrorValue)val).getException();
        }
        return SingletonIterator.makeIterator(val);
    }


    /**
     * Factory method to create values of a derived atomic type. This method
     * is not used to create values of a built-in type, even one that is not
     * primitive.
     *
     * @param primValue    the value in the value space of the primitive type
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
     * @param validate     true if the value is to be validated against the facets of the derived
     *                     type; false if the caller knows that the value is already valid.
     */

    public AtomicValue makeDerivedValue(AtomicValue primValue, CharSequence lexicalValue, boolean validate) {
        throw new UnsupportedOperationException("makeDerivedValue is not supported for built-in types");
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
        analyzeContentExpression(this, expression, env, kind);
    }

   /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     * @param simpleType the simple type against which the expression is to be checked
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link Type#ELEMENT},
     *                   {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public static void analyzeContentExpression(SimpleType simpleType, Expression expression, StaticContext env, int kind)
    throws XPathException {
        if (kind == Type.ELEMENT) {
            expression.checkPermittedContents(simpleType, env, true);
//            // if we are building the content of an element or document, no atomization will take
//            // place, and therefore the presence of any element or attribute nodes in the content will
//            // cause a validity error, since only simple content is allowed
//            if (Type.isSubType(itemType, NodeKindTest.makeNodeKindTest(Type.ELEMENT))) {
//                throw new StaticError("The content of an element with a simple type must not include any element nodes");
//            }
//            if (Type.isSubType(itemType, NodeKindTest.makeNodeKindTest(Type.ATTRIBUTE))) {
//                throw new StaticError("The content of an element with a simple type must not include any attribute nodes");
//            }
        } else if (kind == Type.ATTRIBUTE) {
            // for attributes, do a check only for text nodes and atomic values: anything else gets atomized
            if (expression instanceof ValueOf || expression instanceof Value) {
                expression.checkPermittedContents(simpleType, env, true);
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//