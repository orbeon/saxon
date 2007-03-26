package org.orbeon.saxon.pattern;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.tinytree.TinyTree;
import org.orbeon.saxon.type.*;

/**
  * A CombinedNodeTest combines two nodetests using one of the operators
  * union (=or), intersect (=and), difference (= "and not"). This arises
  * when optimizing a union (etc) of two path expressions using the same axis.
  * A CombinedNodeTest is also used to support constructs such as element(N,T),
  * which can be expressed as (element(N,*) AND element(*,T))
  *
  * @author Michael H. Kay
  */

public class CombinedNodeTest extends NodeTest {

    private NodeTest nodetest1;
    private NodeTest nodetest2;
    private int operator;
    private boolean isGlobalComponentTest;

    public CombinedNodeTest(NodeTest nt1, int operator, NodeTest nt2) {
        nodetest1 = nt1;
        this.operator = operator;
        nodetest2 = nt2;
    }

    /**
     * Indicate whether this CombinedNodeTest actually represents a SequenceType of the form
     * schema-element(X) or schema-attribute(X). This information is used only when reconstructing a
     * string representation of the NodeTest for diagnostics.
     */

    public void setGlobalComponentTest(boolean global) {
        isGlobalComponentTest = global;
    }

    /**
    * Test whether this node test is satisfied by a given node.
    * @param nodeType The type of node to be matched
     @param fingerprint identifies the expanded name of the node to be matched.

     */

    public boolean matches(int nodeType, int fingerprint, int annotation) {
        switch (operator) {
            case Token.UNION:
                return nodetest1==null ||
                       nodetest2==null ||
                       nodetest1.matches(nodeType, fingerprint, annotation) ||
                       nodetest2.matches(nodeType, fingerprint, annotation);
            case Token.INTERSECT:
                return (nodetest1==null || nodetest1.matches(nodeType, fingerprint, annotation)) &&
                       (nodetest2==null || nodetest2.matches(nodeType, fingerprint, annotation));
            case Token.EXCEPT:
                return (nodetest1==null || nodetest1.matches(nodeType, fingerprint, annotation)) &&
                       !(nodetest2==null || nodetest2.matches(nodeType, fingerprint, annotation));
            default:
                throw new IllegalArgumentException("Unknown operator in Combined Node Test");
        }
    }

    /**
     * Test whether this node test is satisfied by a given node on a TinyTree. The node
     * must be a document, element, text, comment, or processing instruction node.
     * This method is provided
     * so that when navigating a TinyTree a node can be rejected without
     * actually instantiating a NodeInfo object.
     *
     * @param tree   the TinyTree containing the node
     * @param nodeNr the number of the node within the TinyTree
     * @return true if the node matches the NodeTest, otherwise false
     */

    public boolean matches(TinyTree tree, int nodeNr) {
        switch (operator) {
            case Token.UNION:
                return nodetest1==null ||
                       nodetest2==null ||
                       nodetest1.matches(tree, nodeNr) ||
                       nodetest2.matches(tree, nodeNr);
            case Token.INTERSECT:
                return (nodetest1==null || nodetest1.matches(tree, nodeNr)) &&
                       (nodetest2==null || nodetest2.matches(tree, nodeNr));
            case Token.EXCEPT:
                return (nodetest1==null || nodetest1.matches(tree, nodeNr)) &&
                       !(nodetest2==null || nodetest2.matches(tree, nodeNr));
            default:
                throw new IllegalArgumentException("Unknown operator in Combined Node Test");
        }
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        switch (operator) {
            case Token.UNION:
                return nodetest1==null ||
                       nodetest2==null ||
                       nodetest1.matches(node) ||
                       nodetest2.matches(node);
            case Token.INTERSECT:
                return (nodetest1==null || nodetest1.matches(node)) &&
                       (nodetest2==null || nodetest2.matches(node));
            case Token.EXCEPT:
                return (nodetest1==null || nodetest1.matches(node)) &&
                       !(nodetest2==null || nodetest2.matches(node));
            default:
                throw new IllegalArgumentException("Unknown operator in Combined Node Test");
        }
    }

    public String toString(NamePool pool) {
        if (isGlobalComponentTest) {
            if (nodetest1 instanceof SubstitutionGroupTest) {
                return nodetest1.toString(pool);
            } else {
                final int kind = nodetest1.getPrimitiveType();
                final String skind = (kind == Type.ELEMENT ? "schema-element(" : "schema-attribute(");
                final String name = pool.getClarkName(nodetest1.getFingerprint());
                return skind + name + ')';
            }
        } else if (nodetest1 instanceof NameTest && operator==Token.INTERSECT) {
            int kind = nodetest1.getPrimitiveType();
            String skind = (kind == Type.ELEMENT ? "element(" : "attribute(");
            String content = "";
            if (nodetest2 instanceof ContentTypeTest) {
                final SchemaType schemaType = ((ContentTypeTest)nodetest2).getSchemaType();
                content = ", " + pool.getClarkName(schemaType.getFingerprint());
            }
            String name = pool.getClarkName(nodetest1.getFingerprint());
            return skind + name + content + ')';
        } else {
            String nt1 = (nodetest1==null ? "true()" : nodetest1.toString(pool));
            String nt2 = (nodetest2==null ? "true()" : nodetest2.toString(pool));
            return '(' + nt1 + ' ' + Token.tokens[operator] + ' ' + nt2 + ')';
        }
    }

    /**
     * Get the supertype of this type. This isn't actually a well-defined concept: the types
     * form a lattice rather than a strict hierarchy.
     * @param th
     */

    public ItemType getSuperType(TypeHierarchy th) {
        switch (operator) {
            case Token.UNION:
                return Type.getCommonSuperType(nodetest1, nodetest2, th);
            case Token.INTERSECT:
                return nodetest1;
            case Token.EXCEPT:
                return nodetest1;
            default:
                throw new IllegalArgumentException("Unknown operator in Combined Node Test");
        }
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        switch (operator) {
            case Token.UNION:
                return nodetest1.getNodeKindMask() | nodetest2.getNodeKindMask();
            case Token.INTERSECT:
                return nodetest1.getNodeKindMask() & nodetest2.getNodeKindMask();
            case Token.EXCEPT:
                return nodetest1.getNodeKindMask();
            default:
                return 0;
        }

    }

    /**
     * Get the basic kind of object that this ItemType matches: for a NodeTest, this is the kind of node,
     * or Type.Node if it matches different kinds of nodes.
     *
     * @return the node kind matched by this node test
     */

    public int getPrimitiveType() {
        int mask = getNodeKindMask();
        if (mask == (1<<Type.ELEMENT)) {
            return Type.ELEMENT;
        }
        if (mask == (1<<Type.ATTRIBUTE)) {
            return Type.ATTRIBUTE;
        }
        if (mask == (1<<Type.DOCUMENT)) {
            return Type.DOCUMENT;
        }
        return Type.NODE;
    }

    /**
     * Get the set of node names allowed by this NodeTest. This is returned as a set of Integer fingerprints.
     * A null value indicates that all names are permitted (i.e. that there are no constraints on the node name.
     * The default implementation returns null.
     */

    public IntHashSet getRequiredNodeNames() {
        IntHashSet s1 = nodetest1.getRequiredNodeNames();
        IntHashSet s2 = nodetest2.getRequiredNodeNames();
        if (s2 == null) {
            return s1;
        }
        if (s1 == null) {
            return s2;
        }
        switch (operator) {
            case Token.UNION: {
                return s1.union(s2);
            }
            case Token.INTERSECT: {
                return s1.intersect(s2);
            }
            case Token.EXCEPT: {
                return s1.except(s2);
            }
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Get the content type allowed by this NodeTest (that is, the type annotation of the matched nodes).
     * Return AnyType if there are no restrictions. The default implementation returns AnyType.
     */

    public SchemaType getContentType() {
        SchemaType type1 = nodetest1.getContentType();
        SchemaType type2 = nodetest2.getContentType();
        if (type1.isSameType(type2)) return type1;
        if (operator == Token.INTERSECT) {
            if (type2 instanceof AnyType) {
                return type1;
            }
            if (type1 instanceof AnyType) {
                return type2;
            }
        }
        return AnyType.getInstance();
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized (assuming that atomization succeeds)
     */

    public AtomicType getAtomizedItemType() {
        AtomicType type1 = nodetest1.getAtomizedItemType();
        AtomicType type2 = nodetest2.getAtomizedItemType();
        if (type1.isSameType(type2)) return type1;
        if (operator == Token.INTERSECT) {
            if (type2 == Type.ANY_ATOMIC_TYPE) {
                return type1;
            }
            if (type1 == Type.ANY_ATOMIC_TYPE) {
                return type2;
            }
        }
        return Type.ANY_ATOMIC_TYPE;
    }

    /**
     * Get the name of the nodes matched by this nodetest, if it matches a specific name.
     * Return -1 if the node test matches nodes of more than one name
     */

    public int getFingerprint() {
        int fp1 = nodetest1.getFingerprint();
        int fp2 = nodetest2.getFingerprint();
        if (fp1 == fp2) return fp1;
        if (fp2 == -1 && operator==Token.INTERSECT) return fp1;
        if (fp1 == -1 && operator==Token.INTERSECT) return fp2;
        return -1;
    }

    /**
     * Determine whether the content type (if present) is nillable
     * @return true if the content test (when present) can match nodes that are nilled
     */

    public boolean isNillable() {
        // this should err on the safe side
        return nodetest1.isNillable() || nodetest2.isNillable();
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return nodetest1.hashCode() ^ nodetest2.hashCode();
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof CombinedNodeTest &&
                ((CombinedNodeTest)other).nodetest1.equals(nodetest1) &&
                ((CombinedNodeTest)other).nodetest2.equals(nodetest2) &&
                ((CombinedNodeTest)other).operator == operator;
    }

    /**
     * get the default priority of this nodeTest when used as a pattern
     */

    public double getDefaultPriority() {
        return 0.25;
    }

    /**
     * Get the two parts of the combined node test
     */

    public NodeTest[] getComponentNodeTests() {
        NodeTest[] tests = {nodetest1, nodetest2};
        return tests;
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
