package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pattern.NoNodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.Value;

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

        if (config == null) {
            untyped = false;
        } else {
            untyped = config.areAllNodesUntyped();
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
        return AtomizingFunction.getAtomizingIterator(base);
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

        public static SequenceIterator getAtomizingIterator(SequenceIterator base) {
            return new MappingIterator(base, theInstance, null, null);
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
        if (in instanceof NodeTest) {

            if (in instanceof NoNodeTest) {
                return in;
            }
            // Some node-kinds always have a typed value that's a string
            int kinds = ((NodeTest)in).getNodeKindMask();
            if ((kinds | STRING_KINDS) == STRING_KINDS) {
                return Type.STRING_TYPE;
            }
            // Some node-kinds are always untyped atomic; some are untypedAtomic provided that the configuration
            // is untyped
            if (untyped) {
                if ((kinds | UNTYPED_IF_UNTYPED_KINDS) == UNTYPED_IF_UNTYPED_KINDS) {
                    return Type.UNTYPED_ATOMIC_TYPE;
                }
            } else {
                if ((kinds | UNTYPED_KINDS) == UNTYPED_KINDS) {
                    return Type.UNTYPED_ATOMIC_TYPE;
                }
            }

            SchemaType schemaType = ((NodeTest)in).getContentType();
            if (schemaType instanceof SimpleType) {
                return ((SimpleType)schemaType).getCommonAtomicType();
            } else if (((ComplexType)schemaType).isSimpleContent()) {
                return ((ComplexType)schemaType).getSimpleContentType().getCommonAtomicType();
            } else {
                // if a complex type with complex content can be atomized at all, it will return untypedAtomic values
                return Type.UNTYPED_ATOMIC_TYPE;
            }
        }
	    return Type.ANY_ATOMIC_TYPE;
	}

    /**
     * Node kinds whose typed value is always a string
     */
    private static final int STRING_KINDS =
            (1<<Type.NAMESPACE) | (1<<Type.COMMENT) | (1<<Type.PROCESSING_INSTRUCTION);

    /**
     * Node kinds whose typed value is always untypedAtomic
     */

    private static final int UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.DOCUMENT);

    /**
     * Node kinds whose typed value is untypedAtomic if the configuration is untyped
     */

    private static final int UNTYPED_IF_UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.ELEMENT) | (1<<Type.DOCUMENT) | (1<<Type.ATTRIBUTE);

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
