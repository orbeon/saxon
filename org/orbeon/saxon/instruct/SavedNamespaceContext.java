package net.sf.saxon.instruct;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.NamespaceResolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
  * An object representing a list of Namespaces. Used when the namespace
  * controller in the stylesheet needs to be kept for use at run-time. The list of namespaces
  * is maintained in the form of numeric prefix/uri codes, which are only meaningful
  * in the controller of a name pool; however, in order to save space, the NamespaceContext
  * object does not keep a reference to the namepool, it requires this to be supplied
  * by the caller.
  */

public final class SavedNamespaceContext implements Serializable, NamespaceResolver {

    // TODO: static context can't vary within an XPath expression. Therefore, save the
    // NamespaceContext at the outermost expression level if any subexpression needs it.
    // (Or put a namespace context expression on the expression tree, which has no effect
    // at run-time, but is available to descendant expressions).

    private int[] namespaceCodes;
    private NamePool namePool;

    /**
    * Create a NamespaceContext object
    * @param nscodes an array of namespace codes. Each namespace code is an integer
    * in which the first 16 bits represent the prefix (zero if it's the default namespace)
    * and the next 16 bits represent the uri. These are codes held in the NamePool. The
    * list will be searched from the "high" end.
    */

    public SavedNamespaceContext(int[] nscodes, NamePool pool) {
        namespaceCodes = nscodes;
        namePool = pool;
    }

    /**
    * Get the list of in-scope namespaces held in this NamespaceContext
    * @return the list of namespaces
    */

    public int[] getNamespaceCodes() {
        return namespaceCodes;
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

        if (prefix.equals("") && !useDefault) {
            return "";
        }

        if (prefix.equals("xml")) {
            return NamespaceConstant.XML;
        }

        for (int i=namespaceCodes.length-1; i>=0; i--) {
            if (namePool.getPrefixFromNamespaceCode(namespaceCodes[i]).equals(prefix)) {
                return namePool.getURIFromNamespaceCode(namespaceCodes[i]);
            }
        }

        if (prefix.equals("") && useDefault) {
            // use the "default default namespace" - namely ""
            return "";
        } else {
            return null;
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        ArrayList prefixes = new ArrayList(namespaceCodes.length);
        for (int i=0; i<namespaceCodes.length; i++) {
            prefixes.add(namePool.getPrefixFromNamespaceCode(namespaceCodes[i]));
        }
        return prefixes.iterator();
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
