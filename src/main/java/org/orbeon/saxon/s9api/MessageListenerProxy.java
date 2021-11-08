package org.orbeon.saxon.s9api;

import org.orbeon.saxon.event.SequenceWriter;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.expr.ExpressionLocation;

/**
 * This class implements a Receiver that can receive xsl:message output and send it to a
 * user-supplied MessageListener.
 */

class MessageListenerProxy extends SequenceWriter {

    private MessageListener listener;
    private boolean terminate;
    private int locationId = -1;

    protected MessageListenerProxy(MessageListener listener) {
        this.listener = listener;
    }

    /**
     * Get the wrapped MessageListener
     */

    public MessageListener getMessageListener() {
        return listener;
    }


    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        terminate = (properties & ReceiverOptions.TERMINATE) != 0;
        locationId = -1;
        super.startDocument(properties);
    }


    /**
     * Output an element start tag.
     *
     * @param nameCode   The element name code - a code held in the Name Pool
     * @param typeCode   Integer code identifying the type of this element. Zero identifies the default
     *                   type, that is xs:anyType
     * @param properties bit-significant flags indicating any special information
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (this.locationId == -1) {
            this.locationId = locationId;
        }
        super.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
     * Produce text content output. <BR>
     *
     * @param s          The String to be output
     * @param properties bit-significant flags for extra information, e.g. disable-output-escaping
     * @throws org.orbeon.saxon.trans.XPathException
     *          for any failure
     */

    public void characters(CharSequence s, int locationId, int properties) throws XPathException {
        if (this.locationId == -1) {
            this.locationId = locationId;
        }
        super.characters(s, locationId, properties);
    }


    /**
     * Append an item to the sequence, performing any necessary type-checking and conversion
     */

    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
        if (this.locationId == -1) {
            this.locationId = locationId;
        }
        super.append(item, locationId, copyNamespaces);
    }

    /**
     * Abstract method to be supplied by subclasses: output one item in the sequence.
     *
     * @param item the item to be written to the sequence
     */

    public void write(Item item) throws XPathException {
        ExpressionLocation loc = new ExpressionLocation();
        if (locationId != -1) {
            LocationProvider provider = getPipelineConfiguration().getLocationProvider();
            loc.setSystemId(provider.getSystemId(locationId));
            loc.setLineNumber(provider.getLineNumber(locationId));
        }
        listener.message(new XdmNode((NodeInfo)item), terminate, loc);
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

