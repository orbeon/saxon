package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.Configuration;

/**
* This class supports the XPath functions boolean(), not(), true(), and false()
*/


public class BooleanFn extends SystemFunction implements Negatable {

    public static final int BOOLEAN = 0;
    public static final int NOT = 1;
    public static final int TRUE = 2;
    public static final int FALSE = 3;

    /**
     * Simplify the function call. Default method is to simplify each of the supplied arguments and
     * evaluate the function if all are now known.
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        switch (operation) {
            case BOOLEAN:
            case NOT:
                return super.simplify(visitor);
            case TRUE:
                return Literal.makeLiteral(BooleanValue.TRUE);
            case FALSE:
                return Literal.makeLiteral(BooleanValue.FALSE);
            default:
                throw new UnsupportedOperationException("Unknown boolean operation");
        }
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        if (operation==BOOLEAN || operation==NOT) {
            XPathException err = TypeChecker.ebvError(argument[0], visitor.getConfiguration().getTypeHierarchy());
            if (err != null) {
                err.setLocator(this);
                throw err;
            }
            Optimizer opt = visitor.getConfiguration().getOptimizer();
            argument[0] = ExpressionTool.unsortedIfHomogeneous(opt, argument[0]);
        }
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this) {
            if (operation == BOOLEAN) {
                Expression ebv = rewriteEffectiveBooleanValue(argument[0], visitor, contextItemType);
                return (ebv == null ? this : ebv.optimize(visitor, contextItemType));
            } else if (operation == NOT) {
                Expression ebv = rewriteEffectiveBooleanValue(argument[0], visitor, contextItemType);
                if (ebv != null) {
                    argument[0] = ebv;
                }
                if (argument[0] instanceof Negatable && ((Negatable)argument[0]).isNegatable(visitor)) {
                    return ((Negatable)argument[0]).negate();
                } else {
                    return this;
                }
            }
        }
        return e;
    }

    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        return true;
    }

    /**
     * Create an expression that returns the negation of this expression
     * @return the negated expression
     */

    public Expression negate() {
        switch (operation) {
            case BOOLEAN:
                return SystemFunction.makeSystemFunction("not", getArguments());
            case NOT:
                return SystemFunction.makeSystemFunction("boolean", getArguments());
            case TRUE:
                return new Literal(BooleanValue.FALSE);
            case FALSE:
            default:
                return new Literal(BooleanValue.TRUE);
        }
    }

    /**
     * Optimize an expression whose effective boolean value is required
     * @param exp the expression whose EBV is to be evaluated
     * @param visitor an expression visitor
     * @param contextItemType the type of the context item for this expression
     * @return an expression that returns the EBV of exp, or null if no optimization was possible
     * @throws XPathException if static errors are found
     */

    public static Expression rewriteEffectiveBooleanValue(
            Expression exp, ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Configuration config = visitor.getConfiguration();
        TypeHierarchy th = config.getTypeHierarchy();
        exp = ExpressionTool.unsortedIfHomogeneous(config.getOptimizer(), exp);
        if (exp instanceof ValueComparison) {
            ValueComparison vc = (ValueComparison)exp;
            if (vc.getResultWhenEmpty() == null) {
                vc.setResultWhenEmpty(BooleanValue.FALSE);
            }
            return exp;
        } else if (th.isSubType(exp.getItemType(th), BuiltInAtomicType.BOOLEAN) &&
                exp.getCardinality() == StaticProperty.EXACTLY_ONE) {
            return exp;
        } else if (exp.getItemType(th) instanceof NodeTest) {
            // rewrite boolean(x) => exists(x)
            FunctionCall exists = SystemFunction.makeSystemFunction("exists", new Expression[]{exp});
            exists.setLocationId(exp.getLocationId());
            return exists.optimize(visitor, contextItemType);
        } else {
            return null;
        }
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
            e.maybeSetLocation(this);
            e.maybeSetContext(c);
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
