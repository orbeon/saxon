package net.sf.saxon.om;

/**
 * This class is not instantiated, it exists to hold a set of constants representing known
 * namespaces. For each of these, there is a constant for the namespace URI and for many of
 * them, there is a numeric constant used as the code for this namespace in the name pool.
 *
 * <p>This class also defines constant URIs for some objects other than namespaces -
 * for example, URIs that identify the various object models used in the JAXP XPath API,
 * and the Unicode codepoint collation URI.</p>
 *
 * @author Michael H. Kay
 */

public class NamespaceConstant {

	/**
	 * A URI representing the null namespace (actually, an empty string)
	 */

	public static final String NULL = "";
	/**
	 * The numeric code representing the null namespace (actually, zero)
	 */
	public static final short NULL_CODE = 0;
    /**
     * The namespace code for the null namespace
     */
    public static final int NULL_NAMESPACE_CODE = 0;

    /**
     * Fixed namespace name for XML: "http://www.w3.org/XML/1998/namespace".
     */
    public static final String XML = "http://www.w3.org/XML/1998/namespace";
    /**
     * Numeric code representing the XML namespace
     */
    public static final short XML_CODE = 1;
    /**
     * The namespace code for the XML namespace
     */
    public static final int XML_NAMESPACE_CODE = 0x00010001;


    /**
     * Fixed namespace name for XSLT: "http://www.w3.org/1999/XSL/Transform"
     */
    public static final String XSLT = "http://www.w3.org/1999/XSL/Transform";
    /**
     * Numeric code representing the XSLT namespace
     */
    public static final short XSLT_CODE = 2;

    /**
     * Fixed namespace name for SAXON: "http://saxon.sf.net/"
     */
    public static final String SAXON = "http://saxon.sf.net/";
    /**
     * Numeric code representing the SAXON namespace
     */
    public static final short SAXON_CODE = 3;

    /**
     * Namespace name for XML Schema: "http://www.w3.org/2001/XMLSchema"
     */
    public static final String SCHEMA = "http://www.w3.org/2001/XMLSchema";
    /**
     * Numeric code representing the schema namespace
     */
    public static final short SCHEMA_CODE = 4;

    /**
     * Namespace for additional XPath-defined data types:
     * "http://www.w3.org/2005/04/xpath-datatypes"
     */
    public static final String XDT = "http://www.w3.org/2005/04/xpath-datatypes";

    /**
     * Older versions of XDT namespace
     */

    public static final String XDT200502 = "http://www.w3.org/2005/02/xpath-datatypes";
    public static final String XDT200410 = "http://www.w3.org/2004/10/xpath-datatypes";
    public static final String XDT200407 = "http://www.w3.org/2004/07/xpath-datatypes";

    /**
     * Test whether a namespace is the XDT namespace
     */

    public static final boolean isXDTNamespace(String uri) {
        return uri.equals(XDT) || uri.equals(XDT200502) || uri.equals(XDT200410) || uri.equals(XDT200407);
    }

    /**
     * Numeric code representing the schema namespace
     */
    public static final short XDT_CODE = 5;

    /**
     * XML-schema-defined namespace for use in instance documents ("xsi")
     */
    public static final String SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    public static final short XSI_CODE = 6;

    /**
     * Fixed namespace name for EXSLT/Common: "http://exslt.org/common"
     */
    public static final String EXSLT_COMMON = "http://exslt.org/common";

    /**
     * Fixed namespace name for EXSLT/math: "http://exslt.org/math"
     */
    public static final String EXSLT_MATH = "http://exslt.org/math";

    /**
     * Fixed namespace name for EXSLT/sets: "http://exslt.org/sets"
     */
    public static final String EXSLT_SETS = "http://exslt.org/sets";

    /**
     * Fixed namespace name for EXSLT/date: "http://exslt.org/dates-and-times"
     */
    public static final String EXSLT_DATES_AND_TIMES = "http://exslt.org/dates-and-times";

    /**
     * Fixed namespace name for EXSLT/random: "http://exslt.org/random"
     */
    public static final String EXSLT_RANDOM = "http://exslt.org/random";

    /**
     * The standard namespace for functions and operators
     */
    public static final String FN = "http://www.w3.org/2005/04/xpath-functions";

    /**
     * The standard namespace for system error codes
     */
    public static final String ERR = "http://www.w3.org/2004/07/xqt-errors";


    /**
     * Predefined XQuery namespace for local functions
     */
    public static final String LOCAL = "http://www.w3.org/2005/02/xquery-local-functions";
    /**
     * Recognize the Microsoft namespace so we can give a suitably sarcastic error message
     */

    public static final String MICROSOFT_XSL = "http://www.w3.org/TR/WD-xsl";

    /**
     * The XHTML namespace http://www.w3.org/1999/xhtml
     */

    public static final String XHTML = "http://www.w3.org/1999/xhtml";

    /**
     * Namespace for types representing external Java objects
     */

    public static final String JAVA_TYPE = "http://saxon.sf.net/java-type";

    /**
     * Namespace for names allocated to anonymous types. This exists so that
     * a name fingerprint can be allocated for use as a type annotation.
     */

    public static final String ANONYMOUS = "http://ns.saxonica.com/anonymous-type";

    /**
     * URI identifying the Saxon object model for use in the JAXP 1.3 XPath API
     */

    public static final String OBJECT_MODEL_SAXON = "http://saxon.sf.net/jaxp/xpath/om";


    /**
     * URI identifying the XOM object model for use in the JAXP 1.3 XPath API
     */

    public static final String OBJECT_MODEL_XOM = "http://www.xom.nu/jaxp/xpath/xom";

    /**
     * URI identifying the JDOM object model for use in the JAXP 1.3 XPath API
     */

    public static final String OBJECT_MODEL_JDOM = "http://jdom.org/jaxp/xpath/jdom";

    /**
     * URI identifying the Unicode codepoint collation
     */

    public static final String CodepointCollationURI = "http://www.w3.org/2005/04/xpath-functions/collation/codepoint";

    /**
     * Private constructor: class is never instantiated
     */

    private NamespaceConstant() {
    }

    /**
     * Determine whether a namespace is a reserved namespace
     */

    public static final boolean isReserved(String uri) {
        return uri.equals(XSLT) ||
                uri.equals(FN) ||
                uri.equals(XML) ||
                uri.equals(SCHEMA)||
                uri.equals(XDT) ||
                uri.equals(SCHEMA_INSTANCE);
    }

    /**
     * Determine whether a namespace is a reserved namespace
     */

    public static final boolean isReservedInQuery(String uri) {
        return  uri.equals(FN) ||
                uri.equals(XML) ||
                uri.equals(SCHEMA) ||
                uri.equals(XDT) ||
                uri.equals(SCHEMA_INSTANCE);
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
