package org.orbeon.saxon.type;

import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.MappingFunction;
import org.orbeon.saxon.expr.MappingIterator;
import org.orbeon.saxon.expr.StringTokenIterator;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.NormalizeSpace;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.util.Iterator;

/**
 * Represents a SimpleType that is a list of a given
 * SimpleType, known as its itemType. A ListType may be a restriction
 * of another ListType; the itemType must either be an AtomicType or
 * a UnionType whose members are not list types.
 *
 * <p>In the non-schema-aware product this class is used to implement the built-in
 * list types NMTOKENS, ENTITIES, IDREFS. In the schema-aware product it is also
 * used to support user-defined list types.</p>
 *
**/

public class ListType extends SimpleType implements MappingFunction {

    /**
     *  The SimpleType of the items in the list.
     */

    private SimpleType itemType = null;

    /**
     * Create a new ListType.
     */

    public ListType(NamePool pool) {
        super();
        super.setNamePool(pool);
    }

   /**
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     */

    public boolean isListType() {
        return true;
    }

    /**
     * Returns the simpleType of the items in this ListType.
     * @return the simpleType of the items in this ListType.
    */

    public SimpleType getItemType() {
        return itemType;
    }

    /**
     * This method returns true if there is a derivation between the reference type definition, that is the TypeInfo
     * on which the method is being called, and the other type definition, that is the one passed as parameters.
     * This method implements the DOM Level 3 TypeInfo interface.
     * @param typeNamespaceArg the namespace of the "other" type
     * @param typeNameArg the local name of the "other" type
     * @param derivationMethod the derivation method: zero or more of {@link TypeInfo#DERIVATION_RESTRICTION},
     * {@link TypeInfo#DERIVATION_EXTENSION}, {@link TypeInfo#DERIVATION_LIST}, or {@link TypeInfo#DERIVATION_UNION}.
     * Zero means derived by any possible route.
     */

    public boolean isDerivedFrom(String typeNamespaceArg,
                                 String typeNameArg,
                                 int derivationMethod) {
        if (derivationMethod == 0 || (derivationMethod & TypeInfo.DERIVATION_LIST) != 0) {
            if (itemType.isDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod)) {
                return true;
            }
        }
        return super.isDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
    }

    /**
     * Sets the itemType for this ListType (the type of
     * item that instances of this list type contain).
     * For internal use only.
     * @param type the SimpleType for this ListType.
    */

    public void setItemType(SimpleType type) throws SchemaException {
        itemType = type;
    }

    /**
     * Determine the relationship of this schema type to another schema type.
     * @param other the other schema type
     * @return {@link Type#SAME_TYPE} if the types are the same; {@link Type#SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link Type#SUBSUMED_BY} if the second type subsumes the first;
     * {@link Type#OVERLAPS} if the two types overlap (have a non-empty intersection);
     * {@link Type#DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    public int relationshipTo(SchemaType other) {
        // TODO: this seems wrong. According to the formal semantics, list types are
        // only considered to be derived from other list types if they are explicitly
        // derived - "list of integer" is not considered to be derived from "list of decimal".
        if (other instanceof AnyType) {
            return Type.SUBSUMED_BY;
        } else if (other instanceof ListType) {
            return itemType.relationshipTo(((ListType)other).getItemType());
        } else if (other instanceof SimpleType) {
            int itemRel = itemType.relationshipTo(other);
            switch (itemRel) {
                case Type.SAME_TYPE: return Type.SUBSUMES;
                case Type.SUBSUMES: return Type.SUBSUMES;
                case Type.SUBSUMED_BY: return Type.OVERLAPS;
                case Type.OVERLAPS: return Type.OVERLAPS;
                case Type.DISJOINT: return Type.DISJOINT;
                default: throw new IllegalStateException();
            }
        } else {
            return Type.DISJOINT;
        }
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public String applyWhitespaceNormalization(String value) {
        return NormalizeSpace.normalize(value);
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

    public void validateContent(CharSequence value, NamespaceResolver nsResolver) throws ValidationException {
        SimpleType base = getItemType();
        SequenceIterator iter = new StringTokenIterator(value.toString());
        int count = 0;
        try {
            while (true) {
                StringValue val = (StringValue)iter.next();
                if (val == null) break;
                base.validateContent(val.getStringValue(), nsResolver);
                count++;
            }
            Iterator fi = getFacets();
            while (fi.hasNext()) {
                Facet f = (Facet)fi.next();
                if (!f.testLength(count)) {
                    throw new ValidationException(
                            "Length of list (" + count + ") violates " +
                            f.getName() + " facet " + Err.wrap(f.getValue()));

                }
                if (f.appliesToWholeList()) {
                    boolean match = f.testAtomicValue(new StringValue(NormalizeSpace.normalize(value.toString())));
                    if (!match) {
                        throw new ValidationException(
                            "List " + Err.wrap(value) + " violates the " + f.getName() + " facet " +
                            Err.wrap(f.getValue()));
                    }

                }
            }
        } catch (XPathException err) {
            throw new ValidationException(err);
        }
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
     * For details see {@link MappingFunction}
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


