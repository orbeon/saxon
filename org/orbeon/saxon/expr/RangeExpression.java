package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

/**
* A RangeExpression is an expression that represents an integer sequence as
* a pair of end-points (for example "x to y").
* If the end-points are equal, the sequence is of length one.
 * <p>From Saxon 7.8, the sequence must be ascending; if the end-point is less
 * than the start-point, an empty sequence is returned. This is to allow
 * expressions of the form "for $i in 1 to count($seq) return ...." </p>
*/

public class RangeExpression extends BinaryExpression {

    /**
    * Construct a RangeExpression
    */

    public RangeExpression(Expression start, int op, Expression end) {
        super(start, op, end);
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand0 = operand0.typeCheck(env, contextItemType);
        operand1 = operand1.typeCheck(env, contextItemType);

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 0, null);
        role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_INTEGER, false, role0, env);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 1, null);
        role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_INTEGER, false, role1, env);

        return this;
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
        operand0 = operand0.optimize(opt, env, contextItemType);
        operand1 = operand1.optimize(opt, env, contextItemType);

        if (operand0 instanceof IntegerValue && operand1 instanceof IntegerValue) {
            long i0 = ((IntegerValue)operand0).longValue();
            long i1 = ((IntegerValue)operand1).longValue();
            if (i0 > i1) {
                return EmptySequence.getInstance();
            } else {
                return new IntegerRange(i0, i1);
            }
        }
        return this;

    }


    /**
    * Get the data type of the items returned
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.INTEGER_TYPE;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
    * Return an iteration over the sequence
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
        if (av1 == null) {
            return new EmptyIterator();
        }
        NumericValue v1 = (NumericValue)av1.getPrimitiveValue();

        AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
        if (av2 == null) {
            return new EmptyIterator();
        }
        NumericValue v2 = (NumericValue)av2.getPrimitiveValue();

        if (v1.compareTo(v2) > 0) {
            return new EmptyIterator();
        }
        return new RangeIterator(v1.longValue(), v2.longValue());
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
// Contributor(s): none.
//
