package net.sf.saxon.om;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Stripper;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;


/**
 * A StrippedNode is a view of a node, in a virtual tree that has whitespace
 * text nodes stripped from it. All operations on the node produce the same result
 * as operations on the real underlying node, except that iterations over the axes
 * take care to skip whitespace-only text nodes that are supposed to be stripped.
 * Note that this class is only used in cases where a pre-built tree is supplied as
 * the input to a transformation, and where the stylesheet does whitespace stripping;
 * if a SAXSource or StreamSource is supplied, whitespace is stripped as the tree
 * is built.
*/

public class StrippedNode implements NodeInfo, VirtualNode {

    protected NodeInfo node;
    protected StrippedNode parent;     // null means unknown
    protected StrippedDocument docWrapper;

    protected StrippedNode() {}

    /**
     * This constructor is protected: nodes should be created using the makeWrapper
     * factory method
     * @param node    The node to be wrapped
     * @param parent  The StrippedNode that wraps the parent of this node
     */

    protected StrippedNode(NodeInfo node, StrippedNode parent) {
        this.node = node;
        this.parent = parent;
    }

    /**
     * Factory method to wrap a node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The underlying node
     * @param docWrapper  The wrapper for the document node (must be supplied)
     * @param parent      The wrapper for the parent of the node (null if unknown)
     * @return            The new wrapper for the supplied node
     */

    protected StrippedNode makeWrapper(NodeInfo node,
                                       StrippedDocument docWrapper,
                                       StrippedNode parent) {
        StrippedNode wrapper = new StrippedNode(node, parent);
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
        return node.getConfiguration();
    }

    /**
     * Get the name pool for this node
     * @return the NamePool
     */

    public NamePool getNamePool() {
        return node.getNamePool();
    }

    /**
    * Return the type of node.
    * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
    */

    public int getNodeKind() {
        return node.getNodeKind();
    }

    /**
    * Get the typed value of the item
    */

    public SequenceIterator getTypedValue() throws XPathException {
        return node.getTypedValue();
    }

    /**
    * Get the type annotation
    * @return 0 (there is no type annotation)
    */

    public int getTypeAnnotation() {
        return node.getTypeAnnotation();
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (other instanceof StrippedNode) {
            return node.isSameNodeInfo(((StrippedNode)other).node);
        } else {
            return node.isSameNodeInfo(other);
        }
    }

    /**
    * Get the System ID for the node.
    * @return the System Identifier of the entity in the source document containing the node,
    * or null if not known. Note this is not the same as the base URI: the base URI can be
    * modified by xml:base, but the system ID cannot.
    */

    public String getSystemId() {
        return node.getSystemId();
    }

    public void setSystemId(String uri) {
        node.setSystemId(uri);
    }

    /**
    * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. In the JDOM model, base URIs are held only an the document level. We don't
     * currently take any account of xml:base attributes.
    */

    public String getBaseURI() {
        return node.getBaseURI();
    }

    /**
    * Get line number
    * @return the line number of the node in its original source document; or -1 if not available
    */

    public int getLineNumber() {
        return node.getLineNumber();
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
        if (other instanceof StrippedNode) {
            return node.compareOrder(((StrippedNode)other).node);
        } else {
            return node.compareOrder(other);
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
        // Might not be the same as the string value of the underlying node because of space stripping
        switch (getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                AxisIterator iter = iterateAxis(Axis.DESCENDANT, NodeKindTest.makeNodeKindTest(Type.TEXT));
                FastStringBuffer sb = new FastStringBuffer(1024);
                while(true) {
                    NodeInfo it = (NodeInfo)iter.next();
                    if (it == null) {
                        break;
                    }
                    sb.append(it.getStringValueCS());
                }
                return sb.condense();
            default:
                return node.getStringValueCS();
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
        return node.getNameCode();
	}

	/**
	* Get fingerprint. The fingerprint is a coded form of the expanded name
	* of the node: two nodes
	* with the same name code have the same namespace URI and the same local name.
	* A fingerprint of -1 should be returned for a node with no name.
	*/

	public int getFingerprint() {
	    return node.getFingerprint();
	}

    /**
    * Get the local part of the name of this node. This is the name after the ":" if any.
    * @return the local part of the name. For an unnamed node, returns null, except for
     * un unnamed namespace node, which returns "".
    */

    public String getLocalPart() {
        return node.getLocalPart();
    }

   /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
    * @return The URI of the namespace of this node. For an unnamed node, return null.
    * For a node with an empty prefix, return an empty string.
    */

    public String getURI() {
        return node.getURI();
    }

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     *
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
        return node.getDisplayName();
    }

    /**
    * Get the NodeInfo object representing the parent of this node
    */

    public NodeInfo getParent() {
        if (parent==null) {
            NodeInfo realParent = node.getParent();
            if (realParent != null) {
                parent = makeWrapper(realParent, docWrapper, null);
            }
        }
        return parent;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ATTRIBUTE:
            case Axis.NAMESPACE:
                return new WrappingIterator(node.iterateAxis(axisNumber), this);
            case Axis.CHILD:
                return new StrippingIterator(node.iterateAxis(axisNumber), this);
            case Axis.FOLLOWING_SIBLING:
            case Axis.PRECEDING_SIBLING:
                StrippedNode parent = (StrippedNode)getParent();
                if (parent == null) {
                    return EmptyIterator.getInstance();
                } else {
                    return new StrippingIterator(node.iterateAxis(axisNumber), parent);
                }
            default:
                return new StrippingIterator(node.iterateAxis(axisNumber), null);
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
        return node.getAttributeValue(fingerprint);
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
        return node.hasChildNodes();
    }

    /**
    * Get a character string that uniquely identifies this node.
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return a string that uniquely identifies this node, within this
    * document. The calling code prepends information to make the result
    * unique across all documents.
    */

    public String generateId() {
        return node.generateId();
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return docWrapper.getDocumentNumber();
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        // The underlying code does not do whitespace stripping. So we need to interpose
        // a stripper.
        Stripper stripper = docWrapper.getStripper().getAnother();
        stripper.setUnderlyingReceiver(out);
        node.copy(stripper, whichNamespaces, copyAnnotations, locationId);
    }

    /**
    * Output all namespace nodes associated with this element. Does nothing if
    * the node is not an element.
    * @param out The relevant outputter
     * @param includeAncestors True if namespaces declared on ancestor elements must
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
    throws XPathException {
        node.sendNamespaceDeclarations(out, includeAncestors);
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
        return node.getDeclaredNamespaces(buffer);
    }

    /**
     * A WrappingIterator delivers wrappers for the nodes delivered
     * by its underlying iterator. It is used when no whitespace stripping
     * is actually needed, e.g. for the attribute axis. But we still need to
     * create wrappers, so that further iteration remains in the virtual layer
     * rather than switching to the real nodes.
     */

    private final class WrappingIterator implements AxisIterator, AtomizableIterator {

        AxisIterator base;
        StrippedNode parent;
        Item current;
        boolean atomizing = false;

        /**
         * Create a WrappingIterator
         * @param base The underlying iterator
         * @param parent If all the nodes to be wrapped have the same parent,
         * it can be specified here. Otherwise specify null.
         */

        public WrappingIterator(AxisIterator base, StrippedNode parent) {
            this.base = base;
            this.parent = parent;
        }

        public Item next() {
            Item n = base.next();
            if (n instanceof NodeInfo && !atomizing) {
                current = makeWrapper((NodeInfo)n, docWrapper, parent);
            } else {
                current = n;
            }
            return current;
        }

        public Item current() {
            return current;
        }

        public int position() {
            return base.position();
        }

        public SequenceIterator getAnother() {
            return new WrappingIterator(base, parent);
        }

        /**
         * Indicate that any nodes returned in the sequence will be atomized. This
         * means that if it wishes to do so, the implementation can return the typed
         * values of the nodes rather than the nodes themselves. The implementation
         * is free to ignore this hint.
         * @param atomizing true if the caller of this iterator will atomize any
         * nodes that are returned, and is therefore willing to accept the typed
         * value of the nodes instead of the nodes themselves.
         */

        public void setIsAtomizing(boolean atomizing) {
            this.atomizing = true;
            if (base instanceof AtomizableIterator) {
                ((AtomizableIterator)base).setIsAtomizing(atomizing);
            }
        }

    }  // end of class WrappingIterator

    /**
     * A StrippingIterator delivers wrappers for the nodes delivered
     * by its underlying iterator. It is used when whitespace stripping
     * may be needed, e.g. for the child axis. It examines all text nodes
     * encountered to see if they need to be stripped, and if so, it
     * skips them.
     */

    private final class StrippingIterator implements AxisIterator {

        // TODO: whitespace shouldn't be stripped if xml:space="preserve" is set
        // on an ancestor, even if the stylesheet requests it. But this is very
        // expensive to implement.

        AxisIterator base;
        StrippedNode parent;
        NodeInfo currentVirtualNode;
        int position;

        /**
         * Create a StrippingIterator
         * @param base The underlying iterator
         * @param parent If all the nodes to be wrapped have the same parent,
         * it can be specified here. Otherwise specify null.
         */

        public StrippingIterator(AxisIterator base, StrippedNode parent) {
            this.base = base;
            this.parent = parent;
            position = 0;
        }

        public Item next() {
            NodeInfo nextRealNode;
            while (true) {
                nextRealNode = (NodeInfo)base.next();
                if (nextRealNode==null) {
                    return null;
                }
                if (nextRealNode.getNodeKind() != Type.TEXT) {
                    break;
                }
                if (!Navigator.isWhite(nextRealNode.getStringValueCS())) {
                    break;
                }
                NodeInfo actualParent =
                        (parent==null ? nextRealNode.getParent() : parent.node);
                if (docWrapper.getStripper().isSpacePreserving(actualParent) == Stripper.ALWAYS_PRESERVE) {
                    break;
                }
                // otherwise skip this whitespace text node
            }

            currentVirtualNode = makeWrapper(nextRealNode, docWrapper, parent);
            position++;
            return currentVirtualNode;
        }

        public Item current() {
            return currentVirtualNode;
        }

        public int position() {
            return position;
        }

        public SequenceIterator getAnother() {
            return new StrippingIterator((AxisIterator)base.getAnother(), parent);
        }

    }  // end of class StrippingIterator



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
// The Initial Developer of the Original Code is
// Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
