package org.orbeon.saxon.tree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;

/**
  * A node in the "linked" tree representing an attribute. Note that this is
  * generated only "on demand", when the attribute is selected by a path expression.<P>
  * @author Michael H. Kay
  */

final class AttributeImpl extends NodeImpl {

    private int nameCode;
    private int typeCode;
    private String value;

    /**
    * Construct an Attribute node for the n'th attribute of a given element
    * @param element The element containing the relevant attribute
    * @param index The index position of the attribute starting at zero
    */

    public AttributeImpl(ElementImpl element, int index) {
        parent = element;
        this.index = index;
        AttributeCollection atts = element.getAttributeList();
        this.nameCode = atts.getNameCode(index);
        this.value = atts.getValue(index);
        this.typeCode = atts.getTypeAnnotation(index);
    }

	/**
	* Get the name code, which enables the name to be located in the name pool
	*/

	public int getNameCode() {
		return nameCode;
	}

    /**
     * Get the type annotation of this node, if any
     */

    public int getTypeAnnotation() {
        return typeCode;
    }

    /**
     * Determine whether this node has the is-id property
     * @return true if the node is an ID
     */

    public boolean isId() {
        if (getFingerprint() == StandardNames.XML_ID) {
            return true;
        }
        TypeHierarchy th = getConfiguration().getTypeHierarchy();
        return th.isIdCode(typeCode);
    }

    /**
     * Determine whether this node has the is-idref property
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        TypeHierarchy th = getConfiguration().getTypeHierarchy();
        return th.isIdrefsCode(typeCode);
    }

    /**
     * Determine whether the node has the is-nilled property
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return false;  
    }

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof AttributeImpl)) return false;
        if (this==other) return true;
        AttributeImpl otherAtt = (AttributeImpl)other;
        return (parent.isSameNodeInfo(otherAtt.parent) &&
        		 ((nameCode&0xfffff)==(otherAtt.nameCode&0xfffff)));
    }

     /**
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

     public int hashCode() {
         return parent.hashCode() ^ getFingerprint();
     }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    */

    protected long getSequenceNumber() {
        long parseq = parent.getSequenceNumber();
        return (parseq == -1L ? parseq : parseq + 0x8000 + index);
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
    * Return the character value of the node.
    * @return the attribute value
    */

    public String getStringValue() {
        return value;
    }

    /**
    * Get next sibling - not defined for attributes
    */

    public NodeInfo getNextSibling() {
        return null;
    }

    /**
    * Get previous sibling - not defined for attributes
    */

    public NodeInfo getPreviousSibling() {
        return null;
    }

    /**
    * Get the previous node in document order (skipping attributes)
    */

    public NodeImpl getPreviousInDocument() {
        return (NodeImpl)getParent();
    }

    /**
    * Get the next node in document order (skipping attributes)
    */

    public NodeImpl getNextInDocument(NodeImpl anchor) {
        if (anchor==this) return null;
        return ((NodeImpl)getParent()).getNextInDocument(anchor);
    }

    /**
     * Get sequential key. Returns key of owning element with the attribute index as a suffix
     * @param buffer a buffer to which the generated ID will be written
     */

    public void generateId(FastStringBuffer buffer) {
        getParent().generateId(buffer);
        buffer.append('a');
        buffer.append(Integer.toString(index));
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
		int nameCode = getNameCode();
    	int typeCode = (copyAnnotations ? getTypeAnnotation() : -1);
        out.attribute(nameCode, typeCode, getStringValue(), locationId, 0);
    }

    /**
     * Delete this node (that is, detach it from its parent)
     */

    public void delete() {
        if (parent != null) {
            ((ElementImpl)parent).removeAttribute(getNameCode());
        }
        parent = null;
        // TODO: allow for the fact that transiently during an update operation, several attributes may have the same
        // name. 
    }


    /**
     * Replace this node with a given sequence of nodes
     * @param replacement the replacement nodes (which for this version of the method mut be attribute
     * nodes). The target attribute node is deleted, and the replacement nodes are added to the
     * parent element; if they have the same names as existing nodes, then the existing nodes will be
     * overwritten.
     * @param inherit set to true if new child elements are to inherit the in-scope namespaces
     * of their new parent. Not used when replacing attribute nodes.
     * @throws IllegalArgumentException if any of the replacement nodes is not an attribute
     */

    public void replace(NodeInfo[] replacement, boolean inherit) {
        ParentNodeImpl element = parent;
        delete();
        for (int i=0; i<replacement.length; i++) {
            NodeInfo n = replacement[i];
            if (n.getNodeKind() != Type.ATTRIBUTE) {
                throw new IllegalArgumentException("Replacement nodes must be attributes");
            }
            element.putAttribute(n.getNameCode(), StandardNames.XS_UNTYPED_ATOMIC, n.getStringValue(), 0);
        }
    }

    /**
     * Rename this node
     *
     * @param newNameCode the NamePool code of the new name
     */

    public void rename(int newNameCode) {
        // The attribute node itself is transient; we need to update the attribute collection held in the parent
        if (parent != null) {
            AttributeCollectionImpl atts = (AttributeCollectionImpl)((ElementImpl)parent).getAttributeList();
            atts.renameAttribute(nameCode, newNameCode);
            if ((newNameCode>>20) != 0) {
                // new attribute name is in a namespace
                int nscode = getNamePool().getNamespaceCode(newNameCode);
                int prefixCode = nscode>>16 & 0xffff;
                short uc = ((ElementImpl)parent).getURICodeForPrefixCode(prefixCode);
                if (uc == -1) {
                    parent.addNamespace(nscode, false);
                } else if (uc != (nscode&0xffff)) {
                    throw new IllegalArgumentException(
                            "Namespace binding of new name conflicts with existing namespace binding");
                }
            }
        }
        nameCode = newNameCode;
    }

    public void replaceStringValue(CharSequence stringValue) {
        value = stringValue.toString();
        // The attribute node itself is transient; we need to update the attribute collection held in the parent
        if (parent != null) {
            AttributeCollectionImpl atts = (AttributeCollectionImpl)((ElementImpl)parent).getAttributeList();
            atts.replaceAttribute(nameCode, stringValue);
        }
    }


    /**
     * Remove type information from this node (and its ancestors, recursively).
     * This method implements the upd:removeType() primitive defined in the XQuery Update specification
     *
     */

    public void removeTypeAnnotation() {
        typeCode = StandardNames.XS_UNTYPED_ATOMIC;
        if (parent != null) {
            AttributeCollectionImpl atts = (AttributeCollectionImpl)((ElementImpl)parent).getAttributeList();
            atts.setTypeAnnotation(nameCode, StandardNames.XS_UNTYPED_ATOMIC);
            parent.removeTypeAnnotation();
        }
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
        this.typeCode = typeCode;
        if (parent != null) {
            AttributeCollectionImpl atts = (AttributeCollectionImpl)((ElementImpl)parent).getAttributeList();
            atts.setTypeAnnotation(nameCode, typeCode);
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
