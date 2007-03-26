package org.orbeon.saxon.event;

import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
 * Sends an entire document to a Receiver.
 *
 * @author Ruud Diterwich, integrated by Michael Kay
 */

public class DocumentSender implements SaxonLocator {

	private NodeInfo top;

    /**
    * Create a DocumentSender, which takes an input document tree and generates
    * a stream of events for a Receiver
    * @param top the document or element node to be turned into a stream of events
    */

	public DocumentSender(NodeInfo top) {
		this.top = top;
        int kind = top.getNodeKind();
        if (kind != Type.DOCUMENT && kind != Type.ELEMENT) {
            throw new IllegalArgumentException("DocumentSender can only handle document or element nodes");
        }
	}

    /**
    * Send the entire document to the receiver
    */

	public void send(Receiver receiver) throws XPathException {

        PipelineConfiguration pipe = receiver.getPipelineConfiguration();
        if (top.getNamePool() != pipe.getConfiguration().getNamePool()
                && !(receiver instanceof NamePoolConverter)) {
            throw new IllegalArgumentException("DocumentSender source and target must use the same NamePool");
        }

		// set system id
        if (pipe.getLocationProvider() == null) {
		    receiver.setSystemId(top.getSystemId());
	        pipe.setLocationProvider(this);
        }

		// start event stream
		receiver.open();

		// copy the contents of the document
        receiver.startDocument(0);
		top.copy(receiver, NodeInfo.ALL_NAMESPACES, true, 0);
        receiver.endDocument();

		// end event stream
		receiver.close();
	}

    // Implement the SAX Locator interface. This is needed to pass the base URI of nodes
    // to the receiver. We don't attempt to preserve the original base URI of each individual
    // node as it is copied, only the base URI of the document as a whole.

	public int getColumnNumber() {
		return -1;
	}

	public int getLineNumber() {
		return -1;
	}

	public String getPublicId() {
		return null;
	}

	public String getSystemId() {
		return top.getSystemId();
	}

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
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