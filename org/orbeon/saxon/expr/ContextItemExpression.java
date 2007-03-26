package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;

import java.io.PrintStream;


/**
* This class represents the expression "(dot)", which always returns the context item.
* This may be a AtomicValue or a Node.
*/

public class ContextItemExpression extends ComputedExpression {

    ItemType itemType = Type.ITEM_TYPE;

    public ContextItemExpression() {}

    /**
     * Create a clone copy of this expression
     * @return a copy of this expression
     */

    public ContextItemExpression copy() {
        ContextItemExpression cie2 = new ContextItemExpression();
        cie2.itemType = itemType;
        return cie2;
    }

    protected String getErrorCodeForUndefinedContext() {
        return "XPDY0002";
    }

    /**
    * Type-check the expression.
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            StaticError err = new StaticError("The context item is undefined at this point");
            err.setErrorCode(getErrorCodeForUndefinedContext());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        itemType = contextItemType;
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws org.orbeon.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        // In XSLT, we don't catch this error at the typeCheck() phase because it's done one XPath expression
        // at a time. So we repeat the check here.
        return typeCheck(env, contextItemType);
    }

    /**
    * Determine the item type
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
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
            dynamicError("The context item is not set", getErrorCodeForUndefinedContext(), context);
        }
        return SingletonIterator.makeIterator(item);
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item is not set", getErrorCodeForUndefinedContext(), context);
        }
        return item;
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
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
