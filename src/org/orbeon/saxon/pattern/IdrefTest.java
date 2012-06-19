package org.orbeon.saxon.pattern;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;

import java.io.Serializable;

/**
 * IdrefTest is a test that cannot be represented directly in XPath or
 * XSLT patterns, but which is used internally for matching IDREF nodes: it tests
 * whether the node has the is-idref property
  *
  * @author Michael H. Kay
  */

public class IdrefTest implements PatternFinder, Serializable {

    private static IdrefTest THE_INSTANCE = new IdrefTest();

    /**
     * Get the singleton instance of this class
     */

    public static IdrefTest getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Create a IdrefTest
     */

	private IdrefTest() {}

    /**
      * Select nodes in a document using this PatternFinder.
      * @param doc the document node at the root of a tree
      * @param context the dynamic evaluation context
      * @return an iterator over the selected nodes in the document.
      */

     public SequenceIterator selectNodes(DocumentInfo doc, final XPathContext context) throws XPathException {

         AxisIterator allElements = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
         MappingFunction atts = new MappingFunction() {
             public SequenceIterator map(Item item) {
                 return new PrependIterator((NodeInfo)item, ((NodeInfo)item).iterateAxis(Axis.ATTRIBUTE));
             }
         };
         SequenceIterator allAttributes = new MappingIterator(allElements, atts);
         ItemMappingFunction test = new ItemMappingFunction() {
             public Item map(Item item) {
                 if ((matches((NodeInfo)item))) {
                     return item;
                 } else {
                     return null;
                 }
             }
         };
         return new ItemMappingIterator(allAttributes, test);

     }

    /**
     * Test whether this node test is satisfied by a given node.
     * @param node the node to be matched
     */

    private boolean matches(NodeInfo node) {
        return node.isIdref();
    }


    public String toString() {
        return "is-idref()";
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
