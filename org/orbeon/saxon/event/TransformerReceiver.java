package net.sf.saxon.event;
import net.sf.saxon.Controller;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

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
    }

    /**
     * Start of event stream
     */

    public void open() throws XPathException {
        setPipelineConfiguration(controller.makePipelineConfiguration());
        builder = controller.makeBuilder();
        builder.setPipelineConfiguration(getPipelineConfiguration());
        builder.setSystemId(systemId);
        Receiver stripper = controller.makeStripper(builder);
        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            stripper = controller.getConfiguration().getAnnotationStripper(stripper);
        }
        this.setUnderlyingReceiver(stripper);
        super.open();
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

    public void setSystemId(String url) {
        systemId = url;
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
     * @param properties bit-significant properties of the element node.
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
        DocumentInfo doc = (DocumentInfo)builder.getCurrentRoot();
        if (doc==null) {
            throw new DynamicError("No source document has been built");
        }
        doc.getNamePool().allocateDocumentNumber(doc);
        try {
            controller.transformDocument(doc, result);
        } catch (TransformerException e) {
            throw DynamicError.makeDynamicError(e);
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
