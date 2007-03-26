package org.orbeon.saxon.trans;

/**
 * The class acts as a register of Saxon-specific error codes.
 * <p>
 * Technically, these codes should be in their own namespace. At present, however, they share the
 * same namespace as system-defined error codes.
 */
public class SaxonErrorCode {

    /**
     * SXLM0001: stylesheet or query appears to be looping/recursing indefinitely
     */

    public static final String SXLM0001 = "SXLM0001";

    /**
     * SXCH0002: cannot supply output to ContentHandler because it is not well-formed
     */

    public static final String SXCH0002 = "SXCH0002";

    /**
     * SXCH0003: error reported by the ContentHandler (SAXResult) to which the result tree was sent
     */

    public static final String SXCH0003 = "SXCH0003";

    /**
     * SXSE0001: cannot use character maps in an environment with no Controller
     */

    public static final String SXSE0001 = "SXSE0001";

    /**
     * SXSE0002: cannot use output property saxon:supply-source-locator unless tracing was enabled at compile time
     */

    public static final String SXSE0002 = "SXSE0002";


    /**
     * SXXP0003: error reported by XML parser while parsing source document
     */

    public static final String SXXP0003 = "SXXP0003";

    /**
     * SXXF0001: first argument to saxon:eval must be an expression prepared using saxon:expression
     */

    public static final String SXXF0001 = "SXXF0001";

    /**
     * SXXF0002: undeclared namespace prefix used in saxon:script
     */

    public static final String SXXF0002 = "SXXF0002";

    /**
     * SXSQ0001: value of argument to SQL instruction is not a JDBC Connection object
     */

     public static final String SXSQ0001 = "SXSQ0001";

    /**
     * SXSQ0002: failed to close JDBC Connection
     */

     public static final String SXSQ0002 = "SXSQ0002";

    /**
     * SXSQ0003: failed to open JDBC Connection
     */

     public static final String SXSQ0003 = "SXSQ0003";

    /**
     * SXSQ0004: SQL Insert/Update/Delete action failed
     */

     public static final String SXSQ0004 = "SXSQ0004";

    /**
     * SXJE0001: cannot convert xs:boolean to the required Java type
     */

    public static final String SXJE0001 = "SXJE0001";

    /**
     * SXJE0002: cannot convert xs:double to the required Java type
     */

    public static final String SXJE0002 = "SXJE0002";

    /**
     * SXJE0003: cannot convert xs:duration to the required Java type
     */

    public static final String SXJE0003 = "SXJE0003";

    /**
     * SXJE0004: cannot convert xs:float to the required Java type
     */

    public static final String SXJE0004 = "SXJE0004";

    /**
     * SXJE0005: cannot convert xs:string to Java char unless the length is exactly one
     */

    public static final String SXJE0005 = "SXJE0005";

    /**
     * SXJE0006: cannot convert xs:string to the required Java type
     */

    public static final String SXJE0006 = "SXJE0006";


    /**
     * SXWN9001: a variable declaration with no following siblings has no effect
     */

    public static final String SXWN9001 = "SXWN9001";

    /**
     * SXWN9002: saxon:indent-spaces must be a positive integer
     */

    public static final String SXWN9002 = "SXWN9002";

    /**
     * SXWN9003: saxon:require-well-formed must be "yes" or "no"
     */

    public static final String SXWN9003 = "SXWN9003";

    /**
     * SXWN9004: saxon:next-in-chain cannot be specified dynamically
     */

    public static final String SXWN9004 = "SXWN9004";

    /**
     * SXWN9005: The 'default' attribute of saxon:collation no longer has any effect
     */

    public static final String SXWN9005 = "SXWN9005";

    /**
     * SXWN9006: No schema-location was specified, and no schema with the requested target namespace
     * is known, so the schema import was ignored
     */

    public static final String SXWN9006 = "SXWN9006";

   /**
     * SXWN9007: Invalid value for saxon:allow-all-built-in-types - must be "yes" or "no"
     */

    public static final String SXWN9007 = "SXWN9007";

    /**
     * SXWN9008: Saxon extension element not recognized because namespace not declared
     * in extension-element-prefixes
     */

    public static final String SXWN9008 = "SXWN9008";
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

