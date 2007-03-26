package org.orbeon.saxon.type;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Value;

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

public interface SchemaType extends SchemaComponent {

    // DerivationMethods. These constants are copied from org.w3.dom.TypeInfo. They are redefined here to avoid
    // creating a dependency on the TypeInfo class, which is only available when JAXP 1.3 is available.

    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the derivation by <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#key-typeRestriction'>
     * restriction</a> if complex types are involved, or a <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-restriction'>
     * restriction</a> if simple types are involved.
     * <br>  The reference type definition is derived by restriction from the
     * other type definition if the other type definition is the same as the
     * reference type definition, or if the other type definition can be
     * reached recursively following the {base type definition} property
     * from the reference type definition, and all the <em>derivation methods</em> involved are restriction.
     */
    public static final int DERIVATION_RESTRICTION    = 0x00000001;
    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the derivation by <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#key-typeExtension'>
     * extension</a>.
     * <br>  The reference type definition is derived by extension from the
     * other type definition if the other type definition can be reached
     * recursively following the {base type definition} property from the
     * reference type definition, and at least one of the <em>derivation methods</em> involved is an extension.
     */
    public static final int DERIVATION_EXTENSION      = 0x00000002;
    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-union'>
     * union</a> if simple types are involved.
     * <br> The reference type definition is derived by union from the other
     * type definition if there exists two type definitions T1 and T2 such
     * as the reference type definition is derived from T1 by
     * <code>DERIVATION_RESTRICTION</code> or
     * <code>DERIVATION_EXTENSION</code>, T2 is derived from the other type
     * definition by <code>DERIVATION_RESTRICTION</code>, T1 has {variety} <em>union</em>, and one of the {member type definitions} is T2. Note that T1 could be
     * the same as the reference type definition, and T2 could be the same
     * as the other type definition.
     */
    public static final int DERIVATION_UNION          = 0x00000004;
    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-list'>list</a>.
     * <br> The reference type definition is derived by list from the other
     * type definition if there exists two type definitions T1 and T2 such
     * as the reference type definition is derived from T1 by
     * <code>DERIVATION_RESTRICTION</code> or
     * <code>DERIVATION_EXTENSION</code>, T2 is derived from the other type
     * definition by <code>DERIVATION_RESTRICTION</code>, T1 has {variety} <em>list</em>, and T2 is the {item type definition}. Note that T1 could be the same as
     * the reference type definition, and T2 could be the same as the other
     * type definition.
     */
    public static final int DERIVATION_LIST           = 0x00000008;

    /**
     * Derivation by substitution.
     * This constant, unlike the others, is NOT defined in the DOM level 3 TypeInfo interface.
     */

    public static final int DERIVE_BY_SUBSTITUTION = 16;

    /**
     * Get the namecode of the name of this type. This includes the prefix from the original
     * type declaration: in the case of built-in types, there may be a conventional prefix
     * or there may be no prefix.
     */

    int getNameCode();

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
     * Test whether this SchemaType is an atomic type
     * @return true if this SchemaType is an atomic type
     */

    boolean isAtomicType();

    /**
     * Test whether this is an anonymous type
     * @return true if this SchemaType is an anonymous type
     */

    boolean isAnonymousType();

    /**
	 * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link SchemaType#DERIVATION_LIST} and {@link SchemaType#DERIVATION_EXTENSION}
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
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     * @return a numeric code representing the derivation method, for example {@link SchemaType#DERIVATION_RESTRICTION}
     */

    int getDerivationMethod();

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     * @param derivation the kind of derivation, for example {@link SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    boolean allowsDerivation(int derivation);

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     * @param expression the expression that delivers the content
     * @param kind the node kind whose content is being delivered: {@link Type#ELEMENT},
     * {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     * @param env The static evaluation context for the query or stylesheet
     * @throws XPathException if the expression will never deliver a value of the correct type
     */

    void analyzeContentExpression(Expression expression, int kind, StaticContext env) throws XPathException;

    /**
     * Get the typed value of a node that is annotated with this schema type. The results of this method
     * are consistent with the {@link #atomize} method, but this version returns a SequenceIterator which may
     * be more efficient when handling long lists.
     * @param node the node whose typed value is required
     * @return a SequenceIterator over the atomic values making up the typed value of the specified
     * node. The objects returned by this iterator are of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    SequenceIterator getTypedValue(NodeInfo node) throws XPathException;

    /**
     * Get the typed value of a node that is annotated with this schema type. The result of this method will always be consistent with the method
     * {@link #getTypedValue}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     * @param node the node whose typed value is required
     * @return the typed value. 
     * @since 8.5
     */

    Value atomize(NodeInfo node) throws XPathException;

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
     * Check that this type is validly derived from a given type, following the rules for the Schema Component
     * Constraint "Is Type Derivation OK (Simple)" (3.14.6) or "Is Type Derivation OK (Complex)" (3.4.6) as
     * appropriate.
     * @param base the base type; the algorithm tests whether derivation from this type is permitted
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType base, int block) throws SchemaException, ValidationException;
    // TODO: method no longer used, can delete its implementations once everything is tested

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
