package org.orbeon.saxon.instruct;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceResolver;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

/**
  * A dummy namespace resolver used when validating QName-valued attributes written to
  * the result tree. The namespace node might be created after the initial validation
  * of the attribute, so in the first round of validation we only check the lexical form
  * of the value, and we defer prefix checks until later.
  */

public final class DummyNamespaceResolver implements Serializable, NamespaceResolver {

    private static DummyNamespaceResolver theInstance = new DummyNamespaceResolver();

    /**
     * Return the singular instance of this class
     * @return the singular instance
     */

    public static DummyNamespaceResolver getInstance() {
        return theInstance;
    }

    private DummyNamespaceResolver() {};


    /**
    * Get the namespace URI corresponding to a given prefix. This implementation
     * returns a dummy URI
    * @param prefix the namespace prefix
    * @param useDefault true if the default namespace is to be used when the
    * prefix is ""
    * @return the uri for the namespace, or null if the prefix is not in scope
    */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        return "saxon dummy namespace URI";
    }

    /**
    * Use this NamespaceContext to resolve a lexical QName
    * @param qname the lexical QName; this must have already been lexically validated
    * @param useDefault true if the default namespace is to be used to resolve an unprefixed QName
    * @param pool the NamePool to be used
    * @return the integer fingerprint that uniquely identifies this name
    */

    public int getFingerprint(String qname, boolean useDefault, NamePool pool) {
        return -1;
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        return Collections.EMPTY_LIST.iterator();
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
// Contributor(s): none.
//
