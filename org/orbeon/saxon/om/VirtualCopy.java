package org.orbeon.saxon.om;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.xpath.XPathException;

/**
 * This class represents a node that is a virtual copy of another node: that is, it behaves as a node that's the
 * same as another node, but has different identity. It is implemented by means of a reference to the node of which
 * it is a copy, but methods that are sensitive to node identity return a different result.
 */

public class VirtualCopy implements NodeInfo {

    protected String baseURI;
    protected int documentNumber;
    protected NodeInfo original;
    protected VirtualCopy parent;
    protected NodeInfo root;        // the node forming the root of the subtree that was copied

    protected VirtualCopy(NodeInfo base) {
        this.original = base;
    }

    public static VirtualCopy makeVirtualCopy(NodeInfo original, NodeInfo root) {

        VirtualCopy vc;
        // Don't allow copies of copies of copies: define the new copy in terms of the original
        while (original instanceof VirtualCopy) {
            original = ((VirtualCopy)original).original;
        }
        while (root instanceof VirtualCopy) {
            root = ((VirtualCopy)root).original;
        }
        if (original instanceof DocumentInfo) {
            vc = new VirtualDocumentCopy((DocumentInfo)original);
        } else {
            vc = new VirtualCopy(original);
        }
        vc.root = root;
        return vc;
    }

    public void setDocumentNumber(int documentNumber) {
        this.documentNumber = documentNumber;
    }

    /**
     * Get the kind of node. This will be a value such as Type.ELEMENT or Type.ATTRIBUTE
     *
     * @return an integer identifying the kind of node. These integer values are the
     *         same as those used in the DOM
     * @see org.orbeon.saxon.type.Type
     */

    public int getNodeKind() {
        return original.getNodeKind();
    }

    /**
     * Determine whether this is the same node as another node.
     * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b).
     * This method has the same semantics as isSameNode() in DOM Level 3, but
     * works on Saxon NodeInfo objects rather than DOM Node objects.
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *         the same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        return other instanceof VirtualCopy &&
                documentNumber == other.getDocumentNumber() &&
                original.isSameNodeInfo(((VirtualCopy)other).original);


    }

    /**
     * Get the System ID for the node.
     *
     * @return the System Identifier of the entity in the source document
     *         containing the node, or null if not known. Note this is not the
     *         same as the base URI: the base URI can be modified by xml:base, but
     *         the system ID cannot.
     */

    public String getSystemId() {
        return baseURI;
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. This will be the same as the System ID unless xml:base has been used.
     *
     * @return the base URI of the node
     */

    public String getBaseURI() {
        return baseURI;
    }

    /**
     * Get line number
     *
     * @return the line number of the node in its original source document; or
     *         -1 if not available
     */

    public int getLineNumber() {
        return original.getLineNumber();
    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this
     *              node
     * @return -1 if this node precedes the other node, +1 if it follows the
     *         other node, or 0 if they are the same node. (In this case,
     *         isSameNode() will always return true, and the two nodes will
     *         produce the same result for generateId())
     */

    public int compareOrder(NodeInfo other) {
        return original.compareOrder(((VirtualCopy)other).original);
    }

    /**
     * Return the string value of the node. The interpretation of this depends on the type
     * of node. For an element it is the accumulated character content of the element,
     * including descendant elements.
     *
     * @return the string value of the node
     */

    public String getStringValue() {
        return original.getStringValue();
    }

    /**
     * Get name code. The name code is a coded form of the node name: two nodes
     * with the same name code have the same namespace URI, the same local name,
     * and the same prefix. By masking the name code with &0xfffff, you get a
     * fingerprint: two nodes with the same fingerprint have the same local name
     * and namespace URI.
     *
     * @return an integer name code, which may be used to obtain the actual node
     *         name from the name pool
     * @see NamePool#allocate allocate
     * @see NamePool#getFingerprint getFingerprint
     */

    public int getNameCode() {
        return original.getNameCode();
    }

    /**
     * Get fingerprint. The fingerprint is a coded form of the expanded name
     * of the node: two nodes
     * with the same name code have the same namespace URI and the same local name.
     * A fingerprint of -1 should be returned for a node with no name.
     *
     * @return an integer fingerprint; two nodes with the same fingerprint have
     *         the same expanded QName
     */

    public int getFingerprint() {
        return original.getFingerprint();
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
     *         interface, this returns the full name in the case of a non-namespaced name.
     */

    public String getLocalPart() {
        return original.getLocalPart();
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *         or for a node with an empty prefix, return an empty
     *         string.
     */

    public String getURI() {
        return original.getURI();
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node. For a node with no name, return
     *         an empty string.
     */

    public String getDisplayName() {
        return original.getDisplayName();
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return original.getConfiguration();
    }

    /**
     * Get the NamePool that holds the namecode for this node
     *
     * @return the namepool
     */

    public NamePool getNamePool() {
        return original.getNamePool();
    }

    /**
     * Get the type annotation of this node, if any.
     * Returns -1 for kinds of nodes that have no annotation, and for elements annotated as
     * untyped, and attributes annotated as untypedAtomic.
     *
     * @return the type annotation of the node.
     * @see org.orbeon.saxon.type.Type
     */

    public int getTypeAnnotation() {
        return original.getTypeAnnotation();
    }

    /**
     * Get the NodeInfo object representing the parent of this node
     *
     * @return the parent of this node; null if this node has no parent
     */

    public NodeInfo getParent() {
        if (original.isSameNodeInfo(root)) {
            return null;
        }
        if (parent == null) {
            NodeInfo basep = original.getParent();
            if (basep == null) {
                return null;
            }
            parent = new VirtualCopy(basep);
            parent.setDocumentNumber(documentNumber);
        }
        return parent;
    }

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     *
     * @param axisNumber an integer identifying the axis; one of the constants
     *                   defined in class org.orbeon.saxon.om.Axis
     * @return an AxisIterator that scans the nodes reached by the axis in
     *         turn.
     * @throws UnsupportedOperationException if the namespace axis is
     *                                       requested and this axis is not supported for this implementation.
     * @see Axis
     */

    public AxisIterator iterateAxis(byte axisNumber) {
        return iterateAxis(axisNumber, AnyNodeTest.getInstance());
    }

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     * that match a given NodeTest
     *
     * @param axisNumber an integer identifying the axis; one of the constants
     *                   defined in class org.orbeon.saxon.om.Axis
     * @param nodeTest   A pattern to be matched by the returned nodes; nodes
     *                   that do not match this pattern are not included in the result
     * @return a NodeEnumeration that scans the nodes reached by the axis in
     *         turn.
     * @throws UnsupportedOperationException if the namespace axis is
     *                                       requested and this axis is not supported for this implementation.
     * @see Axis
     */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        VirtualCopy newParent = null;
        if (axisNumber == Axis.CHILD || axisNumber == Axis.ATTRIBUTE || axisNumber == Axis.NAMESPACE) {
            newParent = this;
        }
        if (axisNumber == Axis.SELF || axisNumber == Axis.PRECEDING_SIBLING || axisNumber == Axis.FOLLOWING_SIBLING) {
            newParent = parent;
        }
        NodeInfo root;
        if (Axis.isSubtreeAxis[axisNumber]) {
            root = null;
        } else {
            root = this.root;
        }
        return new VirtualCopier(original.iterateAxis(axisNumber, nodeTest), newParent, root);
    }

    /**
     * Get the value of a given attribute of this node
     *
     * @param fingerprint The fingerprint of the attribute name
     * @return the attribute value if it exists or null if not
     */

    public String getAttributeValue(int fingerprint) {
        return original.getAttributeValue(fingerprint);
    }

    /**
     * Get the root node of the tree containing this node
     *
     * @return the NodeInfo representing the top-level ancestor of this node.
     *         This will not necessarily be a document node
     */

    public NodeInfo getRoot() {
        NodeInfo n = this;
        while (true) {
            NodeInfo p = n.getParent();
            if (p == null) {
                return n;
            }
            n = p;
        }
    }

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *         node is part of a tree that does not have a document node as its
     *         root, return null.
     */

    public DocumentInfo getDocumentRoot() {
        NodeInfo root = getRoot();
        if (root instanceof DocumentInfo) {
            return (DocumentInfo)root;
        }
        return null;
    }

    /**
     * Determine whether the node has any children. <br />
     * Note: the result is equivalent to <br />
     * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
     *
     * @return True if the node has one or more children
     */

    public boolean hasChildNodes() {
        return original.hasChildNodes();
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @return a string that uniquely identifies this node, across all
     *         documents. (Changed in Saxon 7.5. Previously this method returned
     *         an id that was unique within the current document, and the calling
     *         code prepended a document id).
     */

    public String generateId() {
        return "d" + documentNumber + original.generateId();
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Copy this node to a given outputter
     *
     * @param out             the Receiver to which the node should be copied
     * @param whichNamespaces in the case of an element, controls
     *                        which namespace nodes should be copied. Values are NO_NAMESPACES,
     *                        LOCAL_NAMESPACES, ALL_NAMESPACES
     * @param copyAnnotations indicates whether the type annotations
     *                        of element and attribute nodes should be copied
     * @param locationId      Identifies the location of the instruction
     *                        that requested this copy. Pass zero if no other information is available
     * @throws org.orbeon.saxon.xpath.XPathException
     *
     */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        original.copy(out, whichNamespaces, copyAnnotations, locationId);
    }

    /**
     * Output all namespace nodes associated with this element. Does nothing if
     * the node is not an element.
     *
     * @param out              The relevant outputter
     * @param includeAncestors True if namespaces declared on ancestor
     *                         elements must be output; false if it is known that these are
     *                         already on the result tree
     */

    public void outputNamespaceNodes(Receiver out, boolean includeAncestors) throws XPathException {
        original.outputNamespaceNodes(out, includeAncestors);
    }

    /**
     * Set the system identifier for this Source.
     * <p/>
     * <p>The system identifier is optional if the source does not
     * get its data from a URL, but it may still be useful to provide one.
     * The application can use a system identifier, for example, to resolve
     * relative URIs and to include in error messages and warnings.</p>
     *
     * @param systemId The system identifier as a URL string.
     */
    public void setSystemId(String systemId) {
        baseURI = systemId;
    }

    /**
     * Get the typed value of the item
     *
     * @return the typed value of the item. In general this will be a sequence
     * @throws org.orbeon.saxon.xpath.XPathException
     *          where no typed value is available, e.g. for
     *          an element with complex content
     */

    public SequenceIterator getTypedValue() throws XPathException {
        return original.getTypedValue();
    }

    private class VirtualCopier implements AxisIterator, AtomizableIterator {

        private AxisIterator base;
        private VirtualCopy parent;
        private NodeInfo subtreeRoot;
        private Item current;

        public VirtualCopier(AxisIterator base, VirtualCopy parent, NodeInfo subtreeRoot) {
            this.base = base;
            this.parent = parent;
            this.subtreeRoot = subtreeRoot;
        }

        /**
         * Indicate that any nodes returned in the sequence will be atomized. This
         * means that if it wishes to do so, the implementation can return the typed
         * values of the nodes rather than the nodes themselves. The implementation
         * is free to ignore this hint.
         *
         * @param atomizing true if the caller of this iterator will atomize any
         *                  nodes that are returned, and is therefore willing to accept the typed
         *                  value of the nodes instead of the nodes themselves.
         */

        public void setIsAtomizing(boolean atomizing) {
            if (base instanceof AtomizableIterator) {
                ((AtomizableIterator)base).setIsAtomizing(atomizing);
            }
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next Item. If there are no more nodes, return null.
         */

        public Item next() {
            Item next = base.next();

            if (next instanceof NodeInfo) {
                if (subtreeRoot != null) {
                    // we're only interested in nodes within the subtree that was copied.
                    // Assert: once we find a node outside this subtree, all further nodes will also be outside
                    //         the subtree.
                    if (!isAncestorOrSelf(subtreeRoot, ((NodeInfo)next))) {
                        return null;
                    }
                }
                VirtualCopy vc = VirtualCopy.makeVirtualCopy(((NodeInfo)next), root);
                vc.parent = parent;
                vc.baseURI = baseURI;
                vc.documentNumber = documentNumber;
                next = vc;
            }
            current = next;
            return next;
        }

        /**
         * Get the current item in the sequence.
         *
         * @return the current item, that is, the item most recently returned by
         *         next()
         */

        public Item current() {
            return current;
        }

        /**
         * Get the current position
         *
         * @return the position of the current item (the item most recently
         *         returned by next()), starting at 1 for the first node
         */

        public int position() {
            return base.position();
        }

        /**
         * Get another iterator over the same sequence of items, positioned at the
         * start of the sequence
         *
         * @return a new iterator over the same sequence
         */

        public SequenceIterator getAnother() {
            return new VirtualCopier(base, parent, subtreeRoot);
        }

        /**
         * Test whether a node is an ancestor-or-self of another
         */

        private boolean isAncestorOrSelf(NodeInfo a, NodeInfo d) {
            while (true) {
                if (a.isSameNodeInfo(d)) {
                    return true;
                }
                d = d.getParent();
                if (d == null) {
                    return false;
                }
            }
        }

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
