package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.ContentTypeTest;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.xpath.XPathException;

/**
* An Atomizer is an expression corresponding essentially to the fn:data() function: it
* maps a sequence by replacing nodes with their typed values
*/

public final class Atomizer extends UnaryExpression implements MappingFunction {

    /**
    * Constructor
    */

    public Atomizer(Expression sequence) {
        super(sequence);
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (operand instanceof AtomicValue) {
            return operand;
        } else if (operand instanceof Value) {
            SequenceIterator iter = operand.iterate(null);
            while (true) {
                // if all items in the sequence are atomic (they generally will be, since this is
                // done at compile time), then return the sequence
                Item i = iter.next();
                if (i == null) {
                    return operand;
                }
                if (i instanceof NodeInfo) {
                    return this;
                }
            }
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        if (Type.isSubType(operand.getItemType(), Type.ANY_ATOMIC_TYPE)) {
            return operand;
        }
        return this;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        if (base instanceof AtomizableIterator) {
            ((AtomizableIterator)base).setIsAtomizing(true);
        }
        return new MappingIterator(base, this, null, context.getController().getConfiguration());
    }

    /**
    * Evaluate as an Item. This should only be called if the Atomizer has cardinality zero-or-one,
    * which will only be the case if the underlying expression has cardinality zero-or-one.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item i = operand.evaluateItem(context);
        if (i==null) {
            return null;
        }
        if (i instanceof NodeInfo) {
            SequenceIterator it = i.getTypedValue(context.getController().getConfiguration());
            return it.next();
        } else {
            return i;
        }
    }

    /**
    * Implement the mapping function
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        if (item instanceof NodeInfo) {
            return item.getTypedValue((Configuration)info);
        } else {
            return item;
        }
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
    */

	public ItemType getItemType() {
        ItemType in = operand.getItemType();
        if (Type.isSubType(in, Type.ANY_ATOMIC_TYPE)) {
            return in;
        }
        if (in instanceof ContentTypeTest) {
            SchemaType schemaType = ((ContentTypeTest)in).getSchemaType();
            if (schemaType instanceof AtomicType) {
                // TODO: could do better, e.g. with list types, mixed content elements, etc
                return (AtomicType)schemaType;
            }

        }
	    return Type.ANY_ATOMIC_TYPE;
	}

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
        // TODO: this is where we need schema information...
	}

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "atomize";
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
