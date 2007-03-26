package org.orbeon.saxon.event;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.Controller;

/**
 * This filter is inserted into the serializer pipeline when serializing an implicit XSLT result tree, that
 * is, one that is created without use of xsl:result-document. Its main purpose is to check, if and only if
 * the result destination is actually written to, that it does not conflict with an explicit result destination
 * with the same URI.
 */
public class ImplicitResultChecker extends ProxyReceiver {

    private boolean clean = true;
    private boolean open = false;
    private Controller controller;

    public ImplicitResultChecker(Receiver next, Controller controller) {
        setUnderlyingReceiver(next);
        this.controller = controller;
    }

    public void open() throws XPathException {
        super.open();
        open = true;
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
        nextReceiver.characters(chars, locationId, properties);    //AUTO
    }

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (clean) {
            firstContent();
        }
        nextReceiver.processingInstruction(target, data, locationId, properties);    //AUTO
    }

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (clean) {
            firstContent();
        }
        nextReceiver.comment(chars, locationId, properties);    //AUTO
    }

    private void firstContent() throws XPathException {
        controller.checkImplicitResultTree();
        if (!open) {
            open();
            startDocument(0);
        }
        clean = false;
    }

    public void close() throws XPathException {
        // If we haven't written any output, don't do the close, as this would cause a file to be
        // created and perhaps an XML declaration to be written
        if (!clean) {
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

