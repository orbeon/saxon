package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.evpull.EventIterator;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.SortExpression;
import org.orbeon.saxon.sort.TupleSorter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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

public class UserFunction extends Procedure {

    private StructuredQName functionName;
    private boolean memoFunction = false;
    private boolean tailCalls = false;
            // indicates that the function contains tail calls, not necessarily recursive ones.
    private boolean tailRecursive = false;
            // indicates that the function contains tail calls on itself
    private UserFunctionParameter[] parameterDefinitions;
    private SequenceType resultType;
    private int evaluationMode = ExpressionTool.UNDECIDED;
    private boolean isUpdating = false;

    /**
     * Create a user-defined function with no body (the body must be added later)
     */

    public UserFunction() {}

    /**
     * Create a user-defined function
     * @param body the expression comprising the body of the function, which is evaluated to compute the result
     * of the function
     */

    public UserFunction(Expression body) {
        setBody(body);
    }

    /**
     * Set the function name
     * @param name the function name
     */

    public void setFunctionName(StructuredQName name) {
        functionName = name;
    }

    /**
     * Get the function name
     * @return the function name, as a StructuredQName
     */

    public StructuredQName getFunctionName() {
        return functionName;
    }


    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     *
     */

    public StructuredQName getObjectName() {
        return functionName;
    }

    /**
     * Determine the preferred evaluation mode for this function
     */

    public void computeEvaluationMode() {
        if (tailRecursive || memoFunction) {
                // If this function contains tail calls, we evaluate it eagerly, because
                // the caller needs to know whether a tail call was returned or not: if we
                // return a Closure, the tail call escapes into the wild and can reappear anywhere...
                // Eager evaluation also makes sense if it's a memo function.
            evaluationMode = ExpressionTool.eagerEvaluationMode(getBody());
        } else {
            evaluationMode = ExpressionTool.lazyEvaluationMode(getBody());
        }
    }

    /**
     * Set the definitions of the declared parameters for this function
     * @param params an array of parameter definitions
     */

    public void setParameterDefinitions(UserFunctionParameter[] params) {
        parameterDefinitions = params;
    }

    /**
     * Get the definitions of the declared parameters for this function
     * @return an array of parameter definitions
     */

    public UserFunctionParameter[] getParameterDefinitions() {
        return parameterDefinitions;
    }

    /**
     * Set the declared result type of the function
     * @param resultType the declared return type
     */

    public void setResultType(SequenceType resultType) {
        this.resultType = resultType;
    }

    /**
     * Indicate whether the function contains a tail call
     * @param tailCalls true if the function contains a tail call (on any function)
     * @param recursiveTailCalls true if the function contains a tail call (on itself)
     */

    public void setTailRecursive(boolean tailCalls, boolean recursiveTailCalls) {
        this.tailCalls = tailCalls;
        tailRecursive = recursiveTailCalls;
    }

    /**
     * Determine whether the function contains tail calls (on this or other functions)
     * @return true if the function contains tail calls
     */

    public boolean containsTailCalls() {
        return tailCalls;
    }

    /**
     * Determine whether the function contains a tail call, calling itself
     * @return true if the function contains a directly-recursive tail call
     */

    public boolean isTailRecursive() {
        return tailRecursive;
    }

    /**
     * Set whether this is an updating function (as defined in XQuery Update)
     * @param isUpdating true if this is an updating function
     */

    public void setUpdating(boolean isUpdating) {
        this.isUpdating = isUpdating;
    }

    /**
     * Ask whether this is an updating function (as defined in XQuery Update)
     * @return true if this is an updating function
     */

    public boolean isUpdating() {
        return isUpdating;
    }
    

    /**
     * Get the type of value returned by this function
     * @param th the type hierarchy cache
     * @return the declared result type, or the inferred result type
     * if this is more precise
     */

    public SequenceType getResultType(TypeHierarchy th) {
        if (resultType == SequenceType.ANY_SEQUENCE) {
            // see if we can infer a more precise result type. We don't do this if the function contains
            // calls on further functions, to prevent infinite regress.
            if (!containsUserFunctionCalls(getBody())) {
                resultType = SequenceType.makeSequenceType(
                        getBody().getItemType(th), getBody().getCardinality());
            }
        }
        return resultType;
    }

    /**
     * Determine whether a given expression contains calls on user-defined functions
     * @param exp the expression to be tested
     * @return true if the expression contains calls to user functions.
     */

    private static boolean containsUserFunctionCalls(Expression exp) {
        if (exp instanceof UserFunctionCall) {
            return true;
        }
        Iterator i = exp.iterateSubExpressions();
        while (i.hasNext()) {
            Expression e = (Expression)i.next();
            if (containsUserFunctionCalls(e)) {
                return true;
            }
        }
        return false;
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
     * Get the evaluation mode. The evaluation mode will be computed if this has not already been done
     * @return the computed evaluation mode
     */

    public int getEvaluationMode() {
        if (evaluationMode == ExpressionTool.UNDECIDED) {
            computeEvaluationMode();
        }
        return evaluationMode;
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
     * Ask whether this function is a memo function
     * @return true if this function is marked as a memo function
     */

    public boolean isMemoFunction() {
        return memoFunction;
    }

    /**
     * Gather the direct contributing callees of this function. A callee is a function that this one
     * calls. A contributing callee is a function whose output is added directly to the output of this
     * function without further processing (other than by attaching it to a parent element or document
     * node). A direct contributing callee is a function that is called directly, rather than indirectly.
     * @param result the list into which the callees are gathered.
     */

    public void gatherDirectContributingCallees(Set result) {
        Expression body = getBody();
        gatherDirectContributingCallees(body, result);
    }

    private void gatherDirectContributingCallees(Expression exp, Set result) {
        Iterator kids = exp.iterateSubExpressions();
        while (kids.hasNext()) {
            Expression kid = (Expression)kids.next();
            if (kid instanceof UserFunctionCall) {
                result.add(((UserFunctionCall)kid).getFunction());
            } else if (kid instanceof Assignation) {
                gatherDirectContributingCallees(((Assignation)kid).getAction(), result);
//            } else if (kid instanceof IfExpression) {
//                gatherDirectContributingCallees(((IfExpression)kid).getThenExpression(), result);
//                gatherDirectContributingCallees(((IfExpression)kid).getElseExpression(), result);
            } else if (kid instanceof Choose) {
                Expression[] actions = ((Choose)kid).getActions();
                for (int c=0; c<actions.length; c++) {
                    gatherDirectContributingCallees(actions[c], result);
                }
            } else if (kid instanceof ParentNodeConstructor) {
                gatherDirectContributingCallees(((ParentNodeConstructor)kid).getContentExpression(), result);
            } else if (kid instanceof FilterExpression) {
                gatherDirectContributingCallees(((FilterExpression)kid).getBaseExpression(), result);
            } else if (kid instanceof SortExpression) {
                gatherDirectContributingCallees(((SortExpression)kid).getBaseExpression(), result);
            } else if (kid instanceof TupleSorter) {
                gatherDirectContributingCallees(((TupleSorter)kid).getBaseExpression(), result);
            } else if (kid instanceof TailCallLoop) {
                gatherDirectContributingCallees(((TailCallLoop)kid).getBaseExpression(), result);
            }
        }
    }


    /**
     * Call this function to return a value.
     * @param actualArgs the arguments supplied to the function. These must have the correct
     * types required by the function signature (it is the caller's responsibility to check this).
     * It is acceptable to supply a {@link org.orbeon.saxon.value.Closure} to represent a value whose
     * evaluation will be delayed until it is needed. The array must be the correct size to match
     * the number of arguments: again, it is the caller's responsibility to check this.
     * @param context This provides the run-time context for evaluating the function. It is the caller's
     * responsibility to allocate a "clean" context for the function to use; the context that is provided
     * will be overwritten by the function.
     * @return a Value representing the result of the function.
     */

    public ValueRepresentation call(ValueRepresentation[] actualArgs, XPathContextMajor context)
            throws XPathException {

        // If this is a memo function, see if the result is already known
        Controller controller = context.getController();
        if (memoFunction) {
            ValueRepresentation value = getCachedValue(controller, actualArgs);
            if (value != null) return value;
        }

        if (evaluationMode == ExpressionTool.UNDECIDED) {
            // should have been done at compile time
            computeEvaluationMode();
        }

        // Otherwise evaluate the function

        context.setStackFrame(getStackFrameMap(), actualArgs);
        ValueRepresentation result;
        try {
            result = ExpressionTool.evaluate(getBody(), evaluationMode, context, 1);
        } catch (XPathException err) {
            err.maybeSetLocation(this);
            throw err;
        }

        // If this is a memo function, save the result in the cache
        if (memoFunction) {
            putCachedValue(controller, actualArgs, result);
        }

        return result;
    }

    /**
      * Call this function in "push" mode, writing the results to the current output destination.
      * @param actualArgs the arguments supplied to the function. These must have the correct
      * types required by the function signature (it is the caller's responsibility to check this).
      * It is acceptable to supply a {@link org.orbeon.saxon.value.Closure} to represent a value whose
      * evaluation will be delayed until it is needed. The array must be the correct size to match
      * the number of arguments: again, it is the caller's responsibility to check this.
      * @param context This provides the run-time context for evaluating the function. It is the caller's
      * responsibility to allocate a "clean" context for the function to use; the context that is provided
      * will be overwritten by the function.
      */

     public void process(ValueRepresentation[] actualArgs, XPathContextMajor context)
             throws XPathException {
         context.setStackFrame(getStackFrameMap(), actualArgs);
         getBody().process(context);
     }

    /**
      * Call this function in "pull" mode, returning the results as a sequence of PullEvents.
      * @param actualArgs the arguments supplied to the function. These must have the correct
      * types required by the function signature (it is the caller's responsibility to check this).
      * It is acceptable to supply a {@link org.orbeon.saxon.value.Closure} to represent a value whose
      * evaluation will be delayed until it is needed. The array must be the correct size to match
      * the number of arguments: again, it is the caller's responsibility to check this.
      * @param context This provides the run-time context for evaluating the function. It is the caller's
      * responsibility to allocate a "clean" context for the function to use; the context that is provided
      * will be overwritten by the function.
      * @return an iterator over the results of the function call
      */

     public EventIterator iterateEvents(ValueRepresentation[] actualArgs, XPathContextMajor context)
             throws XPathException {
         context.setStackFrame(getStackFrameMap(), actualArgs);
         return getBody().iterateEvents(context);
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
     * may be obtained by calling {@link org.orbeon.saxon.query.XQueryExpression#newController}. This may
     * be used for a series of calls on functions defined in the same module as the XQueryExpression.
     * @return a Value representing the result of the function.
     */

    public ValueRepresentation call(ValueRepresentation[] actualArgs, Controller controller) throws XPathException {
        return call(actualArgs, controller.newXPathContext());
    }

    /**
     * Call an updating function.
     * @param actualArgs the arguments supplied to the function. These must have the correct
     * types required by the function signature (it is the caller's responsibility to check this).
     * It is acceptable to supply a {@link org.orbeon.saxon.value.Closure} to represent a value whose
     * evaluation will be delayed until it is needed. The array must be the correct size to match
     * the number of arguments: again, it is the caller's responsibility to check this.
     * @param context the dynamic evaluation context
     * @param pul the pending updates list, to which the function's update actions are to be added.
     */

    public void callUpdating(ValueRepresentation[] actualArgs, XPathContextMajor context, PendingUpdateList pul)
    throws XPathException {
        context.setStackFrame(getStackFrameMap(), actualArgs);
        try {
            getBody().evaluatePendingUpdates(context, pul);
        } catch (XPathException err) {
            err.maybeSetLocation(this);
            throw err;
        }
    }

    /**
     * For memo functions, get a saved value from the cache.
     * @param controller the run-time Controller
     * @param params the supplied parameters for the function call
     * @return the cached value, or null if no value has been saved for these parameters
     */

    private ValueRepresentation getCachedValue(Controller controller, ValueRepresentation[] params) throws XPathException {
        HashMap map = (HashMap) controller.getUserData(this, "memo-function-cache");
        if (map == null) {
            return null;
        }
        String key = getCombinedKey(params);
        //System.err.println("Used cached value");
        return (ValueRepresentation) map.get(key);
    }

    /**
     * For memo functions, put the computed value in the cache.
     * @param controller the run-time Controller
     * @param params the supplied parameter values
     * @param value the computed result of the function
     */

    private void putCachedValue(Controller controller, ValueRepresentation[] params, ValueRepresentation value) throws XPathException {
        HashMap map = (HashMap) controller.getUserData(this, "memo-function-cache");
        if (map == null) {
            map = new HashMap(32);
            controller.setUserData(this, "memo-function-cache", map);
        }
        String key = getCombinedKey(params);
        map.put(key, value);
    }

    /**
     * Get a key value representing the values of all the supplied arguments
     * @param params the supplied values of the function arguments
     * @return a key value which can be used to index the cache of remembered function results
     */

    private static String getCombinedKey(ValueRepresentation[] params) throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(120);

        for (int i = 0; i < params.length; i++) {
            ValueRepresentation val = params[i];
            // TODO: if the argument value is a sequence, use the identity of the sequence, not its content
            SequenceIterator iter = Value.getIterator(val);
            while (true) {
                Item item = iter.next();
                if (item == null) {
                    break;
                }
                if (item instanceof NodeInfo) {
                    NodeInfo node = (NodeInfo) item;
                    node.generateId(sb);
                } else {
                    sb.append("" + Type.displayTypeName(item));
                    sb.append('/');
                    sb.append(item.getStringValueCS());
                }
                sb.append('\u0001');
            }
            sb.append('\u0002');
        }
        return sb.toString();
    }


    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link org.orbeon.saxon.om.StandardNames}: all less than 1024)
     * or it will be a constant in class {@link org.orbeon.saxon.trace.Location}.
     */

    public int getConstructType() {
        return Location.FUNCTION;
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
