package org.orbeon.saxon.query;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.UserFunctionCall;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.Configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An UnboundFunctionLibrary is not a real function library; rather, it is used to keep track of function calls
 * that cannot yet be bound to a known declared function, but will have to be bound when all user-declared functions
 * are available.
*/

public class UnboundFunctionLibrary implements FunctionLibrary {

    private List unboundFunctionCalls = new ArrayList(20);
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
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
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

    public Expression bind(int nameCode, String uri, String local, Expression[] arguments) throws XPathException {
        if (resolving) {
            return null;
        }
        UserFunctionCall ufc = new UserFunctionCall();
        ufc.setFunctionNameCode(nameCode);
        ufc.setArguments(arguments);
        unboundFunctionCalls.add(ufc);
        return ufc;
    }

     /**
     * Bind function calls that could not be bound when first encountered. These
     * will either be forwards references to functions declared later in the query,
     * or errors. This method is for internal use.
     * @throws org.orbeon.saxon.trans.StaticError if a function call refers to a function that has
     * not been declared
     */

    public void bindUnboundFunctionCalls(XQueryFunctionBinder lib, Configuration config) throws XPathException {
        resolving = true;
        final NamePool pool = config.getNamePool();
        Iterator iter = unboundFunctionCalls.iterator();
        while (iter.hasNext()) {
            UserFunctionCall ufc = (UserFunctionCall)iter.next();

            // The original UserFunctionCall is effectively a dummy: we weren't able to find a function
            // definition at the time. So we try again.
            final int nc = ufc.getFunctionNameCode();
            final int arity = ufc.getNumberOfArguments();
            final String uri = pool.getURI(nc);
            final String local = pool.getLocalName(nc);

            XQueryFunction fd = lib.getDeclaration(nc, uri, local, ufc.getArguments());
            if (fd != null) {
                fd.registerReference(ufc);
                ufc.setStaticType(fd.getResultType());
                ufc.setConfirmed(true);
                // TODO: check for error XQST0036 (schema types in function signature not available in calling module)
            } else {
                String msg = "Cannot find a matching " + arity +
                                    "-argument function named " + pool.getClarkName(nc) + "()";
                if (!config.isAllowExternalFunctions()) {
                    msg += ". Note: external function calls have been disabled";
                }
                StaticError err = new StaticError(msg,
                        ExpressionTool.getLocator(ufc));
                err.setErrorCode("XPST0017");
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
