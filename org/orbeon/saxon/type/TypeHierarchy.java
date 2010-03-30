package org.orbeon.saxon.type;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.DocumentNodeTest;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.sort.IntHashSet;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class exists to provide answers to questions about the type hierarchy. Because
 * such questions are potentially expensive, it caches the answers. There is one instance of
 * this class for a Configuration.
 */

public class TypeHierarchy implements Serializable {

    private Map map;
    private Configuration config;

    /**
     * Constant denoting relationship between two types: A is the same type as B
     */
    public static final int SAME_TYPE = 0;
    /**
     * Constant denoting relationship between two types: A subsumes B
     */
    public static final int SUBSUMES = 1;
    /**
     * Constant denoting relationship between two types: A is subsumed by B
     */
    public static final int SUBSUMED_BY = 2;
    /**
     * Constant denoting relationship between two types: A overlaps B
     */
    public static final int OVERLAPS = 3;
    /**
     * Constant denoting relationship between two types: A is disjoint from B
     */
    public static final int DISJOINT = 4;

    //private String[] relnames = {"SAME", "SUBSUMES", "SUBSUMED_BY", "OVERLAPS", "DISJOINT"};

    /**
     * Create the type hierarchy cache for a configuration
     * @param config the configuration
     */

    public TypeHierarchy(Configuration config){
        this.config = config;
        try {
            // J2SE 5.0 path
            Class concurrentHashMapClass = config.getClass("java.util.concurrent.ConcurrentHashMap", false, null);
            map = (Map)concurrentHashMapClass.newInstance();
        } catch (Exception e) {
            // JDK 1.4 path
            map =  Collections.synchronizedMap(new HashMap(100));
        }
    }

    /**
     * Get the Saxon configuration to which this type hierarchy belongs
     * @return the configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Determine whether type A is type B or one of its subtypes, recursively
     *
     * @param subtype identifies the first type
     * @param supertype identifies the second type
     * @return true if the first type is the second type or a (direct or
     *     indirect) subtype of the second type
     */

    public boolean isSubType(ItemType subtype, ItemType supertype) {
        int relation = relationship(subtype, supertype);
        return (relation==SAME_TYPE || relation==SUBSUMED_BY);
    }

    /**
     * Determine the relationship of one item type to another.
     * @param t1 the first item type
     * @param t2 the second item type
     * @return {@link #SAME_TYPE} if the types are the same; {@link #SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link #SUBSUMED_BY} if the second type subsumes the first;
     * {@link #OVERLAPS} if the two types overlap (have a non-empty intersection, but neither
     * subsumes the other); {@link #DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    public int relationship(ItemType t1, ItemType t2) {
        if (t1 == null) {
            throw new NullPointerException();
        }
        if (t1.equals(t2)) {
            return SAME_TYPE;
        }
        ItemTypePair pair = new ItemTypePair(t1, t2);
        Integer result = (Integer)map.get(pair);
        if (result == null) {
            final int r = computeRelationship(t1, t2);
            result = new Integer(r);
            map.put(pair, result);
        }
        return result.intValue();
    }

    /**
     * Determine the relationship of one item type to another.
     * @param t1 the first item type
     * @param t2 the second item type
     * @return {@link #SAME_TYPE} if the types are the same; {@link #SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link #SUBSUMED_BY} if the second type subsumes the first;
     * {@link #OVERLAPS} if the two types overlap (have a non-empty intersection, but neither
     * subsumes the other); {@link #DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    private int computeRelationship(ItemType t1, ItemType t2) {
        //System.err.println("computeRelationship " + t1 + ", " + t2);
        if (t1 == t2) {
            return SAME_TYPE;
        }
        if (t1 instanceof AnyItemType) {
            if (t2 instanceof AnyItemType) {
                return SAME_TYPE;
            } else {
                return SUBSUMES;
            }
        } else if (t2 instanceof AnyItemType) {
            return SUBSUMED_BY;
        } else if (t1.isAtomicType()) {
            if (t2 instanceof NodeTest) {
                return DISJOINT;
            } else if (t1 instanceof ExternalObjectType) {
                if (t2 instanceof ExternalObjectType) {
                    return ((ExternalObjectType)t1).getRelationship((ExternalObjectType)t2);
                } else if (((AtomicType)t2).getFingerprint() == StandardNames.XS_ANY_ATOMIC_TYPE) {
                    return SUBSUMED_BY;
                } else {
                    return DISJOINT;
                }
            } else if (t2 instanceof ExternalObjectType) {
                if (((AtomicType)t1).getFingerprint() == StandardNames.XS_ANY_ATOMIC_TYPE) {
                    return SUBSUMES;
                } else {
                    return DISJOINT;
                }
            } else {
                if (((AtomicType)t1).getFingerprint() == ((AtomicType)t2).getFingerprint()) {
                    return SAME_TYPE;
                }
                ItemType t = t2;
                while (t.isAtomicType()) {
                    if (((AtomicType)t1).getFingerprint() == ((AtomicType)t).getFingerprint()) {
                        return SUBSUMES;
                    }
                    t = t.getSuperType(this);
                }
                t = t1;
                while (t.isAtomicType()) {
                    if (((AtomicType)t).getFingerprint() == ((AtomicType)t2).getFingerprint()) {
                        return SUBSUMED_BY;
                    }
                    t = t.getSuperType(this);
                }
                return DISJOINT;
            }
        } else {
            // t1 is a NodeTest
            if (t2.isAtomicType()) {
                return DISJOINT;
            } else {
                // both types are NodeTests
                if (t1 instanceof AnyNodeTest) {
                    if (t2 instanceof AnyNodeTest) {
                        return SAME_TYPE;
                    } else {
                        return SUBSUMES;
                    }
                } else if (t2 instanceof AnyNodeTest) {
                    return SUBSUMED_BY;
                } else if (t1 instanceof EmptySequenceTest) {
                    return DISJOINT;
                } else if (t2 instanceof EmptySequenceTest) {
                    return DISJOINT;
                } else {
                    // first find the relationship between the node kinds allowed
                    int nodeKindRelationship;
                    int m1 = ((NodeTest)t1).getNodeKindMask();
                    int m2 = ((NodeTest)t2).getNodeKindMask();
                    if ((m1 & m2) == 0) {
                        return DISJOINT;
                    } else if (m1 == m2) {
                        nodeKindRelationship = SAME_TYPE;
                    } else if ((m1 & m2) == m1) {
                        nodeKindRelationship = SUBSUMED_BY;
                    } else if ((m1 & m2) == m2) {
                        nodeKindRelationship = SUBSUMES;
                    } else {
                        nodeKindRelationship = OVERLAPS;
                    }

                    // now find the relationship between the node names allowed. Note that although
                    // NamespaceTest and LocalNameTest are NodeTests, they do not occur in SequenceTypes,
                    // so we don't need to consider them.
                    int nodeNameRelationship;
                    IntHashSet n1 = ((NodeTest)t1).getRequiredNodeNames(); // null means all names allowed
                    IntHashSet n2 = ((NodeTest)t2).getRequiredNodeNames(); // null means all names allowed
                    if (n1 == null) {
                        if (n2 == null) {
                            nodeNameRelationship = SAME_TYPE;
                        } else {
                            nodeNameRelationship = SUBSUMES;
                        }
                    } else if (n2 == null) {
                        nodeNameRelationship = SUBSUMED_BY;
                    } else if (n1.containsAll(n2)) {
                        if (n1.size() == n2.size()) {
                            nodeNameRelationship = SAME_TYPE;
                        } else {
                            nodeNameRelationship = SUBSUMES;
                        }
                    } else if (n2.containsAll(n1)) {
                        nodeNameRelationship = SUBSUMED_BY;
                    } else if (n1.containsSome(n2)) {
                        nodeNameRelationship = OVERLAPS;
                    } else {
                        nodeNameRelationship = DISJOINT;
                    }

                    // now find the relationship between the content types allowed

                    int contentRelationship;

                    if (t1 instanceof DocumentNodeTest) {
                        if (t2 instanceof DocumentNodeTest) {
                            contentRelationship = relationship(((DocumentNodeTest)t1).getElementTest(),
                                ((DocumentNodeTest)t2).getElementTest());
                        } else {
                            contentRelationship = SUBSUMED_BY;
                        }
                    } else if (t2 instanceof DocumentNodeTest) {
                        contentRelationship = SUBSUMES;
                    } else {
                        SchemaType s1 = ((NodeTest)t1).getContentType();
                        SchemaType s2 = ((NodeTest)t2).getContentType();
                        contentRelationship = schemaTypeRelationship(s1, s2);
                    }

                    // now analyse the three different relationsships

                    if (nodeKindRelationship == SAME_TYPE &&
                            nodeNameRelationship == SAME_TYPE &&
                            contentRelationship == SAME_TYPE) {
                        return SAME_TYPE;
                    } else if ((nodeKindRelationship == SAME_TYPE || nodeKindRelationship == SUBSUMES) &&
                            (nodeNameRelationship == SAME_TYPE || nodeNameRelationship == SUBSUMES) &&
                            (contentRelationship == SAME_TYPE || contentRelationship == SUBSUMES)) {
                        return SUBSUMES;
                    } else if ((nodeKindRelationship == SAME_TYPE || nodeKindRelationship == SUBSUMED_BY) &&
                            (nodeNameRelationship == SAME_TYPE || nodeNameRelationship == SUBSUMED_BY) &&
                            (contentRelationship == SAME_TYPE || contentRelationship == SUBSUMED_BY)) {
                        return SUBSUMED_BY;
                    } else if (nodeKindRelationship == DISJOINT ||
                            nodeNameRelationship == DISJOINT ||
                            contentRelationship == DISJOINT) {
                        return DISJOINT;
                    } else {
                        return OVERLAPS;
                    }
                }
            }
        }

    }

    /**
     * Test whether a type annotation code represents the type xs:ID or one of its subtypes
     * @param typeCode the type annotation to be tested
     * @return true if the type annotation represents an xs:ID
     */

     public boolean isIdCode(int typeCode) {
         typeCode &= NamePool.FP_MASK;
         if (typeCode == StandardNames.XS_ID) {
             return true;
         } else if (typeCode < 1024) {
             // No other built-in type is an ID
             return false;
         } else {
             SchemaType type = config.getSchemaType(typeCode);
             if (type == null) {
                 return false;      // this shouldn't happen, but there's no need to crash right here
             }
             if (type.isAtomicType()) {
                 return isSubType((AtomicType)type, BuiltInAtomicType.ID);
             }
             if (type instanceof ComplexType && ((ComplexType)type).isSimpleContent()) {
                 SimpleType contentType = ((ComplexType)type).getSimpleContentType();
                 if (contentType.isAtomicType()) {
                     return isSubType((AtomicType)contentType, BuiltInAtomicType.ID);
                 }
             }
             return false;
         }
     }

    /**
     * Test whether a type annotation code represents the type xs:IDREF, xs:IDREFS or one of their subtypes
     * @param typeCode the type annotation to be tested
     * @return true if the type annotation represents an xs:IDREF or xs:IDREFS or a subtype thereof
     */

     public boolean isIdrefsCode(int typeCode) {
         typeCode &= NamePool.FP_MASK;
         if (typeCode == StandardNames.XS_IDREF || typeCode == StandardNames.XS_IDREFS) {
             return true;
         } else if (typeCode < 1024) {
             // No other built-in type is an IDREF or IDREFS
             return false;
         } else {
             SchemaType type = config.getSchemaType(typeCode);
             if (type == null) {
                 // shouldn't happen, but we don't need to crash right now
                 return false;
             }
             if (type.isAtomicType()) {
                 return isSubType((AtomicType)type, BuiltInAtomicType.IDREF);
             }
             if (type instanceof ListType) {
                 return ((ListType)type).getBuiltInBaseType().getFingerprint() == StandardNames.XS_IDREFS;
             }
             if (type.isComplexType() && ((ComplexType)type).isSimpleContent()) {
                 SimpleType contentType = ((ComplexType)type).getSimpleContentType();
                 if (contentType.isAtomicType()) {
                     return isSubType((AtomicType)contentType, BuiltInAtomicType.IDREF);
                 } else if (contentType instanceof ListType) {
                     return contentType.getBuiltInBaseType().getFingerprint() == StandardNames.XS_IDREFS;
                 }
             }
             return false;
         }
     }

    /**
     * Get the relationship of two schema types to each other
     * @param s1 the first type
     * @param s2 the second type
     * @return the relationship of the two types, as one of the constants
     * {@link org.orbeon.saxon.type.TypeHierarchy#SAME_TYPE}, {@link org.orbeon.saxon.type.TypeHierarchy#SUBSUMES},
     * {@link org.orbeon.saxon.type.TypeHierarchy#SUBSUMED_BY}, {@link org.orbeon.saxon.type.TypeHierarchy#DISJOINT}
     */

    public static int schemaTypeRelationship(SchemaType s1, SchemaType s2) {
        if (s1.isSameType(s2)) {
            return SAME_TYPE;
        }
        if (s1 instanceof AnyType) {
            return SUBSUMES;
        }
        if (s2 instanceof AnyType) {
            return SUBSUMED_BY;
        }
        SchemaType t1 = s1;
        while (true) {
            t1 = t1.getBaseType();
            if (t1 == null) {
                break;
            }
            if (t1.isSameType(s2)) {
                return SUBSUMED_BY;
            }
        }
        SchemaType t2 = s2;
        while (true) {
            t2 = t2.getBaseType();
            if (t2 == null) {
                break;
            }
            if (t2.isSameType(s1)) {
                return SUBSUMES;
            }
        }
        return DISJOINT;
    }


    private class ItemTypePair implements Serializable {
        ItemType s;
        ItemType t;

        public ItemTypePair(ItemType s, ItemType t) {
            this.s = s;
            this.t = t;
        }

        /**
         * Returns a hash code value for the object.
         * @return a hash code value for this object.
         * @see Object#equals(Object)
         * @see java.util.Hashtable
         */
        public int hashCode() {
            return s.hashCode() ^ t.hashCode();
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         */

        public boolean equals(Object obj) {
            final ItemTypePair pair = (ItemTypePair)obj;
            return s.equals(pair.s) && t.equals(pair.t);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
