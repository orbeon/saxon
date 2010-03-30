package org.orbeon.saxon.om;

import org.orbeon.saxon.charcode.XMLCharacterData;

/**
 * The class checks names against the rules of the XML 1.1 and XML Namespaces 1.1 specification
 */

public final class Name11Checker extends NameChecker {

    public static final Name11Checker theInstance = new Name11Checker();

    /**
     * Get the singular instance of this class
     * @return the singular instance of this class
     */

    public static Name11Checker getInstance() {
        return theInstance;
    }

    /**
     * Test whether a character is a valid XML character
     *
     * @param ch the character to be tested
     * @return true if this is a valid character in the selected version of XML
     */

    public boolean isValidChar(int ch) {
        //return XMLChar.isValid(ch);
        return XMLCharacterData.isValid11(ch);
    }


    /**
     * Test whether a character can appear in an NCName
     *
     * @param ch the character to be tested
     * @return true if this is a valid character in an NCName the selected version of XML
     */

    public boolean isNCNameChar(int ch) {
        return XMLCharacterData.isNCName11(ch);
    }

    /**
     * Test whether a character can appear at the start of an NCName
     *
     * @param ch the character to be tested
     * @return true if this is a valid character at the start of an NCName the selected version of XML
     */

    public boolean isNCNameStartChar(int ch) {
        return XMLCharacterData.isNCNameStart11(ch);
    }


    /**
     * Return the XML version supported by this NameChecker
     *
     * @return "1.1" as a string
     */

    public String getXMLVersion() {
        return "1.1";
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

