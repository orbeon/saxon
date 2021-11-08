package org.orbeon.saxon.s9api;

/**
 * An unchecked exception thrown by the Saxon API. Unchecked exceptions are used only when errors occur in a method
 * for which the interface specification defines no checked exception, for example {@link java.util.Iterator#next()}.
 * The exception always wraps some underlying exception, which can be retrieved using {@link #getCause()}
 */
public class SaxonApiUncheckedException extends RuntimeException {

    /**
     * Create an unchecked exception
     * @param err the underlying cause
     */

    public SaxonApiUncheckedException(Throwable err) {
        super(err);
    }


    /**
     * Returns the detail message string of this throwable.
     *
     * @return the detail message string of this <tt>Throwable</tt> instance
     *         (which may be <tt>null</tt>).
     */
    public String getMessage() {
        return getCause().getMessage(); 
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

