package net.sf.saxon.type;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.value.UntypedAtomicValue;

/**
 * This class has a singleton instance which represents the XML Schema built-in type xs:anyType,
 * also known as the urtype.
 */

public final class AnyType extends SchemaType implements ComplexType {

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
     * Test whether this ComplexType has been marked as abstract.
     * @return false: this class is not abstract.
     */

    public boolean isAbstract() {
        return false;
    }

    /**
     * Test whether this SchemaType is a simple type
     * @return true if this SchemaType is a simple type
     */

    public boolean isSimpleType() {
        return false;
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
     * Get the local name of this type.
     * @return the string "anyType".
     */
    public String getLocalName() {
        return "anyType";
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
        if (other instanceof AnyType) {
            return Type.SAME_TYPE;
        } else {
            return Type.SUBSUMES;
        }
    }

    /**
     * Get a description of this type for use in diagnostics
     * @return the string "xs:anyType"
     */

    public String getDescription() {
        return "xs:anyType";
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