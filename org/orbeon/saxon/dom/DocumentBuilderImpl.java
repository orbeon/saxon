package org.orbeon.saxon.dom;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.tinytree.TinyDocumentImpl;
import org.orbeon.saxon.trans.XPathException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;

/**
 * This class implements the JAXP DocumentBuilder interface, allowing a Saxon TinyTree to be
 * constructed using standard JAXP parsing interfaces. The returned DOM document node is a wrapper
 * over the Saxon TinyTree structure. Note that although this wrapper
 * implements the DOM interfaces, it is read-only, and all attempts to update it will throw
 * an exception. No schema or DTD validation is carried out on the document.
 */

public class DocumentBuilderImpl extends DocumentBuilder {

    private Configuration config;
    private EntityResolver entityResolver;
    private ErrorHandler errorHandler;
    private boolean xIncludeAware;
    private boolean validating;
    private int stripSpace = Whitespace.UNSPECIFIED;

    /**
     * Set the Saxon Configuration to be used by the document builder.
     * This non-JAXP method must be called if the resulting document is to be used
     * within a Saxon query or transformation. If no Configuration is supplied,
     * Saxon creates a Configuration on the first call to the {@link #parse} method,
     * and subsequent calls reuse the same Configuration.
     *
     * <p>As an alternative to calling this method, a Configuration can be supplied by calling
     * <code>setAttribute(FeatureKeys.CONFIGURATION, config)</code> on the <code>DocumentBuilderFactory</code>
     * object, where <code>config</code> can be obtained by calling
     * <code>getAttribute(FeatureKeys.CONFIGURATION)</code> on the <code>TransformerFactory</code>.</p>
     * 
     * @since Saxon 8.8
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the Saxon Configuration to be used by the document builder. This is
     * a non-JAXP method.
     * @return the Configuration previously supplied to {@link #setConfiguration},
     * or the Configuration created automatically by Saxon on the first call to the
     * {@link #parse} method, or null if no Configuration has been supplied and
     * the {@link #parse} method has not been called.
     * 
     * @since Saxon 8.8
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Indicates whether or not this document builder is configured to
     * understand namespaces.
     *
     * @return true if this document builder is configured to understand
     *         namespaces. This implementation always returns true.
     */

    public boolean isNamespaceAware() {
        return true;
    }

    /**
     * Determine whether the document builder should perform DTD validation
     * @param state set to true to request DTD validation
     */

    public void setValidating(boolean state) {
        validating = state;
    }

    /**
     * Indicates whether or not this document builder is configured to
     * validate XML documents against a DTD.
     *
     * @return true if this parser is configured to validate
     *         XML documents against a DTD; false otherwise.
     */

    public boolean isValidating() {
        return validating;
    }

    /**
     * Create a new Document Node.
     * @throws UnsupportedOperationException (always). The only way to build a document using this DocumentBuilder
     * implementation is by using the parse() method.
     */

    public Document newDocument() {
        throw new UnsupportedOperationException("The only way to build a document using this DocumentBuilder is with the parse() method");
    }

    /**
     * Parse the content of the given input source as an XML document
     * and return a new DOM {@link Document} object.
     *
     * <p>Note: for this document to be usable as part of a Saxon query or transformation,
     * the document should be built within the {@link Configuration} in which that query
     * or transformation is running. This can be achieved using the non-JAXP
     * {@link #setConfiguration} method.
     *
     * @param in InputSource containing the content to be parsed. Note that if
     * an EntityResolver or ErrorHandler has been supplied, then the XMLReader contained
     * in this InputSource will be modified to register this EntityResolver or ErrorHandler,
     * replacing any that was previously registered.
     *
     * @exception SAXException If any parse errors occur.
     * @return A new DOM Document object.
     */

    public Document parse(InputSource in) throws SAXException {
        try {
            Builder builder = new TinyBuilder();
            if (config == null) {
                config = new Configuration();
            }
            PipelineConfiguration pipe = config.makePipelineConfiguration();
            builder.setPipelineConfiguration(pipe);
            SAXSource source = new SAXSource(in);
            if (entityResolver != null) {
                XMLReader reader = source.getXMLReader();
                if (reader == null) {
                    reader = config.getSourceParser();
                }
                reader.setEntityResolver(entityResolver);
            }
            if (errorHandler != null) {
                XMLReader reader = source.getXMLReader();
                if (reader == null) {
                    reader = config.getSourceParser();
                }
                reader.setErrorHandler(errorHandler);
            }
            source.setSystemId(in.getSystemId());
            Source ss = source;
            if (xIncludeAware) {
                ss = AugmentedSource.makeAugmentedSource(ss);
                ((AugmentedSource)ss).setXIncludeAware(true);
            }
            if (validating) {
                ss = AugmentedSource.makeAugmentedSource(ss);
                ((AugmentedSource)ss).setDTDValidationMode(Validation.STRICT);
            }
            if (stripSpace != Whitespace.UNSPECIFIED) {
                ss = AugmentedSource.makeAugmentedSource(ss);
                ((AugmentedSource)ss).setStripSpace(stripSpace);
            }
            new Sender(pipe).send(source, builder);
            TinyDocumentImpl doc = (TinyDocumentImpl)builder.getCurrentRoot();
            builder.reset();
            return (Document)DocumentOverNodeInfo.wrap(doc);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    /**
     * Parse the content of the given file as an XML document
     * and return a new DOM {@link Document} object.
     * An <code>IllegalArgumentException</code> is thrown if the
     * <code>File</code> is <code>null</code> null.
     *
     * <p><i>This implementation differs from the parent implementation
     * by using a correct algorithm for filename-to-uri conversion.<i></p>
     *
     * @param f The file containing the XML to parse.
     * @exception java.io.IOException If any IO errors occur.
     * @exception SAXException If any parse errors occur.
     * @return A new DOM Document object.
     */

    public Document parse(File f) throws SAXException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        String uri = f.toURI().toString();
        InputSource in = new InputSource(uri);
        return parse(in);
    }


    /**
     * Specify the {@link EntityResolver} to be used to resolve
     * entities present in the XML document to be parsed. Setting
     * this to <code>null</code> will result in the underlying
     * implementation using the EntityResolver registered with the
     * XMLReader contained in the InputSource.
     *
     * @param er The <code>EntityResolver</code> to be used to resolve entities
     *           present in the XML document to be parsed.
     */

    public void setEntityResolver(EntityResolver er) {
        entityResolver = er;
    }

    /**
     * Specify the {@link ErrorHandler} to be used by the parser.
     * Setting this to <code>null</code> will result in the underlying
     * implementation using using the ErrorHandler registered with the
     * XMLReader contained in the InputSource.
     *
     * @param eh The <code>ErrorHandler</code> to be used by the parser.
     */


    public void setErrorHandler(ErrorHandler eh) {
        errorHandler = eh;
    }

    /**
     * Obtain an instance of a {@link DOMImplementation} object.
     *
     * @return A new instance of a <code>DOMImplementation</code>.
     */

    public DOMImplementation getDOMImplementation() {
        return newDocument().getImplementation();
    }

    /**
     * <p>Set state of XInclude processing.</p>
     * <p/>
     * <p>If XInclude markup is found in the document instance, should it be
     * processed as specified in <a href="http://www.w3.org/TR/xinclude/">
     * XML Inclusions (XInclude) Version 1.0</a>.</p>
     * <p/>
     * <p>XInclude processing defaults to <code>false</code>.</p>
     *
     * @param state Set XInclude processing to <code>true</code> or
     *              <code>false</code>
     */
    public void setXIncludeAware(boolean state) {
        xIncludeAware = state;
    }


    /**
     * <p>Get the XInclude processing mode for this parser.</p>
     *
     * @return the return value of
     *         the {@link javax.xml.parsers.DocumentBuilderFactory#isXIncludeAware()}
     *         when this parser was created from factory.
     * @throws UnsupportedOperationException For backward compatibility, when implementations for
     *                                       earlier versions of JAXP is used, this exception will be
     *                                       thrown.
     * @see javax.xml.parsers.DocumentBuilderFactory#setXIncludeAware(boolean)
     * @since JAXP 1.5, Saxon 8.9
     */
    public boolean isXIncludeAware() {
        return xIncludeAware;
    }

    /**
     * Set the space-stripping action to be applied to the source document
     * @param stripAction one of {@link org.orbeon.saxon.value.Whitespace#IGNORABLE},
     * {@link org.orbeon.saxon.value.Whitespace#ALL}, or {@link org.orbeon.saxon.value.Whitespace#NONE}
     * @since 8.9
     */

    public void setStripSpace(int stripAction) {
        stripSpace = stripAction;
    }

    /**
     * Get the space-stripping action to be applied to the source document
     * @return one of {@link org.orbeon.saxon.value.Whitespace#IGNORABLE},
     * {@link org.orbeon.saxon.value.Whitespace#ALL}, or {@link org.orbeon.saxon.value.Whitespace#NONE}
     * @since 8.9
     */

    public int getStripSpace() {
        return stripSpace;
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
// Contributor(s): none
//