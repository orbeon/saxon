package org.orbeon.saxon.dom;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.xpath.XPathException;

/**
  * A node (implementing the NodeInfo interface) representing a Namespace node
 * in the DOM wrapping interface
  */

public class NamespaceNode implements NodeInfo {

    private NodeWrapper parent;
    private String prefix;
    private String uri;

    public NamespaceNode(NodeWrapper parent, String prefix, String uri) {
        this.parent = parent;
        this.prefix = prefix;
        this.uri = uri;
    }

    public void setSystemId(String sysId) {
        // needed to implement Source interface
    }

    /**
    * Return the type of node.
    * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
    */

    public int getNodeKind() {
        return Type.NAMESPACE;
    }

    /**
    * Get the typed value of the item
    */

    public SequenceIterator getTypedValue(Configuration config) {
        return SingletonIterator.makeIterator(new UntypedAtomicValue(uri));
    }

    /**
     * Get the name pool
     */

    public NamePool getNamePool() {
        return getParent().getNamePool();
    }

    /**
    * Get the type annotation
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
        if (!(other instanceof NamespaceNode)) {
            return false;
        }
        if (!((NamespaceNode)other).prefix.equals(this.prefix)) {
            return false;
        }
        if (!(other.getParent().isSameNodeInfo(this.getParent()))) {
            return false;
        }
        return true;
    }

    /**
    * Get the System ID for the node.
    * @return the System Identifier of the entity in the source document containing the node,
    * or null if not known. Note this is not the same as the base URI: the base URI can be
    * modified by xml:base, but the system ID cannot.
    */

    public String getSystemId() {
        return null;
    }

    /**
    * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
    * in the node. This will be the same as the System ID unless xml:base has been used.
    */

    public String getBaseURI() {
        return null;
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
        if (getParent().isSameNodeInfo(other.getParent())) {
            if (other instanceof NamespaceNode) {
                return getDisplayName().compareTo(other.getDisplayName());
            } else {
                return -1;
            }
        }
        return (getParent().compareOrder(other));
    }

    /**
    * Return the string value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
        return uri;
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
        return getNamePool().allocate("", "", prefix);
	}

	/**
	* Get fingerprint. The fingerprint is a coded form of the expanded name
	* of the node: two nodes
	* with the same name code have the same namespace URI and the same local name.
	* A fingerprint of -1 should be returned for a node with no name.
	*/

	public int getFingerprint() {
	    return getNameCode()&0xfffff;
	}

    /**
    * Get the local part of the name of this node. This is the name after the ":" if any.
    * @return the local part of the name. For an unnamed node, returns "".
    */

    public String getLocalPart() {
        return prefix;
    }

    /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
    * @return The URI of the namespace of this node. For an unnamed node, return an empty string.
    * For a node with an empty prefix, return an empty string.
    */

    public String getURI() {
        return "";
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        return prefix;
    }

    /**
    * Get the NodeInfo object representing the parent of this node
    */

    public NodeInfo getParent() {
        return parent;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be searched, e.g. Axis.CHILD or Axis.ANCESTOR
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    public AxisIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ANCESTOR_OR_SELF:
                return new Navigator.AncestorEnumeration(this, true);
            case Axis.DESCENDANT_OR_SELF:
            case Axis.SELF:
                return SingletonIterator.makeIterator(this);
            case Axis.PARENT:
                return SingletonIterator.makeIterator(parent);
            case Axis.ANCESTOR:
                return parent.iterateAxis(Axis.ANCESTOR_OR_SELF);
            case Axis.PRECEDING_OR_ANCESTOR:
                return new Navigator.PrecedingEnumeration(this, true);
            case Axis.FOLLOWING:
                return new Navigator.FollowingEnumeration(this);
            case Axis.PRECEDING:
                return new Navigator.PrecedingEnumeration(this, false);
            case Axis.ATTRIBUTE:
            case Axis.CHILD:
            case Axis.DESCENDANT:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PRECEDING_SIBLING:
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
        return new Navigator.AxisFilter(iterateAxis(axisNumber), nodeTest);
    }

    /**
     * Find the value of a given attribute of this node. <BR>
     * This method is defined on all nodes to meet XSL requirements, but for nodes
     * other than elements it will always return null.
     * @param uri the namespace uri of an attribute ("" if no namespace)
     * @param localName the local name of the attribute
     * @return the value of the attribute, if it exists, otherwise null
     */

//    public String getAttributeValue(String uri, String localName) {
//        return null;
//    }

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
        return parent.getRoot();
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document, or null if the
    * node is not part of a document. Always null for an Orphan node.
    */

    public DocumentInfo getDocumentRoot() {
        return parent.getDocumentRoot();
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
        return parent.generateId() + 'n' + prefix.hashCode();
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public int getDocumentNumber() {
        return parent.getDocumentNumber();
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        Navigator.copy(this, out, getNamePool(), whichNamespaces, copyAnnotations, locationId);
    }

    /**
    * Output all namespace nodes associated with this element. Does nothing if
    * the node is not an element.
    * @param out The relevant outputter
    * @param includeAncestors True if namespaces declared on ancestor elements must
    * be output; false if it is known that these are already on the result tree
    */

    public void outputNamespaceNodes(Receiver out, boolean includeAncestors) {
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
