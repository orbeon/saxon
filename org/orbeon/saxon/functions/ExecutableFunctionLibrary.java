package org.orbeon.saxon.functions;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.UserFunctionCall;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trans.XPathException;

import java.util.HashMap;
import java.util.Iterator;

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

    /**
     * Create the ExecutableFunctionLibrary
     * @param config the Saxon configuration
     */

    public ExecutableFunctionLibrary(Configuration config) {
        this.config = config;
    }

    /**
     * Make a key that will uniquely identify a function
     * @param functionName the name of the function
     * @param arity the number of arguments
     * @return the constructed key. This is of the form {uri}local/arity
     */

    private String makeKey(StructuredQName functionName, int arity) {
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        FastStringBuffer sb = new FastStringBuffer(uri.length() + local.length() + 8);
        sb.append('{');
        sb.append(uri);
        sb.append('}');
        sb.append(local);
        sb.append("/" + arity);
        return sb.toString();
    }

    /**
     * Register a function with the function library
     * @param fn the function to be registered
     */

    public void addFunction(UserFunction fn) {
        functions.put(makeKey(fn.getFunctionName(), fn.getNumberOfArguments()), fn);
    }

    /**
     * Test whether a function with a given name and arity is available. This supports
     * the function-available() function in XSLT.
     * @param functionName the name of the function being sought
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     */

    public boolean isAvailable(StructuredQName functionName, int arity) {
        if (arity == -1) {
            for (int i=0; i<=20; i++) {
                if (isAvailable(functionName, i)) {
                    return true;
                }
            }
            return false;
        }
        return functions.get(makeKey(functionName, arity)) != null;
    }

    /**
     * Bind a function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName  The name of the function to be called
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env the static evaluation context
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws org.orbeon.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env)
            throws XPathException {
        UserFunction fn = (UserFunction)functions.get(makeKey(functionName, staticArgs.length));
        if (fn == null) {
            return null;
        }
        ExpressionVisitor visitor = ExpressionVisitor.make(env);
        UserFunctionCall fc = new UserFunctionCall();
        fc.setFunctionName(functionName);
        fc.setArguments(staticArgs);
        fc.setFunction(fn);
        fc.checkFunctionCall(fn, visitor);
        fc.setStaticType(fn.getResultType(config.getTypeHierarchy()));
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

    /**
     * Iterate over all the functions defined in this function library. The objects
     * returned by the iterator are of class {@link UserFunction}
     * @return an iterator delivering the {@link UserFunction} objects representing
     * the user-defined functions in a stylesheet or query
     */

    public Iterator iterateFunctions() {
        return functions.values().iterator();
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