package net.sf.saxon.om;
import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;

/**
  * A node (implementing the NodeInfo interface) representing an attribute, text node,
  * comment, processing instruction, or namespace that has no parent (and of course no children).
  * Exceptionally it is also used (during whitespace stripping) to represent a standalone element.
  * @author Michael H. Kay
  */

public final class Orphan implements NodeInfo {

    private short kind;
    private int nameCode = -1;
    private CharSequence stringValue;
    private int typeAnnotation = -1;
    private Configuration config;
    private String systemId;

    public Orphan(Configuration config) {
        this.config = config;
    }

    public void setNodeKind(short kind) {
        this.kind = kind;
    }

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    public void setStringValue(CharSequence stringValue) {
        this.stringValue = stringValue;
    }

    public void setTypeAnnotation(int typeAnnotation) {
        this.typeAnnotation = typeAnnotation;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Return the type of node.
    * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
    */

    public int getNodeKind() {
        return kind;
    }

    /**
    * Get the typed value of the item
    */

    public SequenceIterator getTypedValue() throws XPathException {
        if (typeAnnotation == -1) {
            return SingletonIterator.makeIterator(
                    new UntypedAtomicValue(stringValue));
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

    /**
     * Get the configuration
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
    * @see net.sf.saxon.om.NamePool#allocate allocate
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
                return SingletonIterator.makeIterator(this);
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
                if (nodeTest.matches(this)) {
                    return SingletonIterator.makeIterator(this);
                } else {
                    return EmptyIterator.getInstance();
                }
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
    * Determine whether the node has any children. <br />
    * Note: the result is equivalent to <br />
    * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
    */

    public boolean hasChildNodes() {
        return false;
    }

    /**
    * Get a character string that uniquely identifies this node.
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return a string that uniquely identifies this node, within this
    * document. The calling code prepends information to make the result
    * unique across all documents.
    */

    public String generateId() {
        return "Q" + hashCode();
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
    * Output all namespace nodes associated with this element. Does nothing if
    * the node is not an element.
    * @param out The relevant outputter
     * @param includeAncestors True if namespaces declared on ancestor elements must
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors) {
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
