package net.sf.saxon.event;
import net.sf.saxon.Err;
import net.sf.saxon.om.XMLChar;
import net.sf.saxon.trans.DynamicError;

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
     * indentSpaces = integer.
     *
     * <p>Defines the number of spaces used for indentation of output</p>
     */

    public static final String INDENT_SPACES = "{http://saxon.sf.net/}indent-spaces";

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

    public static final String ESCAPE_URI_ATTRIBUTES = "escape-uri-attibutes";

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
    * saxon:require-well-formed = yes|no.
    *
    * <p>Indicates whether a user-supplied ContentHandler requires the stream of SAX events to be
    * well-formed (that is, to have a single element node and no text nodes as children of the root).
    * The default is "no".</p>
    */

    public static final String REQUIRE_WELL_FORMED = "{http://saxon.sf.net/}require-well-formed";

    /**
     * Check that a supplied output property is valid.
     * @param key the name of the property
     * @param value the value of the property. This may be set to null, in which case
     * only the property name is checked.
     */

    public static final void checkOutputProperty(String key, String value) throws DynamicError {
        if (!key.startsWith("{") || key.startsWith("{http://saxon.sf.net/}" )) {
            if (key.equals(OutputKeys.CDATA_SECTION_ELEMENTS)) {
                if (value != null) {
                    checkListOfClarkNames(key, value);
                }
            } else if (key.equals(OutputKeys.DOCTYPE_PUBLIC)) {
                // no constraints
            } else if (key.equals(OutputKeys.DOCTYPE_SYSTEM)) {
                // no constraints
            } else if (key.equals(OutputKeys.ENCODING)) {
                // no constraints
            } else if (key.equals(OutputKeys.INDENT)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(OutputKeys.MEDIA_TYPE)) {
                // no constraints
            } else if (key.equals(OutputKeys.METHOD)) {
                if (value != null) {
                    checkMethod(value);
                }
            } else if (key.equals(OutputKeys.OMIT_XML_DECLARATION)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(OutputKeys.STANDALONE)) {
                if (value != null && !value.equals("omit")) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(OutputKeys.VERSION)) {
                // no constraints
            } else if (key.equals(INDENT_SPACES)) {
                if (value != null) {
                    checkNonNegativeInteger(key, value);
                }
            } else if (key.equals(INCLUDE_CONTENT_TYPE)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(ESCAPE_URI_ATTRIBUTES)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(CHARACTER_REPRESENTATION)) {
                // no validation performed
            } else if (key.equals(NEXT_IN_CHAIN)) {
                // no validation performed
            } else if (key.equals(NEXT_IN_CHAIN_BASE_URI)) {
                // no validation performed
            } else if (key.equals(UNDECLARE_PREFIXES)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(USE_CHARACTER_MAPS)) {
                if (value != null) {
                    checkListOfClarkNames(key, value);
                }
            } else if (key.equals(REQUIRE_WELL_FORMED)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else if (key.equals(BYTE_ORDER_MARK)) {
                if (value != null) {
                    checkYesOrNo(key, value);
                }
            } else {
                throw new DynamicError("Unknown serialization parameter " + Err.wrap(key));
            }
        } else {
            return;
        }
    }

    private static void checkYesOrNo(String key, String value) throws DynamicError {
        if ("yes".equals(value) || "no".equals(value)) {
            // OK
        } else {
            throw new DynamicError("Serialization parameter " + Err.wrap(key) + " must have the value yes or no");
        }
    }

    private static void checkMethod(String value) throws DynamicError {
        if ("xml".equals(value)) return;
        if ("html".equals(value)) return;
        if ("xhtml".equals(value)) return;
        if ("text".equals(value)) return;
        if (isValidClarkName(value)) return;
        throw new DynamicError("Invalid value for serialization method: " +
                "must be xml, html, xhtml, text, or a QName in '{uri}local' form");

    }

    private static boolean isValidClarkName(String value) {
        if (value.charAt(0) != '{') return false;
        int closer = value.indexOf('}');
        if (closer < 2) return false;
        if (closer == value.length()-1) return false;
        if (!XMLChar.isValidNCName(value.substring(closer+1))) return false;
        return true;
    }

    private static void checkNonNegativeInteger(String key, String value) throws DynamicError {
        try {
            int n = Integer.parseInt(value);
            if (n < 0) {
                throw new DynamicError("Value of " +  Err.wrap(key) + " must be a non-negative integer");
            }
        } catch (NumberFormatException err) {
            throw new DynamicError("Value of " +  Err.wrap(key) + " must be a non-negative integer");
        }
    }

    private static void checkListOfClarkNames(String key, String value) throws DynamicError {
        StringTokenizer tok = new StringTokenizer(value);
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            if (isValidClarkName(s) || XMLChar.isValidNCName(s)) {
                // ok
            } else {
                throw new DynamicError("Value of " +  Err.wrap(key) +
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