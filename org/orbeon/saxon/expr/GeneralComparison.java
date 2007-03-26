package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.GenericAtomicComparer;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.AtomicComparer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.functions.Position;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.functions.Minimax;

import java.util.Comparator;


/**
 * GeneralComparison: a boolean expression that compares two expressions
 * for equals, not-equals, greater-than or less-than. This implements the operators
 * =, !=, <, >, etc. This implementation is not used when in backwards-compatible mode
 */

public class GeneralComparison extends BinaryExpression implements ComparisonExpression {

    protected int singletonOperator;
    protected transient AtomicComparer comparer;  // transient because not Serializable

    /**
     * Create a relational expression identifying the two operands and the operator
     *
     * @param p0 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p1 the right-hand operand
     */

    public GeneralComparison(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
        singletonOperator = getSingletonOperator(op);
    }

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used
     */

    public AtomicComparer getAtomicComparer() {
        return comparer;
    }

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator() {
        return singletonOperator;
    }

    /**
     * Determine whether untyped atomic values should be converted to the type of the other operand
     * @return true if untyped values should be converted to the type of the other operand, false if they
     * should be converted to strings.
     */

    public boolean convertsUntypedToOther() {
        return true;
    }    

    /**
     * Determine the static cardinality. Returns [1..1]
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Type-check the expression
     *
     * @return the checked expression
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = operand0.typeCheck(env, contextItemType);
        operand1 = operand1.typeCheck(env, contextItemType);

        // If either operand is statically empty, return false

        if (operand0 == EmptySequence.getInstance() || operand1 == EmptySequence.getInstance()) {
            return BooleanValue.FALSE;
        }

        // Neither operand needs to be sorted

        Optimizer opt = env.getConfiguration().getOptimizer();
        operand0 = ExpressionTool.unsorted(opt, operand0, false);
        operand1 = ExpressionTool.unsorted(opt, operand1, false);

        SequenceType atomicType = SequenceType.ATOMIC_SEQUENCE;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, false, role0, env);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, false, role1, env);

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        ItemType t0 = operand0.getItemType(th);  // this is always an atomic type
        ItemType t1 = operand1.getItemType(th);  // this is always an atomic type

        int pt0 = t0.getPrimitiveType();
        int pt1 = t1.getPrimitiveType();

        int c0 = operand0.getCardinality();
        int c1 = operand1.getCardinality();

        if (c0 == StaticProperty.EMPTY || c1 == StaticProperty.EMPTY) {
            return BooleanValue.FALSE;
        }

        if (t0 == Type.ANY_ATOMIC_TYPE || t0 == Type.UNTYPED_ATOMIC_TYPE ||
                t1 == Type.ANY_ATOMIC_TYPE || t1 == Type.UNTYPED_ATOMIC_TYPE) {
            // then no static type checking is possible
        } else {

            if (!Type.isComparable(pt0, pt1, Token.isOrderedOperator(singletonOperator))) {
                StaticError err = new StaticError("Cannot compare " + t0.toString(env.getNamePool()) +
                        " to " + t1.toString(env.getNamePool()));
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
        }

        if (c0 == StaticProperty.EXACTLY_ONE &&
                c1 == StaticProperty.EXACTLY_ONE &&
                t0 != Type.ANY_ATOMIC_TYPE &&
                t1 != Type.ANY_ATOMIC_TYPE) {

            // Use a value comparison if both arguments are singletons, and if the comparison operator to
            // be used can be determined.

            Expression e0 = operand0;
            Expression e1 = operand1;

            if (t0 == Type.UNTYPED_ATOMIC_TYPE) {
                if (t1 == Type.UNTYPED_ATOMIC_TYPE) {
                    e0 = new CastExpression(operand0, Type.STRING_TYPE, false);
                    adoptChildExpression(e0);
                    e1 = new CastExpression(operand1, Type.STRING_TYPE, false);
                    adoptChildExpression(e1);
                } else if (th.isSubType(t1, Type.NUMBER_TYPE)) {
                    e0 = new CastExpression(operand0, Type.DOUBLE_TYPE, false);
                    adoptChildExpression(e0);
                } else {
                    e0 = new CastExpression(operand0, (AtomicType)t1, false);
                    adoptChildExpression(e0);
                }
            } else if (t1 == Type.UNTYPED_ATOMIC_TYPE) {
                if (th.isSubType(t0, Type.NUMBER_TYPE)) {
                    e1 = new CastExpression(operand1, Type.DOUBLE_TYPE, false);
                    adoptChildExpression(e1);
                } else {
                    e1 = new CastExpression(operand1, (AtomicType)t0, false);
                    adoptChildExpression(e1);
                }
            }

            ValueComparison vc = new ValueComparison(e0, singletonOperator, e1);
            ExpressionTool.copyLocationInfo(this, vc);
            vc.setParentExpression(getParentExpression());
            return vc.simplify(env).typeCheck(env, contextItemType);
        }

        Comparator collation = env.getCollation(env.getDefaultCollationName());
        if (collation == null) {
            collation = CodepointCollator.getInstance();
        }
        comparer = GenericAtomicComparer.makeAtomicComparer(pt0, pt1, collation, env.getConfiguration());


        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
            return (AtomicValue)evaluateItem(env.makeEarlyEvaluationContext());
        }

        return this;
    }

    private static Expression makeMinOrMax(Expression exp, StaticContext env, String function) {
        FunctionCall fn = SystemFunction.makeSystemFunction(function, 1, env.getNamePool());
        Expression[] args = {exp};
        fn.setArguments(args);
        ((Minimax)fn).setIgnoreNaN(true);
        return fn;
    }

    /**
     * Optimize the expression
     *
     * @return the checked expression
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();

        operand0 = operand0.optimize(opt, env, contextItemType);
        operand1 = operand1.optimize(opt, env, contextItemType);

        // If either operand is statically empty, return false

        if (operand0 == EmptySequence.getInstance() || operand1 == EmptySequence.getInstance()) {
            return BooleanValue.FALSE;
        }

        // Neither operand needs to be sorted

        operand0 = ExpressionTool.unsorted(opt, operand0, false);
        operand1 = ExpressionTool.unsorted(opt, operand1, false);

        ItemType t0 = operand0.getItemType(th);
        ItemType t1 = operand1.getItemType(th);

        int c0 = operand0.getCardinality();
        int c1 = operand1.getCardinality();

        // Check if neither argument allows a sequence of >1

        if (!Cardinality.allowsMany(c0) && !Cardinality.allowsMany(c1)) {

            // Use a singleton comparison if both arguments are singletons

            SingletonComparison sc = new SingletonComparison(operand0, singletonOperator, operand1);
            ExpressionTool.copyLocationInfo(this, sc);
            sc.setParentExpression(getParentExpression());
            sc.setComparator(comparer, env.makeEarlyEvaluationContext());
            return sc.optimize(opt, env, contextItemType);
        }

        // see if second argument is a singleton...

        if (!Cardinality.allowsMany(c0)) {

            // if first argument is a singleton, reverse the arguments
            GeneralComparison mc = getInverseComparison();
            ExpressionTool.copyLocationInfo(this, mc);
            mc.setParentExpression(getParentExpression());
            mc.comparer = comparer;
            return mc.optimize(opt, env, contextItemType);
        }


        if (c0 == StaticProperty.EXACTLY_ONE &&
                c1 == StaticProperty.EXACTLY_ONE &&
                t0 != Type.ANY_ATOMIC_TYPE &&
                t1 != Type.ANY_ATOMIC_TYPE) {

            // Use a value comparison if both arguments are singletons, and if the comparison operator to
            // be used can be determined.

            Expression e0 = operand0;
            Expression e1 = operand1;

            if (t0 == Type.UNTYPED_ATOMIC_TYPE) {
                if (t1 == Type.UNTYPED_ATOMIC_TYPE) {
                    e0 = new CastExpression(operand0, Type.STRING_TYPE, false);
                    adoptChildExpression(e0);
                    e1 = new CastExpression(operand1, Type.STRING_TYPE, false);
                    adoptChildExpression(e1);
                } else if (th.isSubType(t1, Type.NUMBER_TYPE)) {
                    e0 = new CastExpression(operand0, Type.DOUBLE_TYPE, false);
                    adoptChildExpression(e0);
                } else {
                    e0 = new CastExpression(operand0, (AtomicType)t1, false);
                    adoptChildExpression(e0);
                }
            } else if (t1 == Type.UNTYPED_ATOMIC_TYPE) {
                if (th.isSubType(t0, Type.NUMBER_TYPE)) {
                    e1 = new CastExpression(operand1, Type.DOUBLE_TYPE, false);
                    adoptChildExpression(e1);
                } else {
                    e1 = new CastExpression(operand1, (AtomicType)t0, false);
                    adoptChildExpression(e1);
                }
            }

            ValueComparison vc = new ValueComparison(e0, singletonOperator, e1);
            ExpressionTool.copyLocationInfo(this, vc);
            vc.setParentExpression(getParentExpression());
            return vc.simplify(env).typeCheck(env, contextItemType).optimize(opt, env, contextItemType);
        }

        Comparator comp = env.getCollation(env.getDefaultCollationName());
        if (comp == null) {
            comp = CodepointCollator.getInstance();
        }
        int pt0 = t0.getPrimitiveType();
        int pt1 = t1.getPrimitiveType();
        comparer = GenericAtomicComparer.makeAtomicComparer(pt0, pt1, comp, env.getConfiguration());

        // If the operator is gt, ge, lt, le then replace X < Y by min(X) < max(Y) or
        // by (some $x in X satisfies $x lt Y)

        // This optimization is done only in the case where at least one of the
        // sequences is known to be purely numeric. It isn't safe if both sequences
        // contain untyped atomic values, because in that case, the type of the
        // comparison isn't known in advance. For example [(1, U1) < ("fred", U2)]
        // involves both string and numeric comparisons.


        if (operator != Token.EQUALS && operator != Token.NE &&
                (th.isSubType(t0, Type.NUMBER_TYPE) || th.isSubType(t1, Type.NUMBER_TYPE))) {

            Expression e0 = operand0;
            if (!th.isSubType(t0, Type.NUMBER_TYPE)) {
                RoleLocator role = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
                role.setSourceLocator(this);
                e0 = TypeChecker.staticTypeCheck(e0, SequenceType.NUMERIC_SEQUENCE, false, role, env);
            }
            Expression e1 = operand1;
            if (!th.isSubType(t1, Type.NUMBER_TYPE)) {
                RoleLocator role = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
                role.setSourceLocator(this);
                e1 = TypeChecker.staticTypeCheck(e1, SequenceType.NUMERIC_SEQUENCE, false, role, env);
            }

            if (c1 == StaticProperty.EXACTLY_ONE) {

                // If second operand is a singleton, rewrite as
                //      some $x in E0 satisfies $x op E1

                // System.err.println("** using quantified optimization R **");
                RangeVariableDeclaration decl = new RangeVariableDeclaration();
                decl.setVariableName("qq:" + decl.hashCode());
                SequenceType type = SequenceType.makeSequenceType(e0.getItemType(th), StaticProperty.EXACTLY_ONE);
                decl.setRequiredType(type);

                VariableReference var = new VariableReference(decl);
                ValueComparison vc = new ValueComparison(var, singletonOperator, e1);
                QuantifiedExpression qe = new QuantifiedExpression();
                qe.setOperator(Token.SOME);
                qe.setVariableDeclaration(decl);
                qe.setSequence(e0);
                qe.setAction(vc);
                qe.setLocationId(getLocationId());
                qe.setParentExpression(getParentExpression());
                return qe.typeCheck(env, contextItemType);

            } else {

                // If neither operand is known to be a singleton, rewrite as (for example)
                // min(E0) op max(E1)

                // System.err.println("** using minimax optimization **");
                ValueComparison vc;
                switch(operator) {
                    case Token.LT:
                    case Token.LE:
                        vc = new ValueComparison(makeMinOrMax(e0, env, "min"),
                                singletonOperator,
                                makeMinOrMax(e1, env, "max"));
                        vc.setResultWhenEmpty(BooleanValue.FALSE);
                        break;
                    case Token.GT:
                    case Token.GE:
                        vc = new ValueComparison(makeMinOrMax(e0, env, "max"),
                                singletonOperator,
                                makeMinOrMax(e1, env, "min"));
                        vc.setResultWhenEmpty(BooleanValue.FALSE);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown operator " + operator);
                }

                ExpressionTool.copyLocationInfo(this, vc);
                vc.setParentExpression(getParentExpression());
                return vc.typeCheck(env, contextItemType);
            }
        }


        // look for (N to M = I)


        if (operand0 instanceof RangeExpression &&
                th.isSubType(operand1.getItemType(th), Type.INTEGER_TYPE) &&
                !Cardinality.allowsMany(operand1.getCardinality())) {
            Expression min = ((RangeExpression)operand0).operand0;
            Expression max = ((RangeExpression)operand0).operand1;
            if (operand1 instanceof Position) {
                PositionRange pr = new PositionRange(min, max);
                ExpressionTool.copyLocationInfo(this, pr);
                pr.setParentExpression(getParentExpression());
                return pr;
            } else {
                IntegerRangeTest ir = new IntegerRangeTest(operand1, min, max);
                ExpressionTool.copyLocationInfo(this, ir);
                ir.setParentExpression(getParentExpression());
                return ir;
            }
        }

        if (operand0 instanceof IntegerRange &&
                th.isSubType(operand1.getItemType(th), Type.INTEGER_TYPE) &&
                !Cardinality.allowsMany(operand1.getCardinality())) {
            long min = ((IntegerRange)operand0).getStart();
            long max = ((IntegerRange)operand0).getEnd();
            if (operand1 instanceof Position) {
                PositionRange pr = new PositionRange(new IntegerValue(min), new IntegerValue(max));
                ExpressionTool.copyLocationInfo(this, pr);
                pr.setParentExpression(getParentExpression());
                return pr;
            } else {
                IntegerRangeTest ir = new IntegerRangeTest(operand1, new IntegerValue(min), new IntegerValue(max));
                ExpressionTool.copyLocationInfo(this, ir);
                ir.setParentExpression(getParentExpression());
                return ir;
            }
        }

        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
            return (AtomicValue)evaluateItem(env.makeEarlyEvaluationContext());
        }

        return this;
    }


    /**
     * Evaluate the expression in a given context
     *
     * @param context the given context for evaluation
     * @return a BooleanValue representing the result of the numeric comparison of the two operands
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
     * Evaluate the expression in a boolean context
     *
     * @param context the given context for evaluation
     * @return a boolean representing the result of the numeric comparison of the two operands
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        try {
            SequenceIterator iter1 = operand0.iterate(context);
            SequenceIterator iter2 = operand1.iterate(context);

            Value seq2 = (Value)SequenceExtent.makeSequenceExtent(iter2);
            // we choose seq2 because it's more likely to be a singleton
            int count2 = seq2.getLength();

            if (count2 == 0) {
                return false;
            }

            if (count2 == 1) {
                AtomicValue s2 = (AtomicValue)seq2.itemAt(0);
                while (true) {
                    AtomicValue s1 = (AtomicValue)iter1.next();
                    if (s1 == null) {
                        break;
                    }
                    if (compare(s1, singletonOperator, s2, comparer, context)) {
                        return true;
                    }
                }
                return false;
            }

            while (true) {
                AtomicValue s1 = (AtomicValue)iter1.next();
                if (s1 == null) {
                    break;
                }
                SequenceIterator e2 = seq2.iterate(null);
                while (true) {
                    AtomicValue s2 = (AtomicValue)e2.next();
                    if (s2 == null) {
                        break;
                    }
                    if (compare(s1, singletonOperator, s2, comparer, context)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (DynamicError e) {
            // re-throw the exception with location information added
            if (e.getXPathContext() == null) {
                e.setXPathContext(context);
            }
            if (e.getLocator() == null) {
                e.setLocator(this);
            }
            throw e;
        } catch (ValidationException e) {
            DynamicError err = new DynamicError(e);
            err.setXPathContext(context);
            if (e.getLineNumber() == -1) {
                err.setLocator(this);
            } else {
                err.setLocator(e);
            }
            err.setErrorCode(e.getErrorCodeLocalPart());
            throw err;
        }

    }

    /**
     * Compare two atomic values
     */

    protected static boolean compare(AtomicValue a1,
                                     int operator,
                                     AtomicValue a2,
                                     AtomicComparer comparer,
                                     XPathContext context) throws XPathException {

        AtomicValue v1 = a1;
        AtomicValue v2 = a2;
        if (a1 instanceof UntypedAtomicValue) {
            if (a2 instanceof NumericValue) {
                v1 = a1.convert(Type.DOUBLE, context);
            } else if (a2 instanceof UntypedAtomicValue) {
                // the spec says convert it to a string, but this doesn't affect the outcome
            } else {
                final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
                v1 = a1.convert(a2.getItemType(th).getPrimitiveType(), context);
            }
        }
        if (a2 instanceof UntypedAtomicValue) {
            if (a1 instanceof NumericValue) {
                v2 = a2.convert(Type.DOUBLE, context);
            } else if (a1 instanceof UntypedAtomicValue) {
                // the spec says convert it to a string, but this doesn't affect the outcome
            } else {
                final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
                v2 = a2.convert(a1.getItemType(th).getPrimitiveType(), context);
            }
        }
        return ValueComparison.compare(v1, operator, v2, comparer);

    }

    /**
     * Determine the data type of the expression
     *
     * @return Type.BOOLEAN
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.BOOLEAN_TYPE;
    }

    /**
     * Return the singleton form of the comparison operator, e.g. FEQ for EQUALS, FGT for GT
     */

    private static int getSingletonOperator(int op) {
        switch (op) {
            case Token.EQUALS:
                return Token.FEQ;
            case Token.GE:
                return Token.FGE;
            case Token.NE:
                return Token.FNE;
            case Token.LT:
                return Token.FLT;
            case Token.GT:
                return Token.FGT;
            case Token.LE:
                return Token.FLE;
            default:
                return op;
        }
    }

    protected GeneralComparison getInverseComparison() {
        return new GeneralComparison(operand1, Token.inverse(operator), operand0);
    }

    protected String displayOperator() {
        return "many-to-many " + super.displayOperator();
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
