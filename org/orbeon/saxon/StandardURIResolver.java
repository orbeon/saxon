package org.orbeon.saxon;
import org.orbeon.saxon.event.IDFilter;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.functions.URIQueryParameters;
import org.orbeon.saxon.om.AllElementStripper;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;


/**
* This class provides the service of converting a URI into an InputSource.
* It is used to get stylesheet modules referenced by xsl:import and xsl:include,
* and source documents referenced by the document() function. The standard version
* handles anything that the java URL class will handle.
* You can write a subclass to handle other kinds of URI, e.g. references to things in
* a database.
* @author Michael H. Kay
*/

public class StandardURIResolver implements NonDelegatingURIResolver, Serializable {

    // TODO: support the data: URI scheme. (Requires unescaping of the URI, then parsing the content as XML)

    private Configuration config = null;
    protected boolean recognizeQueryParameters = false;

    /**
     * Create a StandardURIResolver, with no reference to a Configuration.
     * This constructor is not used internally by Saxon, but it may be used by user-written application code.
     * It is deprecated because the StandardURIResolver works best when the Configuration is known.
     * @deprecated since 8.7
     */

    public StandardURIResolver() {
        this(null);
    }

    /**
     * Create a StandardURIResolver, with a reference to a Configuration
     * @param config The Configuration object.
     * This is used to get a SAX Parser for a source XML document
     */

    public StandardURIResolver(Configuration config) {
        this.config = config;
    }

    /**
     * Indicate that query parameters (such as validation=strict) are to be recognized
     * @param recognize Set to true if query parameters in the URI are to be recognized and acted upon.
     * The default (for compatibility and interoperability reasons) is false.
     */

    public void setRecognizeQueryParameters(boolean recognize) {
        recognizeQueryParameters = recognize;
    }

    /**
     * Determine whether query parameters (such as validation=strict) are to be recognized
     * @return true if query parameters are recognized and interpreted by Saxon.
     */

    public boolean queryParametersAreRecognized() {
        return recognizeQueryParameters;
    }

    /**
     * Get the relevant platform
     */

    protected Platform getPlatform() {
        if (config == null) {
            return JavaPlatform.getInstance();
        } else {
            return config.getPlatform();
        }
    }

    /**
    * Resolve a URI
    * @param href The relative or absolute URI. May be an empty string. May contain
    * a fragment identifier starting with "#", which must be the value of an ID attribute
    * in the referenced XML document.
    * @param base The base URI that should be used. May be null if uri is absolute.
    * @return a Source object representing an XML document
    */

    public Source resolve(String href, String base)
    throws XPathException {

        // System.err.println("StandardURIResolver, href=" + href + ", base=" + base);

        Platform platform = getPlatform();

        String relativeURI = href;
        String id = null;

        // Extract any fragment identifier. Note, this code is no longer used to
        // resolve fragment identifiers in URI references passed to the document()
        // function: the code of the document() function handles these itself.

        int hash = href.indexOf('#');
        if (hash>=0) {
            relativeURI = href.substring(0, hash);
            id = href.substring(hash+1);
            // System.err.println("StandardURIResolver, href=" + href + ", id=" + id);
        }

        URIQueryParameters params = null;
        URI url;
        URI relative;
        try {
            relativeURI = JavaPlatform.escapeSpaces(relativeURI);
            relative = new URI(relativeURI);
        } catch (URISyntaxException err) {
            throw new DynamicError("Invalid relative URI " + Err.wrap(relativeURI), err);
        }

        String query = relative.getQuery();
        if (query != null && recognizeQueryParameters) {
            params = new URIQueryParameters(query, config);
            int q = relativeURI.indexOf('?');
            relativeURI = relativeURI.substring(0, q);
        }

        Source source = null;
        if (recognizeQueryParameters && relativeURI.endsWith(".ptree")) {
            source = getPTreeSource(relativeURI, base);
        }

        if (source == null) {
            try {
                url = platform.makeAbsolute(relativeURI, base);
            } catch (URISyntaxException err) {
                // System.err.println("Recovering from " + err);
                // last resort: if the base URI is null, or is itself a relative URI, we
                // try to expand it relative to the current working directory
                String expandedBase = JavaPlatform.tryToExpand(base);
                if (!expandedBase.equals(base)) { // prevent infinite recursion
                    return resolve(href, expandedBase);
                }
                //err.printStackTrace();
                throw new DynamicError("Invalid URI " + Err.wrap(relativeURI) + " - base " + Err.wrap(base), err);
            }

            source = new SAXSource();
            ((SAXSource)source).setInputSource(new InputSource(url.toString()));
            source.setSystemId(url.toString());

            if (params != null) {
                XMLReader parser = params.getXMLReader();
                if (parser != null) {
                    ((SAXSource)source).setXMLReader(parser);
                }
            }

            if (((SAXSource)source).getXMLReader() == null) {
                if (config==null) {
                    try {
                        ((SAXSource)source).setXMLReader(SAXParserFactory.newInstance().newSAXParser().getXMLReader());
                    } catch (Exception err) {
                        throw new DynamicError(err);
                    }
                } else {
                    //((SAXSource)source).setXMLReader(config.getSourceParser());
                    // Leave the Sender to allocate an XMLReader, so that it can be returned to the pool after use
                }
            }
        }

        if (params != null) {
            int stripSpace = params.getStripSpace();
            switch (stripSpace) {
                case Whitespace.ALL: {
                    Stripper stripper = AllElementStripper.getInstance();
                    stripper.setStripAll();
                    source = AugmentedSource.makeAugmentedSource(source);
                    ((AugmentedSource)source).addFilter(stripper);
                    break;
                }
                case Whitespace.IGNORABLE:
                case Whitespace.NONE:
                    source = AugmentedSource.makeAugmentedSource(source);
                    ((AugmentedSource)source).setStripSpace(stripSpace);
            }
        }

        if (id != null) {
            IDFilter filter = new IDFilter(id);
            source = AugmentedSource.makeAugmentedSource(source);
            ((AugmentedSource)source).addFilter(filter);
        }

        if (params != null) {
            Integer validation = params.getValidationMode();
            if (validation != null) {
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setSchemaValidationMode(validation.intValue());
            }
        }

        return source;
    }

    /**
     * Handle a PTree source file (Saxon-SA only)
     */

    protected Source getPTreeSource(String href, String base) throws XPathException {
        return null;
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
