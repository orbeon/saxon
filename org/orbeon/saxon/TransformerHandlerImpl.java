package net.sf.saxon;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceivingContentHandler;
import net.sf.saxon.om.DocumentInfo;
import org.xml.sax.SAXException;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.TransformerHandler;


/**
  * <b>TransformerHandlerImpl</b> implements the javax.xml.transform.sax.TransformerHandler
  * interface. It acts as a ContentHandler and LexicalHandler which receives a stream of
  * SAX events representing an input document, and performs a transformation treating this
  * SAX stream as the source document of the transformation.
  * @author Michael H. Kay
  */

public class TransformerHandlerImpl extends ReceivingContentHandler implements TransformerHandler {

    // TODO: if requested, apply schema validation to the source document

    Controller controller;
    Builder builder;
    Result result;
    String systemId;
    boolean started = false;

    /**
    * Create a TransformerHandlerImpl and initialise variables. The constructor is protected, because
    * the Filter should be created using newTransformerHandler() in the SAXTransformerFactory
    * class
    */

    protected TransformerHandlerImpl(Controller controller) {
        this.controller = controller;
        setPipelineConfiguration(controller.makePipelineConfiguration());
        builder = controller.makeBuilder();
        builder.setPipelineConfiguration(getPipelineConfiguration());
        Receiver stripper = controller.makeStripper(builder);
        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            stripper = controller.getConfiguration().getAnnotationStripper(stripper);
        }
        this.setReceiver(stripper);
    }

    /**
     * Start of a new document. The TransformerHandler is not serially reusable, so this method
     * must only be called once.
     * @throws SAXException only if an overriding subclass throws this exception
     * @throws UnsupportedOperationException if an attempt is made to reuse the TransformerHandler by calling
     * startDocument() more than once.
     */

    public void startDocument () throws SAXException {
        if (started) {
            throw new UnsupportedOperationException(
                    "The TransformerHandler is not serially reusable. The startDocument() method must be called once only.");
        }
        started = true;
        super.startDocument();
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
        builder.setSystemId(url);
    }

    /**
    * Get the systemId of the document
    */

    public String getSystemId() {
        return systemId;
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

    public void endDocument() throws SAXException {
        super.endDocument();
        DocumentInfo doc = (DocumentInfo)builder.getCurrentRoot();
        if (doc==null) {
            throw new SAXException("No source document has been built");
        }
        doc.getNamePool().allocateDocumentNumber(doc);
        try {
            controller.transformDocument(doc, result);
        } catch (TransformerException err) {
            throw new SAXException(err);
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
