package net.sf.saxon.expr;
import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * GeneralComparison10: a boolean expression that compares two expressions
 * for equals, not-equals, greater-than or less-than. This implements the operators
 * =, !=, <, >, etc. This version of the class implements general comparisons
 * in XPath 1.0 backwards compatibility mode, as defined in the Oct 2004 revision
 * of the specifications.
*/

public class GeneralComparison10 extends BinaryExpression {

    protected int singletonOperator;
    protected AtomicComparer comparer;
    private boolean atomize0 = true;
    private boolean atomize1 = true;
    private boolean maybeBoolean0 = true;
    private boolean maybeBoolean1 = true;

    /**
    * Create a general comparison identifying the two operands and the operator
    * @param p0 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
    * @param p1 the right-hand operand
    */

    public GeneralComparison10(Expression p0, int op, Expression p1) {
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

        // Neither operand needs to be sorted

        operand0 = ExpressionTool.unsorted(operand0, false);
        operand1 = ExpressionTool.unsorted(operand1, false);

        Comparator comp = env.getCollation(env.getDefaultCollationName());
        if (comp==null) comp = CodepointCollator.getInstance();
        comparer = new AtomicComparer(comp);

        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
            return (AtomicValue)evaluateItem(null);
        }

        ItemType type0 = operand0.getItemType();
        ItemType type1 = operand1.getItemType();

        if (type0 instanceof AtomicType) {
            atomize0 = false;
        }
        if (type1 instanceof AtomicType) {
            atomize1 = false;
        }

        if (Type.relationship(type0, Type.BOOLEAN_TYPE) == Type.DISJOINT) {
            maybeBoolean0 = false;
        }
        if (Type.relationship(type1, Type.BOOLEAN_TYPE) == Type.DISJOINT) {
            maybeBoolean1 = false;
        }

        if (!maybeBoolean0 && !maybeBoolean1) {
            boolean maybeNumeric0 = Type.relationship(type0, Type.NUMBER_TYPE) != Type.DISJOINT;
            boolean maybeNumeric1 = Type.relationship(type1, Type.NUMBER_TYPE) != Type.DISJOINT;

            // Use the 2.0 path if we don't have to deal with the possibility of boolean values,
            // or the complications of converting values to numbers
            if (!maybeNumeric0 && !maybeNumeric1) {
                return new GeneralComparison(operand0, operator, operand1).analyze(env, contextItemType);
            }
        }

        return this;
    }

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

        // If the first operand is a singleton boolean,
        // compare it with the effective boolean value of the other operand

        SequenceIterator iter0 = null;

        if (maybeBoolean0) {
            iter0 = operand0.iterate(context);
            Item i01 = iter0.next();
            Item i02 = (i01 == null ? null : iter0.next());
            if (i01 instanceof BooleanValue && i02 == null) {
                boolean b = operand1.effectiveBooleanValue(context);
                return compare((BooleanValue)i01, singletonOperator, BooleanValue.get(b), comparer, context);
            }
            if (i01 == null && !maybeBoolean1) {
                return false;
            }
        }

        // If the second operand is a singleton boolean,
        // compare it with the effective boolean value of the other operand

        SequenceIterator iter1 = null;

        if (maybeBoolean1) {
            iter1 = operand1.iterate(context);
            Item i11 = iter1.next();
            Item i12 = (i11 == null ? null : iter1.next());
            if (i11 instanceof BooleanValue && i12 == null) {
                boolean b = operand0.effectiveBooleanValue(context);
                return compare(BooleanValue.get(b), singletonOperator, (BooleanValue)i11, comparer, context);
            }
            if (i11 == null && !maybeBoolean0) {
                return false;
            }
        }

        // Atomize both operands where necessary

        if (iter0 == null) {
            iter0 = operand0.iterate(context);
        } else {
            iter0 = iter0.getAnother();
        }

        if (iter1 == null) {
            iter1 = operand1.iterate(context);
        } else {
            iter1 = iter1.getAnother();
        }

        if (atomize0) {
            iter0 = Atomizer.AtomizingFunction.getAtomizingIterator(iter0);
        }

        if (atomize1) {
            iter1 = Atomizer.AtomizingFunction.getAtomizingIterator(iter1);
        }

        // If the operator is one of <, >, <=, >=, then convert both operands to sequences of xs:double
        // using the number() function

        if (operator == Token.LT || operator == Token.LE || operator == Token.GT || operator == Token.GE) {
            iter0 = new MappingIterator(iter0, new NumberFn(), null, null);
            iter1 = new MappingIterator(iter1, new NumberFn(), null, null);
        }

        // Compare all pairs of atomic values in the two atomized sequences

        List seq1 = null;
        while (true) {
            AtomicValue item0 = (AtomicValue)iter0.next();
            if (item0 == null) {
                return false;
            }
            if (iter1 != null) {
                while (true) {
                    AtomicValue item1 = (AtomicValue)iter1.next();
                    if (item1 == null) {
                        iter1 = null;
                        if (seq1 == null) {
                            // second operand is empty
                            return false;
                        }
                        break;
                    }
                    try {
                        if (compare(item0, singletonOperator, item1, comparer, context)) {
                            return true;
                        }
                        if (seq1 == null) {
                            seq1 = new ArrayList(40);
                        }
                        seq1.add(item1);
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
            } else {
                Iterator listIter1 = seq1.iterator();
                while (listIter1.hasNext()) {
                    AtomicValue item1 = (AtomicValue)listIter1.next();
                    if (compare(item0, singletonOperator, item1, comparer, context)) {
                        return true;
                    }
                }
            }
        }
    }

    /**
    * Compare two atomic values
    */

    protected static boolean compare(AtomicValue a0,
                                     int operator,
                                     AtomicValue a1,
                                     AtomicComparer comparer,
                                     XPathContext context) throws XPathException {

        AtomicType t0 = (AtomicType)a0.getItemType().getPrimitiveItemType();
        AtomicType t1 = (AtomicType)a1.getItemType().getPrimitiveItemType();

        // If either operand is a number, convert both operands to xs:double using
        // the rules of the number() function, and compare them

        if (Type.isNumericPrimitiveType(t0) || Type.isNumericPrimitiveType(t1)) {
            DoubleValue v0 = NumberFn.convert(a0);
            DoubleValue v1 = NumberFn.convert(a1);
            return ValueComparison.compare(v0, operator, v1, comparer);
        }

        // If either operand is a string, or if both are untyped atomic, convert
        // both operands to strings and compare them

        if (t0 == Type.STRING_TYPE || t1 == Type.STRING_TYPE ||
                (t0 == Type.UNTYPED_ATOMIC_TYPE && t1 == Type.UNTYPED_ATOMIC_TYPE)) {
            StringValue s0 = (StringValue)a0.convert(Type.STRING);
            StringValue s1 = (StringValue)a1.convert(Type.STRING);
            return ValueComparison.compare(s0, operator, s1, comparer);
        }

        // If either operand is untyped atomic,
        // convert it to the type of the other operand, and compare

        if (t0 == Type.UNTYPED_ATOMIC_TYPE) {
            a0 = a0.convert(t1, context, true);
            if (a0 instanceof ErrorValue) {
                throw ((ErrorValue)a0).getException();
            }
        }

        if (t1 == Type.UNTYPED_ATOMIC_TYPE) {
            a1 = a1.convert(t0, context, true);
            if (a1 instanceof ErrorValue) {
                throw ((ErrorValue)a1).getException();
            }
        }

        return ValueComparison.compare(a0, operator, a1, comparer);
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

    protected String displayOperator() {
        return "many-to-many (1.0) " + super.displayOperator();
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
