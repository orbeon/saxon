package org.orbeon.saxon.event;

import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.StandardErrorHandler;
import org.orbeon.saxon.dom.DOMSender;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;
import org.w3c.dom.Node;
import org.xml.sax.*;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

/**
* Sender is a helper class that sends events to a Receiver from any kind of Source object
*/

public class Sender {

    PipelineConfiguration pipe;
    public Sender (PipelineConfiguration pipe) {
        this.pipe = pipe;
    }

    /**
    * Send the contents of a Source to a Receiver. Note that if the Source
     * identifies an element node rather than a document node, only the subtree
     * rooted at that element will be copied.
    * @param source the document or element to be copied
    * @param receiver the destination to which it is to be copied
    */

    public void send(Source source, Receiver receiver)
    throws XPathException {
        send(source, receiver, false);
    }

    /**
     * Send the contents of a Source to a Receiver. Note that if the Source
     * identifies an element node rather than a document node, only the subtree
     * rooted at that element will be copied.
     * @param source the document or element to be copied
     * @param receiver the destination to which it is to be copied
     * @param isFinal set to true when the document is being processed purely for the
     * sake of validation, in which case multiple validation errors in the source can be
     * reported.
     */

    public void send(Source source, Receiver receiver, boolean isFinal)
    throws XPathException {
        receiver.setPipelineConfiguration(pipe);
        receiver.setSystemId(source.getSystemId());

        int validation = (pipe.getConfiguration().isSchemaValidation() ? Validation.STRICT : Validation.PRESERVE);
        if (isFinal) {
            // this ensures that the Validate command produces multiple error messages
            validation |= Validation.VALIDATE_OUTPUT;
        }

        if (source instanceof AugmentedSource) {
            Boolean localValidate = ((AugmentedSource)source).getSchemaValidation();
            if (localValidate != null) {
                validation = (localValidate.booleanValue() ? Validation.STRICT : Validation.PRESERVE);
            }
            source = ((AugmentedSource)source).getContainedSource();
        }

        if (source instanceof NodeInfo) {
            if ((validation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
                try {
                    pipe.getErrorListener().warning(
                            new TransformerException("Validation request ignored for a NodeInfo source"));
                } catch (TransformerException e) {
                    throw DynamicError.makeDynamicError(e);
                }
            }
            NodeInfo ns = (NodeInfo)source;
            int kind = ns.getNodeKind();
            if (kind != Type.DOCUMENT && kind != Type.ELEMENT) {
                throw new IllegalArgumentException("Sender can only handle document or element nodes");
            }
            sendDocumentInfo(ns, receiver, pipe.getConfiguration().getNamePool());

        } else if (source instanceof SAXSource) {
            sendSAXSource((SAXSource)source, receiver, validation);

        } else if (source instanceof DOMSource) {
            sendDOMSource((DOMSource)source, receiver, validation);

        } else if (source instanceof StreamSource) {
//            if (config.isSchemaAware()) {
//                config.builtInParse(source, receiver, validation);
//            } else {
                StreamSource ss = (StreamSource)source;
                String url = source.getSystemId();
                InputSource is = new InputSource(url);
                is.setCharacterStream(ss.getReader());
                is.setByteStream(ss.getInputStream());
                SAXSource sax = new SAXSource(pipe.getConfiguration().getSourceParser(), is);
                sax.setSystemId(source.getSystemId());
                sendSAXSource(sax, receiver, validation);
//            }
        } else {
            throw new IllegalArgumentException("Unknown type of source " + source.getClass());
        }
    }


    private void sendDocumentInfo(NodeInfo top, Receiver receiver, NamePool namePool)
    throws XPathException {
        if (top.getNamePool() != namePool) {
            // This happens for example when turning an arbitrary DocumentInfo tree into a stylesheet
            NamePoolConverter converter = new NamePoolConverter(top.getNamePool(), namePool);
            converter.setUnderlyingReceiver(receiver);
            receiver = converter;
        }
        DocumentSender sender = new DocumentSender(top);
        sender.send(receiver);
    }

    private void sendDOMSource(DOMSource source, Receiver receiver, int validation)
    throws XPathException {
        Node startNode = source.getNode();
        Configuration config = pipe.getConfiguration();
        NamePool pool = config.getNamePool();
        if (startNode instanceof DocumentInfo) {
            sendDocumentInfo((DocumentInfo)startNode, receiver, pool);
        } else {
            if ((validation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
                // Add a document validator to the pipeline
                receiver = config.getDocumentValidator(receiver,
                                                   source.getSystemId(),
                                                   pool,
                                                   validation);
            }
            DOMSender driver = new DOMSender();
            driver.setStartNode(startNode);
            driver.setReceiver(receiver);
            driver.setPipelineConfiguration(pipe);
            driver.setSystemId(source.getSystemId());
            driver.send();
        }
    }

    private void sendSAXSource(SAXSource source, Receiver receiver, int validation)
    throws XPathException {
        XMLReader parser = source.getXMLReader();
        if (parser==null) {
            SAXSource ss = new SAXSource();
            ss.setInputSource(source.getInputSource());
            ss.setSystemId(source.getSystemId());
            parser = pipe.getConfiguration().getSourceParser();
            ss.setXMLReader(parser);
            source = ss;
        }

        if (parser.getErrorHandler()==null) {
            parser.setErrorHandler(new StandardErrorHandler(pipe.getErrorListener()));
        }

		try {
        	parser.setFeature("http://xml.org/sax/features/namespaces", true);
        } catch (SAXNotSupportedException err) {    // SAX2 parsers MUST support this feature!
            throw new DynamicError(
                "The SAX2 parser does not recognize the 'namespaces' feature");
    	} catch (SAXNotRecognizedException err) {
            throw new DynamicError(
                "The SAX2 parser does not support setting the 'namespaces' feature to true");
    	}

		try {
        	parser.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        } catch (SAXNotSupportedException err) {    // SAX2 parsers MUST support this feature!
            throw new DynamicError(
                "The SAX2 parser does not recognize the 'namespace-prefixes' feature");
    	} catch (SAXNotRecognizedException err) {
            throw new DynamicError(
                "The SAX2 parser does not support setting the 'namespace-prefixes' feature to false");
    	}

        if ((validation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
            // Add a document validator to the pipeline
            Configuration config = pipe.getConfiguration();
            receiver = config.getDocumentValidator(receiver,
                                                   source.getSystemId(),
                                                   config.getNamePool(),
                                                   validation);
        }

        ReceivingContentHandler ce = new ReceivingContentHandler();
        ce.setReceiver(receiver);
        ce.setPipelineConfiguration(pipe);
        parser.setContentHandler(ce);
	    parser.setDTDHandler(ce);

        try {
    	    parser.setProperty("http://xml.org/sax/properties/lexical-handler", ce);
        } catch (SAXNotSupportedException err) {    // this just means we won't see the comments
	    } catch (SAXNotRecognizedException err) {
	    }

        try {
            parser.parse(source.getInputSource());
        } catch (SAXException err) {
            Exception nested = err.getException();
            if (nested instanceof XPathException) {
                throw (XPathException)nested;
            } else if (nested instanceof RuntimeException) {
                throw (RuntimeException)nested;
            } else {
                throw new DynamicError(err);
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
