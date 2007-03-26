package org.orbeon.saxon.dom;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.AtomicValue;
import org.w3c.dom.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


/**
  * A node in the XML parse tree representing an XML element, character content, or attribute.<P>
  * This is the implementation of the NodeInfo interface used as a wrapper for DOM nodes.
  */

public class NodeWrapper implements NodeInfo, VirtualNode, SiblingCountingNode {

    protected Node node;
    private int namecode = -1;
    protected short nodeKind;
    private NodeWrapper parent;     // null means unknown
    protected DocumentWrapper docWrapper;
    protected int index;            // -1 means unknown
    protected int span = 1;         // the number of adjacent text nodes wrapped by this NodeWrapper.
                                    // If span>1, node will always be the first of a sequence of adjacent text nodes

    /**
     * This constructor is protected: nodes should be created using the makeWrapper
     * factory method
     * @param node    The DOM node to be wrapped
     * @param parent  The NodeWrapper that wraps the parent of this node
     * @param index   Position of this node among its siblings
     */
    protected NodeWrapper(Node node, NodeWrapper parent, int index) {
        this.node = node;
        this.parent = parent;
        this.index = index;
    }

    /**
     * Factory method to wrap a DOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The DOM node
     * @param docWrapper  The wrapper for the containing Document node
     * @return            The new wrapper for the supplied node
     * @throws NullPointerException if the node or the document wrapper are null
     */
    protected NodeWrapper makeWrapper(Node node, DocumentWrapper docWrapper) {
        if (node == null) {
            throw new NullPointerException("NodeWrapper#makeWrapper: Node must not be null");
        }
        if (docWrapper == null) {
            throw new NullPointerException("NodeWrapper#makeWrapper: DocumentWrapper must not be null");
        }
        return makeWrapper(node, docWrapper, null, -1);
    }

    /**
     * Factory method to wrap a DOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The DOM node
     * @param docWrapper  The wrapper for the containing Document node     *
     * @param parent      The wrapper for the parent of the JDOM node
     * @param index       The position of this node relative to its siblings
     * @return            The new wrapper for the supplied node
     */

    protected NodeWrapper makeWrapper(Node node, DocumentWrapper docWrapper,
                                   NodeWrapper parent, int index) {
        NodeWrapper wrapper;
        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
            return docWrapper;
        case Node.ELEMENT_NODE:
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ELEMENT;
            break;
        case Node.ATTRIBUTE_NODE:
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ATTRIBUTE;
            break;
        case Node.TEXT_NODE:
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.TEXT;
            break;
        case Node.CDATA_SECTION_NODE:
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.TEXT;
            break;
        case Node.COMMENT_NODE:
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.COMMENT;
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.PROCESSING_INSTRUCTION;
            break;
        default:
            throw new IllegalArgumentException("Unsupported node type in DOM! " + node.getNodeType() + " instance " + node.toString());
        }
        wrapper.docWrapper = docWrapper;
        return wrapper;
    }

    /**
    * Get the underlying DOM node, to implement the VirtualNode interface
    */

    public Object getUnderlyingNode() {
        return node;
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return docWrapper.getConfiguration();
    }

    /**
     * Get the name pool for this node
     * @return the NamePool
     */

    public NamePool getNamePool() {
        return docWrapper.getNamePool();
    }

    /**
    * Return the type of node.
    * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
    */

    public int getNodeKind() {
        return nodeKind;
    }

    /**
    * Get the typed value of the item
    */

    public SequenceIterator getTypedValue() {
        return SingletonIterator.makeIterator((AtomicValue)atomize());
    }

    /**
     * Get the typed value. The result of this method will always be consistent with the method
     * {@link org.orbeon.saxon.om.Item#getTypedValue()}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @return the typed value. If requireSingleton is set to true, the result will always be an
     *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
     *         values.
     * @since 8.5
     */

    public Value atomize() {
        switch (getNodeKind()) {
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return new StringValue(getStringValueCS());
            default:
                return new UntypedAtomicValue(getStringValueCS());
        }
    }

    /**
    * Get the type annotation
    */

    public int getTypeAnnotation() {
        if (getNodeKind() == Type.ATTRIBUTE) {
            return StandardNames.XDT_UNTYPED_ATOMIC;
        }
        return StandardNames.XDT_UNTYPED;
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        // DOM does not offer any guarantees that the same node is always represented
        // by the same object

        if (!(other instanceof NodeWrapper)) {
            return false;
        }

        // For a level-3 DOM, use the DOM isSameNode() method
        if (docWrapper.level3) {
            try {
                Class[] argClasses = {Node.class};
                Method isSameNode = Node.class.getMethod("isSameNode", argClasses);
                Object[] args = {((NodeWrapper)other).node};
                Boolean b = (Boolean)(isSameNode.invoke(node, args));
                return b.booleanValue();
            } catch (NoSuchMethodException e) {
                // use fallback implementation
            } catch (IllegalAccessException e) {
                // use fallback implementation
            } catch (InvocationTargetException e) {
                // use fallback implementation
            } catch (AbstractMethodError e) {
                // use fallback implementation
            }
        }
        NodeWrapper ow = (NodeWrapper)other;
        return getNodeKind()==ow.getNodeKind() &&
                getNameCode()==ow.getNameCode() &&  // redundant, but gives a quick exit
                getSiblingPosition()==ow.getSiblingPosition() &&
                getParent().isSameNodeInfo(ow.getParent());
        // Note: this has been known to fail because getParent() returns null. Extra checks have been added to the
        // methods for constructing DOM document wrappers to ensure that wrapped nodes always belong to a wrapped
        // Document, which should prevent this happening again.
    }

    /**
      * The equals() method compares nodes for identity. It is defined to give the same result
      * as isSameNodeInfo().
      * @param other the node to be compared with this node
      * @return true if this NodeInfo object and the supplied NodeInfo object represent
      *      the same node in the tree.
      * @since 8.7 Previously, the effect of the equals() method was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics. It is safer to use isSameNodeInfo() for this reason.
      * The equals() method has been defined because it is useful in contexts such as a Java Set or HashMap.
      */

     public boolean equals(Object other) {
        if (other instanceof NodeInfo) {
            return isSameNodeInfo((NodeInfo)other);
        } else {
            return false;
        }
    }

     /**
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

     public int hashCode() {
         FastStringBuffer buffer = new FastStringBuffer(20);
         generateId(buffer);
         return buffer.toString().hashCode();
     }

    /**
    * Get the System ID for the node.
    * @return the System Identifier of the entity in the source document containing the node,
    * or null if not known. Note this is not the same as the base URI: the base URI can be
    * modified by xml:base, but the system ID cannot.
    */

    public String getSystemId() {
        return docWrapper.baseURI;
    }

    public void setSystemId(String uri) {
        docWrapper.baseURI = uri;
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. In the DOM model, base URIs are held only an the document level.
    */

    public String getBaseURI() {
        NodeInfo n = this;
        if (getNodeKind() != Type.ELEMENT) {
            n = getParent();
        }
        // Look for an xml:base attribute
        while (n != null) {
            String xmlbase = n.getAttributeValue(StandardNames.XML_BASE);
            if (xmlbase != null) {
                return xmlbase;
            }
            n = n.getParent();
        }
        // if not found, return the base URI of the document node
        return docWrapper.baseURI;
    }

    /**
    * Get line number
    * @return the line number of the node in its original source document; or -1 if not available
    */

    public int getLineNumber() {
        return -1;
    }

    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public int compareOrder(NodeInfo other) {
        // Use the DOM Level-3 compareDocumentPosition() method if available
        if (docWrapper.level3 && other instanceof NodeWrapper) {
            if (isSameNodeInfo(other)) {
                return 0;
            }
            try {
                Class[] argClasses = {Node.class};
                Method compareDocumentPosition = Node.class.getMethod("compareDocumentPosition", argClasses);
                Object[] args = {((NodeWrapper)other).node};
                Short i = (Short)(compareDocumentPosition.invoke(node, args));
                switch (i.shortValue()) {
                                // the symbolic constants require JDK 1.5
                    case 2: //Node.DOCUMENT_POSITION_PRECEDING:
                    case 8: //Node.DOCUMENT_POSITION_CONTAINS:
                        return +1;
                    case 4: //Node.DOCUMENT_POSITION_FOLLOWING:
                    case 16: //Node.DOCUMENT_POSITION_CONTAINED_BY:
                        return -1;
                    default:
                        // use fallback implementation
                }
            } catch (NoSuchMethodException e) {
                // use fallback implementation
            } catch (IllegalAccessException e) {
                // use fallback implementation
            } catch (InvocationTargetException e) {
                // use fallback implementation
            } catch (AbstractMethodError e) {
                // use fallback implementation
            } catch (DOMException e) {
                // use fallback implementation
            }
        }

        if (other instanceof SiblingCountingNode) {
            return Navigator.compareOrder(this, (SiblingCountingNode)other);
        } else {
            // it's presumably a Namespace Node
            return -other.compareOrder(this);
        }
    }

    /**
    * Return the string value of the node. The interpretation of this depends on the type
    * of node. For an element it is the accumulated character content of the element,
    * including descendant elements.
    * @return the string value of the node
    */

    public String getStringValue() {
        return getStringValueCS().toString();

    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        //return getStringValue(node, nodeKind);
        switch (nodeKind) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                NodeList children1 = node.getChildNodes();
                StringBuffer sb1 = new StringBuffer(16);
                expandStringValue(children1, sb1);
                return sb1;

            case Type.ATTRIBUTE:
                return ((Attr)node).getValue();

            case Type.TEXT:
                if (span == 1) {
                    return node.getNodeValue();
                } else {
                    FastStringBuffer fsb = new FastStringBuffer(100);
                    Node textNode = node;
                    for (int i=0; i<span; i++) {
                        fsb.append(textNode.getNodeValue());
                        textNode = textNode.getNextSibling();
                    }
                    return fsb.condense();
                }

            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return node.getNodeValue();

            default:
                return "";
        }
    }

    /**
     * Get the string value of a DOM node
     * @param node
     * @param nodeKind
     * @return
     */

    private static CharSequence getStringValue(Node node, int nodeKind) {
        switch (nodeKind) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                NodeList children1 = node.getChildNodes();
                StringBuffer sb1 = new StringBuffer(16);
                expandStringValue(children1, sb1);
                return sb1;

            case Type.ATTRIBUTE:
                return ((Attr)node).getValue();

            case Type.TEXT:
            case Node.CDATA_SECTION_NODE:
                return node.getNodeValue();

            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return node.getNodeValue();

            default:
                return "";
        }
    }

    private static void expandStringValue(NodeList list, StringBuffer sb) {
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    expandStringValue(child.getChildNodes(), sb);
                    break;
                case Node.COMMENT_NODE:
                case Node.PROCESSING_INSTRUCTION_NODE:
                    break;
                default:
                    sb.append(child.getNodeValue());
            }
        }
    }

	/**
	* Get name code. The name code is a coded form of the node name: two nodes
	* with the same name code have the same namespace URI, the same local name,
	* and the same prefix. By masking the name code with &0xfffff, you get a
	* fingerprint: two nodes with the same fingerprint have the same local name
	* and namespace URI.
    * @see NamePool#allocate allocate
	*/

	public int getNameCode() {
        if (namecode != -1) {
            // this is a memo function
            return namecode;
        }
        int nodeKind = getNodeKind();
        if (nodeKind == Type.ELEMENT || nodeKind == Type.ATTRIBUTE) {
            String prefix = node.getPrefix();
            if (prefix==null) {
                prefix = "";
            }
            namecode = docWrapper.getNamePool().allocate(prefix, getURI(), getLocalPart());
            return namecode;
        } else if (nodeKind == Type.PROCESSING_INSTRUCTION ) {
            namecode = docWrapper.getNamePool().allocate("", "", getLocalPart());
            return namecode;
        } else {
            return -1;
        }
	}

	/**
	* Get fingerprint. The fingerprint is a coded form of the expanded name
	* of the node: two nodes
	* with the same name code have the same namespace URI and the same local name.
	* A fingerprint of -1 should be returned for a node with no name.
	*/

	public int getFingerprint() {
        int nc = getNameCode();
        if (nc == -1) {
            return -1;
        }
	    return nc&0xfffff;
	}

    /**
    * Get the local part of the name of this node. This is the name after the ":" if any.
    * @return the local part of the name. For an unnamed node, returns null, except for
     * un unnamed namespace node, which returns "".
    */

    public String getLocalPart() {
        String s = node.getLocalName();
        if (s == null) {
            // Crimson returns null for the attribute "xml:space": test axes-dom132
            String n = getDisplayName();
            int colon = n.indexOf(':');
            if (colon >= 0) {
                return n.substring(colon+1);
            }
            return n;
        } else {
            return s;
        }
    }

    /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
     * @return The URI of the namespace of this node. For an unnamed node,
     *     or for a node with an empty prefix, return an empty
     *     string.
    */

    public String getURI() {
        NodeInfo element;
        if (nodeKind == Type.ELEMENT) {
            element = this;
        } else if (nodeKind == Type.ATTRIBUTE) {
            element = parent;
        } else {
            return "";
        }

        // The DOM methods getPrefix() and getNamespaceURI() do not always
        // return the prefix and the URI; they both return null, unless the
        // prefix and URI have been explicitly set in the node by using DOM
        // level 2 interfaces. There's no obvious way of deciding whether
        // an element whose name has no prefix is in the default namespace,
        // other than searching for a default namespace declaration. So we have to
        // be prepared to search.

        // If getPrefix() and getNamespaceURI() are non-null, however,
        // we can use the values.

        String uri = node.getNamespaceURI();
        if (uri != null) {
            return uri;
        }

        // Otherwise we have to work it out the hard way...

        if (node.getNodeName().startsWith("xml:")) {
            return NamespaceConstant.XML;
        }

        String[] parts;
        try {
            parts = Name11Checker.getInstance().getQNameParts(node.getNodeName());
            // use the XML 1.1 rules: these will do because it should already have been checked
        } catch (QNameException e) {
            throw new IllegalStateException("Invalid QName in DOM node. " + e);
        }

        if (nodeKind == Type.ATTRIBUTE && parts[0].equals("")) {
            // for an attribute, no prefix means no namespace
            uri = "";
        } else {
            AxisIterator nsiter = element.iterateAxis(Axis.NAMESPACE);
            while (true) {
                NodeInfo ns = (NodeInfo)nsiter.next();
                if (ns == null) break;
                if (ns.getLocalPart().equals(parts[0])) {
                    uri = ns.getStringValue();
                    break;
                }
            }
            if (uri == null) {
                if (parts[0].equals("")) {
                    uri = "";
                } else {
                    throw new IllegalStateException("Undeclared namespace prefix in DOM input: " + parts[0]);
                }
            }
        }
        return uri;
    }

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     * This implementation simply returns the prefix defined in the DOM model; this is nto strictly
     * accurate in all cases, but is good enough for the purpose.
     * @return The prefix of the name of the node.
     */

    public String getPrefix() {
        return node.getPrefix();
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        switch (nodeKind) {
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
            case Type.PROCESSING_INSTRUCTION:
                return node.getNodeName();
            default:
                return "";

        }
    }

    /**
    * Get the NodeInfo object representing the parent of this node
    */

    public NodeInfo getParent() {
        if (parent==null) {
            switch (getNodeKind()) {
            case Type.ATTRIBUTE:
                parent = makeWrapper(((Attr)node).getOwnerElement(), docWrapper);
                break;
            default:
                Node p = node.getParentNode();
                if (p==null) {
                    return null;
                } else {
                    parent = makeWrapper(p, docWrapper);
                }
            }
        }
        return parent;
    }

    /**
     * Get the index position of this node among its siblings (starting from 0).
     * In the case of a text node that maps to several adjacent siblings in the DOM,
     * the numbering actually refers to the position of the underlying DOM nodes;
     * thus the sibling position for the text node is that of the first DOM node
     * to which it relates, and the numbering of subsequent XPath nodes is not necessarily
     * consecutive.
     */

    public int getSiblingPosition() {
        if (index == -1) {
            switch (nodeKind) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    int ix = 0;
                    Node start = node;
                    while (true) {
                        start = start.getPreviousSibling();
                        if (start == null) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }
                case Type.ATTRIBUTE:
                    ix = 0;
                    int fp = getFingerprint();
                    AxisIterator iter = parent.iterateAxis(Axis.ATTRIBUTE);
                    while (true) {
                        NodeInfo n = (NodeInfo)iter.next();
                        if (n==null || n.getFingerprint()==fp) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }

                case Type.NAMESPACE:
                    ix = 0;
                    fp = getFingerprint();
                    iter = parent.iterateAxis(Axis.NAMESPACE);
                    while (true) {
                        NodeInfo n = (NodeInfo)iter.next();
                        if (n==null || n.getFingerprint()==fp) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }
                default:
                    index = 0;
                    return index;
            }
        }
        return index;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
                if (nodeKind==Type.DOCUMENT) return EmptyIterator.getInstance();
                return new Navigator.AncestorEnumeration(this, false);

            case Axis.ANCESTOR_OR_SELF:
                if (nodeKind==Type.DOCUMENT) return SingletonIterator.makeIterator(this);
                return new Navigator.AncestorEnumeration(this, true);

            case Axis.ATTRIBUTE:
                if (nodeKind!=Type.ELEMENT) return EmptyIterator.getInstance();
                return new AttributeEnumeration(this);

            case Axis.CHILD:
                if (hasChildNodes()) {
                    return new ChildEnumeration(this, true, true);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT:
                if (hasChildNodes()) {
                    return new Navigator.DescendantEnumeration(this, false, true);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                 return new Navigator.DescendantEnumeration(this, true, true);

            case Axis.FOLLOWING:
                 return new Navigator.FollowingEnumeration(this);

            case Axis.FOLLOWING_SIBLING:
                 switch (nodeKind) {
                    case Type.DOCUMENT:
                    case Type.ATTRIBUTE:
                    case Type.NAMESPACE:
                        return EmptyIterator.getInstance();
                    default:
                        return new ChildEnumeration(this, false, true);
                 }

            case Axis.NAMESPACE:
                 if (nodeKind!=Type.ELEMENT) {
                     return EmptyIterator.getInstance();
                 }
                 return new NamespaceIterator(this, null);
                 //return new NamespaceEnumeration(this);

            case Axis.PARENT:
                 getParent();
                 return SingletonIterator.makeIterator(parent);

            case Axis.PRECEDING:
                 return new Navigator.PrecedingEnumeration(this, false);

            case Axis.PRECEDING_SIBLING:
                 switch (nodeKind) {
                    case Type.DOCUMENT:
                    case Type.ATTRIBUTE:
                    case Type.NAMESPACE:
                        return EmptyIterator.getInstance();
                    default:
                        return new ChildEnumeration(this, false, false);
                 }

            case Axis.SELF:
                 return SingletonIterator.makeIterator(this);

            case Axis.PRECEDING_OR_ANCESTOR:
                 return new Navigator.PrecedingEnumeration(this, true);

            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        return new Navigator.AxisFilter(iterateAxis(axisNumber), nodeTest);
    }

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
        NameTest test = new NameTest(Type.ATTRIBUTE, fingerprint, getNamePool());
        AxisIterator iterator = iterateAxis(Axis.ATTRIBUTE, test);
        NodeInfo attribute = (NodeInfo)iterator.next();
        if (attribute == null) {
            return null;
        } else {
            return attribute.getStringValue();
        }
    }

    /**
    * Get the root node - always a document node with this tree implementation
    * @return the NodeInfo representing the containing document
    */

    public NodeInfo getRoot() {
        return docWrapper;
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document
    */

    public DocumentInfo getDocumentRoot() {
        return docWrapper;
    }

    /**
    * Determine whether the node has any children. <br />
    * Note: the result is equivalent to <br />
    * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
    */

    public boolean hasChildNodes() {
        // In Xerces, an attribute node has child text nodes
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            return false;
        }
        return node.hasChildNodes();
    }

    /**
    * Get a character string that uniquely identifies this node.
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @param buffer a buffer to contain a string that uniquely identifies this node, across all
    * documents
     *
     */

    public void generateId(FastStringBuffer buffer) {
        Navigator.appendSequentialKey(this, buffer, true);
        //buffer.append(Navigator.getSequentialKey(this));
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return getDocumentRoot().getDocumentNumber();
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        Navigator.copy(this, out, docWrapper.getNamePool(), whichNamespaces, copyAnnotations, locationId);
    }

    /**
    * Output all namespace nodes associated with this element. Does nothing if
    * the node is not an element.
    * @param out The relevant outputter
     * @param includeAncestors True if namespaces declared on ancestor elements must
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
        throws XPathException {
        Navigator.sendNamespaceDeclarations(this, out, includeAncestors);
    }

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public int[] getDeclaredNamespaces(int[] buffer) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element)node;
            NamedNodeMap atts = elem.getAttributes();

            if (atts == null) {
                return EMPTY_NAMESPACE_LIST;
            }
            int count = 0;
            for (int i=0; i<atts.getLength(); i++) {
                Attr att = (Attr)atts.item(i);
                String attName = att.getName();
                if (attName.equals("xmlns")) {
                    count++;
                } else if (attName.startsWith("xmlns:")) {
                    count++;
                }
            }
            if (count == 0) {
                return EMPTY_NAMESPACE_LIST;
            } else {
                int[] result = (count > buffer.length ? new int[count] : buffer);
                NamePool pool = getNamePool();
                int n = 0;
                for (int i=0; i<atts.getLength(); i++) {
                    Attr att = (Attr)atts.item(i);
                    String attName = att.getName();
                    if (attName.equals("xmlns")) {
                        String prefix = "";
                        String uri = att.getValue();
                        result[n++] = pool.allocateNamespaceCode(prefix, uri);
                    } else if (attName.startsWith("xmlns:")) {
                        String prefix = attName.substring(6);
                        String uri = att.getValue();
                        result[n++] = pool.allocateNamespaceCode(prefix, uri);
                    }
                }
                if (count < result.length) {
                    result[count] = -1;
                }
                return result;
            }
        } else {
            return null;
        }
    }


    private final class AttributeEnumeration implements AxisIterator, LookaheadIterator {

        private ArrayList attList = new ArrayList(10);
        private int ix = 0;
        private NodeWrapper start;
        private NodeWrapper current;

        public AttributeEnumeration(NodeWrapper start) {
            this.start = start;
            NamedNodeMap atts = start.node.getAttributes();
            if (atts != null) {
                for (int i=0; i<atts.getLength(); i++) {
                    String name = atts.item(i).getNodeName();
                    if (!(name.startsWith("xmlns") &&
                            (name.length() == 5 || name.charAt(5) == ':'))) {
                        attList.add(atts.item(i));
                    }
                }
            }
            ix = 0;
        }

        public boolean hasNext() {
            return ix < attList.size();
        }

        public Item next() {
            if (ix >= attList.size()) {
                return null;
            }
            current = start.makeWrapper(
                    (Attr)attList.get(ix), docWrapper, start, ix);
            ix++;
            return current;
        }

        public Item current() {
            return current;
        }

        public int position() {
            return ix+1;
        }

        public SequenceIterator getAnother() {
            return new AttributeEnumeration(start);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link GROUNDED}, {@link LAST_POSITION_FINDER},
         *         and {@link LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return LOOKAHEAD;
        }
    }


    /**
    * The class ChildEnumeration handles not only the child axis, but also the
    * following-sibling and preceding-sibling axes. It can also iterate the children
    * of the start node in reverse order, something that is needed to support the
    * preceding and preceding-or-ancestor axes (the latter being used by xsl:number)
    */

    private final class ChildEnumeration extends AxisIteratorImpl implements LookaheadIterator{

        private NodeWrapper start;
        private NodeWrapper commonParent;
        private ArrayList items = new ArrayList(20);
        private int ix = 0;
        private boolean downwards;  // iterate children of start node (not siblings)
        private boolean forwards;   // iterate in document order (not reverse order)

        public ChildEnumeration(NodeWrapper start,
                                boolean downwards, boolean forwards)  {
            this.start = start;
            this.downwards = downwards;
            this.forwards = forwards;
            position = 0;

            if (downwards) {
                commonParent = start;
            } else {
                commonParent = (NodeWrapper)start.getParent();
            }

            NodeList childNodes = commonParent.node.getChildNodes();
            if (downwards) {
                if (!forwards) {
                    // backwards enumeration: go to the end
                    ix = childNodes.getLength() - 1;
                }
            } else {
                ix = start.getSiblingPosition() + (forwards ? span : -1);
            }

            if (forwards) {
                boolean previousText = false;
                for (int i=ix; i<childNodes.getLength(); i++) {
                    boolean thisText = false;
                    Node node = childNodes.item(i);
                    switch (node.getNodeType()) {
                        case Node.DOCUMENT_TYPE_NODE:
                            break;
                        case Node.TEXT_NODE:
                        case Node.CDATA_SECTION_NODE:
                            thisText = true;
                            if (previousText) {
                                if (isAtomizing()) {
                                    UntypedAtomicValue old = (UntypedAtomicValue)(items.get(items.size()-1));
                                    String newval = old.getStringValue() + getStringValue(node, node.getNodeType());
                                    items.set(items.size()-1, new UntypedAtomicValue(newval));
                                } else {
                                    NodeWrapper old = ((NodeWrapper)items.get(items.size()-1));
                                    old.span++;
                                }
                                break;
                            }
                            // otherwise fall through to default case
                        default:
                            previousText = thisText;
                            if (isAtomizing()) {
                                items.add(new UntypedAtomicValue(
                                        getStringValue(node, node.getNodeType())));
                            } else {
                                items.add(makeWrapper(node, docWrapper, commonParent, i));
                            }
                    }
                }
            } else {
                boolean previousText = false;
                for (int i=ix; i>=0; i--) {
                    boolean thisText = false;
                    Node node = childNodes.item(i);
                    switch (node.getNodeType()) {
                        case Node.DOCUMENT_TYPE_NODE:
                            break;
                        case Node.TEXT_NODE:
                        case Node.CDATA_SECTION_NODE:
                            thisText = true;
                            if (previousText) {
                                if (isAtomizing()) {
                                    UntypedAtomicValue old = (UntypedAtomicValue)(items.get(items.size()-1));
                                    String newval = old.getStringValue() + getStringValue(node, node.getNodeType());
                                    items.set(items.size()-1, new UntypedAtomicValue(newval));
                                } else {
                                    NodeWrapper old = ((NodeWrapper)items.get(items.size()-1));
                                    old.node = node;
                                    old.span++;
                                }
                                break;
                            }
                            // otherwise fall through to default case
                        default:
                            previousText = thisText;
                            if (isAtomizing()) {
                                items.add(new UntypedAtomicValue(
                                        getStringValue(node, node.getNodeType())));
                            } else {
                                items.add(makeWrapper(node, docWrapper, commonParent, i));
                            }
                    }
                }
            }
        }

        public boolean hasNext() {
            return position < items.size();
        }

        public Item next() {
            if (position < items.size()) {
                current = (Item)items.get(position++);
                return current;
            } else {
                return null;
            }
        }

        public SequenceIterator getAnother() {
            return new ChildEnumeration(start, downwards, forwards);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link GROUNDED}, {@link LAST_POSITION_FINDER},
         *         and {@link LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return LOOKAHEAD;
        }

    } // end of class ChildEnumeration


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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
