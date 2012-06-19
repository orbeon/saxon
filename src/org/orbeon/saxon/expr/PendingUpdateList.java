package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;

import java.util.Set;

/**
 * A PendingUpdateList is created by updating expressions in XQuery Update.
 *
 * <p>The implementation of this interface is in Saxon-SA.</p>
 */
public interface PendingUpdateList {

    /**
     * Apply the pending updates
     * @param context the XPath dynamic evaluation context
     * @param validationMode the revalidation mode from the static context
     * @throws XPathException
     */

    void apply(XPathContext context, int validationMode) throws XPathException;

    /**
     * Get the root nodes of the trees that are affected by updates in the pending update list
     * @return the root nodes of affected trees, as a Set
     */

    Set getAffectedTrees();

    /**
     * Add a put() action to the pending update list
     * @param node (the first argument of put())
     * @param uri (the second argument of put())
     */

    void addPutAction(NodeInfo node, String uri) throws XPathException;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

