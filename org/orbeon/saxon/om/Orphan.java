package org.orbeon.saxon.om;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.StringValue;

/**
  * A node (implementing the NodeInfo interface) representing an attribute, text node,
  * comment, processing instruction, or namespace that has no parent (and of course no children).
  * Exceptionally it is also used (during whitespace stripping) to represent a standalone element.
  * @author Michael H. Kay
  */

public final class Orphan implements MutableNodeInfo, FingerprintedNode {

    private short kind;
    private int nameCode = -1;
    private CharSequence stringValue;
    private int typeAnnotation = -1;
    private Configuration config;
    private String systemId;
    private boolean isId;
    private boolean isIdref;

    /**
     * Create an Orphan node
     * @param config the Saxon configuration
     */

    public Orphan(Configuration config) {
        this.config = config;
    }

    /**
     * Set the node kind
     * @param kind the kind of node, for example {@link Type#ELEMENT} or {@link Type#ATTRIBUTE}
     */

    public void setNodeKind(short kind) {
        this.kind = kind;
    }

    /**
     * Set the name of the node
     * @param nameCode the integer code representing the name of the node in the NamePool
     */

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Set the string value of the node
     * @param stringValue the string value of the node
     */

    public void setStringValue(CharSequence stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Set the type annotation of the node
     * @param typeAnnotation the type annotation, and integer code representing the fingerprint of the type name
     */

    public void setTypeAnnotation(int typeAnnotation) {
        this.typeAnnotation = typeAnnotation;
    }

    /**
     * Set the base URI of the node
     * @param systemId the base URI of the node
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Set the isId property
     * @param id the isId property
     */

    public void setIsId(boolean id) {
        this.isId = id;
    }

    /**
     * Set the isIdref property
     * @param idref the isIdref property
     */

    public void setIsIdref(boolean idref) {
        this.isIdref = idref;
    }

    /**
    * Return the kind of node.
    * @return one of the values Type.ELEMENT, Type.TEXT, Type.ATTRIBUTE, etc.
    */

    public int getNodeKind() {
        return kind;
    }

    /**
     * Get the typed value of the node
     * @return an iterator over the items making up the typed value
    */

    public SequenceIterator getTypedValue() throws XPathException {
        switch (getNodeKind()) {
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return SingletonIterator.makeIterator(new StringValue(stringValue));
            case Type.TEXT:
            case Type.DOCUMENT:
            case Type.NAMESPACE:
                return SingletonIterator.makeIterator(new UntypedAtomicValue(stringValue));
            default:
                if (typeAnnotation == -1 || typeAnnotation == StandardNames.XS_UNTYPED ||
                        typeAnnotation == StandardNames.XS_UNTYPED_ATOMIC) {
                    return SingletonIterator.makeIterator(new UntypedAtomicValue(stringValue));
                } else {
                    SchemaType stype = config.getSchemaType(typeAnnotation);
                    if (stype == null) {
                        String typeName = config.getNamePool().getDisplayName(typeAnnotation);
                        throw new IllegalStateException("Unknown type annotation " +
                                Err.wrap(typeName) + " in standalone node");
                    } else {
                        return stype.getTypedValue(this);
                    }
                }
        }
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
        switch (getNodeKind()) {
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return new StringValue(stringValue);
            case Type.TEXT:
            case Type.DOCUMENT:
            case Type.NAMESPACE:
                return new UntypedAtomicValue(stringValue);
            default:
                if (typeAnnotation == -1 || typeAnnotation == StandardNames.XS_UNTYPED ||
                        typeAnnotation == StandardNames.XS_UNTYPED_ATOMIC) {
                    return new UntypedAtomicValue(stringValue);
                } else {
                    SchemaType stype = config.getSchemaType(typeAnnotation);
                    if (stype == null) {
                        String typeName = config.getNamePool().getDisplayName(typeAnnotation);
                        throw new IllegalStateException("Unknown type annotation " +
                                Err.wrap(typeName) + " in standalone node");
                    } else {
                        return stype.atomize(this);
                    }
                }
        }
    }

    /**
     * Get the configuration
     * @return the Saxon configuration object
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the name pool
     */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
    * Get the type annotation
    */

    public int getTypeAnnotation() {
        if (typeAnnotation == -1) {
            if (kind == Type.ELEMENT) {
                return StandardNames.XS_UNTYPED;
            } else if (kind == Type.ATTRIBUTE) {
                return StandardNames.XS_UNTYPED_ATOMIC;
            }
        }
        return typeAnnotation;
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        return this==other;
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
         return super.hashCode();
     }


    /**
    * Get the System ID for the node.
    * @return the System Identifier of the entity in the source document containing the node,
    * or null if not known. Note this is not the same as the base URI: the base URI can be
    * modified by xml:base, but the system ID cannot.
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
    * in the node. This will be the same as the System ID unless xml:base has been used.
    */

    public String getBaseURI() {
        if (kind == Type.PROCESSING_INSTRUCTION) {
            return systemId;
        } else {
            return null;
        }
    }

    /**
    * Get line number
    * @return the line number of the node in its original source document; or -1 if not available
    */

    public int getLineNumber() {
        return -1;
    }

   /**
     * Get column number
     * @return the column number of the node in its original source document; or -1 if not available
     */

    public int getColumnNumber() {
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

        // are they the same node?
        if (this.isSameNodeInfo(other)) {
            return 0;
        }
        return (this.hashCode() < other.hashCode() ? -1 : +1);
    }

    /**
    * Return the string value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
        return stringValue.toString();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return stringValue;
    }

    /**
	* Get name code. The name code is a coded form of the node name: two nodes
	* with the same name code have the same namespace URI, the same local name,
	* and the same prefix. By masking the name code with &0xfffff, you get a
	* fingerprint: two nodes with the same fingerprint have the same local name
	* and namespace URI.
    * @see org.orbeon.saxon.om.NamePool#allocate allocate
	*/

	public int getNameCode() {
        return nameCode;
	}

	/**
	* Get fingerprint. The fingerprint is a coded form of the expanded name
	* of the node: two nodes
	* with the same name code have the same namespace URI and the same local name.
	* A fingerprint of -1 should be returned for a node with no name.
	*/

	public int getFingerprint() {
        if (nameCode == -1) {
            return -1;
        } else {
	        return getNameCode()&0xfffff;
        }
	}

    /**
    * Get the local part of the name of this node. This is the name after the ":" if any.
    * @return the local part of the name. For an unnamed node, returns "".
    */

    public String getLocalPart() {
        if (nameCode == -1) {
            return "";
        } else {
            return config.getNamePool().getLocalName(nameCode);
        }
    }

    /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
    * @return The URI of the namespace of this node. For an unnamed node, return null.
    * For a node with an empty prefix, return an empty string.
    */

    public String getURI() {
        if (nameCode == -1) {
            return "";
        } else {
            return config.getNamePool().getURI(nameCode);
        }
    }

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     *
     * @return The prefix of the name of the node.
     */

    public String getPrefix() {
        if (nameCode == -1) {
            return "";
        } else {
            return config.getNamePool().getPrefix(nameCode);
        }
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        if (nameCode == -1) {
            return "";
        } else {
            return config.getNamePool().getDisplayName(nameCode);
        }
    }

    /**
    * Get the NodeInfo object representing the parent of this node
     * @return null - an Orphan has no parent.
    */

    public NodeInfo getParent() {
        return null;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be searched, e.g. Axis.CHILD or Axis.ANCESTOR
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ANCESTOR_OR_SELF:
            case Axis.DESCENDANT_OR_SELF:
            case Axis.SELF:
                return SingleNodeIterator.makeIterator(this);
            case Axis.ANCESTOR:
            case Axis.ATTRIBUTE:
            case Axis.CHILD:
            case Axis.DESCENDANT:
            case Axis.FOLLOWING:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PARENT:
            case Axis.PRECEDING:
            case Axis.PRECEDING_SIBLING:
            case Axis.PRECEDING_OR_ANCESTOR:
                return EmptyIterator.getInstance();
            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }


    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be searched, e.g. Axis.CHILD or Axis.ANCESTOR
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        switch (axisNumber) {
            case Axis.ANCESTOR_OR_SELF:
            case Axis.DESCENDANT_OR_SELF:
            case Axis.SELF:
                return Navigator.filteredSingleton(this, nodeTest);
            case Axis.ANCESTOR:
            case Axis.ATTRIBUTE:
            case Axis.CHILD:
            case Axis.DESCENDANT:
            case Axis.FOLLOWING:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PARENT:
            case Axis.PRECEDING:
            case Axis.PRECEDING_SIBLING:
            case Axis.PRECEDING_OR_ANCESTOR:
                return EmptyIterator.getInstance();
            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
        return null;
    }

    /**
    * Get the root node of this tree (not necessarily a document node).
    * Always returns this node in the case of an Orphan node.
    */

    public NodeInfo getRoot() {
        return this;
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document, or null if the
    * node is not part of a document. Always null for an Orphan node.
    */

    public DocumentInfo getDocumentRoot() {
        return null;
    }

    /**
    * Determine whether the node has any children.
    * @return false - an orphan node never has any children
    */

    public boolean hasChildNodes() {
        return false;
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     * @param buffer a buffer, into which will be placed
     * a string that uniquely identifies this node, within this
     * document. The calling code prepends information to make the result
     * unique across all documents.
     */

    public void generateId(FastStringBuffer buffer) {
        buffer.append('Q');
        buffer.append(Integer.toString(hashCode()));
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return hashCode() & 0xffffff;
        // lose the top bits because we need to subtract these values for comparison
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        Navigator.copy(this, out, config.getNamePool(), whichNamespaces, copyAnnotations, locationId);
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
     * Determine whether this node has the is-id property
     *
     * @return true if the node is an ID
     */

    public boolean isId() {
        return isId || (kind == Type.ATTRIBUTE && (nameCode & NamePool.FP_MASK) == StandardNames.XML_ID);
    }

    /**
     * Determine whether this node has the is-idref property
     *
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        return isIdref;
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
     * Insert copies of a sequence of nodes as children of this node.
     * <p/>
     * <p>This method takes no action unless the target node is a document node or element node. It also
     * takes no action in respect of any supplied nodes that are not elements, text nodes, comments, or
     * processing instructions.</p>
     * <p/>
     * <p>The supplied nodes will be copied to form the new children. Adjacent text nodes will be merged, and
     * zero-length text nodes removed.</p>
     * @param source  the nodes to be inserted
     * @param atStart true if the new nodes are to be inserted before existing children; false if they are
     * @param inherit true if the insert nodes are to inherit the namespaces of their new parent; false
     *                if such namespaces are to be undeclared
     */

    public void insertChildren(NodeInfo[] source, boolean atStart, boolean inherit) {
        // no action: node is not a document or element node
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
     * @param source  the nodes to be inserted
     * @param before  true if the new nodes are to be inserted before the target node; false if they are
     * @param inherit true if the insert nodes are to inherit the namespaces of their new parent; false
     *                if such namespaces are to be undeclared
     */

    public void insertSiblings(NodeInfo[] source, boolean before, boolean inherit) {
        // no action: node has no parent
    }

    /**
     * Remove an attribute from this element node
     * <p/>
     * <p>If this node is not an element, or if it has no attribute with the specified name,
     * this method takes no action.</p>
     * <p/>
     * <p>The attribute node itself is not modified in any way.</p>
     * @param nameCode the name of the attribute to be removed
     */

    public void removeAttribute(int nameCode) {
        // no action: node is not an element
    }

    /**
     * Add an attribute to this element node.
     * <p/>
     * <p>If this node is not an element, or if the supplied node is not an attribute, the method
     * takes no action. If the element already has an attribute with this name, the existing attribute
     * is replaced.</p>
     * @param nameCode   the name of the new attribute
     * @param typeCode   the type annotation of the new attribute
     * @param value      the string value of the new attribute
     * @param properties properties including IS_ID and IS_IDREF properties
     */

    public void putAttribute(int nameCode, int typeCode, CharSequence value, int properties) {
        // no action: node is not an element
    }

    /**
     * Delete this node (that is, detach it from its parent).
     * <p>If this node has preceding and following siblings that are both text nodes,
     * the two text nodes will be joined into a single text node (the identity of this node
     * with respect to its predecessors is undefined).</p>
     */

    public void delete() {
        // no action: node has no parent from which it can be detached
    }

    /**
     * Replace this node with a given sequence of nodes
     * @param replacement the replacement nodes
     * @param inherit     true if the replacement nodes are to inherit the namespaces of their new parent; false
     *                    if such namespaces are to be undeclared
     * @throws IllegalArgumentException if any of the replacement nodes is of the wrong kind. When replacing
     *                                  a child node, the replacement nodes must all be elements, text, comment, or PI nodes; when replacing
     *                                  an attribute, the replacement nodes must all be attributes.
     */

    public void replace(NodeInfo[] replacement, boolean inherit) {
        // no action, node has no parent
    }

    /**
     * Replace the string-value of this node. If applied to an element or document node, this
     * causes all existing children to be deleted, and replaced with a new text node
     * whose string value is the value supplied. The caller is responsible for checking
     * that the value is valid, for example that comments do not contain a double hyphen; the
     * implementation is not required to check for such conditions.
     * @param stringValue the new string value
     */

    public void replaceStringValue(CharSequence stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Rename this node.
     * <p>This call has no effect if applied to a nameless node, such as a text node or comment.</p>
     * <p>If necessary, a new namespace binding will be added to the target element, or to the element
     * parent of the target attribute</p>
     * @param newNameCode the namecode of the new name in the name pool
     * @throws IllegalArgumentException if the new name code is not present in the name pool, or if
     *         it has a (prefix, uri) pair in which the
     *         prefix is the same as that of an existing in-scope namespace binding and the uri is different from that
     *         namespace binding.
     */

    public void rename(int newNameCode) {
        if (kind==Type.ATTRIBUTE || kind==Type.PROCESSING_INSTRUCTION) {
            nameCode = newNameCode;
        }
    }

    /**
     * Add a namespace binding (that is, a namespace node) to this element. This call has no effect if applied
     * to a node other than an element.
     * @param nscode  The namespace code representing the (prefix, uri) pair of the namespace binding to be
     *                added. If the target element already has a namespace binding with this (prefix, uri) pair, the call has
     *                no effect. If the target element currently has a namespace binding with this prefix and a different URI, an
     *                exception is raised.
     * @param inherit If true, the new namespace binding will be inherited by any children of the target element
     *                that do not already have a namespace binding for the specified prefix, recursively.
     *                If false, the new namespace binding will not be inherited.
     * @throws IllegalArgumentException if the namespace code is not present in the namepool, or if the target
     *                                  element already has a namespace binding for this prefix
     */

    public void addNamespace(int nscode, boolean inherit) {
        // no action: node is not an element
    }

    /**
     * Remove type information from this node (and its ancestors, recursively).
     * This method implements the upd:removeType() primitive defined in the XQuery Update specification.
     * (Note: the caller is responsible for updating the set of nodes marked for revalidation)
     */

    public void removeTypeAnnotation() {
        typeAnnotation = StandardNames.XS_UNTYPED_ATOMIC;
    }

    /**
     * Get a Builder suitable for building nodes that can be attached to this document.
     * This implementation always throws an exception: the method should only be called on a document or element
     * node when creating new children.
     */

    public Builder newBuilder() {
        throw new UnsupportedOperationException("Cannot create children for an Orphan node");
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
// The Initial Developer of the Original Code is
// Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
