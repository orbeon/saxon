package org.orbeon.saxon;

import org.orbeon.saxon.functions.FunctionLibraryList;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.Properties;

/**
 * This interface provides access to methods whose implementation depends on the chosen platform
 * (typically Java or .NET)
 */
public interface Platform extends Serializable {

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config);

    /**
     * Construct an absolute URI from a relative URI and a base URI
     * @param relativeURI the relative URI
     * @param base the base URI
     * @return the absolutized URI
     * @throws URISyntaxException
     */

    public URI makeAbsolute(String relativeURI, String base) throws URISyntaxException;

    /**
     * Get the platform version
     */

    public String getPlatformVersion();

    /**
     * Get a suffix letter to add to the Saxon version number to identify the platform
     */

    public String getPlatformSuffix();

    /**
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     * @param input the supplied StreamSource
     * @param validation
     * @param dtdValidation
     * @param stripspace
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     * input Source, if now special handling is required or possible
     */

    public Source getParserSource(StreamSource input, int validation, boolean dtdValidation, int stripspace);

    /**
     * Create a compiled regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param isXPath set to true if this is an XPath regular expression, false if it is XML Schema
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws XPathException if the syntax of the regular expression or flags is incorrect
     * @return the compiled regular expression
     */

    public RegularExpression compileRegularExpression(CharSequence regex, boolean isXPath, CharSequence flags)
    throws XPathException;

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

    public Comparator makeCollation(Configuration config, Properties props) throws XPathException;

    /**
     * Given a collation, determine whether it is capable of returning collation keys.
     * The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     * @param collation the collation being examined, provided as a Comparator
     * @return true if this collation can supply collation keys
     */

    public boolean canReturnCollationKeys(Comparator collation);

    /**
     * Given a collation, get a collation key. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     * @return a representation of the collation key, such that two collation keys are
     * equal() if and only if the string values they represent are equal under the specified collation.
     * @throws ClassCastException if the collation is not one that is capable of supplying
     * collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(Comparator collation, String value);

    /**
     * Add platform-specific function libraries to the function library list
     */

    public void addFunctionLibraries(FunctionLibraryList list, Configuration config);

    public SchemaType getExternalObjectType(String uri, String localName);

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

