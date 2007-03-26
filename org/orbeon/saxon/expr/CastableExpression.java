package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.ValidationErrorValue;

/**
* Castable Expression: implements "Expr castable as atomic-type?".
* The implementation simply wraps a cast expression with a try/catch.
*/

public final class CastableExpression extends UnaryExpression {

    AtomicType targetType;
    boolean allowEmpty;

    public CastableExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source);
        this.targetType = target;
        this.allowEmpty = allowEmpty;
    }

    /**
    * Simplify the expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (operand instanceof AtomicValue) {
            return BooleanValue.get(effectiveBooleanValue(env.makeEarlyEvaluationContext()));
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);
        SequenceType atomicType = SequenceType.makeSequenceType(
                                 Type.ANY_ATOMIC_TYPE,
                                 (allowEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE
                                             : StaticProperty.EXACTLY_ONE));

        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "castable as", 0, null);
        role.setSourceLocator(this);
        try {
            operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, env);
        } catch (XPathException err) {
            return BooleanValue.FALSE;
        }

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (!CastExpression.isPossibleCast(operand.getItemType(th).getPrimitiveType(), targetType.getPrimitiveType())) {
            return BooleanValue.FALSE;
        }

        if (operand instanceof AtomicValue) {
            return BooleanValue.get(effectiveBooleanValue(env.makeEarlyEvaluationContext()));
        }
        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.optimize(opt, env, contextItemType);
        if (operand instanceof AtomicValue) {
            return BooleanValue.get(effectiveBooleanValue(env.makeEarlyEvaluationContext()));
        }
        return this;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((CastableExpression)other).targetType &&
                allowEmpty == ((CastableExpression)other).allowEmpty;
    }

    /**
    * Determine the data type of the result of the Castable expression
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.BOOLEAN_TYPE;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    public boolean effectiveBooleanValue(XPathContext context) {
        try {
            AtomicValue value = (AtomicValue)operand.evaluateItem(context);
            if (value == null) {
                return allowEmpty;
            }
            if (targetType instanceof BuiltInAtomicType) {
                return !(value.convert(targetType, context, true) instanceof ValidationErrorValue);
            } else {
                AtomicValue prim =
                    value.convert((AtomicType)targetType.getBuiltInBaseType(), context, true);
                if (prim instanceof ValidationErrorValue) {
                    return false;
                }
                AtomicValue val =
                    targetType.makeDerivedValue(prim, prim.getStringValueCS(), true);
                return !(val instanceof ValidationErrorValue);
            }
        } catch (XPathException err) {
            return false;
        }
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        return "castable as " + targetType.toString(config.getNamePool());
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
