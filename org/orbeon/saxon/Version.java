package org.orbeon.saxon;

/**
 * The Version class holds the SAXON version information.
 */

public final class Version {

    private static final int[] STRUCTURED_VERSION = {9,1,0,8};
    private static final String VERSION = "9.1.0.8";
    private static final String BUILD = "102723"; //mmddhh
    private static final String RELEASE_DATE = "2009-10-27";

    private Version() {
        // class is never instantiated
    }

    /**
     * Return the name of this product. Supports the XSLT 2.0 system property xsl:product-name
     * @return the string "SAXON"
     */


    public static String getProductName() {
        return "SAXON";
    }

   /**
     * Get the version number of the schema-aware version of the product
     * @return the version number of this version of Saxon, as a string
     */

   public static String getSchemaAwareProductVersion() {
        return "SA " + getProductVersion();
    }

    /**
     * Get the user-visible version number of this version of the product
     * @return the version number of this version of Saxon, as a string: for example "9.0.1"
     */

    public static String getProductVersion() {
        return VERSION;
    }

    /**
     * Get the four components of the structured version number. This is used in the .NET product
     * to locate an assembly in the dynamic assembly cache: the assumption is that the third
     * and fourth components represent implementation changes rather than interface changes
     * @return  the four components of the version number, as an array: for example {9, 0, 1, 1}
     */ 

    public static int[] getStructuredVersionNumber() {
        return STRUCTURED_VERSION;
    }

    /**
     * Get the issue date of this version of the product
     * @return the release date, as an ISO 8601 string
     */

    public static String getReleaseDate() {
        return RELEASE_DATE;
    }

    /**
     * Get the version of the XSLT specification that this product supports
     * @return the string 2.0
     */

    public static String getXSLVersionString() {
        return "2.0";
    }

    /**
     * Get a message used to identify this product when a transformation is run using the -t option
     * @return A string containing both the product name and the product
     *     version
     */

    public static String getProductTitle() {
        return getProductName() + ' ' + getProductVersion() + " from Saxonica";
    }

    /**
     * Return a web site address containing information about the product. Supports the XSLT system property xsl:vendor-url
     * @return the string "http://saxon.sf.net/"
     */

    public static String getWebSiteAddress() {
        return "http://www.saxonica.com/";
    }

    /**
     * Invoking org.orbeon.saxon.Version from the command line outputs the build number
     * @param args not used
     */
    public static void main(String[] args) {
        System.err.println(getProductTitle() + " (build " + BUILD + ')');
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
