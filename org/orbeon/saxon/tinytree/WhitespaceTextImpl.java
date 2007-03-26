package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
  * A node in the XML parse tree representing a text node with compressed whitespace content
  * @author Michael H. Kay
  */

public final class WhitespaceTextImpl extends TinyNodeImpl {

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
     * the version of the method that returns a String.
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
        long value = ((long)tree.alpha[nodeNr]<<32) | (tree.beta[nodeNr]);
        return new CompressedWhitespace(value);
    }

    /**
     * Static method to get the "long" value representing the content of a whitespace text node
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
