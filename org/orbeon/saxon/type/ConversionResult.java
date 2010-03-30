package org.orbeon.saxon.type;

import org.orbeon.saxon.value.AtomicValue;

/**
 * This is a marker interface used as the result methods that convert or cast values from one type
 * to another. It is implemented by AtomicValue, which indicates a successful conversion, and by
 * ValidationFailure, which indicates an unsuccessful conversion. An unsuccessful conversion does not
 * throw an exception because exceptions are expensive and should not be used on success paths. For example
 * when validating a union, conversion failures are to be expected.
 */
public interface ConversionResult {

    /**
     * Calling this method on a ConversionResult returns the AtomicValue that results
     * from the conversion if the conversion was successful, and throws a ValidationException
     * explaining the conversion error otherwise.
     *
     * <p>Use this method if you are calling a conversion method that returns a ConversionResult,
     * and if you want to throw an exception if the conversion fails.</p>
     *
     * @return the atomic value that results from the conversion if the conversion was successful
     * @throws ValidationException if the conversion was not successful
     */

    public AtomicValue asAtomic() throws ValidationException;

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

