package org.orbeon.saxon.om;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.tinytree.*;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Sender;

import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.transform.sax.SAXSource;
import org.orbeon.saxon.xpath.XPathException;

/**
 * This class implements the JAXP DocumentBuilder interface, allowing a Saxon TinyTree to be
 * constructed using standard JAXP parsing interfaces. Note that although the TinyTree
 * implements the DOM interfaces, it is read-only, and all attempts to update it will throw
 * an exception. No schema or DTD validation is carried out on the document.
 */

public class DocumentBuilderImpl extends DocumentBuilder {

    private EntityResolver entityResolver;
    private ErrorHandler errorHandler;

    public boolean isNamespaceAware() {
        return true;
    }

    public boolean isValidating() {
        return false;
    }

    public Document newDocument() {
        // The returned document will be of little use, because it is immutable.
        // But it can be used in a DOMResult as the result of a transformation
        return new TinyDocumentImpl();
    }

    public Document parse(InputSource in) throws SAXException {
        try {
            Builder builder = new TinyBuilder();
            Configuration config = new Configuration();
            NamePool pool = config.getNamePool();
            builder.setConfiguration(config);
            SAXSource source = new SAXSource(in);
            if (entityResolver != null) {
                source.getXMLReader().setEntityResolver(entityResolver);
            }
            if (errorHandler != null) {
                source.getXMLReader().setErrorHandler(errorHandler);
            }
            source.setSystemId(in.getSystemId());
            new Sender(config).send(source, builder);
            TinyDocumentImpl doc = (TinyDocumentImpl)builder.getCurrentDocument();
            pool.allocateDocumentNumber(doc);
            return doc;
        } catch (XPathException err) {
            throw new SAXException(err);
        }
    }

    public void setEntityResolver(EntityResolver er) {
        entityResolver = er;
    }

    public void setErrorHandler(ErrorHandler eh) {
        errorHandler = eh;
    }

    public DOMImplementation getDOMImplementation() {
        return new TinyDocumentImpl().getImplementation();
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