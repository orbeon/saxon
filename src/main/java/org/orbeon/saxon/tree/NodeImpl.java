package org.orbeon.saxon.tree;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.SourceLocator;


/**
 * A node in the "linked" tree representing any kind of node except a namespace node.
 * Specific node kinds are represented by concrete subclasses.
 *
 * @author Michael H. Kay
 */

public abstract class NodeImpl
        implements MutableNodeInfo, FingerprintedNode, SiblingCountingNode, SourceLocator {

    protected ParentNodeImpl parent;
    protected int index;
    /**
     * Chararacteristic letters to identify each type of node, indexed using the node type
     * values. These are used as the initial letter of the result of generate-id()
     */

    public static final char[] NODE_LETTER =
            {'x', 'e', 'a', 't', 'x', 'x', 'x', 'p', 'c', 'r', 'x', 'x', 'x', 'n'};

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return getStringValue();
    }

    /**
     * Get the type annotation of this node, if any
     * @return the type annotation, as the integer name code of the type name
     */

    public int getTypeAnnotation() {
        return StandardNames.XS_UNTYPED;
    }

    /**
     * Get the column number of the node.
     * The default implementation returns -1, meaning unknown
     */

    public int getColumnNumber() {
        if (parent == null) {
            return -1;
        } else {
            return parent.getColumnNumber();
        }
    }

    /**
     * Get the public identifier of the document entity containing this node.
     * The default implementation returns null, meaning unknown
     */

    public String getPublicId() {
        return null;
    }


    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return getPhysicalRoot().getDocumentNumber();
    }


    /**
     * Get the index position of this node among its siblings (starting from 0)
     * @return 0 for the first child, 1 for the second child, etc.
     */
    public int getSiblingPosition() {
        return index;
    }

    /**
     * Get the typed value of this node.
     * If there is no type annotation, we return the string value, as an instance
     * of xs:untypedAtomic
     */

    public SequenceIterator getTypedValue() throws XPathException {
        int annotation = getTypeAnnotation();
        if ((annotation & NodeInfo.IS_DTD_TYPE) != 0) {
            annotation = StandardNames.XS_UNTYPED_ATOMIC;
        }
        annotation &= NamePool.FP_MASK;
        if (annotation == -1 || annotation == StandardNames.XS_UNTYPED_ATOMIC || annotation == StandardNames.XS_UNTYPED) {
            return SingletonIterator.makeIterator(new UntypedAtomicValue(getStringValueCS()));
        } else {
            SchemaType stype = getConfiguration().getSchemaType(annotation);
            if (stype == null) {
                String typeName;
                try {
                    typeName = getNamePool().getDisplayName(annotation);
                } catch (Exception err) {
                    typeName = annotation + "";
                }
                throw new XPathException("Unknown type annotation " +
                        Err.wrap(typeName) + " in document instance");
            } else {
                return stype.getTypedValue(this);
            }
        }
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

    public Value atomize() throws XPathException {
        int annotation = getTypeAnnotation();
        if ((annotation & NodeInfo.IS_DTD_TYPE) != 0) {
            annotation = StandardNames.XS_UNTYPED_ATOMIC;
        }
        if (annotation == -1 || annotation == StandardNames.XS_UNTYPED_ATOMIC || annotation == StandardNames.XS_UNTYPED) {
            return new UntypedAtomicValue(getStringValueCS());
        } else {
            SchemaType stype = getConfiguration().getSchemaType(annotation);
            if (stype == null) {
                String typeName = getNamePool().getDisplayName(annotation);
                throw new XPathException("Unknown type annotation " +
                        Err.wrap(typeName) + " in document instance");
            } else {
                return stype.atomize(this);
            }
        }
    }

    /**
     * Set the system ID of this node. This method is provided so that a NodeInfo
     * implements the javax.xml.transform.Source interface, allowing a node to be
     * used directly as the Source of a transformation
     */

    public void setSystemId(String uri) {
        // overridden in DocumentImpl and ElementImpl
        getParent().setSystemId(uri);
    }

    /**
     * Determine whether this is the same node as another node
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        // default implementation: differs for attribute and namespace nodes
        return this == other;
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
       return other instanceof NodeInfo && isSameNodeInfo((NodeInfo)other);
   }

     /**
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

//     public int hashCode() {
//         FastStringBuffer buff = new FastStringBuffer(20);
//         generateId(buff);
//         return buff.toString().hashCode();
//     }

    /**
     * Get the nameCode of the node. This is used to locate the name in the NamePool
     */

    public int getNameCode() {
        // default implementation: return -1 for an unnamed node
        return -1;
    }

    /**
     * Get the fingerprint of the node. This is used to compare whether two nodes
     * have equivalent names. Return -1 for a node with no name.
     */

    public int getFingerprint() {
        int nameCode = getNameCode();
        if (nameCode == -1) {
            return -1;
        }
        return nameCode & 0xfffff;
    }

    /**
     * Get a character string that uniquely identifies this node within this document
     * (The calling code will prepend a document identifier)
     */

    public void generateId(FastStringBuffer buffer) {
        long seq = getSequenceNumber();
        if (seq == -1L) {
            getPhysicalRoot().generateId(buffer);
            buffer.append(NODE_LETTER[getNodeKind()]);
            buffer.append(Long.toString(seq));
        } else {
            parent.generateId(buffer);
            buffer.append(NODE_LETTER[getNodeKind()]);
            buffer.append(Integer.toString(index));
        }
    }

    /**
     * Get the system ID for the node. Default implementation for child nodes.
     */

    public String getSystemId() {
        return parent.getSystemId();
    }

    /**
     * Get the base URI for the node. Default implementation for child nodes.
     */

    public String getBaseURI() {
        return parent.getBaseURI();
    }

    /**
     * Get the node sequence number (in document order). Sequence numbers are monotonic but not
     * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
     * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
     * the top word the same as their owner and the bottom half reflecting their relative position.
     * This is the default implementation for child nodes.
     * For nodes added by XQUery Update, the sequence number is -1L
     * @return the sequence number if there is one, or -1L otherwise.
     */

    protected long getSequenceNumber() {
        NodeImpl prev = this;
        for (int i = 0; ; i++) {
            if (prev instanceof ParentNodeImpl) {
                long prevseq = prev.getSequenceNumber();
                return (prevseq == -1L ? prevseq : prevseq + 0x10000 + i);
                // note the 0x10000 is to leave room for namespace and attribute nodes.
            }
            prev = prev.getPreviousInDocument();
        }

    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this node
     * @return -1 if this node precedes the other node, +1 if it follows the other
     *         node, or 0 if they are the same node. (In this case, isSameNode() will always
     *         return true, and the two nodes will produce the same result for generateId())
     */

    public final int compareOrder(NodeInfo other) {
        if (other instanceof NamespaceIterator.NamespaceNodeImpl) {
            return 0 - other.compareOrder(this);
        }
        long a = getSequenceNumber();
        long b = ((NodeImpl)other).getSequenceNumber();
        if (a == -1L || b == -1L) {
            // Nodes added by XQuery Update do not have sequence numbers
            return Navigator.compareOrder(this, ((NodeImpl)other));
        }
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return +1;
        }
        return 0;
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return getPhysicalRoot().getConfiguration();
    }

    /**
     * Get the NamePool
     */

    public NamePool getNamePool() {
        return getPhysicalRoot().getNamePool();
    }

    /**
     * Get the prefix part of the name of this node. This is the name before the ":" if any.
     *
     * @return the prefix part of the name. For an unnamed node, return an empty string.
     */

    public String getPrefix() {
        int nameCode = getNameCode();
        if (nameCode == -1) {
            return "";
        }
        if (NamePool.getPrefixIndex(nameCode) == 0) {
            return "";
        }
        return getNamePool().getPrefix(nameCode);
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For the null namespace, return an
     *         empty string. For an unnamed node, return the empty string.
     */

    public String getURI() {
        int nameCode = getNameCode();
        if (nameCode == -1) {
            return "";
        }
        return getNamePool().getURI(nameCode);
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node.
     *         For a node with no name, return an empty string.
     */

    public String getDisplayName() {
        int nameCode = getNameCode();
        if (nameCode == -1) {
            return "";
        }
        return getNamePool().getDisplayName(nameCode);
    }

    /**
     * Get the local name of this node.
     *
     * @return The local name of this node.
     *         For a node with no name, return "",.
     */

    public String getLocalPart() {
        int nameCode = getNameCode();
        if (nameCode == -1) {
            return "";
        }
        return getNamePool().getLocalName(nameCode);
    }

    /**
     * Get the line number of the node within its source document entity
     */

    public int getLineNumber() {
        return parent.getLineNumber();
    }



    /**
     * Find the parent node of this node.
     *
     * @return The Node object describing the containing element or root node.
     */

    public final NodeInfo getParent() {
        if (parent instanceof DocumentImpl && ((DocumentImpl)parent).isImaginary()) {
            return null;
        }
        return parent;
    }

    /**
     * Get the previous sibling of the node
     *
     * @return The previous sibling node. Returns null if the current node is the first
     *         child of its parent.
     */

    public NodeInfo getPreviousSibling() {
        if (parent == null) {
            return null;
        }
        return parent.getNthChild(index - 1);
    }


    /**
     * Get next sibling node
     *
     * @return The next sibling node of the required type. Returns null if the current node is the last
     *         child of its parent.
     */

    public NodeInfo getNextSibling() {
        if (parent == null) {
            return null;
        }
        return parent.getNthChild(index + 1);
    }

    /**
     * Get first child - default implementation used for leaf nodes
     *
     * @return null
     */

    public NodeInfo getFirstChild() {
        return null;
    }

    /**
     * Get last child - default implementation used for leaf nodes
     *
     * @return null
     */

    public NodeInfo getLastChild() {
        return null;
    }

    /**
     * Return an enumeration over the nodes reached by the given axis from this node
     *
     * @param axisNumber The axis to be iterated over
     * @return an AxisIterator that scans the nodes reached by the axis in turn.
     */

    public AxisIterator iterateAxis(byte axisNumber) {
        // Fast path for child axis
        if (axisNumber == Axis.CHILD) {
            if (this instanceof ParentNodeImpl) {
                return ((ParentNodeImpl)this).enumerateChildren(null);
            } else {
                return EmptyIterator.getInstance();
            }
        } else {
            return iterateAxis(axisNumber, AnyNodeTest.getInstance());
        }
    }

    /**
     * Return an enumeration over the nodes reached by the given axis from this node
     *
     * @param axisNumber The axis to be iterated over
     * @param nodeTest   A pattern to be matched by the returned nodes
     * @return an AxisIterator that scans the nodes reached by the axis in turn.
     */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {

        switch (axisNumber) {
            case Axis.ANCESTOR:
                return new AncestorEnumeration(this, nodeTest, false);

            case Axis.ANCESTOR_OR_SELF:
                return new AncestorEnumeration(this, nodeTest, true);

            case Axis.ATTRIBUTE:
                if (getNodeKind() != Type.ELEMENT) {
                    return EmptyIterator.getInstance();
                }
                return new AttributeEnumeration(this, nodeTest);

            case Axis.CHILD:
                if (this instanceof ParentNodeImpl) {
                    return ((ParentNodeImpl)this).enumerateChildren(nodeTest);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT:
                if (getNodeKind() == Type.DOCUMENT &&
                        nodeTest instanceof NameTest &&
                        nodeTest.getPrimitiveType() == Type.ELEMENT) {
                    return ((DocumentImpl)this).getAllElements(nodeTest.getFingerprint());
                } else if (hasChildNodes()) {
                    return new DescendantEnumeration(this, nodeTest, false);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                return new DescendantEnumeration(this, nodeTest, true);

            case Axis.FOLLOWING:
                return new FollowingEnumeration(this, nodeTest);

            case Axis.FOLLOWING_SIBLING:
                return new FollowingSiblingEnumeration(this, nodeTest);

            case Axis.NAMESPACE:
                if (getNodeKind() != Type.ELEMENT) {
                    return EmptyIterator.getInstance();
                }
                return NamespaceIterator.makeIterator(this, nodeTest);

            case Axis.PARENT:
                NodeInfo parent = getParent();
                if (parent == null) {
                    return EmptyIterator.getInstance();
                }
                return Navigator.filteredSingleton(parent, nodeTest);

            case Axis.PRECEDING:
                return new PrecedingEnumeration(this, nodeTest);

            case Axis.PRECEDING_SIBLING:
                return new PrecedingSiblingEnumeration(this, nodeTest);

            case Axis.SELF:
                return Navigator.filteredSingleton(this, nodeTest);

            case Axis.PRECEDING_OR_ANCESTOR:
                return new PrecedingOrAncestorEnumeration(this, nodeTest);

            default:
                throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
     * Find the value of a given attribute of this node. <BR>
     * This method is defined on all nodes to meet XSL requirements, but for nodes
     * other than elements it will always return null.
     * @param uri the namespace uri of an attribute
     * @param localName the local name of an attribute
     * @return the value of the attribute, if it exists, otherwise null
     */

//    public String getAttributeValue( String uri, String localName ) {
//        return null;
//    }

    /**
     * Find the value of a given attribute of this node. <BR>
     * This method is defined on all nodes to meet XSL requirements, but for nodes
     * other than elements it will always return null.
     * @param name the name of an attribute. This must be an unqualified attribute name,
     * i.e. one with no namespace prefix.
     * @return the value of the attribute, if it exists, otherwise null
     */

    //public String getAttributeValue( String name ) {
    //    return null;
    //}

    /**
     * Get the value of a given attribute of this node
     *
     * @param fingerprint The fingerprint of the attribute name
     * @return the attribute value if it exists or null if not
     */

    public String getAttributeValue(int fingerprint) {
        return null;
    }

    /**
     * Get the root node
     * @return the NodeInfo representing the logical root of the tree. For this tree implementation the
     * root will either be a document node or an element node.
     */

    public NodeInfo getRoot() {
        NodeInfo parent = getParent();
        if (parent == null) {
            return this;
        } else {
            return parent.getRoot();
        }
    }

    /**
     * Get the root (document) node
     * @return the DocumentInfo representing the containing document. If this
     *     node is part of a tree that does not have a document node as its
     *     root, returns null.
     */

    public DocumentInfo getDocumentRoot() {
        NodeInfo parent = getParent();
        if (parent == null) {
            return null;
        } else {
            return parent.getDocumentRoot();
        }
    }


    /**
     * Get the physical root of the tree. This may be an imaginary document node: this method
     * should be used only when control information held at the physical root is required
     * @return the document node, which may be imaginary. In the case of a node that has been detached
     * from the tree by means of a delete() operation, this method returns null.
     */

    public DocumentImpl getPhysicalRoot() {
        ParentNodeImpl up = parent;
        while (up != null && !(up instanceof DocumentImpl)) {
            up = up.parent;
        }
        return (DocumentImpl)up;
    }

    /**
     * Get the next node in document order
     *
     * @param anchor the scan stops when it reaches a node that is not a descendant of the specified
     *               anchor node
     * @return the next node in the document, or null if there is no such node
     */

    public NodeImpl getNextInDocument(NodeImpl anchor) {
        // find the first child node if there is one; otherwise the next sibling node
        // if there is one; otherwise the next sibling of the parent, grandparent, etc, up to the anchor element.
        // If this yields no result, return null.

        NodeImpl next = (NodeImpl)getFirstChild();
        if (next != null) {
            return next;
        }
        if (this == anchor) {
            return null;
        }
        next = (NodeImpl)getNextSibling();
        if (next != null) {
            return next;
        }
        NodeImpl parent = this;
        while (true) {
            parent = (NodeImpl)parent.getParent();
            if (parent == null) {
                return null;
            }
            if (parent == anchor) {
                return null;
            }
            next = (NodeImpl)parent.getNextSibling();
            if (next != null) {
                return next;
            }
        }
    }


    /**
     * Get the previous node in document order
     *
     * @return the previous node in the document, or null if there is no such node
     */

    public NodeImpl getPreviousInDocument() {

        // finds the last child of the previous sibling if there is one;
        // otherwise the previous sibling element if there is one;
        // otherwise the parent, up to the anchor element.
        // If this reaches the document root, return null.

        NodeImpl prev = (NodeImpl)getPreviousSibling();
        if (prev != null) {
            return prev.getLastDescendantOrSelf();
        }
        return (NodeImpl)getParent();
    }

    private NodeImpl getLastDescendantOrSelf() {
        NodeImpl last = (NodeImpl)getLastChild();
        if (last == null) {
            return this;
        }
        return last.getLastDescendantOrSelf();
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
        return null;
    }
    /**
     * Copy nodes. Copying type annotations is not yet supported for this tree
     * structure, so we simply map the new interface onto the old
     */

//    public final void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId)
//    throws XPathException {
//        copy(out, whichNamespaces);
//    }
//
//    public abstract void copy(Receiver out, int whichNamespaces) throws XPathException;

    // implement DOM Node methods

    /**
     * Determine whether the node has any children.
     *
     * @return <code>true</code> if the node has any children,
     *         <code>false</code> if the node has no children.
     */

    public boolean hasChildNodes() {
        return getFirstChild() != null;
    }

    /**
     * Determine whether this node has the is-id property
     *
     * @return true if the node is an ID
     */

    public boolean isId() {
        return false;
    }

    /**
     * Determine whether this node has the is-idref property
     *
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        return false;
    }

    /**
     * Determine whether the node has the is-nilled property
     *
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return false;
    }


    /**
     * Set the type annotation on a node. This must only be called when the caller has verified (by validation)
     * that the node is a valid instance of the specified type. The call is ignored if the node is not an element
     * or attribute node.
     *
     * @param typeCode the type annotation (possibly including high bits set to indicate the isID, isIDREF, and
     *                 isNilled properties)
     */

    public void setTypeAnnotation(int typeCode) {
        // no action
    }

    /**
     * Delete this node (that is, detach it from its parent)
     */

    public void delete() {
        // Overridden for attribute nodes
        if (parent != null) {
            parent.removeChild(this);
            DocumentImpl newRoot = new DocumentImpl();
            newRoot.setConfiguration(parent.getConfiguration());
            newRoot.setImaginary(true);
            parent = newRoot;
        }
        index = -1;
    }


    /**
     * Remove an attribute from this element node
     *
     * <p>If this node is not an element, or if it has no attribute with the specified name,
     * this method takes no action.</p>
     *
     * <p>The attribute node itself is not modified in any way.</p>
     * @param nameCode the name of the attribute to be removed
     */

    public void removeAttribute(int nameCode) {
        // no action (overridden in subclasses)
    }

    /**
     * Add an attribute to this element node.
     * <p/>
     * <p>If this node is not an element, or if the supplied node is not an attribute, the method
     * takes no action. If the element already has an attribute with this name, the existing attribute
     * is replaced.</p>
     *
     * @param nameCode the name of the new attribute
     * @param typeCode the type annotation of the new attribute
     * @param value the string value of the new attribute
     * @param properties properties including IS_ID and IS_IDREF properties
     */

    public void putAttribute(int nameCode, int typeCode, CharSequence value, int properties) {
        // No action, unless this is an element node
    }

    /**
     * Rename this node
     * @param newNameCode the NamePool code of the new name
     */

    public void rename(int newNameCode) {
        // implemented for node kinds that have a name
    }


    public void addNamespace(int nscode, boolean inherit) {
        // implemented for element nodes only
    }

    /**
     * Replace this node with a given sequence of nodes     *
     * @param replacement the replacement nodes
     * @param inherit set to true if new child elements are to inherit the in-scope namespaces
     * of their new parent
     * @throws IllegalArgumentException if any of the replacement nodes is not an element, text,
     * comment, or processing instruction node
     */

    public void replace(NodeInfo[] replacement, boolean inherit) {
        parent.replaceChildrenAt(replacement, index, inherit);
    }

    /**
     * Insert copies of a sequence of nodes as children of this node.
     * <p/>
     * <p>This method takes no action unless the target node is a document node or element node. It also
     * takes no action in respect of any supplied nodes that are not elements, text nodes, comments, or
     * processing instructions.</p>
     * <p/>
     * <p>The supplied nodes will be copied to form the new children. Adjacent text nodes will be merged, and
     * zero-length text nodes removed.</p>
     *
     * @param source  the nodes to be inserted
     * @param atStart true if the new nodes are to be inserted before existing children; false if they are
     * @param inherit true if the inserted nodes are to inherit the namespaces that are in-scope for their
     * new parent; false if such namespaces should be undeclared on the children
     */

    public void insertChildren(NodeInfo[] source, boolean atStart, boolean inherit) {
        throw new UnsupportedOperationException("insertChildren() can only be applied to a parent node");
    }

    /**
     * Insert copies of a sequence of nodes as siblings of this node.
     * <p/>
     * <p>This method takes no action unless the target node is an element, text node, comment, or
     * processing instruction, and one that has a parent node. It also
     * takes no action in respect of any supplied nodes that are not elements, text nodes, comments, or
     * processing instructions.</p>
     * <p/>
     * <p>The supplied nodes must use the same data model implementation as the tree into which they
     * will be inserted.</p>
     *
     * @param source the nodes to be inserted
     * @param before true if the new nodes are to be inserted before the target node; false if they are
     * @param inherit
     */

    public void insertSiblings(NodeInfo[] source, boolean before, boolean inherit) {
        if (parent == null) {
            throw new IllegalStateException("Cannot add siblings if there is no parent");
        }
        parent.insertChildrenAt(source, (before ? index : index+1), inherit);
    }


    /**
     * Remove type information from this node (and its ancestors, recursively).
     * This method implements the upd:removeType() primitive defined in the XQuery Update specification
     */

    public void removeTypeAnnotation() {
        // no action
    }

    /**
     * Get a Builder suitable for building nodes that can be attached to this document.
     * @return a new Builder that constructs nodes using the same object model implementation
     * as this one, suitable for attachment to this tree
     */    

    public Builder newBuilder() {
        return getPhysicalRoot().newBuilder();
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
