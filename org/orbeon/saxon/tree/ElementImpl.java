package net.sf.saxon.tree;
import net.sf.saxon.event.LocationCopier;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
  * ElementImpl implements an element with no attributes or namespace declarations.<P>
  * This class is an implementation of NodeInfo. For elements with attributes or
  * namespace declarations, class ElementWithAttributes is used.
  * @author Michael H. Kay
  */


public class ElementImpl extends ParentNodeImpl {

    private static final AttributeCollectionImpl emptyAtts = new AttributeCollectionImpl(null);

    protected int nameCode;
    protected DocumentImpl root;

    /**
    * Construct an empty ElementImpl
    */

    public ElementImpl() {}

    /**
    * Set the name code. Used when creating a dummy element in the Stripper
    */

    public void setNameCode(int nameCode) {
    	this.nameCode = nameCode;
    }

    /**
     * Initialise a new ElementImpl with an element name
     * @param nameCode  Integer representing the element name, with namespaces resolved
     * @param atts The attribute list: always null
     * @param parent  The parent node
     * @param baseURI  The base URI of the new element
     * @param lineNumber  The line number of the element in the source document
     * @param sequenceNumber  Integer identifying this element within the document
     */

    public void initialise(int nameCode, AttributeCollectionImpl atts, NodeInfo parent,
                            String baseURI, int lineNumber, int sequenceNumber) {
        this.nameCode = nameCode;
        this.parent = (ParentNodeImpl)parent;
        this.sequence = sequenceNumber;
        this.root = (DocumentImpl)parent.getDocumentRoot();
        root.setLineNumber(sequenceNumber, lineNumber);
        root.setSystemId(sequenceNumber, baseURI);
    }

    /**
    * Set the system ID of this node. This method is provided so that a NodeInfo
    * implements the javax.xml.transform.Source interface, allowing a node to be
    * used directly as the Source of a transformation
    */

    public void setSystemId(String uri) {
        root.setSystemId(sequence, uri);
    }

	/**
	* Get the root node
	*/

	public NodeInfo getRoot() {
		return root;
	}

	/**
	* Get the root document node
	*/

	public DocumentInfo getDocumentRoot() {
		return root;
	}

    /**
    * Get the system ID of the entity containing this element node.
    */

    public final String getSystemId() {
        return ((DocumentImpl)getDocumentRoot()).getSystemId(sequence);
    }

    /**
    * Get the base URI of this element node. This will be the same as the System ID unless
    * xml:base has been used.
    */

    public String getBaseURI() {
        return Navigator.getBaseURI(this);
    }

    /**
    * Set the line number of the element within its source document entity
    */

    public void setLineNumber(int line) {
        ((DocumentImpl)getDocumentRoot()).setLineNumber(sequence, line);
    }

    /**
    * Get the line number of the node within its source document entity
    */

    public int getLineNumber() {
        return ((DocumentImpl)getDocumentRoot()).getLineNumber(sequence);
    }


	/**
	* Get the nameCode of the node. This is used to locate the name in the NamePool
	*/

	public int getNameCode() {
		return nameCode;
	}

    /**
    * Get a character string that uniquely identifies this node
    * @return a string.
    */

    public String generateId() {
        return getDocumentRoot().generateId() + 'e' + sequence;
    }

    /**
    * Output all namespace nodes associated with this element.
    * @param out The relevant outputter
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors) throws XPathException {

        // just add the namespaces defined on the ancestor nodes. We rely on the outputter
        // to eliminate multiple declarations of the same prefix

        if (includeAncestors) {
            if (!(parent instanceof DocumentInfo)) {
                parent.sendNamespaceDeclarations(out, true);
            }
        }
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
        return EMPTY_NAMESPACE_LIST;
    }


    /**
    * Return the type of node.
    * @return Type.ELEMENT
    */

    public final int getNodeKind() {
        return Type.ELEMENT;
    }

    /**
    * Get the attribute list for this element.
    * @return The attribute list. This will not include any
    * namespace attributes. The attribute names will be in expanded form, with prefixes
    * replaced by URIs
    */

    public AttributeCollection getAttributeList() {
        return emptyAtts;
    }

    /**
     *  Find the value of a given attribute of this element. <BR>
     *  This is a short-cut method; the full capability to examine
     *  attributes is offered via the getAttributeList() method. <BR>
     *  The attribute may either be one that was present in the original XML document,
     *  or one that has been set by the application using setAttribute(). <BR>
     *  @param name the name of an attribute. There must be no prefix in the name.
     *  @return the value of the attribute, if it exists, otherwise null
     */

//    public String getAttributeValue( String name ) {
//        return null;
//    }


    /**
    * Copy this node to a given outputter (supporting xsl:copy-of)
    * @param out The outputter
    * @param whichNamespaces indicates which namespaces should be output: all, none, or local
    * namespaces only (those not declared on the parent element)
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {

        int typeCode = (copyAnnotations ? getTypeAnnotation() : -1);
        if (locationId == 0 && out instanceof LocationCopier) {
            out.setSystemId(getSystemId());
            ((LocationCopier)out).setLineNumber(getLineNumber());
        }
        out.startElement(getNameCode(), typeCode, locationId, 0);

        // output the namespaces

        if (whichNamespaces != NO_NAMESPACES) {
            sendNamespaceDeclarations(out, whichNamespaces==ALL_NAMESPACES);
        }

        // output the children

        int childNamespaces = (whichNamespaces==NO_NAMESPACES ? NO_NAMESPACES : LOCAL_NAMESPACES);
        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            next.copy(out, childNamespaces, copyAnnotations, locationId);
            next = (NodeImpl)next.getNextSibling();
        }

        out.endElement();
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
