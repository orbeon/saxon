package net.sf.saxon.expr;
import net.sf.saxon.functions.Position;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

import java.util.Comparator;



/**
* GeneralComparison: a boolean expression that compares two expressions
* for equals, not-equals, greater-than or less-than. This implements the operators
* =, !=, <, >, etc. This implementation is not used when in backwards-compatible mode
*/

public class GeneralComparison extends BinaryExpression {

    protected int singletonOperator;
    protected AtomicComparer comparer;

    /**
    * Create a relational expression identifying the two operands and the operator
    * @param p0 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
    * @param p1 the right-hand operand
    */

    public GeneralComparison(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
        singletonOperator = getSingletonOperator(op);
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Type-check the expression
    * @return the checked expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        operand0 = operand0.analyze(env, contextItemType);
        operand1 = operand1.analyze(env, contextItemType);

        // If either operand is statically empty, return false

        if (operand0 == EmptySequence.getInstance() || operand1 == EmptySequence.getInstance()) {
            return BooleanValue.FALSE;
        }

        // Neither operand needs to be sorted

        operand0 = ExpressionTool.unsorted(operand0, false);
        operand1 = ExpressionTool.unsorted(operand1, false);

        SequenceType atomicType = SequenceType.ATOMIC_SEQUENCE;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, false, role0, env);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, false, role1, env);

        ItemType t0 = operand0.getItemType();
        ItemType t1 = operand1.getItemType();

        int c0 = operand0.getCardinality();
        int c1 = operand1.getCardinality();

        if (t0 == Type.ANY_ATOMIC_TYPE || t0 == Type.UNTYPED_ATOMIC_TYPE ||
                t1 == Type.ANY_ATOMIC_TYPE || t1 == Type.UNTYPED_ATOMIC_TYPE ) {
            // then no static type checking is possible
        } else {
            int pt0 = t0.getPrimitiveType();
            int pt1 = t1.getPrimitiveType();
            if (!Type.isComparable(pt0, pt1)) {
                StaticError err = new StaticError(
                        "Cannot compare " + t0.toString(env.getNamePool()) +
                        " to " + t1.toString(env.getNamePool()));
                err.setErrorCode("XP0006");
                err.setIsTypeError(true);
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

            if (t0==Type.UNTYPED_ATOMIC_TYPE) {
                if (t1==Type.UNTYPED_ATOMIC_TYPE) {
                    e0 = new CastExpression(operand0, Type.STRING_TYPE, false);
                    adoptChildExpression(e0);
                    e1 = new CastExpression(operand1, Type.STRING_TYPE, false);
                    adoptChildExpression(e1);
                } else if (Type.isSubType(t1, Type.NUMBER_TYPE)) {
                    e0 = new CastExpression(operand0, Type.DOUBLE_TYPE, false);
                    adoptChildExpression(e0);
                } else {
                    e0 = new CastExpression(operand0, (AtomicType) t1, false);
                    adoptChildExpression(e0);
                }
            } else if (t1==Type.UNTYPED_ATOMIC_TYPE) {
                if (Type.isSubType(t0, Type.NUMBER_TYPE)) {
                    e1 = new CastExpression(operand1, Type.DOUBLE_TYPE, false);
                    adoptChildExpression(e1);
                } else {
                    e1 = new CastExpression(operand1, (AtomicType) t0, false);
                    adoptChildExpression(e1);
                }
            }

            ValueComparison vc = new ValueComparison(e0, singletonOperator, e1);
            ExpressionTool.copyLocationInfo(this, vc);
            vc.setParentExpression(getParentExpression());
            return vc.simplify(env).analyze(env, contextItemType);
        }

        Comparator comp = env.getCollation(env.getDefaultCollationName());
        if (comp==null) comp = CodepointCollator.getInstance();
        comparer = new AtomicComparer(comp);

        // Check if neither argument allows a sequence of >1

        if (!Cardinality.allowsMany(c0) && !Cardinality.allowsMany(c1)) {

            // Use a singleton comparison if both arguments are singletons

            SingletonComparison sc = new SingletonComparison(operand0, singletonOperator, operand1);
            ExpressionTool.copyLocationInfo(this, sc);
            sc.setParentExpression(getParentExpression());
            sc.setComparator(comparer);
            return sc.analyze(env, contextItemType);
        }

        // see if second argument is a singleton...

        if (!Cardinality.allowsMany(c0)) {

            // if first argument is a singleton, reverse the arguments
            //ManyToOneComparison mc = new ManyToOneComparison(operand1, Value.inverse(singletonOperator), operand0);
            GeneralComparison mc = getInverseComparison();
            ExpressionTool.copyLocationInfo(this, mc);
            mc.setParentExpression(getParentExpression());
            mc.comparer = comparer;
            return mc.analyze(env, contextItemType);
        }

        // look for (N to M = I)

        if (operand0 instanceof RangeExpression &&
                Type.isSubType(operand1.getItemType(), Type.INTEGER_TYPE) &&
                !Cardinality.allowsMany(operand1.getCardinality())) {
            Expression min = ((RangeExpression)operand0).operand0;
            Expression max = ((RangeExpression)operand0).operand1;
            if (operand1 instanceof Position &&
                    min instanceof IntegerValue &&
                    max instanceof IntegerValue) {
                PositionRange pr = new PositionRange((int)((IntegerValue)min).longValue(),
                                         (int)((IntegerValue)max).longValue());
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

        // If the operator is gt, ge, lt, le then replace X < Y by min(X) < max(Y)

        // This optimization is done only in the case where at least one of the
        // sequences is known to be purely numeric. It isn't safe if both sequences
        // contain untyped atomic values, because in that case, the type of the
        // comparison isn't known in advance. For example [(1, U1) < ("fred", U2)]
        // involves both string and numeric comparisons.


        if (operator != Token.EQUALS && operator != Token.NE &&
                (Type.isSubType(t0, Type.NUMBER_TYPE) || Type.isSubType(t1, Type.NUMBER_TYPE))) {

            Expression e0 = operand0;
            if (!Type.isSubType(t0, Type.NUMBER_TYPE)) {
                e0 = TypeChecker.staticTypeCheck(e0, SequenceType.NUMERIC_SEQUENCE,
                        false,
                        new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null), env);
            }
            Expression e1 = operand1;
            if (!Type.isSubType(t1, Type.NUMBER_TYPE)) {
                e1 = TypeChecker.staticTypeCheck(e1, SequenceType.NUMERIC_SEQUENCE,
                        false,
                        new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null), env);
            }
            MinimaxComparison mc = new MinimaxComparison(e0, operator, e1);
            ExpressionTool.copyLocationInfo(this, mc);
            mc.setParentExpression(getParentExpression());
            return mc.analyze(env, contextItemType);

        }


        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
            return (AtomicValue)evaluateItem(null);
        }

        return this;
    }

// --Commented out by Inspection START (16/12/04 14:39):
//    /**
//    * Issue warnings about backwards compatibility
//    */
//
//    private void issueWarnings(ItemType t1, ItemType t2, StaticContext env) {
//
//        // System.err.println("Check " + Type.getTypeName(t1) + " op " + Type.getTypeName(t2));
//
//        if (t1 instanceof NodeTest && t2.getPrimitiveType() == Type.BOOLEAN) {
//            env.issueWarning("Comparison of a node-set to a boolean has changed since XPath 1.0", this);
//        }
//
//        if (t1.getPrimitiveType() == Type.BOOLEAN && t2 instanceof NodeTest) {
//            env.issueWarning("Comparison of a boolean to a node-set has changed since XPath 1.0", this);
//        }
//
//        if ((t1 instanceof NodeTest || t1.getPrimitiveType() == Type.STRING) &&
//            (t2 instanceof NodeTest || t2.getPrimitiveType() == Type.STRING) &&
//            (operator==Token.LT || operator==Token.LE || operator==Token.GT || operator==Token.GE )) {
//            env.issueWarning("Less-than and greater-than comparisons between strings have changed since XPath 1.0", this);
//        }
//    }
// --Commented out by Inspection STOP (16/12/04 14:39)

    /**
    * Evaluate the expression in a given context
    * @param context the given context for evaluation
    * @return a BooleanValue representing the result of the numeric comparison of the two operands
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression in a boolean context
    * @param context the given context for evaluation
    * @return a boolean representing the result of the numeric comparison of the two operands
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        SequenceIterator iter1 = operand0.iterate(context);
        SequenceIterator iter2 = operand1.iterate(context);

        Value seq2 = SequenceExtent.makeSequenceExtent(iter2);
                // we choose seq2 because it's more likely to be a singleton
        int count2 = seq2.getLength();

        if (count2 == 0) {
            return false;
        }

        if (count2 == 1) {
            AtomicValue s2 = (AtomicValue)seq2.itemAt(0);
            while (true) {
                AtomicValue s1 = (AtomicValue)iter1.next();
                if (s1 == null) break;
                try {
                    if (compare(s1, singletonOperator, s2, comparer, context)) {
                        return true;
                    }
                } catch (DynamicError e) {
                    // re-throw the exception with location information added
                    if (e.getXPathContext() == null) {
                        e.setXPathContext(context);
                    }
                    if (e.getLocator() == null) {
                        e.setLocator(this);
                    }
                    throw e;
                }
            }
            return false;
        }

        while (true) {
            AtomicValue s1 = (AtomicValue)iter1.next();
            if (s1 == null) break;
            SequenceIterator e2 = seq2.iterate(null);
            while (true) {
                AtomicValue s2 = (AtomicValue)e2.next();
                if (s2 == null) break;
                try {
                    if (compare(s1, singletonOperator, s2, comparer, context)) {
                        return true;
                    }
                } catch (DynamicError e) {
                    // re-throw the exception with location information added
                    if (e.getXPathContext() == null) {
                        e.setXPathContext(context);
                    }
                    if (e.getLocator() == null) {
                        e.setLocator(this);
                    }
                    throw e;
                }
            }
        }

        return false;

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
                v1 = a1.convert(Type.DOUBLE);
            } else if (a2 instanceof UntypedAtomicValue) {
                // the spec says convert it to a string, but this doesn't affect the outcome
            } else {
                v1 = a1.convert(a2.getItemType().getPrimitiveType());
            }
        }
        if (a2 instanceof UntypedAtomicValue) {
            if (a1 instanceof NumericValue) {
                v2 = a2.convert(Type.DOUBLE);
            } else if (a1 instanceof UntypedAtomicValue) {
                // the spec says convert it to a string, but this doesn't affect the outcome
            } else {
                v2 = a2.convert(a1.getItemType().getPrimitiveType());
            }
        }
        return ValueComparison.compare(v1, operator, v2, comparer);
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
    */

    public ItemType getItemType() {
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
