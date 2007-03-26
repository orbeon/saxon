package org.orbeon.saxon.functions;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.DescendingComparer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;

import java.util.Comparator;

/**
* This class implements the min() and max() functions
*/

public class Minimax extends CollatingFunction {

    public static final int MIN = 2;
    public static final int MAX = 3;

    private boolean ignoreNaN = false;

    /**
     * Indicate whether NaN values should be ignored. For the external min() and max() function, a
     * NaN value in the input causes the result to be NaN. Internally, however, min() and max() are also
     * used in such a way that NaN values should be ignored.
     * @param ignore true if NaN values are to be ignored when computing the min or max.
     */

    public void setIgnoreNaN(boolean ignore) {
        ignoreNaN = ignore;
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        Optimizer opt = env.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
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
        if (e != this) {
            return e;
        }
        if (getNumberOfArguments() == 1) {
            // test for a singlton: this often happens after (A<B) is rewritten as (min(A) lt max(B))
            int card = argument[0].getCardinality();
            TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            if (!Cardinality.allowsMany(card) && th.isSubType(argument[0].getItemType(th), Type.NUMBER_TYPE)) {
                ComputedExpression.setParentExpression(argument[0], getParentExpression());
                return argument[0];
            }
        }
        return this;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Comparator collator = getAtomicComparer(1, context);
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        boolean foundDouble = false;
        boolean foundFloat = false;
        boolean foundNaN = false;

        // For the max function, reverse the collator
        if (operation == MAX) {
            collator = new DescendingComparer(collator);
        }

        // Process the sequence, retaining the min (or max) so far. This will be an actual value found
        // in the sequence. At the same time, remember if a double and/or float has been encountered
        // anywhere in the sequence, and if so, convert the min/max to double/float at the end. This is
        // done to avoid problems if a decimal is converted first to a float and then to a double.

        SequenceIterator iter = argument[0].iterate(context);

        // Get the first value in the sequence, ignoring any NaN values
        AtomicValue min;
        AtomicValue prim;
        while (true) {
            min = (AtomicValue)iter.next();
            if (min == null) {
                return null;
            }
            prim = min.getPrimitiveValue();
            if (min instanceof UntypedAtomicValue) {
                try {
                    min = new DoubleValue(Value.stringToNumber(min.getStringValueCS()));
                    prim = min;
                    foundDouble = true;
                } catch (NumberFormatException e) {
                    dynamicError("Failure converting " +
                                                     Err.wrap(min.getStringValueCS()) +
                                                     " to a number", "FORG0001", context);
                }
            } else {
                if (prim instanceof DoubleValue) {
                    foundDouble = true;
                } else if (prim instanceof FloatValue) {
                    foundFloat = true;
                }
            }
            if (prim instanceof NumericValue && ((NumericValue)prim).isNaN()) {
                // if there's a NaN in the sequence, return NaN, unless ignoreNaN is set
                if (ignoreNaN) {
                    continue;   // ignore the NaN and treat the next item as the first real one
                } else if (prim instanceof DoubleValue) {
                    return min; // return double NaN
                } else {
                    // we can't ignore a float NaN, because we might need to promote it to a double NaN
                    foundNaN = true;
                    min = FloatValue.NaN;
                    break;
                }
            } else {
                int p = prim.getItemType(th).getPrimitiveType();
                if (!Type.isOrdered(p)) {
                    typeError("Type " + min.getItemType(th) + " is not an ordered type", "FORG0006", context);
                    return null;
                }
                break;          // process the rest of the sequence
            }
        }

        while (true) {
            AtomicValue test = (AtomicValue)iter.next();
            if (test==null) {
                break;
            }
            AtomicValue test2 = test;
            prim = test2.getPrimitiveValue();
            if (test instanceof UntypedAtomicValue) {
                try {
                    test2 = new DoubleValue(Value.stringToNumber(test.getStringValueCS()));
                    if (foundNaN) {
                        return DoubleValue.NaN;
                    }
                    prim = test2;
                    foundDouble = true;
                } catch (NumberFormatException e) {
                    dynamicError("Failure converting " +
                                                     Err.wrap(test.getStringValueCS()) +
                                                     " to a number", "FORG0001", context);
                }
            } else {
                if (prim instanceof DoubleValue) {
                    if (foundNaN) {
                        return DoubleValue.NaN;
                    }
                    foundDouble = true;
                } else if (prim instanceof FloatValue) {
                    foundFloat = true;
                }
            }
            if (prim instanceof NumericValue && ((NumericValue)prim).isNaN()) {
                // if there's a double NaN in the sequence, return NaN, unless ignoreNaN is set
                if (ignoreNaN) {
                    continue;
                } else if (foundDouble) {
                    return DoubleValue.NaN;
                } else {
                    // can't return float NaN until we know whether to promote it
                    foundNaN = true;
                }
            } else {
                try {
                    if (collator.compare(prim, min) < 0) {
                        min = test2;
                    }
                } catch (ClassCastException err) {
                    typeError("Cannot compare " + min.getItemType(th) +
                                       " with " + test2.getItemType(th), "FORG0006", context);
                    return null;
                }
            }
        }
        if (foundNaN) {
            return FloatValue.NaN;
        }
        if (foundDouble) {
            if (!(min instanceof DoubleValue)) {
                min = min.convert(Type.DOUBLE, context);
            }
        } else if (foundFloat) {
            if (!(min instanceof FloatValue)) {
                min = min.convert(Type.FLOAT, context);
            }    
        }
        return min;
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
