package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;



/**
* A node set expression that will always return zero or one nodes
*/

public abstract class SingleNodeExpression extends ComputedExpression {

    /**
    * Type-check the expression.
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            StaticError err = new StaticError("Cannot select a node here: the context item is undefined");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        if (contextItemType instanceof AtomicType) {
            StaticError err = new StaticError("Cannot select a node here: the context item is an atomic value");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        return this;
    }

    /**
    * Specify that the expression returns a singleton
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_ONE;
    }

    /**
    * Determine the data type of the items returned by this expression
    * @return Type.NODE
    */

    public ItemType getItemType() {
        return AnyNodeTest.getInstance();
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.CONTEXT_DOCUMENT_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
    }

    /**
    * Get the single node to which this expression refers. Returns null if the node-set is empty
    */

    public abstract NodeInfo getNode(XPathContext context) throws XPathException;

    /**
    * Evaluate the expression in a given context to return an iterator
    * @param context the evaluation context
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return SingletonIterator.makeIterator(getNode(context));
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        return getNode(context);
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return getNode(context) != null;
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
