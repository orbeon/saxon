package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.sort.GroupIterator;

/**
* Implements the XSLT functions current-group() and current-grouping-key()
*/

public class CurrentGroup extends SystemFunction implements XSLTFunction {

    public static final int CURRENT_GROUP = 0;
    public static final int CURRENT_GROUPING_KEY = 1;

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the value of the expression depends on the runtime context)
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        if (operation==CURRENT_GROUPING_KEY) {
            GroupIterator gi = c.getCurrentGroupIterator();
            if (gi==null) {
                return null;
            }
            return gi.getCurrentGroupingKey();
        } else {
            return super.evaluateItem(c);
        }
    }

    /**
    * Return an iteration over the result sequence
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        if (operation==CURRENT_GROUP) {
            GroupIterator gi = c.getCurrentGroupIterator();
            if (gi==null) {
                return EmptyIterator.getInstance();
            }
            return gi.iterateCurrentGroup();
        } else {
            return super.iterate(c);
        }
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
       return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
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
