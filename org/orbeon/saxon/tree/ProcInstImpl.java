package org.orbeon.saxon.tree;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.XPathException;
import org.w3c.dom.DOMException;

/**
  * ProcInstImpl is an implementation of ProcInstInfo used by the Propagator to construct
  * its trees.
  * @author Michael H. Kay
  */


class ProcInstImpl extends NodeImpl implements ProcessingInstruction {

    String content;
    int nameCode;
    String systemId;
    int lineNumber = -1;

    public ProcInstImpl(int nameCode, String content) {
        this.nameCode = nameCode;
        this.content = content;
    }

	/**
	* Get the nameCode of the node. This is used to locate the name in the NamePool
	*/

	public int getNameCode() {
		return nameCode;
	}

    public String getStringValue() {
        return content;
    }

    public final int getNodeKind() {
        return Type.PROCESSING_INSTRUCTION;
    }

    /**
    * Set the system ID and line number
    */

    public void setLocation(String uri, int lineNumber) {
        this.systemId = uri;
        this.lineNumber = lineNumber;
    }

    /**
    * Get the system ID for the entity containing this node.
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Get the line number of the node within its source entity
    */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        out.processingInstruction(getLocalPart(), content, locationId, 0);
    }

    // DOM methods

    /**
     * The target of this processing instruction. XML defines this as being
     * the first token following the markup that begins the processing
     * instruction.
     */

    public String getTarget() {
        return getLocalPart();
    }

    /**
     *  The content of this processing instruction. This is from the first non
     * white space character after the target to the character immediately
     * preceding the <code>?&gt;</code> .
     */

    public String getData() {
        return content;
    }

    /**
     * Set the content of this PI. Always fails.
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     */

    public void setData(String data) throws DOMException {
        disallowUpdate();
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
