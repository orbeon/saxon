package net.sf.saxon.om;
import net.sf.saxon.Err;
import net.sf.saxon.dom.DOMNodeList;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.functions.DeepEqual;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.tree.DOMExceptionImpl;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.*;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.dom.DOMLocator;
import java.util.ArrayList;


/**
  * This class is an abstract implementation of the Saxon NodeInfo interface;
  * it also contains concrete implementations of most of the DOM methods in terms
  * of the NodeInfo methods. These include all the methods defined on the DOM Node
  * class itself, and most of those defined on subclasses such as Document, Text,
  * and Comment: because
  * of the absence of multiple inheritance, this is the only way of making these
  * methods reusable by multiple implementations.
  * The class contains no data, and can be used as a common
  * superclass for different implementations of Node and NodeInfo.
  * @author Michael H. Kay
  */

public abstract class AbstractNode
        implements Node, NodeInfo, FingerprintedNode, SourceLocator, DOMLocator {

    /**
    * Chararacteristic letters to identify each type of node, indexed using the node type
    * values. These are used as the initial letter of the result of generate-id()
    */

    public static final char[] NODE_LETTER =
        {'x', 'e', 'a', 't', 'x', 'x', 'x', 'p', 'c', 'r', 'x', 'x', 'x', 'n'};


    /**
     * Determine whether this is the same node as another node.
     * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b).
     * This method has the same semantics as isSameNode() in DOM Level 3, but
     * works on Saxon NodeInfo objects rather than DOM Node objects.
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *      the same node in the tree.
     */

    public abstract boolean isSameNodeInfo(NodeInfo other);

    /**
    * Determine whether this is the same node as another node. DOM Level 3 method.
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public final boolean isSameNode(Node other) {
        if (other instanceof NodeInfo) {
            return isSameNodeInfo((NodeInfo)other);
        } else {
            return false;
        }
    }

    /**
    * Get a character string that uniquely identifies this node
    * @return a string.
    */

    public abstract String generateId();

    /**
    * Get the system ID for the entity containing the node.
    */

    public abstract String getSystemId();

    /**
    * Get the base URI for the node. Default implementation for child nodes gets
    * the base URI of the parent node.
    */

    public abstract String getBaseURI();

	/**
	* Get the node corresponding to this javax.xml.transform.dom.DOMLocator
	*/

    public Node getOriginatingNode() {
        return this;
    }

    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public abstract int compareOrder(NodeInfo other);

	/**
	* Get the name code of the node, used for displaying names
	*/

	public abstract int getNameCode();

	/**
	* Get the fingerprint of the node, used for matching names
	*/

	public abstract int getFingerprint();

    /**
    * Get the name of this node, following the DOM rules
    * @return The name of the node. For an element this is the element name, for an attribute
    * it is the attribute name, as a QName. Other node types return conventional names such
    * as "#text" or "#comment"
    */

    public String getNodeName() {
        switch (getNodeKind()) {
            case Type.DOCUMENT:
                return "#document";
            case Type.ELEMENT:
                return getDisplayName();
            case Type.ATTRIBUTE:
                return getDisplayName();
            case Type.TEXT:
                return "#text";
            case Type.COMMENT:
                return "#comment";
            case Type.PROCESSING_INSTRUCTION:
                return getLocalPart();
            case Type.NAMESPACE:
                return getLocalPart();
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
        switch (getNodeKind()) {
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
                return getLocalPart();
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
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
    * @return The URI of the namespace of this node. For an unnamed node, or for
    * an element or attribute in the default namespace, return an empty string.
    */

    public abstract String getURI();

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        String localName = getLocalPart();
        if (localName==null || "".equals(localName)) {
            return "";
        }
        String prefix = getPrefix();
        if ("".equals(prefix)) {
            return localName;
        }
        return prefix + ':' + localName;
    }

    /**
    * Get the local name of this node.
    * @return The local name of this node.
    * For a node with no name, return null (as required by DOM); but for an unnamed
     * namespace node, return "".
    */

    public abstract String getLocalPart();

    /**
     * Get the string value of the node
     */

    public abstract String getStringValue();

    /**
    * Get the type annotation of this node, if any
    */

    public int getTypeAnnotation() {
        return -1;
    }

    /**
     * Determine whether this (attribute) node is an ID. This method is introduced
     * in DOM Level 3. The current implementation is simplistic; it returns true if the
     * type annotation of the node is xs:ID (subtypes not allowed).
     */

    public boolean isId() {
        return getTypeAnnotation() == StandardNames.XS_ID;
    }

    /**
    * Determine whether the node has any children.
    * @return <code>true</code> if this node has any attributes,
    *   <code>false</code> otherwise.
    */

    public abstract boolean hasChildNodes();

    /**
     * Returns whether this node has any attributes.
     * @return <code>true</code> if this node has any attributes,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public abstract boolean hasAttributes();

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public abstract String getAttributeValue(int fingerprint);

    /**
    * Get the line number of the node within its source document entity.
    * The default implementation returns -1, meaning unknown
    */

    public int getLineNumber() {
        return -1;
    }

    /**
    * Get the column number of the node.
    * The default implementation returns -1, meaning unknown
    */

    public int getColumnNumber() {
        return -1;
    }

    /**
    * Get the public identifier of the document entity containing this node.
    * The default implementation returns null, meaning unknown
    */

    public String getPublicId() {
        return null;
    }

    /**
    * Return an enumeration over the nodes reached by the given axis from this node
    * @param axisNumber The axis to be used
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a AxisIterator that scans the nodes reached by the axis in turn.
    */

    public abstract AxisIterator iterateAxis(
                                        byte axisNumber,
                                        NodeTest nodeTest);

    /**
     * Find the parent node of this node.
     * @return The Node object describing the containing element or root node.
     */

    public abstract NodeInfo getParent();

    /**
    * Get the root node (which is not necessarily a document node)
    * @return the NodeInfo representing the containing document
    */

    public NodeInfo getRoot() {
        NodeInfo self = this;
        NodeInfo parent = getParent();
        while (parent != null) {
            self = parent;
            parent = parent.getParent();
        }
        return self;
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document.
    * If this node is in a tree whose root is not a document node, return null.
    */

    public DocumentInfo getDocumentRoot() {
        NodeInfo root = getRoot();
        if (root instanceof DocumentInfo) {
            return (DocumentInfo)root;
        } else {
            return null;
        }
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return getRoot().getDocumentNumber();
    }

    /**
    * Get the type of this node (DOM method). Note, the numbers assigned to node kinds
    * in Saxon (see module Type) are the same as those assigned in the DOM
    */

    public short getNodeType() {
        return (short)getNodeKind();
    }

    /**
     * Get the ItemType. This is the most specific type that the node
     * satisfies; it includes both the node kind and the node name and
     * type annotation
     */

    /**
     * Find the parent node of this node (DOM method).
     * @return The Node object describing the containing element or root node.
     */

    public Node getParentNode()  {
        return (Node)getParent();
    }

    /**
    * Get the previous sibling of the node (DOM method)
    * @return The previous sibling node. Returns null if the current node is the first
    * child of its parent.
    */

    public Node getPreviousSibling()  {
        return (Node)iterateAxis(Axis.PRECEDING_SIBLING).next();
    }

   /**
    * Get next sibling node (DOM method)
    * @return The next sibling node. Returns null if the current node is the last
    * child of its parent.
    */

    public Node getNextSibling()  {
        return (Node)iterateAxis(Axis.FOLLOWING_SIBLING).next();
    }

    /**
    * Get first child (DOM method)
    * @return the first child node of this node, or null if it has no children
    */

    public Node getFirstChild()  {
        return (Node)iterateAxis(Axis.CHILD).next();
    }

    /**
    * Get last child (DOM method)
    * @return last child of this node, or null if it has no children
    */

    public Node getLastChild()  {
        AxisIterator children = iterateAxis(Axis.CHILD);
        NodeInfo last = null;
        while (true) {
            NodeInfo next = (NodeInfo)children.next();
            if (next == null) {
                return (Node)last;
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
        NodeInfo root = getDocumentRoot();
        if (root==null) {
            return null;
        }
        AxisIterator children =
            root.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
        return (Element)children.next();
    }

    /**
    * Get the typed value of this node.
     * If there is no type annotation, we return the string value, as an instance
     * of xdt:untypedAtomic
    */

    public SequenceIterator getTypedValue() throws XPathException {
        int annotation = getTypeAnnotation();
        if (annotation==-1) {
            return SingletonIterator.makeIterator(
                                new UntypedAtomicValue(getStringValue()));
        } else {
            SchemaType stype = getConfiguration().getSchemaType(annotation);
            if (stype == null) {
                String typeName = getNamePool().getDisplayName(annotation);
                throw new DynamicError("Unknown type annotation " +
                        Err.wrap(typeName) + " in document instance");
            } else {
                return stype.getTypedValue(this);
            }
        }
    }

    /**
    * Output all namespace nodes associated with this element. Does nothing if
    * the node is not an element.
    * @param out The relevant outputter
    * @param includeAncestors True if namespaces declared on ancestor elements must
    * be output; false if it is known that these are already on the result tree
    */

    public void outputNamespaceNodes(Receiver out, boolean includeAncestors)
        throws XPathException
    {}


    /**
    * Get the node value as defined in the DOM.
    * This is not necessarily the same as the XPath string-value.
    */

    public String getNodeValue() {
        switch (getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                return null;
            case Type.ATTRIBUTE:
            case Type.TEXT:
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
                return getStringValue();
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
            return new DOMNodeList(
                    new SequenceExtent(iterateAxis(Axis.CHILD)));
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
        if (getNodeKind()==Type.ELEMENT) {
            return new DOMAttributeMap(this);
        } else {
            return null;
        }
    }

    /**
     * Return the <code>Document</code> object associated with this node. (DOM method)
     */

    public Document getOwnerDocument() {
        return (Document)getDocumentRoot();
    }

    /**
     * Insert the node <code>newChild</code> before the existing child node
     * <code>refChild</code>. DOM method: always fails.
     * @param newChild  The node to insert.
     * @param refChild  The reference node, i.e., the node before which the
     *   new node must be inserted.
     * @return  The node being inserted.
     * @exception DOMException
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
     * @exception DOMException
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
     * @exception DOMException
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
     * @exception DOMException
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
        String uri = getURI();
        return ("".equals(uri) ? null : uri);
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
     * @throws DOMException
     * @throws UnsupportedOperationException (always)
     */

    public short compareDocumentPosition(Node other) throws DOMException {
        final short      DOCUMENT_POSITION_DISCONNECTED = 0x01;
        final short      DOCUMENT_POSITION_PRECEDING    = 0x02;
        final short      DOCUMENT_POSITION_FOLLOWING    = 0x04;
        final short      DOCUMENT_POSITION_CONTAINS     = 0x08;
        final short      DOCUMENT_POSITION_CONTAINED_BY = 0x10;
        if (!(other instanceof NodeInfo)) {
            return DOCUMENT_POSITION_DISCONNECTED;
        }
        int c = compareOrder((NodeInfo)other);
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
            short d = ((AbstractNode)getParentNode()).compareDocumentPosition(other);
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
     * @throws DOMException
     */

    public String getTextContent() throws DOMException {
        if (getNodeKind() == Type.DOCUMENT) {
            return null;
        } else {
            return getStringValue();
        }
    }

    /**
     * Set the text content of a node. DOM Level 3 method, not supported.
     * @param textContent
     * @throws DOMException
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
        AxisIterator iter = iterateAxis(Axis.NAMESPACE);
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
        AxisIterator iter = iterateAxis(Axis.NAMESPACE);
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
     * @param arg The node to be compared. This must be a Saxon NodeInfo.
     * @return true if the two nodes are deep-equal.
     */

    public boolean isEqualNode(Node arg) {
        if (!(arg instanceof NodeInfo)) {
            throw new IllegalArgumentException("Other Node must be a Saxon NodeInfo");
        }
        return DeepEqual.deepEquals(
                SingletonIterator.makeIterator(this),
                SingletonIterator.makeIterator((NodeInfo)arg),
                new AtomicComparer(CodepointCollator.getInstance()),
                getDocumentRoot().getConfiguration());
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

    ////////////////////////////////////////////////////////////////////////////
    // DOM methods defined on the Document class
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Get the Document Type Declaration (see <code>DocumentType</code> )
     * associated with this document. For HTML documents as well as XML
     * documents without a document type declaration this returns
     * <code>null</code>. DOM method.
     * @return null: The Saxon tree model does not include the document type
     * information.
     */

    public DocumentType getDoctype() {
        return null;
    }

    /**
     * Get a <code>DOMImplementation</code> object that handles this document.
     * A DOM application may use objects from multiple implementations.
     * DOM method.
     */

    public DOMImplementation getImplementation() {
        return new DOMImplementationImpl();
    }

    /**
     * Creates an element of the type specified. DOM method: always fails,
     * because the Saxon tree is not updateable.
     */

    public Element createElement(String tagName) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Creates an empty <code>DocumentFragment</code> object.
     * @return  A new <code>DocumentFragment</code> .
     * DOM method: returns null, because the Saxon tree is not updateable.
     */

    public DocumentFragment createDocumentFragment() {
        return null;
    }

    /**
     * Create a <code>Text</code> node given the specified string.
     * DOM method: returns null, because the Saxon tree is not updateable.
     * @param data  The data for the node.
     * @return  The new <code>Text</code> object.
     */

    public Text createTextNode(String data) {
        return null;
    }

    /**
     * Create a <code>Comment</code> node given the specified string.
     * DOM method: returns null, because the Saxon tree is not updateable.
     * @param data  The data for the node.
     * @return  The new <code>Comment</code> object.
     */
    public Comment createComment(String data) {
        return null;
    }

    /**
     * Create a <code>CDATASection</code> node whose value  is the specified
     * string.
     * DOM method: always fails, because the Saxon tree is not updateable.
     * @param data  The data for the <code>CDATASection</code> contents.
     * @return  The new <code>CDATASection</code> object.
     * @exception DOMException
     *    NOT_SUPPORTED_ERR: Raised if this document is an HTML document.
     */

    public CDATASection createCDATASection(String data) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Create a <code>ProcessingInstruction</code> node given the specified
     * name and data strings.
     * DOM method: returns null, because the Saxon tree is not updateable.
     * @param target  The target part of the processing instruction.
     * @param data  The data for the node.
     * @return  The new <code>ProcessingInstruction</code> object.
     * @exception DOMException
     *    INVALID_CHARACTER_ERR: Raised if the specified target contains an
     *   illegal character.
     *   <br> NOT_SUPPORTED_ERR: Raised if this document is an HTML document.
     */

    public ProcessingInstruction createProcessingInstruction(String target,
                                                             String data)
                                                             throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Create an <code>Attr</code> of the given name.
     * DOM method: always fails, because the Saxon tree is not updateable.
     * @param name  The name of the attribute.
     * @return  A new <code>Attr</code> object with the <code>nodeName</code>
     *   attribute set to <code>name</code> , and <code>localName</code> ,
     *   <code>prefix</code> , and <code>namespaceURI</code> set to
     *   <code>null</code> .
     * @exception DOMException
     *    INVALID_CHARACTER_ERR: Raised if the specified name contains an
     *   illegal character.
     */

    public Attr createAttribute(String name) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Create an <code>EntityReference</code> object.
     * DOM method: returns null, because the Saxon tree is not updateable.
     * @param name  The name of the entity to reference.
     * @return  The new <code>EntityReference</code> object.
     * @exception DOMException
     *    INVALID_CHARACTER_ERR: Raised if the specified name contains an
     *   illegal character.
     *   <br> NOT_SUPPORTED_ERR: Raised if this document is an HTML document.
     */

    public EntityReference createEntityReference(String name) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Return a <code>NodeList</code> of all the <code>Elements</code> with
     * a given tag name in the order in which they are encountered in a
     * preorder traversal of the <code>Document</code> tree.
     * @param tagname  The name of the tag to match on. The special value "*"
     *   matches all tags.
     * @return  A new <code>NodeList</code> object containing all the matched
     *   <code>Elements</code> .
     */

    public NodeList getElementsByTagName(String tagname) {
        // The DOM method is defined only on the Document and Element nodes,
        // but we'll support it on any node.

        AxisIterator allElements = iterateAxis(Axis.DESCENDANT);
        ArrayList nodes = new ArrayList(500);
        while(true) {
            NodeInfo next = (NodeInfo)allElements.next();
            if (next == null) {
                break;
            }
            if (next.getNodeKind()==Type.ELEMENT) {
                if (tagname.equals("*") || tagname.equals(next.getDisplayName())) {
                    nodes.add(next);
                }
            }
        }
        return new DOMNodeList(new SequenceExtent(nodes));
    }


    /**
     * Import a node from another document to this document.
     * DOM method: always fails, because the Saxon tree is not updateable.
     * @exception DOMException
     * @since DOM Level 2
     */

    public Node importNode(Node importedNode, boolean deep) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Create an element of the given qualified name and namespace URI.
     * HTML-only DOM implementations do not need to implement this method.
     * DOM method: always fails, because the Saxon tree is not updateable.
     * @param namespaceURI  The  namespace URI of the element to create.
     * @param qualifiedName  The  qualified name of the element type to
     *   instantiate.
     * @return  A new <code>Element</code> object
     * @exception DOMException
     */

    public Element createElementNS(String namespaceURI,
                                   String qualifiedName)
                                   throws DOMException
    {
        disallowUpdate();
        return null;
    }

    /**
     * Create an attribute of the given qualified name and namespace URI.
     * HTML-only DOM implementations do not need to implement this method.
     * DOM method: returns null, because the Saxon tree is not updateable.
     * @param namespaceURI  The  namespace URI of the attribute to create.
     * @param qualifiedName  The  qualified name of the attribute to
     *   instantiate.
     * @return  A new <code>Attr</code> object.
     * @exception DOMException
     */

    public Attr createAttributeNS(String namespaceURI,
                                  String qualifiedName)
                                  throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Return a <code>NodeList</code> of all the <code>Elements</code> with
     * a given  local name and namespace URI in the order in which they are
     * encountered in a preorder traversal of the <code>Document</code> tree.
     * DOM method.
     * @param namespaceURI  The  namespace URI of the elements to match on.
     *   The special value "*" matches all namespaces.
     * @param localName  The  local name of the elements to match on. The
     *   special value "*" matches all local names.
     * @return  A new <code>NodeList</code> object containing all the matched
     *   <code>Elements</code> .
     * @since DOM Level 2
     */

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        // The DOM method is defined only on the Document and Element nodes,
        // but we'll support it on any node.

        AxisIterator allElements = iterateAxis(Axis.DESCENDANT);
        ArrayList nodes = new ArrayList(500);
        while(true) {
            NodeInfo next = (NodeInfo)allElements.next();
            if (next == null) {
                break;
            }
            if (next.getNodeKind()==Type.ELEMENT) {
                if ((namespaceURI.equals("*") || namespaceURI.equals(next.getURI())) &&
                    (localName.equals("*") || localName.equals(next.getLocalPart()))) {
                    nodes.add(next);
                }
            }
        }
        return new DOMNodeList(new SequenceExtent(nodes));
    }

    /**
     * Return the <code>Element</code> whose <code>ID</code> is given by
     * <code>elementId</code> . If no such element exists, returns
     * <code>null</code> . Behavior is not defined if more than one element
     * has this <code>ID</code> .  The DOM implementation must have
     * information that says which attributes are of type ID. Attributes with
     * the name "ID" are not of type ID unless so defined. Implementations
     * that do not know whether attributes are of type ID or not are expected
     * to return <code>null</code> .
     * @param elementId  The unique <code>id</code> value for an element.
     * @return  The matching element, or null if there is none.
     * @since DOM Level 2
     */

    public Element getElementById(String elementId) {
        // Defined on Document node; but we support it on any node.
        DocumentInfo doc = getDocumentRoot();
        if (doc == null) {
            return null;
        }
        return (Element)doc.selectID(elementId);
    }

    //////////////////////////////////////////////////////////////////
    // Methods defined on the DOM Element class
    //////////////////////////////////////////////////////////////////

    /**
     *  The name of the element (DOM interface).
     */

    public String getTagName() {
        return getDisplayName();
    }

    /**
     * Retrieves an attribute value by name. Namespace declarations will not
     * be retrieved. DOM interface.
     * @param name  The QName of the attribute to retrieve.
     * @return  The <code>Attr</code> value as a string, or the empty string if
     *    that attribute does not have a specified or default value.
     */

    public String getAttribute(String name) {
        AxisIterator atts = iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return "";
            }
            if (att.getDisplayName().equals(name)) {
                String val = att.getStringValue();
                if (val==null) return "";
                return val;
            }
        }
    }

    /**
     * Retrieves an attribute node by name.
     * Namespace declarations will not be retrieved.
     * <br> To retrieve an attribute node by qualified name and namespace URI,
     * use the <code>getAttributeNodeNS</code> method.
     * @param name  The name (<code>nodeName</code> ) of the attribute to
     *   retrieve.
     * @return  The <code>Attr</code> node with the specified name (
     *   <code>nodeName</code> ) or <code>null</code> if there is no such
     *   attribute.
     */

    public Attr getAttributeNode(String name) {
        AxisIterator atts = iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return null;
            }
            if (att.getDisplayName().equals(name)) {
                return (Attr)att;
            }
        }
    }

    /**
     * Adds a new attribute node. Always fails
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Removes the specified attribute. Always fails
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void removeAttribute(String oldAttr) throws DOMException {
        disallowUpdate();
    }

    /**
     * Removes the specified attribute node. Always fails
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        disallowUpdate();
        return null;
    }


    /**
     * Retrieves an attribute value by local name and namespace URI.
     * HTML-only DOM implementations do not need to implement this method.
     * @param namespaceURI  The  namespace URI of the attribute to retrieve.
     * @param localName  The  local name of the attribute to retrieve.
     * @return  The <code>Attr</code> value as a string, or the empty string if
     *    that attribute does not have a specified or default value.
     * @since DOM Level 2
     */

    public String getAttributeNS(String namespaceURI, String localName) {
    	String val = Navigator.getAttributeValue(this, namespaceURI, localName);
    	if (val==null) return "";
    	return val;
    }

    /**
     * Adds a new attribute. Always fails.
     * @param namespaceURI  The  namespace URI of the attribute to create or
     *   alter.
     * @param qualifiedName  The  qualified name of the attribute to create or
     *   alter.
     * @param value  The value to set in string form.
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void setAttributeNS(String namespaceURI,
                               String qualifiedName,
                               String value)
                               throws DOMException {
        disallowUpdate();
    }

    /**
     * Removes an attribute by local name and namespace URI. Always fails
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     * @since DOM Level 2
     */

    public void removeAttributeNS(String namespaceURI,
                                  String localName)
                                  throws DOMException{
        disallowUpdate();
    }

    /**
     * Retrieves an <code>Attr</code> node by local name and namespace URI.
     * DOM method, so namespace declarations count as attributes.
     * @param namespaceURI  The  namespace URI of the attribute to retrieve.
     * @param localName  The  local name of the attribute to retrieve.
     * @return  The <code>Attr</code> node with the specified attribute local
     *   name and namespace URI or <code>null</code> if there is no such
     *   attribute.
     * @since DOM Level 2
     */

    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        DocumentInfo doc = getDocumentRoot();
        if (doc==null) {
            throw new UnsupportedOperationException("getAttributeNodeNS is not supported on a tree with no document node");
        }
        int fingerprint = doc.getNamePool().getFingerprint(namespaceURI, localName);
        if (fingerprint==-1) return null;
        NameTest test = new NameTest(Type.ATTRIBUTE, fingerprint, getNamePool());
        AxisIterator atts = iterateAxis(Axis.ATTRIBUTE, test);
        return (Attr)atts.next();
    }

    /**
     * Add a new attribute. Always fails.
     * @param newAttr  The <code>Attr</code> node to add to the attribute list.
     * @return  If the <code>newAttr</code> attribute replaces an existing
     *   attribute with the same  local name and  namespace URI , the
     *   replaced <code>Attr</code> node is returned, otherwise
     *   <code>null</code> is returned.
     * @exception DOMException
     *   <br> NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     * @since DOM Level 2
     */

    public Attr setAttributeNodeNS(Attr newAttr)
                                   throws DOMException{
        disallowUpdate();
        return null;
    }

    /**
     * Returns <code>true</code> when an attribute with a given name is
     * specified on this element or has a default value, <code>false</code>
     * otherwise.
     * Namespace declarations will not be retrieved.
     * @param name  The name of the attribute to look for.
     * @return <code>true</code> if an attribute with the given name is
     *   specified on this element or has a default value, <code>false</code>
     *   otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttribute(String name) {
        AxisIterator atts = iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return false;
            }
            if (att.getDisplayName().equals(name)) {
                return true;
            }
        }
    }

    /**
     * Returns <code>true</code> when an attribute with a given local name
     * and namespace URI is specified on this element or has a default value,
     * <code>false</code> otherwise.
     * Namespace declarations will not be retrieved.
     * @param namespaceURI  The  namespace URI of the attribute to look for.
     * @param localName  The  local name of the attribute to look for.
     * @return <code>true</code> if an attribute with the given local name and
     *   namespace URI is specified or has a default value on this element,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttributeNS(String namespaceURI, String localName) {
		return (Navigator.getAttributeValue(this, namespaceURI, localName) != null);
    }

    // DOM Level 3 methods

    public void setIdAttribute(String name,
                               boolean isId)
                               throws DOMException{
        disallowUpdate();
    }

    public void setIdAttributeNS(String namespaceURI,
                                 String localName,
                                 boolean isId)
                                 throws DOMException{
        disallowUpdate();
    }

    public void setIdAttributeNode(Attr idAttr,
                                   boolean isId)
                                   throws DOMException{
        disallowUpdate();
    }


    /**
     * Get the schema type information for this node. Returns null for an untyped node.
     */

    public TypeInfo getSchemaTypeInfo() {
        int annotation = getTypeAnnotation();
        if (annotation == -1) {
            return null;
        }
        return getDocumentRoot().getConfiguration().getSchemaType(annotation);
            // TODO: what if the node is orphaned?

    }

    ///////////////////////////////////////////////////////////////////
    // Methods defined on the DOM Text and Comment classes
    ///////////////////////////////////////////////////////////////////


    /**
    * Get the character data of a Text or Comment node.
    * DOM method.
    */

    public String getData() {
        return getStringValue();
    }

    /**
    * Set the character data of a Text or Comment node.
    * DOM method: always fails, Saxon tree is immutable.
    */

    public void setData(String data) throws DOMException {
        disallowUpdate();
    }

    /**
    * Get the length of a Text or Comment node.
    * DOM method.
    */

    public int getLength() {
        return getStringValue().length();
    }

    /**
     * Extract a range of data from a Text or Comment node. DOM method.
     * @param offset  Start offset of substring to extract.
     * @param count  The number of 16-bit units to extract.
     * @return  The specified substring. If the sum of <code>offset</code> and
     *   <code>count</code> exceeds the <code>length</code> , then all 16-bit
     *   units to the end of the data are returned.
     * @exception DOMException
     *    INDEX_SIZE_ERR: Raised if the specified <code>offset</code> is
     *   negative or greater than the number of 16-bit units in
     *   <code>data</code> , or if the specified <code>count</code> is
     *   negative.
     */

    public String substringData(int offset, int count) throws DOMException {
        try {
            return getStringValue().substring(offset, offset+count);
        } catch (IndexOutOfBoundsException err2) {
            throw new DOMExceptionImpl(DOMException.INDEX_SIZE_ERR,
                             "substringData: index out of bounds");
        }
    }

    /**
     * Append the string to the end of the character data of the node.
     * DOM method: always fails.
     * @param arg  The <code>DOMString</code> to append.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void appendData(String arg) throws DOMException {
        disallowUpdate();
    }

    /**
     * Insert a string at the specified character offset.
     * DOM method: always fails.
     * @param offset  The character offset at which to insert.
     * @param arg  The <code>DOMString</code> to insert.
     * @exception DOMException
     */

    public void insertData(int offset, String arg) throws DOMException {
        disallowUpdate();
    }

    /**
     * Remove a range of 16-bit units from the node.
     * DOM method: always fails.
     * @param offset  The offset from which to start removing.
     * @param count  The number of 16-bit units to delete.
     * @exception DOMException
     */

    public void deleteData(int offset, int count) throws DOMException {
        disallowUpdate();
    }

    /**
     * Replace the characters starting at the specified 16-bit unit offset
     * with the specified string. DOM method: always fails.
     * @param offset  The offset from which to start replacing.
     * @param count  The number of 16-bit units to replace.
     * @param arg  The <code>DOMString</code> with which the range must be
     *   replaced.
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void replaceData(int offset,
                            int count,
                            String arg) throws DOMException {
        disallowUpdate();
    }


    /**
     * Break this node into two nodes at the specified offset,
     * keeping both in the tree as siblings. DOM method, always fails.
     * @param offset  The 16-bit unit offset at which to split, starting from 0.
     * @return  The new node, of the same type as this node.
     * @exception DOMException
     */

    public Text splitText(int offset)
                          throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Replaces the text of the current node and all logically-adjacent text
     * nodes with the specified text. All logically-adjacent text nodes are
     * removed including the current node unless it was the recipient of the
     * replacement text.
     * <br>This method returns the node which received the replacement text.
     * The returned node is:
     * <ul>
     * <li><code>null</code>, when the replacement text is
     * the empty string;
     * </li>
     * <li>the current node, except when the current node is
     * read-only;
     * </li>
     * <li> a new <code>Text</code> node of the same type (
     * <code>Text</code> or <code>CDATASection</code>) as the current node
     * inserted at the location of the replacement.
     * </li>
     * </ul>
     * <br>For instance, in the above example calling
     * <code>replaceWholeText</code> on the <code>Text</code> node that
     * contains "bar" with "yo" in argument results in the following:
     * <br>Where the nodes to be removed are read-only descendants of an
     * <code>EntityReference</code>, the <code>EntityReference</code> must
     * be removed instead of the read-only nodes. If any
     * <code>EntityReference</code> to be removed has descendants that are
     * not <code>EntityReference</code>, <code>Text</code>, or
     * <code>CDATASection</code> nodes, the <code>replaceWholeText</code>
     * method must fail before performing any modification of the document,
     * raising a <code>DOMException</code> with the code
     * <code>NO_MODIFICATION_ALLOWED_ERR</code>.
     * <br>For instance, in the example below calling
     * <code>replaceWholeText</code> on the <code>Text</code> node that
     * contains "bar" fails, because the <code>EntityReference</code> node
     * "ent" contains an <code>Element</code> node which cannot be removed.
     *
     * @param content The content of the replacing <code>Text</code> node.
     * @return The <code>Text</code> node created with the specified content.
     * @throws org.w3c.dom.DOMException NO_MODIFICATION_ALLOWED_ERR: Raised if one of the <code>Text</code>
     *                                  nodes being replaced is readonly.
     * @since DOM Level 3
     */
    public Text replaceWholeText(String content) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Returns whether this text node contains <a href='http://www.w3.org/TR/2004/REC-xml-infoset-20040204#infoitem.character'>
     * element content whitespace</a>, often abusively called "ignorable whitespace". The text node is
     * determined to contain whitespace in element content during the load
     * of the document or if validation occurs while using
     * <code>Document.normalizeDocument()</code>.
     *
     * @since DOM Level 3
     */
    public boolean isElementContentWhitespace() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns all text of <code>Text</code> nodes logically-adjacent text
     * nodes to this node, concatenated in document order.
     * <br>For instance, in the example below <code>wholeText</code> on the
     * <code>Text</code> node that contains "bar" returns "barfoo", while on
     * the <code>Text</code> node that contains "foo" it returns "barfoo".
     *
     * @since DOM Level 3
     */
    public String getWholeText() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    /////////////////////////////////////////////////////////////////////////
    // Methods to implement the DOM Attr interface
    /////////////////////////////////////////////////////////////////////////

    /**
    * Get the name of an attribute node (the QName) (DOM method)
    */

    public String getName() {
        return getDisplayName();
    }

    /**
    * Return the character value of an attribute node (DOM method)
    * @return the attribute value
    */

    public String getValue() {
        return getStringValue();
    }

    /**
     * If this attribute was explicitly given a value in the original
     * document, this is <code>true</code> ; otherwise, it is
     * <code>false</code>. (DOM method)
     * @return Always true in this implementation.
     */

    public boolean getSpecified() {
        return true;
    }

    /**
    * Set the value of an attribute node. (DOM method).
    * Always fails (because tree is readonly)
    */

    public void setValue(String value) throws DOMException {
        disallowUpdate();
    }

    /**
     * The <code>Element</code> node this attribute is attached to or
     * <code>null</code> if this attribute is not in use.
     * @since DOM Level 2
     */

    public Element getOwnerElement() {
        if (getNodeKind()!=Type.ATTRIBUTE) {
            throw new UnsupportedOperationException(
                        "This method is defined only on attribute nodes");
        }
        return (Element)getParent();
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
