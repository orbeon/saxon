package org.orbeon.saxon;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.ReceivingContentHandler;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;
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

    Controller controller;
    Builder builder;
    Receiver receiver;
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
        Configuration config = controller.getConfiguration();
        int validation = config.getSchemaValidationMode();
        builder = controller.makeBuilder();
        builder.setPipelineConfiguration(getPipelineConfiguration());
        receiver = controller.makeStripper(builder);
        if (controller.getExecutable().stripsInputTypeAnnotations()) {
            receiver = controller.getConfiguration().getAnnotationStripper(receiver);
        }
        int val = validation & Validation.VALIDATION_MODE_MASK;
            if (val != Validation.PRESERVE) {
                receiver = config.getDocumentValidator(
                        receiver, getSystemId(), val, Whitespace.NONE, null);
            }
        this.setReceiver(receiver);
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
        receiver.setSystemId(url);
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

        try {
            controller.transformDocument(doc, result);
        } catch (TransformerException err) {
            if (err instanceof XPathException) {
                controller.reportFatalError((XPathException)err);
            }
            throw new SAXException(err);
        }
    }


//    public static void main(String[] args) throws Exception {
    // test case for a TransformerHandler that validates the source document
//        TransformerFactory tfactory = new SchemaAwareTransformerFactory();
//        tfactory.setAttribute(FeatureKeys.SCHEMA_VALIDATION, new Integer(Validation.STRICT));
//        // Does this factory support SAX features?
//        if (tfactory.getFeature(SAXSource.FEATURE)) {
//
//            // If so, we can safely cast.
//            SAXTransformerFactory stfactory =
//                ((SAXTransformerFactory) tfactory);
//
//            // A TransformerHandler is a ContentHandler that will listen for
//            // SAX events, and transform them to the result.
//            TransformerHandler handler =
//                stfactory.newTransformerHandler(new StreamSource(new File("c:/MyJava/samples/styles/books.xsl")));
//
//            // Set the result handling to be a serialization to System.out.
//            Result result = new StreamResult(System.out);
//
//            handler.setResult(result);
//
//            // Create a reader, and set it's content handler to be the TransformerHandler.
//            SAXParserFactory factory = SAXParserFactory.newInstance();
//            factory.setNamespaceAware(true);
//            XMLReader reader = factory.newSAXParser().getXMLReader();
//
//            reader.setContentHandler(handler);
//
//            // It's a good idea for the parser to send lexical events.
//            // The TransformerHandler is also a LexicalHandler.
//            reader.setProperty(
//                "http://xml.org/sax/properties/lexical-handler", handler);
//
//            // Parse the source XML, and send the parse events to the TransformerHandler.
//            handler.setSystemId("file:///MyJava/samples/data/books.xml");
//            reader.parse("file:///MyJava/samples/data/books.xml");
//        } else {
//            System.out.println(
//                "Can't do exampleContentHandlerToContentHandler because tfactory is not a SAXTransformerFactory");
//        }
//    }


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
