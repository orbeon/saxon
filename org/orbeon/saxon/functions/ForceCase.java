package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;


/**
* This class implements the upper-case() and lower-case() functions
*/

public class ForceCase extends SystemFunction {

    public static final int UPPERCASE = 0;
    public static final int LOWERCASE = 1;

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return StringValue.EMPTY_STRING;
        }

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
