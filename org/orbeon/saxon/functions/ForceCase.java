package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.XPathException;


/**
* This class implements the upper-case() and lower-case() functions
*/

public class ForceCase extends SystemFunction {

    public final static int UPPERCASE = 0;
    public final static int LOWERCASE = 1;

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) return null;

        switch(operation) {
            case UPPERCASE:
                return new StringValue(sv.getStringValue().toUpperCase());
            case LOWERCASE:
                return new StringValue(sv.getStringValue().toLowerCase());
            default:
                throw new UnsupportedOperationException("Unknown function");
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
