package net.sf.saxon.om;

import java.util.Iterator;

/**
 * Abstract class that supports lookup of a lexical QName to get the expanded QName.
 * This implements the JAXP NamespaceContext interface with some Saxon-specific methods,
 * which must be supplied in a concrete implementation.
 */

public interface NamespaceResolver {

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     * @param prefix the namespace prefix. May be the zero-length string, indicating
     * that there is no prefix. This indicates either the default namespace or the
     * null namespace, depending on the value of useDefault.
     * @param useDefault true if the default namespace is to be used when the
     * prefix is "". If false, the method returns "" when the prefix is "".
     * @return the uri for the namespace, or null if the prefix is not in scope.
     * The "null namespace" is represented by the pseudo-URI "".
    */

    public abstract String getURIForPrefix(String prefix, boolean useDefault);

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public abstract Iterator iteratePrefixes();

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