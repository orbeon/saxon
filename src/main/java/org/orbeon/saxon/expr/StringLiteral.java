package org.orbeon.saxon.expr;

import org.orbeon.saxon.value.StringValue;

/**
 * Subclass of Literal used specifically for string literals, as this is a common case
 */
public class StringLiteral extends Literal {

    /**
     * Create a StringLiteral that wraps a StringValue
     * @param value the StringValue
     */

    public StringLiteral(StringValue value) {
        super(value);
    }

    /**
     * Create a StringLiteral that wraps any CharSequence (including, of course, a String)
     * @param value the CharSequence to be wrapped
     */

    public StringLiteral(CharSequence value) {
        super(StringValue.makeStringValue(value));
    }

    /**
     * Get the string represented by this StringLiteral
     * @return the underlying string
     */

    public String getStringValue() {
        //noinspection RedundantCast
        return ((StringValue)getValue()).getStringValue();
    }

    public Expression copy() {
        return new StringLiteral((StringValue)getValue());
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

