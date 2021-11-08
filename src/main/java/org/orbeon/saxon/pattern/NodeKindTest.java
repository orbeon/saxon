package org.orbeon.saxon.pattern;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.tinytree.TinyTree;
import org.orbeon.saxon.type.*;

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
        return tree.getNodeKind(nodeNr) == kind;
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
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<kind;
    }

    /**
     * Get the content type allowed by this NodeTest (that is, the type of content allowed).
     * Return AnyType if there are no restrictions.
     */

    public SchemaType getContentType() {
        switch (kind) {
            case Type.DOCUMENT:
                return AnyType.getInstance();
            case Type.ELEMENT:
                return AnyType.getInstance();
            case Type.ATTRIBUTE:
                return AnySimpleType.getInstance();
            case Type.COMMENT:
                return BuiltInAtomicType.STRING;
            case Type.TEXT:
                return BuiltInAtomicType.UNTYPED_ATOMIC;
            case Type.PROCESSING_INSTRUCTION:
                return BuiltInAtomicType.STRING;
            case Type.NAMESPACE:
                return BuiltInAtomicType.STRING;
            default:
                throw new AssertionError("Unknown node kind");
        }
    }

    /**
     * Get the content type allowed by this NodeTest (that is, the type annotation).
     * Return AnyType if there are no restrictions. The default implementation returns AnyType.
     */

    public AtomicType getAtomizedItemType() {
        switch (kind) {
            case Type.DOCUMENT:
                return BuiltInAtomicType.UNTYPED_ATOMIC;
            case Type.ELEMENT:
                return BuiltInAtomicType.ANY_ATOMIC;
            case Type.ATTRIBUTE:
                return BuiltInAtomicType.ANY_ATOMIC;
            case Type.COMMENT:
                return BuiltInAtomicType.STRING;
            case Type.TEXT:
                return BuiltInAtomicType.UNTYPED_ATOMIC;
            case Type.PROCESSING_INSTRUCTION:
                return BuiltInAtomicType.STRING;
            case Type.NAMESPACE:
                return BuiltInAtomicType.STRING;
            default:
                throw new AssertionError("Unknown node kind");
        }
    }

    public String toString() {
        return toString(kind);
    }

    public static String toString(int kind) {
        switch (kind) {
            case Type.DOCUMENT:
                return( "document-node()" );
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
     * Get the name of a node kind
     * @param kind the node kind, for example Type.ELEMENT or Type.ATTRIBUTE
     * @return the name of the node kind, for example "element" or "attribute"
     */

    public static String nodeKindName(int kind) {
        switch (kind) {
            case Type.DOCUMENT:
                return( "document" );
            case Type.ELEMENT:
                return( "element" );
            case Type.ATTRIBUTE:
                return( "attribute" );
            case Type.COMMENT:
                return( "comment" );
            case Type.TEXT:
                return( "text" );
            case Type.PROCESSING_INSTRUCTION:
                return( "processing-instruction" );
            case Type.NAMESPACE:
                return( "namespace" );
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

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof NodeKindTest &&
                ((NodeKindTest)other).kind == kind;
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
