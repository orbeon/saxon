package org.orbeon.saxon.event;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;


/**
  * <b>TransformerReceiver</b> is similar in concept to the JAXP TransformerHandler,
 * except that it implements Saxon's Receiver interface rather than the standard
 * SAX2 interface. This means that it allows nodes with type annotations to be
 * passed down a pipeline from one transformation to another.
  */

public class TransformerReceiver extends ProxyReceiver {

    Controller controller;
    Builder builder;
    Result result;
    String systemId;

    /**
    * Create a TransformerHandlerImpl and initialise variables.
    */

    public TransformerReceiver(Controller controller) {
        this.controller = controller;
        setConfiguration(controller.getConfiguration());
        builder = controller.makeBuilder();
        builder.setConfiguration(controller.getConfiguration());
        Stripper stripper = controller.makeStripper(builder);
        this.setUnderlyingReceiver(stripper);
    }

    // TODO: make the TransformerReceiver serially reusable

    /**
    * Get the Transformer used for this transformation
    */

    public Transformer getTransformer() {
        return controller;
    }

    /**
    * Set the SystemId of the document
    */

    public void setSystemId(String url) {
        systemId = url;
        builder.setSystemId(url);
    }

    /**
    * Get the systemId of the document
    */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Notify the start of an element
     * @param nameCode integer code identifying the name of the element within the name pool.
     * @param typeCode integer code identifying the element's type within the name pool.
     * @param properties: for future use. Should be set to zero.
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        super.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
    * Set the output destination of the transformation
    */

    public void setResult(Result result) {
        if (result==null) {
            throw new IllegalArgumentException("Result must not be null");
        }
        this.result = result;
    }

    /**
    * Get the output destination of the transformation
    */

    public Result getResult() {
        return result;
    }

    /**
    * Override the behaviour of endDocument() in ReceivingContentHandler, so that it fires off
    * the transformation of the constructed document
    */

    public void close() throws XPathException {
        super.close();
        DocumentInfo doc = builder.getCurrentDocument();
        if (doc==null) {
            throw new DynamicError("No source document has been built");
        }
        doc.getNamePool().allocateDocumentNumber(doc);
        try {
            controller.transformDocument(doc, result);
        } catch (TransformerException e) {
            throw XPathException.wrap(e);
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
// Contributor(s): None
//
