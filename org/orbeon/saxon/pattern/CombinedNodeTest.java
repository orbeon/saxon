package net.sf.saxon.pattern;
import net.sf.saxon.expr.Token;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.AnyType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;

import java.util.Set;

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

    public String toString() {
        if (nodetest1 instanceof NameTest) {
            int kind = nodetest1.getPrimitiveType();
            String content = "";
            if (nodetest2 instanceof ContentTypeTest) {
                content = ", " + ((ContentTypeTest)nodetest2).getSchemaType().getDisplayName();
            }
            if (kind == Type.ELEMENT) {
                return "schema-element(*" + content + ")";
            } else if (kind == Type.ATTRIBUTE) {
                return "schema-attribute(*" + content + ")";
            }
        }
        String nt1 = (nodetest1==null ? "true()" : nodetest1.toString());
        String nt2 = (nodetest2==null ? "true()" : nodetest2.toString());
        return "(" + nt1 + " " + Token.tokens[operator] + " " + nt2 + ")";
    }

    /**
     * Indicate whether this NodeTest is capable of matching text nodes
     */

    public boolean allowsTextNodes() {
        return nodetest1.allowsTextNodes() && nodetest2.allowsTextNodes();
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return nodetest1.getNodeKindMask() & nodetest2.getNodeKindMask();
    }

    /**
     * Get the set of node names allowed by this NodeTest. This is returned as a set of Integer fingerprints.
     * A null value indicates that all names are permitted (i.e. that there are no constraints on the node name.
     * The default implementation returns null.
     */

    public Set getRequiredNodeNames() {
        if (nodetest2.getRequiredNodeNames() == null) {
            return nodetest1.getRequiredNodeNames();
        }
        if (nodetest1.getRequiredNodeNames() == null) {
            return nodetest2.getRequiredNodeNames();
        }
        throw new UnsupportedOperationException("Combined nodetest has multiple name constraints");
    }

    /**
     * Get the content type allowed by this NodeTest (that is, the type annotation of the matched nodes).
     * Return AnyType if there are no restrictions. The default implementation returns AnyType.
     */

    public SchemaType getContentType() {
        if (nodetest2.getContentType() instanceof AnyType) {
            return nodetest1.getContentType();
        }
        if (nodetest1.getContentType() instanceof AnyType) {
            return nodetest2.getContentType();
        }
        throw new UnsupportedOperationException("Combined nodetest has multiple content type constraints");
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
