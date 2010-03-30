package org.orbeon.saxon;
import org.orbeon.saxon.event.IDFilter;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.functions.EscapeURI;
import org.orbeon.saxon.functions.ResolveURI;
import org.orbeon.saxon.functions.URIQueryParameters;
import org.orbeon.saxon.om.AllElementStripper;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.Err;
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

    // TODO: IDEA: support the data: URI scheme. (Requires unescaping of the URI, then parsing the content as XML)

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
     * @return the platform
     */

    protected Platform getPlatform() {
        return Configuration.getPlatform();
    }

    /**
     * Set the configuration
     * @param config the configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the configuration if available
     * @return the configuration
     */

    public Configuration getConfiguration() {
        return config;
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
        URI uri;
        URI relative;
        try {
            relativeURI = ResolveURI.escapeSpaces(relativeURI);
            relative = new URI(relativeURI);
        } catch (URISyntaxException err) {
            throw new XPathException("Invalid relative URI " + Err.wrap(relativeURI), err);
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
                uri = platform.makeAbsolute(relativeURI, base);
            } catch (URISyntaxException err) {
                // System.err.println("Recovering from " + err);
                // last resort: if the base URI is null, or is itself a relative URI, we
                // try to expand it relative to the current working directory
                String expandedBase = ResolveURI.tryToExpand(base);
                if (!expandedBase.equals(base)) { // prevent infinite recursion
                    return resolve(href, expandedBase);
                }
                //err.printStackTrace();
                throw new XPathException("Invalid URI " + Err.wrap(relativeURI) + " - base " + Err.wrap(base), err);
            }

            // Check that any "%" sign in the URI is part of a well-formed percent-encoded UTF-8 character.
            // Without this check, dereferencing the resulting URL can fail with arbitrary unchecked exceptions

            final String uriString = uri.toString();
            EscapeURI.checkPercentEncoding(uriString);

            source = new SAXSource();
            setSAXInputSource((SAXSource)source, uriString);


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
                        throw new XPathException(err);
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

        if (params != null) {
            Boolean xinclude = params.getXInclude();
            if (xinclude != null) {
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setXIncludeAware(xinclude.booleanValue());
            }
        }

        return source;
    }

    /**
     * Handle a PTree source file (Saxon-SA only)
     * @param href the relative URI
     * @param base the base URI
     * @return the new Source object
     */

    protected Source getPTreeSource(String href, String base) throws XPathException {
        throw new XPathException("PTree files can only be read using a Saxon-SA configuration");
    }

    /**
     * Set the InputSource part of the returned SAXSource. This is done in a separate
     * method to allow subclassing. The default implementation simply places the URI in the
     * InputSource, allowing the XML parser to take responsibility for the dereferencing.
     * A subclass may choose to dereference the URI at this point an place an InputStream
     * in the SAXSource.
     * @param source the SAXSource being initialized
     * @param uriString the absolute (resolved) URI to be used
     */

    protected void setSAXInputSource(SAXSource source, String uriString) {
        source.setInputSource(new InputSource(uriString));
        source.setSystemId(uriString);
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
