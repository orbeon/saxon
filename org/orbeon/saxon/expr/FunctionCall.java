package net.sf.saxon.expr;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Value;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

/**
* Abstract superclass for calls to system-defined and user-defined functions
*/

public abstract class FunctionCall extends ComputedExpression {

    /**
     * The name of the function
     */

    private int nameCode;

    /**
    * The array of expressions representing the actual parameters
    * to the function call
    */

    protected Expression[] argument;

    /**
     * Set the name code of the function being called
     */

    public final void setFunctionNameCode(int nc) {
        nameCode = nc;
    }

    /**
     * Get the name code of the function being called
     * @return the name code as recorded in the name pool
     */

    public final int getFunctionNameCode() {
        return nameCode;
    }

    /**
    * Determine the number of actual arguments supplied in the function call
    */

    public final int getNumberOfArguments() {
        return argument.length;
    }

    /**
    * Method called by the expression parser when all arguments have been supplied
    */

    public void setArguments(Expression[] args) {
        argument = args;
        for (int a=0; a<args.length; a++) {
            adoptChildExpression(args[a]);
        }
    }

    /**
    * Simplify the function call. Default method is to simplify each of the supplied arguments and
    * evaluate the function if all are now known.
    */

     public Expression simplify(StaticContext env) throws XPathException {
        return simplifyArguments(env);
    }

    /**
    * Simplify the arguments of the function.
    * Called from the simplify() method of each function.
    * @return the result of simplifying the arguments of the expression
    */

    protected final Expression simplifyArguments(StaticContext env) throws XPathException {
        for (int i=0; i<argument.length; i++) {
            Expression exp = argument[i].simplify(env);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
        }
        return this;
    }

    /**
    * Type-check the expression. This also calls preEvaluate() to evaluate the function
    * if all the arguments are constant; functions that do not require this behavior
    * can override the preEvaluate method.
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        boolean fixed = true;
        for (int i=0; i<argument.length; i++) {
            Expression exp = argument[i].analyze(env, contextItemType);
            if (exp != argument[i]) {
                adoptChildExpression(exp);
                argument[i] = exp;
            }
            if (!(argument[i] instanceof Value)) {
                fixed = false;
            }
        }
        checkArguments(env);
        if (fixed) {
            return preEvaluate(env);
        } else {
            return this;
        }
    }

    /**
    * Pre-evaluate a function at compile time. Functions that do not allow
    * pre-evaluation, or that need access to context information, can override this method.
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        return ExpressionTool.eagerEvaluate(this, null);
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                for (int i=0; i<argument.length; i++) {
                    argument[i] = argument[i].promote(offer);
                }
            }
            return this;
        }
    }

    /**
    * Method supplied by each class of function to check arguments during parsing, when all
    * the argument expressions have been read
    */

    protected abstract void checkArguments(StaticContext env) throws XPathException;

    /**
    * Check number of arguments. <BR>
    * A convenience routine for use in subclasses.
    * @param min the minimum number of arguments allowed
    * @param max the maximum number of arguments allowed
    * @return the actual number of arguments
    * @throws net.sf.saxon.trans.XPathException if the number of arguments is out of range
    */

    protected int checkArgumentCount(int min, int max, StaticContext env) throws XPathException {
        int numArgs = argument.length;
        if (min==max && numArgs != min) {
            throw new StaticError("Function " + getDisplayName(env.getNamePool()) + " must have "
                    + min + pluralArguments(min),
                    ExpressionTool.getLocator(this));
        }
        if (numArgs < min) {
            throw new StaticError("Function " + getDisplayName(env.getNamePool()) + " must have at least "
                    + min + pluralArguments(min),
                    ExpressionTool.getLocator(this));
        }
        if (numArgs > max) {
            throw new StaticError("Function " + getDisplayName(env.getNamePool()) + " must have no more than "
                    + max + pluralArguments(max),
                    ExpressionTool.getLocator(this));
        }
        return numArgs;
    }

    /**
    * Utility routine used in constructing error messages
    */

    private static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
    }

    /**
    * Get the immediate subexpressions of this expression
    */

    public Iterator iterateSubExpressions() {
        return Arrays.asList(argument).iterator();
    }

    /**
    * Diagnostic print of expression structure
    */

    public final String getDisplayName(NamePool pool) {
        return pool.getDisplayName(getFunctionNameCode());
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "function " + getDisplayName(pool));
        for (int a=0; a<argument.length; a++) {
            argument[a].display(level+1, pool, out);
        }
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
