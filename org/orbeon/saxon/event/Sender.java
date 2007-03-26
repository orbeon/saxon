package org.orbeon.saxon.event;

import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.StandardErrorHandler;
import org.orbeon.saxon.om.ExternalObjectModel;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pull.PullProvider;
import org.orbeon.saxon.pull.PullPushCopier;
import org.orbeon.saxon.pull.PullSource;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.*;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
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

        int stripSpace = Whitespace.UNSPECIFIED;

        int schemaValidation = config.getSchemaValidationMode();
        int dtdValidation = config.isValidation() ? Validation.STRICT : Validation.STRIP;
        if (isFinal) {
            // this ensures that the Validate command produces multiple error messages
            schemaValidation |= Validation.VALIDATE_OUTPUT;
        }

        XMLReader parser = null;
        if (source instanceof AugmentedSource) {
            stripSpace = ((AugmentedSource)source).getStripSpace();
            int localValidate = ((AugmentedSource)source).getSchemaValidation();
            if (localValidate != Validation.DEFAULT) {
                schemaValidation = localValidate;
            }
            int localDTDValidate = ((AugmentedSource)source).getDTDValidation();
            if (localDTDValidate != Validation.DEFAULT) {
                dtdValidation = localDTDValidate;
            }
            parser = ((AugmentedSource)source).getXMLReader();

            List filters = ((AugmentedSource)source).getFilters();
            if (filters != null) {
                for (int i=filters.size()-1; i>=0; i--) {
                    ProxyReceiver filter = (ProxyReceiver)filters.get(i);
                    filter.setPipelineConfiguration(pipe);
                    filter.setSystemId(source.getSystemId());
                    filter.setUnderlyingReceiver(receiver);
                    receiver = filter;
                }
            }

            source = ((AugmentedSource)source).getContainedSource();
        }

        if (source instanceof NodeInfo) {
            NodeInfo ns = (NodeInfo)source;
            String baseURI = ns.getBaseURI();
            int val = schemaValidation & Validation.VALIDATION_MODE_MASK;
            if (val != Validation.PRESERVE) {
                receiver = config.getDocumentValidator(
                        receiver, baseURI, val, Whitespace.NONE, null);
            }

            int kind = ns.getNodeKind();
            if (kind != Type.DOCUMENT && kind != Type.ELEMENT) {
                throw new IllegalArgumentException("Sender can only handle document or element nodes");
            }
            receiver.setSystemId(baseURI);
            sendDocumentInfo(ns, receiver);
            return;

        } else if (source instanceof PullSource) {
            sendPullSource((PullSource)source, receiver, schemaValidation, stripSpace);
            return;

        } else if (source instanceof SAXSource) {
            sendSAXSource((SAXSource)source, receiver, schemaValidation, stripSpace);
            return;

        } else if (source instanceof StreamSource) {
            StreamSource ss = (StreamSource)source;
            // Following code allows the .NET platform to use a Pull parser
            Source ps = config.getPlatform().getParserSource(ss, schemaValidation,
                    (dtdValidation == Validation.STRICT), stripSpace);
            if (ps == ss) {
                String url = source.getSystemId();
                InputSource is = new InputSource(url);
                is.setCharacterStream(ss.getReader());
                is.setByteStream(ss.getInputStream());
                boolean reuseParser = false;
                if (parser == null) {
                    parser = config.getSourceParser();
                    reuseParser = true;
                }
                SAXSource sax = new SAXSource(parser, is);
                sax.setSystemId(source.getSystemId());
                sendSAXSource(sax, receiver, schemaValidation, stripSpace);
                if (reuseParser) {
                    config.reuseSourceParser(parser);
                }
            } else {
                // the Platform substituted a different kind of source
                send(ps, receiver, isFinal);
            }
            return;
        } else {
            if ((schemaValidation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
                // Add a document validator to the pipeline
                receiver = config.getDocumentValidator(receiver,
                                                   source.getSystemId(),
                        schemaValidation, stripSpace, null);
            }

            // See if there is a registered SourceResolver than can handle it
            Source newSource = config.getSourceResolver().resolveSource(source, config);
            if (newSource instanceof StreamSource ||
                    newSource instanceof SAXSource ||
                    newSource instanceof NodeInfo ||
                    newSource instanceof PullSource) {
                send(newSource, receiver, isFinal);
            }

            // See if there is a registered external object model that knows about this kind of source

            List externalObjectModels = config.getExternalObjectModels();
            for (int m=0; m<externalObjectModels.size(); m++) {
                ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
                boolean done = model.sendSource(source, receiver, pipe);
                if (done) {
                    return;
                }
            }

        }
        if (source instanceof DOMSource) {
            throw new DynamicError("DOMSource cannot be processed: check that saxon8-dom.jar is on the classpath");
        }
        throw new DynamicError("A source of type " + source.getClass().getName() +
                " is not supported in this environment");
    }


    private void sendDocumentInfo(NodeInfo top, Receiver receiver)
    throws XPathException {
        NamePool targetNamePool = pipe.getConfiguration().getNamePool();
        if (top.getNamePool() != targetNamePool) {
            // This happens for example when turning an arbitrary DocumentInfo tree into a stylesheet
            // TODO: code probably untested, possibly unreachable
            NamePoolConverter converter = new NamePoolConverter(top.getNamePool(), targetNamePool);
            converter.setUnderlyingReceiver(receiver);
            //PipelineConfiguration newpipe = pipe.getConfiguration().makePipelineConfiguration();

            converter.setPipelineConfiguration(receiver.getPipelineConfiguration());
            receiver = converter;
        }
        DocumentSender sender = new DocumentSender(top);
        sender.send(receiver);
    }

    private void sendSAXSource(SAXSource source, Receiver receiver, int validation, int stripSpace)
    throws XPathException {
        XMLReader parser = source.getXMLReader();
        boolean reuseParser = false;
        final Configuration config = pipe.getConfiguration();
        if (parser==null) {
            SAXSource ss = new SAXSource();
            ss.setInputSource(source.getInputSource());
            ss.setSystemId(source.getSystemId());
            parser = config.getSourceParser();
            ss.setXMLReader(parser);
            source = ss;
            reuseParser = true;
        } else {
            // user-supplied parser: ensure that it meets the namespace requirements
            configureParser(parser);
        }
//        if (config.isTiming()) {
//            System.err.println("Using SAX parser " + parser);
//        }
        parser.setErrorHandler(new StandardErrorHandler(pipe.getErrorListener()));


        if ((validation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
            // Add a document validator to the pipeline
            receiver = config.getDocumentValidator(receiver,
                                                   source.getSystemId(),
                    validation, stripSpace, null);
        }

        // Reuse the previous ReceivingContentHandler if possible (it contains a useful cache of names)

        ReceivingContentHandler ce;
        final ContentHandler ch = parser.getContentHandler();
        if (ch instanceof ReceivingContentHandler) {
            ce = (ReceivingContentHandler)ch;
            ce.reset();
        } else {
            ce = new ReceivingContentHandler();
            parser.setContentHandler(ce);
            parser.setDTDHandler(ce);
            try {
                parser.setProperty("http://xml.org/sax/properties/lexical-handler", ce);
            } catch (SAXNotSupportedException err) {    // this just means we won't see the comments
            } catch (SAXNotRecognizedException err) {
            }
        }
//        TracingFilter tf = new TracingFilter();
//        tf.setUnderlyingReceiver(receiver);
//        tf.setPipelineConfiguration(pipe);
//        receiver = tf;

        ce.setReceiver(receiver);
        ce.setPipelineConfiguration(pipe);

//        if (stripSpace == Whitespace.IGNORABLE) {
//            ce.setIgnoreIgnorableWhitespace(true);
//        } else if (stripSpace == Whitespace.NONE) {
//            ce.setIgnoreIgnorableWhitespace(false);
//        }

        try {
            parser.parse(source.getInputSource());
        } catch (SAXException err) {
            Exception nested = err.getException();
            if (nested instanceof XPathException) {
                throw (XPathException)nested;
            } else if (nested instanceof RuntimeException) {
                throw (RuntimeException)nested;
            } else {
                DynamicError de = new DynamicError(err);
                de.setHasBeenReported();
                throw de;
            }
        } catch (java.io.IOException err) {
            throw new DynamicError(err);
        }
        if (reuseParser) {
            config.reuseSourceParser(parser);
        }
    }

    private void sendPullSource(PullSource source, Receiver receiver, int validation, int stripSpace)
            throws XPathException {
//        if (validation != Validation.PRESERVE && validation != Validation.STRIP) {
//            throw new DynamicError("Validation is not currently supported with a PullSource");
//        }

        if ((validation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
            // Add a document validator to the pipeline
            final Configuration config = pipe.getConfiguration();
            receiver = config.getDocumentValidator(receiver,
                                                   source.getSystemId(),
                    validation, stripSpace, null);
        }

        receiver.open();
        PullProvider provider = source.getPullProvider();
        if (provider instanceof LocationProvider) {
            pipe.setLocationProvider((LocationProvider)provider);
        }
        provider.setPipelineConfiguration(pipe);
        receiver.setPipelineConfiguration(pipe);
        PullPushCopier copier = new PullPushCopier(provider, receiver);
        copier.copy();
        receiver.close();
    }

    /**
     * Configure a SAX parser to ensure it has the correct namesapce properties set
     */

    public static void configureParser(XMLReader parser) throws DynamicError {
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
