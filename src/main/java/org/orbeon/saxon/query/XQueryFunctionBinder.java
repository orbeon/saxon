package org.orbeon.saxon.query;

import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.om.StructuredQName;

/**
 * XQueryFunctionBinder is an extension of the FunctionLibrary interface used for function libraries
 * that contain user-written XQuery functions. It provides a method that allows the XQueryFunction
 * with a given name and arity to be located.
 */

public interface XQueryFunctionBinder extends FunctionLibrary {

    /**
     * Get the function declaration corresponding to a given function name and arity
     * @param functionName the name of the function as a QName
     * @param staticArgs the expressions supplied as arguments in the function call (typically,
     * we only need to know the number of arguments)
     * @return the XQueryFunction if there is one, or null if not.
     */

    public XQueryFunction getDeclaration(StructuredQName functionName, Expression[] staticArgs);

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

