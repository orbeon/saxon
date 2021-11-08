package org.orbeon.saxon.event;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.om.DocumentInfo;

import org.orbeon.saxon.trans.XPathException;

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

    /**
    * Create a TransformerHandlerImpl and initialise variables.
    */

    public TransformerReceiver(Controller controller) {
        this.controller = controller;
    }

    /**
     * Start of event stream
     */

    public void open() throws XPathException {
        builder = controller.makeBuilder();
        setPipelineConfiguration(builder.getPipelineConfiguration());
        builder.setSystemId(systemId);
        Receiver stripper = controller.makeStripper(builder);
        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            stripper = controller.getConfiguration().getAnnotationStripper(stripper);
        }
        setUnderlyingReceiver(stripper);
        nextReceiver.open();
    }

    /**
    * Get the Transformer used for this transformation
    */

    public Transformer getTransformer() {
        return controller;
    }

    /**
    * Set the SystemId of the document
    */

    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        controller.setBaseOutputURI(systemId);
    }

    /**
     * Notify the start of an element
     * @param nameCode integer code identifying the name of the element within the name pool.
     * @param typeCode integer code identifying the element's type within the name pool.
     * @param properties bit-significant properties of the element node.
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        nextReceiver.startElement(nameCode, typeCode, locationId, properties);
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
    * Override the behaviour of endDocument() in ProxyReceiver, so that it fires off
    * the transformation of the constructed document
    */

    public void close() throws XPathException {
        nextReceiver.close();
        DocumentInfo doc = (DocumentInfo)builder.getCurrentRoot();
        builder.reset();
        if (doc==null) {
            throw new XPathException("No source document has been built");
        }
        //doc.getNamePool().allocateDocumentNumber(doc);
        try {
            controller.transformDocument(doc, result);
        } catch (TransformerException e) {
            throw XPathException.makeXPathException(e);
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
