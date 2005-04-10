package net.sf.saxon;

import net.sf.saxon.event.PIGrabber;
import net.sf.saxon.event.Sender;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.List;


/**
 * A TransformerFactoryImpl instance can be used to create Transformer and Template
 * objects.
 *
 * <p>The system property that determines which Factory implementation
 * to create is named "javax.xml.transform.TransformerFactory". This
 * property names a concrete subclass of the TransformerFactory abstract
 * class. If the property is not defined, a platform default is be used.</p>
 *
 * <p>This implementation class implements the abstract methods on both the
 * javax.xml.transform.TransformerFactory and javax.xml.transform.sax.SAXTransformerFactory
 * classes.
 */

public class TransformerFactoryImpl extends SAXTransformerFactory {

    private Configuration config;

    /**
     * Default constructor.
     */
    public TransformerFactoryImpl() {
        config = new Configuration();
    }

    /**
     * Set the configuration (en bloc)
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the configuration (en bloc)
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Process the Source into a Transformer object.  Care must
     * be given not to use this object in multiple threads running concurrently.
     * Different TransformerFactories can be used concurrently by different
     * threads.
     *
     * @param source An object that holds a URI, input stream, etc.
     *
     * @return A Transformer object that may be used to perform a transformation
     * in a single thread, never null.
     *
     * @exception TransformerConfigurationException May throw this during the parse
     *            when it is constructing the Templates object and fails.
     */

    public Transformer newTransformer(Source source)
        	throws TransformerConfigurationException {
        Templates templates = newTemplates(source);
        Transformer trans = templates.newTransformer();
        return trans;
    }

    /**
     * Create a new Transformer object that performs a copy
     * of the source to the result.
     *
     * @return A Transformer object that may be used to perform a transformation
     * in a single thread, never null.
     *
     * @exception TransformerConfigurationException May throw this during
     *            the parse when it is constructing the
     *            Templates object and fails.
     */

    public Transformer newTransformer()
        			throws TransformerConfigurationException {

        return new IdentityTransformer(config);
	}


    /**
     * Process the Source into a Templates object, which is a
     * a compiled representation of the source. This Templates object
     * may then be used concurrently across multiple threads.  Creating
     * a Templates object allows the TransformerFactory to do detailed
     * performance optimization of transformation instructions, without
     * penalizing runtime transformation.
     *
     * @param source An object that holds a URL, input stream, etc.
     *
     * @return A Templates object capable of being used for transformation purposes,
     * never null.
     *
     * @exception TransformerConfigurationException May throw this during the parse when it
     *            is constructing the Templates object and fails.
     */

    public Templates newTemplates(Source source)
        throws TransformerConfigurationException {

        PreparedStylesheet pss = new PreparedStylesheet(config);
        pss.prepare(source);
        return pss;
	}

    /**
     * Get the stylesheet specification(s) associated
     * via the xml-stylesheet processing instruction (see
     * http://www.w3.org/TR/xml-stylesheet/) with the document
     * document specified in the source parameter, and that match
     * the given criteria.  Note that it is possible to return several
     * stylesheets, in which case they are applied as if they were
     * a list of imports or cascades.
     *
     * @param source The XML source document.
     * @param media The media attribute to be matched.  May be null, in which
     *              case the prefered templates will be used (i.e. alternate = no).
     * @param title The value of the title attribute to match.  May be null.
     * @param charset The value of the charset attribute to match.  May be null.
     *
     * @return A Source object suitable for passing to the TransformerFactory.
     *
     * @throws TransformerConfigurationException if any problems occur
     */

    public Source getAssociatedStylesheet(
        Source source, String media, String title, String charset)
            throws TransformerConfigurationException {


        PIGrabber grabber = new PIGrabber();
        grabber.setFactory(config);
        grabber.setCriteria(media, title, charset);
        grabber.setBaseURI(source.getSystemId());
        grabber.setURIResolver(config.getURIResolver());


        try {
            new Sender(config.makePipelineConfiguration()).send(source, grabber);
            // this parse will be aborted when the first start tag is found
        } catch (XPathException err) {
            if (grabber.isTerminated()) {
            	// do nothing
            } else {
                throw new TransformerConfigurationException(
                        "Failed while looking for xml-stylesheet PI", err);
            }
        }

        try {
            Source[] sources = grabber.getAssociatedStylesheets();
            if (sources==null) {
                throw new TransformerConfigurationException(
                    "No matching <?xml-stylesheet?> processing instruction found");
            }
            return compositeStylesheet(source.getSystemId(), sources);
        } catch (TransformerException err) {
            if (err instanceof TransformerConfigurationException) {
                throw (TransformerConfigurationException)err;
            } else {
                throw new TransformerConfigurationException(err);
            }
        }
    }

    /**
    * Process a series of stylesheet inputs, treating them in import or cascade
    * order.  This is mainly for support of the getAssociatedStylesheets
    * method, but may be useful for other purposes.
    *
    * @param sources An array of Source objects representing individual stylesheets.
    * @return A Source object representing a composite stylesheet.
    */

    private Source compositeStylesheet(String baseURI, Source[] sources)
    					throws TransformerConfigurationException {

        if (sources.length == 1) {
            return sources[0];
        } else if (sources.length == 0) {
            throw new TransformerConfigurationException(
                            "No stylesheets were supplied");
        }

        // create a new top-level stylesheet that imports all the others

        StringBuffer sb = new StringBuffer(250);
        sb.append("<xsl:stylesheet version='1.0' ");
        sb.append(" xmlns:xsl='" + NamespaceConstant.XSLT + "'>");
        for (int i=0; i<sources.length; i++) {
            sb.append("<xsl:import href='" + sources[i].getSystemId() + "'/>");
        }
        sb.append("</xsl:stylesheet>");
        InputSource composite = new InputSource();
        composite.setSystemId(baseURI);
        composite.setCharacterStream(new StringReader(sb.toString()));
        return new SAXSource(config.getSourceParser(), composite);
    }

    /**
     * Set an object that is used by default during the transformation
     * to resolve URIs used in xsl:import, or xsl:include.
     *
     * @param resolver An object that implements the URIResolver interface,
     * or null.
     */

    public void setURIResolver(URIResolver resolver) {
    	config.setURIResolver(resolver);
    }

    /**
     * Get the object that is used by default during the transformation
     * to resolve URIs used in document(), xsl:import, or xsl:include.
     *
     * @return The URIResolver that was set with setURIResolver.
     */

    public URIResolver getURIResolver() {
    	return config.getURIResolver();
    }

    //======= CONFIGURATION METHODS =======

    private static final String FEATURE_SECURE_PROCESSING =
            //javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
            "http://javax.xml.XMLConstants/feature/secure-processing";
                    // Avoid reference to this JDK 1.5 constant


    /**
     * Look up the value of a feature.
     *
     * <p>The feature name is any absolute URI.</p>
     * @param name The feature name, which is an absolute URI.
     * @return The current state of the feature (true or false).
     */

    public boolean getFeature(String name) {
    	if (name.equals(SAXSource.FEATURE)) return true;
    	if (name.equals(SAXResult.FEATURE)) return true;
    	if (name.equals(DOMSource.FEATURE)) return isDOMAvailable();
    	if (name.equals(DOMResult.FEATURE)) return isDOMAvailable();
    	if (name.equals(StreamSource.FEATURE)) return true;
    	if (name.equals(StreamResult.FEATURE)) return true;
        if (name.equals(SAXTransformerFactory.FEATURE)) return true;
        if (name.equals(SAXTransformerFactory.FEATURE_XMLFILTER)) return true;
        if (name.equals(FEATURE_SECURE_PROCESSING)) {
            return !config.isAllowExternalFunctions();
        }
    	throw new IllegalArgumentException("Unknown feature " + name);
    }

    /**
     * Test whether DOM processing is available
     */

    private boolean isDOMAvailable() {
        List models = config.getExternalObjectModels();
        for (int i=0; i<models.size(); i++) {
            if (models.get(i).getClass().getName().equals("net.sf.saxon.dom.DOMObjectModel")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Allows the user to set specific attributes on the underlying
     * implementation.  An attribute in this context is defined to
     * be an option that the implementation provides.
     *
     * @param name The name of the attribute. This must be one of the constants
     * defined in class net.sf.saxon.FeatureKeys.
     * @param value The value of the attribute.
     * @throws IllegalArgumentException thrown if Saxon
     * doesn't recognize the attribute.
     * @see net.sf.saxon.FeatureKeys
     */

    public void setAttribute(String name, Object value)
        							throws IllegalArgumentException {
        if (name.equals(FeatureKeys.TREE_MODEL)) {
        	if (!(value instanceof Integer)) {
        		throw new IllegalArgumentException("Tree model must be an Integer");
        	}
        	config.setTreeModel(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("allow-external-functions must be a boolean");
        	}
        	config.setAllowExternalFunctions(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("trace-external-functions must be a boolean");
        	}
        	config.setTraceExternalFunctions(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.TIMING)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("Timing must be a boolean");
        	}
        	config.setTiming(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("Validation must be a boolean");
        	}
        	config.setValidation(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION)) {
        	if (!(value instanceof Integer)) {
        		throw new IllegalArgumentException("Schema validation must be an integer");
        	}
        	config.setSchemaValidationMode(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.VALIDATION_WARNINGS)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("validation-warnings must be a boolean");
        	}
        	config.setValidationWarnings(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.VERSION_WARNING)) {
             if (!(value instanceof Boolean)) {
                 throw new IllegalArgumentException("version-warning must be a boolean");
             }
             config.setVersionWarning(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
        	if (!(value instanceof TraceListener)) {
        		throw new IllegalArgumentException("Trace listener is of wrong class");
        	}
        	config.setTraceListener((TraceListener)value);

        } else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
        	if (!(value instanceof Boolean)) {
        		throw new IllegalArgumentException("Line Numbering value must be Boolean");
        	}
        	config.setLineNumbering(((Boolean)value).booleanValue());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
        	if (!(value instanceof Integer)) {
        		throw new IllegalArgumentException("Recovery Policy value must be Integer");
        	}
        	config.setRecoveryPolicy(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
        	if (!(value instanceof String)) {
        		throw new IllegalArgumentException("Message Emitter class must be a String");
        	}
        	config.setMessageEmitterClass((String)value);

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
        	if (!(value instanceof String)) {
        		throw new IllegalArgumentException("Source Parser class must be a String");
        	}
        	config.setSourceParserClass((String)value);

        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
        	if (!(value instanceof String)) {
        		throw new IllegalArgumentException("Style Parser class must be a String");
        	}
        	config.setStyleParserClass((String)value);

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
        	if (!(value instanceof OutputURIResolver)) {
        		throw new IllegalArgumentException("Output URI resolver value must be an instance of net.sf.saxon.OutputURIResolver");
        	}
        	config.setOutputURIResolver((OutputURIResolver)value);

        } else if (name.equals(FeatureKeys.NAME_POOL)) {
        	if (!(value instanceof NamePool)) {
        		throw new IllegalArgumentException("NAME_POOL value must be an instance of net.sf.saxon.om.NamePool");
        	}
        	config.setNamePool((NamePool)value);

        } else {
	        throw new IllegalArgumentException("Unknown attribute " + name);
	    }
    }

    /**
     * Allows the user to retrieve specific attributes on the underlying
     * implementation.
     * @param name The name of the attribute.
     * @return value The value of the attribute.
     * @throws IllegalArgumentException thrown if the underlying
     * implementation doesn't recognize the attribute.
     */
    public Object getAttribute(String name)
        throws IllegalArgumentException{
        if (name.equals(FeatureKeys.TREE_MODEL)) {
        	return new Integer(config.getTreeModel());

        } else if (name.equals(FeatureKeys.TIMING)) {
        	return Boolean.valueOf(config.isTiming());

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
        	return Boolean.valueOf(config.isValidation());

        } else if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
        	return Boolean.valueOf(config.isAllowExternalFunctions());

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
        	return Boolean.valueOf(config.isTraceExternalFunctions());

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
        	return config.getTraceListener();

    	} else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
    		return Boolean.valueOf(config.isLineNumbering());

    	} else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
    		return new Integer(config.getRecoveryPolicy());

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
        	return config.getMessageEmitterClass();

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
        	return config.getSourceParserClass();

        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
        	return config.getStyleParserClass();

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
        	return config.getOutputURIResolver();

        } else {
	        throw new IllegalArgumentException("Unknown attribute " + name);
	    }
    }

    /**
     * Set the error event listener for the TransformerFactory, which
     * is used for the processing of transformation instructions,
     * and not for the transformation itself.
     *
     * @param listener The new error listener.
     * @throws IllegalArgumentException if listener is null.
     */

    public void setErrorListener(ErrorListener listener)
        	throws IllegalArgumentException {
        config.setErrorListener(listener);
    }

    /**
     * Get the error event handler for the TransformerFactory.
     * @return The current error listener, which should never be null.
     */
    public ErrorListener getErrorListener() {
    	return config.getErrorListener();
    }




    ///////////////////////////////////////////////////////////////////////////////
    // Methods defined in class javax.xml.transform.sax.SAXTransformerFactory
    ///////////////////////////////////////////////////////////////////////////////

     /**
     * Get a TransformerHandler object that can process SAX
     * ContentHandler events into a Result, based on the transformation
     * instructions specified by the argument.
     *
     * @param src The Source of the transformation instructions.
     *
     * @return TransformerHandler ready to transform SAX events.
     *
     * @throws TransformerConfigurationException If for some reason the
     * TransformerHandler can not be created.
     */

    public TransformerHandler newTransformerHandler(Source src)
    throws TransformerConfigurationException {
        Templates tmpl = newTemplates(src);
        return newTransformerHandler(tmpl);
    }

    /**
     * Get a TransformerHandler object that can process SAX
     * ContentHandler events into a Result, based on the Templates argument.
     *
     * @param templates The compiled transformation instructions.
     *
     * @return TransformerHandler ready to transform SAX events.
     *
     * @throws TransformerConfigurationException If for some reason the
     * TransformerHandler can not be created.
     */

    public TransformerHandler newTransformerHandler(Templates templates)
    throws TransformerConfigurationException {
        if (!(templates instanceof PreparedStylesheet)) {
            throw new TransformerConfigurationException("Templates object was not created by Saxon");
        }
        Controller controller = (Controller)templates.newTransformer();
        TransformerHandlerImpl handler = new TransformerHandlerImpl(controller);
        return handler;
    }

    /**
     * Get a TransformerHandler object that can process SAX
     * ContentHandler events into a Result. The transformation
     * is defined as an identity (or copy) transformation, for example
     * to copy a series of SAX parse events into a DOM tree.
     *
     * @return A non-null reference to a TransformerHandler, that may
     * be used as a ContentHandler for SAX parse events.
     *
     * @throws TransformerConfigurationException If for some reason the
     * TransformerHandler cannot be created.
     */

    public TransformerHandler newTransformerHandler()
    throws TransformerConfigurationException {
        Controller controller = new IdentityTransformer(config);
        return new IdentityTransformerHandler(controller);
    }

    /**
     * Get a TemplatesHandler object that can process SAX
     * ContentHandler events into a Templates object.
     *
     * @return A non-null reference to a TransformerHandler, that may
     * be used as a ContentHandler for SAX parse events.
     *
     * @throws TransformerConfigurationException If for some reason the
     * TemplatesHandler cannot be created.
     */

    public TemplatesHandler newTemplatesHandler()
    throws TransformerConfigurationException {
        return new TemplatesHandlerImpl(config);
    }

    /**
     * Create an XMLFilter that uses the given Source as the
     * transformation instructions.
     *
     * @param src The Source of the transformation instructions.
     *
     * @return An XMLFilter object, or null if this feature is not supported.
     *
     * @throws TransformerConfigurationException If for some reason the
     * XMLFilter cannot be created.
     */

    public XMLFilter newXMLFilter(Source src)
    throws TransformerConfigurationException {
        Templates tmpl = newTemplates(src);
        return newXMLFilter(tmpl);
    }

    /**
     * Create an XMLFilter, based on the Templates argument..
     *
     * @param templates The compiled transformation instructions.
     *
     * @return An XMLFilter object, or null if this feature is not supported.
     *
     * @throws TransformerConfigurationException If for some reason the
     * XMLFilter cannot be created.
     */

    public XMLFilter newXMLFilter(Templates templates)
    throws TransformerConfigurationException {
        if (!(templates instanceof PreparedStylesheet)) {
            throw new TransformerConfigurationException("Supplied Templates object was not created using Saxon");
        }
        Controller controller = (Controller)templates.newTransformer();
        return new Filter(controller);
    }

    /**
     * <p>Set a feature for this <code>TransformerFactory</code> and <code>Transformer</code>s
     * or <code>Template</code>s created by this factory.</p>
     * <p/>
     * <p/>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * An {@link javax.xml.transform.TransformerConfigurationException} is thrown if this <code>TransformerFactory</code> or the
     * <code>Transformer</code>s or <code>Template</code>s it creates cannot support the feature.
     * It is possible for an <code>TransformerFactory</code> to expose a feature value but be unable to change its state.
     * </p>
     * <p/>
     * <p>All implementations are required to support the FEATURE_SECURE_PROCESSING feature.
     * When the feature is:</p>
     * <ul>
     * <li>
     * <code>true</code>: the implementation will limit XML processing to conform to implementation limits
     * and behave in a secure fashion as defined by the implementation.
     * Examples include resolving user defined style sheets and functions.
     * If XML processing is limited for security reasons, it will be reported via a call to the registered
     * {@link javax.xml.transform.ErrorListener#fatalError(javax.xml.transform.TransformerException exception)}.
     * See {@link  #setErrorListener(javax.xml.transform.ErrorListener listener)}. In the Saxon implementation,
     * this option causes calls on extension functions and extensions instructions to be disabled, and also
     * disables the use of xsl:result-document to write to secondary output destinations.
     * </li>
     * <li>
     * <code>false</code>: the implementation will processing XML according to the XML specifications without
     * regard to possible implementation limits.
     * </li>
     * </ul>
     *
     * @param name  Feature name.
     * @param value Is feature state <code>true</code> or <code>false</code>.
     * @throws javax.xml.transform.TransformerConfigurationException
     *                              if this <code>TransformerFactory</code>
     *                              or the <code>Transformer</code>s or <code>Template</code>s it creates cannot support this feature.
     * @throws NullPointerException If the <code>name</code> parameter is null.
     */

    public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        if (name.equals(FEATURE_SECURE_PROCESSING)) {
            config.setAllowExternalFunctions(!value);
        } else {
            throw new TransformerConfigurationException("Unsupported TransformerFactory feature: " + name);
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
