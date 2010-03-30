package org.orbeon.saxon.query;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;

/**
 * An UpdateAgent is a callback class that is called to handle a document after it has been updated.
 * Typically the UpdateAgent might take responsibility for writing the updated document back to
 * persistent storage.
 */
public interface UpdateAgent {

    /**
     * Handle an updated document.
     * This method is called by {@link XQueryExpression#runUpdate(DynamicQueryContext, UpdateAgent)}
     * once for each document (or more generally, for the root of each tree) that has been modified
     * by the update query.
     * @param node the root of the tree that has been updated
     * @param controller the Controller that was used for executing the query
     * @throws XPathException if the callback code cannot handle the updated document
     */

    public void update(NodeInfo node, Controller controller) throws XPathException;
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

