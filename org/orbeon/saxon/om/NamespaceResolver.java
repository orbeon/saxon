package org.orbeon.saxon.om;

import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.DynamicError;

import java.util.Iterator;

/**
 * Interface that supports lookup of a lexical QName to get the expanded QName.
 */

public interface NamespaceResolver {

    /**
    * Get the namespace URI corresponding to a given prefix. Return null
    * if the prefix is not in scope.
    * @param prefix the namespace prefix
    * @param useDefault true if the default namespace is to be used when the
    * prefix is ""
    * @return the uri for the namespace, or null if the prefix is not in scope
    */

    public String getURIForPrefix(String prefix, boolean useDefault);

    /**
    * Use this NamespaceContext to resolve a lexical QName
    * @param qname the lexical QName; this must have already been lexically validated
    * @param useDefault true if the default namespace is to be used to resolve an unprefixed QName
    * @param pool the NamePool to be used
    * @return the integer fingerprint that uniquely identifies this name
     * @throws org.orbeon.saxon.xpath.DynamicError if the string is not a valid lexical QName or
     * if the namespace prefix has not been declared
    */

    public int getFingerprint(String qname, boolean useDefault, NamePool pool)
    throws DynamicError;

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes();
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//