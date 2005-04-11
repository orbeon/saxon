package org.orbeon.saxon.functions;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.UserFunctionCall;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.trans.IndependentContext;
import org.orbeon.saxon.trans.XPathException;

import java.util.HashMap;

/**
 * An ExecutableFunctionLibrary is a function library that contains definitions of functions for use at
 * run-time. Normally functions are bound at compile-time; however there are various situations in which
 * the information is needed dynamically, for example (a) to support the XSLT function-available() call
 * (in the pathological case where the argument is not known statically), (b) to allow functions to be
 * called from saxon:evaluate(), (c) to allow functions to be called from a debugging breakpoint.
 *
 * The objects actually held in the ExecutableFunctionLibrary are UserFunctionCall objects that have been
 * prepared at compile time. These are function calls that do full dynamic type checking: that is, they
 * are prepared on the basis that the static types of the arguments are all "item()*", meaning that full
 * type checking is needed at run-time.
 */

public class ExecutableFunctionLibrary implements FunctionLibrary {

    private Configuration config;
    private HashMap functions = new HashMap(20);
    // The key of the hash table is a Long containing the arity in the top half, and the
    // fingerprint of the function name in the bottom half. The value is a UserFunction object.

    public ExecutableFunctionLibrary(Configuration config) {
        this.config = config;
    }

    /**
     * Register a function with the function library
     */

    public void addFunction(UserFunction fn) {
        long key = ((long)fn.getNumberOfArguments())<<32 | (fn.getFunctionNameCode() & 0xfffff);
        functions.put(new Long(key), fn);
    }

    /**
     * Test whether a function with a given name and arity is available. This supports
     * the function-available() function in XSLT.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
        if (arity == -1) {
            for (int i=0; i<=20; i++) {
                if (isAvailable(fingerprint, uri, local, i)) {
                    return true;
                }
            }
            return false;
        }
        long key = ((long)arity)<<32 | fingerprint;
        return functions.get(new Long(key)) != null;
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
        long key = ((long)staticArgs.length)<<32 | (nameCode & 0xfffff);
        UserFunction fn = (UserFunction)functions.get(new Long(key));
        if (fn == null) {
            return null;
        }
        IndependentContext env = new IndependentContext(config); // this is needed only for the name pool
        UserFunctionCall fc = new UserFunctionCall();
        fc.setFunctionNameCode(nameCode);
        fc.setArguments(staticArgs);
        fc.setFunction(fn, env);
        fc.checkFunctionCall(fn, env);
        fc.setStaticType(fn.getResultType());
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
        ExecutableFunctionLibrary efl = new ExecutableFunctionLibrary(config);
        efl.functions = new HashMap(functions);
        return efl;
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