package org.orbeon.saxon.event;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.om.ExternalObjectModel;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

/**
* Helper class to construct a serialization pipeline for a given result destination
* and a given set of output properties. The pipeline is represented by a Receiver object
* to which result tree events are sent.
 *
 * Since Saxon 8.8 is is possible to write a subclass of SerializerFactory and register it
 * with the Configuration, allowing customisation of the Serializer pipeline.
 *
 * The class includes methods for instantiating each of the components used on the Serialization
 * pipeline. This allows a customized SerializerFactory to replace any or all of these components
 * by subclasses that refine the behaviour.
 *
*/

// renamed in Saxon 8.7 - previously named ResultWrapper
// changed in Saxon 8.8 to be instantiable, and registered with the Configuration

public class SerializerFactory implements Serializable {

    /**
     * Create a SerializerFactory
     */

    public SerializerFactory() {
    }

    /**
     * Get a Receiver that wraps a given Result object. Saxon calls this method to construct
     * a serialization pipeline. The method can be overridden in a subclass; alternatively, the
     * subclass can override the various methods used to instantiate components of the serialization
     * pipeline.
     *
     * <p>Note that this method ignores the {@link org.orbeon.saxon.event.SaxonOutputKeys#WRAP} output property. If
     * wrapped output is required, the user must create a {@link org.orbeon.saxon.query.SequenceWrapper} directly.</p>
     *
     * @param result The final destination of the serialized output. Usually a StreamResult,
     * but other kinds of Result are possible.
     * @param pipe The PipelineConfiguration.
     * @param props The serialization properties
     * @return the newly constructed Receiver that performs the required serialization
    */

    public Receiver getReceiver(Result result,
                                PipelineConfiguration pipe,
                                Properties props)
                                    throws XPathException {
        if (result instanceof Emitter) {
            if (((Emitter)result).getOutputProperties() == null) {
                ((Emitter)result).setOutputProperties(props);
            }
            return (Emitter)result;
        } else if (result instanceof Receiver) {
            Receiver receiver = (Receiver)result;
            receiver.setSystemId(result.getSystemId());
            receiver.setPipelineConfiguration(pipe);
            return receiver;
        } else if (result instanceof SAXResult) {
            ContentHandlerProxy proxy = newContentHandlerProxy();
            proxy.setUnderlyingContentHandler(((SAXResult)result).getHandler());
            proxy.setPipelineConfiguration(pipe);
            proxy.setOutputProperties(props);
            if ("yes".equals(props.getProperty(SaxonOutputKeys.SUPPLY_SOURCE_LOCATOR))) {
                if (pipe.getConfiguration().isCompileWithTracing()) {
                    pipe.getController().addTraceListener(proxy.getTraceListener());
                } else {
                    XPathException de = new XPathException("Cannot use saxon:supply-source-locator unless tracing was enabled at compile time");
                    de.setErrorCode(SaxonErrorCode.SXSE0002);
                    throw de;
                }
            }
            //proxy.open();
            return proxy;
        } else if (result instanceof StreamResult) {

            // The "target" is the start of the output pipeline, the Receiver that
            // instructions will actually write to (except that other things like a
            // NamespaceReducer may get added in front of it). The "emitter" is the
            // last thing in the output pipeline, the Receiver that actually generates
            // characters or bytes that are written to the StreamResult.

            Receiver target;
            String method = props.getProperty(OutputKeys.METHOD);
            if (method==null) {
            	target = newUncommittedSerializer(result, props);
                target.setPipelineConfiguration(pipe);
                return target;
            }

            Emitter emitter;

            CharacterMapExpander characterMapExpander = null;
            String useMaps = props.getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
            if (useMaps != null) {
                Controller controller = (pipe == null ? null : pipe.getController());
                if (controller == null) {
                    XPathException de = new XPathException("Cannot use character maps in an environment with no Controller");
                    de.setErrorCode(SaxonErrorCode.SXSE0001);
                    throw de;
                }
                characterMapExpander = controller.makeCharacterMapExpander(useMaps, this);
                characterMapExpander.setPipelineConfiguration(pipe);
            }

            ProxyReceiver normalizer = null;
            String normForm = props.getProperty(SaxonOutputKeys.NORMALIZATION_FORM);
            if (normForm != null && !normForm.equals("none")) {
                normalizer = newUnicodeNormalizer(pipe, props);
            }

            if ("html".equals(method)) {
                emitter = newHTMLEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = createHTMLSerializer(emitter, props, pipe, characterMapExpander, normalizer);

            } else if ("xml".equals(method)) {
                emitter = newXMLEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = createXMLSerializer(emitter, props, pipe, characterMapExpander, normalizer);

            } else if ("xhtml".equals(method)) {
                emitter = newXHTMLEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = createXHTMLSerializer(emitter, props, pipe, characterMapExpander, normalizer);

            } else if ("text".equals(method)) {
                emitter = newTEXTEmitter();
                emitter.setPipelineConfiguration(pipe);
                target = createTextSerializer(emitter, props, pipe, characterMapExpander, normalizer);

            } else if (SaxonOutputKeys.SAXON_XQUERY_METHOD.equals(method)) {
                emitter = new XQueryEmitter();
                emitter.setPipelineConfiguration(pipe);
                props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                target = createXMLSerializer(emitter, props, pipe, characterMapExpander, normalizer);
                
            } else {
                Receiver userReceiver;
                if (pipe == null) {
                    throw new XPathException("Unsupported serialization method " + method);
                } else {
                    userReceiver = createUserDefinedOutputMethod(method, props, pipe);
                    target = userReceiver;
                    if (userReceiver instanceof Emitter) {
                        emitter = (Emitter)userReceiver;
                    } else {
                        return userReceiver;
                    }
                }
            }
            emitter.setOutputProperties(props);
            StreamResult sr = (StreamResult)result;
            emitter.setStreamResult(sr);
            return target;

        } else {
            if (pipe != null) {
                // try to find an external object model that knows this kind of Result
                List externalObjectModels = pipe.getConfiguration().getExternalObjectModels();
                for (int m=0; m<externalObjectModels.size(); m++) {
                    ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
                    Receiver builder = model.getDocumentBuilder(result);
                    if (builder != null) {
                        builder.setSystemId(result.getSystemId());
                        builder.setPipelineConfiguration(pipe);
                        return builder;
                    }
                }
            }

            if (result instanceof DOMResult) {
                throw new UnsupportedOperationException(
                        "Saxon cannot write a DOMResult unless saxon9-dom.jar is on the classpath");
            }
        }

        throw new IllegalArgumentException("Unknown type of result: " + result.getClass());
    }

    /**
     * Create a serialization pipeline to implement the HTML output method. This method is protected
     * so that it can be customized in a user-written SerializerFactory
     * @param emitter the emitter at the end of the pipeline (created using the method {@link #newHTMLEmitter}
     * @param props the serialization properties
     * @param pipe the pipeline configuration information
     * @param characterMapExpander the filter to be used for expanding character maps defined in the stylesheet
     * @param normalizer the filter used for Unicode normalization
     * @return a Receiver acting as the entry point to the serialization pipeline
     * @throws XPathException if a failure occurs
     */

    protected Receiver createHTMLSerializer(
            Emitter emitter, Properties props, PipelineConfiguration pipe,
            CharacterMapExpander characterMapExpander, ProxyReceiver normalizer) throws XPathException {
        Receiver target;
        target = emitter;
        if (!"no".equals(props.getProperty(OutputKeys.INDENT))) {
            ProxyReceiver in = newHTMLIndenter(pipe, props);
            in.setUnderlyingReceiver(target);
            target=in;
        }
        if (normalizer != null) {
            normalizer.setUnderlyingReceiver(target);
            target = normalizer;
        }
        if (characterMapExpander != null) {
            characterMapExpander.setUnderlyingReceiver(target);
            target = characterMapExpander;
        }
        if (!"no".equals(props.getProperty(SaxonOutputKeys.ESCAPE_URI_ATTRIBUTES))) {
            ProxyReceiver escaper = newHTMLURIEscaper(pipe, props);
            escaper.setUnderlyingReceiver(target);
            target = escaper;
        }
        if (!"no".equals(props.getProperty(SaxonOutputKeys.INCLUDE_CONTENT_TYPE))) {
            ProxyReceiver mta = newHTMLMetaTagAdjuster(pipe, props);
            mta.setUnderlyingReceiver(target);
            target=mta;
        }
        return target;
    }

    /**
     * Create a serialization pipeline to implement the text output method. This method is protected
     * so that it can be customized in a user-written SerializerFactory
     * @param emitter the emitter at the end of the pipeline (created using the method {@link #newTEXTEmitter}
     * @param props the serialization properties
     * @param pipe the pipeline configuration information
     * @param characterMapExpander the filter to be used for expanding character maps defined in the stylesheet
     * @param normalizer the filter used for Unicode normalization
     * @return a Receiver acting as the entry point to the serialization pipeline
     * @throws XPathException if a failure occurs
     */

    protected Receiver createTextSerializer(
            Emitter emitter, Properties props, PipelineConfiguration pipe,
            CharacterMapExpander characterMapExpander, ProxyReceiver normalizer) throws XPathException {
        Receiver target;
        target = emitter;
        if (characterMapExpander != null) {
            characterMapExpander.setUnderlyingReceiver(target);
            characterMapExpander.setUseNullMarkers(false);
            target = characterMapExpander;
        }
        if (normalizer != null) {
            normalizer.setUnderlyingReceiver(target);
            target = normalizer;
        }
        return target;
    }

    /**
     * Create a serialization pipeline to implement the XHTML output method. This method is protected
     * so that it can be customized in a user-written SerializerFactory
     * @param emitter the emitter at the end of the pipeline (created using the method {@link #newXHTMLEmitter}
     * @param props the serialization properties
     * @param pipe the pipeline configuration information
     * @param characterMapExpander the filter to be used for expanding character maps defined in the stylesheet
     * @param normalizer the filter used for Unicode normalization
     * @return a Receiver acting as the entry point to the serialization pipeline
     * @throws XPathException if a failure occurs
     */

    protected Receiver createXHTMLSerializer(
            Emitter emitter, Properties props, PipelineConfiguration pipe,
            CharacterMapExpander characterMapExpander, ProxyReceiver normalizer) throws XPathException {
        // Ensure that the XHTML namespace is registered in the NamePool. Without this, the meta-tag insertion can fail
        pipe.getConfiguration().getNamePool().allocateCodeForURI(NamespaceConstant.XHTML);
        Receiver target;
        target = emitter;
        if (!"no".equals(props.getProperty(OutputKeys.INDENT))) {
            ProxyReceiver in = newXHTMLIndenter(pipe, props);
            in.setUnderlyingReceiver(target);
            target=in;
        }
        if (normalizer != null) {
            normalizer.setUnderlyingReceiver(target);
            target = normalizer;
        }
        if (characterMapExpander != null) {
            characterMapExpander.setUnderlyingReceiver(target);
            characterMapExpander.setPipelineConfiguration(pipe);
            target = characterMapExpander;
        }
        String cdataElements = props.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
        if (cdataElements!=null && cdataElements.length()>0) {
            ProxyReceiver filter = newCDATAFilter(pipe, props);
            filter.setUnderlyingReceiver(target);
            target = filter;
        }
        if (!"no".equals(props.getProperty(SaxonOutputKeys.ESCAPE_URI_ATTRIBUTES))) {
            ProxyReceiver escaper = newHTMLURIEscaper(pipe, props);
            escaper.setUnderlyingReceiver(target);
            target = escaper;
        }
        if (!"no".equals(props.getProperty(SaxonOutputKeys.INCLUDE_CONTENT_TYPE))) {
            ProxyReceiver mta = newXHTMLMetaTagAdjuster(pipe, props);
            mta.setUnderlyingReceiver(target);
            target=mta;
        }
        return target;
    }

   /**
     * Create a serialization pipeline to implement the XML output method. This method is protected
     * so that it can be customized in a user-written SerializerFactory
     * @param emitter the emitter at the end of the pipeline (created using the method {@link #newXHTMLEmitter}
     * @param props the serialization properties
     * @param pipe the pipeline configuration information
     * @param characterMapExpander the filter to be used for expanding character maps defined in the stylesheet
     * @param normalizer the filter used for Unicode normalization
     * @return a Receiver acting as the entry point to the serialization pipeline
     * @throws XPathException if a failure occurs
     */

    protected Receiver createXMLSerializer(
            Emitter emitter, Properties props, PipelineConfiguration pipe,
            CharacterMapExpander characterMapExpander, ProxyReceiver normalizer) throws XPathException {
        Receiver target;
        target = emitter;
        if ("1.0".equals(props.getProperty(OutputKeys.VERSION)) &&
                pipe.getConfiguration().getXMLVersion() == Configuration.XML11) {
            // Check result meets XML 1.0 constraints if configuration allows XML 1.1 input but
            // this result document must conform to 1.0
            ProxyReceiver in = newXML10ContentChecker(pipe, props);
            in.setUnderlyingReceiver(target);
            target=in;
        }
        if ("yes".equals(props.getProperty(OutputKeys.INDENT))) {
            ProxyReceiver in = newXMLIndenter(pipe, props);
            in.setUnderlyingReceiver(target);
            target=in;
        }
        if (normalizer != null) {
            normalizer.setUnderlyingReceiver(target);
            target = normalizer;
        }
        if (characterMapExpander != null) {
            characterMapExpander.setUnderlyingReceiver(target);
            target = characterMapExpander;
        }
        String cdataElements = props.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
        if (cdataElements!=null && cdataElements.length()>0) {
            ProxyReceiver filter = newCDATAFilter(pipe, props);
            filter.setUnderlyingReceiver(target);
            target = filter;
        }
        return target;
    }

   /**
     * Create a serialization pipeline to implement a user-defined output method. This method is protected
     * so that it can be customized in a user-written SerializerFactory
     * @param method the name of the user-defined output method, as a QName in Clark format
     * (that is "{uri}local").
     * @param props the serialization properties
     * @param pipe the pipeline configuration information
     * @return a Receiver acting as the entry point to the serialization pipeline
     * @throws XPathException if a failure occurs
     */

    protected Receiver createUserDefinedOutputMethod(String method, Properties props, PipelineConfiguration pipe) throws XPathException {
        Receiver userReceiver;// See if this output method is recognized by the Configuration
        userReceiver = pipe.getConfiguration().makeEmitter(method, pipe.getController());
        userReceiver.setPipelineConfiguration(pipe);
        if (userReceiver instanceof ContentHandlerProxy &&
                "yes".equals(props.getProperty(SaxonOutputKeys.SUPPLY_SOURCE_LOCATOR))) {
            if (pipe.getConfiguration().isCompileWithTracing()) {
                pipe.getController().addTraceListener(
                        ((ContentHandlerProxy)userReceiver).getTraceListener());
            } else {
                XPathException de = new XPathException(
                        "Cannot use saxon:supply-source-locator unless tracing was enabled at compile time");
                de.setErrorCode(SaxonErrorCode.SXSE0002);
                throw de;
            }
        }
        return userReceiver;
    }


    /**
     * Create a ContentHandlerProxy. This method exists so that it can be overridden in a subclass.
     * @return the newly created ContentHandlerProxy.
     */

    protected ContentHandlerProxy newContentHandlerProxy() {
        return new ContentHandlerProxy();
    }

    /**
     * Create an UncommittedSerializer. This method exists so that it can be overridden in a subclass.
     * @param result the result destination
     * @param properties the serialization properties
     * @return the newly created UncommittedSerializer.
     */

    protected UncommittedSerializer newUncommittedSerializer(Result result, Properties properties) {
        return new UncommittedSerializer(result, properties);
    }

    /**
     * Create a new XML Emitter. This method exists so that it can be overridden in a subclass.
     * @return the newly created XML emitter.
     */

    protected Emitter newXMLEmitter() {
        return new XMLEmitter();
    }

    /**
     * Create a new HTML Emitter. This method exists so that it can be overridden in a subclass.
     * @return the newly created HTML emitter.
     */

    protected Emitter newHTMLEmitter() {
        return new HTMLEmitter();
    }

    /**
     * Create a new XHTML Emitter. This method exists so that it can be overridden in a subclass.
     * @return the newly created XHTML emitter.
     */

    protected Emitter newXHTMLEmitter() {
        return new XHTMLEmitter();
    }

    /**
     * Create a new Text Emitter. This method exists so that it can be overridden in a subclass.
     * @return the newly created text emitter.
     */

    protected Emitter newTEXTEmitter() {
        return new TEXTEmitter();
    }


    /**
     * Create a new XML Indenter. This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created XML indenter.
     */

    protected ProxyReceiver newXMLIndenter(PipelineConfiguration pipe, Properties outputProperties) {
        XMLIndenter r = new XMLIndenter();
        r.setPipelineConfiguration(pipe);
        r.setOutputProperties(outputProperties);
        return r;
    }

    /**
     * Create a new HTML Indenter. This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created HTML indenter.
     */

    protected ProxyReceiver newHTMLIndenter(PipelineConfiguration pipe, Properties outputProperties) {
        HTMLIndenter r = new HTMLIndenter();
        r.setPipelineConfiguration(pipe);
        r.setOutputProperties(outputProperties);
        return r;
    }

    /**
     * Create a new XHTML Indenter. This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created XHTML indenter.
     */

    protected ProxyReceiver newXHTMLIndenter(PipelineConfiguration pipe, Properties outputProperties) {
        XHTMLIndenter r = new XHTMLIndenter();
        r.setPipelineConfiguration(pipe);
        r.setOutputProperties(outputProperties);
        return r;
    }

    /**
     * Create a new XHTML MetaTagAdjuster, responsible for insertion, removal, or replacement of meta
     * elements. This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created XHTML MetaTagAdjuster.
     */

    protected MetaTagAdjuster newXHTMLMetaTagAdjuster(PipelineConfiguration pipe, Properties outputProperties) {
        MetaTagAdjuster r = new MetaTagAdjuster();
        r.setPipelineConfiguration(pipe);
        r.setOutputProperties(outputProperties);
        r.setIsXHTML(true);
        return r;
    }

    /**
     * Create a new XHTML MetaTagAdjuster, responsible for insertion, removal, or replacement of meta
     * elements. This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created HTML MetaTagAdjuster.
     */

    protected MetaTagAdjuster newHTMLMetaTagAdjuster(PipelineConfiguration pipe, Properties outputProperties) {
        MetaTagAdjuster r = new MetaTagAdjuster();
        r.setPipelineConfiguration(pipe);
        r.setOutputProperties(outputProperties);
        r.setIsXHTML(false);
        return r;
    }

    /**
     * Create a new HTML URI Escaper, responsible for percent-encoding of URIs in
     * HTML output documents. This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created HTML URI escaper.
     */

    protected ProxyReceiver newHTMLURIEscaper(PipelineConfiguration pipe, Properties outputProperties) {
        HTMLURIEscaper r = new HTMLURIEscaper();
        r.setPipelineConfiguration(pipe);
        return r;
    }

    /**
     * Create a new CDATA Filter, responsible for insertion of CDATA sections where required.
     * This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created CDATA filter.
     */

    protected ProxyReceiver newCDATAFilter(PipelineConfiguration pipe, Properties outputProperties) throws XPathException {
        CDATAFilter r = new CDATAFilter();
        r.setPipelineConfiguration(pipe);
        r.setOutputProperties(outputProperties);
        return r;
    }

    /**
     * Create a new XML 1.0 content checker, responsible for checking that the output conforms to
     * XML 1.0 rules (this is used only if the Configuration supports XML 1.1 but the specific output
     * file requires XML 1.0). This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created XML 1.0 content checker.
     */

    protected ProxyReceiver newXML10ContentChecker(PipelineConfiguration pipe, Properties outputProperties) {
        XML10ContentChecker r = new XML10ContentChecker();
        r.setPipelineConfiguration(pipe);
        return r;
    }

    /**
     * Create a Unicode Normalizer. This method exists so that it can be overridden in a subclass.
     * @param pipe the pipeline configuration
     * @param outputProperties the serialization parameters
     * @return the newly created Unicode normalizer.
     */

    protected ProxyReceiver newUnicodeNormalizer(PipelineConfiguration pipe, Properties outputProperties) throws XPathException {
        String normForm = outputProperties.getProperty(SaxonOutputKeys.NORMALIZATION_FORM);
        UnicodeNormalizer r = new UnicodeNormalizer(normForm);
        r.setPipelineConfiguration(pipe);
        return r;
    }

    /**
     * Create a new CharacterMapExpander. This method exists so that it can be overridden in a subclass.
     * @return the newly created CharacterMapExpander.
     */

    public CharacterMapExpander newCharacterMapExpander() {
        return new CharacterMapExpander();
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
// Contributor(s): none.
//