package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.Type;

/**
* This class supports the XPath functions boolean(), not(), true(), and false()
*/


public class BooleanFn extends SystemFunction {

    public static final int BOOLEAN = 0;
    public static final int NOT = 1;
    public static final int TRUE = 2;
    public static final int FALSE = 3;

    /**
     * Simplify the function call. Default method is to simplify each of the supplied arguments and
     * evaluate the function if all are now known.
     */

    public Expression simplify(StaticContext env) throws XPathException {
        switch (operation) {
            case BOOLEAN:
            case NOT:
                return super.simplify(env);
            case TRUE:
                return BooleanValue.TRUE;
            case FALSE:
                return BooleanValue.FALSE;
            default:
                throw new UnsupportedOperationException("Unknown boolean operation");
        }
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        if (operation==BOOLEAN || operation==NOT) {
            XPathException err = TypeChecker.ebvError(argument[0], env.getConfiguration().getTypeHierarchy());
            if (err != null) {
                err.setLocator(this);
                throw err;
            }
            Optimizer opt = env.getConfiguration().getOptimizer();
            argument[0] = ExpressionTool.unsortedIfHomogeneous(opt, argument[0], false);
        }
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(opt, env, contextItemType);
        TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (e == this) {
            if (operation == BOOLEAN) {
                if (argument[0] instanceof ValueComparison) {
                    ValueComparison vc = (ValueComparison)argument[0];
                    if (vc.getResultWhenEmpty() == null) {
                        vc.setResultWhenEmpty(BooleanValue.FALSE);
                    }
                    vc.setParentExpression(getParentExpression());
                    return argument[0];
                } else if (th.isSubType(argument[0].getItemType(th), Type.BOOLEAN_TYPE) &&
                        argument[0].getCardinality() == StaticProperty.EXACTLY_ONE) {
                    ComputedExpression.setParentExpression(argument[0], getParentExpression());
                    return argument[0];
                }
            } else if (operation == NOT) {
                if (argument[0] instanceof ValueComparison) {
                    ValueComparison vc = ((ValueComparison)argument[0]).negate();
                    vc.setParentExpression(getParentExpression());
                    return vc.optimize(opt, env, contextItemType);
                } else if (argument[0] instanceof BooleanExpression) {
                    Expression be = ((BooleanExpression)argument[0]).negate(env);
                    ComputedExpression.setParentExpression(be, getParentExpression());
                    return be.optimize(opt, env, contextItemType);
                } else if (argument[0] instanceof BooleanFn) {
                    BooleanFn arg = (BooleanFn)argument[0];
                    if (arg.operation == BOOLEAN) {
                        // rewrite not(boolean(X)) as not(X)
                        argument[0] = arg.argument[0];
                        adoptChildExpression(argument[0]);
                    } else if (arg.argument[0].getItemType(th).getPrimitiveType() == Type.BOOLEAN) {
                        // rewrite not(not(X)) as X, provided X has type boolean
                        ComputedExpression.setParentExpression(arg.argument[0], getParentExpression());
                        return arg.argument[0];
                    }
                }
            }
        }
        return this;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the effective boolean value
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        try {
            switch (operation) {
                case BOOLEAN:
                    return argument[0].effectiveBooleanValue(c);
                case NOT:
                    return !argument[0].effectiveBooleanValue(c);
                case TRUE:
                    return true;
                case FALSE:
                    return false;
                default:
                    throw new UnsupportedOperationException("Unknown boolean operation");
            }
        } catch (XPathException e) {
            if (e.getLocator() == null) {
                e.setLocator(this);
            }
            throw e;
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
