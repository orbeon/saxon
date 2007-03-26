package org.orbeon.saxon.om;

import org.orbeon.saxon.type.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A NamespaceResolver that resolves namespace prefixes by reference to a node in a document for which
 * those namespaces are in-scope.
 */
public class InscopeNamespaceResolver implements NamespaceResolver {

    private NodeInfo node;

    public InscopeNamespaceResolver(NodeInfo node) {
        if (node.getNodeKind() == Type.ELEMENT) {
            this.node = node;
        } else {
            this.node = node.getParent();
        }
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     *
     * @param prefix     the namespace prefix
     * @param useDefault true if the default namespace is to be used when the
     *                   prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope
     * Return "" for the no-namespace.
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if ("".equals(prefix) && !useDefault) {
            return "";
        }
        AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
        while (true) {
            NodeInfo node = (NodeInfo)iter.next();
            if (node == null) {
                break;
            }
            if (node.getLocalPart().equals(prefix)) {
                return node.getStringValue();
            }
        }
        if ("".equals(prefix)) {
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
        List list = new ArrayList(16);
        AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
        while (true) {
            NodeInfo node = (NodeInfo)iter.next();
            if (node == null) {
                break;
            }
            list.add(node.getLocalPart());
        }
        return list.iterator();
    }

    /**
     * Get the node on which this namespace resolver is based
     */

    public NodeInfo getNode() {
        return node;
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