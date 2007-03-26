package org.orbeon.saxon.style;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.UserFunctionCall;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.trans.XPathException;

/**
 * A StylesheetFunctionLibrary contains functions defined by the user in a stylesheet. This library is used at
 * compile time only, as it contains references to the actual XSLFunction objects. Binding to a function in this
 * library registers the function call on a fix-up list to be notified when the actual compiled function becomes
 * available.
 */

public class StylesheetFunctionLibrary implements FunctionLibrary {

    private XSLStylesheet stylesheet;
    private boolean overriding;

    /**
     * Create a FunctionLibrary that provides access to stylesheet functions
     * @param sheet The XSLStylesheet element of the principal stylesheet module
     * @param overriding set to true if this library is to contain functions specifying override="yes",
     * or to false if it is to contain functions specifying override="no". (XSLT uses two instances
     * of this class, one for overriding functions and one for non-overriding functions.)
     */
    public StylesheetFunctionLibrary(XSLStylesheet sheet, boolean overriding) {
        this.stylesheet = sheet;
        this.overriding = overriding;
    }

    /**
     * Test whether a Saxon function with a given name and arity is available. This supports
     * the function-available() function in XSLT.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
        XSLFunction fn = stylesheet.getStylesheetFunction(fingerprint, arity);
        return (fn != null);
    }

    /**
     * Bind a function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param nameCode The namepool nameCode of the function name. The uri and local name are also
     * supplied (redundantly) to avoid fetching them from the name pool.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws org.orbeon.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(int nameCode, String uri, String local, Expression[] staticArgs)
            throws XPathException {
        int fingerprint = nameCode & 0xfffff;
        XSLFunction fn = stylesheet.getStylesheetFunction(fingerprint, staticArgs.length);
        if (fn==null) {
            return null;
        }
        if (fn.isOverriding() != overriding) {
            return null;
        }
        UserFunctionCall fc = new UserFunctionCall();
        fn.registerReference(fc);
        fc.setFunctionNameCode(nameCode);
        fc.setArguments(staticArgs);
        fc.setConfirmed(true);
        return fc;
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        return this;
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