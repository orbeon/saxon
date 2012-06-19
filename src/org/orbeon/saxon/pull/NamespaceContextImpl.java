package org.orbeon.saxon.pull;

import org.orbeon.saxon.om.NamespaceResolver;

import javax.xml.namespace.NamespaceContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class bridges between the JAXP 1.3 NamespaceContext interface and Saxon's
 * equivalent NamespaceResolver interface. It allows any implementation of the Saxon
 * NamespaceResolver to be wrapped as a JAXP NamespaceContext.
 */

public class NamespaceContextImpl implements NamespaceContext, NamespaceResolver {

    NamespaceResolver resolver;

    /**
     * Constructor: wrap a Saxon NamespaceResolver as a JAXP NamespaceContext
     * @param resolver the Saxon NamespaceResolver
     */

    public NamespaceContextImpl(NamespaceResolver resolver) {
        this.resolver = resolver;
    }

    /**
    * Get the namespace URI corresponding to a given prefix. Return null
    * if the prefix is not in scope.
    * @param prefix the namespace prefix
    * @param useDefault true if the default namespace is to be used when the
    * prefix is ""
    * @return the uri for the namespace, or null if the prefix is not in scope
    */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        return resolver.getURIForPrefix(prefix, useDefault);
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        return resolver.iteratePrefixes();
    }

    /**
     * Implement the JAXP getNamespaceURI() method in terms of the Saxon-specific methods
     * @param prefix a namespace prefix
     * @return the corresponding URI, if the prefix is bound, or "" otherwise
     */

    public String getNamespaceURI(String prefix) {
        if (prefix.equals("xmlns")) {
            return "http://www.w3.org/2000/xmlns/";
        }
        return resolver.getURIForPrefix(prefix, true);
    }

    /**
     * Get the prefix bound to a particular namespace URI, if there is one, or null if not (JAXP method)
     * @param uri the namespace URI
     * @return the prefix bound to the URI if there is one, or null if not
     */

    public String getPrefix(String uri) {
        Iterator prefixes = iteratePrefixes();
        while (prefixes.hasNext()) {
            String p = (String)prefixes.next();
            String u = resolver.getURIForPrefix(p, true);
            if (u.equals(uri)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Get all the prefixes mapped to a given namespace URI (JAXP method)
     * @param uri the namespace URI
     * @return an iterator over all the prefixes bound to this namespace URI
     */
    public Iterator getPrefixes(String uri) {
        List list = new ArrayList(4);
        Iterator prefixes = iteratePrefixes();
        while (prefixes.hasNext()) {
            String p = (String)prefixes.next();
            String u = resolver.getURIForPrefix(p, true);
            if (u.equals(uri)) {
                list.add(p);
            }
        }
        return list.iterator();
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//