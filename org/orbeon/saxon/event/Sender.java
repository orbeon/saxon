package net.sf.saxon.event;

import net.sf.saxon.AugmentedSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.StandardErrorHandler;
import net.sf.saxon.om.ExternalObjectModel;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Validation;
import net.sf.saxon.pull.PullProvider;
import net.sf.saxon.pull.PullPushCopier;
import net.sf.saxon.pull.PullSource;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import org.xml.sax.*;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.util.List;

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
        Configuration config = pipe.getConfiguration();
        receiver.setPipelineConfiguration(pipe);
        receiver.setSystemId(source.getSystemId());

        int validation = config.getSchemaValidationMode();
        if (isFinal) {
            // this ensures that the Validate command produces multiple error messages
            validation |= Validation.VALIDATE_OUTPUT;
        }

        XMLReader parser = null;
        if (source instanceof AugmentedSource) {
            int localValidate = ((AugmentedSource)source).getSchemaValidation();
            if (localValidate != Validation.DEFAULT) {
                validation = localValidate;
            }
            parser = ((AugmentedSource)source).getXMLReader();
            source = ((AugmentedSource)source).getContainedSource();
        }

        if (source instanceof NodeInfo) {
            int val = validation & Validation.VALIDATION_MODE_MASK;
            if (val != Validation.PRESERVE) {
                receiver = config.getDocumentValidator(receiver, source.getSystemId(), config.getNamePool(), val);
//                try {
//                    pipe.getErrorListener().warning(
//                            new TransformerException("Validation request ignored for a NodeInfo source"));
//                } catch (TransformerException e) {
//                    throw DynamicError.makeDynamicError(e);
//                }
            }
            NodeInfo ns = (NodeInfo)source;
            int kind = ns.getNodeKind();
            if (kind != Type.DOCUMENT && kind != Type.ELEMENT) {
                throw new IllegalArgumentException("Sender can only handle document or element nodes");
            }
            sendDocumentInfo(ns, receiver, pipe.getConfiguration().getNamePool());
            return;

        } else if (source instanceof PullSource) {
            // support for a PullSource is experimental
            sendPullSource((PullSource)source, receiver, validation);
            return;

        } else if (source instanceof SAXSource) {
            sendSAXSource((SAXSource)source, receiver, validation);
            return;

        } else if (source instanceof StreamSource) {
            StreamSource ss = (StreamSource)source;
            String url = source.getSystemId();
            InputSource is = new InputSource(url);
            is.setCharacterStream(ss.getReader());
            is.setByteStream(ss.getInputStream());
            if (parser == null) {
                parser = pipe.getConfiguration().getSourceParser();
            }
            SAXSource sax = new SAXSource(parser, is);
            sax.setSystemId(source.getSystemId());
            sendSAXSource(sax, receiver, validation);
            return;
        } else {
            // See if there is a registered external object model that knows about this kind of source
            if ((validation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
                // Add a document validator to the pipeline
                receiver = config.getDocumentValidator(receiver,
                                                   source.getSystemId(),
                                                   config.getNamePool(),
                                                   validation);
            }
            List externalObjectModels = pipe.getConfiguration().getExternalObjectModels();
            for (int m=0; m<externalObjectModels.size(); m++) {
                ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
                boolean done = model.sendSource(source, receiver, pipe);
                if (done) {
                    return;
                }
            }

        }
        throw new IllegalArgumentException("Unknown type of source " + source.getClass());
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

//    private void sendDOMSource(DOMSource source, Receiver receiver, int validation)
//    throws XPathException {
//        Node startNode = source.getNode();
//        Configuration config = pipe.getConfiguration();
//        NamePool pool = config.getNamePool();
//        if (startNode instanceof DocumentInfo) {
//            sendDocumentInfo((DocumentInfo)startNode, receiver, pool);
//        } else {
//            if ((validation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
//                // Add a document validator to the pipeline
//                receiver = config.getDocumentValidator(receiver,
//                                                   source.getSystemId(),
//                                                   pool,
//                                                   validation);
//            }
//            DOMSender driver = new DOMSender();
//            driver.setStartNode(startNode);
//            driver.setReceiver(receiver);
//            driver.setPipelineConfiguration(pipe);
//            driver.setSystemId(source.getSystemId());
//            driver.send();
//        }
//    }

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

    private void sendPullSource(PullSource source, Receiver receiver, int validation)
            throws XPathException {
        if (validation != Validation.PRESERVE && validation != Validation.STRIP) {
            throw new DynamicError("Validation is not currently supported with a PullSource");
        }
        receiver.open();
        PullProvider provider = source.getPullProvider();
        provider.setPipelineConfiguration(pipe);
        receiver.setPipelineConfiguration(pipe);
        PullPushCopier copier = new PullPushCopier(provider, receiver);
        copier.copy();
        receiver.close();
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
