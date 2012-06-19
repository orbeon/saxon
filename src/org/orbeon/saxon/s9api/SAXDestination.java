package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.ContentHandlerProxy;
import org.orbeon.saxon.event.Receiver;
import org.xml.sax.ContentHandler;


/**
 * This class represents a Destination (for example, the destination of the output of a transformation)
 * in which events representing the XML document are sent to a user-supplied SAX2 ContentHandler, as
 * if the ContentHandler were receiving the document directly from an XML parser.
 */

public class SAXDestination implements Destination {

    private ContentHandler contentHandler;

    /**
     * Create a SAXDestination, supplying a SAX ContentHandler to which
     * events will be routed
     * @param handler the SAX ContentHandler that is to receive the output. If the
     * ContentHandler is also a {@link org.xml.sax.ext.LexicalHandler} then it will also receive
     * notification of events such as comments.
     */

    public SAXDestination(ContentHandler handler) {
        contentHandler = handler;
    }

    /**
     * Return a Receiver. Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing the content of an XML document.
     *
     * @param config The Saxon configuration. This is supplied so that the destination can
     *               use information from the configuration (for example, a reference to the name pool)
     *               to construct or configure the returned Receiver.
     * @return the Receiver to which events are to be sent.
     * @throws org.orbeon.saxon.s9api.SaxonApiException
     *          if the Receiver cannot be created
     */

    public Receiver getReceiver(Configuration config) throws SaxonApiException {
        ContentHandlerProxy chp = new ContentHandlerProxy();
        chp.setUnderlyingContentHandler(contentHandler);
        chp.setPipelineConfiguration(config.makePipelineConfiguration());
        return chp;
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

