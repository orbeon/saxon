package net.sf.saxon.om;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Implementation of DOM NamedNodeMap used to represent the attributes of an element, for use when
 * Saxon element and attribute nodes are accessed using the DOM API.
 */

class DOMAttributeMap implements NamedNodeMap {

    private AbstractNode parent;

    /**
     * Construct an AttributeMap for a given element node
     */

    public DOMAttributeMap(AbstractNode parent) {
        this.parent = parent;
    }

    /**
    * Get named attribute (DOM NamedNodeMap method)
    */

    public Node getNamedItem(String name) {
        AxisIterator atts = parent.iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return null;
            }
            if (name.equals(att.getDisplayName())) {
                return (Node)att;
            }
        }
    }

    /**
    * Get n'th attribute (DOM NamedNodeMap method).
    * Namespace declarations are not retrieved.
    */

    public Node item(int index) {
        if (index<0) {
            return null;
        }
        int length = 0;
        AxisIterator atts = parent.iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return null;
            }
            if (length==index) {
                return (Node)att;
            }
            length++;
        }
    }

    /**
    * Get number of attributes (DOM NamedNodeMap method).
    */

    public int getLength() {
        int length = 0;
        AxisIterator atts = parent.iterateAxis(Axis.ATTRIBUTE);
        while (atts.next() != null) {
            length++;
        }
        return length;
    }

    /**
    * Get named attribute (DOM NamedNodeMap method)
    */

    public Node getNamedItemNS(String uri, String localName) {
        if (uri==null) uri="";
        AxisIterator atts = parent.iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo att = (NodeInfo)atts.next();
            if (att == null) {
                return null;
            }
            if (uri.equals(att.getURI()) && localName.equals(att.getLocalPart())) {
                return (Node)att;
            }
        }
    }

    /**
    * Set named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node setNamedItem(Node arg) throws DOMException {
        AbstractNode.disallowUpdate();
        return null;
    }

    /**
    * Remove named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node removeNamedItem(String name) throws DOMException {
        AbstractNode.disallowUpdate();
        return null;
    }

    /**
    * Set named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node setNamedItemNS(Node arg) throws DOMException {
        AbstractNode.disallowUpdate();
        return null;
    }

    /**
    * Remove named attribute (DOM NamedNodeMap method: always fails)
    */

    public Node removeNamedItemNS(String uri, String localName) throws DOMException {
        AbstractNode.disallowUpdate();
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

