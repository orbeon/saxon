package net.sf.saxon;
import net.sf.saxon.event.ContentHandlerProxy;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;



/**
  * <B>Filter</B> is an XMLFilter (a SAX2 filter) that performs a transformation
  * taking a SAX stream as input and producing a SAX stream as output.
  * @author Michael H. Kay
  */

public class Filter implements XMLFilter {

    private Controller controller;
    private XMLReader parser;
    private ContentHandler contentHandler;      // destination for output of this filter
    private LexicalHandler lexicalHandler;      // destination for output of this filter



    /**
    * Create a Filter and initialise variables. The constructor is protected, because
    * the Filter should be created using newXMLFilter() in the SAXTransformerFactory
    * class
    */

    protected Filter(Controller controller) {
        this.controller = controller;
    }


    //////////////////////////////////////////////////////////////////
    // Implement XMLFilter interface methods
    //////////////////////////////////////////////////////////////////

    /**
    * Set the parent reader.
    *
    * <p>This method allows the application to link the filter to
    * a parent reader (which may be another filter).  The argument
    * may not be null.</p>
    *
    * @param parent The parent reader (the supplier of SAX events).
    */

    public void setParent (XMLReader parent) {
        parser = parent;
    }

    /**
    * Get the parent reader.
    *
    * <p>This method allows the application to query the parent
    * reader (which may be another filter).  It is generally a
    * bad idea to perform any operations on the parent reader
    * directly: they should all pass through this filter.</p>
    *
    * @return The parent filter, or null if none has been set.
    */

    public XMLReader getParent() {
        return parser;
    }

    ///////////////////////////////////////////////////////////////////
    // implement XMLReader interface methods
    ///////////////////////////////////////////////////////////////////

    /**
     * Look up the value of a feature.
     *
     * <p>The feature name is any fully-qualified URI.  It is
     * possible for an XMLReader to recognize a feature name but
     * to be unable to return its value; this is especially true
     * in the case of an adapter for a SAX1 Parser, which has
     * no way of knowing whether the underlying parser is
     * performing validation or expanding external entities.</p>
     *
     * <p>All XMLReaders are required to recognize the
     * http://xml.org/sax/features/namespaces and the
     * http://xml.org/sax/features/namespace-prefixes feature names.</p>
     *
     * @param name The feature name, which is a fully-qualified URI.
     * @return The current state of the feature (true or false).
     * @exception org.xml.sax.SAXNotRecognizedException When the
     *            XMLReader does not recognize the feature name.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the feature name but
     *            cannot determine its value at this time.
     * @see #setFeature
     */

    public boolean getFeature (String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals("http://xml.org/sax/features/namespaces")) {
            return true;
        } else if (name.equals("http://xml.org/sax/features/namespace-prefixes")) {
            return false;
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }


    /**
     * Set the state of a feature.
     *
     * <p>The feature name is any fully-qualified URI.  It is
     * possible for an XMLReader to recognize a feature name but
     * to be unable to set its value</p>
     *
     * <p>All XMLReaders are required to support setting
     * http://xml.org/sax/features/namespaces to true and
     * http://xml.org/sax/features/namespace-prefixes to false.</p>
     *
     * <p>Some feature values may be immutable or mutable only
     * in specific contexts, such as before, during, or after
     * a parse.</p>
     *
     * @param name The feature name, which is a fully-qualified URI.
     * @param value The requested state of the feature (true or false).
     * @exception org.xml.sax.SAXNotRecognizedException When the
     *            XMLReader does not recognize the feature name.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the feature name but
     *            cannot set the requested value.
     * @see #getFeature
     */

    public void setFeature (String name, boolean value)
	throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals("http://xml.org/sax/features/namespaces")) {
            if (!value) {
                throw new SAXNotSupportedException(name);
            }
        } else if (name.equals("http://xml.org/sax/features/namespace-prefixes")) {
            if (value) {
                throw new SAXNotSupportedException(name);
            }
        } else {
            throw new SAXNotRecognizedException(name);
        }
	}

    /**
     * Look up the value of a property.
     *
     * <p>The property name is any fully-qualified URI.  It is
     * possible for an XMLReader to recognize a property name but
     * to be unable to return its state.</p>
     *
     * <p>XMLReaders are not required to recognize any specific
     * property names, though an initial core set is documented for
     * SAX2.</p>
     *
     * <p>Some property values may be available only in specific
     * contexts, such as before, during, or after a parse.</p>
     *
     * <p>Implementors are free (and encouraged) to invent their own properties,
     * using names built on their own URIs.</p>
     *
     * @param name The property name, which is a fully-qualified URI.
     * @return The current value of the property.
     * @exception org.xml.sax.SAXNotRecognizedException When the
     *            XMLReader does not recognize the property name.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the property name but
     *            cannot determine its value at this time.
     * @see #setProperty
     */

    public Object getProperty (String name)
	throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals("http://xml.org/sax/properties/lexical-handler")) {
            return lexicalHandler;
        } else {
	        throw new SAXNotRecognizedException(name);
        }
	}


    /**
     * Set the value of a property.
     *
     * <p>The property name is any fully-qualified URI.  It is
     * possible for an XMLReader to recognize a property name but
     * to be unable to set its value.</p>
     *
     * <p>XMLReaders are not required to recognize setting
     * any specific property names, though a core set is provided with
     * SAX2.</p>
     *
     * <p>Some property values may be immutable or mutable only
     * in specific contexts, such as before, during, or after
     * a parse.</p>
     *
     * <p>This method is also the standard mechanism for setting
     * extended handlers.</p>
     *
     * @param name The property name, which is a fully-qualified URI.
     * @param value The requested value for the property.
     * @exception org.xml.sax.SAXNotRecognizedException When the
     *            XMLReader does not recognize the property name.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the property name but
     *            cannot set the requested value.
     */

    public void setProperty (String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals("http://xml.org/sax/properties/lexical-handler")) {
            if (value instanceof LexicalHandler) {
                lexicalHandler = (LexicalHandler)value;
            } else {
                throw new SAXNotSupportedException(
                    "Lexical Handler must be instance of org.xml.sax.ext.LexicalHandler");
            }
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }

    /**
    * Register a content handler to receive the output of the transformation
    * filter. If the content handler is also a LexicalHandler, and if no LexicalHandler
    * is separately registered, the ContentHandler will also act as the LexicalHandler
    */

    public void setContentHandler(ContentHandler handler) {
        contentHandler = handler;
        if (handler instanceof LexicalHandler && lexicalHandler==null) {
            lexicalHandler = (LexicalHandler)handler;
        }
    }

    /**
    * Get the ContentHandler registered using setContentHandler()
    */

    public ContentHandler getContentHandler() {
        return contentHandler;
    }


    /**
     * Allow an application to register an entity resolver.
     *
     * <p>If the application does not register an entity resolver,
     * the XMLReader will perform its own default resolution.</p>
     *
     * <p>Applications may register a new or different resolver in the
     * middle of a parse, and the SAX parser must begin using the new
     * resolver immediately.</p>
     *
     * @param resolver The entity resolver.
     * @exception java.lang.NullPointerException If the resolver
     *            argument is null.
     * @see #getEntityResolver
     */

    public void setEntityResolver (EntityResolver resolver) {
        // XSLT output does not use entities, so the resolver is never used
    }


    /**
     * Return the current entity resolver.
     *
     * @return Always null, since no entity resolver is used even if one
     * is supplied.
     * @see #setEntityResolver
     */

    public EntityResolver getEntityResolver () {
        return null;
    }


    /**
     * Allow an application to register a DTD event handler.
     *
     * <p>If the application does not register a DTD handler, all DTD
     * events reported by the SAX parser will be silently ignored.</p>
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The DTD handler.
     * @exception java.lang.NullPointerException If the handler
     *            argument is null.
     * @see #getDTDHandler
     */

    public void setDTDHandler (DTDHandler handler) {
        // XSLT output does not include a DTD
    }


    /**
     * Return the current DTD handler.
     *
     * @return Always null, since no DTD handler is used even if one has been
     * supplied.
     * @see #setDTDHandler
     */

    public DTDHandler getDTDHandler () {
        return null;
    }



    /**
     * Allow an application to register an error event handler.
     *
     * <p>If the application does not register an error handler, all
     * error events reported by the SAX parser will be silently
     * ignored; however, normal processing may not continue.  It is
     * highly recommended that all SAX applications implement an
     * error handler to avoid unexpected bugs.</p>
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The error handler.
     * @exception java.lang.NullPointerException If the handler
     *            argument is null.
     * @see #getErrorHandler
     */

    public void setErrorHandler (ErrorHandler handler) {
        // No effect
    }

    /**
     * Return the current error handler.
     *
     * @return The current error handler, or null if none
     *         has been registered.
     * @see #setErrorHandler
     */
    public ErrorHandler getErrorHandler () {
        return null;
    }

    /**
     * Parse an XML document - In the context of a Transformer, this means
     * perform a transformation. The method is equivalent to transform().
     *
     * @param input The input source (the XML document to be transformed)
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @exception java.io.IOException An IO exception from the parser,
     *            possibly from a byte stream or character stream
     *            supplied by the application.
     * @see org.xml.sax.InputSource
     * @see #parse(java.lang.String)
     * @see #setEntityResolver
     * @see #setDTDHandler
     * @see #setContentHandler
     * @see #setErrorHandler
     */

    public void parse (InputSource input) throws IOException, SAXException {
        if (parser==null) {
            try {
                parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            } catch (Exception err) {
                throw new SAXException(err);
            }
        }
        SAXSource source = new SAXSource();
        source.setInputSource(input);
        source.setXMLReader(parser);
        ContentHandlerProxy result = new ContentHandlerProxy();
        result.setPipelineConfiguration(controller.makePipelineConfiguration());
        result.setUnderlyingContentHandler(contentHandler);
        if (lexicalHandler!=null) {
            result.setLexicalHandler(lexicalHandler);
        }
        try {
            controller.transform(source, result);
        } catch (TransformerException err) {
            Throwable cause = err.getException();
            if (cause != null && cause instanceof SAXException) {
                throw (SAXException)cause;
            } else if (cause != null && cause instanceof IOException) {
                throw (IOException)cause;
            } else {
                throw new SAXException(err);
            }
        }


    }

    /**
     * Parse (that is, transform) an XML document given a system identifier (URI).
     *
     * <p>This method is a shortcut for the common case of reading a
     * document from a system identifier.  It is the exact
     * equivalent of the following:</p>
     *
     * <pre>
     * parse(new InputSource(systemId));
     * </pre>
     *
     * <p>If the system identifier is a URL, it must be fully resolved
     * by the application before it is passed to the parser.</p>
     *
     * @param systemId The system identifier (URI).
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @exception java.io.IOException An IO exception from the parser,
     *            possibly from a byte stream or character stream
     *            supplied by the application.
     * @see #parse(org.xml.sax.InputSource)
     */

    public void parse (String systemId)	throws IOException, SAXException {
        InputSource input = new InputSource(systemId);
        parse(input);
    }


    /**
    * Get the underlying Transformer. This is a Saxon-specific method that allows the
    * user to set parameters on the transformation, set a URIResolver or ErrorListener, etc.
    * New in Saxon 7.2
    */

    public Transformer getTransformer() {
        return controller;
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
