package org.orbeon.saxon.om;

/**
 * The class checks names and characters
 * against the rules of the XML 1.0 and XML Namespaces 1.0 specification
 */

public final class Name10Checker extends NameChecker {

    public static final Name10Checker theInstance = new Name10Checker();

    public static final Name10Checker getInstance() {
        return theInstance;
    }

    /**
     * Validate whether a given string constitutes a valid NCName, as defined in XML Namespaces.
     *
     * @param name the name to be tested
     * @return true if the name is a lexically-valid NCName
     */

    public boolean isValidNCName(String name) {
        return XMLChar.isValidNCName(name);
    }

    /**
     * Test whether a character is a valid XML character
     *
     * @param ch the character to be tested
     * @return true if this is a valid character in the selected version of XML
     */

    public boolean isValidChar(int ch) {
        return XMLChar.isValid(ch);
    }

    /**
     * Return the XML version supported by this NameChecker
     *
     * @return "1.0" as a string
     */

    public String getXMLVersion() {
        return "1.0";
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

