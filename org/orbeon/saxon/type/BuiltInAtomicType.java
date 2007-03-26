package org.orbeon.saxon.type;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.instruct.ValueOf;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;

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
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     */

    public boolean isExternalType() {
        return false;
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
     * Returns the value of the 'block' attribute for this type, as a bit-significant
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
     * Test whether this is an anonymous type
     * @return true if this SchemaType is an anonymous type
     */

    public boolean isAnonymousType() {
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
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
     */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        if (item instanceof AtomicValue) {
            AtomicValue value = (AtomicValue)item;
            AtomicType type = (AtomicType)value.getItemType(config.getTypeHierarchy());
            if (type.getFingerprint()==this.getFingerprint()) {
                // note, with compiled stylesheets one can have two objects representing
                // the same type, so comparing identity is not safe
                return true;
            }
            final TypeHierarchy th = config.getTypeHierarchy();
            boolean ok = th.isSubType(type, this);
            if (ok) {
                return true;
            }
            if (allowURIPromotion && this.getFingerprint() == Type.STRING && th.isSubType(type, Type.ANY_URI_TYPE)) {
                // allow promotion from anyURI to string
                return true;
            }
        }
        return false;
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xdt:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     *
     * @return the supertype, or null if this type is item()
     * @param th
     */

    public ItemType getSuperType(TypeHierarchy th) {
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
             if (s.isAtomicType()) {
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
            if (s.isAtomicType()) {
                return ((AtomicType)s).getPrimitiveType();
            } else {
                return this.getFingerprint();
            }
        }
    }

    /**
     * Determine whether this type is supported in a basic XSLT processor
     */

    public boolean isAllowedInBasicXSLT() {
        int fp = getFingerprint();
        return (Type.isPrimitiveType(fp) && fp != StandardNames.XS_NOTATION);
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

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException, ValidationException {
        // method not used
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
     * Test whether this Simple Type is an atomic type
     * @return true, this is an atomic type
     */

    public boolean isAtomicType() {
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
     * @param th
     */

    public int getWhitespaceAction(TypeHierarchy th) {
        if (getPrimitiveType() == Type.STRING) {
            if (th.isSubType(this, (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_TOKEN))) {
                return Whitespace.COLLAPSE;
            } else if (th.isSubType(this,
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
     * Returns the built-in base type this type is derived from.
     *
     * @return the first built-in type found when searching up the type hierarchy
     */
    public SchemaType getBuiltInBaseType() {
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
     * @param nameChecker
     * @return XPathException if the value is invalid. Note that the exception is returned rather than being thrown.
     * Returns null if the value is valid.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     *                                       resolver is supplied
     */

    public ValidationException validateContent(CharSequence value, NamespaceResolver nsResolver,
                                               NameChecker nameChecker) {
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
                String[] parts = nameChecker.getQNameParts(value.toString());
                String uri = nsResolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    result = new ValidationException("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                new QNameValue(parts[0], uri, parts[1], nameChecker);
            } catch (QNameException err) {
                result = new ValidationException("Invalid lexical QName " + Err.wrap(value));
            } catch (XPathException err) {
                result = new ValidationException(err.getMessage());
            }
        } else {

            Value v = StringValue.convertStringToBuiltInType(value, this, nameChecker);
            if (v instanceof ValidationErrorValue) {
                result = ((ValidationErrorValue)v).getException();
//                result = new ValidationException("Value " + Err.wrap(value, Err.VALUE) + " is invalid for type "
//                        + getDisplayName() + ". " + ((ValidationErrorValue)v).getException().getMessage());
            }
        }
        return result;
    }

    /**
     * Get the typed value of a node that is annotated with this schema type
     *
     * @param node the node whose typed value is required
     * @return an iterator over the items making up the typed value of this node. The objects
     *         returned by this SequenceIterator will all be of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    public final SequenceIterator getTypedValue(NodeInfo node)
            throws XPathException {
        try {
            return getTypedValue(node.getStringValue(),
                    new InscopeNamespaceResolver(node),
                    node.getConfiguration().getNameChecker());
        } catch (ValidationException err) {
            throw new DynamicError("Internal error: value doesn't match its type annotation. " + err.getMessage());
        }
    }

    /**
     * Get the typed value of a node that is annotated with this schema type.
     * The result of this method will always be consistent with the method
     * {@link #getTypedValue}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @param node the node whose typed value is required
     * @return the typed value.
     * @since 8.5
     */

    public Value atomize(NodeInfo node) throws XPathException {
                // Fast path for common cases
        if (fingerprint == StandardNames.XS_STRING) {
            return StringValue.makeStringValue(node.getStringValueCS());
        } else if (fingerprint == StandardNames.XDT_UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(node.getStringValueCS());
        }
        final NameChecker checker = node.getConfiguration().getNameChecker();
        if (isNamespaceSensitive()) {
            try {
                NamespaceResolver resolver = new InscopeNamespaceResolver(node);
                String[] parts = checker.getQNameParts(node.getStringValueCS());
                String uri = resolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    throw new ValidationException("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                return new QNameValue(parts[0], uri, parts[1], checker);
            } catch (QNameException err) {
                throw new ValidationException("Invalid lexical QName " + Err.wrap(node.getStringValueCS()));
            } catch (XPathException err) {
                throw new ValidationException(err.getMessage());
            }
        }
        AtomicValue val = StringValue.convertStringToBuiltInType(node.getStringValueCS(), this, checker);
        if (val instanceof ValidationErrorValue) {
            throw ((ValidationErrorValue)val).getException();
        }
        return val;
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

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver, NameChecker nameChecker)
            throws ValidationException {
        // Fast path for common cases
        if (fingerprint == StandardNames.XS_STRING) {
            return SingletonIterator.makeIterator(StringValue.makeStringValue(value));
        } else if (fingerprint == StandardNames.XDT_UNTYPED_ATOMIC) {
            return SingletonIterator.makeIterator(new UntypedAtomicValue(value));
        } else if (isNamespaceSensitive()) {
            try {
                String[] parts = nameChecker.getQNameParts(value.toString());
                String uri = resolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    throw new ValidationException("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                return SingletonIterator.makeIterator(new QNameValue(parts[0], uri, parts[1], nameChecker));
            } catch (QNameException err) {
                throw new ValidationException("Invalid lexical QName " + Err.wrap(value));
            } catch (XPathException err) {
                throw new ValidationException(err.getMessage());
            }
        }
        // TODO: if we really can assume validity, we should set nameChecker to null in the following call
        AtomicValue val = StringValue.convertStringToBuiltInType(value, this, nameChecker);
        if (val instanceof ValidationErrorValue) {
            throw ((ValidationErrorValue)val).getException();
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
     * @throws org.orbeon.saxon.trans.XPathException
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
     * @throws org.orbeon.saxon.trans.XPathException
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