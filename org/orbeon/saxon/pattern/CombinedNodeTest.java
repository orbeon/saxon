package org.orbeon.saxon.pattern;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.type.AnyType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;

import java.util.Set;
import java.util.HashSet;

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

    public CombinedNodeTest(NodeTest nt1, int operator, NodeTest nt2) {
        nodetest1 = nt1;
        this.operator = operator;
        nodetest2 = nt2;
    }

    /**
    * Test whether this node test is satisfied by a given node.
    * @param nodeType The type of node to be matched
    * @param fingerprint identifies the expanded name of the node to be matched.
    * The value should be -1 for a node with no name.
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
        if (nodetest1 instanceof NameTest) {
            int kind = nodetest1.getPrimitiveType();
            String content = "";
            if (nodetest2 instanceof ContentTypeTest) {
                content = ", " + ((ContentTypeTest)nodetest2).getSchemaType().getDisplayName();
            }
            if (kind == Type.ELEMENT) {
                return "element(*" + content + ')';
            } else if (kind == Type.ATTRIBUTE) {
                return "attribute(*" + content + ')';
            }
        }
        String nt1 = (nodetest1==null ? "true()" : nodetest1.toString(pool));
        String nt2 = (nodetest2==null ? "true()" : nodetest2.toString(pool));
        return '(' + nt1 + ' ' + Token.tokens[operator] + ' ' + nt2 + ')';
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
//        if (operator == Token.INTERSECT) {
//
//        }
        return nodetest1.getNodeKindMask() & nodetest2.getNodeKindMask();
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

    public Set getRequiredNodeNames() {
        Set s1 = nodetest1.getRequiredNodeNames();
        Set s2 = nodetest2.getRequiredNodeNames();
        if (s2 == null) {
            return s1;
        }
        if (s1 == null) {
            return s2;
        }
        switch (operator) {
            case Token.UNION: {
                Set result = new HashSet(s1);
                result.addAll(s2);
                return result;
            }
            case Token.INTERSECT: {
                Set result = new HashSet(s1);
                result.retainAll(s2);
                return result;
            }
            case Token.EXCEPT: {
                Set result = new HashSet(s1);
                result.removeAll(s2);
                return result;
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
                return nodetest1.getContentType();
            }
            if (type1 instanceof AnyType) {
                return nodetest2.getContentType();
            }
        }
        return AnyType.getInstance();
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
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return nodetest1.hashCode() ^ nodetest2.hashCode();
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
