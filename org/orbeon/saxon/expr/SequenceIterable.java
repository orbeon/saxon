package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

import java.io.Serializable;

/**
 * This interface is a simple subset of the Expression interface, that provides a single method to
 * evaluate the result of an expression as a sequence
 */
public interface SequenceIterable extends Serializable {

    /**
     * Return an iterator over the results of evaluating an expression
     * @param context the dynamic evaluation context
     * @return an iterator over the items delivered by the expression
     */ 

    public SequenceIterator iterate(XPathContext context) throws XPathException;
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

