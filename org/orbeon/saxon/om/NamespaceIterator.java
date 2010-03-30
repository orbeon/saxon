package org.orbeon.saxon.om;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Value;

/**
 * This class provides an implementation of the namespace axis over any implementation
 * of the data model. It relies on element nodes to implement the method
 * {@link NodeInfo#getDeclaredNamespaces(int[])}
 */
public class NamespaceIterator implements AxisIterator {

    private NodeInfo element;
    private NodeTest test;
    private int index;
    private int position;
    private NamespaceNodeImpl next;
    private NamespaceNodeImpl current;
    private IntIterator nsIterator;

    /**
     * Factory method to create an iterator over the in-scope namespace nodes
     * @param element the node whose namespaces are required
     * @param test used to filter the returned nodes
     * @return an iterator over the namespace nodes that satisfy the test
     */

    public static AxisIterator makeIterator(NodeInfo element, NodeTest test) {
        boolean first = true;
        if (test instanceof AnyNodeTest || test == NodeKindTest.NAMESPACE) {
            test = null;
        }
        AxisIterator result = null;
        IntHashSet declared = null;
        IntHashSet undeclared = null;
        int[] buffer = new int[8];
        NodeInfo node = element;

        while (node != null && node.getNodeKind() == Type.ELEMENT) {

            int[] nslist = node.getDeclaredNamespaces(buffer);
            if (nslist != null) {
                for (int i=0; i<nslist.length; i++) {
                    if (nslist[i] ==-1) {
                        break;
                    }
                    if (first) {
                        NamespaceIterator nsi = new NamespaceIterator();
                        nsi.element = element;
                        nsi.test = test;
                        nsi.index = -1;
                        undeclared = new IntHashSet(8);
                        declared = new IntHashSet(8);
                        declared.add(NamespaceConstant.XML_NAMESPACE_CODE);
                        first = false;
                        result = nsi;
                    }
                    short uriCode = (short)(nslist[i] & 0xffff);
                    short prefixCode = (short)(nslist[i] >> 16);
                    if (uriCode == 0) {
                        // this is an undeclaration
                        undeclared.add(prefixCode);
                    } else {
                        if (!undeclared.contains(prefixCode)) {
                            declared.add(nslist[i]);
                            undeclared.add(prefixCode);
                        }
                    }
                }
            }
            node = node.getParent();
        }
        if (result == null) {
            NodeInfo ns = new NamespaceNodeImpl(element, NamespaceConstant.XML_NAMESPACE_CODE, 0);
            if (test == null) {
                return SingleNodeIterator.makeIterator(ns);
            } else {
                return Navigator.filteredSingleton(ns, test);
            }
        } else {
            ((NamespaceIterator)result).nsIterator = declared.iterator();
            return result;
        }
    }


    private NamespaceIterator() {

    }

    /**
     * Get the next item in the sequence.
     */

    public void advance() {
        while (nsIterator.hasNext()) {
            int nscode = nsIterator.next();
            next = new NamespaceNodeImpl(element, nscode, ++index);
            if (test == null || test.matches(next)) {
               return;
            }
        }
        next = null;
    }

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return (next() != null);
    }


    /**
     * Get the next item in the sequence. <BR>
     *
     * @return the next Item. If there are no more nodes, return null.
     */

    public Item next() {
        if (index == -1) {
            advance();
            index = 0;
        }
        current = next;
        if (current == null) {
            position = -1;
            return null;
        }
        advance();
        position++;
        return current;
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
        return position;
    }

    public void close() {
    }

    /**
     * Return an iterator over an axis, starting at the current node.
     *
     * @param axis the axis to iterate over, using a constant such as
     *             {@link Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     * @throws NullPointerException if there is no current node
     */

    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        return current.iterateAxis(axis, test);
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        return current.atomize();
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        return current.getStringValueCS();
    }

    /**
     * Get another iterator over the same sequence of items, positioned at the
     * start of the sequence
     *
     * @return a new iterator over the same sequence
     */

    public SequenceIterator getAnother() {
        return makeIterator(element, test);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }

    /**
     * Get a list of in-scope namespace codes. If an array of namespace codes is needed, without
     * actually constructing the namespace nodes, this factory method can be used directly.
     * The result is an array of integers, each containing a prefix code in the top
     * half and a uri code in the bottom half. Note that calling this method is destructive: the
     * iterator is consumed and cannot be used again.
     * @param element the element whose nodes are required
     * @return the list of in scope namespaces
     */

    public static int[] getInScopeNamespaceCodes(NodeInfo element) {
        boolean first = true;
        IntHashSet declared = null;
        IntHashSet undeclared = null;
        int[] buffer = new int[8];
        NodeInfo node = element;

        while (node != null && node.getNodeKind() == Type.ELEMENT) {

            int[] nslist = node.getDeclaredNamespaces(buffer);
            if (nslist != null) {
                for (int i=0; i<nslist.length; i++) {
                    if (nslist[i] ==-1) {
                        break;
                    }
                    if (first) {
                        undeclared = new IntHashSet(8);
                        declared = new IntHashSet(8);
                        declared.add(NamespaceConstant.XML_NAMESPACE_CODE);
                        first = false;
                    }
                    short uriCode = (short)(nslist[i] & 0xffff);
                    short prefixCode = (short)(nslist[i] >> 16);
                    if (uriCode == 0) {
                        // this is an undeclaration
                        undeclared.add(prefixCode);
                    } else {
                        if (!undeclared.contains(prefixCode)) {
                            declared.add(nslist[i]);
                            undeclared.add(prefixCode);
                        }
                    }
                }
            }
            node = node.getParent();
        }
        if (first) {
            return XML_NAMESPACE_CODE_ARRAY;
        } else {

            try {
                int[] codes = new int[declared.size()];
                int i = 0;
                IntIterator ii = declared.iterator();
                while (ii.hasNext()) {
                    codes[i++] = ii.next();
                }
                return codes;
            } catch (ArrayIndexOutOfBoundsException e) {
                // Diagnostic patch for a rarely-occurring problem, observed in both 8.6 and 9.1
                System.err.println("*** Internal error in NamespaceIterator ***");
                declared.diagnosticDump();
                throw e;
            }
        }
    }

    private static int[] XML_NAMESPACE_CODE_ARRAY = {NamespaceConstant.XML_NAMESPACE_CODE};

    /**
     * Inner class: a model-independent representation of a namespace node
     */

    public static class NamespaceNodeImpl implements NodeInfo, FingerprintedNode {

        NodeInfo element;
        int nscode;
        int position;
        int namecode;

        /**
         * Create a namespace node
         * @param element the parent element of the namespace node
         * @param nscode the namespace code, representing the prefix and URI of the namespace binding
         * @param position maintains document order among namespace nodes for the same element
         */

        public NamespaceNodeImpl(NodeInfo element, int nscode, int position) {
            this.element = element;
            this.nscode = nscode;
            this.position = position;
            NamePool pool = element.getNamePool();
            String prefix = pool.getPrefixFromNamespaceCode(nscode);
            if ("".equals(prefix)) {
                namecode = -1;
            } else {
                namecode = pool.allocate("", "", prefix);
            }
        }

        /**
         * Get the kind of node. This will be a value such as Type.ELEMENT or Type.ATTRIBUTE
         *
         * @return an integer identifying the kind of node. These integer values are the
         *         same as those used in the DOM
         * @see org.orbeon.saxon.type.Type
         */

        public int getNodeKind() {
            return Type.NAMESPACE;
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
            return other instanceof NamespaceNodeImpl &&
                    element.isSameNodeInfo(((NamespaceNodeImpl)other).element) &&
                    nscode == ((NamespaceNodeImpl)other).nscode;

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

         public int hashCode() {
             return element.hashCode() ^ (position<<13);
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
            return element.getSystemId();
        }

        /**
         * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
         * in the node. This will be the same as the System ID unless xml:base has been used.
         *
         * @return the base URI of the node
         */

        public String getBaseURI() {
            return null;    // the base URI of a namespace node is the empty sequence
        }

        /**
         * Get line number
         *
         * @return the line number of the node in its original source document; or
         *         -1 if not available
         */

        public int getLineNumber() {
            return element.getLineNumber();
        }

        /**
         * Get column number
         *
         * @return the column number of the node in its original source document; or
         *         -1 if not available
         */

        public int getColumnNumber() {
            return element.getColumnNumber();
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
            if (other instanceof NamespaceNodeImpl && element.isSameNodeInfo(((NamespaceNodeImpl)other).element)) {
                // JDK 1.5: return Integer.signum(position - ((NamespaceNodeI)other).position);
                int c = position - ((NamespaceNodeImpl)other).position;
                if (c == 0) return 0;
                if (c < 0) return -1;
                return +1;
            } else if (element.isSameNodeInfo(other)) {
                return +1;
            } else {
                return element.compareOrder(other);
            }
        }

        /**
         * Return the string value of the node. The interpretation of this depends on the type
         * of node. For a namespace node, it is the namespace URI.
         *
         * @return the string value of the node
         */

        public String getStringValue() {
            return element.getNamePool().getURIFromURICode((short)(nscode & 0xffff));
        }

        /**
         * Get the value of the item as a CharSequence. This is in some cases more efficient than
         * the version of the method that returns a String.
         */

        public CharSequence getStringValueCS() {
            return getStringValue();
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
            return namecode;
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
            if (namecode == -1) {
                return -1;
            }
            return namecode & NamePool.FP_MASK;
        }

        /**
         * Get the local part of the name of this node. This is the name after the ":" if any.
         *
         * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
         *         interface, this returns the full name in the case of a non-namespaced name.
         */

        public String getLocalPart() {
            if (namecode == -1) {
                return "";
            } else {
                return element.getNamePool().getLocalName(namecode);
            }
        }

        /**
         * Get the URI part of the name of this node. This is the URI corresponding to the
         * prefix, or the URI of the default namespace if appropriate.
         *
         * @return The URI of the namespace of this node. Since the name of a namespace
         * node is always an NCName (the namespace prefix), this method always returns "".
         */

        public String getURI() {
            return "";
        }

        /**
         * Get the display name of this node. For elements and attributes this is [prefix:]localname.
         * For unnamed nodes, it is an empty string.
         *
         * @return The display name of this node. For a node with no name, return
         *         an empty string.
         */

        public String getDisplayName() {
            return getLocalPart();
        }

        /**
         * Get the prefix of the name of the node. This is defined only for elements and attributes.
         * If the node has no prefix, or for other kinds of node, return a zero-length string.
         *
         * @return The prefix of the name of the node.
         */

        public String getPrefix() {
            return "";
        }

        /**
         * Get the configuration
         */

        public Configuration getConfiguration() {
            return element.getConfiguration();
        }

        /**
         * Get the NamePool that holds the namecode for this node
         *
         * @return the namepool
         */

        public NamePool getNamePool() {
            return element.getNamePool();
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
            return -1;
        }

        /**
         * Get the NodeInfo object representing the parent of this node
         *
         * @return the parent of this node; null if this node has no parent
         */

        public NodeInfo getParent() {
            return element;
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
            switch (axisNumber) {
                case Axis.ANCESTOR:
                    return element.iterateAxis(Axis.ANCESTOR_OR_SELF, nodeTest);

                case Axis.ANCESTOR_OR_SELF:
                    if (nodeTest.matches(this)) {
                        return new PrependIterator(this, element.iterateAxis(Axis.ANCESTOR_OR_SELF, nodeTest));
                    } else {
                        return element.iterateAxis(Axis.ANCESTOR_OR_SELF, nodeTest);
                    }

                case Axis.ATTRIBUTE:
                case Axis.CHILD:
                case Axis.DESCENDANT:
                case Axis.DESCENDANT_OR_SELF:
                case Axis.FOLLOWING_SIBLING:
                case Axis.NAMESPACE:
                case Axis.PRECEDING_SIBLING:
                     return EmptyIterator.getInstance();

                case Axis.FOLLOWING:
                    return new Navigator.AxisFilter(
                            new Navigator.FollowingEnumeration(this),
                            nodeTest);

                case Axis.PARENT:
                    return Navigator.filteredSingleton(element, nodeTest);

                case Axis.PRECEDING:
                    return new Navigator.AxisFilter(
                            new Navigator.PrecedingEnumeration(this, false),
                            nodeTest);

                case Axis.SELF:
                    return Navigator.filteredSingleton(this, nodeTest);

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
         *
         * @param fingerprint The fingerprint of the attribute name
         * @return the attribute value if it exists or null if not
         */

        public String getAttributeValue(int fingerprint) {
            return null;
        }

        /**
         * Get the root node of the tree containing this node
         *
         * @return the NodeInfo representing the top-level ancestor of this node.
         *         This will not necessarily be a document node
         */

        public NodeInfo getRoot() {
            return element.getRoot();
        }

        /**
         * Get the root node, if it is a document node.
         *
         * @return the DocumentInfo representing the containing document. If this
         *         node is part of a tree that does not have a document node as its
         *         root, return null.
         */

        public DocumentInfo getDocumentRoot() {
            return element.getDocumentRoot();
        }

        /**
         * Determine whether the node has any children. <br />
         * Note: the result is equivalent to <br />
         * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
         *
         * @return True if the node has one or more children
         */

        public boolean hasChildNodes() {
            return false;
        }

        /**
         * Get a character string that uniquely identifies this node.
         * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
         *
         * @param buffer buffer to hold a string that uniquely identifies this node, across all
         *         documents.
         */

        public void generateId(FastStringBuffer buffer) {
            element.generateId(buffer);
            buffer.append("n");
            buffer.append(Integer.toString(position));
        }

        /**
         * Get the document number of the document containing this node. For a free-standing
         * orphan node, just return the hashcode.
         */

        public int getDocumentNumber() {
            return element.getDocumentNumber();
        }

        /**
         * Copy this node to a given outputter
         *
         * @param out             the Receiver to which the node should be copied
         * @param whichNamespaces in the case of an element, controls
         *                        which namespace nodes should be copied. Values are {@link #NO_NAMESPACES},
         *                        {@link #LOCAL_NAMESPACES}, {@link #ALL_NAMESPACES}
         * @param copyAnnotations indicates whether the type annotations
         *                        of element and attribute nodes should be copied
         * @param locationId      If non-zero, identifies the location of the instruction
         *                        that requested this copy. If zero, indicates that the location information
         *                        for the original node is to be copied; in this case the Receiver must be
         *                        a LocationCopier
         * @throws org.orbeon.saxon.trans.XPathException
         *
         */

        public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
            out.namespace(nscode, ReceiverOptions.REJECT_DUPLICATES);
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
            // no action: namespace nodes have the same base URI as their parent
        }

        /**
         * Get the typed value of the item
         *
         * @return the typed value of the item. In general this will be a sequence
         * @throws org.orbeon.saxon.trans.XPathException
         *          where no typed value is available, e.g. for
         *          an element with complex content
         */

        public SequenceIterator getTypedValue() throws XPathException {
            return SingletonIterator.makeIterator(new StringValue(getStringValueCS()));
        }

        /**
         * Get the typed value. The result of this method will always be consistent with the method
         * {@link Item#getTypedValue()}. However, this method is often more convenient and may be
         * more efficient, especially in the common case where the value is expected to be a singleton.
         *
         * @return the typed value. If requireSingleton is set to true, the result will always be an
         *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
         *         values.
         * @since 8.5
         */

        public Value atomize() throws XPathException {
            return new StringValue(getStringValueCS());
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
