package org.orbeon.saxon.trans;

/**
 * This exception class is used when early (compile-time) evaluation of an expression
 * is attempted, and the expression requires knowledge of the current dateTime or implicit
 * timezone. This exception should be caught internally, and should result in evaluation
 * of the expression being deferred until run-time
 */
public class NoDynamicContextException extends XPathException {

    /**
     * Create a NoDynamicContextException
     * @param message the error message
     */

    public NoDynamicContextException(String message) {
        super("Dynamic context missing: " + message);
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

