package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.xpath.XPathException;

/**
* An UntypedAtomicConverter is an expression that converts any untypedAtomic items in
* a sequence to a specified type, returning all other items unchanged
*/

public final class UntypedAtomicConverter extends UnaryExpression implements MappingFunction {

    private AtomicType requiredItemType;

    /**
    * Constructor
    * @param sequence: this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredItemType: the item type to which untypedAtomic items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public UntypedAtomicConverter(Expression sequence, AtomicType requiredItemType) {
        super(sequence);
        this.requiredItemType = requiredItemType;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        ItemType type = operand.getItemType();
        if (type instanceof NodeTest) {
            return this;
        }
        if (type==Type.ANY_ATOMIC_TYPE || type instanceof AnyItemType ||
                type==Type.UNTYPED_ATOMIC_TYPE) {
            return this;
        }
        // the sequence can't contain any untyped atomic values, so there's no need for
        // a converter
        return operand;
    }

     /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return new MappingIterator(base, this, null, null);
    }

    /**
    * Evaluate as an Item. This should only be called if the UntypedAtomicConverter has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        if (item instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)item).convert(requiredItemType, context);
        } else {
            return item;
        }
    }

    /**
    * Implement the mapping function
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        if (item instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)item).convert(requiredItemType, context);
        } else {
            return item;
        }
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "convert untyped atomic items to " + requiredItemType.toString(pool);
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
