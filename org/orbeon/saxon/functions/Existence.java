package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Optimizer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.LookaheadIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.BooleanValue;

/** Implement the exists() and empty() functions **/

public class Existence extends SystemFunction {

    public static final int EXISTS = 0;
    public static final int EMPTY = 1;

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        Optimizer opt = env.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
    }

    /**
    * Evaluate the function in a boolean context
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        SequenceIterator iter = argument[0].iterate(c);
        if ((iter.getProperties() & SequenceIterator.LOOKAHEAD) != 0) {
            switch (operation) {
                case EXISTS: return ((LookaheadIterator)iter).hasNext();
                case EMPTY: return !((LookaheadIterator)iter).hasNext();
            }
        } else {
            switch (operation) {
                case EXISTS: return iter.next() != null;
                case EMPTY: return iter.next() == null;
            }
        }    
        return false;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(c));
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
