package org.orbeon.saxon.expr;

/**
 *
 */
public interface CodeGeneratorService {

    /**
     * Get the name of the Java variable currently bound to the dynamic XPathContext object
     *
     * @return the Java variable name
     */

    public String getContextVariableName();

    /**
     * Generate a Java cast unless it is known to be unnecessary.
     *
     * @param variable the name of the variable that possibly needs to be cast
     * @param target   the required type for the expression where the variable is being used
     * @return either the variable name on its own, if no cast is required, or a string in the form
     *         "((class)variable)" if casting is needed.
     */

    public String cast(String variable, Class target);    
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

