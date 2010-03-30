package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;


/**
  * A node in the XML parse tree representing an attribute. Note that this is
  * generated only "on demand", when the attribute is selected by a select pattern.<P>
  * @author Michael H. Kay
  */

final class TinyAttributeImpl extends TinyNodeImpl {

    public TinyAttributeImpl(TinyTree tree, int nodeNr) {
        this.tree = tree;
        this.nodeNr = nodeNr;
    }


    public void setSystemId(String uri) {
        // no action: an attribute has the same base URI as its parent
    }

    /**
    * Get the parent node
    */

    public NodeInfo getParent() {
        return tree.getNode(tree.attParent[nodeNr]);
    }

    /**
     * Get the root node of the tree (not necessarily a document node)
     *
     * @return the NodeInfo representing the root of this tree
     */

    public NodeInfo getRoot() {
        NodeInfo parent = getParent();
        if (parent == null) {
            return this;    // doesn't happen - parentless attributes are represented by the Orphan class
        } else {
            return parent.getRoot();
        }
    }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In this implementation, elements have a zero
    * least-significant word, while attributes and namespaces use the same value in the top word as
    * the containing element, and use the bottom word to hold
    * a sequence number, which numbers namespaces first and then attributes.
    */

    protected long getSequenceNumber() {
        return
            ((TinyNodeImpl)getParent()).getSequenceNumber()
            + 0x8000 +
            (nodeNr - tree.alpha[tree.attParent[nodeNr]]);
        // note the 0x8000 is to leave room for namespace nodes
    }

    /**
    * Return the type of node.
    * @return Node.ATTRIBUTE
    */

    public final int getNodeKind() {
        return Type.ATTRIBUTE;
    }

    /**
    * Return the string value of the node.
    * @return the attribute value
    */

    public CharSequence getStringValueCS() {
        return tree.attValue[nodeNr];
    }

    /**
    * Return the string value of the node.
    * @return the attribute value
    */

    public String getStringValue() {
        return tree.attValue[nodeNr].toString();
    }

	/**
	* Get the fingerprint of the node, used for matching names
	*/

	public int getFingerprint() {
		return tree.attCode[nodeNr] & 0xfffff;
	}

	/**
	* Get the name code of the node, used for finding names in the name pool
	*/

	public int getNameCode() {
		return tree.attCode[nodeNr];
	}

    /**
    * Get the prefix part of the name of this node. This is the name before the ":" if any.
    * @return the prefix part of the name. For an unnamed node, return null.
    */

    public String getPrefix() {
    	int code = tree.attCode[nodeNr];
    	if (NamePool.getPrefixIndex(code) == 0) return "";
    	return tree.getNamePool().getPrefix(code);
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        return tree.getNamePool().getDisplayName(tree.attCode[nodeNr]);
    }


    /**
    * Get the local name of this node.
    * @return The local name of this node.
    * For a node with no name, return an empty string.
    */

    public String getLocalPart() {
        return tree.getNamePool().getLocalName(tree.attCode[nodeNr]);
    }

    /**
    * Get the URI part of the name of this node.
    * @return The URI of the namespace of this node. For the default namespace, return an
    * empty string
    */

    public final String getURI() {
        return tree.getNamePool().getURI(tree.attCode[nodeNr]);
    }

    /**
    * Get the type annotation of this node, if any
    * The bit {@link NodeInfo#IS_DTD_TYPE} (1<<30) will be set in the case of an attribute node if the type annotation
    * is one of ID, IDREF, or IDREFS and this is derived from DTD rather than schema validation.
    * Returns UNTYPED_ATOMIC if there is no type annotation
    */

    public int getTypeAnnotation() {
        return tree.getAttributeAnnotation(nodeNr);
    }

    /**
    * Generate id. Returns key of owning element with the attribute namecode as a suffix
     * @param buffer Buffer to contain the generated ID value
     */

    public void generateId(FastStringBuffer buffer) {
        getParent().generateId(buffer);
        buffer.append("a");
        buffer.append(Integer.toString(tree.attCode[nodeNr]));
        // we previously used the attribute name. But this breaks the requirement
        // that the result of generate-id consists entirely of alphanumeric ASCII
        // characters
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
		int nameCode = tree.attCode[nodeNr];
		int typeCode = (copyAnnotations ? getTypeAnnotation() : -1);
        out.attribute(nameCode, typeCode, getStringValue(), locationId, 0);
    }

    /**
    * Get the line number of the node within its source document entity
    */

    public int getLineNumber() {
        return getParent().getLineNumber();
    }

    /**
    * Get the column number of the node within its source document entity
    */

    public int getColumnNumber() {
        return getParent().getColumnNumber();
    }

    /**
     * Determine whether the node has the is-nilled property
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return false;
    }

    /**
     * Determine whether this node has the is-id property
     *
     * @return true if the node is an ID
     */

    public boolean isId() {
        return tree.isIdAttribute(nodeNr);
    }

    /**
     * Determine whether this node has the is-idref property
     *
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        return tree.isIdrefAttribute(nodeNr);
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
