package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.XPathException;


public class Concat extends SystemFunction {

    /**
    * Get the required type of the nth argument
    */

    protected SequenceType getRequiredType(int arg) {
        return getDetails().argumentTypes[0];
        // concat() is a special case
    }

    /**
    * Evaluate the function in a string context
    */

    public String evaluateAsString(XPathContext c) throws XPathException {
        int numArgs = argument.length;

        StringBuffer sb = new StringBuffer();
        for (int i=0; i<numArgs; i++) {
            AtomicValue val = (AtomicValue)argument[i].evaluateItem(c);
            if (val!=null) {
                sb.append(val.getStringValue());
            }
        }

        return sb.toString();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return new StringValue(evaluateAsString(c));
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
