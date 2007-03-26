package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

/**
* A NumericPromoter performs numeric promotion on each item in a supplied sequence
*/

public final class NumericPromoter extends UnaryExpression {

    private int requiredType; // always xs:float or xs:double

    /**
    * Constructor
    * @param sequence this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredType the item type to which all items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public NumericPromoter(Expression sequence, int requiredType) {
        super(sequence);
        this.requiredType = requiredType;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (operand instanceof AtomicValue) {
            return promote(((AtomicValue)operand), null);
        } else if (operand instanceof Value) {
            return ((Value)SequenceExtent.makeSequenceExtent(iterate(env.makeEarlyEvaluationContext()))).reduce();
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);
        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.optimize(opt, env, contextItemType);
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
    * Evaluate as an Item. This should only be called if the expression has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return promote(((AtomicValue)item), context);
    }

    /**
     * Perform the promotion
     */

    private AtomicValue promote(AtomicValue value, XPathContext context) throws XPathException {
        AtomicValue v = value.getPrimitiveValue();
        if (!(v instanceof NumericValue || v instanceof UntypedAtomicValue)) {
            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            DynamicError err = new DynamicError("Cannot promote non-numeric value to " + getItemType(th).toString());
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        return v.convert(requiredType, context);
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     * @param th
     */

	public ItemType getItemType(TypeHierarchy th) {
        if (requiredType == StandardNames.XS_DOUBLE) {
            return Type.DOUBLE_TYPE;
        } else {
	        return Type.FLOAT_TYPE;
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
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        final TypeHierarchy th = config.getTypeHierarchy();
        return "promote items to " + getItemType(th).toString(config.getNamePool());
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
