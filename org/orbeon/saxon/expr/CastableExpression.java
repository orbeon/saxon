package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.xpath.XPathException;

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
        if (operand instanceof Value) {
            return BooleanValue.get(effectiveBooleanValue(null));
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        SequenceType atomicType =
                new SequenceType(Type.ANY_ATOMIC_TYPE,
                                 (allowEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE
                                             : StaticProperty.EXACTLY_ONE));

        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "castable as", 0);
        operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, env);

        if (operand instanceof AtomicValue) {
            return BooleanValue.get(effectiveBooleanValue(null));
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
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
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
            AtomicValue prim =
                    value.convert(targetType.getBuiltInBaseType().getFingerprint(), context);
            DerivedAtomicValue val =
                    DerivedAtomicValue.makeValue(prim, prim.getStringValue(), targetType, false);
            return val != null;
        } catch (XPathException err) {
            return false;
        }
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "castable as " + targetType.toString(pool);
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
