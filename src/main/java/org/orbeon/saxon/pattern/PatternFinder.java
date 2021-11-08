package org.orbeon.saxon.pattern;

import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.expr.XPathContext;

import java.io.Serializable;

/**
 * This interface enables a client to find all nodes in a document that match a particular pattern.
 * In fact, it allows any subset of nodes in a document to be located. It is used specifically by the
 * internal implementation of keys. In XSLT, the criterion for including nodes in a key is that they
 * match an XSLT pattern. Internally, however, keys are used for a wider range of purposes, and the
 * nodes indexed by the key are defined by a PatternFinder
 */

public interface PatternFinder extends Serializable {

    /**
     * Select nodes in a document using this PatternFinder.
     * @param doc the document node at the root of a tree
     * @param context the dynamic evaluation context
     * @return an iterator over the selected nodes in the document.
     */

    public SequenceIterator selectNodes(DocumentInfo doc, XPathContext context) throws XPathException;

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

