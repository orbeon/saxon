package net.sf.saxon.tree;
import net.sf.saxon.event.LocationCopier;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;

/**
  * A node in the XML parse tree representing an XML element.<P>
  * This class is an implementation of NodeInfo
  * @author Michael H. Kay
  * @version 8 August 2000: separated from ElementImpl
  */

// The name of the element and its attributes are now namespace-resolved by the
// parser. However, this class retains the ability to do namespace resolution for other
// names, for example variable and template names in a stylesheet.

public class ElementWithAttributes extends ElementImpl
    implements Element, NamedNodeMap {

    protected AttributeCollection attributeList;      // this excludes namespace attributes
    protected int[] namespaceList = null;             // list of namespace codes
            // note that this namespace list includes only the namespaces actually defined on
            // this element, not those inherited from outer elements.


    /**
    * Initialise a new ElementWithAttributes with an element name and attribute list
    * @param nameCode The element name, with namespaces resolved
    * @param atts The attribute list, after namespace processing
    * @param parent The parent node
    */

    public void initialise(int nameCode, AttributeCollection atts, NodeInfo parent,
                            String baseURI, int lineNumber, int sequenceNumber) {
        this.nameCode = nameCode;
        this.attributeList = atts;
        this.parent = (ParentNodeImpl)parent;
        this.sequence = sequenceNumber;
        this.root = (DocumentImpl)parent.getDocumentRoot();
        root.setLineNumber(sequenceNumber, lineNumber);
        root.setSystemId(sequenceNumber, baseURI);
    }

    /**
    * Set the namespace declarations for the element
    */

    public void setNamespaceDeclarations(int[] namespaces, int namespacesUsed) {
        namespaceList = new int[namespacesUsed];
        System.arraycopy(namespaces, 0, namespaceList, 0, namespacesUsed);
    }


    /**
    * Search the NamespaceList for a given prefix, returning the corresponding URI.
    * @param prefix The prefix to be matched. To find the default namespace, supply ""
    * @return The URI code corresponding to this namespace. If it is an unnamed default namespace,
    * return Namespace.NULL_CODE.
    * @throws NamespaceException if the prefix has not been declared on this NamespaceList.
    */

    public short getURICodeForPrefix(String prefix) throws NamespaceException {
        if (prefix.equals("xml")) return NamespaceConstant.XML_CODE;

		NamePool pool = getNamePool();
		int prefixCode = pool.getCodeForPrefix(prefix);
		if (prefixCode==-1) {
		    throw new NamespaceException(prefix);
		}
		return getURICodeForPrefixCode(prefixCode);
    }

    private short getURICodeForPrefixCode(int prefixCode) throws NamespaceException {
        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
                if ((namespaceList[i]>>16) == prefixCode) {
                    return (short)(namespaceList[i] & 0xffff);
                }
            }
        }
        NodeImpl next = parent;
        while (true) {
	        if (next.getNodeKind()==Type.DOCUMENT) {
	        	// prefixCode==0 represents the empty namespace prefix ""
	            if (prefixCode==0) return NamespaceConstant.NULL_CODE;
	            throw new NamespaceException(getNamePool().getPrefixFromNamespaceCode(prefixCode<<16));
	        } else if (next instanceof ElementWithAttributes) {
	            return ((ElementWithAttributes)next).getURICodeForPrefixCode(prefixCode);
	        } else {
	        	next = (NodeImpl)next.getParentNode();
	        }
	    }
	}

    /**
    * Search the NamespaceList for a given URI, returning the corresponding prefix.
    * @param uri The URI to be matched.
    * @return The prefix corresponding to this URI. If not found, return null. If there is
    * more than one prefix matching the URI, the first one found is returned. If the URI matches
    * the default namespace, return an empty string.
    */

    public String getPrefixForURI(String uri) {
        if (uri.equals(NamespaceConstant.XML)) return "xml";

		NamePool pool = getNamePool();
		int uriCode = pool.getCodeForURI(uri);
		if (uriCode<0) return null;
		return getPrefixForURICode(uriCode);
	}

	private String getPrefixForURICode(int code) {
        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
                if ((namespaceList[i] & 0xffff) == code) {
                    return getNamePool().getPrefixFromNamespaceCode(namespaceList[i]);
                }
            }
        }
        NodeImpl next = parent;
        while (true) {
	        if (next instanceof DocumentInfo) {
	            return null;
	        } else if (next instanceof ElementWithAttributes) {
	            return ((ElementWithAttributes)next).getPrefixForURICode(code);
	        } else {
	        	next = (NodeImpl)next.getParentNode();
	        }
	    }
    }

    /**
    * Make the set of all namespace nodes associated with this element.
    * @param owner The element owning these namespace nodes.
    * @param list a List containing NamespaceImpl objects representing the namespaces
    * in scope for this element; the method appends nodes to this List, which should
    * initially be empty. Note that the returned list will never contain the XML namespace
    * (to get this, the NamespaceEnumeration class adds it itself). The list WILL include
    * an entry for the undeclaration xmlns=""; again it is the job of NamespaceEnumeration
    * to ignore this, since it doesn't represent a true namespace node.
    * @param addXML Add a namespace node for the XML namespace
    */

    public void addNamespaceNodes(ElementImpl owner, List list, boolean addXML) {
        if (namespaceList!=null) {
            int max = list.size();
            for (int i=0; i<namespaceList.length; i++) {
            	int nscode = namespaceList[i];
                int prefixCode = nscode>>16;

                boolean found = false;

                // Don't add a node if the prefix is already in the list
                for (int j=0; j<max; ) {
                    NamespaceImpl ns = (NamespaceImpl)list.get(j++);
                    if ((ns.getNamespaceCode()>>16) == prefixCode) {
                        found=true;
                        break;
                    }
                }
                if (!found) {
                    list.add(
                        new NamespaceImpl(
                            owner, nscode, list.size()+1));
                }
            }
        }

        // now add the namespaces defined on the ancestor nodes

        if (parent.getNodeKind()!=Type.DOCUMENT) {
            ((ElementImpl)parent).addNamespaceNodes(owner, list, false);
        }

        if (addXML) {
        	int nsxml = (1<<16) + 1;
            list.add(
                new NamespaceImpl(this, nsxml, list.size()+1)
                );
        }
    }

    /**
    * Output all namespace nodes associated with this element.
    * @param out The relevant outputter
    */

    public void outputNamespaceNodes(Receiver out, boolean includeAncestors) throws XPathException {

        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
                out.namespace(namespaceList[i], 0);
            }
        }

        // now add the namespaces defined on the ancestor nodes. We rely on the outputter
        // to eliminate multiple declarations of the same prefix

        if (includeAncestors) {
            if (parent.getNodeKind()!=Type.DOCUMENT) {
                parent.outputNamespaceNodes(out, true);
            }
        }
    }

    /**
    * Get the list of in-scope namespaces for this element as an array of
    * namespace codes. (Used by LiteralResultElement)
    */

    public int[] getNamespaceCodes() {
    	ArrayList namespaceNodes = new ArrayList();
        addNamespaceNodes(this, namespaceNodes, true);

        // copy to the namespace code list
        int[] namespaceCodes = new int[namespaceNodes.size()];
        for (int i=0; i<namespaceNodes.size(); i++) {
        	NamespaceImpl nsi = (NamespaceImpl)namespaceNodes.get(i);
        	namespaceCodes[i] = nsi.getNamespaceCode();
        }
        return namespaceCodes;
    }

    /**
    * Get the attribute list for this element.
    * @return The attribute list. This will not include any
    * namespace attributes. The attribute names will be in expanded form, with prefixes
    * replaced by URIs
    */

    public AttributeCollection getAttributeList() {
        return attributeList;
    }

    /**
     * Returns whether this node (if it is an element) has any attributes.
     * @return <code>true</code> if this node has any attributes,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttributes() {
        return attributeList.getLength() > 0;
    }

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
    	return attributeList.getValueByFingerprint(fingerprint);
    }

    /**
    * Set the value of an attribute on the current element. This affects subsequent calls
    * of getAttribute() for that element.
    * @param name The name of the attribute to be set. Any prefix is interpreted relative
    * to the namespaces defined for this element.
    * @param value The new value of the attribute. Set this to null to remove the attribute.
    * @throws DOMException (always): Saxon trees are immutable.
    */

    public void setAttribute(String name, String value ) throws DOMException {
        disallowUpdate();
    }

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
            outputNamespaceNodes(out, whichNamespaces==ALL_NAMESPACES);
        }

        // output the attributes

        for (int i=0; i<attributeList.getLength(); i++) {
            out.attribute(attributeList.getNameCode(i), -1,
                               attributeList.getValue(i), 0, 0);
        }

        out.startContent();

        // output the children

        int childNamespaces = (whichNamespaces==NO_NAMESPACES ? NO_NAMESPACES : LOCAL_NAMESPACES);
        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            next.copy(out, childNamespaces, copyAnnotations, locationId);
            next = (NodeImpl)next.getNextSibling();
        }

        out.endElement();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Following interfaces are provided to implement the DOM Element interface
    ////////////////////////////////////////////////////////////////////////////


    /**
     * Retrieves an attribute value by name. Namespace declarations are not
     * returned.
     * @param name  The name of the attribute to retrieve.
     * @return  The <code>Attr</code> value as a string, or the empty string if
     *    that attribute does not have a specified or default value. (Note the
     * difference from getAttributeValue(), which returns null if there is no
     * value).
     */

    public String getAttribute(String name) {
        int index = attributeList.getIndex(name);
        if (index<0) return "";
        return attributeList.getValue(index);
    }

    /**
     * A <code>NamedNodeMap</code> containing the attributes of this element. This
     * is a DOM method, so the list of attributes includes namespace declarations.
     */

    public NamedNodeMap getAttributes() {
        return this;
    }

    /**
     * Removes an attribute by name.
     * @param name  The name of the attribute to remove.
     */

    public void removeAttribute(String name) {
        setAttribute(name, null);
    }

    /**
     * Retrieves an attribute node by name. Namespace declarations are not
     * returned.
     * <br> To retrieve an attribute node by qualified name and namespace URI,
     * use the <code>getAttributeNodeNS</code> method.
     * @param name  The name (<code>nodeName</code> ) of the attribute to
     *   retrieve.
     * @return  The <code>Attr</code> node with the specified name (
     *   <code>nodeName</code> ) or <code>null</code> if there is no such
     *   attribute.
     */

    public Attr getAttributeNode(String name) {
        int index = getAttributeList().getIndex(name);
        if (index<0) {
            return null;
        }
        return new AttributeImpl(this, index);
    }

    /**
     * Adds a new attribute node. Always fails
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Removes the specified attribute node. Always fails
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Retrieves an attribute value by local name and namespace URI.
     * HTML-only DOM implementations do not need to implement this method.
     * @param namespaceURI  The  namespace URI of the attribute to retrieve.
     * @param localName  The  local name of the attribute to retrieve.
     * @return  The <code>Attr</code> value as a string, or the empty string if
     *    that attribute does not have a specified or default value.
     * @since DOM Level 2
     */

    public String getAttributeNS(String namespaceURI, String localName) {
        String value = Navigator.getAttributeValue(this, namespaceURI, localName);
        return (value==null ? "" : value);
    }

    /**
     * Adds a new attribute. Always fails.
     * @param namespaceURI  The  namespace URI of the attribute to create or
     *   alter.
     * @param qualifiedName  The  qualified name of the attribute to create or
     *   alter.
     * @param value  The value to set in string form.
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void setAttributeNS(String namespaceURI,
                               String qualifiedName,
                               String value)
                               throws DOMException {
        disallowUpdate();
    }

    /**
     * Removes an attribute by local name and namespace URI. Always fails
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     * @since DOM Level 2
     */

    public void removeAttributeNS(String namespaceURI,
                                  String localName)
                                  throws DOMException{
        disallowUpdate();
    }

    /**
     * Retrieves an <code>Attr</code> node by local name and namespace URI.
     * DOM method, so namespace declarations count as attributes.
     * @param namespaceURI  The  namespace URI of the attribute to retrieve.
     * @param localName  The  local name of the attribute to retrieve.
     * @return  The <code>Attr</code> node with the specified attribute local
     *   name and namespace URI or <code>null</code> if there is no such
     *   attribute.
     * @since DOM Level 2
     */

    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
    	int index = attributeList.getIndex(namespaceURI, localName);
    	if (index<0) return null;
    	return new AttributeImpl(this, index);
    }

    /**
     * Add a new attribute. Always fails.
     * @param newAttr  The <code>Attr</code> node to add to the attribute list.
     * @return  If the <code>newAttr</code> attribute replaces an existing
     *   attribute with the same  local name and  namespace URI , the
     *   replaced <code>Attr</code> node is returned, otherwise
     *   <code>null</code> is returned.
     * @exception DOMException
     *   <br> NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     * @since DOM Level 2
     */

    public Attr setAttributeNodeNS(Attr newAttr)
                                   throws DOMException{
        disallowUpdate();
        return null;
    }

    /**
     * Returns <code>true</code> when an attribute with a given name is
     * specified on this element or has a default value, <code>false</code>
     * otherwise. Namespace declarations are not included.
     * @param name  The name of the attribute to look for.
     * @return <code>true</code> if an attribute with the given name is
     *   specified on this element or has a default value, <code>false</code>
     *   otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttribute(String name) {
        return attributeList.getIndex(name) >= 0;
    }

    /**
     * Returns <code>true</code> when an attribute with a given local name
     * and namespace URI is specified on this element or has a default value,
     * <code>false</code> otherwise. This is a DOM method so namespace declarations
     * are treated as attributes.
     * @param namespaceURI  The  namespace URI of the attribute to look for.
     * @param localName  The  local name of the attribute to look for.
     * @return <code>true</code> if an attribute with the given local name and
     *   namespace URI is specified or has a default value on this element,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttributeNS(String namespaceURI, String localName) {
    	return (Navigator.getAttributeValue(this, namespaceURI, localName) != null);
    }

    //////////////////////////////////////////////////////////////////////
    // Methods to implement DOM NamedNodeMap (the set of attributes)
    //////////////////////////////////////////////////////////////////////

    /**
    * Get named attribute (DOM NamedNodeMap method)
    * Treats namespace declarations as attributes.
    */

    public Node getNamedItem(String name) {
        return getAttributeNode(name);
    }

    /**
    * Set named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node setNamedItem(Node arg) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
    * Remove named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node removeNamedItem(String name) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
    * Get n'th attribute (DOM NamedNodeMap method). Namespace declarations are
    * not returned.
    */

    public Node item(int index) {
        if (index<0 || index>=attributeList.getLength()) {
            return null;
        }
        return new AttributeImpl(this, index);
    }

    /**
    * Get number of attributes (DOM NamedNodeMap method).
    * Treats namespace declarations as attributes.
    */

    public int getLength() {
        return attributeList.getLength();
    }

    /**
    * Get named attribute (DOM NamedNodeMap method)
    * Treats namespace declarations as attributes.
    */

    public Node getNamedItemNS(String uri, String localName) {
        return getAttributeNodeNS(uri, localName);
    }

    /**
    * Set named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node setNamedItemNS(Node arg) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
    * Remove named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node removeNamedItemNS(String uri, String localName) throws DOMException {
        disallowUpdate();
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
