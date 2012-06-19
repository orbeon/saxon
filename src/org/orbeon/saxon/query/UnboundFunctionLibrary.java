package org.orbeon.saxon.query;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.UserFunctionCall;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trans.XPathException;

import java.util.ArrayList;
import java.util.List;

/**
 * An UnboundFunctionLibrary is not a real function library; rather, it is used to keep track of function calls
 * that cannot yet be bound to a known declared function, but will have to be bound when all user-declared functions
 * are available.
*/

public class UnboundFunctionLibrary implements FunctionLibrary {

    private List unboundFunctionCalls = new ArrayList(20);
    private List correspondingStaticContext = new ArrayList(20);
    private boolean resolving = false;

	/**
	* Create an XQueryFunctionLibrary
	*/

	public UnboundFunctionLibrary() {
	}

    /**
     * Test whether a function with a given name and arity is available. This supports
     * the function-available() function in XSLT. Since this library is used only in XQuery,
     * and contains no real functions, we always return false
     * @param functionName the name of the function required
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     */

    public boolean isAvailable(StructuredQName functionName, int arity) {
        return false;
    }

    /**
     * Identify a (namespace-prefixed) function appearing in the expression. This
     * method is called by the XQuery parser to resolve function calls found within
     * the query.
     * <p>Note that a function call may appear earlier in the query than the definition
     * of the function to which it is bound. Unlike XSLT, we cannot search forwards to
     * find the function definition. Binding of function calls is therefore a two-stage
     * process; at the time the function call is parsed, we simply register it as
     * pending; subsequently at the end of query parsing all the pending function
     * calls are resolved. Another consequence of this is that we cannot tell at the time
     * a function call is parsed whether it is a call to an internal (XSLT or XQuery)
     * function or to an extension function written in Java.
     * @return an Expression representing the function call. This will normally be
     * a FunctionCall, but it may be rewritten as some other expression.
     * @throws org.orbeon.saxon.trans.XPathException if the function call is invalid, either because it is
     * an unprefixed call to a non-system function, or because it is calling a system
     * function that is available in XSLT only. A prefixed function call that cannot
     * be recognized at this stage is assumed to be a forwards reference, and is bound
     * later when bindUnboundFunctionCalls() is called.
    */

    public Expression bind(StructuredQName functionName, Expression[] arguments, StaticContext env) throws XPathException {
        if (resolving) {
            return null;
        }
        UserFunctionCall ufc = new UserFunctionCall();
        ufc.setFunctionName(functionName);
        ufc.setArguments(arguments);
        unboundFunctionCalls.add(ufc);
        correspondingStaticContext.add(env);
        return ufc;
    }

     /**
      * Bind function calls that could not be bound when first encountered. These
      * will either be forwards references to functions declared later in the query,
      * or errors. This method is for internal use.
      * @param lib A library containing all the XQuery functions that have been declared;
      * the method searches this library for this missing function call
      * @param config The Saxon configuration
      * @throws XPathException if a function call refers to a function that has
      * not been declared
     */

    public void bindUnboundFunctionCalls(XQueryFunctionBinder lib, Configuration config) throws XPathException {
        resolving = true;
        for (int i=0; i<unboundFunctionCalls.size(); i++) {
            UserFunctionCall ufc = (UserFunctionCall)unboundFunctionCalls.get(i);
            QueryModule importingModule = (QueryModule)correspondingStaticContext.get(i);
            correspondingStaticContext.set(i, null);    // for garbage collection purposes
            // The original UserFunctionCall is effectively a dummy: we weren't able to find a function
            // definition at the time. So we try again.
            final StructuredQName q = ufc.getFunctionName();
            final int arity = ufc.getNumberOfArguments();

            XQueryFunction fd = lib.getDeclaration(q, ufc.getArguments());
            if (fd != null) {
                fd.registerReference(ufc);
                ufc.setStaticType(fd.getResultType());
                ufc.setConfirmed(true);
                // Check that the result type and all the argument types are in the static context of the
                // calling module
                importingModule.checkImportedFunctionSignature(fd);
            } else {
                String msg = "Cannot find a matching " + arity +
                        "-argument function named " + q.getClarkName() + "()";
                if (!config.isAllowExternalFunctions()) {
                    msg += ". Note: external function calls have been disabled";
                }
                XPathException err = new XPathException(msg, ufc);
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
                throw err;
            }
        }
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        UnboundFunctionLibrary qfl = new UnboundFunctionLibrary();
        qfl.unboundFunctionCalls = new ArrayList(unboundFunctionCalls);
        qfl.correspondingStaticContext = new ArrayList(correspondingStaticContext);
        return qfl;
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
