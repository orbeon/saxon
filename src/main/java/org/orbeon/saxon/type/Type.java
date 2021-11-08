package org.orbeon.saxon.type;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.ObjectValue;

import java.io.Serializable;


/**
 * This class contains static information about types and methods for constructing type codes.
 * The class is never instantiated.
 *
 * <p><i>The constant integers used for type names in earlier versions of this class have been replaced
 * by constants in {@link StandardNames}. The constants representing {@link AtomicType} objects are now
 * available through the {@link BuiltInAtomicType} class.</i></p>
 *
 */

public abstract class Type implements Serializable {

    // Note that the integer codes representing node kinds are the same as
    // the codes allocated in the DOM interface, while the codes for built-in
    // atomic types are fingerprints allocated in StandardNames. These two sets of
    // codes must not overlap!

    /**
     * Type representing an element node - element()
     */

    public static final short ELEMENT = 1;
    /**
     * Item type representing an attribute node - attribute()
     */
    public static final short ATTRIBUTE = 2;
    /**
     * Item type representing a text node - text()
     */
    public static final short TEXT = 3;
    /**
     * Item type representing a text node stored in the tiny tree as compressed whitespace
     */
    public static final short WHITESPACE_TEXT = 4;
    /**
     * Item type representing a processing-instruction node
     */
    public static final short PROCESSING_INSTRUCTION = 7;
    /**
     * Item type representing a comment node
     */
    public static final short COMMENT = 8;
    /**
     * Item type representing a document node
     */
    public static final short DOCUMENT = 9;
    /**
     * Item type representing a namespace node
     */
    public static final short NAMESPACE = 13;
    /**
     * Dummy node kind used in the tiny tree to mark the end of the tree
     */
    public static final short STOPPER = 11;
    /**
     * Dummy node kind used in the tiny tree to contain a parent pointer
     */
    public static final short PARENT_POINTER = 12;

    /**
     * An item type that matches any node
     */

    public static final short NODE = 0;

    public static final ItemType NODE_TYPE = AnyNodeTest.getInstance();

    /**
     * An item type that matches any item
     */

    public static final short ITEM = 88;

    public static final ItemType ITEM_TYPE = AnyItemType.getInstance();

    public static final short MAX_NODE_TYPE = 13;
    /**
     * Item type that matches no items (corresponds to SequenceType empty())
     */
    public static final short EMPTY = 15;    // a test for this type will never be satisfied

    private Type() {
    }

    /**
     * Test whether a given type is (some subtype of) node()
     *
     * @param type The type to be tested
     * @return true if the item type is node() or a subtype of node()
     */

    public static boolean isNodeType(ItemType type) {
        return type instanceof NodeTest;
    }

    /**
     * Get the ItemType of an Item
     * @param item the item whose type is required
     * @param th the type hierarchy cache
     * @return the item type of the item
     */

    public static ItemType getItemType(Item item, TypeHierarchy th) {
        if (item instanceof AtomicValue) {
            return ((AtomicValue)item).getItemType(th);
        } else {
            return NodeKindTest.makeNodeKindTest(((NodeInfo)item).getNodeKind());
            // We could return a more precise type than this, for example one that includes
            // a ContentTypeTest for the type annotation of the nodes. However, given the way in which
            // this method is used, this wouldn't be very useful
        }
    }


    /**
     * Output (for diagnostics) a representation of the type of an item. This
     * does not have to be the most specific type
     * @param item the item whose type is to be displayed
     * @return a string representation of the type of the item
     */

    public static String displayTypeName(Item item) {
        if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo)item;
            switch (node.getNodeKind()) {
                case DOCUMENT:
                    return "document-node()";
                case ELEMENT:
                    NamePool pool = node.getNamePool();
                    int annotation = node.getTypeAnnotation();
                    return "element(" +
                            ((NodeInfo)item).getDisplayName() + ", " +
                            (annotation == -1 ?
                                "xs:untyped)" :
                                pool.getDisplayName(annotation) + ')');
                case ATTRIBUTE:
                    NamePool pool2 = node.getNamePool();
                    int annotation2 = node.getTypeAnnotation() & NamePool.FP_MASK;
                    return "attribute(" +
                            ((NodeInfo)item).getDisplayName()+ ", " +
                            (annotation2 == -1 ?
                                "xs:untypedAtomic)" :
                                pool2.getDisplayName(annotation2) + ')');
                case TEXT:      return "text()";
                case COMMENT:   return "comment()";
                case PROCESSING_INSTRUCTION:
                                return "processing-instruction()";
                case NAMESPACE: return "namespace()";
                default:        return "";
            }
        } else if (item instanceof ObjectValue) {
            return ((ObjectValue)item).displayTypeName();
        } else {
            return ((AtomicValue)item).getItemType(null).toString();
        }
    }

    /**
     * Get the ItemType object for a built-in type
     * @param namespace the namespace URI of the type
     * @param localName the local name of the type
     * @return the ItemType, or null if not found
     */

    public static ItemType getBuiltInItemType(String namespace, String localName) {
        SchemaType t = BuiltInType.getSchemaType(
                StandardNames.getFingerprint(namespace, localName));
        if (t instanceof ItemType) {
            return (ItemType)t;
        } else {
            return null;
        }
    }

    /**
     * Get a type that is a common supertype of two given item types
     *
     * @param t1 the first item type
     * @param t2 the second item type
     * @param th the type hierarchy cache
     * @return the item type that is a supertype of both
     *     the supplied item types
     */

    public static ItemType getCommonSuperType(ItemType t1, ItemType t2, TypeHierarchy th) {
        if (t1 instanceof EmptySequenceTest) {
            return t2;
        }
        if (t2 instanceof EmptySequenceTest) {
            return t1;
        }
        int r = th.relationship(t1, t2);
        if (r == TypeHierarchy.SAME_TYPE) {
            return t1;
        } else if (r == TypeHierarchy.SUBSUMED_BY) {
            return t2;
        } else if (r == TypeHierarchy.SUBSUMES) {
            return t1;
        } else {
            return getCommonSuperType(t2.getSuperType(th), t1, th);
            // eventually we will hit a type that is a supertype of t2. We reverse
            // the arguments so we go up each branch of the tree alternately.
            // If we hit the root of the tree, one of the earlier conditions will be satisfied,
            // so the recursion will stop.
        }
    }

    /**
     * Determine whether this type is a primitive type. The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; the 7 node kinds; and all supertypes of these (item(), node(), xs:anyAtomicType,
     * xs:numeric, ...)
     * @param code the item type code to be tested
     * @return true if the type is considered primitive under the above rules
     */
    public static boolean isPrimitiveType(int code) {
        return code >= 0 && (code <= StandardNames.XS_INTEGER ||
                code == StandardNames.XS_NUMERIC ||
                code == StandardNames.XS_UNTYPED_ATOMIC ||
                code == StandardNames.XS_ANY_ATOMIC_TYPE ||
                code == StandardNames.XS_DAY_TIME_DURATION ||
                code == StandardNames.XS_YEAR_MONTH_DURATION ||
                code == StandardNames.XS_ANY_SIMPLE_TYPE);
    }

    /**
     * Determine whether two primitive atomic types are comparable
     * @param t1 the first type to compared.
     * This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param t2 the second type to compared.
     * This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     * if testing for an equality comparison (eq, ne)
     * @return true if the types are comparable, as defined by the rules of the "eq" operator
     */

    public static boolean isComparable(BuiltInAtomicType t1, BuiltInAtomicType t2, boolean ordered) {
        if (t1.equals(BuiltInAtomicType.ANY_ATOMIC) || t2.equals(BuiltInAtomicType.ANY_ATOMIC)) {
            return true; // meaning we don't actually know at this stage
        }
        if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            t1 = BuiltInAtomicType.STRING;
        }
        if (t2.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            t2 = BuiltInAtomicType.STRING;
        }
        if (t1.equals(BuiltInAtomicType.ANY_URI)) {
            t1 = BuiltInAtomicType.STRING;
        }
        if (t2.equals(BuiltInAtomicType.ANY_URI)) {
            t2 = BuiltInAtomicType.STRING;
        }
        if (t1.isPrimitiveNumeric()) {
            t1 = BuiltInAtomicType.NUMERIC;
        }
        if (t2.isPrimitiveNumeric()) {
            t2 = BuiltInAtomicType.NUMERIC;
        }
        if (!ordered) {
            if (t1.equals(BuiltInAtomicType.DAY_TIME_DURATION)) {
                t1 = BuiltInAtomicType.DURATION;
            }
            if (t2.equals(BuiltInAtomicType.DAY_TIME_DURATION)) {
                t2 = BuiltInAtomicType.DURATION;
            }
            if (t1.equals(BuiltInAtomicType.YEAR_MONTH_DURATION)) {
                t1 = BuiltInAtomicType.DURATION;
            }
            if (t2.equals(BuiltInAtomicType.YEAR_MONTH_DURATION)) {
                t2 = BuiltInAtomicType.DURATION;
            }
        }
        return t1 == t2;
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
