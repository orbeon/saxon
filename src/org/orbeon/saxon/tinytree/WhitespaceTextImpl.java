package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.om.FastStringBuffer;

/**
  * A node in the XML parse tree representing a text node with compressed whitespace content
  * @author Michael H. Kay
  */

public final class WhitespaceTextImpl extends TinyNodeImpl {

    // TODO: make this class implement CharSequence directly, avoiding the need to create a CompressedWhitespace object

    /**
     * Create a compressed whitespace text node
     * @param tree the tree to contain the node
     * @param nodeNr the internal node number
     */

    public WhitespaceTextImpl(TinyTree tree, int nodeNr) {
        this.tree = tree;
        this.nodeNr = nodeNr;
    }

    /**
    * Return the character value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
        return getStringValueCS().toString();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String. For a WhitespaceTextImpl node, it avoids the
     * cost of decompressing the whitespace
     */

    public CharSequence getStringValueCS() {
        long value = ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
        return new CompressedWhitespace(value);
    }

    /**
     * Static method to get the string value of a text node without first constructing the node object
     * @param tree the tree
     * @param nodeNr the node number of the text node
     * @return the string value of the text node
     */

    public static CharSequence getStringValue(TinyTree tree, int nodeNr) {
        long value = ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
        return new CompressedWhitespace(value);
    }

   /**
     * Static method to get the string value of a text node and append it to a supplied buffer
     * without first constructing the node object
     * @param tree the tree
     * @param nodeNr the node number of the text node
     * @param buffer a buffer to which the string value will be appended
     */

    public static void appendStringValue(TinyTree tree, int nodeNr, FastStringBuffer buffer) {
        long value = ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
        CompressedWhitespace.uncompress(value, buffer);
    }

    /**
     * Static method to get the "long" value representing the content of a whitespace text node
     * @param tree the TinyTree
     * @param nodeNr the internal node number
     * @return a value representing the compressed whitespace content
     * @see CompressedWhitespace
     */

    public static long getLongValue(TinyTree tree, int nodeNr) {
        return ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
    }

    /**
    * Return the type of node.
    * @return Type.TEXT
    */

    public final int getNodeKind() {
        return Type.TEXT;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        out.characters(getStringValueCS(), 0, 0);
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
