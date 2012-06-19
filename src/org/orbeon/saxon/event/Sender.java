package org.orbeon.saxon.event;

import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.StandardErrorHandler;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.evpull.PullEventSource;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.evpull.EventIteratorToReceiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pull.PullProvider;
import org.orbeon.saxon.pull.PullPushCopier;
import org.orbeon.saxon.pull.PullSource;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.SchemaType;
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
                                     
    /**
     * Create a Sender
     * @param pipe the pipeline configuration
     */
    public Sender (PipelineConfiguration pipe) {
        this.pipe = pipe;
    }

    /**
    * Send the contents of a Source to a Receiver.
    * @param source the source to be copied
    * @param receiver the destination to which it is to be copied
    */

    public void send(Source source, Receiver receiver)
    throws XPathException {
        send(source, receiver, false);
    }

    /**
     * Send the contents of a Source to a Receiver.
     * @param source the source to be copied. Note that if the Source contains an InputStream
     * or Reader then it will be left open, unless it is an AugmentedSource with the pleaseCloseAfterUse
     * flag set. On the other hand, if it contains a URI that needs to be dereferenced to obtain
     * an InputStream, then the InputStream will be closed after use.
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
        Receiver next = receiver;

        ParseOptions options = new ParseOptions();
        options.setXIncludeAware(config.isXIncludeAware());
        int schemaValidation = config.getSchemaValidationMode();
        if (isFinal) {
            // this ensures that the Validate command produces multiple error messages
            schemaValidation |= Validation.VALIDATE_OUTPUT;
        }
        options.setSchemaValidationMode(schemaValidation);
        options.setDTDValidationMode(config.isValidation() ? Validation.STRICT : Validation.STRIP);
        options.setXIncludeAware(config.isXIncludeAware());

        int stripSpace = Whitespace.UNSPECIFIED;
//        boolean xInclude = config.isXIncludeAware();
//        boolean xqj = false;
//        boolean closeAfterUse = false;
//        int schemaValidation = config.getSchemaValidationMode();
//        int topLevelNameCode = -1;
//        int dtdValidation = config.isValidation() ? Validation.STRICT : Validation.STRIP;


        XMLReader parser = null;
        SchemaType topLevelType = null;

        if (source instanceof AugmentedSource) {
            AugmentedSource as = (AugmentedSource)source;
            options.setPleaseCloseAfterUse(as.isPleaseCloseAfterUse());
            stripSpace = as.getStripSpace();
            if (as.isXIncludeAwareSet()) {
                options.setXIncludeAware(as.isXIncludeAware());
            }
            options.setSourceIsXQJ(as.sourceIsXQJ());

            int localValidate = ((AugmentedSource)source).getSchemaValidation();
            if (localValidate != Validation.DEFAULT) {
                schemaValidation = localValidate;
                if (isFinal) {
                    // this ensures that the Validate command produces multiple error messages
                    schemaValidation |= Validation.VALIDATE_OUTPUT;
                }
                options.setSchemaValidationMode(schemaValidation);
            }

            topLevelType = ((AugmentedSource)source).getTopLevelType();
            StructuredQName topLevelName = ((AugmentedSource)source).getTopLevelElement();
            if (topLevelName != null) {
                options.setTopLevelElement(topLevelName);
            }

            int localDTDValidate = ((AugmentedSource)source).getDTDValidation();
            if (localDTDValidate != Validation.DEFAULT) {
                options.setDTDValidationMode(localDTDValidate);
            }

            parser = ((AugmentedSource)source).getXMLReader();

            List filters = ((AugmentedSource)source).getFilters();
            if (filters != null) {
                for (int i=filters.size()-1; i>=0; i--) {
                    ProxyReceiver filter = (ProxyReceiver)filters.get(i);
                    filter.setPipelineConfiguration(pipe);
                    filter.setSystemId(source.getSystemId());
                    filter.setUnderlyingReceiver(next);
                    next = filter;
                }
            }

            source = ((AugmentedSource)source).getContainedSource();
        }
        if (stripSpace == Whitespace.UNSPECIFIED) {
            stripSpace = config.getStripsWhiteSpace();
        }
        options.setStripSpace(stripSpace);
        if (stripSpace == Whitespace.ALL) {
            Stripper s = new AllElementStripper();
            s.setStripAll();
            s.setPipelineConfiguration(pipe);
            s.setUnderlyingReceiver(receiver);
            next = s;
        } else if (stripSpace == Whitespace.XSLT) {
            Controller controller = pipe.getController();
            if (controller != null) {
                next = controller.makeStripper(next);
            }
        }

        if (source instanceof NodeInfo) {
            NodeInfo ns = (NodeInfo)source;
            String baseURI = ns.getBaseURI();
            int val = schemaValidation & Validation.VALIDATION_MODE_MASK;
            if (val != Validation.PRESERVE) {
                StructuredQName topLevelName = options.getTopLevelElement();
                int topLevelNameCode = -1;
                if (topLevelName != null) {
                    topLevelNameCode = config.getNamePool().allocate(
                        topLevelName.getPrefix(), topLevelName.getNamespaceURI(), topLevelName.getLocalName());
                }
                next = config.getDocumentValidator(
                        next, baseURI, val, stripSpace, topLevelType, topLevelNameCode);
            }

            int kind = ns.getNodeKind();
            if (kind != Type.DOCUMENT && kind != Type.ELEMENT) {
                throw new IllegalArgumentException("Sender can only handle document or element nodes");
            }
            next.setSystemId(baseURI);
            sendDocumentInfo(ns, next);
            return;

        } else if (source instanceof PullSource) {
            sendPullSource((PullSource)source, next, options);
            return;

        } else if (source instanceof PullEventSource) {
            sendPullEventSource((PullEventSource)source, next, options);
            return;

        } else if (source instanceof EventSource) {
            ((EventSource)source).send(next);
            return;

        } else if (source instanceof SAXSource) {
            sendSAXSource((SAXSource)source, next, options);
            return;

        } else if (source instanceof StreamSource) {
            StreamSource ss = (StreamSource)source;
            // Following code allows the .NET platform to use a Pull parser
            boolean dtdValidation = options.getDTDValidationMode() == Validation.STRICT;
            Source ps = Configuration.getPlatform().getParserSource(
                    pipe, ss, schemaValidation, dtdValidation, stripSpace);
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
                sendSAXSource(sax, next, options);
                if (reuseParser) {
                    config.reuseSourceParser(parser);
                }
            } else {
                // the Platform substituted a different kind of source
                // On .NET with a default URIResolver we can expect an AugnmentedSource wrapping a PullSource
                send(ps, next, isFinal);
            }
            return;
        } else {
            next = makeValidator(next, source.getSystemId(), options);

            // See if there is a registered SourceResolver than can handle it
            Source newSource = config.getSourceResolver().resolveSource(source, config);
            if (newSource instanceof StreamSource ||
                    newSource instanceof SAXSource ||
                    newSource instanceof NodeInfo ||
                    newSource instanceof PullSource ||
                    newSource instanceof AugmentedSource ||
                    newSource instanceof EventSource) {
                send(newSource, next, isFinal);
            }

            // See if there is a registered external object model that knows about this kind of source

            List externalObjectModels = config.getExternalObjectModels();
            for (int m=0; m<externalObjectModels.size(); m++) {
                ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
                boolean done = model.sendSource(source, next, pipe);
                if (done) {
                    return;
                }
            }

        }
        if (source instanceof DOMSource) {
            throw new XPathException("DOMSource cannot be processed: check that saxon9-dom.jar is on the classpath");
        }
        throw new XPathException("A source of type " + source.getClass().getName() +
                " is not supported in this environment");
    }

    /**
     * Send a copy of a Saxon NodeInfo document or element to a receiver
     * @param top the root of the subtree to be send. Despite the method name, this can be a document
     * node or an element node
     * @param receiver the destination to receive the events
     * @throws XPathException if any error occurs
     */


    private void sendDocumentInfo(NodeInfo top, Receiver receiver)
    throws XPathException {
        NamePool targetNamePool = pipe.getConfiguration().getNamePool();
        if (top.getNamePool() != targetNamePool) {
            // This code allows a document in one Configuration to be copied to another, changing
            // namecodes as necessary
            NamePoolConverter converter = new NamePoolConverter(top.getNamePool(), targetNamePool);
            converter.setUnderlyingReceiver(receiver);
            converter.setPipelineConfiguration(receiver.getPipelineConfiguration());
            receiver = converter;
        }
        DocumentSender sender = new DocumentSender(top);
        sender.send(receiver);
    }

    /**
     * Send the contents of a SAXSource to a given Receiver
     * @param source the SAXSource
     * @param receiver the destination Receiver
     * @param options options for parsing the SAXSource
     * @throws XPathException
     */

    private void sendSAXSource(SAXSource source, Receiver receiver, ParseOptions options)
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

        if (!pipe.isExpandAttributeDefaults()) { //TODO: put this in ParseOptions
            try {
                parser.setFeature("http://xml.org/sax/features/use-attributes2", true);
            } catch (SAXNotRecognizedException err) {
                // ignore the failure, we did our best (Xerces gives us an Attribute2 even though it
                // doesn't recognize this request!)
            } catch (SAXNotSupportedException err) {
                // ignore the failure, we did our best
            }
        }

        boolean dtdValidation = options.getDTDValidationMode() == Validation.STRICT;
        try {
            parser.setFeature("http://xml.org/sax/features/validation", dtdValidation);
        } catch (SAXNotRecognizedException err) {
            if (dtdValidation) {
                throw new XPathException("XML Parser does not recognize request for DTD validation");
            }
        } catch (SAXNotSupportedException err) {
            if (dtdValidation) {
                throw new XPathException("XML Parser does not support DTD validation");
            }
        }

        boolean xInclude = options.isXIncludeAware();
        if (xInclude) {
            boolean tryAgain = false;
            try {
                // This feature name is supported in the version of Xerces bundled with JDK 1.5
                parser.setFeature("http://apache.org/xml/features/xinclude-aware", true);
            } catch (SAXNotRecognizedException err) {
                tryAgain = true;
            } catch (SAXNotSupportedException err) {
                tryAgain = true;
            }
            if (tryAgain) {
                try {
                    // This feature name is supported in Xerces 2.9.0
                    parser.setFeature("http://apache.org/xml/features/xinclude", true);
                } catch (SAXNotRecognizedException err) {
                    throw new XPathException("Selected XML parser " + parser.getClass().getName() +
                            " does not recognize request for XInclude processing");
                } catch (SAXNotSupportedException err) {
                    throw new XPathException("Selected XML parser " + parser.getClass().getName() +
                            " does not support XInclude processing");
                }
            }
            // TODO: need to unset the parser property when it is returned to the cache?
        }
//        if (config.isTiming()) {
//            System.err.println("Using SAX parser " + parser);
//        }
        StandardErrorHandler errorHandler = new StandardErrorHandler(pipe.getErrorListener());
                // TODO: what about the ErrorListener in the ParseOptions?
        parser.setErrorHandler(errorHandler);


        receiver = makeValidator(receiver, source.getSystemId(), options);

        // Reuse the previous ReceivingContentHandler if possible (it contains a useful cache of names)

        ReceivingContentHandler ce;
        final ContentHandler ch = parser.getContentHandler();
        if (ch instanceof ReceivingContentHandler && config.isCompatible(((ReceivingContentHandler)ch).getConfiguration())) {
            ce = (ReceivingContentHandler)ch;
            ce.reset();
        } else {
            ce = new ReceivingContentHandler();
            parser.setContentHandler(ce);
            parser.setDTDHandler(ce);
            try {
                parser.setProperty("http://xml.org/sax/properties/lexical-handler", ce);
            } catch (SAXNotSupportedException err) {    // this just means we won't see the comments
                // ignore the error
            } catch (SAXNotRecognizedException err) {
                // ignore the error
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

        boolean xqj = options.sourceIsXQJ();
        try {
            if (xqj) {
                parser.parse("dummy");
            } else {
                parser.parse(source.getInputSource());
            }
        } catch (SAXException err) {
            Exception nested = err.getException();
            if (nested instanceof XPathException) {
                throw (XPathException)nested;
            } else if (nested instanceof RuntimeException) {
                throw (RuntimeException)nested;
            } else {
                if (errorHandler.getErrorCount() == 0) {
                    // The built-in parser for JDK 1.6 has a nasty habit of not notifying errors to the ErrorHandler
                    throw new XPathException("Error reported by XML parser processing " +
                            source.getSystemId() + ": " + err.getMessage());
                } else {
                    XPathException de = new XPathException(err);
                    de.setHasBeenReported();
                    throw de;
                }
            }
        } catch (java.io.IOException err) {
            throw new XPathException(err);
        }
        if (errorHandler.getErrorCount() > 0) {
            throw new XPathException("The XML parser reported one or more errors");
        }
        if (reuseParser) {
            config.reuseSourceParser(parser);
        }
    }

    private Receiver makeValidator(Receiver receiver, String systemId, ParseOptions options) {
        Configuration config = pipe.getConfiguration();
        int schemaValidation = options.getSchemaValidationMode();
        if ((schemaValidation & Validation.VALIDATION_MODE_MASK) != Validation.PRESERVE) {
            // Add a document validator to the pipeline
            int stripSpace = options.getStripSpace();
            SchemaType topLevelType = options.getTopLevelType();
            StructuredQName topLevelElement = options.getTopLevelElement();
            int topLevelElementCode = -1;
            if (topLevelElement != null) {
                topLevelElementCode = config.getNamePool().allocate(
                        topLevelElement.getPrefix(), topLevelElement.getNamespaceURI(), topLevelElement.getLocalName());
            }
            receiver = config.getDocumentValidator(
                    receiver, systemId,
                    schemaValidation, stripSpace, topLevelType, topLevelElementCode);
        }
        return receiver;
    }

    private void sendPullSource(PullSource source, Receiver receiver, ParseOptions options)
            throws XPathException {
        boolean xInclude = options.isXIncludeAware();
        if (xInclude) {
            throw new XPathException("XInclude processing is not supported with a pull parser");
        }
        receiver = makeValidator(receiver, source.getSystemId(), options);

        PullProvider provider = source.getPullProvider();
        if (provider instanceof LocationProvider) {
            pipe.setLocationProvider((LocationProvider)provider);
        }
        provider.setPipelineConfiguration(pipe);
        receiver.setPipelineConfiguration(pipe);
        PullPushCopier copier = new PullPushCopier(provider, receiver);
        try {
            copier.copy();
        } finally {
            if (options.isPleaseCloseAfterUse()) {
                provider.close();
            }
        }
    }

    private void sendPullEventSource(PullEventSource source, Receiver receiver, ParseOptions options)
            throws XPathException {
        boolean xInclude = options.isXIncludeAware();
        if (xInclude) {
            throw new XPathException("XInclude processing is not supported with a pull parser");
        }
        receiver = makeValidator(receiver, source.getSystemId(), options);

        receiver.open();
        EventIterator provider = source.getEventIterator();
        if (provider instanceof LocationProvider) {
            pipe.setLocationProvider((LocationProvider)provider);
        }
        receiver.setPipelineConfiguration(pipe);
        SequenceReceiver out = receiver instanceof SequenceReceiver
                ? ((SequenceReceiver)receiver)
                : new TreeReceiver(receiver);
        EventIteratorToReceiver.copy(provider, out);
        receiver.close();
    }


    /**
     * Configure a SAX parser to ensure it has the correct namesapce properties set
     * @param parser the parser to be configured
     */

    public static void configureParser(XMLReader parser) throws XPathException {
        try {
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
        } catch (SAXNotSupportedException err) {    // SAX2 parsers MUST support this feature!
            throw new XPathException(
                "The SAX2 parser " + parser.getClass().getName() +
                        " does not recognize the 'namespaces' feature");
        } catch (SAXNotRecognizedException err) {
            throw new XPathException(
                "The SAX2 parser " + parser.getClass().getName() +
                        " does not support setting the 'namespaces' feature to true");
        }

        try {
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        } catch (SAXNotSupportedException err) {    // SAX2 parsers MUST support this feature!
            throw new XPathException(
                "The SAX2 parser "+ parser.getClass().getName() +
                        " does not recognize the 'namespace-prefixes' feature");
        } catch (SAXNotRecognizedException err) {
            throw new XPathException(
                "The SAX2 parser " + parser.getClass().getName() +
                        " does not support setting the 'namespace-prefixes' feature to false");
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
