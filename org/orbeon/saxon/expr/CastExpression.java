package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.ErrorValue;
import org.orbeon.saxon.value.SequenceType;

/**
* Cast Expression: implements "cast as data-type ( expression )". It also allows an internal
* cast, which has the same semantics as a user-requested cast, but maps an empty sequence to
* an empty sequence.
*/

public final class CastExpression extends UnaryExpression  {

    private AtomicType targetType;
    private AtomicType targetPrimitiveType;
    private boolean allowEmpty = false;
    private boolean derived = false;

    public CastExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source);
        this.allowEmpty = allowEmpty;
        targetType = target;        
        targetPrimitiveType = (AtomicType)target.getPrimitiveItemType();
        derived = (targetType.getFingerprint() != targetPrimitiveType.getFingerprint());
        adoptChildExpression(source);
    }

    /**
    * Simplify the expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (operand instanceof AtomicValue) {
            return analyze(env, Type.ITEM_TYPE);
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        SequenceType atomicType = SequenceType.makeSequenceType(Type.ANY_ATOMIC_TYPE, getCardinality());

        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "cast as", 0, null);
        operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, env);

        if (Type.isSubType(operand.getItemType(), targetType)) {
            return operand;
            // It's not entirely clear that the spec permits this. Perhaps we should change the type label?
            // On the other hand, it's generally true that any expression defined to return an X
            // is allowed to return a subtype of X.
        }
        if (targetType.isNamespaceSensitive()) {
            return new CastAsQName(operand, targetType).analyze(env, contextItemType);
        }
        if (operand instanceof AtomicValue) {
            return (AtomicValue)evaluateItem(null);
        }
        return this;
    }

    /**
    * Get the static cardinality of the expression
    */

    public int computeCardinality() {
        return (allowEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE);
    }

    /**
    * Get the static type of the expression
    */

    public ItemType getItemType() {
        return targetType;
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

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue)operand.evaluateItem(context);
        if (value==null) {
            if (allowEmpty) {
                return null;
            } else {
                DynamicError e = new DynamicError("Cast does not allow an empty sequence");
                e.setXPathContext(context);
                throw e;
            }
        }
        AtomicValue result = value.convert(targetPrimitiveType, context, true);
        if (result instanceof ErrorValue) {
            XPathException err = ((ErrorValue)result).getException();
            String code = err.getErrorCodeLocalPart();
            dynamicError(err.getMessage(), code, context);
        }
        if (derived) {
            result = result.convert(targetType, context, true);
            if (result instanceof ErrorValue) {
                XPathException err = ((ErrorValue)result).getException();
                String code = err.getErrorCodeLocalPart();
                dynamicError(err.getMessage(), code, context);
            }
        }
        return result;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((CastExpression)other).targetType &&
                allowEmpty == ((CastExpression)other).allowEmpty;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "cast as " + targetType.toString(pool);
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
