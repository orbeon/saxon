package net.sf.saxon.dom;
import net.sf.saxon.functions.DeepEqual;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;


/**
  * This class implements the DOM Node interface as a wrapper around a Saxon NodeInfo object
  */

public abstract class NodeOverNodeInfo implements Node {

    protected NodeInfo node;

    public NodeInfo getUnderlyingNodeInfo() {
        return node;
    }

    public void setUnderlyingNodeInfo(NodeInfo node) {
        this.node = node;
    }

    public static NodeOverNodeInfo wrap(NodeInfo node) {
        NodeOverNodeInfo n = null;
        if (node == null) {
            return null;
        }
        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                n = new DocumentOverNodeInfo();
                break;
            case Type.ELEMENT:
                n = new ElementOverNodeInfo();
                break;
            case Type.ATTRIBUTE:
                n = new AttrOverNodeInfo();
                break;
            case Type.TEXT:
            case Type.COMMENT:
                n = new TextOverNodeInfo();
                break;
            case Type.PROCESSING_INSTRUCTION:
                n = new PIOverNodeInfo();
                break;
            case Type.NAMESPACE:
                throw new IllegalArgumentException("Cannot wrap a namespace node as a DOM Node");
        }
        n.node = node;
        return n;
    }


    /**
    * Determine whether this is the same node as another node. DOM Level 3 method.
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public final boolean isSameNode(Node other) {
        if (other instanceof NodeOverNodeInfo) {
            return node.isSameNodeInfo(((NodeOverNodeInfo)other).node);
        } else {
            return false;
        }
    }

    /**
    * Get the base URI for the node. Default implementation for child nodes gets
    * the base URI of the parent node.
    */

    public String getBaseURI() {
        return node.getBaseURI();
    }

    /**
    * Get the name of this node, following the DOM rules
    * @return The name of the node. For an element this is the element name, for an attribute
    * it is the attribute name, as a QName. Other node types return conventional names such
    * as "#text" or "#comment"
    */

    public String getNodeName() {
        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                return "#document";
            case Type.ELEMENT:
                return node.getDisplayName();
            case Type.ATTRIBUTE:
                return node.getDisplayName();
            case Type.TEXT:
                return "#text";
            case Type.COMMENT:
                return "#comment";
            case Type.PROCESSING_INSTRUCTION:
                return node.getLocalPart();
            case Type.NAMESPACE:
                return node.getLocalPart();
            default:
                return "#unknown";
       }
    }

    /**
    * Get the local name of this node, following the DOM rules
    * @return The local name of the node. For an element this is the element name, for an attribute
    * it is the attribute name, as a QName. Other node types return conventional names such
    * as "#text" or "#comment"
    */

    public String getLocalName() {
        switch (node.getNodeKind()) {
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
                return node.getLocalPart();
            case Type.DOCUMENT:
            case Type.TEXT:
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
            default:
                return null;
       }
    }

    /**
     * Determine whether this (attribute) node is an ID. This method is introduced
     * in DOM Level 3. The current implementation is simplistic; it returns true if the
     * type annotation of the node is xs:ID (subtypes not allowed).
     */

    public boolean isId() {
        return node.getTypeAnnotation() == StandardNames.XS_ID;
    }

    /**
    * Determine whether the node has any children.
    * @return <code>true</code> if this node has any attributes,
    *   <code>false</code> otherwise.
    */

    public boolean hasChildNodes() {
        return node.iterateAxis(Axis.CHILD).next() != null;
    }

    /**
     * Returns whether this node has any attributes.
     * @return <code>true</code> if this node has any attributes,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttributes() {
        return node.iterateAxis(Axis.ATTRIBUTE).next() != null;
    }

    /**
    * Get the type of this node (DOM method). Note, the numbers assigned to node kinds
    * in Saxon (see module Type) are the same as those assigned in the DOM
    */

    public short getNodeType() {
        return (short)node.getNodeKind();
    }

    /**
     * Find the parent node of this node (DOM method).
     * @return The Node object describing the containing element or root node.
     */

    public Node getParentNode()  {
        return wrap(node.getParent());
    }

    /**
    * Get the previous sibling of the node (DOM method)
    * @return The previous sibling node. Returns null if the current node is the first
    * child of its parent.
    */

    public Node getPreviousSibling()  {
        return wrap((NodeInfo)node.iterateAxis(Axis.PRECEDING_SIBLING).next());
    }

   /**
    * Get next sibling node (DOM method)
    * @return The next sibling node. Returns null if the current node is the last
    * child of its parent.
    */

    public Node getNextSibling()  {
        return wrap((NodeInfo)node.iterateAxis(Axis.FOLLOWING_SIBLING).next());
    }

    /**
    * Get first child (DOM method)
    * @return the first child node of this node, or null if it has no children
    */

    public Node getFirstChild()  {
        return wrap((NodeInfo)node.iterateAxis(Axis.CHILD).next());
    }

    /**
    * Get last child (DOM method)
    * @return last child of this node, or null if it has no children
    */

    public Node getLastChild()  {
        AxisIterator children = node.iterateAxis(Axis.CHILD);
        NodeInfo last = null;
        while (true) {
            NodeInfo next = (NodeInfo)children.next();
            if (next == null) {
                return wrap(last);
            } else {
                last = next;
            }
        }
    }


    /**
     * Get the outermost element. (DOM method)
     * @return the Element for the outermost element of the document. If the document is
     * not well-formed, this returns the first element child of the root if there is one, otherwise
     * null.
     */

    public Element getDocumentElement() {
        NodeInfo root = node.getDocumentRoot();
        if (root==null) {
            return null;
        }
        AxisIterator children =
            root.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
        return (Element)wrap((NodeInfo)children.next());
    }


    /**
    * Get the node value as defined in the DOM.
    * This is not necessarily the same as the XPath string-value.
    */

    public String getNodeValue() {
        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                return null;
            case Type.ATTRIBUTE:
            case Type.TEXT:
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
                return node.getStringValue();
            default:
                return null;
        }
    }

    /**
    * Set the node value. DOM method: always fails
    */

    public void setNodeValue(String nodeValue) throws DOMException {
        disallowUpdate();
    }

    /**
     * Return a <code>NodeList</code> that contains all children of this node. If
     * there are no children, this is a <code>NodeList</code> containing no
     * nodes. DOM Method.
     */

    public NodeList getChildNodes() {
        try {
            List nodes = new ArrayList(10);
            SequenceIterator iter = node.iterateAxis(Axis.CHILD);
            while (true) {
                NodeInfo node = (NodeInfo)iter.next();
                if (node == null) break;
                nodes.add(NodeOverNodeInfo.wrap(node));
            }
            return new DOMNodeList(nodes);
        } catch (XPathException err) {
            return null;
            // can't happen
        }
    }

    /**
     * Return a <code>NamedNodeMap</code> containing the attributes of this node (if
     * it is an <code>Element</code> ) or <code>null</code> otherwise. (DOM method)
     */

    public NamedNodeMap getAttributes() {
        if (node.getNodeKind()==Type.ELEMENT) {
            return new DOMAttributeMap(node);
        } else {
            return null;
        }
    }

    /**
     * Return the <code>Document</code> object associated with this node. (DOM method)
     */

    public Document getOwnerDocument() {
        return (Document)wrap(node.getDocumentRoot());
    }

    /**
     * Insert the node <code>newChild</code> before the existing child node
     * <code>refChild</code>. DOM method: always fails.
     * @param newChild  The node to insert.
     * @param refChild  The reference node, i.e., the node before which the
     *   new node must be inserted.
     * @return  The node being inserted.
     * @exception org.w3c.dom.DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Always raised.
     */

    public Node insertBefore(Node newChild,
                             Node refChild)
                             throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Replace the child node <code>oldChild</code> with
     * <code>newChild</code> in the list of children, and returns the
     * <code>oldChild</code> node. Always fails.
     * @param newChild  The new node to put in the child list.
     * @param oldChild  The node being replaced in the list.
     * @return  The node replaced.
     * @exception org.w3c.dom.DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Always raised.
     */

    public Node replaceChild(Node newChild,
                             Node oldChild)
                             throws DOMException{
        disallowUpdate();
        return null;
    }

    /**
     * Remove the child node indicated by <code>oldChild</code> from the
     * list of children, and returns it. DOM method: always fails.
     * @param oldChild  The node being removed.
     * @return  The node removed.
     * @exception org.w3c.dom.DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Always raised.
     */

    public Node removeChild(Node oldChild) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     *  Adds the node <code>newChild</code> to the end of the list of children
     * of this node. DOM method: always fails.
     * @param newChild  The node to add.
     * @return  The node added.
     * @exception org.w3c.dom.DOMException
     *   <br> NO_MODIFICATION_ALLOWED_ERR: Always raised.
     */

    public Node appendChild(Node newChild) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Returns a duplicate of this node, i.e., serves as a generic copy
     * constructor for nodes. The duplicate node has no parent. Not
     * implemented: always returns null. (Because trees are read-only, there
     * would be no way of using the resulting node.)
     * @param deep  If <code>true</code> , recursively clone the subtree under
     *   the specified node; if <code>false</code> , clone only the node
     *   itself (and its attributes, if it is an <code>Element</code> ).
     * @return  The duplicate node.
     */

    public Node cloneNode(boolean deep) {
        // Not implemented
        return null;
    }

    /**
     * Puts all <code>Text</code> nodes in the full depth of the sub-tree
     * underneath this <code>Node</code>, including attribute nodes, into a
     * "normal" form where only structure (e.g., elements, comments,
     * processing instructions, CDATA sections, and entity references)
     * separates <code>Text</code> nodes, i.e., there are neither adjacent
     * <code>Text</code> nodes nor empty <code>Text</code> nodes.
     * @since DOM Level 2
     */

    public void normalize() {
        // null operation; nodes are always normalized
    }

    /**
     *  Tests whether the DOM implementation implements a specific feature and
     * that feature is supported by this node.
     * @param feature  The name of the feature to test. This is the same name
     *   which can be passed to the method <code>hasFeature</code> on
     *   <code>DOMImplementation</code> .
     * @param version  This is the version number of the feature to test. In
     *   Level 2, version 1, this is the string "2.0". If the version is not
     *   specified, supporting any version of the feature will cause the
     *   method to return <code>true</code> .
     * @return  Returns <code>true</code> if the specified feature is supported
     *    on this node, <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean isSupported(String feature,
                               String version) {
        return feature.equalsIgnoreCase("xml");
    }

    /**
    * Alternative to isSupported(), defined in a draft DOM spec
    */

    public boolean supports(String feature,
                               String version) {
        return isSupported(feature, version);
    }

    /**
     * The namespace URI of this node, or <code>null</code> if it is
     * unspecified. DOM method.
     * <br> This is not a computed value that is the result of a namespace
     * lookup based on an examination of the namespace declarations in scope.
     * It is merely the namespace URI given at creation time.
     * <br> For nodes of any type other than <code>ELEMENT_NODE</code> and
     * <code>ATTRIBUTE_NODE</code> and nodes created with a DOM Level 1
     * method, such as <code>createElement</code> from the
     * <code>Document</code> interface, this is always <code>null</code> .
     * Per the  Namespaces in XML Specification  an attribute does not
     * inherit its namespace from the element it is attached to. If an
     * attribute is not explicitly given a namespace, it simply has no
     * namespace.
     * @since DOM Level 2
     */

    public String getNamespaceURI() {
        String uri = node.getURI();
        return ("".equals(uri) ? null : uri);
    }

    /**
     * The namespace prefix of this node, or <code>null</code> if it is
     * unspecified.
     * <br>For nodes of any type other than <code>ELEMENT_NODE</code> and
     * <code>ATTRIBUTE_NODE</code> and nodes created with a DOM Level 1
     * method, such as <code>createElement</code> from the
     * <code>Document</code> interface, this is always <code>null</code>.
     *
     * @since DOM Level 2
     */

    public String getPrefix() {
        String p = node.getNamePool().getPrefix(node.getNameCode());
        return ("".equals(p) ? null : p);
    }

    /**
    * Set the namespace prefix of this node. Always fails.
    */

    public void setPrefix(String prefix)
                            throws DOMException {
        disallowUpdate();
    }

    /**
     * Compare the position of the (other) node in document order with the reference node (this node).
     * DOM Level 3 method.
     * @param other the other node.
     * @return -1 (this node is first), 0 (same node), +1 (other node is first)
     * @throws org.w3c.dom.DOMException
     */

    public short compareDocumentPosition(Node other) throws DOMException {
        final short      DOCUMENT_POSITION_DISCONNECTED = 0x01;
        final short      DOCUMENT_POSITION_PRECEDING    = 0x02;
        final short      DOCUMENT_POSITION_FOLLOWING    = 0x04;
        final short      DOCUMENT_POSITION_CONTAINS     = 0x08;
        final short      DOCUMENT_POSITION_CONTAINED_BY = 0x10;
        if (!(other instanceof NodeOverNodeInfo)) {
            return DOCUMENT_POSITION_DISCONNECTED;
        }
        int c = node.compareOrder(((NodeOverNodeInfo)other).node);
        if (c==0) {
            return (short)0;
        } else if (c==-1) {
            short d = compareDocumentPosition(other.getParentNode());
            short result = DOCUMENT_POSITION_FOLLOWING;
            if (d==0 || (d&DOCUMENT_POSITION_CONTAINED_BY) != 0) {
                d |= DOCUMENT_POSITION_CONTAINED_BY;
            }
            return result;
        } else if (c==+1) {
            short d = ((NodeOverNodeInfo)getParentNode()).compareDocumentPosition(other);
            short result = DOCUMENT_POSITION_PRECEDING;
            if (d==0 || (d&DOCUMENT_POSITION_CONTAINS) != 0) {
                d |= DOCUMENT_POSITION_CONTAINS;
            }
            return result;
        } else {
            throw new AssertionError();
        }
    }

    /**
     * Get the text content of a node. This is a DOM Level 3 method. The definition
     * is the same as the definition of the string value of a node in XPath, except
     * in the case of document nodes.
     * @return the string value of the node, or null in the case of document nodes.
     * @throws org.w3c.dom.DOMException
     */

    public String getTextContent() throws DOMException {
        if (node.getNodeKind() == Type.DOCUMENT) {
            return null;
        } else {
            return node.getStringValue();
        }
    }

    /**
     * Set the text content of a node. DOM Level 3 method, not supported.
     * @param textContent
     * @throws org.w3c.dom.DOMException
     */

    public void setTextContent(String textContent) throws DOMException {
        disallowUpdate();
    }

    /**
     * Get the (first) prefix assigned to a specified namespace URI, or null
     * if the namespace is not in scope. DOM Level 3 method.
     * @param namespaceURI the namespace whose prefix is required
     * @return the corresponding prefix
     */

    public String lookupPrefix(String namespaceURI){
        AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
        while (true) {
            NodeInfo ns = (NodeInfo)iter.next();
            if (ns==null) {
                return null;
            }
            if (ns.getStringValue().equals(namespaceURI)) {
                return ns.getLocalPart();
            }
        }
    }

    /**
     * Test whether a particular namespace is the default namespace.
     * DOM Level 3 method.
     * @param namespaceURI the namespace to be tested
     * @return true if this is the default namespace
     */

    public boolean isDefaultNamespace(String namespaceURI) {
        return namespaceURI.equals(lookupNamespaceURI(""));
    }

    /**
     * Find the URI corresponding to a given in-scope prefix
     * @param prefix The namespace prefix whose namespace URI is required.
     * @return the corresponding namespace URI, or null if the prefix is
     * not declared.
     */

    public String lookupNamespaceURI(String prefix) {
        AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
        while (true) {
            NodeInfo ns = (NodeInfo)iter.next();
            if (ns==null) {
                return null;
            }
            if (ns.getLocalPart().equals(prefix)) {
                return ns.getStringValue();
            }
        }
    }

    /**
     * Compare whether two nodes have the same content. This is a DOM Level 3 method; the implementation
     * uses the same algorithm as the XPath deep-equals() function.
     * @param arg The node to be compared. This must wrap a Saxon NodeInfo.
     * @return true if the two nodes are deep-equal.
     */

    public boolean isEqualNode(Node arg) {
        if (!(arg instanceof NodeOverNodeInfo)) {
            throw new IllegalArgumentException("Other Node must wrap a Saxon NodeInfo");
        }
        return DeepEqual.deepEquals(
                SingletonIterator.makeIterator(node),
                SingletonIterator.makeIterator(((NodeOverNodeInfo)arg).node),
                new AtomicComparer(CodepointCollator.getInstance()),
                node.getConfiguration());
    }

    /**
     * Get a feature of this node. DOM Level 3 method, always returns null.
     * @param feature the required feature
     * @param version the version of the required feature
     * @return the value of the feature. Always null in this implementation
     */

    public Object getFeature(String feature, String version) {
        return null;
    }

    /**
     * Set user data. Always throws UnsupportedOperationException in this implementation
     * @param key
     * @param data
     * @param handler
     * @return This implementation always throws an exception
     */

    public Object setUserData(String key, Object data, UserDataHandler handler) {
        disallowUpdate();
        return null;
    }

    /**
     * Get user data associated with this node. DOM Level 3 method, always returns
     * null in this implementation
     * @param key identifies the user data required
     * @return always null in this implementation
     */
    public Object getUserData(String key) {
        return null;
    }

    /**
    * Internal method used to indicate that update operations are not allowed
    */

    protected static void disallowUpdate() throws DOMException {
        throw new UnsupportedOperationException("The Saxon DOM cannot be updated");
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
