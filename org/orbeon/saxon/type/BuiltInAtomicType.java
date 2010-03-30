package org.orbeon.saxon.type;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.Literal;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.instruct.ValueOf;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;

import java.io.Serializable;

/**
 * This class represents a built-in atomic type, which may be either a primitive type
 * (such as xs:decimal or xs:anyURI) or a derived type (such as xs:ID or xs:dayTimeDuration).
 */

public class BuiltInAtomicType implements AtomicType, Serializable {

    int fingerprint;
    int baseFingerprint;
    int primitiveFingerprint;
    boolean ordered = false;
    //private static boolean initialized = false;

    public static BuiltInAtomicType ANY_ATOMIC = makeAtomicType(StandardNames.XS_ANY_ATOMIC_TYPE, AnySimpleType.getInstance(), true);

    public static BuiltInAtomicType NUMERIC = makeAtomicType(StandardNames.XS_NUMERIC, ANY_ATOMIC, true);

    public static BuiltInAtomicType STRING = makeAtomicType(StandardNames.XS_STRING, ANY_ATOMIC, true);

    public static BuiltInAtomicType BOOLEAN = makeAtomicType(StandardNames.XS_BOOLEAN, ANY_ATOMIC, true);

    public static BuiltInAtomicType DURATION = makeAtomicType(StandardNames.XS_DURATION, ANY_ATOMIC, false);

    public static BuiltInAtomicType DATE_TIME = makeAtomicType(StandardNames.XS_DATE_TIME, ANY_ATOMIC, true);

    public static BuiltInAtomicType DATE = makeAtomicType(StandardNames.XS_DATE, ANY_ATOMIC, true);

    public static BuiltInAtomicType TIME = makeAtomicType(StandardNames.XS_TIME, ANY_ATOMIC, true);

    public static BuiltInAtomicType G_YEAR_MONTH = makeAtomicType(StandardNames.XS_G_YEAR_MONTH, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_MONTH = makeAtomicType(StandardNames.XS_G_MONTH, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_MONTH_DAY = makeAtomicType(StandardNames.XS_G_MONTH_DAY, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_YEAR = makeAtomicType(StandardNames.XS_G_YEAR, ANY_ATOMIC, false);

    public static BuiltInAtomicType G_DAY = makeAtomicType(StandardNames.XS_G_DAY, ANY_ATOMIC, false);

    public static BuiltInAtomicType HEX_BINARY = makeAtomicType(StandardNames.XS_HEX_BINARY, ANY_ATOMIC, false);

    public static BuiltInAtomicType BASE64_BINARY = makeAtomicType(StandardNames.XS_BASE64_BINARY, ANY_ATOMIC, false);

    public static BuiltInAtomicType ANY_URI = makeAtomicType(StandardNames.XS_ANY_URI, ANY_ATOMIC, true);;

    public static BuiltInAtomicType QNAME = makeAtomicType(StandardNames.XS_QNAME, ANY_ATOMIC, false);

    public static BuiltInAtomicType NOTATION = makeAtomicType(StandardNames.XS_NOTATION, ANY_ATOMIC, false);

    public static BuiltInAtomicType UNTYPED_ATOMIC = makeAtomicType(StandardNames.XS_UNTYPED_ATOMIC, ANY_ATOMIC, true);

    public static BuiltInAtomicType DECIMAL = makeAtomicType(StandardNames.XS_DECIMAL, NUMERIC, true);

    public static BuiltInAtomicType FLOAT = makeAtomicType(StandardNames.XS_FLOAT, NUMERIC, true);

    public static BuiltInAtomicType DOUBLE = makeAtomicType(StandardNames.XS_DOUBLE, NUMERIC, true);

    public static BuiltInAtomicType INTEGER = makeAtomicType(StandardNames.XS_INTEGER, DECIMAL, true);

    public static BuiltInAtomicType NON_POSITIVE_INTEGER = makeAtomicType(StandardNames.XS_NON_POSITIVE_INTEGER, INTEGER, true);

    public static BuiltInAtomicType NEGATIVE_INTEGER = makeAtomicType(StandardNames.XS_NEGATIVE_INTEGER, NON_POSITIVE_INTEGER, true);

    public static BuiltInAtomicType LONG = makeAtomicType(StandardNames.XS_LONG, INTEGER, true);

    public static BuiltInAtomicType INT = makeAtomicType(StandardNames.XS_INT, LONG, true);

    public static BuiltInAtomicType SHORT = makeAtomicType(StandardNames.XS_SHORT, INT, true);

    public static BuiltInAtomicType BYTE = makeAtomicType(StandardNames.XS_BYTE, SHORT, true);

    public static BuiltInAtomicType NON_NEGATIVE_INTEGER = makeAtomicType(StandardNames.XS_NON_NEGATIVE_INTEGER, INTEGER, true);

    public static BuiltInAtomicType POSITIVE_INTEGER = makeAtomicType(StandardNames.XS_POSITIVE_INTEGER, NON_NEGATIVE_INTEGER, true);

    public static BuiltInAtomicType UNSIGNED_LONG = makeAtomicType(StandardNames.XS_UNSIGNED_LONG, NON_NEGATIVE_INTEGER, true);

    public static BuiltInAtomicType UNSIGNED_INT = makeAtomicType(StandardNames.XS_UNSIGNED_INT, UNSIGNED_LONG, true);

    public static BuiltInAtomicType UNSIGNED_SHORT = makeAtomicType(StandardNames.XS_UNSIGNED_SHORT, UNSIGNED_INT, true);

    public static BuiltInAtomicType UNSIGNED_BYTE = makeAtomicType(StandardNames.XS_UNSIGNED_BYTE, UNSIGNED_SHORT, true);

    public static BuiltInAtomicType YEAR_MONTH_DURATION = makeAtomicType(StandardNames.XS_YEAR_MONTH_DURATION, DURATION, true);

    public static BuiltInAtomicType DAY_TIME_DURATION = makeAtomicType(StandardNames.XS_DAY_TIME_DURATION, DURATION, true);

    public static BuiltInAtomicType NORMALIZED_STRING = makeAtomicType(StandardNames.XS_NORMALIZED_STRING, STRING, true);

    public static BuiltInAtomicType TOKEN = makeAtomicType(StandardNames.XS_TOKEN, NORMALIZED_STRING, true);

    public static BuiltInAtomicType LANGUAGE = makeAtomicType(StandardNames.XS_LANGUAGE, TOKEN, true);

    public static BuiltInAtomicType NAME = makeAtomicType(StandardNames.XS_NAME, TOKEN, true);

    public static BuiltInAtomicType NMTOKEN = makeAtomicType(StandardNames.XS_NMTOKEN, TOKEN, true);

    public static BuiltInAtomicType NCNAME = makeAtomicType(StandardNames.XS_NCNAME, NAME, true);

    public static BuiltInAtomicType ID = makeAtomicType(StandardNames.XS_ID, NCNAME, true);

    public static BuiltInAtomicType IDREF = makeAtomicType(StandardNames.XS_IDREF, NCNAME, true);

    public static BuiltInAtomicType ENTITY = makeAtomicType(StandardNames.XS_ENTITY, NCNAME, true);

    /**
     * Static initialization
     */

//    public static void init() {
//        if (initialized) {
//            return;
//        }
//        initialized = true;
//
//        DECIMAL = makeAtomicType(StandardNames.XS_DECIMAL, NUMERIC);
//        FLOAT = makeAtomicType(StandardNames.XS_FLOAT, NUMERIC);
//        DOUBLE = makeAtomicType(StandardNames.XS_DOUBLE, NUMERIC);
//        INTEGER = makeAtomicType(StandardNames.XS_INTEGER, DECIMAL);
//        NON_POSITIVE_INTEGER = makeAtomicType(StandardNames.XS_NON_POSITIVE_INTEGER, INTEGER);
//        NEGATIVE_INTEGER = makeAtomicType(StandardNames.XS_NEGATIVE_INTEGER, NON_POSITIVE_INTEGER);
//        LONG = makeAtomicType(StandardNames.XS_LONG, INTEGER);
//        INT = makeAtomicType(StandardNames.XS_INT, LONG);
//        SHORT = makeAtomicType(StandardNames.XS_SHORT, INT);
//        BYTE = makeAtomicType(StandardNames.XS_BYTE, SHORT);
//        NON_NEGATIVE_INTEGER = makeAtomicType(StandardNames.XS_NON_NEGATIVE_INTEGER, INTEGER);
//        POSITIVE_INTEGER = makeAtomicType(StandardNames.XS_POSITIVE_INTEGER, NON_NEGATIVE_INTEGER);
//        UNSIGNED_LONG = makeAtomicType(StandardNames.XS_UNSIGNED_LONG, NON_NEGATIVE_INTEGER);
//        UNSIGNED_INT = makeAtomicType(StandardNames.XS_UNSIGNED_INT, UNSIGNED_LONG);
//        UNSIGNED_SHORT = makeAtomicType(StandardNames.XS_UNSIGNED_SHORT, UNSIGNED_INT);
//        UNSIGNED_BYTE = makeAtomicType(StandardNames.XS_UNSIGNED_BYTE, UNSIGNED_SHORT);
//        YEAR_MONTH_DURATION = makeAtomicType(StandardNames.XS_YEAR_MONTH_DURATION, DURATION);
//        YEAR_MONTH_DURATION.ordered = true;
//        DAY_TIME_DURATION = makeAtomicType(StandardNames.XS_DAY_TIME_DURATION, DURATION);
//        DAY_TIME_DURATION.ordered = true;
//        NORMALIZED_STRING = makeAtomicType(StandardNames.XS_NORMALIZED_STRING, STRING);
//        TOKEN = makeAtomicType(StandardNames.XS_TOKEN, NORMALIZED_STRING);
//        LANGUAGE = makeAtomicType(StandardNames.XS_LANGUAGE, TOKEN);
//        NAME = makeAtomicType(StandardNames.XS_NAME, TOKEN);
//        NMTOKEN = makeAtomicType(StandardNames.XS_NMTOKEN, TOKEN);
//        NCNAME = makeAtomicType(StandardNames.XS_NCNAME, NAME);
//        ID = makeAtomicType(StandardNames.XS_ID, NCNAME);
//        IDREF = makeAtomicType(StandardNames.XS_IDREF, NCNAME);
//        ENTITY = makeAtomicType(StandardNames.XS_ENTITY, NCNAME);
//
//        ANY_ATOMIC.ordered = true;  // give it the benefit of the doubt: used only for static types
//
//    }


    private BuiltInAtomicType(int fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Get the local name of this type
     *
     * @return the local name of this type definition, if it has one. Return null in the case of an
     *         anonymous type.
     */

    public String getName() {
        if (fingerprint == StandardNames.XS_NUMERIC) {
            return "numeric";
        } else {
            return StandardNames.getLocalName(fingerprint);
        }
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
     * Determine whether the type is abstract, that is, whether it cannot have instances that are not also
     * instances of some concrete subtype
     */

    public boolean isAbstract() {
        switch (fingerprint) {
            case StandardNames.XS_NOTATION:
            case StandardNames.XS_ANY_ATOMIC_TYPE:
            case StandardNames.XS_NUMERIC:
            case StandardNames.XS_ANY_SIMPLE_TYPE:
                return true;
            default:
                return false;
        }
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
     * Determine whether the atomic type is ordered, that is, whether less-than and greater-than comparisons
     * are permitted
     *
     * @return true if ordering operations are permitted
     */

    public boolean isOrdered() {
        return ordered;
    }


    /**
     * Get the URI of the schema document where the type was originally defined.
     *
     * @return the URI of the schema document. Returns null if the information is unknown or if this
     *         is a built-in type
     */

    public String getSystemId() {
        return null;
    }

    /**
     * Determine whether the atomic type is numeric
     *
     * @return true if the type is a built-in numeric type
     */

    public boolean isPrimitiveNumeric() {
        switch (fingerprint) {
        case StandardNames.XS_INTEGER:
        case StandardNames.XS_DECIMAL:
        case StandardNames.XS_DOUBLE:
        case StandardNames.XS_FLOAT:
        case StandardNames.XS_NUMERIC:
            return true;
        default:
            return false;
        }
    }

    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     *
     * @return the lowest common supertype of all member types
     */

    public AtomicType getCommonAtomicType() {
        return this;
    }

    /**
     * Get the validation status - always valid
     */
    public final int getValidationStatus() {
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

    /**
     * Set the base type of this type
     *
     * @param baseFingerprint the namepool fingerprint of the name of the base type
     */

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
     * Get the name of the type as a QName
     *
     * @return a StructuredQName containing the name of the type. The conventional prefix "xs" is used
     *         to represent the XML Schema namespace
     */

    public StructuredQName getQualifiedName() {
        return new StructuredQName("xs", NamespaceConstant.SCHEMA, StandardNames.getLocalName(fingerprint));
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        if (fingerprint == StandardNames.XS_NUMERIC) {
            return "numeric";
        } else {
            return StandardNames.getDisplayName(fingerprint);
        }
    }


    /**
     * Determine whether the atomic type is a primitive type.  The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; and all supertypes of these (xs:anyAtomicType, xs:numeric, ...)
     *
     * @return true if the type is considered primitive under the above rules
     */

    public boolean isPrimitiveType() {
        return Type.isPrimitiveType(fingerprint);
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
     *
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
            return BuiltInType.getSchemaType(baseFingerprint);
        }
    }

    /**
     * Test whether a given item conforms to this type
     *
     * @param item              The item to be tested
     * @param allowURIPromotion true if we regard a URI as effectively a subtype of String
     * @param config            the Saxon configuration (used to locate the type hierarchy cache)
     * @return true if the item is an instance of this type; false otherwise
     */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        if (item instanceof AtomicValue) {
            AtomicValue value = (AtomicValue)item;
            // Try to match primitive types first
            if (value.getPrimitiveType() == this) {
                return true;
            }
            AtomicType type = value.getTypeLabel();
            if (type.getFingerprint() == getFingerprint()) {
                // note, with compiled stylesheets one can have two objects representing
                // the same type, so comparing identity is not safe
                return true;
            }
            final TypeHierarchy th = config.getTypeHierarchy();
            boolean ok = th.isSubType(type, this);
            if (ok) {
                return true;
            }
            if (allowURIPromotion && getFingerprint() == StandardNames.XS_STRING && th.isSubType(type, BuiltInAtomicType.ANY_URI)) {
                // allow promotion from anyURI to string
                return true;
            }
        }
        return false;
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     *
     * @param th the type hierarchy cache, not used in this implementation
     * @return the supertype, or null if this type is item()
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
        if (isPrimitiveType()) {
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
        return primitiveFingerprint;
    }

    /**
     * Determine whether this type is supported in a basic XSLT processor
     *
     * @return true if this type is permitted in a basic XSLT processor
     */

    public boolean isAllowedInBasicXSLT() {
        return (isPrimitiveType() && getFingerprint() != StandardNames.XS_NOTATION);
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
        return other.getFingerprint() == getFingerprint();
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

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException {
        if (type == AnySimpleType.getInstance()) {
            // OK
        } else if (isSameType(type)) {
            // OK
        } else {
            SchemaType base = getBaseType();
            if (base == null) {
                throw new SchemaException("Type " + getDescription() +
                        " is not validly derived from " + type.getDescription());
            }
            try {
                base.checkTypeDerivationIsOK(type, block);
            } catch (SchemaException se) {
                throw new SchemaException("Type " + getDescription() +
                        " is not validly derived from " + type.getDescription());
            }
        }
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
     *
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
     * @param th the type hierarchy cache
     * @return one of PRESERVE, REPLACE, COLLAPSE
     */

    public int getWhitespaceAction(TypeHierarchy th) {
        switch (getFingerprint()) {
            case StandardNames.XS_STRING:
                return Whitespace.PRESERVE;
            case StandardNames.XS_NORMALIZED_STRING:
                return Whitespace.REPLACE;
            default:
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

        return fp == StandardNames.XS_QNAME || fp == StandardNames.XS_NOTATION;
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     *
     * @param value       the input string to be checked
     * @param nsResolver  a namespace resolver used to resolve namespace prefixes if the type
     *                    is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     *                    content will throw an UnsupportedOperationException.
     * @param nameChecker XML 1.0 or 1.1 name checker. Must not be null.
     * @return XPathException if the value is invalid. Note that the exception is returned rather than being thrown.
     *         Returns null if the value is valid.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     *                                       resolver is supplied
     */

    public ValidationFailure validateContent(CharSequence value, NamespaceResolver nsResolver,
                                             NameChecker nameChecker) {
        int f = getFingerprint();
        if (f == StandardNames.XS_STRING ||
                f == StandardNames.XS_ANY_SIMPLE_TYPE ||
                f == StandardNames.XS_UNTYPED_ATOMIC ||
                f == StandardNames.XS_ANY_ATOMIC_TYPE) {
            return null;
        }
        ValidationFailure result = null;
        if (isNamespaceSensitive()) {
            if (nsResolver == null) {
                throw new UnsupportedOperationException("Cannot validate a QName without a namespace resolver");
            }
            try {
                String[] parts = nameChecker.getQNameParts(Whitespace.trimWhitespace(value));
                String uri = nsResolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    result = new ValidationFailure("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, nameChecker);
            } catch (QNameException err) {
                result = new ValidationFailure("Invalid lexical QName " + Err.wrap(value));
            } catch (XPathException err) {
                result = new ValidationFailure(err.getMessage());
            }
        } else {
            ConversionResult v = StringValue.convertStringToBuiltInType(value, this, nameChecker);
            if (v instanceof ValidationFailure) {
                result = (ValidationFailure)v;
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
            CharSequence stringValue = node.getStringValueCS();
            if (stringValue.length() == 0 && node.isNilled()) {
                return EmptyIterator.getInstance();
            }
            return getTypedValue(stringValue,
                    new InscopeNamespaceResolver(node),
                    node.getConfiguration().getNameChecker());
        } catch (ValidationException err) {
            throw new XPathException("Internal error: value doesn't match its type annotation. " + err.getMessage());
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
        CharSequence stringValue = node.getStringValueCS();
        if (stringValue.length() == 0 && node.isNilled()) {
            return EmptySequence.getInstance();
        }
        if (fingerprint == StandardNames.XS_STRING) {
            return StringValue.makeStringValue(stringValue);
        } else if (fingerprint == StandardNames.XS_UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(stringValue);
        }
        final NameChecker checker = node.getConfiguration().getNameChecker();
        if (isNamespaceSensitive()) {
            try {
                NamespaceResolver resolver = new InscopeNamespaceResolver(node);
                String[] parts = checker.getQNameParts(Whitespace.trimWhitespace(stringValue));
                String uri = resolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    throw new ValidationException("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                return new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, checker);
            } catch (QNameException err) {
                throw new ValidationException("Invalid lexical QName " + Err.wrap(stringValue));
            } catch (XPathException err) {
                throw new ValidationException(err.getMessage());
            }
        }
        return StringValue.convertStringToBuiltInType(stringValue, this, checker).asAtomic();
    }

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type (and that the containing node is not nilled)
     *
     * @param value       the string value
     * @param resolver    a namespace resolver used to resolve any namespace prefixes appearing
     *                    in the content of values. Can supply null, in which case any namespace-sensitive content
     *                    will be rejected.
     * @param nameChecker XML 1.0 or 1.1 name checker. May be null if name checking is not required
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     *         returned by this SequenceIterator will all be of type {@link AtomicValue}
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver, NameChecker nameChecker)
            throws ValidationException {
        // Fast path for common cases
        if (fingerprint == StandardNames.XS_STRING) {
            return SingletonIterator.makeIterator(StringValue.makeStringValue(value));
        } else if (fingerprint == StandardNames.XS_UNTYPED_ATOMIC) {
            return SingletonIterator.makeIterator(new UntypedAtomicValue(value));
        } else if (fingerprint == StandardNames.XS_QNAME || fingerprint == StandardNames.XS_NOTATION) {
            try {
                String[] parts = nameChecker.getQNameParts(Whitespace.trimWhitespace(value));
                String uri = resolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    throw new ValidationException("Namespace prefix " + Err.wrap(parts[0]) +
                            " has not been declared");
                }
                return SingletonIterator.makeIterator(
                        fingerprint == StandardNames.XS_QNAME ?
                        (QualifiedNameValue)new QNameValue(parts[0], uri, parts[1], this, nameChecker) :
                        (QualifiedNameValue)new NotationValue(parts[0], uri, parts[1], nameChecker));
            } catch (QNameException err) {
                throw new ValidationException("Invalid lexical QName " + Err.wrap(value));
            } catch (XPathException err) {
                throw new ValidationException(err.getMessage());
            }
        }
        // TODO: if we really can assume validity, we should set nameChecker to null in the following call
        AtomicValue val = StringValue.convertStringToBuiltInType(value, this, nameChecker).asAtomic();
        return SingletonIterator.makeIterator(val);
    }

    /**
     * Two types are equal if they have the same fingerprint.
     * Note: it is normally safe to use ==, because we always use the static constants, one instance
     * for each built in atomic type. However, after serialization and deserialization a different instance
     * can appear.
     */

    public boolean equals(Object obj) {
        return obj instanceof BuiltInAtomicType &&
                getFingerprint() == ((BuiltInAtomicType)obj).getFingerprint();
    }

    /**
     * The fingerprint can be used as a hashcode
     */

    public int hashCode() {
        return getFingerprint();
    }


    /**
     * Validate that a primitive atomic value is a valid instance of a type derived from the
     * same primitive type.
     *
     * @param primValue    the value in the value space of the primitive type.
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
     *                     is used. This value is checked against the pattern facet (if any)
     * @param checker Used to check names against XML 1.0 or XML 1.1 syntax rules
     * @return null if the value is valid; otherwise, a ValidationFailure object indicating
     *         the nature of the error.
     * @throws UnsupportedOperationException in the case of an external object type
     */

    public ValidationFailure validate(AtomicValue primValue, CharSequence lexicalValue, NameChecker checker) {
        switch (fingerprint) {
            case StandardNames.XS_NUMERIC:
            case StandardNames.XS_STRING:
            case StandardNames.XS_BOOLEAN:
            case StandardNames.XS_DURATION:
            case StandardNames.XS_DATE_TIME:
            case StandardNames.XS_DATE:
            case StandardNames.XS_TIME:
            case StandardNames.XS_G_YEAR_MONTH:
            case StandardNames.XS_G_MONTH:
            case StandardNames.XS_G_MONTH_DAY:
            case StandardNames.XS_G_YEAR:
            case StandardNames.XS_G_DAY:
            case StandardNames.XS_HEX_BINARY:
            case StandardNames.XS_BASE64_BINARY:
            case StandardNames.XS_ANY_URI:
            case StandardNames.XS_QNAME:
            case StandardNames.XS_NOTATION:
            case StandardNames.XS_UNTYPED_ATOMIC:
            case StandardNames.XS_DECIMAL:
            case StandardNames.XS_FLOAT:
            case StandardNames.XS_DOUBLE:
            case StandardNames.XS_INTEGER:
                return null;
            case StandardNames.XS_NON_POSITIVE_INTEGER:
            case StandardNames.XS_NEGATIVE_INTEGER:
            case StandardNames.XS_LONG:
            case StandardNames.XS_INT:
            case StandardNames.XS_SHORT:
            case StandardNames.XS_BYTE:
            case StandardNames.XS_NON_NEGATIVE_INTEGER:
            case StandardNames.XS_POSITIVE_INTEGER:
            case StandardNames.XS_UNSIGNED_LONG:
            case StandardNames.XS_UNSIGNED_INT:
            case StandardNames.XS_UNSIGNED_SHORT:
            case StandardNames.XS_UNSIGNED_BYTE:
                return ((IntegerValue)primValue).validateAgainstSubType(this);
            case StandardNames.XS_YEAR_MONTH_DURATION:
            case StandardNames.XS_DAY_TIME_DURATION:
                return null;  // treated as primitive
            case StandardNames.XS_NORMALIZED_STRING:
            case StandardNames.XS_TOKEN:
            case StandardNames.XS_LANGUAGE:
            case StandardNames.XS_NAME:
            case StandardNames.XS_NMTOKEN:
            case StandardNames.XS_NCNAME:
            case StandardNames.XS_ID:
            case StandardNames.XS_IDREF:
            case StandardNames.XS_ENTITY:
                return StringValue.validate(
                        this, primValue.getStringValueCS(), checker);
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link Type#ELEMENT},
     *                   {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     * @param env        the static context
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public void analyzeContentExpression(Expression expression, int kind, StaticContext env) throws XPathException {
        analyzeContentExpression(this, expression, env, kind);
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param simpleType the simple type against which the expression is to be checked
     * @param expression the expression that delivers the content
     * @param env        the static context of the expression
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
//                throw new XPathException("The content of an element with a simple type must not include any element nodes");
//            }
//            if (Type.isSubType(itemType, NodeKindTest.makeNodeKindTest(Type.ATTRIBUTE))) {
//                throw new XPathException("The content of an element with a simple type must not include any attribute nodes");
//            }
        } else if (kind == Type.ATTRIBUTE) {
            // for attributes, do a check only for text nodes and atomic values: anything else gets atomized
            if (expression instanceof ValueOf || expression instanceof Literal) {
                expression.checkPermittedContents(simpleType, env, true);
            }
        }
    }

    /**
     * Internal factory method to create a BuiltInAtomicType. There is one instance for each of the
     * built-in atomic types
     *
     * @param fingerprint The name of the type
     * @param baseType    The base type from which this type is derived
     * @return the newly constructed built in atomic type
     */
    private static BuiltInAtomicType makeAtomicType(int fingerprint, SimpleType baseType, boolean ordered) {
        BuiltInAtomicType t = new BuiltInAtomicType(fingerprint);
        t.setBaseTypeFingerprint(baseType.getFingerprint());
        if (t.isPrimitiveType()) {
            t.primitiveFingerprint = fingerprint;
        } else {
            t.primitiveFingerprint = ((AtomicType)baseType).getPrimitiveType();
        }
        t.ordered = ordered;
        BuiltInType.register(fingerprint, t);
        return t;
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