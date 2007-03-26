package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
  * A node in the XML parse tree representing character content
  * @author Michael H. Kay
  */

public final class TinyTextImpl extends TinyNodeImpl {

    public TinyTextImpl(TinyTree tree, int nodeNr) {
        this.tree = tree;
        this.nodeNr = nodeNr;
    }

    /**
    * Return the character value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
        int start = tree.alpha[nodeNr];
        int len = tree.beta[nodeNr];
        //return new String(tree.charBuffer, start, len);
        return tree.charBuffer.substring(start, start+len);
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        int start = tree.alpha[nodeNr];
        int len = tree.beta[nodeNr];
        //return new CharSlice(tree.charBuffer, start, len);
        return tree.charBuffer.subSequence(start, start+len);
    }

    /**
     * Static method to get the string value of a text node without first constructing the node object
     * @param tree the tree
     * @param nodeNr the node number of the text node
     * @return the string value of the text node
     */

    public static CharSequence getStringValue(TinyTree tree, int nodeNr) {
        int start = tree.alpha[nodeNr];
        int len = tree.beta[nodeNr];
        //return new CharSlice(tree.charBuffer, start, len);
        return tree.charBuffer.subSequence(start, start+len);
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
