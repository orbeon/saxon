package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;

import java.io.Serializable;

/**
 * SequenceType: a sequence type consists of a primary type, which indicates the type of item,
 * and a cardinality, which indicates the number of occurrences permitted. Where the primary type
 * is element or attribute, there may also be a content type, indicating the required type
 * annotation on the element or attribute content.
 */

public final class SequenceType implements Serializable {

    // TODO: avoid creating multiple objects representing the same SequenceType

    private ItemType primaryType;    // the primary type of the item, e.g. "element", "comment", or "integer"
    private int cardinality;    // the required cardinality

    /**
     * A type that allows any sequence of items
     */

    public static final SequenceType ANY_SEQUENCE =
            new SequenceType(AnyItemType.getInstance(), StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * A type that allows exactly one item, of any kind
     */

    public static final SequenceType SINGLE_ITEM =
            new SequenceType(AnyItemType.getInstance(), StaticProperty.EXACTLY_ONE);

     /**
     * A type that allows zero or one items, of any kind
     */

//    public static final SequenceType OPTIONAL_ITEM =
//            new SequenceType(Type.ITEM, Type.ITEM, StaticProperty.CARDINALITY_ALLOWS_ZERO_OR_ONE);

    /**
    * A type that allows exactly one atomic value
    */

   public static final SequenceType SINGLE_ATOMIC =
            new SequenceType(Type.ANY_ATOMIC_TYPE,
                             StaticProperty.EXACTLY_ONE);

    /**
    * A type that allows zero or one atomic values
    */

   public static final SequenceType OPTIONAL_ATOMIC =
            new SequenceType(Type.ANY_ATOMIC_TYPE,
                             StaticProperty.ALLOWS_ZERO_OR_ONE);
    /**
    * A type that allows zero or more atomic values
    */

   public static final SequenceType ATOMIC_SEQUENCE =
            new SequenceType(Type.ANY_ATOMIC_TYPE,
                             StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * A type that allows a single string
     */

    public static final SequenceType SINGLE_STRING =
            new SequenceType(Type.STRING_TYPE,
                             StaticProperty.EXACTLY_ONE);

    /**
     * A type that allows a single integer
     */

    public static final SequenceType SINGLE_INTEGER =
            new SequenceType(Type.INTEGER_TYPE,
                             StaticProperty.EXACTLY_ONE);

    /**
     * A type that allows a single integer
     */

    public static final SequenceType OPTIONAL_INTEGER =
            new SequenceType(Type.INTEGER_TYPE,
                             StaticProperty.ALLOWS_ZERO_OR_ONE);

    /**
     * A type that allows zero or one nodes
     */

    public static final SequenceType OPTIONAL_NODE =
            new SequenceType(AnyNodeTest.getInstance(),
                             StaticProperty.ALLOWS_ZERO_OR_ONE);

    /**
     * A type that allows a single node
     */

    public static final SequenceType SINGLE_NODE =
            new SequenceType(AnyNodeTest.getInstance(),
                             StaticProperty.EXACTLY_ONE);


    /**
     * A type that allows a sequence of zero or more nodes
     */

    public static final SequenceType NODE_SEQUENCE =
            new SequenceType(AnyNodeTest.getInstance(),
                             StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * A type that allows a sequence of zero or more numeric values
     */

    public static final SequenceType NUMERIC_SEQUENCE =
            new SequenceType(Type.NUMBER_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

    /**
     * Construct an instance of SequenceType
     *
     * @param primaryType The item type
     * @param cardinality The required cardinality
     */
    public SequenceType(ItemType primaryType, int cardinality) {
        this.primaryType = primaryType;
        this.cardinality = cardinality;
    }

    /**
     * Get the "primary" part of this required type. E.g. for type element(*, xs:date) the "primary type" is element()
     *
     * @return The item type code of the primary type
     */
    public ItemType getPrimaryType() {
        return primaryType;
    }

    /**
     * Get the cardinality component of this SequenceType. This is one of the constants Cardinality.EXACTLY_ONE,
     * Cardinality.ONE_OR_MORE, etc
     *
     * @return the required cardinality
     * @see org.orbeon.saxon.value.Cardinality
     */
    public int getCardinality() {
        return cardinality;
    }



    /**
     * Return a string representation of this SequenceType
     * @return the string representation as an instance of the XPath
     *     SequenceType construct
     */
    public String toString() {
        String s = primaryType.toString();
        if (cardinality == StaticProperty.ALLOWS_ONE_OR_MORE) {
            s = s + '+';
        } else if (cardinality == StaticProperty.ALLOWS_ZERO_OR_MORE) {
            s = s + '*';
        } else if (cardinality == StaticProperty.ALLOWS_ZERO_OR_ONE) {
            s = s + '?';
        }
        return s;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
