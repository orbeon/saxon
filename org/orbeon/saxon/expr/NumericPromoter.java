package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

/**
* A NumericPromoter performs numeric promotion on each item in a supplied sequence
*/

public final class NumericPromoter extends UnaryExpression {

    private BuiltInAtomicType requiredType; // always xs:float or xs:double

    /**
    * Constructor
    * @param sequence this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredType the item type to which all items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public NumericPromoter(Expression sequence, BuiltInAtomicType requiredType) {
        super(sequence);
        this.requiredType = requiredType;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (operand instanceof Literal) {
            if (((Literal)operand).getValue() instanceof AtomicValue) {
                return Literal.makeLiteral(
                        promote(((AtomicValue)((Literal)operand).getValue()), null));
            } else {
                return Literal.makeLiteral(
                        ((Value)SequenceExtent.makeSequenceExtent(
                                iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))).reduce());
            }
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        return this;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemMappingFunction promoter = new ItemMappingFunction() {
            public Item map(Item item) throws XPathException {
                return promote(((AtomicValue)item), context);
            }
        };
        return new ItemMappingIterator(base, promoter);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new NumericPromoter(getBaseExpression().copy(), requiredType);
    }

    /**
    * Evaluate as an Item. This should only be called if the expression has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return promote(((AtomicValue)item), context);
    }

    /**
     * Perform the promotion
     * @param value the numeric or untyped atomic value to be promoted
     * @param context the XPath dynamic evaluation context
     * @return the value that results from the promotion
     */

    private AtomicValue promote(AtomicValue value, XPathContext context) throws XPathException {
        if (!(value instanceof NumericValue || value instanceof UntypedAtomicValue)) {
            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            XPathException err = new XPathException(
                    "Cannot promote non-numeric value to " + getItemType(th).toString(), "XPTY0004", context);
            err.setLocator(this);
            throw err;
        }
        if (requiredType.equals(BuiltInAtomicType.FLOAT) && value instanceof DoubleValue) {
            XPathException err = new XPathException(
                    "Cannot promote from xs:double to xs:float", "XPTY0004", context);
            err.setLocator(this);
            throw err;
        }
        return value.convert(requiredType, true, context).asAtomic();
    }

    /**
     * Get the required type. Always StandardNames.XS_DOUBLE or StandardNames.XS_FLOAT
     * @return the fingerprint of the name of the required type
     */

    public int getRequiredType() {
        return requiredType.getFingerprint();
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
        if (requiredType.equals(BuiltInAtomicType.DOUBLE)) {
            return BuiltInAtomicType.DOUBLE;
        } else {
            return BuiltInAtomicType.FLOAT;
        }
	}

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredType == ((NumericPromoter)other).requiredType;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("promoteNumeric");
        out.emitAttribute("to", getItemType(out.getTypeHierarchy()).toString(out.getConfiguration().getNamePool()));
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
