package net.sf.saxon.om;

import net.sf.saxon.Err;

/**
 * This class exists to contain some static methods
 * for validating the syntax of names.
 *
 * @author Michael H. Kay
 */

public abstract class Name {


    /**
     * Validate whether a given string constitutes a valid QName, as defined in XML Namespaces.
     * Note that this does not test whether the prefix is actually declared.
     *
     * @param name the name to be tested
     * @return true if the name is a lexically-valid QName
     */

    public final static boolean isQName(String name) {
        int colon = name.indexOf(':');
        if (colon<0) return XMLChar.isValidNCName(name);
        if (colon==0 || colon==name.length()-1) return false;
        if (!XMLChar.isValidNCName(name.substring(0, colon))) return false;
        if (!XMLChar.isValidNCName(name.substring(colon+1))) return false;
        return true;
    }

	/**
	 * Extract the prefix from a QName. Note, the QName is assumed to be valid.
	 *
	 * @param qname The lexical QName whose prefix is required
	 * @return the prefix, that is the part before the colon. Returns an empty
	 *     string if there is no prefix
	 */

	public final static String getPrefix(String qname) {
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
	 * @param qname the lexical QName whose parts are required
	 * @return an array of two strings, the prefix and the local name. The first
	 *      item is a zero-length string if there is no prefix.
	 */

	public final static String[] getQNameParts(CharSequence qname) throws QNameException {
	    String[] parts = new String[2];
        int colon = -1;
        int len = qname.length();
        for (int i=0; i<len; i++) {
            if (qname.charAt(i)==':') {
                colon = i;
            }
        }
        if (colon<0) {
            parts[0] = "";
            parts[1] = qname.toString();
            if (!XMLChar.isValidNCName(qname)) {
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
            if (!XMLChar.isValidNCName(parts[1])) {
                throw new QNameException("Invalid QName local part " + Err.wrap(parts[1]));
            }
        }
        return parts;
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
