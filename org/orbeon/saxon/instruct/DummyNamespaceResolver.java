package net.sf.saxon.instruct;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.NamespaceResolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    * Get the namespace URI corresponding to a given prefix.
    * @param prefix the namespace prefix
    * @param useDefault true if the default namespace is to be used when the
    * prefix is ""
    * @return the uri for the namespace, or null if the prefix is not in scope
    */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if ("".equals(prefix)) {
            return "";
        } else if ("xml".equals(prefix)) {
            return NamespaceConstant.XML;
        } else {
            // this is a dummy namespace resolver, we don't actually know the URI
            return "";
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        List list = new ArrayList(2);
        list.add("");
        list.add("xml");
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
// Contributor(s): none.
//
