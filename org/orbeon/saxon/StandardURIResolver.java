package net.sf.saxon;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


/**
* This class provides the service of converting a URI into an InputSource.
* It is used to get stylesheet modules referenced by xsl:import and xsl:include,
* and source documents referenced by the document() function. The standard version
* handles anything that the java URL class will handle.
* You can write a subclass to handle other kinds of URI, e.g. references to things in
* a database.
* @author Michael H. Kay
*/

public class StandardURIResolver implements URIResolver {

    private Configuration config = null;

    /**
     * Create a StandardURIResolver, with no reference to a TransformerFactory
     */
    public StandardURIResolver() {
        this(null);
    }

    /**
     * Create a StandardURIResolver, with a reference to a TransformerFactory
     * @param config The Configuration object.
     * This is used to get a SAX Parser for a source XML document
     */

    public StandardURIResolver(Configuration config) {
        this.config = config;
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

		URI url;
        try {
            url = makeAbsolute(relativeURI, base);
        } catch (URISyntaxException err) {
            // System.err.println("Recovering from " + err);
            // last resort: if the base URI is null, or is itself a relative URI, we
            // try to expand it relative to the current working directory
            String expandedBase = tryToExpand(base);
            if (!expandedBase.equals(base)) { // prevent infinite recursion
                return resolve(href, expandedBase);
            }
            //err.printStackTrace();
            throw new DynamicError("Invalid URI " + Err.wrap(relativeURI) + " - base " + Err.wrap(base), err);
        }

        SAXSource source = new SAXSource();
        source.setInputSource(new InputSource(url.toString()));
        source.setSystemId(url.toString());

        if (id!=null) {
            IDFilter filter = new IDFilter(id);
            XMLReader parser;
            if (config==null) {
                try {
                    parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                } catch (Exception err) {
                    throw new DynamicError(err);
                }
            } else {
                parser = config.getSourceParser();
            }
            filter.setParent(parser);
            source.setXMLReader(filter);
        }
        return source;
    }

    /**
     * Combine the relative URI and base URI
     */

    public static URI makeAbsolute(String relativeURI, String base) throws DynamicError, URISyntaxException {
        URI url;
        try {
            if (base==null) {
                url = new URI(relativeURI);
                // System.err.println("Resolved " + relativeURI + " as " + url.toString());
            } else {
                // System.err.println("Resolving " + relativeURI + " against " + base);
                URI baseURL = new URI(base);
                // System.err.println("Base URI " + base);
                url = (relativeURI.length()==0 ?
                                 baseURL :
                                 baseURL.resolve(relativeURI)
                             );
                // This method incorrectly double-escapes percent signs, for example %20 is escaped
                // to %2520. The only solution seems to be to remove them by hand..
                String u = url.toString();
                int pc = u.indexOf("%25");
                if (pc >= 0) {
                    while (pc>=0) {
                        u = u.substring(0, pc+1) + u.substring(pc+3);
                        pc = u.indexOf("%25");
                    }
                    url = new URI(u);
                }
                // System.err.println("Resolved URI " + url);
            }
        } catch (IllegalArgumentException err0) {
            // can be thrown by resolve() when given a bad URI
            throw new DynamicError("Invalid URI " + Err.wrap(relativeURI) + " - base " + Err.wrap(base));
        }
        return url;
    }
    /**
    * If a system ID can't be parsed as a URL, we'll try to expand it as a relative
    * URI using the current directory as the base URI: MHK addition.
    */

    public static String tryToExpand(String systemId) {
        if (systemId==null) {
            systemId = "";
        }
	    try {
	        new URL(systemId);
	        return systemId;   // all is well
	    } catch (MalformedURLException err) {
	        String dir;
	        try {
	            dir = System.getProperty("user.dir");
	        } catch (Exception geterr) {
	            // this doesn't work when running an applet
	            return systemId;
	        }
	        if (!(dir.endsWith("/") || systemId.startsWith("/"))) {
	            dir = dir + '/';
	        }

	        try {
	            URL currentDirectoryURL = new File(dir).toURL();  
	            URL baseURL = new URL(currentDirectoryURL, systemId);
	            // System.err.println("SAX Driver: expanded " + systemId + " to " + baseURL);
	            return baseURL.toString();
	        } catch (MalformedURLException err2) {
	            // go with the original one
	            return systemId;
	        }
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
