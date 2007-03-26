package org.orbeon.saxon.om;

import org.orbeon.saxon.Err;

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
        if (colon<0) return isValidNCName(name);
        if (colon==0 || colon==name.length()-1) return false;
        if (!isValidNCName(name.substring(0, colon))) return false;
        if (!isValidNCName(name.substring(colon+1))) return false;
        return true;
    }

    /**
     * Validate whether a given string constitutes a valid NCName, as defined in XML Namespaces.
     *
     * @param name the name to be tested
     * @return true if the name is a lexically-valid QName
     */

    public abstract boolean isValidNCName(String name);

    /**
     * Extract the prefix from a QName. Note, the QName is assumed to be valid.
     *
     * @param qname The lexical QName whose prefix is required
     * @return the prefix, that is the part before the colon. Returns an empty
     *     string if there is no prefix
     */

    public static final String getPrefix(String qname) {
        int colon = qname.indexOf(':');
        if (colon<0) {
            return "";
        }
        return qname.substring(0, colon);
    }

    /**
     * Validate a QName, and return the prefix and local name.
     *
     * @exception QNameException if not a valid QName.
     * @param qname the lexical QName whose parts are required. Note that leading and trailing
     * whitespace is not permitted
     * @return an array of two strings, the prefix and the local name. The first
     *      item is a zero-length string if there is no prefix.
     */

    public final String[] getQNameParts(CharSequence qname) throws QNameException {
        String[] parts = new String[2];
        int colon = -1;
        int len = qname.length();
        for (int i=0; i<len; i++) {
            if (qname.charAt(i)==':') {
                colon = i;
                break;
            }
        }
        if (colon<0) {
            parts[0] = "";
            parts[1] = qname.toString();
            if (!isValidNCName(parts[1])) {
                throw new QNameException("Invalid QName " + Err.wrap(qname));
            }
        } else {
            if (colon==0) {
                throw new QNameException("QName cannot start with colon: " + Err.wrap(qname));
            }
            if (colon==len-1) {
                throw new QNameException("QName cannot end with colon: " + Err.wrap(qname));
            }
            parts[0] = qname.subSequence(0, colon).toString();
            parts[1] = qname.subSequence(colon+1, len).toString();
            // don't validate the prefix. If it isn't valid, then we'll get an error when we try to
            // find the namespace declaration
//            if (!XMLChar.isValidNCName(parts[0])) {
//                throw new QNameException("Invalid QName prefix " + Err.wrap(parts[0]));
//            }
            if (!isValidNCName(parts[1])) {
                throw new QNameException("Invalid QName local part " + Err.wrap(parts[1]));
            }
        }
        return parts;
    }

    /**
     * Test whether a character is a valid XML character
     * @param ch the character to be tested
     * @return true if this is a valid character in the selected version of XML
     */

    public abstract boolean isValidChar(int ch);

    /**
     * Return the XML version supported by this NameChecker
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
