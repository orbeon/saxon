package net.sf.saxon.jdom;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;
import org.jdom.*;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
  * A node in the XML parse tree representing an XML element, character content, or attribute.<P>
  * This is the implementation of the NodeInfo interface used as a wrapper for JDOM nodes.
  * @author Michael H. Kay
  */

public class NodeWrapper implements NodeInfo, VirtualNode, SiblingCountingNode {

    protected Object node;
    protected short nodeKind;
    private NodeWrapper parent;     // null means unknown
    protected DocumentWrapper docWrapper;
    protected int index;            // -1 means unknown

    /**
     * This constructor is protected: nodes should be created using the wrap
     * factory method on the DocumentWrapper class
     * @param node    The JDOM node to be wrapped
     * @param parent  The NodeWrapper that wraps the parent of this node
     * @param index   Position of this node among its siblings
     */
    protected NodeWrapper(Object node, NodeWrapper parent, int index) {
        this.node = node;
        this.parent = parent;
        this.index = index;
    }

    /**
     * Factory method to wrap a JDOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The JDOM node
     * @param docWrapper  The wrapper for the Document containing this node
     * @return            The new wrapper for the supplied node
     */
    protected NodeWrapper makeWrapper(Object node, DocumentWrapper docWrapper) {
        return makeWrapper(node, docWrapper, null, -1);
    }

    /**
     * Factory method to wrap a JDOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The JDOM node
     * @param docWrapper  The wrapper for the Document containing this node
     * @param parent      The wrapper for the parent of the JDOM node
     * @param index       The position of this node relative to its siblings
     * @return            The new wrapper for the supplied node
     */

    protected NodeWrapper makeWrapper(Object node, DocumentWrapper docWrapper,
                                      NodeWrapper parent, int index) {
        NodeWrapper wrapper;
        if (node instanceof Document) {
            return docWrapper;
        } else if (node instanceof Element) {
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ELEMENT;
        } else if (node instanceof Attribute) {
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.ATTRIBUTE;
        } else if (node instanceof String || node instanceof Text) {
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.TEXT;
        } else if (node instanceof Comment) {
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.COMMENT;
        } else if (node instanceof ProcessingInstruction) {
            wrapper = new NodeWrapper(node, parent, index);
            wrapper.nodeKind = Type.PROCESSING_INSTRUCTION;
        } else if (node instanceof Namespace) {
            throw new IllegalArgumentException("Cannot wrap JDOM namespace objects");
            //wrapper = new NodeWrapper(node, parent, index);
            //wrapper.nodeKind = Type.NAMESPACE;
        } else {
            throw new IllegalArgumentException("Bad node type in JDOM! " + node.getClass() + " instance " + node.toString());
        }
        wrapper.docWrapper = docWrapper;
        return wrapper;
    }

    /**
    * Get the underlying JDOM node, to implement the VirtualNode interface
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
        return SingletonIterator.makeIterator(new UntypedAtomicValue(getStringValueCS()));
    }

    /**
    * Get the type annotation
    * @return -1 (there is no type annotation)
    */

    public int getTypeAnnotation() {
        return -1;
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof NodeWrapper)) {
            return false;
        }
        NodeWrapper ow = (NodeWrapper)other;
//        if (node instanceof Namespace) {
//            return this.getLocalPart().equals(ow.getLocalPart()) && this.getParent().isSameNodeInfo(ow.getParent());
//        }
        return node.equals(ow.node);
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
     * in the node. In the JDOM model, base URIs are held only an the document level.
    */

    public String getBaseURI() {
        if (getNodeKind() == Type.NAMESPACE) {
            return null;
        }
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
    * @return the line number of the node in its original source document; or -1 if not available.
     * Always returns -1 in this implementation.
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
        if (other instanceof SiblingCountingNode) {
            return Navigator.compareOrder(this, (SiblingCountingNode)other);
        } else {
            // it must be a namespace node
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
        return getStringValue(node);
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return getStringValue(node);
    }

    /**
     * Supporting method to get the string value of a node
     */

    private static String getStringValue(Object node) {
        if (node instanceof Document) {
            List children1 = ((Document)node).getContent();
            FastStringBuffer sb1 = new FastStringBuffer(2048);
            expandStringValue(children1, sb1);
            return sb1.toString();
        } else if (node instanceof Element) {
            return ((Element)node).getValue();
        } else if (node instanceof Attribute) {
            return ((Attribute)node).getValue();
        } else if (node instanceof Text) {
            return ((Text)node).getText();
        } else if (node instanceof String) {
            return (String)node;
        } else if (node instanceof Comment) {
            return ((Comment)node).getText();
        } else if (node instanceof ProcessingInstruction) {
            return ((ProcessingInstruction)node).getData();
        } else if (node instanceof Namespace) {
            return ((Namespace)node).getURI();
        } else {
            return "";
        }
    }

    /**
     * Get the string values of all the nodes in a list, concatenating the values into
     * a supplied string buffer
     * @param list the list containing the nodes
     * @param sb the StringBuffer to contain the result
     */
    private static void expandStringValue(List list, FastStringBuffer sb) {
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof Element) {
                sb.append(((Element)obj).getValue());
            } else if (obj instanceof Text) {
                sb.append(((Text)obj).getText());
            } else if (obj instanceof EntityRef) {
                throw new IllegalStateException("Unexpanded entity in JDOM tree");
            } else if (obj instanceof DocType) {
                // do nothing: can happen in JDOM beta 10
            } else {
                throw new AssertionError("Unknown JDOM node type");
            }
        }
    }

	/**
	* Get name code. The name code is a coded form of the node name: two nodes
	* with the same name code have the same namespace URI, the same local name,
	* and the same prefix. By masking the name code with &0xfffff, you get a
	* fingerprint: two nodes with the same fingerprint have the same local name
	* and namespace URI.
    * @see net.sf.saxon.om.NamePool#allocate allocate
	*/

	public int getNameCode() {
	    switch (nodeKind) {
	        case Type.ELEMENT:
	        case Type.ATTRIBUTE:
	        case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
	            return docWrapper.getNamePool().allocate(getPrefix(),
	                                                getURI(),
	                                                getLocalPart());
	        default:
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
        } else {
	        return nc&0xfffff;
        }
	}

    /**
    * Get the local part of the name of this node. This is the name after the ":" if any.
    * @return the local part of the name. For an unnamed node, returns "".
    */

    public String getLocalPart() {
        switch (nodeKind) {
            case Type.ELEMENT:
                return ((Element)node).getName();
            case Type.ATTRIBUTE:
                return ((Attribute)node).getName();
            case Type.TEXT:
            case Type.COMMENT:
            case Type.DOCUMENT:
                return "";
            case Type.PROCESSING_INSTRUCTION:
                return ((ProcessingInstruction)node).getTarget();
            case Type.NAMESPACE:
                return ((Namespace)node).getPrefix();
            default:
                return null;
        }
    }

    /**
    * Get the prefix part of the name of this node. This is the name before the ":" if any.
     * (Note, this method isn't required as part of the NodeInfo interface.)
    * @return the prefix part of the name. For an unnamed node, return an empty string.
    */

    public String getPrefix() {
        switch (nodeKind) {
            case Type.ELEMENT:
                return ((Element)node).getNamespacePrefix();
            case Type.ATTRIBUTE:
                return ((Attribute)node).getNamespacePrefix();
            default:
                return "";
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
        switch (nodeKind) {
            case Type.ELEMENT:
                return ((Element)node).getNamespaceURI();
            case Type.ATTRIBUTE:
                return ((Attribute)node).getNamespaceURI();
            default:
                return "";
        }
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
                return ((Element)node).getQualifiedName();
            case Type.ATTRIBUTE:
                return ((Attribute)node).getQualifiedName();
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
                return getLocalPart();
            default:
                return "";

        }
    }

    /**
    * Get the NodeInfo object representing the parent of this node
    */

    public NodeInfo getParent() {
        if (parent==null) {
            if (node instanceof Element) {
                if (((Element)node).isRootElement()) {
                    parent = makeWrapper(((Element)node).getDocument(), docWrapper);
                } else {
                    parent = makeWrapper(((Element)node).getParent(), docWrapper);
                }
            } else if (node instanceof Text) {
                parent = makeWrapper(((Text)node).getParent(), docWrapper);
            } else if (node instanceof Comment) {
                parent = makeWrapper(((Comment)node).getParent(), docWrapper);
            } else if (node instanceof ProcessingInstruction) {
                parent = makeWrapper(((ProcessingInstruction)node).getParent(), docWrapper);
            } else if (node instanceof Attribute) {
                parent = makeWrapper(((Attribute)node).getParent(), docWrapper);
            } else if (node instanceof Document) {
                parent = null;
            } else if (node instanceof Namespace) {
                throw new UnsupportedOperationException("Cannot find parent of JDOM namespace node");
            } else {
                throw new IllegalStateException("Unknown JDOM node type " + node.getClass());
            }
        }
        return parent;
    }

    /**
     * Get the index position of this node among its siblings (starting from 0)
     */

    public int getSiblingPosition() {
        if (index == -1) {
            int ix = 0;
            getParent();
            AxisIterator iter;
            switch (nodeKind) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    iter = parent.iterateAxis(Axis.CHILD);
                    break;
                case Type.ATTRIBUTE:
                    iter = parent.iterateAxis(Axis.ATTRIBUTE);
                    break;
                case Type.NAMESPACE:
                    iter = parent.iterateAxis(Axis.NAMESPACE);
                    break;
                default:
                    index = 0;
                    return index;
            }
            while (true) {
                NodeInfo n = (NodeInfo)iter.next();
                if (n == null) {
                    break;
                }
                if (n.isSameNodeInfo(this)) {
                    index = ix;
                    return index;
                }
                ix++;
            }
            throw new IllegalStateException("JDOM node not linked to parent node");
        }
        return index;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber) {
        return iterateAxis(axisNumber, AnyNodeTest.getInstance());
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
                if (nodeKind==Type.DOCUMENT) return EmptyIterator.getInstance();
                return new Navigator.AxisFilter(
                            new Navigator.AncestorEnumeration(this, false),
                            nodeTest);

            case Axis.ANCESTOR_OR_SELF:
                if (nodeKind==Type.DOCUMENT) return EmptyIterator.getInstance();
                return new Navigator.AxisFilter(
                            new Navigator.AncestorEnumeration(this, true),
                            nodeTest);

            case Axis.ATTRIBUTE:
                if (nodeKind!=Type.ELEMENT) return EmptyIterator.getInstance();
                return new Navigator.AxisFilter(
                            new AttributeEnumeration(this),
                            nodeTest);

            case Axis.CHILD:
                if (hasChildNodes()) {
                    return new Navigator.AxisFilter(
                            new ChildEnumeration(this, true, true),
                            nodeTest);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT:
                if (hasChildNodes()) {
                    return new Navigator.AxisFilter(
                            new Navigator.DescendantEnumeration(this, false, true),
                            nodeTest);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                 return new Navigator.AxisFilter(
                            new Navigator.DescendantEnumeration(this, true, true),
                            nodeTest);

            case Axis.FOLLOWING:
                 return new Navigator.AxisFilter(
                            new Navigator.FollowingEnumeration(this),
                            nodeTest);

            case Axis.FOLLOWING_SIBLING:
                 switch (nodeKind) {
                    case Type.DOCUMENT:
                    case Type.ATTRIBUTE:
                    case Type.NAMESPACE:
                        return EmptyIterator.getInstance();
                    default:
                        return new Navigator.AxisFilter(
                            new ChildEnumeration(this, false, true),
                            nodeTest);
                 }

            case Axis.NAMESPACE:
                 if (nodeKind!=Type.ELEMENT) {
                     return EmptyIterator.getInstance();
                 }
                 return new NamespaceIterator(this, nodeTest);
//                 return new Navigator.AxisFilter(
//                                new NamespaceEnumeration(this),
//                                nodeTest);

            case Axis.PARENT:
                 getParent();
                 if (parent==null) return EmptyIterator.getInstance();
                 if (nodeTest.matches(parent)) {
                    return SingletonIterator.makeIterator(parent);
                 }
                 return EmptyIterator.getInstance();

            case Axis.PRECEDING:
                 return new Navigator.AxisFilter(
                            new Navigator.PrecedingEnumeration(this, false),
                            nodeTest);

            case Axis.PRECEDING_SIBLING:
                 switch (nodeKind) {
                    case Type.DOCUMENT:
                    case Type.ATTRIBUTE:
                    case Type.NAMESPACE:
                        return EmptyIterator.getInstance();
                    default:
                        return new Navigator.AxisFilter(
                            new ChildEnumeration(this, false, false),
                            nodeTest);
                 }

            case Axis.SELF:
                 if (nodeTest.matches(this)) {
                     return SingletonIterator.makeIterator(this);
                 }
                 return EmptyIterator.getInstance();

            case Axis.PRECEDING_OR_ANCESTOR:
                 return new Navigator.AxisFilter(
                            new Navigator.PrecedingEnumeration(this, true),
                            nodeTest);

            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists, or null if not
    */

    public String getAttributeValue(int fingerprint) {
        if (nodeKind==Type.ELEMENT) {
            NamePool pool = docWrapper.getNamePool();
            String uri = pool.getURI(fingerprint);
            String local = pool.getLocalName(fingerprint);
            return ((Element)node).getAttributeValue(local,
                    (   uri.equals(NamespaceConstant.XML) ?
                            Namespace.XML_NAMESPACE :
                            Namespace.getNamespace(uri)));
                // JDOM doesn't allow getNamespace() on the XML namespace URI
        }
        return null;
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
        switch (nodeKind) {
            case Type.DOCUMENT:
                return true;
            case Type.ELEMENT:
                return !((Element)node).getContent().isEmpty();
            default:
                return false;
        }
    }
    /**
    * Get a character string that uniquely identifies this node.
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return a string that uniquely identifies this node, across all
    * documents
    */

    public String generateId() {
        return Navigator.getSequentialKey(this);
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
        if (node instanceof Element) {
            Element elem = (Element)node;
            List addl = elem.getAdditionalNamespaces();
            int size = addl.size() + 1;
            int[] result = (size <= buffer.length ? buffer : new int[size]);
            NamePool pool = getNamePool();
            Namespace ns = elem.getNamespace();
            String prefix = ns.getPrefix();
            String uri = ns.getURI();
            result[0] = pool.allocateNamespaceCode(prefix, uri);
            int i = 1;
            if (addl.size() > 0) {
                Iterator itr = addl.iterator();
                while (itr.hasNext()) {
                    ns = (Namespace) itr.next();
                    result[i++] = pool.allocateNamespaceCode(ns.getPrefix(), ns.getURI());
                }
            }
            if (size < buffer.length) {
                result[size] = -1;
            }
            return result;
        } else {
            return null;
        }
    }
    ///////////////////////////////////////////////////////////////////////////////
    // Axis enumeration classes
    ///////////////////////////////////////////////////////////////////////////////


    private final class AttributeEnumeration extends Navigator.BaseEnumeration {

        private Iterator atts;
        private int ix = 0;
        private NodeWrapper start;

        public AttributeEnumeration(NodeWrapper start) {
            this.start = start;
            atts = ((Element)start.node).getAttributes().iterator();
        }

        public void advance() {
            if (atts.hasNext()) {
                current = makeWrapper(atts.next(), docWrapper, start, ix++);
            } else {
                current = null;
            }
        }

        public SequenceIterator getAnother() {
            return new AttributeEnumeration(start);
        }

    }  // end of class AttributeEnumeration


    /**
    * The class ChildEnumeration handles not only the child axis, but also the
    * following-sibling and preceding-sibling axes. It can also iterate the children
    * of the start node in reverse order, something that is needed to support the
    * preceding and preceding-or-ancestor axes (the latter being used by xsl:number)
    */

    private final class ChildEnumeration extends Navigator.BaseEnumeration {

        private NodeWrapper start;
        private NodeWrapper commonParent;
        private ListIterator children;
        private int ix = 0;
        private boolean downwards;  // iterate children of start node (not siblings)
        private boolean forwards;   // iterate in document order (not reverse order)

        public ChildEnumeration(NodeWrapper start,
                                boolean downwards, boolean forwards) {
            this.start = start;
            this.downwards = downwards;
            this.forwards = forwards;

            if (downwards) {
                commonParent = start;
            } else {
                commonParent = (NodeWrapper)start.getParent();
            }

            if (commonParent.getNodeKind()==Type.DOCUMENT) {
                children = ((Document)commonParent.node).getContent().listIterator();
            } else {
                children = ((Element)commonParent.node).getContent().listIterator();
            }

            if (downwards) {
                if (!forwards) {
                    // backwards enumeration: go to the end
                    while (children.hasNext()) {
                        children.next();
                        ix++;
                    }
                }
            } else {
                ix = start.getSiblingPosition();
                // find the start node among the list of siblings
                if (forwards) {
                    for (int i=0; i<=ix; i++) {
                        children.next();
                    }
                    ix++;
                } else {
                    for (int i=0; i<ix; i++) {
                        children.next();
                    }
                    ix--;
                }
            }
            //advance();
        }



        public void advance() {
            if (forwards) {
                if (children.hasNext()) {
                    Object nextChild = children.next();
                    if (nextChild instanceof DocType) {
                        advance();
                        return;
                    }
                    if (nextChild instanceof EntityRef) {
                        throw new IllegalStateException("Unexpanded entity in JDOM tree");
                    } else {
                        if (isAtomizing()) {
                            current = new UntypedAtomicValue(getStringValue(node));
                        } else {
                            current = makeWrapper(nextChild, docWrapper, commonParent, ix++);
                        }
                    }
                } else {
                    current = null;
                }
            } else {    // backwards
                if (children.hasPrevious()) {
                    Object nextChild = children.previous();
                    if (nextChild instanceof DocType) {
                        advance();
                        return;
                    }
                    if (nextChild instanceof EntityRef) {
                        throw new IllegalStateException("Unexpanded entity in JDOM tree");
                    } else {
                        if (isAtomizing()) {
                            current = new UntypedAtomicValue(getStringValue(node));
                        } else {
                            current = makeWrapper(nextChild, docWrapper, commonParent, ix--);
                        }
                    }
                } else {
                    current = null;
                }
            }
        }

        public SequenceIterator getAnother() {
            return new ChildEnumeration(start, downwards, forwards);
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
