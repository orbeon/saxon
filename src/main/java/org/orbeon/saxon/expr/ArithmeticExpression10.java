package org.orbeon.saxon.expr;

import org.orbeon.saxon.functions.NumberFn;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.instruct.Choose;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod, in backwards
 * compatibility mode: see {@link ArithmeticExpression} for the non-backwards
 * compatible case.
 */

public class ArithmeticExpression10 extends BinaryExpression {

    private Calculator calculator;

    /**
     * Create an arithmetic expression to be evaluated in XPath 1.0 mode
     * @param p0 the first operand
     * @param operator the operator, for example {@link Token#PLUS}
     * @param p1 the second operand
     */

    public ArithmeticExpression10(Expression p0, int operator, Expression p1) {
        super(p0, operator, p1);
    }

    /**
     * Determine whether the expression is to be evaluated in backwards-compatible mode
     * @return true, always
     */

    public boolean isBackwardsCompatible() {
        return true;
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        if (Literal.isEmptySequence(operand0)) {
            return new Literal(DoubleValue.NaN);
        }

        if (Literal.isEmptySequence(operand1)) {
            return new Literal(DoubleValue.NaN);
        }

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, true, role0, visitor);
        final ItemType itemType0 = operand0.getItemType(th);
        if (itemType0 instanceof EmptySequenceTest) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }
        AtomicType type0 = (AtomicType) itemType0.getPrimitiveItemType();
        if (calculator == null) {
            operand0 = createConversionCode(operand0, th, type0);
        }
        type0 = (AtomicType) operand0.getItemType(th).getPrimitiveItemType();

        // System.err.println("First operand"); operand0.display(10);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, true, role1, visitor);
        final ItemType itemType1 = operand1.getItemType(th);
        if (itemType1 instanceof EmptySequenceTest) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }
        AtomicType type1 = (AtomicType)itemType1.getPrimitiveItemType();
        if (calculator == null) {
            operand1 = createConversionCode(operand1, th, type1);
        }

        type1 = (AtomicType) operand1.getItemType(th).getPrimitiveItemType();

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        if (operator == Token.NEGATE) {
            if (operand1 instanceof Literal) {
                Value v = ((Literal)operand1).getValue();
                if (v instanceof NumericValue) {
                    return Literal.makeLiteral(((NumericValue)v).negate());
                }
            }
            NegateExpression ne = new NegateExpression(operand1);
            ne.setBackwardsCompatible(true);
            return visitor.typeCheck(ne, contextItemType);
        }

        // Get a calculator to implement the arithmetic operation. If the types are not yet specifically known,
        // we allow this to return an "ANY" calculator which defers the decision. However, we only allow this if
        // at least one of the operand types is AnyAtomicType.

        boolean mustResolve = !(type0.equals(BuiltInAtomicType.ANY_ATOMIC) || type1.equals(BuiltInAtomicType.ANY_ATOMIC)
            || type0.equals(BuiltInAtomicType.NUMERIC) || type1.equals(BuiltInAtomicType.NUMERIC));

        calculator = Calculator.getCalculator(type0.getFingerprint(), type1.getFingerprint(),
                ArithmeticExpression.mapOpCode(operator), mustResolve);

        if (calculator == null) {
            XPathException de = new XPathException("Arithmetic operator is not defined for arguments of types (" +
                    type0.getDescription() + ", " + type1.getDescription() + ")");
            de.setLocator(this);
            de.setErrorCode("XPTY0004");
            throw de;
        }

        try {
            if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
                return Literal.makeLiteral(
                        Value.asValue(evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext())));
            }
        } catch (XPathException err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }

    private Expression createConversionCode(
            Expression operand, final TypeHierarchy th, AtomicType type) {
        if (Cardinality.allowsMany(operand.getCardinality())) {               
            FirstItemExpression fie = new FirstItemExpression(operand);
            ExpressionTool.copyLocationInfo(this, fie);
            operand = fie;
        }

        if (th.isSubType(type, BuiltInAtomicType.DOUBLE) ||
                th.isSubType(type, BuiltInAtomicType.DATE) ||
                th.isSubType(type, BuiltInAtomicType.TIME) ||
                th.isSubType(type, BuiltInAtomicType.DATE_TIME) ||
                th.isSubType(type, BuiltInAtomicType.DURATION)) {
            return operand;
        }
        if (th.isSubType(type, BuiltInAtomicType.BOOLEAN) ||
                th.isSubType(type, BuiltInAtomicType.STRING) ||
                th.isSubType(type, BuiltInAtomicType.UNTYPED_ATOMIC) ||
                th.isSubType(type, BuiltInAtomicType.FLOAT) ||
                th.isSubType(type, BuiltInAtomicType.DECIMAL)) {
            if (operand instanceof Literal) {
                Value val = ((Literal)operand).getValue();
                return new Literal(NumberFn.convert((AtomicValue)val));
            } else {
                return SystemFunction.makeSystemFunction("number", new Expression[]{operand});
            }
        }
        // If we can't determine the primitive type at compile time, we generate a run-time typeswitch

        LetExpression let = new LetExpression();
        let.setRequiredType(SequenceType.OPTIONAL_ATOMIC);
        let.setVariableQName(new StructuredQName("nn", NamespaceConstant.SAXON, "nn" + let.hashCode()));
        let.setSequence(operand);

        LocalVariableReference var = new LocalVariableReference(let);
        Expression isDouble = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.DOUBLE, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isDecimal = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.DECIMAL, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isFloat = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.FLOAT, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isString = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.STRING, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isUntypedAtomic = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.UNTYPED_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isBoolean = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.BOOLEAN, StaticProperty.ALLOWS_ZERO_OR_ONE));

        Expression condition = new BooleanExpression(isDouble, Token.OR, isDecimal);
        condition = new BooleanExpression(condition, Token.OR, isFloat);
        condition = new BooleanExpression(condition, Token.OR, isString);
        condition = new BooleanExpression(condition, Token.OR, isUntypedAtomic);
        condition = new BooleanExpression(condition, Token.OR, isBoolean);

        var = new LocalVariableReference(let);
        NumberFn fn = (NumberFn)SystemFunction.makeSystemFunction("number", new Expression[]{var});

        var = new LocalVariableReference(let);
        var.setStaticType(SequenceType.SINGLE_ATOMIC, null, 0);
        Expression action = Choose.makeConditional(condition, fn, var);
        let.setAction(action);
        return let;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (calculator == null) {
            return BuiltInAtomicType.ANY_ATOMIC;  // type is not known statically
        } else {
            ItemType t1 = operand0.getItemType(th);
            if (!(t1 instanceof AtomicType)) {
                t1 = t1.getAtomizedItemType();
            }
            ItemType t2 = operand1.getItemType(th);
            if (!(t2 instanceof AtomicType)) {
                t2 = t2.getAtomizedItemType();
            }
            return calculator.getResultType((AtomicType) t1.getPrimitiveItemType(),
                    (AtomicType) t2.getPrimitiveItemType());
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        ArithmeticExpression10 a2 = new ArithmeticExpression10(operand0.copy(), operator, operand1.copy());
        a2.calculator = calculator;
        return a2;
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue v1 = (AtomicValue) operand0.evaluateItem(context);
        if (v1 == null) {
            return DoubleValue.NaN;
        }

        AtomicValue v2 = (AtomicValue) operand1.evaluateItem(context);
        if (v2 == null) {
            return DoubleValue.NaN;
        }

        return calculator.compute(v1, v2, context);
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
