package org.orbeon.saxon.type;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;

import java.io.Serializable;


/**
 * ItemType is an interface that allows testing of whether an Item conforms to an
 * expected type. ItemType represents the types in the type hierarchy in the XPath model,
 * as distinct from the schema model: an item type is either item() (matches everything),
 * a node type (matches nodes), an atomic type (matches atomic values), or empty()
 * (matches nothing). Atomic types, represented by the class AtomicType, are also
 * instances of SimpleType in the schema type heirarchy. Node Types, represented by
 * the class NodeTest, are also Patterns as used in XSLT.
 * @see org.orbeon.saxon.type.AtomicType
 * @see org.orbeon.saxon.pattern.NodeTest
*/

public interface ItemType extends Serializable {

    /**
     * Determine whether this item type is atomic (that is, whether it can ONLY match
     * atomic values)
     * @return true if this is ANY_ATOMIC_TYPE or a subtype thereof
     */

    public boolean isAtomicType();

    /**
     * Test whether a given item conforms to this type
     * @param item The item to be tested
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config);

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     * <p>
     * In fact the concept of "supertype" is not really well-defined, because the types
     * form a lattice rather than a hierarchy. The only real requirement on this function
     * is that it returns a type that strictly subsumes this type, ideally as narrowly
     * as possible.
     * @return the supertype, or null if this type is item()
     * @param th the type hierarchy cache
     */

    public ItemType getSuperType(TypeHierarchy th);

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that integer, xs:dayTimeDuration, and xs:yearMonthDuration
     * are considered to be primitive types.
     */

    public ItemType getPrimitiveItemType();

    /**
     * Get the primitive type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public int getPrimitiveType();

    /**
     * Produce a representation of this type name for use in error messages.
     * Where this is a QName, it will use conventional prefixes
     */

    public String toString(NamePool pool);

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public AtomicType getAtomizedItemType();

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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
