package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

import java.io.PrintStream;


/**
* This class represents the expression "(dot)", which always returns the context item.
* This may be a AtomicValue or a Node.
*/

public final class ContextItemExpression extends ComputedExpression {

    ItemType itemType = Type.ITEM_TYPE;

    public ContextItemExpression() {}

    /**
    * Type-check the expression.
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            StaticError err = new StaticError("The context item is undefined at this point");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        itemType = contextItemType;
        return this;
    }

    /**
    * Determine the item type
    */

    public ItemType getItemType() {
        return itemType;
    }

    /**
    * Get the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return (other instanceof ContextItemExpression);
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        return "ContextItemExpression".hashCode();
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    /**
    * Iterate over the value of the expression
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item is not set", context);
        }
        return SingletonIterator.makeIterator(item);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item is not set", context);
        }
        return item;
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + '.');
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
