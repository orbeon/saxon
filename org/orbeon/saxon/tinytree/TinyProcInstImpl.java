package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.Navigator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Value;

/**
  * TProcInstImpl is an implementation of ProcInstInfo
  * @author Michael H. Kay
  * @version 16 July 1999
  */


final class TinyProcInstImpl extends TinyNodeImpl {

    public TinyProcInstImpl(TinyTree tree, int nodeNr) {
        this.tree = tree;
        this.nodeNr = nodeNr;
    }

    public String getStringValue() {
        int start = tree.alpha[nodeNr];
        int len = tree.beta[nodeNr];
        if (len==0) {
        	return "";	// need to special-case this for the Microsoft JVM
        }
        char[] dest = new char[len];
        tree.commentBuffer.getChars(start, start+len, dest, 0);
        return new String(dest, 0, len);
    }

    /**
     * Get the typed value of this node.
     * Returns the string value, as an instance of xs:string
     */

    public SequenceIterator getTypedValue() {
        return SingletonIterator.makeIterator(new StringValue(getStringValue()));
    }

    /**
     * Get the typed value of this node.
     * Returns the string value, as an instance of xs:string
     */

    public Value atomize() {
        return new StringValue(getStringValue());
    }

    public final int getNodeKind() {
        return Type.PROCESSING_INSTRUCTION;
    }

    /**
    * Get the base URI of this processing instruction node.
    */

    public String getBaseURI() {
        return Navigator.getBaseURI(this);
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        out.processingInstruction(getDisplayName(), getStringValue(), 0, 0);
    }

    // DOM methods

    /**
     * The target of this processing instruction. XML defines this as being
     * the first token following the markup that begins the processing
     * instruction.
     */

    public String getTarget() {
        return getDisplayName();
    }

    /**
     *  The content of this processing instruction. This is from the first non
     * white space character after the target to the character immediately
     * preceding the <code>?&gt;</code> .
     */

    public String getData() {
        return getStringValue();
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
