package net.sf.saxon.pattern;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and kind. A NodeKindTest matches the node kind only.
  *
  * @author Michael H. Kay
  */

public class NodeKindTest extends NodeTest {

    public static final NodeKindTest DOCUMENT = new NodeKindTest(Type.DOCUMENT);
    public static final NodeKindTest ELEMENT = new NodeKindTest(Type.ELEMENT);
    public static final NodeKindTest ATTRIBUTE = new NodeKindTest(Type.ATTRIBUTE);
    public static final NodeKindTest TEXT = new NodeKindTest(Type.TEXT);
    public static final NodeKindTest COMMENT = new NodeKindTest(Type.COMMENT);
    public static final NodeKindTest PROCESSING_INSTRUCTION = new NodeKindTest(Type.PROCESSING_INSTRUCTION);
    public static final NodeKindTest NAMESPACE = new NodeKindTest(Type.NAMESPACE);


	private int kind;

	private NodeKindTest(int nodeKind) {
		kind = nodeKind;
	}

    /**
     * Make a test for a given kind of node
     */

    public static NodeTest makeNodeKindTest(int kind) {
		switch (kind) {
		    case Type.DOCUMENT:
		        return DOCUMENT;
		    case Type.ELEMENT:
                return ELEMENT;
		    case Type.ATTRIBUTE:
		        return ATTRIBUTE;
		    case Type.COMMENT:
		        return COMMENT;
		    case Type.TEXT:
		        return TEXT;
		    case Type.PROCESSING_INSTRUCTION:
		        return PROCESSING_INSTRUCTION;
		    case Type.NAMESPACE:
		        return NAMESPACE;
            case Type.NODE:
                return AnyNodeTest.getInstance();
            default:
                throw new IllegalArgumentException("Unknown node kind in NodeKindTest");
		}
    }

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
    * @param fingerprint identifies the expanded name of the node to be matched
    */

    public boolean matches(int nodeKind, int fingerprint, int annotation) {
        return (kind == nodeKind);
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind() == kind;
    }


    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.5;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return kind;
    }

    /**
     * Indicate whether this NodeTest is capable of matching text nodes
     */

    public boolean allowsTextNodes() {
        return kind == Type.TEXT;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<kind;
    }

    public String toString() {
        return toString(kind);
    }

    public static String toString(int kind) {
            switch (kind) {
                case Type.DOCUMENT:
                    return("document-node()" );
                case Type.ELEMENT:
                    return( "element()" );
                case Type.ATTRIBUTE:
                    return( "attribute()" );
                case Type.COMMENT:
                    return( "comment()" );
                case Type.TEXT:
                    return( "text()" );
                case Type.PROCESSING_INSTRUCTION:
                    return( "processing-instruction()" );
                case Type.NAMESPACE:
                    return( "namespace()" );
                default:
                    return( "** error **");
            }

    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return kind;
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
