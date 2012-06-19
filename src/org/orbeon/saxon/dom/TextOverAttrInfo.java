package org.orbeon.saxon.dom;

import org.w3c.dom.Node;
import org.w3c.dom.DOMException;
import org.orbeon.saxon.type.Type;

/**
 * This class represents a DOM text node that is the child of a DOM attribute node. The DOM attribute node
 * will be a wrapper over a Saxon attribute node or namespace node.
 */
public class TextOverAttrInfo extends TextOverNodeInfo {

    private AttrOverNodeInfo attr;

    public TextOverAttrInfo(AttrOverNodeInfo attr) {
        this.attr = attr;
        this.node = attr.getUnderlyingNodeInfo();
    }

    /**
     * Returns whether this text node contains <a href='http://www.w3.org/TR/2004/REC-xml-infoset-20040204#infoitem.character'>
     * element content whitespace</a>, often abusively called "ignorable whitespace". The text node is
     * determined to contain whitespace in element content during the load
     * of the document or if validation occurs while using
     * <code>Document.normalizeDocument()</code>.
     *
     * @since DOM Level 3
     */
    public boolean isElementContentWhitespace() {
        return false;
    }

    /**
     * Get the type of this node (node kind, in XPath terminology).
     * Note, the numbers assigned to node kinds
     * in Saxon (see {@link org.orbeon.saxon.type.Type}) are the same as those assigned in the DOM
     */

    public short getNodeType() {
        return Type.TEXT;
    }

    /**
     * Compare the position of the (other) node in document order with the reference node (this node).
     * DOM Level 3 method.
     *
     * @param other the other node.
     * @return Returns how the node is positioned relatively to the reference
     *         node.
     * @throws org.w3c.dom.DOMException
     */

    public short compareDocumentPosition(Node other) throws DOMException {
        final short      DOCUMENT_POSITION_FOLLOWING    = 0x04;
        if (other instanceof TextOverAttrInfo) {
            if (node.isSameNodeInfo(((TextOverAttrInfo)other).node)) {
                return 0;
            } else {
                return attr.compareDocumentPosition(((TextOverAttrInfo)other).attr);
            }
        } else if (other instanceof AttrOverNodeInfo) {
            if (node.isSameNodeInfo(((AttrOverNodeInfo)other).getUnderlyingNodeInfo())) {
                return DOCUMENT_POSITION_FOLLOWING;
            }
        }
        return attr.compareDocumentPosition(other);
    }

    /**
     * Find the parent node of this node.
     *
     * @return The Node object describing the containing element or root node.
     */

    public Node getParentNode() {
        return attr;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

