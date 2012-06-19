package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.SequenceType;

/**
* InstanceOf Expression: implements "Expr instance of data-type"
*/

public final class InstanceOfExpression extends UnaryExpression {

    ItemType targetType;
    int targetCardinality;

    /**
     * Construct an "instance of" expression in the form "source instance of target"
     * @param source the expression whose type is to be tested
     * @param target the type against which it is tested
     */

    public InstanceOfExpression(Expression source, SequenceType target) {
        super(source);
        targetType = target.getPrimaryType();
        if (targetType == null) {
            throw new IllegalArgumentException("Primary item type must not be null");
        }
        targetCardinality = target.getCardinality();
    }

    /**
     * Get the item type that we are testing for membership of
     * @return the item type
     */

    public ItemType getRequiredItemType() {
        return targetType;
    }

    /**
     * Get the cardinality that we are testing for membership of
     * @return the required cardinality
     */

    public int getRequiredCardinality() {
        return targetCardinality;
    }

    /**
    * Type-check the expression
    * @return the checked expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand instanceof Literal) {
            return Literal.makeLiteral(
                    (AtomicValue)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()));
        }

        // See if we can get the answer by static analysis.

        if (Cardinality.subsumes(targetCardinality, operand.getCardinality())) {
            final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            int relation = th.relationship(operand.getItemType(th), targetType);
            if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                return Literal.makeLiteral(BooleanValue.TRUE);
            } else if (relation == TypeHierarchy.DISJOINT) {
                // if the item types are disjoint, the result might still be true if both sequences are empty
                if (!Cardinality.allowsZero(targetCardinality) || !Cardinality.allowsZero(operand.getCardinality())) {
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            }
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        if (Cardinality.subsumes(targetCardinality, operand.getCardinality())) {
            final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            int relation = th.relationship(operand.getItemType(th), targetType);
            if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                return Literal.makeLiteral(BooleanValue.TRUE);
            } else if (relation == TypeHierarchy.DISJOINT) {
                // if the item types are disjoint, the result might still be true if both sequences are empty
                if (!Cardinality.allowsZero(targetCardinality) || !Cardinality.allowsZero(operand.getCardinality())) {
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            }
        }
        return this;
    }


    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((InstanceOfExpression)other).targetType &&
                targetCardinality == ((InstanceOfExpression)other).targetCardinality;
    }

     /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new InstanceOfExpression(getBaseExpression().copy(), 
                SequenceType.makeSequenceType(targetType, targetCardinality));
    }

    /**
     * Determine the data type of the result of the InstanceOf expression
     * @param th  the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression as a boolean
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        int count = 0;
        while (true) {
            Item item = iter.next();
            if (item == null) break;
            count++;
            if (!targetType.matchesItem(item, false, context.getConfiguration())) {
                return false;
            }
            if (count==2 && !Cardinality.allowsMany(targetCardinality)) {
                return false;
            }
        }
        return !(count == 0 && ((targetCardinality & StaticProperty.ALLOWS_ZERO) == 0));
    }


    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("instance");
        destination.emitAttribute("of", targetType.toString(destination.getNamePool()));
        destination.emitAttribute("occurs", Cardinality.getOccurrenceIndicator(targetCardinality));
        operand.explain(destination);
        destination.endElement();
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
