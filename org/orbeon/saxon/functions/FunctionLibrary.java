package org.orbeon.saxon.functions;

import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.expr.Expression;

import java.io.Serializable;

/**
 * A FunctionLibrary handles the binding of function calls in XPath (or XQuery) expressions.
 * There are a number of implementations of this
 * class to handle different kinds of function: system functions, constructor functions, vendor-defined
 * functions, Java extension functions, stylesheet functions, and so on. There is also an implementation
 * {@link org.orbeon.saxon.functions.FunctionLibraryList} that allows a FunctionLibrary to be constructed by combining other
 * FunctionLibrary objects.
 */

public interface FunctionLibrary extends Serializable {

    /**
     * Test whether an extension function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param fingerprint The namepool fingerprint of the function name. This must match the
     * uri and localName; the information is provided redundantly to avoid repeated lookups in the name pool.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity);

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param nameCode The namepool nameCode of the function name. The uri and local name are also
     * supplied (redundantly) to avoid fetching them from the name pool.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @return An object representing the function to be called, if one is found;
     * null if no function was found matching the required name and arity.
     * @throws org.orbeon.saxon.xpath.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function.
     */

    public Expression bind(int nameCode, String uri, String local, Expression[] staticArgs)
            throws XPathException;

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