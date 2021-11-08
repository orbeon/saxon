package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.Controller;

/**
 * This filter is inserted into the serializer pipeline when serializing an implicit XSLT result tree, that
 * is, one that is created without use of xsl:result-document. Its main purpose is to check, if and only if
 * the result destination is actually written to, that it does not conflict with an explicit result destination
 * with the same URI. It also ensures that the output destination is opened before it is first written to.
 */
public class ImplicitResultChecker extends ProxyReceiver {

    private boolean clean = true;
    private boolean open = false;
    private Controller controller;

    /**
     * Create an ImplicitResultChecker. This is a filter on the output pipeline.
     * @param next the next receiver on the pipeline
     * @param controller the controller of the XSLT transformation
     */

    public ImplicitResultChecker(Receiver next, Controller controller) {
        setUnderlyingReceiver(next);
        this.controller = controller;
    }

    public void open() throws XPathException {
        super.open();
        open = true;
    }

    public void startDocument(int properties) throws XPathException {
        if (!open) {
            open();
        }
        nextReceiver.startDocument(properties);
    }

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (clean) {
            firstContent();
        }
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
    }

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (clean) {
            firstContent();
        }
        nextReceiver.characters(chars, locationId, properties);
    }

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (clean) {
            firstContent();
        }
        nextReceiver.processingInstruction(target, data, locationId, properties);
    }

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (clean) {
            firstContent();
        }
        nextReceiver.comment(chars, locationId, properties);
    }

    /**
     * This method does the real work. It is called when the first output is written to the implicit output
     * destination, and checks that no explicit result document has been written to the same URI
     * as the implicit result document
     * @throws XPathException
     */

    private void firstContent() throws XPathException {
        controller.checkImplicitResultTree();
        if (!open) {
            open();
            startDocument(0);
        }
        clean = false;
    }

    public void close() throws XPathException {
        // If we haven't written any output, do the close only if no explicit result document has been written.
        // This will cause a file to be created and perhaps an XML declaration to be written
        if (!clean || !controller.hasThereBeenAnExplicitResultDocument()) {
            if (!open) {
                open();
            }
            nextReceiver.close();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

