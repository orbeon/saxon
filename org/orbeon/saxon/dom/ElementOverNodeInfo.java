package net.sf.saxon.dom;

import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.type.Type;
import org.w3c.dom.*;
import com.saxonica.schema.TypeInfoImpl;

/**
 * This class is an implementation of the DOM Element class that wraps a Saxon NodeInfo
 * representation of an element node.
 */

public class ElementOverNodeInfo extends NodeOverNodeInfo implements Element {

    /**
     *  The name of the element (DOM interface).
     */

    public String getTagName() {
        return node.getDisplayName();
    }

    /**
     * Returns a <code>NodeList</code> of all descendant <code>Elements</code>
     * with a given tag name, in document order.
     *
     * @param name The name of the tag to match on. The special value "*"
     *             matches all tags.
     * @return A list of matching <code>Element</code> nodes.
     */
    public NodeList getElementsByTagName(String name) {
        return DocumentOverNodeInfo.getElementsByTagName(node, name);
    }

    /**
     * Returns a <code>NodeList</code> of all the descendant
     * <code>Elements</code> with a given local name and namespace URI in
     * document order.
     *
     * @param namespaceURI The namespace URI of the elements to match on. The
     *                     special value "*" matches all namespaces.
     * @param localName    The local name of the elements to match on. The
     *                     special value "*" matches all local names.
     * @return A new <code>NodeList</code> object containing all the matched
     *         <code>Elements</code>.
     * @throws org.w3c.dom.DOMException NOT_SUPPORTED_ERR: May be raised if the implementation does not
     *                                  support the feature <code>"XML"</code> and the language exposed
     *                                  through the Document does not support XML Namespaces (such as [<a href='http://www.w3.org/TR/1999/REC-html401-19991224/'>HTML 4.01</a>]).
     * @since DOM Level 2
     */
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
        return DocumentOverNodeInfo.getElementsByTagNameNS(node, namespaceURI, localName);
    }

    /**
     * Retrieves an attribute value by name. Namespace declarations will not
     * be retrieved. DOM interface.
     * @param name  The QName of the attribute to retrieve.
     * @return  The <code>Attr</code> value as a string, or the empty string if
     *    that attribute does not have a specified or default value.
     */

    public String getAttribute(String name) {
        AxisIterator atts = node.iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return "";
            }
            if (att.getDisplayName().equals(name)) {
                String val = att.getStringValue();
                if (val==null) return "";
                return val;
            }
        }
    }

    /**
     * Retrieves an attribute node by name.
     * Namespace declarations will not be retrieved.
     * <br> To retrieve an attribute node by qualified name and namespace URI,
     * use the <code>getAttributeNodeNS</code> method.
     * @param name  The name (<code>nodeName</code> ) of the attribute to
     *   retrieve.
     * @return  The <code>Attr</code> node with the specified name (
     *   <code>nodeName</code> ) or <code>null</code> if there is no such
     *   attribute.
     */

    public Attr getAttributeNode(String name) {
        AxisIterator atts = node.iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return null;
            }
            if (att.getDisplayName().equals(name)) {
                return (Attr)att;
            }
        }
    }

    /**
     * Adds a new attribute node. Always fails
     * @exception org.w3c.dom.DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Removes the specified attribute. Always fails
     * @exception org.w3c.dom.DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void removeAttribute(String oldAttr) throws DOMException {
        disallowUpdate();
    }

    /**
     * Removes the specified attribute node. Always fails
     * @exception org.w3c.dom.DOMException
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
    	String val = Navigator.getAttributeValue(node, namespaceURI, localName);
    	if (val==null) return "";
    	return val;
    }

    /**
     * Adds a new attribute. Always fails
     *
     * @param name  The name of the attribute to create or alter.
     * @param value Value to set in string form.
     * @throws org.w3c.dom.DOMException INVALID_CHARACTER_ERR: Raised if the specified name is not an XML
     *                                  name according to the XML version in use specified in the
     *                                  <code>Document.xmlVersion</code> attribute.
     *                                  <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */
    public void setAttribute(String name, String value) throws DOMException {
        disallowUpdate();
    }

    /**
     * Adds a new attribute. Always fails.
     * @param namespaceURI  The  namespace URI of the attribute to create or
     *   alter.
     * @param qualifiedName  The  qualified name of the attribute to create or
     *   alter.
     * @param value  The value to set in string form.
     * @exception org.w3c.dom.DOMException
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
     * @exception org.w3c.dom.DOMException
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
        DocumentInfo doc = node.getDocumentRoot();
        if (doc==null) {
            throw new UnsupportedOperationException("getAttributeNodeNS is not supported on a tree with no document node");
        }
        int fingerprint = doc.getNamePool().getFingerprint(namespaceURI, localName);
        if (fingerprint==-1) return null;
        NameTest test = new NameTest(Type.ATTRIBUTE, fingerprint, node.getNamePool());
        AxisIterator atts = node.iterateAxis(Axis.ATTRIBUTE, test);
        return (Attr)wrap((NodeInfo)atts.next());
    }

    /**
     * Add a new attribute. Always fails.
     * @param newAttr  The <code>Attr</code> node to add to the attribute list.
     * @return  If the <code>newAttr</code> attribute replaces an existing
     *   attribute with the same  local name and  namespace URI , the
     *   replaced <code>Attr</code> node is returned, otherwise
     *   <code>null</code> is returned.
     * @exception org.w3c.dom.DOMException
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
     * otherwise.
     * Namespace declarations will not be retrieved.
     * @param name  The name of the attribute to look for.
     * @return <code>true</code> if an attribute with the given name is
     *   specified on this element or has a default value, <code>false</code>
     *   otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttribute(String name) {
        AxisIterator atts = node.iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return false;
            }
            if (att.getDisplayName().equals(name)) {
                return true;
            }
        }
    }

    /**
     * Returns <code>true</code> when an attribute with a given local name
     * and namespace URI is specified on this element or has a default value,
     * <code>false</code> otherwise.
     * Namespace declarations will not be retrieved.
     * @param namespaceURI  The  namespace URI of the attribute to look for.
     * @param localName  The  local name of the attribute to look for.
     * @return <code>true</code> if an attribute with the given local name and
     *   namespace URI is specified or has a default value on this element,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttributeNS(String namespaceURI, String localName) {
		return (Navigator.getAttributeValue(node, namespaceURI, localName) != null);
    }

    // DOM Level 3 methods

    public void setIdAttribute(String name,
                               boolean isId)
                               throws DOMException{
        disallowUpdate();
    }

    public void setIdAttributeNS(String namespaceURI,
                                 String localName,
                                 boolean isId)
                                 throws DOMException{
        disallowUpdate();
    }

    public void setIdAttributeNode(Attr idAttr,
                                   boolean isId)
                                   throws DOMException{
        disallowUpdate();
    }


    /**
     * Get the schema type information for this node. Returns null for an untyped node.
     */

    public TypeInfo getSchemaTypeInfo() {
        int annotation = node.getTypeAnnotation();
        if (annotation == -1) {
            return null;
        }
        return new TypeInfoImpl(node.getConfiguration(),
                node.getConfiguration().getSchemaType(annotation));
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