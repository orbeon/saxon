package net.sf.saxon.value;

import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ValidationException;

/**
 * An ErrorValue is a value that has failed validation. It is used by methods that can either
 * return a value or an error.
 */

public class ErrorValue extends ObjectValue {

    public ErrorValue(ValidationException err) {
        super(err);
    }

    public ValidationException getException() {
        return (ValidationException)getObject();
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
