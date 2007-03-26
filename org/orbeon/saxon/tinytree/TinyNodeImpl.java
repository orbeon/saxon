package org.orbeon.saxon.tinytree;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.SourceLocator;


/**
 * A node in a TinyTree representing an XML element, character content, or attribute.<P>
 * This is the top-level class in the implementation class hierarchy; it essentially contains
 * all those methods that can be defined using other primitive methods, without direct access
 * to data.
 *
 * @author Michael H. Kay
 */

public abstract class TinyNodeImpl implements NodeInfo, ExtendedNodeInfo, FingerprintedNode, SourceLocator {

    protected TinyTree tree;
    protected int nodeNr;
    protected TinyNodeImpl parent = null;

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
     */

    public int getTypeAnnotation() {
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
     * Get the typed value of this node.
     * If there is no type annotation, we return the string value, as an instance
     * of xdt:untypedAtomic
     */

    public SequenceIterator getTypedValue() throws XPathException {
        int annotation = getTypeAnnotation();
        if ((annotation & NodeInfo.IS_DTD_TYPE) != 0) {
            annotation = StandardNames.XDT_UNTYPED_ATOMIC;
        }
        annotation &= NamePool.FP_MASK;
        if (annotation == -1 || annotation == StandardNames.XDT_UNTYPED_ATOMIC || annotation == StandardNames.XDT_UNTYPED) {
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
                throw new DynamicError("Unknown type annotation " +
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
            annotation = StandardNames.XDT_UNTYPED_ATOMIC;
        }
        if (annotation == -1 || annotation == StandardNames.XDT_UNTYPED_ATOMIC || annotation == StandardNames.XDT_UNTYPED) {
            return new UntypedAtomicValue(getStringValueCS());
        } else {
            SchemaType stype = getConfiguration().getSchemaType(annotation);
            if (stype == null) {
                String typeName = getNamePool().getDisplayName(annotation);
                throw new DynamicError("Unknown type annotation " +
                        Err.wrap(typeName) + " in document instance");
            } else {
                return stype.atomize(this);
            }
        }
    }


    /**
     * Set the system id of this node. <br />
     * This method is present to ensure that
     * the class implements the javax.xml.transform.Source interface, so a node can
     * be used as the source of a transformation.
     */

    public void setSystemId(String uri) {
        short type = tree.nodeKind[nodeNr];
        if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) {
            getParent().setSystemId(uri);
        } else {
            tree.setSystemId(nodeNr, uri);
        }
    }

    /**
     * Set the parent of this node. Providing this information is useful,
     * if it is known, because otherwise getParent() has to search backwards
     * through the document.
     */

    protected void setParentNode(TinyNodeImpl parent) {
        this.parent = parent;
    }

    /**
     * Determine whether this is the same node as another node
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TinyNodeImpl)) {
            return false;
        }
        if (this.tree != ((TinyNodeImpl)other).tree) {
            return false;
        }
        if (this.nodeNr != ((TinyNodeImpl)other).nodeNr) {
            return false;
        }
        if (this.getNodeKind() != other.getNodeKind()) {
            return false;
        }
        return true;
    }

    /**
     * The equals() method compares nodes for identity. It is defined to give the same result
     * as isSameNodeInfo().
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *         the same node in the tree.
     * @since 8.7 Previously, the effect of the equals() method was not defined. Callers
     *        should therefore be aware that third party implementations of the NodeInfo interface may
     *        not implement the correct semantics. It is safer to use isSameNodeInfo() for this reason.
     *        The equals() method has been defined because it is useful in contexts such as a Java Set or HashMap.
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
     *
     * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
     *        should therefore be aware that third party implementations of the NodeInfo interface may
     *        not implement the correct semantics.
     */

    public int hashCode() {
        return ((tree.getDocumentNumber() & 0x3ff) << 20) ^ nodeNr ^ (getNodeKind() << 14);
    }

    /**
     * Get the system ID for the entity containing the node.
     */

    public String getSystemId() {
        return tree.getSystemId(nodeNr);
    }

    /**
     * Get the base URI for the node. Default implementation for child nodes gets
     * the base URI of the parent node.
     */

    public String getBaseURI() {
        return (getParent()).getBaseURI();
    }

    /**
     * Get the line number of the node within its source document entity
     */

    public int getLineNumber() {
        return tree.getLineNumber(nodeNr);
    }

    /**
     * Get the node sequence number (in document order). Sequence numbers are monotonic but not
     * consecutive. The sequence number must be unique within the document (not, as in
     * previous releases, within the whole document collection).
     * For document nodes, elements, text nodes, comment nodes, and PIs, the sequence number
     * is a long with the sequential node number in the top half and zero in the bottom half.
     * The bottom half is used only for attributes and namespace.
     */

    protected long getSequenceNumber() {
        return (long)nodeNr << 32;
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
        long a = getSequenceNumber();
        if (other instanceof TinyNodeImpl) {
            long b = ((TinyNodeImpl)other).getSequenceNumber();
            if (a < b) {
                return -1;
            }
            if (a > b) {
                return +1;
            }
            return 0;
        } else {
            // it must be a namespace node
            return 0 - other.compareOrder(this);
        }
    }

    /**
     * Get the fingerprint of the node, used for matching names
     */

    public int getFingerprint() {
        int nc = getNameCode();
        if (nc == -1) {
            return -1;
        }
        return nc & 0xfffff;
    }

    /**
     * Get the name code of the node, used for matching names
     */

    public int getNameCode() {
        // overridden for attributes and namespace nodes.
        return tree.nameCode[nodeNr];
    }

    /**
     * Get the prefix part of the name of this node. This is the name before the ":" if any.
     *
     * @return the prefix part of the name. For an unnamed node, return "".
     */

    public String getPrefix() {
        int code = tree.nameCode[nodeNr];
        if (code < 0) {
            return "";
        }
        if (NamePool.getPrefixIndex(code) == 0) {
            return "";
        }
        return tree.getNamePool().getPrefix(code);
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node, or for
     *         an element or attribute in the default namespace, return an empty string.
     */

    public String getURI() {
        int code = tree.nameCode[nodeNr];
        if (code < 0) {
            return "";
        }
        return tree.getNamePool().getURI(code);
    }

    /**
     * Get the display name of this node (a lexical QName). For elements and attributes this is [prefix:]localname.
     * The original prefix is retained. For unnamed nodes, the result is an empty string.
     *
     * @return The display name of this node.
     *         For a node with no name, return an empty string.
     */

    public String getDisplayName() {
        int code = tree.nameCode[nodeNr];
        if (code < 0) {
            return "";
        }
        return tree.getNamePool().getDisplayName(code);
    }

    /**
     * Get the local part of the name of this node.
     *
     * @return The local name of this node.
     *         For a node with no name, return "".
     */

    public String getLocalPart() {
        int code = tree.nameCode[nodeNr];
        if (code < 0) {
            return "";
        }
        return tree.getNamePool().getLocalName(code);
    }

    /**
     * Return an iterator over all the nodes reached by the given axis from this node
     *
     * @param axisNumber Identifies the required axis, eg. Axis.CHILD or Axis.PARENT
     * @return a AxisIteratorImpl that scans the nodes reached by the axis in turn.
     */

    public AxisIterator iterateAxis(byte axisNumber) {
        // fast path for child axis
        if (axisNumber == Axis.CHILD) {
            if (hasChildNodes()) {
                return new SiblingEnumeration(tree, this, null, true);
            } else {
                return EmptyIterator.getInstance();
            }
        } else {
            return iterateAxis(axisNumber, AnyNodeTest.getInstance());
        }
    }

    /**
     * Return an iterator over the nodes reached by the given axis from this node
     *
     * @param axisNumber Identifies the required axis, eg. Axis.CHILD or Axis.PARENT
     * @param nodeTest   A pattern to be matched by the returned nodes.
     * @return a AxisIteratorImpl that scans the nodes reached by the axis in turn.
     */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {

        int type = getNodeKind();
        switch (axisNumber) {
            case Axis.ANCESTOR:
                return new AncestorEnumeration(this, nodeTest, false);

            case Axis.ANCESTOR_OR_SELF:
                return new AncestorEnumeration(this, nodeTest, true);

            case Axis.ATTRIBUTE:
                if (type != Type.ELEMENT) {
                    return EmptyIterator.getInstance();
                }
                if (tree.alpha[nodeNr] < 0) {
                    return EmptyIterator.getInstance();
                }
                return new AttributeEnumeration(tree, nodeNr, nodeTest);

            case Axis.CHILD:
                if (hasChildNodes()) {
                    return new SiblingEnumeration(tree, this, nodeTest, true);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT:
                if (type == Type.DOCUMENT &&
                        nodeTest instanceof NameTest &&
                        nodeTest.getPrimitiveType() == Type.ELEMENT) {
                    return ((TinyDocumentImpl)this).getAllElements(nodeTest.getFingerprint());
                } else if (hasChildNodes()) {
                    return new DescendantEnumeration(tree, this, nodeTest, false);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                if (hasChildNodes()) {
                    return new DescendantEnumeration(tree, this, nodeTest, true);
                } else {
                    if (nodeTest.matches(this)) {
                        return SingletonIterator.makeIterator(this);
                    } else {
                        return EmptyIterator.getInstance();
                    }
                }

            case Axis.FOLLOWING:
                if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) {
                    return new FollowingEnumeration(tree, (TinyNodeImpl)getParent(), nodeTest, true);
                } else if (tree.depth[nodeNr] == 0) {
                    return EmptyIterator.getInstance();
                } else {
                    return new FollowingEnumeration(tree, this, nodeTest, false);
                }

            case Axis.FOLLOWING_SIBLING:
                if (type == Type.ATTRIBUTE || type == Type.NAMESPACE || tree.depth[nodeNr] == 0) {
                    return EmptyIterator.getInstance();
                } else {
                    return new SiblingEnumeration(tree, this, nodeTest, false);
                }

            case Axis.NAMESPACE:
                if (type != Type.ELEMENT) {
                    return EmptyIterator.getInstance();
                }
                return new NamespaceIterator(this, nodeTest);

            case Axis.PARENT:
                NodeInfo parent = getParent();
                return Navigator.filteredSingleton(parent, nodeTest);

            case Axis.PRECEDING:
                if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) {
                    return new PrecedingEnumeration(tree, (TinyNodeImpl)getParent(), nodeTest, false);
                } else if (tree.depth[nodeNr] == 0) {
                    return EmptyIterator.getInstance();
                } else {
                    return new PrecedingEnumeration(tree, this, nodeTest, false);
                }

            case Axis.PRECEDING_SIBLING:
                if (type == Type.ATTRIBUTE || type == Type.NAMESPACE || tree.depth[nodeNr] == 0) {
                    return EmptyIterator.getInstance();
                } else {
                    return new PrecedingSiblingEnumeration(tree, this, nodeTest);
                }

            case Axis.SELF:
                return Navigator.filteredSingleton(this, nodeTest);

            case Axis.PRECEDING_OR_ANCESTOR:
                if (type == Type.DOCUMENT) {
                    return EmptyIterator.getInstance();
                } else if (type == Type.ATTRIBUTE || type == Type.NAMESPACE) {
                    // See test numb32.
                    TinyNodeImpl el = (TinyNodeImpl)getParent();
                    return new PrependIterator(el, new PrecedingEnumeration(tree, el, nodeTest, true));
                } else {
                    return new PrecedingEnumeration(tree, this, nodeTest, true);
                }

            default:
                throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
     * Find the parent node of this node.
     *
     * @return The Node object describing the containing element or root node.
     */

    public NodeInfo getParent() {
        if (parent != null) {
            return parent;
        }
        int p = getParentNodeNr(tree, nodeNr);
        if (p == -1) {
            parent = null;
        } else {
            parent = tree.getNode(p);
        }
        return parent;
    }

    /**
     * Static method to get the parent of a given node, without instantiating the node as an object.
     * The starting node is any node other than an attribute or namespace node.
     *
     * @param tree   the tree containing the starting node
     * @param nodeNr the node number of the starting node within the tree
     * @return the node number of the parent node, or -1 if there is no parent.
     */

    static final int getParentNodeNr(TinyTree tree, int nodeNr) {

        if (tree.depth[nodeNr] == 0) {
            return -1;
        }

        // follow the next-sibling pointers until we reach either a next sibling pointer that
        // points backwards, or a parent-pointer pseudo-node
        int p = tree.next[nodeNr];
        while (p > nodeNr) {
            if (tree.nodeKind[p] == Type.PARENT_POINTER) {
                return tree.alpha[p];
            }
            p = tree.next[p];
        }
        return p;
    }

    /**
     * Determine whether the node has any children.
     *
     * @return <code>true</code> if this node has any attributes,
     *         <code>false</code> otherwise.
     */

    public boolean hasChildNodes() {
        // overridden in TinyParentNodeImpl
        return false;
    }

    /**
     * Get the value of a given attribute of this node
     *
     * @param fingerprint The fingerprint of the attribute name
     * @return the attribute value if it exists or null if not
     */

    public String getAttributeValue(int fingerprint) {
        // overridden in TinyElementImpl
        return null;
    }

    /**
     * Get the root node of the tree (not necessarily a document node)
     *
     * @return the NodeInfo representing the root of this tree
     */

    public NodeInfo getRoot() {
        if (tree.depth[nodeNr] == 0) {
            return this;
        }
        if (parent != null) {
            return parent.getRoot();
        }
        return tree.getNode(tree.getRootNode(nodeNr));
    }

    /**
     * Get the root (document) node
     *
     * @return the DocumentInfo representing the containing document
     */

    public DocumentInfo getDocumentRoot() {
        NodeInfo root = getRoot();
        if (root.getNodeKind() == Type.DOCUMENT) {
            return (DocumentInfo)root;
        } else {
            return null;
        }
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return tree.getConfiguration();
    }

    /**
     * Get the NamePool for the tree containing this node
     *
     * @return the NamePool
     */

    public NamePool getNamePool() {
        return tree.getNamePool();
    }

    /**
     * Output all namespace nodes associated with this element. Does nothing if
     * the node is not an element.
     *
     * @param out              The relevant outputter
     * @param includeAncestors True if namespaces declared on ancestor elements must
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
            throws XPathException {
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
     * Get a character string that uniquely identifies this node
     *
     * @param buffer buffer, which on return will contain
     *  a character string that uniquely identifies this node.
     *
     */

    public void generateId(FastStringBuffer buffer) {
        buffer.append("d");
        buffer.append(Integer.toString(tree.getDocumentNumber()));
        buffer.append(NODE_LETTER[getNodeKind()]);
        buffer.append(Integer.toString(nodeNr));
    }

    /**
     * Get the document number of the document containing this node
     * (Needed when the document isn't a real node, for sorting free-standing elements)
     */

    public final int getDocumentNumber() {
        return tree.getDocumentNumber();
    }

    /**
     * Test if this node is an ancestor-or-self of another
     *
     * @param d the putative descendant-or-self node
     * @return true if this node is an ancestor-or-self of d
     */

    public boolean isAncestorOrSelf(TinyNodeImpl d) {
        // If it's a different tree, return false
        if (tree != d.tree) return false;
        int dn = d.nodeNr;
        // If d is an attribute, then either "this" must be the same attribute, or "this" must
        // be an ancestor-or-self of the parent of d.
        if (d instanceof TinyAttributeImpl) {
            if (this instanceof TinyAttributeImpl) {
                return nodeNr == dn;
            } else {
                dn = tree.attParent[dn];
            }
        }
        // If this is an attribute, return false (we've already handled the case where it's the same attribute)
        if (this instanceof TinyAttributeImpl) return false;

        // From now on, we know that both "this" and "dn" are nodes in the primary array

        // If d is later in document order, return false
        if (nodeNr > dn) return false;

        // If it's the same node, return true
        if (nodeNr == dn) return true;

        // We've dealt with the "self" case: to be an ancestor, it must be an element or document node
        if (!(this instanceof TinyParentNodeImpl)) return false;

        // If this node is deeper than the target node then it can't be an ancestor
        if (tree.depth[nodeNr] >= tree.depth[dn]) return false;

        // The following code will exit as soon as we find an ancestor that has a following-sibling:
        // when that happens, we know it's an ancestor iff its following-sibling is beyond the node we're
        // looking for. If the ancestor has no following sibling, we go up a level.

        // The algorithm depends on the following assertion: if A is before D in document order, then
        // either A is an ancestor of D, or some ancestor-or-self of A has a following-sibling that
        // is before-or-equal to D in document order.

        int n = nodeNr;
        while (true) {
            int nextSib = tree.next[n];
            if (nextSib > dn) {
                return true;
            } else if (tree.depth[nextSib] == 0) {
                return true;
            } else if (nextSib < n) {
                n = nextSib;
                continue;
            } else {
                return false;
            }
        }
    }

    /**
     * Determine whether this node has the is-id property
     * @return true if the node is an ID
     */

    public boolean isId() {
        return false;   // overridden for element and attribute nodes
    }

    /**
     * Determine whether this node has the is-idref property
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        return false;    // overridden for element and attribute nodes
    }

    /**
     * Determine whether the node has the is-nilled property
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return tree.isNilled(nodeNr);
    }

    /**
     * Get the node number of this node within the TinyTree. This method is intended for internal use.
     */

    public int getNodeNumber() {
        return nodeNr;
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
