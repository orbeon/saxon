package net.sf.saxon.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.ProcessingInstruction;

/**
 * This class is an implementation of the DOM ProcessingInstruction interface that wraps a Saxon NodeInfo
 * representation of a text or comment node.
 */

public class PIOverNodeInfo extends NodeOverNodeInfo implements ProcessingInstruction {

    /**
     * The target of this processing instruction. XML defines this as being
     * the first token following the markup that begins the processing
     * instruction.
     */
    public String getTarget() {
        return node.getLocalPart();
    }

    /**
     * The content of this processing instruction. This is from the first non
     * white space character after the target to the character immediately
     * preceding the <code>?&gt;</code>.
     */
    public String getData() {
        return node.getStringValue();
    }

    /**
     * The content of this processing instruction. This is from the first non
     * white space character after the target to the character immediately
     * preceding the <code>?&gt;</code>.
     *
     * @throws org.w3c.dom.DOMException NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
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