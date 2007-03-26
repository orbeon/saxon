package org.orbeon.saxon.dom;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.tinytree.TinyDocumentImpl;
import org.orbeon.saxon.trans.XPathException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.sax.SAXSource;

/**
 * This class implements the JAXP DocumentBuilder interface, allowing a Saxon TinyTree to be
 * constructed using standard JAXP parsing interfaces. Note that although the TinyTree
 * implements the DOM interfaces, it is read-only, and all attempts to update it will throw
 * an exception. No schema or DTD validation is carried out on the document.
 */

public class DocumentBuilderImpl extends DocumentBuilder {

    private Configuration configuration;
    private EntityResolver entityResolver;
    private ErrorHandler errorHandler;

    /**
     * Set the Saxon Configuration to be used by the document builder.
     * This non-JAXP method must be called if the resulting document is to be used
     * within a Saxon query or transformation. If no Configuration is supplied,
     * Saxon creates a Configuration on the first call to the {@link #parse} method,
     * and subsequent calls reuse the same Configuration.
     * @since Saxon 8.8
     */

    public void setConfiguration(Configuration config) {
        this.configuration = config;
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
        return configuration;
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
     * Indicates whether or not this document builder is configured to
     * validate XML documents against a DTD.
     *
     * @return true if this parser is configured to validate
     *         XML documents against a DTD; false otherwise. This
     *         implementation always returns false.
     */

    public boolean isValidating() {
        return false;
    }

    /**
     * Create a new Document Node.
     * @return a new Document Node. The returned document will be of little use, because it is immutable.
     * But it can be used in a DOMResult as the result of a transformation
     */

    public Document newDocument() {
        return new DocumentOverNodeInfo();
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
            if (configuration == null) {
                configuration = new Configuration();
            }
            PipelineConfiguration pipe = configuration.makePipelineConfiguration();
            builder.setPipelineConfiguration(pipe);
            SAXSource source = new SAXSource(in);
            if (entityResolver != null) {
                source.getXMLReader().setEntityResolver(entityResolver);
            }
            if (errorHandler != null) {
                source.getXMLReader().setErrorHandler(errorHandler);
            }
            source.setSystemId(in.getSystemId());
            new Sender(pipe).send(source, builder);
            TinyDocumentImpl doc = (TinyDocumentImpl)builder.getCurrentRoot();
            return (Document)DocumentOverNodeInfo.wrap(doc);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
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