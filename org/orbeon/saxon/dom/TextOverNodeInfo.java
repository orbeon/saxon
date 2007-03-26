package org.orbeon.saxon.dom;

import org.orbeon.saxon.type.ComplexType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Whitespace;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

/**
 * This class is an implementation of the DOM Text and Comment interfaces that wraps a Saxon NodeInfo
 * representation of a text or comment node.
 */

public class TextOverNodeInfo extends NodeOverNodeInfo implements Text, Comment {


    /**
    * Get the character data of a Text or Comment node.
    * DOM method.
    */

    public String getData() {
        return node.getStringValue();
    }

    /**
    * Set the character data of a Text or Comment node.
    * DOM method: always fails, Saxon tree is immutable.
    */

    public void setData(String data) throws DOMException {
        disallowUpdate();
    }

    /**
    * Get the length of a Text or Comment node.
    * DOM method.
    */

    public int getLength() {
        return node.getStringValue().length();
    }

    /**
     * Extract a range of data from a Text or Comment node. DOM method.
     * @param offset  Start offset of substring to extract.
     * @param count  The number of 16-bit units to extract.
     * @return  The specified substring. If the sum of <code>offset</code> and
     *   <code>count</code> exceeds the <code>length</code> , then all 16-bit
     *   units to the end of the data are returned.
     * @exception org.w3c.dom.DOMException
     *    INDEX_SIZE_ERR: Raised if the specified <code>offset</code> is
     *   negative or greater than the number of 16-bit units in
     *   <code>data</code> , or if the specified <code>count</code> is
     *   negative.
     */

    public String substringData(int offset, int count) throws DOMException {
        try {
            return node.getStringValue().substring(offset, offset+count);
        } catch (IndexOutOfBoundsException err2) {
            throw new DOMExceptionImpl(DOMException.INDEX_SIZE_ERR,
                             "substringData: index out of bounds");
        }
    }

    /**
     * Append the string to the end of the character data of the node.
     * DOM method: always fails.
     * @param arg  The <code>DOMString</code> to append.
     * @exception org.w3c.dom.DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void appendData(String arg) throws DOMException {
        disallowUpdate();
    }

    /**
     * Insert a string at the specified character offset.
     * DOM method: always fails.
     * @param offset  The character offset at which to insert.
     * @param arg  The <code>DOMString</code> to insert.
     * @exception org.w3c.dom.DOMException
     */

    public void insertData(int offset, String arg) throws DOMException {
        disallowUpdate();
    }

    /**
     * Remove a range of 16-bit units from the node.
     * DOM method: always fails.
     * @param offset  The offset from which to start removing.
     * @param count  The number of 16-bit units to delete.
     * @exception org.w3c.dom.DOMException
     */

    public void deleteData(int offset, int count) throws DOMException {
        disallowUpdate();
    }

    /**
     * Replace the characters starting at the specified 16-bit unit offset
     * with the specified string. DOM method: always fails.
     * @param offset  The offset from which to start replacing.
     * @param count  The number of 16-bit units to replace.
     * @param arg  The <code>DOMString</code> with which the range must be
     *   replaced.
     * @exception org.w3c.dom.DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     */

    public void replaceData(int offset,
                            int count,
                            String arg) throws DOMException {
        disallowUpdate();
    }


    /**
     * Break this node into two nodes at the specified offset,
     * keeping both in the tree as siblings. DOM method, always fails.
     * @param offset  The 16-bit unit offset at which to split, starting from 0.
     * @return  The new node, of the same type as this node.
     * @exception org.w3c.dom.DOMException
     */

    public Text splitText(int offset) throws DOMException {
        disallowUpdate();
        return null;
    }

    /**
     * Replaces the text of the current node and all logically-adjacent text
     * nodes with the specified text. All logically-adjacent text nodes are
     * removed including the current node unless it was the recipient of the
     * replacement text.
     * <br>This method returns the node which received the replacement text.
     * The returned node is:
     * <ul>
     * <li><code>null</code>, when the replacement text is
     * the empty string;
     * </li>
     * <li>the current node, except when the current node is
     * read-only;
     * </li>
     * <li> a new <code>Text</code> node of the same type (
     * <code>Text</code> or <code>CDATASection</code>) as the current node
     * inserted at the location of the replacement.
     * </li>
     * </ul>
     * <br>For instance, in the above example calling
     * <code>replaceWholeText</code> on the <code>Text</code> node that
     * contains "bar" with "yo" in argument results in the following:
     * <br>Where the nodes to be removed are read-only descendants of an
     * <code>EntityReference</code>, the <code>EntityReference</code> must
     * be removed instead of the read-only nodes. If any
     * <code>EntityReference</code> to be removed has descendants that are
     * not <code>EntityReference</code>, <code>Text</code>, or
     * <code>CDATASection</code> nodes, the <code>replaceWholeText</code>
     * method must fail before performing any modification of the document,
     * raising a <code>DOMException</code> with the code
     * <code>NO_MODIFICATION_ALLOWED_ERR</code>.
     * <br>For instance, in the example below calling
     * <code>replaceWholeText</code> on the <code>Text</code> node that
     * contains "bar" fails, because the <code>EntityReference</code> node
     * "ent" contains an <code>Element</code> node which cannot be removed.
     *
     * @param content The content of the replacing <code>Text</code> node.
     * @return The <code>Text</code> node created with the specified content.
     * @throws org.w3c.dom.DOMException NO_MODIFICATION_ALLOWED_ERR: Raised if one of the <code>Text</code>
     *                                  nodes being replaced is readonly.
     * @since DOM Level 3
     */
    public Text replaceWholeText(String content) throws DOMException {
        disallowUpdate();
        return null;
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
        if (node.getNodeKind() != Type.TEXT) {
            throw new UnsupportedOperationException("Method is defined only on text nodes");
        }
        int annotation = node.getParent().getTypeAnnotation();
        if (annotation == -1) {
            return false;
        }
        if (!Whitespace.isWhite(node.getStringValue())) {
            return false;
        }
        SchemaType type = node.getConfiguration().getSchemaType(annotation);
        if (!type.isComplexType()) {
            return false;
        }
        if (((ComplexType)type).isMixedContent()) {
            return false;
        }
        return true;
    }

    /**
     * Returns all text of <code>Text</code> nodes logically-adjacent text
     * nodes to this node, concatenated in document order.
     * <br>For instance, in the example below <code>wholeText</code> on the
     * <code>Text</code> node that contains "bar" returns "barfoo", while on
     * the <code>Text</code> node that contains "foo" it returns "barfoo".
     *
     * @since DOM Level 3
     */
    public String getWholeText() {
        if (node.getNodeKind() != Type.TEXT) {
            throw new UnsupportedOperationException("Method is defined only on text nodes");
        }
        return node.getStringValue();
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