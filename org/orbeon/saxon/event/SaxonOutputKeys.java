package org.orbeon.saxon.event;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import java.util.StringTokenizer;

/**
 * Provides string constants that can be used to set
 * output properties for a Transformer, or to retrieve
 * output properties from a Transformer or Templates object.
 *
 * These keys are private Saxon keys that supplement the standard keys
 * defined in javax.xml.transform.OutputKeys. As well as Saxon extension
 * attributes, the list includes new attributes defined in XSLT 2.0 which
 * are not yet supported in JAXP
 */

public class SaxonOutputKeys {

    /**
     * This class is not instantiated
     */

    private SaxonOutputKeys() {}

    /**
     * String constant representing the saxon:xquery output method name
     */

    public static final String SAXON_XQUERY_METHOD = "{http://saxon.sf.net/}xquery";

    /**
     * saxon:indentSpaces = integer.
     *
     * <p>Defines the number of spaces used for indentation of output</p>
     */

    public static final String INDENT_SPACES = "{http://saxon.sf.net/}indent-spaces";

    /**
     * saxon:suppress-indentation = list of element names
     *
     * <p>Defines elements within which no indentation will occur</p>
     */

    public static final String SUPPRESS_INDENTATION = "{http://saxon.sf.net/}suppress-indentation";

    /**
     * saxon:double-space = list of element names
     *
     * <p>Defines elements that will have an extra blank line added before the start tag, in addition
     * to normal indentation</p>
     */

    public static final String DOUBLE_SPACE = "{http://saxon.sf.net/}double-space";

    /**
     * stylesheet-version. This serialization parameter is set automatically by the XSLT processor
     * to the value of the version attribute on the principal stylesheet module.
     */

    public static final String STYLESHEET_VERSION = "{http://saxon.sf.net/}stylesheet-version";

    /**
     * use-character-map = list-of-qnames.
     *
     * <p>Defines the character maps used in this output definition. The QNames
     * are represented in Clark notation as {uri}local-name.</p>
     */

    public static final String USE_CHARACTER_MAPS = "use-character-maps";


    /**
     * include-content-type = "yes" | "no". This attribute is defined in XSLT 2.0
     *
     * <p>Indicates whether the META tag is to be added to HTML output</p>
     */

    public static final String INCLUDE_CONTENT_TYPE = "include-content-type";

   /**
     * undeclare-prefixes = "yes" | "no". This attribute is defined in XSLT 2.0
     *
     * <p>Indicates XML 1.1 namespace undeclarations are to be output when required</p>
     */

    public static final String UNDECLARE_PREFIXES = "undeclare-prefixes";

    /**
     * escape-uri-attributes = "yes" | "no". This attribute is defined in XSLT 2.0
     *
     * <p>Indicates whether HTML attributes of type URI are to be URI-escaped</p>
     */

    public static final String ESCAPE_URI_ATTRIBUTES = "escape-uri-attributes";

    /**
     * representation = rep1[;rep2].
     *
     * <p>Indicates the preferred way of representing non-ASCII characters in HTML
     * and XML output. rep1 is for characters in the range 128-256, rep2 for those
     * above 256.</p>
     */
    public static final String CHARACTER_REPRESENTATION = "{http://saxon.sf.net/}character-representation";

    /**
     * saxon:next-in-chain = URI.
     *
     * <p>Indicates that the output is to be piped into another XSLT stylesheet
     * to perform another transformation. The auxiliary property NEXT_IN_CHAIN_BASE_URI
     * records the base URI of the stylesheet element where this attribute was found.</p>
     */
    public static final String NEXT_IN_CHAIN = "{http://saxon.sf.net/}next-in-chain";
    public static final String NEXT_IN_CHAIN_BASE_URI = "{http://saxon.sf.net/}next-in-chain-base-uri";

    /**
    * byte-order-mark = yes|no.
    *
    * <p>Indicates whether UTF-8/UTF-16 output is to start with a byte order mark. Values are "yes" or "no",
    * default is "no"
    */

    public static final String BYTE_ORDER_MARK = "byte-order-mark";

    /**
     * normalization-form = NFC|NFD|NFKC|NFKD|non.
     *
     * <p>Indicates that a given Unicode normalization form (or no normalization) is required.
     */

     public static final String NORMALIZATION_FORM = "normalization-form";

    /**
     * supply-source-locator = yes|no.
     *
     * <p>If set to "yes", and the output is being sent to a SAXResult (or to a user-supplied content handler),
     * indicates that the SAX Locator made available to the ContentHandler will contain information about the
     * location of the context node in the source document as well as the location in the stylesheet or query.</p>
     */

     public static final String SUPPLY_SOURCE_LOCATOR = "{http://saxon.sf.net/}supply-source-locator";

    /**
    * saxon:require-well-formed = yes|no.
    *
    * <p>Indicates whether a user-supplied ContentHandler requires the stream of SAX events to be
    * well-formed (that is, to have a single element node and no text nodes as children of the root).
    * The default is "no".</p>
    */

    public static final String REQUIRE_WELL_FORMED = "{http://saxon.sf.net/}require-well-formed";

    /**
     * wrap="yes"|"no".
     * <p>
     * This property is only available in the XQuery API. The value "yes" indicates that the result
     * sequence produced by the query is to be wrapped, that is, each item in the result is represented
     * as a separate element. This format allows any sequence to be represented as an XML document,
     * including for example sequences consisting of parentless attribute nodes.
     */

    public static final String WRAP = "{http://saxon.sf.net/}wrap-result-sequence";

    /**
     * Property used internally to identify the XSLT implicit result document
     */

    public static final String IMPLICIT_RESULT_DOCUMENT = "{http://saxon.sf.net/}implicit-result-document";

    /**
     * Check that a supplied output property is valid.
     * @param key the name of the property
     * @param value the value of the property. This may be set to null, in which case no validation takes place.
     * The value must be in JAXP format, that is, with lexical QNames expanded to Clark names
     * @param checker the NameChecker to be used for validating QNames
     * @throws XPathException if the property name or value is invalid
     */

    public static void checkOutputProperty(String key, String value, NameChecker checker) throws XPathException {
        if (!key.startsWith("{") || key.startsWith("{http://saxon.sf.net/}" )) {
            if (key.equals(BYTE_ORDER_MARK)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(OutputKeys.CDATA_SECTION_ELEMENTS)) {
                if (value != null) {
                    checkListOfClarkNames(key, value, checker);
                }
            } else if (key.equals(OutputKeys.DOCTYPE_PUBLIC)) {
                // no constraints
            } else if (key.equals(OutputKeys.DOCTYPE_SYSTEM)) {
                // no constraints
            } else if (key.equals(OutputKeys.ENCODING)) {
                // no constraints
            } else if (key.equals(ESCAPE_URI_ATTRIBUTES) || key.equals("escape-uri-attibutes")) {
                // constant was misspelled in 9.0 and earlier releases
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(INCLUDE_CONTENT_TYPE)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(OutputKeys.INDENT)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(OutputKeys.MEDIA_TYPE)) {
                // no constraints
            } else if (key.equals(OutputKeys.METHOD)) {
                if (value != null) {
                    checkMethod(value, checker);
                }
            } else if (key.equals(NORMALIZATION_FORM)) {
                if (value != null) {
                    checkNormalizationForm(value);
                }
            } else if (key.equals(OutputKeys.OMIT_XML_DECLARATION)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(OutputKeys.STANDALONE)) {
                if (value != null && !value.equals("omit")) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(UNDECLARE_PREFIXES)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(USE_CHARACTER_MAPS)) {
                if (value != null) {
                    checkListOfClarkNames(key, value, checker);
                }
            } else if (key.equals(OutputKeys.VERSION)) {
                // no constraints
            } else if (key.equals(STYLESHEET_VERSION)) {
                // no constraints
            } else if (key.equals(INDENT_SPACES)) {
                if (value != null) {
                    checkNonNegativeInteger(key, value);
                }
            } else if (key.equals(CHARACTER_REPRESENTATION)) {
                // no validation performed
            } else if (key.equals(NEXT_IN_CHAIN)) {
                // no validation performed
            } else if (key.equals(NEXT_IN_CHAIN_BASE_URI)) {
                // no validation performed
            } else if (key.equals(REQUIRE_WELL_FORMED)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(SUPPRESS_INDENTATION)) {
                if (value != null) {
                    checkListOfClarkNames(key, value, checker);
                }

            } else if (key.equals(DOUBLE_SPACE)) {
                if (value != null) {
                    checkListOfClarkNames(key, value, checker);
                }

            } else if (key.equals(WRAP)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(SUPPLY_SOURCE_LOCATOR)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else {
                throw new XPathException("Unknown serialization parameter " + Err.wrap(key));
            }
        } else {
            //return;
        }
    }

    private static void checkYesOrNo(String key, String value) throws XPathException {
        if ("yes".equals(value) || "no".equals(value)) {
            // OK
        } else {
            throw new XPathException("Serialization parameter " + Err.wrap(key) + " must have the value yes or no");
        }
    }

    private static void checkMethod(String value, NameChecker checker) throws XPathException {
        if ("xml".equals(value)) return;
        if ("html".equals(value)) return;
        if ("xhtml".equals(value)) return;
        if ("text".equals(value)) return;
        if (isValidClarkName(value, checker)) return;
        throw new XPathException("Invalid value for serialization method: " +
                "must be xml, html, xhtml, text, or a QName in '{uri}local' form");

    }

    private static void checkNormalizationForm(String value) throws XPathException {
        if ("NFC".equals(value)) return;
        if ("NFD".equals(value)) return;
        if ("NFKC".equals(value)) return;
        if ("NFKD".equals(value)) return;
        if ("fully-normalized".equals(value)) return;
        if ("none".equals(value)) return;
        throw new XPathException("Invalid value for normalization-form: " +
                "must be NFC, NFD, NFKC, NFKD, fully-normalized, or none");

    }

    private static boolean isValidClarkName(String value, NameChecker checker) {
        if (value.charAt(0) != '{') {
            return false;
        }
        int closer = value.indexOf('}');
        return closer >= 2 &&
                closer != value.length() - 1 &&
                checker.isValidNCName(value.substring(closer + 1));
    }

    private static void checkNonNegativeInteger(String key, String value) throws XPathException {
        try {
            int n = Integer.parseInt(value);
            if (n < 0) {
                throw new XPathException("Value of " +  Err.wrap(key) + " must be a non-negative integer");
            }
        } catch (NumberFormatException err) {
            throw new XPathException("Value of " +  Err.wrap(key) + " must be a non-negative integer");
        }
    }

    private static void checkListOfClarkNames(String key, String value, NameChecker checker) throws XPathException {
        StringTokenizer tok = new StringTokenizer(value, " \t\n\r", false);
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            if (isValidClarkName(s, checker) || checker.isValidNCName(s)) {
                // ok
            } else {
                throw new XPathException("Value of " +  Err.wrap(key) +
                        " must be a list of QNames in '{uri}local' notation");
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