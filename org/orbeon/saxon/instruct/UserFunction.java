package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.UserFunctionCall;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.xpath.XPathException;

import java.util.HashMap;

/**
 * This object represents the compiled form of a user-written function
 * (the source can be either an XSLT stylesheet function or an XQuery function).
 *
 * <p>It is assumed that type-checking, of both the arguments and the results,
 * has been handled at compile time. That is, the expression supplied as the body
 * of the function must be wrapped in code to check or convert the result to the
 * required type, and calls on the function must be wrapped at compile time to check or
 * convert the supplied arguments.
 */

public final class UserFunction extends Procedure implements InstructionInfoProvider {

    private int functionNameCode;
    private boolean memoFunction = false;
    private UserFunctionParameter[] parameterDefinitions;
    private SequenceType resultType;
    private transient InstructionDetails details = null;

    public UserFunction() {}

    public UserFunction(Expression body) {
        setBody(body);
    };

    public void setParameterDefinitions(UserFunctionParameter[] params) {
        this.parameterDefinitions = params;
    }

    public UserFunctionParameter[] getParameterDefinitions() {
        return parameterDefinitions;
    }

    public void setResultType(SequenceType resultType) {
        this.resultType = resultType;
    }

    /**
     * Get the type of value returned by this function
     * @return the declared result type, or the inferred result type
     * if this is more precise
     */
    public SequenceType getResultType() {
        return resultType;
    }

    /**
     * Get the required types of an argument to this function
     * @param n identifies the argument in question, starting at 0
     * @return a SequenceType object, indicating the required type of the argument
     */

    public SequenceType getArgumentType(int n) {
        return parameterDefinitions[n].getRequiredType();
    }

    /**
     * Get the arity of this function
     * @return the number of arguments
     */

    public int getNumberOfArguments() {
        return parameterDefinitions.length;
    }

    /**
     * Mark this function as a memo function (or not)
     * @param isMemo true if this is a memo function
     */

    public void setMemoFunction(boolean isMemo) {
        memoFunction = isMemo;
    }

    /**
     * Set the namepool name code of the function
     * @param nameCode represents the function name
     */

    public void setFunctionNameCode(int nameCode) {
        functionNameCode = nameCode;
    }

    /**
     * Get the namepool name code of the function
     * @return a name code representing the function name
     */
    
    public int getFunctionNameCode() {
        return functionNameCode;
    }

    /**
     * Call this function.
     * @param actualArgs the arguments supplied to the function. These must have the correct
     * types required by the function signature (it is the caller's responsibility to check this).
     * It is acceptable to supply a {@link org.orbeon.saxon.value.Closure} to represent a value whose
     * evaluation will be delayed until it is needed. The array must be the correct size to match
     * the number of arguments: again, it is the caller's responsibility to check this.
     * @param context This provides the run-time context for evaluating the function. It is the caller's
     * responsibility to allocate a "clean" context for the function to use; the context that is provided
     * will be overwritten by the function.
     * @param evaluateTailCalls if true, then any function calls contained in the body of the function
     * are evaluated in the normal way, whether or not they are marked as tail calls. If the argument
     * is false, then tail calls are not evaluated, and instead a FunctionCallPackage is returned containing
     * the information needed to evaluate the function. The caller must then be prepared to deal with this
     * returned value by evaluating the packaged function call (which may return further packaged function
     * calls, and so on).
     * @return a Value representing the result of the function.
     */

    public Value call(Value[] actualArgs, XPathContextMajor context, boolean evaluateTailCalls)
            throws XPathException {

        // If this is a memo function, see if the result is already known
        Controller controller = context.getController();
        if (memoFunction) {
            Value value = getCachedValue(controller, actualArgs);
            if (value != null) return value;
        }

        // Otherwise evaluate the function

        context.setStackFrame(getStackFrameMap(), actualArgs);
        Value result;
        try {
            result = ExpressionTool.lazyEvaluate(getBody(), context, false);
        } catch (XPathException err) {
            err.setLocator(this);
            throw err;
        }

        if (evaluateTailCalls) {
            while (result instanceof UserFunctionCall.FunctionCallPackage) {
                result = ((UserFunctionCall.FunctionCallPackage)result).call();
            }
        }

        // If this is a memo function, save the result in the cache
        if (memoFunction) {
            putCachedValue(controller, actualArgs, result);
        }

        return result;
    }

    /**
     * Call this function. This method allows an XQuery function to be called directly from a Java
     * application. It creates the environment needed to achieve this
     * @param actualArgs the arguments supplied to the function. These must have the correct
     * types required by the function signature (it is the caller's responsibility to check this).
     * It is acceptable to supply a {@link org.orbeon.saxon.value.Closure} to represent a value whose
     * evaluation will be delayed until it is needed. The array must be the correct size to match
     * the number of arguments: again, it is the caller's responsibility to check this.
     * @param controller This provides the run-time context for evaluating the function. A Controller
     * may be obtained by calling {@link org.orbeon.saxon.query.XQueryExpression#getController}. This may
     * be used for a series of calls on functions defined in the same module as the XQueryExpression.
     * @return a Value representing the result of the function.
     */

    public Value call(Value[] actualArgs, Controller controller) throws XPathException {
        return call(actualArgs, controller.newXPathContext(), true);
    }

    /**
     * For memo functions, get a saved value from the cache.
     * @return the cached value, or null if no value has been saved for these parameters
     */

    private Value getCachedValue(Controller controller, Value[] params) {

        try {
            HashMap map = (HashMap) controller.getUserData(this, "memo-function-cache");
            if (map == null) {
                return null;
            }
            String key = getCombinedKey(params);
            //System.err.println("Used cached value");
            return (Value) map.get(key);
        } catch (XPathException err) {
            return null;
        }
    }

    /**
     * For memo functions, put the computed value in the cache.
     */

    private void putCachedValue(Controller controller, Value[] params, Value value) {
        try {
            HashMap map = (HashMap) controller.getUserData(this, "memo-function-cache");
            if (map == null) {
                map = new HashMap(32);
                controller.setUserData(this, "memo-function-cache", map);
            }
            String key = getCombinedKey(params);
            map.put(key, value);
        } catch (XPathException err) {
            // it doesn't matter if we fail to cache the result
        }
    }

    /**
     * Get a key value representing the values of all the supplied arguments
     */

    private static String getCombinedKey(Value[] params) throws XPathException {
        StringBuffer sb = new StringBuffer(120);

        for (int i = 0; i < params.length; i++) {
            Value val = params[i];
            SequenceIterator iter = val.iterate(null);
            while (true) {
                Item item = iter.next();
                if (item == null) {
                    break;
                }
                if (item instanceof NodeInfo) {
                    NodeInfo node = (NodeInfo) item;
                    //sb.append(""+node.getDocumentRoot().getDocumentNumber());
                    //sb.append('/');
                    sb.append(node.generateId());
                } else {
                    sb.append("" + Type.displayTypeName(item));
                    sb.append('/');
                    sb.append(item.getStringValue());
                }
                sb.append('\u0001');
            }
            sb.append('\u0002');
        }
        return sb.toString();
    }

    /**
     * Get the InstructionInfo details about the construct. This information isn't used for tracing,
     * but it is available when inspecting the context stack.
     */

    public InstructionInfo getInstructionInfo() {
        if (details == null) {
            details = new InstructionDetails();
            details.setSystemId(getSystemId());
            details.setLineNumber(getLineNumber());
            details.setConstructType(StandardNames.XSL_FUNCTION);
            details.setObjectNameCode(functionNameCode);
            details.setProperty("function", this);
        }
        return details;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//
