package org.orbeon.saxon.type;

import org.orbeon.saxon.functions.NormalizeSpace;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.InscopeNamespaceResolver;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.util.Collections;
import java.util.Iterator;

/**
 * This class represents a simple type, as defined in the XML Schema specification. This may be an atomic
 * type, a list type, or a union type. In the non-schema-aware version of Saxon, the simple type will always
 * be a built-in type. With the schema-aware product, it may also be a user-defined simple type.
 */

public abstract class SimpleType extends SchemaType {

    /**
     * The constraining facets of this type
     */

    private FacetCollection facets;

    /**
     * Returns true if this SchemaType is a SimpleType
     * @return true (always)
    */

    public final boolean isSimpleType() {
        return true;
    }

    /**
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     * @return true if this is a list type
     */

    public boolean isListType() {
        return false;
    }

    /**
     * Return true if this type is a union type (that is, if its variety is union)
     * @return true for a union type
     */

    public boolean isUnionType() {
        return false;
    }

    /**
     * Returns an Iterator over all the SimpleTypes that are members of
     * a union type. Note that this is not transitive: if the union has another union
     * as a membertype, the iteration will return this union rather than its members.
     * @return For union types, return an Iterator over all member SimpleTypes. For non-union
     * types, return null.
     */

    public Iterator iterateMemberTypes() {
        return null;
    }

    /**
     * Set the FacetCollection for this simple type.
     * For internal use only.
     * @param facets the collection of facets for this type.
     */

    public void setFacetCollection(FacetCollection facets) {
        this.facets = facets;
    }

    /**
     * Get the FacetCollection for this simple type
     * @return a FacetCollection containing all the facets locally declared on this simple type
     * (and providing access to the inherited facets also)
     */

    public FacetCollection getFacetCollection() {
        return facets;
    }

    /**
     * Returns the first facet associated with the given name, defined either on this type
     * or on a type from which it is derived
     * @return the first facet associated with the given name
    **/

    public Facet getFacet(String name) {
        if (facets == null) {
            return null;
        } else {
            return facets.getFacet(name);
        }
    }

    /**
     * Return all the facets having the given name
     * @return an iterator over all the facets with the given name, including those defined
     * on a type from which this is derived by restriction or by union
     */

    public Iterator getFacets(String name) {
        if (facets==null) {
            SchemaType datatype = getBaseType();
            if (datatype instanceof SimpleType && getDerivationMethod() != DERIVATION_LIST) {
                return ((SimpleType)datatype).getFacets(name);
            } else {
                return Collections.EMPTY_LIST.iterator();
            }
        } else {
            return facets.getFacets(name);
        }
    }

    /**
     * Returns an Iterator over all the Facets (including inherited facets)
     *  for this type.
     * @return an Iterator over all the Facets for this type.
    */

    public Iterator getFacets() {
        if (facets==null) {
            if (getDerivationMethod() == DERIVATION_LIST) {
                return Collections.EMPTY_LIST.iterator();
            } else {
                SchemaType datatype = getBaseType();
                if (datatype instanceof SimpleType) {
                    return ((SimpleType)datatype).getFacets();
                } else {
                    return Collections.EMPTY_LIST.iterator();
                }
            }
        } else {
            return facets.getFacets();
        }
    }

    /**
     * Determine the whitespace normalization required for values of this type
     * @return one of PRESERVE, REPLACE, COLLAPSE
     */

    public int getWhitespaceAction() {
        int action = -1;
        Iterator iter = getFacets(Facet.WHITESPACE);
        if (iter != null) {
            while (iter.hasNext()) {
                Facet f = (Facet)iter.next();
                int a = f.getWhitespaceAction();
                if (a > action) {
                    action = a;
                }
            }
        }
        if (action==-1) {
            if (this instanceof AtomicType && ((AtomicType)this).getPrimitiveType() == Type.STRING) {
                if (Type.isSubType((AtomicType)this,
                        (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_TOKEN))) {
                            return Facet.COLLAPSE;
                } else if (Type.isSubType((AtomicType)this,
                        (ItemType)BuiltInSchemaFactory.getSchemaType(StandardNames.XS_NORMALIZED_STRING))) {
                            return Facet.REPLACE;
                } else {
                    return Facet.PRESERVE;
                }
            } else {
                return Facet.COLLAPSE;
            }
        }
        return action;
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public CharSequence applyWhitespaceNormalization(CharSequence value) {
        int action = getWhitespaceAction();
        switch (action) {
            case Facet.PRESERVE:
                return value;
            case Facet.REPLACE:
                StringBuffer sb = new StringBuffer(value.length());
                for (int i=0; i<value.length(); i++) {
                    if ("\n\r\t".indexOf(value.charAt(i)) >= 0) {
                        sb.append(' ');
                    } else {
                        sb.append(value.charAt(i));
                    }
                }
                return sb;
            case Facet.COLLAPSE:
                return NormalizeSpace.normalize(value.toString());
            default:
                throw new IllegalArgumentException("Unknown whitespace facet value");
        }
    }

    /**
     * Returns the built-in base type this type is derived from.
     * @return the first built-in type found when searching up the type hierarchy
     */
    public SimpleType getBuiltInBaseType() {
        SimpleType base = this;
        while ((base != null) && ( base.getFingerprint() > 1023 )) {
            base = (SimpleType)base.getBaseType();
        }
        return base;
    }

    /**
     * Test whether this simple type is namespace-sensitive, that is, whether
     * it is derived from xs:QName or xs:NOTATION
     * @return true if this type is derived from xs:QName or xs:NOTATION
     */

    public boolean isNamespaceSensitive() {
        SimpleType base = this;
        int fp = base.getFingerprint();
        while (fp > 1023) {
            base = (SimpleType)base.getBaseType();
            fp = base.getFingerprint();
        }

        if (fp == StandardNames.XS_QNAME || fp == StandardNames.XS_NOTATION) {
            return true;
        }
        return false;
    }

    /**
     * Check whether type derivation is allowed. This implements the
     * Schema Component Constraint: Type Derivation OK (Simple). This type
     * is D (the derived type); the type supplied as an argument is B (the
     * base type), and the argument (a subset (extension, restriction, list, union))
     * is provided in the second argument. If type derivation is allowed, the
     * method return null; otherwise it returns a string that can be used as an error
     * message
     * @param b the base type
     * @param derivation the disallowed derivations, as a bit-significant integer
     * @return null if type derivation is OK; otherwise a string that can be used as an error message
     */

    public String isTypeDerivationOK(SimpleType b, int derivation) {
        // rule 1
        if (this == b) {
            return null;
        }
        // rule 2.1 (first half)
        if ((derivation & SchemaType.DERIVATION_RESTRICTION) != 0) {
            return "Simple type derivation is not OK: " +
                    "'restriction' is in the set of disallowed derivations";
        }

        // rule 2.1 (second half)
        SchemaType actualBase = getBaseType();
        if (actualBase != null & !actualBase.allowsDerivation(SchemaType.DERIVATION_RESTRICTION)) {
            return "Simple type derivation is not OK: " +
                    "the base type of type " + getDescription() + " does not allow derivation by restriction";
        }

        // rule 2.2.1
        if (actualBase == b || actualBase.getFingerprint() == b.getFingerprint()) {
                                // the second condition arises when handling the schema for schemas
            return null;
        }

        // rule 2.2.2
        String reason;
        if (actualBase != BuiltInSchemaFactory.getSchemaType(Type.ANY_SIMPLE_TYPE)) {
            if (!(actualBase instanceof SimpleType)) {
                return "Base type of type " + getDescription() + " is not a simple type";
            }
            reason = ((SimpleType)actualBase).isTypeDerivationOK(b, derivation);
            if (reason == null) {
                return null;
            }
        }

        // rule 2.2.3
        if (isListType() || isUnionType()) {
            if (b == BuiltInSchemaFactory.getSchemaType(Type.ANY_SIMPLE_TYPE)) {
                return null;
            }
        }

        // rule 2.2.4
        if (b.isUnionType()) {
            Iterator iter = b.iterateMemberTypes();
            while (iter.hasNext()) {
                SimpleType member = (SimpleType)iter.next();
                if (isTypeDerivationOK(member, derivation) == null) {
                    return null;
                }
            }
        }

        return "Type " + getDescription() + " is not validly derived from type " + b.getDescription();

    }

    /**
     * Checks the validity of this SimpleType definition.
     * @param schema a Schema used for error reporting
     * @throws SchemaException if this SimpleType definition is invalid.
     */

    public boolean validate(SchemaMarker schema)
            throws SchemaException {

        // Check that the base type is a simple type

        if (getFingerprint() < 1023) {
            // built-in type
            return true;
        }

        if (getBaseType() == null) {
            setBaseType(BuiltInSchemaFactory.getSchemaType(StandardNames.XS_ANY_SIMPLE_TYPE));
        } else if (getBaseType() instanceof SimpleType) {
            ((SimpleType)getBaseType()).validate(schema);
        } else if (!getBaseType().isSimpleType()) {
            schema.error("The base type of the simple type " +
                                        getDescription() +
                                        " is not a simple type", this);
            return false;
        }

        return true;
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @throws ValidationException if the content is invalid
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    public abstract void validateContent(CharSequence value, NamespaceResolver nsResolver)
            throws ValidationException;

    /**
     * Get the typed value of a node that is annotated with this schema type
     * @param node the node whose typed value is required
     * @return an iterator over the items making up the typed value of this node. The objects
     * returned by this SequenceIterator will all be of type {@link org.orbeon.saxon.value.AtomicValue}
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
     * @param value the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     * in the content of values. Can supply null, in which case any namespace-sensitive content
     * will be rejected.
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     * returned by this SequenceIterator will all be of type {@link org.orbeon.saxon.value.AtomicValue}
     */

    public abstract SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver)
            throws ValidationException;


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