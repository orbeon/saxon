package org.orbeon.saxon;

import org.orbeon.saxon.functions.FunctionLibraryList;
import org.orbeon.saxon.regex.JRegularExpression;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.JavaCollationFactory;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.Collator;
import java.util.Comparator;
import java.util.Properties;

/**
 * Implementation of the Platform class containing methods specific to the Java platform
 * (as distinct from .NET)
 */
public class JavaPlatform implements Platform {

    private static JavaPlatform theInstance = new JavaPlatform();

    public static JavaPlatform getInstance() {
        return theInstance;
    }

    private JavaPlatform() {}

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config) {}


    /**
     * Construct an absolute URI from a relative URI and a base URI
     *
     * @param relativeURI the relative URI
     * @param base        the base URI
     * @return the absolutized URI
     * @throws java.net.URISyntaxException
     */

    public URI makeAbsolute(String relativeURI, String base) throws URISyntaxException {
        URI url;
        relativeURI = escapeSpaces(relativeURI);
        base = escapeSpaces(base);
        try {
            if (base==null) {
                url = new URI(relativeURI);
                if (!url.isAbsolute()) {
                    String expandedBase = tryToExpand(base);
                    if (!expandedBase.equals(base)) { // prevent infinite recursion
                        return makeAbsolute(relativeURI, expandedBase);
                    }
                }
            } else {
                URI baseURL = new URI(base);
                url = (relativeURI.length()==0 ?
                                 baseURL :
                                 baseURL.resolve(relativeURI)
                             );
            }
        } catch (IllegalArgumentException err0) {
            // can be thrown by resolve() when given a bad URI
            throw new URISyntaxException(relativeURI, "Cannot resolve URI against base " + Err.wrap(base));
        }
        return url;
    }

    /**
     * Replace spaces by %20
     */

    public static String escapeSpaces(String s) {
        // It's not entirely clear why we have to escape spaces by hand, and not other special characters;
        // it's just that tests with a variety of filenames show that this approach seems to work.
        if (s == null) return s;
        int i = s.indexOf(' ');
        if (i < 0) {
            return s;
        }
        return (i == 0 ? "" : s.substring(0, i))
                + "%20"
                + (i == s.length()-1 ? "" : escapeSpaces(s.substring(i+1)));
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

    /**
     * Get the platform version
     */

    public String getPlatformVersion() {
        return "Java version " + System.getProperty("java.version");
    }

    /**
     * Get a suffix letter to add to the Saxon version number to identify the platform
     */

    public String getPlatformSuffix() {
        return "J";
    }

    /**
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     *
     * @param input the supplied StreamSource
     * @param validation
     * @param dtdValidation
     * @param stripspace
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     *         input Source, if now special handling is required or possible. This implementation
     *         always returns the original input unchanged.
     */

    public Source getParserSource(StreamSource input, int validation, boolean dtdValidation, int stripspace) {
        return input;
    }

    /**
     * Create a compiled regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param isXPath set to true if this is an XPath regular expression, false if it is XML Schema
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws XPathException if the syntax of the regular expression or flags is incorrect
     * @return the compiled regular expression
     */

    public RegularExpression compileRegularExpression(CharSequence regex, boolean isXPath, CharSequence flags)
    throws XPathException {
        return new JRegularExpression(regex, isXPath, flags);
    }

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=ed-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     * @param config the configuration object
     * @param props the desired properties of the collation
     * @return a collation with these properties
     * @throws XPathException if a fatal error occurs
     */

    public Comparator makeCollation(Configuration config, Properties props) throws XPathException {
        return JavaCollationFactory.makeCollation(config, props);
    }

    /**
     * Given a collation, determine whether it is capable of returning collation keys.
     * The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     *
     * @param collation the collation, provided as a Comparator
     * @return true if this collation can supply collation keys
     */

    public boolean canReturnCollationKeys(Comparator collation) {
        return collation instanceof Collator ||
                collation instanceof CodepointCollator;
    }

    /**
     * Given a collation, get a collation key. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the compareTo method.
     *
     * @throws ClassCastException if the collation is not one that is capable of supplying
     *                            collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(Comparator collation, String value) {
        if (collation instanceof CodepointCollator) {
            return value;
        }
        return ((Collator)collation).getCollationKey(value);
    }

    /**
     * Add platform-specific function libraries to the function library list
     */

    public void addFunctionLibraries(FunctionLibraryList list, Configuration config) {
        list.addFunctionLibrary(config.getExtensionBinder());
    }

    public SchemaType getExternalObjectType(String uri, String localName) {
        throw new UnsupportedOperationException("getExternalObjectType for Java");
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