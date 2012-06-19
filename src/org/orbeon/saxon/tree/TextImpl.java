package org.orbeon.saxon.tree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
  * A node in the XML parse tree representing character content<P>
  * @author Michael H. Kay
  */

final class TextImpl extends NodeImpl {

    private String content;

    public TextImpl(ParentNodeImpl parent, String content) {
    	this.parent = parent;
    	this.content = content;
    }

    /**
    * Return the character value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
		return content;
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
        out.characters(content, locationId, 0);
    }


    /**
     * Replace the string-value of this node
     *
     * @param stringValue the new string value
     */

    public void replaceStringValue(CharSequence stringValue) {
        if (stringValue.length() == 0) {
            delete();
        } else {
            content = stringValue.toString();
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