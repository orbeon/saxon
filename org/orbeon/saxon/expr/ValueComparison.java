package net.sf.saxon.expr;
import net.sf.saxon.functions.*;
import net.sf.saxon.om.Item;
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
* ValueComparison: a boolean expression that compares two atomic values
* for equals, not-equals, greater-than or less-than. Implements the operators
* eq, ne, lt, le, gt, ge
*/

public final class ValueComparison extends BinaryExpression {

    private AtomicComparer comparer;

    /**
    * Create a relational expression identifying the two operands and the operator
    * @param p1 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
    * @param p2 the right-hand operand
    */

    public ValueComparison(Expression p1, int op, Expression p2) {
        super(p1, op, p2);
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {

        operand0 = operand0.analyze(env, contextItemType);
        if (operand0 instanceof EmptySequence) {
            return operand0;
        }
        operand1 = operand1.analyze(env, contextItemType);
        if (operand1 instanceof EmptySequence) {
            return operand1;
        }

        final SequenceType optionalAtomic = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        operand0 = TypeChecker.staticTypeCheck(operand0, optionalAtomic, false, role0, env);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        operand1 = TypeChecker.staticTypeCheck(operand1, optionalAtomic, false, role1, env);

        AtomicType t1 = operand0.getItemType().getAtomizedItemType();
        AtomicType t2 = operand1.getItemType().getAtomizedItemType();

        int p1 = t1.getPrimitiveType();
        if (p1 == Type.UNTYPED_ATOMIC) {
            p1 = Type.STRING;
        }
        int p2 = t2.getPrimitiveType();
        if (p2 == Type.UNTYPED_ATOMIC) {
            p2 = Type.STRING;
        }

        if (!Type.isComparable(p1, p2)) {
            boolean opt0 = Cardinality.allowsZero(operand0.getCardinality());
            boolean opt1 = Cardinality.allowsZero(operand1.getCardinality());
            if (opt0 || opt1) {
                // This is a comparison such as (xs:integer? eq xs:date?). This is almost
                // certainly an error, but we need to let it through because it will work if
                // one of the operands is an empty sequence.

                String which = null;
                if (opt0) which = "the first operand is";
                if (opt1) which = "the second operand is";
                if (opt0 && opt1) which = "one or both operands are";
                env.issueWarning(
                        "Comparison of " + t1.toString(env.getNamePool()) + (opt0?"?":"") + " to " + t2.toString(env.getNamePool()) +
                        (opt1?"?":"") + " will fail unless " + which + " empty", this);

            } else {
                StaticError err =
                    new StaticError("Cannot compare " + t1.toString(env.getNamePool()) +
                                              " to " + t2.toString(env.getNamePool()));
                err.setIsTypeError(true);
                err.setErrorCode("XP0006");
                throw err;
            }
        }
        if (!(operator == Token.FEQ || operator == Token.FNE)) {
            if (!Type.isOrdered(p1)) {
                StaticError err = new StaticError(
                        "Type " + t1.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XP0004");
                err.setIsTypeError(true);
                throw err;
            }
            if (!Type.isOrdered(p2)) {
                StaticError err = new StaticError(
                        "Type " + t2.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XP0004");
                err.setIsTypeError(true);
                throw err;
            }
        }

        Comparator comp = env.getCollation(env.getDefaultCollationName());
        if (comp==null) comp = CodepointCollator.getInstance();
        comparer = new AtomicComparer(comp);

        // optimise count(x) eq 0 (or gt 0, ne 0, eq 0, etc)

        if (Aggregate.isCountFunction(operand0) &&
                operand1 instanceof AtomicValue) {
            if (isZero(operand1)) {
                if (operator == Token.FEQ || operator == Token.FLE ) {
                    // rewrite count(x)=0 as empty(x)
                    FunctionCall fn = SystemFunction.makeSystemFunction("empty", 1, env.getNamePool());
                    Expression[] args = new Expression[1];
                    args[0] = ((FunctionCall)operand0).argument[0];
                    fn.setArguments(args);
                    return fn;
                } else if (operator == Token.FNE || operator == Token.FGT) {
                    // rewrite count(x)!=0, count(x)>0 as exists(x)
                    FunctionCall fn = SystemFunction.makeSystemFunction("exists", 1, env.getNamePool());
                    Expression[] args = new Expression[1];
                    args[0] = ExpressionTool.unsorted(((FunctionCall)operand0).argument[0], false);
                    fn.setArguments(args);
                    return fn;
                } else if (operator == Token.FGE) {
                    // rewrite count(x)>=0 as true()
                    return BooleanValue.TRUE;
                } else {  // singletonOperator == Token.FLT
                    // rewrite count(x)<0 as false()
                    return BooleanValue.FALSE;
                }
            } else if (operand1 instanceof IntegerValue &&
                    (operator == Token.FGT || operator == Token.FGE) ) {
                // rewrite count(x) gt n as exists(x[n+1])
                //     and count(x) ge n as exists(x[n])
                long val = ((IntegerValue)operand1).longValue();
                if (operator == Token.FGT) {
                    val++;
                }
                FunctionCall fn = SystemFunction.makeSystemFunction("exists", 1, env.getNamePool());
                Expression[] args = new Expression[1];
                FilterExpression filter =
                        new FilterExpression(((FunctionCall)operand0).argument[0],
                                new IntegerValue(val), env);
                args[0] = filter;
                fn.setArguments(args);
                return fn;
            }
        }

        // optimise (0 eq count(x)), etc

        if (Aggregate.isCountFunction(operand1) && isZero(operand0)) {
        	Expression s =
        	    new ValueComparison(operand1, Token.inverse(operator), operand0).analyze(env, contextItemType);
			//((ValueComparison)s).defaultCollation = defaultCollation;
			return s;
        }

        // optimise string-length(x) = 0, >0, !=0 etc

        if ((operand0 instanceof StringLength) &&
        	    (((StringLength)operand0).getNumberOfArguments()==1) && isZero(operand1)) {
            ((StringLength)operand0).setShortcut();
        }

        // optimise (0 = string-length(x)), etc

        if ((operand1 instanceof StringLength) &&
                (((StringLength)operand1).getNumberOfArguments()==1) && isZero(operand0)) {
            ((StringLength)operand1).setShortcut();
        }

        // optimise [position() < n] etc

        if ((operand0 instanceof Position) && (operand1 instanceof IntegerValue)) {
            int pos = (int)((IntegerValue)operand1).longValue();
            if (pos < 0) {
                pos = 0;
            }
            switch (operator) {
                case Token.FEQ:
                    return new PositionRange(pos, pos);
                case Token.FGE:
                    return new PositionRange(pos, Integer.MAX_VALUE);
                case Token.FNE:
                    if (pos==1) {
                        return new PositionRange(2, Integer.MAX_VALUE);
                    } else {
                        break;
                    }
                case Token.FLT:
                    return new PositionRange(1, pos-1);
                case Token.FGT:
                    return new PositionRange(pos+1, Integer.MAX_VALUE);
                case Token.FLE:
                    return new PositionRange(1, pos);
            }
        }

        if ((operand0 instanceof IntegerValue) && (operand1 instanceof Position)) {
            long pos = ((IntegerValue)operand0).longValue();
            if (pos < 0) {
                pos = 0;
            }
            if (pos < Integer.MAX_VALUE) {
                switch (operator) {
                    case Token.FEQ:
                        return new PositionRange((int)pos, (int)pos);
                    case Token.FLE:
                        return new PositionRange((int)pos, Integer.MAX_VALUE);
                    case Token.FNE:
                       if (pos==1) {
                            return new PositionRange(2, Integer.MAX_VALUE);
                        } else {
                            break;
                        }
                    case Token.FGT:
                        return new PositionRange(1, (int)pos - 1 );
                    case Token.FLT:
                        return new PositionRange((int)pos + 1, Integer.MAX_VALUE);
                    case Token.FGE:
                        return new PositionRange(1, (int)pos);
                }
            }
        }

        // optimise [position()=last()] etc

        if ((operand0 instanceof Position) && (operand1 instanceof Last)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FGE:
                    return new IsLastExpression(true);
                case Token.FNE:
                case Token.FLT:
                    return new IsLastExpression(false);
                case Token.FGT:
                    return BooleanValue.FALSE;
                case Token.FLE:
                    return BooleanValue.TRUE;
            }
        }
        if ((operand0 instanceof Last) && (operand1 instanceof Position)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FLE:
                    return new IsLastExpression(true);
                case Token.FNE:
                case Token.FGT:
                    return new IsLastExpression(false);
                case Token.FLT:
                    return BooleanValue.FALSE;
                case Token.FGE:
                    return BooleanValue.TRUE;
            }
        }

        // optimize generate-id(X) = generate-id(Y) as "X is Y"
        // This construct is often used in XSLT 1.0 stylesheets.
        // Only do this if we know the arguments are singletons, because "is" doesn't
        // do first-value extraction.

        if (NamePart.isGenerateIdFunction(operand0) && NamePart.isGenerateIdFunction(operand1)) {
            FunctionCall f0 = (FunctionCall)operand0;
            FunctionCall f1 = (FunctionCall)operand1;
            if (!Cardinality.allowsMany(f0.argument[0].getCardinality()) &&
                    !Cardinality.allowsMany(f1.argument[0].getCardinality()) &&
                    (operator == Token.FEQ) ) {
                IdentityComparison id =
                        new IdentityComparison (
                                f0.argument[0],
                                Token.IS,
                                f1.argument[0] );
                id.setGenerateIdEmulation(true);
                return id.simplify(env).analyze(env, contextItemType);
            }
        }

        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Value) && (operand1 instanceof Value)) {
            return (AtomicValue)evaluateItem(null);
        }

        return this;
    }

    /**
    * Test whether an expression is constant zero
    */

    private static boolean isZero(Expression exp) {
        try {
            if (!(exp instanceof AtomicValue)) return false;
            if (exp instanceof IntegerValue) {
                return ((IntegerValue)exp).longValue()==0;
            }
            if (exp instanceof BigIntegerValue) {
                return ((BigIntegerValue)exp).compareTo(BigIntegerValue.ZERO) == 0;
            }

            Value val = ((AtomicValue)exp).convert(Type.INTEGER);
            return isZero(val);
        } catch (XPathException err) {
            return false;
        }
    }

    /**
    * Evaluate the expression in a boolean context
    * @param context the given context for evaluation
    * @return a boolean representing the result of the numeric comparison of the two operands
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        try {
            AtomicValue v1 = (AtomicValue)operand0.evaluateItem(context);
            if (v1==null) return false;
            if (v1 instanceof UntypedAtomicValue) {
                v1 = v1.convert(Type.STRING);
            }
            AtomicValue v2 = (AtomicValue)operand1.evaluateItem(context);
            if (v2==null) return false;
            if (v2 instanceof UntypedAtomicValue) {
                v2 = v2.convert(Type.STRING);
            }
            return compare(v1, operator, v2, comparer);
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

    /**
    * Compare two atomic values, using a specified operator and collation
     * @param v1 the first operand
     * @param op the operator, as defined by constants such as {@link Token#FEQ} or
     * {@link Token#FLT}
     * @param v2 the second operand
     * @param collator the Collator to be used when comparing strings
    * @throws net.sf.saxon.trans.DynamicError if the values are not comparable
    */

    static boolean compare(AtomicValue v1, int op, AtomicValue v2,
                                     AtomicComparer collator)
            throws DynamicError {
        if (v1 instanceof NumericValue && ((NumericValue)v1).isNaN()) {
            return false;
        }
        if (v2 instanceof NumericValue && ((NumericValue)v2).isNaN()) {
            return false;
        }
        try {
            switch (op) {
                case Token.FEQ:
                    return collator.comparesEqual(v1, v2);
                case Token.FNE:
                    return !collator.comparesEqual(v1, v2);
                case Token.FGT:
                    return collator.compare(v1, v2) > 0;
                case Token.FLT:
                    return collator.compare(v1, v2) < 0;
                case Token.FGE:
                    return collator.compare(v1, v2) >= 0;
                case Token.FLE:
                    return collator.compare(v1, v2) <= 0;
                default:
                    throw new UnsupportedOperationException("Unknown operator " + op);
            }
        } catch (ClassCastException err) {
            DynamicError e2 = new DynamicError (
                "Cannot compare " + v1.getItemType() + " to " + v2.getItemType());
            e2.setErrorCode("XP0006");
            e2.setIsTypeError(true);
            throw e2;
        }
    }

    /**
    * Evaluate the expression in a given context
    * @param context the given context for evaluation
    * @return a BooleanValue representing the result of the numeric comparison of the two operands,
     * or null representing the empty sequence
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
       try {
            AtomicValue v1 = (AtomicValue)operand0.evaluateItem(context);
            if (v1==null) return null;
            if (v1 instanceof UntypedAtomicValue) {
                v1 = v1.convert(Type.STRING);
            }
            AtomicValue v2 = (AtomicValue)operand1.evaluateItem(context);
            if (v2==null) return null;
            if (v2 instanceof UntypedAtomicValue) {
                v2 = v2.convert(Type.STRING);
            }
            return BooleanValue.get(compare(v1, operator, v2, comparer));
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


    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
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
