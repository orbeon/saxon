package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.functions.Position;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.Value;
import net.sf.saxon.xpath.XPathException;

/**
* An Atomizer is an expression corresponding essentially to the fn:data() function: it
* maps a sequence by replacing nodes with their typed values
*/

public final class Atomizer extends UnaryExpression {

    private boolean untyped;    //set to true if it is known that the nodes being atomized will be untyped

    /**
    * Constructor
     * @param sequence the sequence to be atomized
     * @param config the Configuration. Used only for optimization, may be null. Atomization is faster if
     * it is known in advance that all nodes will be untyped.
    */

    public Atomizer(Expression sequence, Configuration config) {
        super(sequence);
        if (sequence instanceof Position) {
            System.err.println("WHY?");
        }
        if (config == null) {
            untyped = false;
        } else {
            untyped = !config.isSchemaAware(Configuration.XML_SCHEMA);
        }
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        if (!env.getConfiguration().isSchemaAware(Configuration.XML_SCHEMA)) {
            untyped = true;
        };
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
        if (!env.getConfiguration().isSchemaAware(Configuration.XML_SCHEMA)) {
            untyped = true;
        };
        operand = operand.analyze(env, contextItemType);
        resetStaticProperties();
        if (Type.isSubType(operand.getItemType(), Type.ANY_ATOMIC_TYPE)) {
            return operand;
        }
        return this;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        if (base instanceof AtomizableIterator) {
            ((AtomizableIterator)base).setIsAtomizing(true);
        }
        return new MappingIterator(base, AtomizingFunction.getInstance(), null, null);
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
            SequenceIterator it = i.getTypedValue();
            return it.next();
        } else {
            return i;
        }
    }

    /**
    * Implement the mapping function
    */

    public static class AtomizingFunction implements MappingFunction {

        private AtomizingFunction(){};

        private static final AtomizingFunction theInstance = new AtomizingFunction();

        public static AtomizingFunction getInstance() {
            return theInstance;
        }

        public Object map(Item item, XPathContext context, Object info) throws XPathException {
            if (item instanceof NodeInfo) {
                return item.getTypedValue();
            } else {
                return item;
            }
        }
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER. For this class, the
     * result is always an atomic type, but it might be more specific.
    */

	public ItemType getItemType() {
        ItemType in = operand.getItemType();
        if (in instanceof AtomicType) {
            return in;
        }
        if (in instanceof NodeTest && untyped) {
            return Type.UNTYPED_ATOMIC_TYPE;    // TODO: some node kinds have a typed value of string?
        }
        if (in instanceof NodeTest) {
            SchemaType schemaType = ((NodeTest)in).getContentType();
            if (schemaType instanceof SimpleType) {
                return ((SimpleType)schemaType).getCommonAtomicType();
            } else if (((ComplexType)schemaType).isSimpleContent()) {
                try {
                    return ((ComplexType)schemaType).getSimpleContentType().getCommonAtomicType();
                } catch (ValidationException e) {
                    return Type.UNTYPED_ATOMIC_TYPE;
                }
            } else {
                // if a complex type with complex content can be atomized at all, it will return untypedAtomic values
                return Type.UNTYPED_ATOMIC_TYPE;
            }
        }
	    return Type.ANY_ATOMIC_TYPE;
	}

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        if (untyped) {
            return operand.getCardinality();
        } else {
            if (Cardinality.allowsMany(operand.getCardinality())) {
                return StaticProperty.ALLOWS_ZERO_OR_MORE;
            }
            ItemType in = operand.getItemType();
            if (in instanceof AtomicType) {
                return operand.getCardinality();
            }
            if (in instanceof NodeTest) {
                SchemaType schemaType = ((NodeTest)in).getContentType();
                if (schemaType instanceof AtomicType) {
                    // can return at most one atomic value per node
                    return operand.getCardinality();
                }
            }
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
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
