package org.orbeon.saxon.expr;

/**
 *  The expression is generated when compiling the current() function in XSLT. It differs from
 *  the ContextItemExpression "." only in the error code that is returned when there is no context item.
 */

public class CurrentItemExpression extends ContextItemExpression {

   protected String getErrorCodeForUndefinedContext() {
        return "XTDE1360";
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

