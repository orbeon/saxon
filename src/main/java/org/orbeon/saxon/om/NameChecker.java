package org.orbeon.saxon.om;

import org.orbeon.saxon.trans.Err;

import org.orbeon.saxon.trans.XPathException;

/**
 * A NameChecker performs validation and analysis of XML names. There are two implementations
 * of this interface, one for XML 1.0 names and one for XML 1.1 names. The class also handles
 * validation of characters against the XML 1.0 or XML 1.1 rules.
 */

public abstract class NameChecker {

    /**
     * Validate whether a given string constitutes a valid QName, as defined in XML Namespaces.
     * Note that this does not test whether the prefix is actually declared.
     *
     * @param name the name to be tested
     * @return true if the name is a lexically-valid QName
     */

    public final boolean isQName(String name) {
        int colon = name.indexOf(':');
        if (colon < 0) {
            return isValidNCName(name);
        }
        return colon != 0 &&
                colon != name.length() - 1 &&
                isValidNCName(name.substring(0, colon)) &&
                isValidNCName(name.substring(colon + 1));
    }

    /**
     * Validate whether a given string constitutes a valid NCName, as defined in XML Namespaces.
     *
     * @param name the name to be tested
     * @return true if the name is a lexically-valid QName
     */

    //public abstract boolean isValidNCName(CharSequence name);

    /**
     * Extract the prefix from a QName. Note, the QName is assumed to be valid.
     *
     * @param qname The lexical QName whose prefix is required
     * @return the prefix, that is the part before the colon. Returns an empty
     *         string if there is no prefix
     */

    public static String getPrefix(String qname) {
        int colon = qname.indexOf(':');
        if (colon < 0) {
            return "";
        }
        return qname.substring(0, colon);
    }

    /**
     * Validate a QName, and return the prefix and local name. The local name is checked
     * to ensure it is a valid NCName. The prefix is not checked, on the theory that the caller
     * will look up the prefix to find a URI, and if the prefix is invalid, then no URI will
     * be found.
     *
     * @param qname the lexical QName whose parts are required. Note that leading and trailing
     *              whitespace is not permitted
     * @return an array of two strings, the prefix and the local name. The first
     *         item is a zero-length string if there is no prefix.
     * @throws QNameException if not a valid QName.
     */

    public final String[] getQNameParts(CharSequence qname) throws QNameException {
        String[] parts = new String[2];
        int colon = -1;
        int len = qname.length();
        for (int i = 0; i < len; i++) {
            if (qname.charAt(i) == ':') {
                colon = i;
                break;
            }
        }
        if (colon < 0) {
            parts[0] = "";
            parts[1] = qname.toString();
            if (!isValidNCName(parts[1])) {
                throw new QNameException("Invalid QName " + Err.wrap(qname));
            }
        } else {
            if (colon == 0) {
                throw new QNameException("QName cannot start with colon: " + Err.wrap(qname));
            }
            if (colon == len - 1) {
                throw new QNameException("QName cannot end with colon: " + Err.wrap(qname));
            }
            parts[0] = qname.subSequence(0, colon).toString();
            parts[1] = qname.subSequence(colon + 1, len).toString();

            if (!isValidNCName(parts[1])) {
                throw new QNameException("Invalid QName local part " + Err.wrap(parts[1]));
            }
        }
        return parts;
    }

    /**
     * Validate a QName, and return the prefix and local name. Both parts are checked
     * to ensure they are valid NCNames.
     * <p/>
     * <p><i>Used from compiled code</i></p>
     *
     * @param qname the lexical QName whose parts are required. Note that leading and trailing
     *              whitespace is not permitted
     * @return an array of two strings, the prefix and the local name. The first
     *         item is a zero-length string if there is no prefix.
     * @throws XPathException if not a valid QName.
     */

    public final String[] checkQNameParts(CharSequence qname) throws XPathException {
        try {
            String[] parts = getQNameParts(qname);
            if (parts[0].length() > 0 && !isValidNCName(parts[0])) {
                throw new XPathException("Invalid QName prefix " + Err.wrap(parts[0]));
            }
            return parts;
        } catch (QNameException e) {
            XPathException err = new XPathException(e.getMessage());
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    /**
     * Validate whether a given string constitutes a valid NCName, as defined in XML Namespaces.
     *
     * @param ncName the name to be tested
     * @return true if the name is a lexically-valid QName
     */

    public final boolean isValidNCName(CharSequence ncName) {
        if (ncName.length() == 0) {
            return false;
        }
        char ch = ncName.charAt(0);
        if (!isNCNameStartChar(ch)) {
            return false;
        }
        for (int i = 1; i < ncName.length(); i++) {
            ch = ncName.charAt(i);
            if (!isNCNameChar(ch)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check to see if a string is a valid Nmtoken according to [7]
     * in the XML 1.0 Recommendation
     *
     * @param nmtoken string to check
     * @return true if nmtoken is a valid Nmtoken
     */

    public final boolean isValidNmtoken(CharSequence nmtoken) {
        if (nmtoken.length() == 0) {
            return false;
        }
        for (int i = 0; i < nmtoken.length(); i++) {
            char ch = nmtoken.charAt(i);
            if (ch != ':' && !isNCNameChar(ch)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Test whether a character is a valid XML character
     *
     * @param ch the character to be tested
     * @return true if this is a valid character in the selected version of XML
     */

    public abstract boolean isValidChar(int ch);

    /**
     * Test whether a character can appear in an NCName
     *
     * @param ch the character to be tested
     * @return true if this is a valid character in an NCName the selected version of XML
     */

    public abstract boolean isNCNameChar(int ch);

    /**
     * Test whether a character can appear at the start of an NCName
     *
     * @param ch the character to be tested
     * @return true if this is a valid character at the start of an NCName the selected version of XML
     */

    public abstract boolean isNCNameStartChar(int ch);

    /**
     * Return the XML version supported by this NameChecker
     *
     * @return "1.0" or "1.1" as a string
     */

    public abstract String getXMLVersion();
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
