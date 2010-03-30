package org.orbeon.saxon.java;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.functions.*;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.NamedCollation;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.Collator;
import java.util.Properties;

/**
 * Implementation of the Platform class containing methods specific to the Java platform
 * (as distinct from .NET). This is a singleton class with no instance data.
 */
public class JavaPlatform implements Platform {

    private static JavaPlatform theInstance = new JavaPlatform();

    /**
     * Get the singleton instance of this class
     * @return the singleton instance
     */

    public static JavaPlatform getInstance() {
        return theInstance;
    }

    private JavaPlatform() {}

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config) {
        config.setExtensionFunctionFactory("java", new JavaExtensionFunctionFactory(config));
    }

    /**
     * Return true if this is the Java platform
     */

    public boolean isJava() {
        return true;
    }

    /**
     * Return true if this is the .NET platform
     */

    public boolean isDotNet() {
        return false;
    }

    /**
     * Construct an absolute URI from a relative URI and a base URI. The method uses the resolve
     * method of the java.net.URI class, except where the base URI uses the (non-standard) "jar:" scheme,
     * in which case the method used is <code>new URL(baseURL, relativeURL)</code>.
     *
     * <p>Spaces in either URI are converted to %20</p>
     *
     * <p>If no base URI is available, and the relative URI is not an absolute URI, then the current
     * directory is used as a base URI.</p>
     *
     * @param relativeURI the relative URI. Null is permitted provided that the base URI is an absolute URI
     * @param base        the base URI
     * @return the absolutized URI
     * @throws java.net.URISyntaxException if either of the strings is not a valid URI or
     * if the resolution fails
     */

    public URI makeAbsolute(String relativeURI, String base) throws URISyntaxException {
        URI absoluteURI;
        // System.err.println("makeAbsolute " + relativeURI + " against base " + base);
        if (relativeURI == null) {
            absoluteURI = new URI(ResolveURI.escapeSpaces(base));
            if (!absoluteURI.isAbsolute()) {
                throw new URISyntaxException(base, "Relative URI not supplied, so base URI must be absolute");
            } else {
                return absoluteURI;
            }
        }
        relativeURI = ResolveURI.escapeSpaces(relativeURI);
        base = ResolveURI.escapeSpaces(base);
        try {
            if (base==null || base.length() == 0) {
                absoluteURI = new URI(relativeURI);
                if (!absoluteURI.isAbsolute()) {
                    String expandedBase = ResolveURI.tryToExpand(base);
                    if (!expandedBase.equals(base)) { // prevent infinite recursion
                        return makeAbsolute(relativeURI, expandedBase);
                    }
                }
            } else if (base != null && (base.startsWith("jar:") || base.startsWith("file:////"))) {

                // jar: URIs can't be resolved by the java.net.URI class, because they don't actually
                // conform with the RFC standards for hierarchic URI schemes (quite apart from not being
                // a registered URI scheme). But they seem to be widely used.

                // URIs starting file://// are accepted by the java.net.URI class, they are used to
                // represent Windows UNC filenames. However, the java.net.URI algorithm for resolving
                // a relative URI against such a base URI fails to produce a usable UNC filename (it's not
                // clear whether Java is implementing RFC 3986 correctly here, it depends on interpretation).
                // So we use the java.net.URL algorithm for this case too, because it works.
                
                try {
                    URL baseURL = new URL(base);
                    URL absoluteURL = new URL(baseURL, relativeURI);
                    absoluteURI = new URI(absoluteURL.toString());
                        // TODO: JDK1.5: use absoluteURL.toURI()
                } catch (MalformedURLException err) {
                    throw new URISyntaxException(base + " " + relativeURI, err.getMessage());
                }
            } else {
                URI baseURI;
                try {
                    baseURI = new URI(base);
                } catch (URISyntaxException e) {
                    throw new URISyntaxException(base, "Invalid base URI: " + e.getMessage());
                }
                try {
                    new URI(relativeURI);   // for validation only
                } catch (URISyntaxException e) {
                    throw new URISyntaxException(base, "Invalid relative URI: " + e.getMessage());
                }
                absoluteURI = (relativeURI.length()==0 ?
                                 baseURI :
                                 baseURI.resolve(relativeURI)
                             );
            }
        } catch (IllegalArgumentException err0) {
            // can be thrown by resolve() when given a bad URI
            throw new URISyntaxException(relativeURI, "Cannot resolve URI against base " + Err.wrap(base));
        }

        return absoluteURI;
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
     * @param pipe the pipeline configuration
     * @param input the supplied StreamSource
     * @param validation indicates whether schema validation is required
     * @param dtdValidation indicates whether DTD validation is required
     * @param stripspace indicates whether whitespace text nodes should be stripped
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     *         input Source, if now special handling is required or possible. This implementation
     *         always returns the original input unchanged.
     */

    public Source getParserSource(PipelineConfiguration pipe, StreamSource input, int validation,
                                  boolean dtdValidation, int stripspace) {
        return input;
    }

    /**
     * Create a compiled regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param xmlVersion set to integer 10 for XML 1.0, 11 for XML 1.1
     * @param syntax requests XPath or XML Schema regex syntax
     *@param flags the flags argument as supplied to functions such as fn:matches(), in string form @throws XPathException if the syntax of the regular expression or flags is incorrect @return the compiled regular expression
     */

    public RegularExpression compileRegularExpression(
            CharSequence regex, int xmlVersion, int syntax, CharSequence flags)
    throws XPathException {
        int flagBits = JRegularExpression.setFlags(flags);
        return new JRegularExpression(regex, xmlVersion, syntax, flagBits);
    }

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=ed-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     * @param config the configuration object
     * @param props the desired properties of the collation
     * @param uri the collation URI
     * @return a collation with these properties
     * @throws XPathException if a fatal error occurs
     */

    public StringCollator makeCollation(Configuration config, Properties props, String uri) throws XPathException {
        return JavaCollationFactory.makeCollation(config, uri, props);
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

    public boolean canReturnCollationKeys(StringCollator collation) {
        return (collation instanceof CodepointCollator) ||
                ((collation instanceof NamedCollation) &&
                        (((NamedCollation)collation).getCollation() instanceof Collator));
    }

    /**
     * Given a collation, get a collation key. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     *
     * @throws ClassCastException if the collation is not one that is capable of supplying
     *                            collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(NamedCollation namedCollation, String value) {
        return ((Collator)namedCollation.getCollation()).getCollationKey(value);
    }

    /**
     * Make the default extension function factory appropriate to the platform
     */

    public void makeExtensionLibrary(Configuration config) {
        config.setExtensionBinder("java", new JavaExtensionLibrary(config));
    }

    /**
     * Add the platform-specific function libraries to a function library list. The libraries
     * that are added are those registered with the Configuration using
     *  {@link Configuration#setExtensionBinder(String, org.orbeon.saxon.functions.FunctionLibrary)}
     * @param list the function library list that is to be extended
     * @param config the Configuration
     * @param hostLanguage the host language, for example Configuration.XQUERY
     */

    public void addFunctionLibraries(FunctionLibraryList list, Configuration config, int hostLanguage) {
        FunctionLibrary extensionBinder = config.getExtensionBinder("java");
//        See bug 2922201, test case test/users/grosso
//        if (extensionBinder instanceof JavaExtensionLibrary) {
//            ((JavaExtensionLibrary)extensionBinder).setStrictJavaUriFormat(
//                    hostLanguage != Configuration.XSLT
//            );
//        }
        list.addFunctionLibrary(extensionBinder);
    }

    /**
     * Register a namespace-to-Java-class mapping declared using saxon:script in an XSLT stylesheet
     * @param library the library to contain the function, which must be a JavaExtensionLibrary
     * @param uri the namespace of the function name
     * @param theClass the Java class that implements this namespace
     */     

    public void declareJavaClass(FunctionLibrary library, String uri, Class theClass) {
        if (library instanceof JavaExtensionLibrary) {
            ((JavaExtensionLibrary)library).declareJavaClass(uri, theClass);
        } else {
            throw new IllegalStateException("saxon:script cannot be used with a custom extension library factory");
        }
    }

    public SchemaType getExternalObjectType(Configuration config, String uri, String localName) {
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