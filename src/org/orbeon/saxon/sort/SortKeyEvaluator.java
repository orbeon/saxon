package org.orbeon.saxon.sort;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;

/**
 * Callback interface used to evaluate sort keys. An instance of this class is passed to the
 * SortedIterator, and is used whenever a sort key value needs to be computed.
 */

public interface SortKeyEvaluator {

    /**
     * Evaluate the n'th sort key
     */

    public Item evaluateSortKey(int n, XPathContext context) throws XPathException;
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

