package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.XPathException;

/**
 * Implement the XPath string-length() function
 */

public class StringLength extends SystemFunction {

    private boolean shortcut = false;
                                // if this is set we return 0 for a zero length string,
                                // 1 for any other. Used by the optimizer.
    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
    */

     public Expression simplify(StaticContext env) throws XPathException {
        useContextItemAsDefault();
        return simplifyArguments(env);
    }

    /**
    * setShortCut() - used by optimizer when we only need to know if the length is non-zero
    */

    public void setShortcut() {
        shortcut = true;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) {
            sv = StringValue.EMPTY_STRING;
        }
        String s = sv.getStringValue();

        if (shortcut) {
            return new IntegerValue((s.length()>0 ? 1 : 0));
        } else {
            return new IntegerValue(StringValue.getLength(s));
        }
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
