package net.sf.saxon.expr;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.CombinedNodeTest;
import net.sf.saxon.sort.DocumentOrderIterator;
import net.sf.saxon.sort.GlobalOrderComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;


/**
* An expression representing a nodeset that is a union, difference, or
* intersection of two other NodeSets
*/

public class VennExpression extends BinaryExpression {

    /**
    * Constructor
    * @param p1 the left-hand operand
    * @param op the operator (union, intersection, or difference)
    * @param p2 the right-hand operand
    */

    public VennExpression(final Expression p1, final int op, final Expression p2) {
        super(p1, op, p2);
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return the data type
    */

    public final ItemType getItemType() {
        final ItemType t1 = operand0.getItemType();
        final ItemType t2 = operand1.getItemType();
        return Type.getCommonSuperType(t1, t2);
    }

    /**
    * Determine the static cardinality of the expression
    */

    public final int computeCardinality() {
        final int c1 = operand0.getCardinality();
        final int c2 = operand1.getCardinality();
        switch (operator) {
            case Token.UNION:
                if (operand0 instanceof EmptySequence) return c2;
                if (operand1 instanceof EmptySequence) return c1;
                return c1 | c2 | StaticProperty.ALLOWS_ONE | StaticProperty.ALLOWS_MANY;
                    // allows ZERO only if one operand allows ZERO
            case Token.INTERSECT:
                if (operand0 instanceof EmptySequence) return StaticProperty.EMPTY;
                if (operand1 instanceof EmptySequence) return StaticProperty.EMPTY;
                return (c1 & c2) | StaticProperty.ALLOWS_ZERO | StaticProperty.ALLOWS_ONE;
                    // allows MANY only if both operands allow MANY
            case Token.EXCEPT:
                if (operand0 instanceof EmptySequence) return StaticProperty.EMPTY;
                if (operand1 instanceof EmptySequence) return c1;
                return c1 | StaticProperty.ALLOWS_ZERO | StaticProperty.ALLOWS_ONE;
                    // allows MANY only if first operand allows MANY
        }
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        final int prop0 = operand0.getSpecialProperties();
        final int prop1 = operand1.getSpecialProperties();
        int props = StaticProperty.ORDERED_NODESET;
        if (testContextDocumentNodeSet(prop0, prop1)) {
            props |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        if (testSubTree(prop0, prop1)) {
            props |= StaticProperty.SUBTREE_NODESET;
        }
        if (!testCreative(prop0, prop1)) {
            props |= StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * Determine whether all the nodes in the node-set are guaranteed to
     * come from the same document as the context node. Used for optimization.
     */

    private boolean testContextDocumentNodeSet(final int prop0, final int prop1) {
        switch (operator) {
            case Token.UNION:
                return (prop0 & prop1 & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
            case Token.INTERSECT:
                return ((prop0 | prop1) & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
            case Token.EXCEPT:
                return (prop0 & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        }
        return false;
    }

    /**
     * Determine whether all the nodes in the node-set are guaranteed to
     * come from a subtree rooted at the context node. Used for optimization.
     */

    private boolean testSubTree(final int prop0, final int prop1) {
        switch (operator) {
            case Token.UNION:
                return (prop0 & prop1 & StaticProperty.SUBTREE_NODESET) != 0;
            case Token.INTERSECT:
                return ((prop0 | prop1) & StaticProperty.SUBTREE_NODESET) != 0;
            case Token.EXCEPT:
                return (prop0 & StaticProperty.SUBTREE_NODESET) != 0;
        }
        return false;
    }

    /**
     * Determine whether the expression can create new nodes
     */

    private boolean testCreative(final int prop0, final int prop1) {
        return !(((prop0 & StaticProperty.NON_CREATIVE) == 1) &&
                ((prop1 & StaticProperty.NON_CREATIVE) == 1));
    }


    /**
    * Simplify the expression
    */

     public Expression simplify(final StaticContext env) throws XPathException {
        operand0 = operand0.simplify(env);
        operand1 = operand1.simplify(env);

        // If either operand is an empty sequence, simplify the expression. This can happen
        // after reduction with constructs of the form //a[condition] | //b[not(condition)],
        // common in XPath 1.0 because there were no conditional expressions.

        switch (operator) {
            case Token.UNION:
                //return Type.isNodeType(getItemType()) && isSingleton();
                // this is a sufficient condition, but other expressions override this method
                if (operand0 instanceof EmptySequence && (operand1.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) return operand1;
                //return Type.isNodeType(getItemType()) && isSingleton();
                // this is a sufficient condition, but other expressions override this method
                if (operand1 instanceof EmptySequence && (operand0.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) return operand0;
                break;
            case Token.INTERSECT:
                if (operand0 instanceof EmptySequence) return operand0;
                if (operand1 instanceof EmptySequence) return operand1;
                break;
            case Token.EXCEPT:
                if (operand0 instanceof EmptySequence) return operand0;
                //return Type.isNodeType(getItemType()) && isSingleton();
                // this is a sufficient condition, but other expressions override this method
                if (operand1 instanceof EmptySequence && (operand0.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) return operand0;
                break;
        }



        // If both are axis expressions on the same axis, merge them
        // ie. rewrite (axis::test1 | axis::test2) as axis::(test1 | test2)

        if (operand0 instanceof AxisExpression && operand1 instanceof AxisExpression) {
            final AxisExpression a1 = (AxisExpression)operand0;
            final AxisExpression a2 = (AxisExpression)operand1;
            if (a1.getAxis() == a2.getAxis()) {
                return new AxisExpression(a1.getAxis(),
                             new CombinedNodeTest(a1.getNodeTest(),
                                                  operator,
                                                  a2.getNodeTest()));
            }
        }

        // If both are path expressions starting the same way, merge them
        // i.e. rewrite (/X | /Y) as /(X|Y). This applies recursively, so that
        // /A/B/C | /A/B/D becomes /A/B/child::(C|D)

        if (operand0 instanceof PathExpression && operand1 instanceof PathExpression) {
            final PathExpression path1 = (PathExpression)operand0;
            final PathExpression path2 = (PathExpression)operand1;

            if (path1.getFirstStep().equals(path2.getFirstStep())) {
                final Expression path = new PathExpression(
                        path1.getFirstStep(),
                        new VennExpression(
                            path1.getRemainingSteps(),
                            operator,
                            path2.getRemainingSteps())).simplify(env);
                ExpressionTool.copyLocationInfo(this, path);
                return path;
            }
        }

        // Try merging two non-positional filter expressions:
        // A[exp0] | A[exp1] becomes A[exp0 or exp1]

        if (operand0 instanceof FilterExpression && operand1 instanceof FilterExpression) {
            final FilterExpression exp0 = (FilterExpression)operand0;
            final FilterExpression exp1 = (FilterExpression)operand1;

            if (!exp0.isPositional() &&
                    !exp1.isPositional() &&
                    exp0.getBaseExpression().equals(exp1.getBaseExpression())) {
                final Expression filter;
                switch (operator) {
                    case Token.UNION:
                        filter = new BooleanExpression(exp0.getFilter(),
                                                Token.OR,
                                                exp1.getFilter());
                        break;
                    case Token.INTERSECT:
                        filter = new BooleanExpression(exp0.getFilter(),
                                                Token.AND,
                                                exp1.getFilter());
                        break;
                    case Token.EXCEPT:
                        final FunctionCall negate2 = SystemFunction.makeSystemFunction("not", 1, env.getNamePool());
                        final Expression[] args = new Expression[1];
                        args[0] = exp1.getFilter();
                        negate2.setArguments(args);
                        filter = new BooleanExpression(exp0.getFilter(),
                                                Token.AND,
                                                negate2);
                        break;
                    default:
                        throw new AssertionError("Unknown operator " + operator);
                }
                final Expression f = new FilterExpression(
                        exp0.getBaseExpression(),
                        filter, env).simplify(env);
                ExpressionTool.copyLocationInfo(this, filter);
                ExpressionTool.copyLocationInfo(this, f);
                return f;
            }
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(final StaticContext env, final ItemType contextItemType) throws XPathException {

        operand0 = operand0.analyze(env, contextItemType);
        operand1 = operand1.analyze(env, contextItemType);

        final RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        operand0 = TypeChecker.staticTypeCheck(operand0, SequenceType.NODE_SEQUENCE, false, role0, env);

        final RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        operand1 = TypeChecker.staticTypeCheck(operand1, SequenceType.NODE_SEQUENCE, false, role1, env);
        return this;
    }

    /**
    * Is this expression the same as another expression?
    */

//    public boolean equals(Object other) {
//        if (other instanceof VennExpression) {
//            VennExpression b = (VennExpression)other;
//            if (operator != b.operator) {
//                return false;
//            }
//            if (operand0.equals(b.operand0) && operand1.equals(b.operand1)) {
//               return true;
//            }
//            if (operator == Token.UNION || operator == Token.INTERSECT) {
//                // commutative operators: A|B == B|A
//                if (operand0.equals(b.operand1) && operand1.equals(b.operand0)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    public int hashCode() {
        return operand0.hashCode() ^ operand1.hashCode();
    }

    /**
    * Iterate over the value of the expression. The result will always be sorted in document order,
    * with duplicates eliminated
    * @param c The context for evaluation
    * @return a SequenceIterator representing the union of the two operands
    */

    public SequenceIterator iterate(final XPathContext c) throws XPathException {
        SequenceIterator i1 = operand0.iterate(c);
        //return Type.isNodeType(getItemType()) && isSingleton();
        // this is a sufficient condition, but other expressions override this method
        if (!((operand0.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0)) {
            i1 = new DocumentOrderIterator(i1, GlobalOrderComparer.getInstance());
        }
        SequenceIterator i2 = operand1.iterate(c);
        //return Type.isNodeType(getItemType()) && isSingleton();
        // this is a sufficient condition, but other expressions override this method
        if (!((operand1.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0)) {
            i2 = new DocumentOrderIterator(i2, GlobalOrderComparer.getInstance());
        }
        switch (operator) {
            case Token.UNION:
                return new UnionEnumeration(i1, i2,
                                            GlobalOrderComparer.getInstance());
            case Token.INTERSECT:
                return new IntersectionEnumeration(i1, i2,
                                            GlobalOrderComparer.getInstance());
            case Token.EXCEPT:
                return new DifferenceEnumeration(i1, i2,
                                            GlobalOrderComparer.getInstance());
        }
        throw new UnsupportedOperationException("Unknown operator in Set Expression");
    }

    /**
    * Get the effective boolean value. In the case of a union expression, this
    * is reduced to an OR expression, for efficiency
    */

    public boolean effectiveBooleanValue(final XPathContext context) throws XPathException {
        if (operator == Token.UNION) {
            return operand0.effectiveBooleanValue(context) || operand1.effectiveBooleanValue(context);
        } else {
            return super.effectiveBooleanValue(context);
        }
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
