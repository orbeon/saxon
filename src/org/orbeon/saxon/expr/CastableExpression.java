package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.Value;

/**
* Castable Expression: implements "Expr castable as atomic-type?".
* The implementation simply wraps a cast expression with a try/catch.
*/

public final class CastableExpression extends UnaryExpression {

    AtomicType targetType;
    boolean allowEmpty;

    /**
     * Create a "castable" expression of the form "source castable as target"
     * @param source The source expression
     * @param target The type being tested against
     * @param allowEmpty true if an empty sequence is acceptable, that is if the expression
     * was written as "source castable as target?"
     */

    public CastableExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source);
        targetType = target;
        this.allowEmpty = allowEmpty;
    }

    /**
     * Get the target type
     * @return the target type
     */

    public AtomicType getTargetType() {
        return targetType;
    }

    /**
     * Determine whether the empty sequence is allowed
     * @return true if an empty sequence is allowed
     */

    public boolean allowsEmpty() {
        return allowEmpty;
    }

    /**
     * Simplify the expression
     * @return the simplified expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        return preEvaluate(visitor);
    }

    private Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (Literal.isAtomic(operand)) {
            return Literal.makeLiteral(
                    BooleanValue.get(effectiveBooleanValue(visitor.getStaticContext().makeEarlyEvaluationContext())));
        }
        if (Literal.isEmptySequence(operand)) {
            return new Literal(BooleanValue.get(allowEmpty));
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);

        // We need to take care here. The usual strategy of wrapping the operand in an expression that
        // does type-checking doesn't work here, because an error in the type checking should be caught,
        // while an error in evaluating the expression as written should not.

//        SequenceType atomicType = SequenceType.makeSequenceType(
//                                 BuiltInAtomicType.ANY_ATOMIC,
//                                 (allowEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE
//                                             : StaticProperty.EXACTLY_ONE));
//
//        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "castable as", 0, null);
//        role.setSourceLocator(this);
//        try {
//            operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, env);
//        } catch (XPathException err) {
//            return Literal.makeLiteral(BooleanValue.FALSE);
//        }

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (!CastExpression.isPossibleCast(
                operand.getItemType(th).getAtomizedItemType().getPrimitiveType(),
                targetType.getPrimitiveType())) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        return preEvaluate(visitor);
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        return preEvaluate(visitor);
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
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
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
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new CastableExpression(getBaseExpression().copy(), targetType, allowEmpty);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        int count = 0;
        SequenceIterator iter = operand.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                Value atomizedValue = ((NodeInfo)item).atomize();
                int length = atomizedValue.getLength();
                count += length;
                if (count > 1) {
                    return false;
                }
                if (length != 0) {
                    AtomicValue av = (AtomicValue)atomizedValue.itemAt(0);
                    if (!isCastable(av, targetType, context)) {
                        return false;
                    }
                }
            } else {
                AtomicValue av = (AtomicValue)item;
                count++;
                if (count > 1) {
                    return false;
                }
                if (!isCastable(av, targetType, context)) {
                    return false;
                }
            }
        }
        return count != 0 || allowEmpty;
    }

    /**
     * Determine whether a value is castable to a given type
     * @param value the value to be tested
     * @param targetType the type to be tested against
     * @param context XPath dynamic context
     * @return true if the value is castable to the required type
     */

    public static boolean isCastable(AtomicValue value, AtomicType targetType, XPathContext context) {
        //if (targetType instanceof BuiltInAtomicType) {
            return !(value.convert(targetType, true, context) instanceof ValidationFailure);
//        } else {
//            ConversionResult result =
//                value.convert((AtomicType)targetType.getBuiltInBaseType(), context, true);
//            if (result instanceof ValidationFailure) {
//                return false;
//            }
//            AtomicValue val = (AtomicValue)result;
//            result = targetType.setDerivedTypeLabel(val.copy(null), val.getStringValueCS(), true);
//            return !(result instanceof ValidationFailure);
//        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("castable");
        out.emitAttribute("as", targetType.toString(out.getConfiguration().getNamePool()));
        operand.explain(out);
        out.endElement();
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
