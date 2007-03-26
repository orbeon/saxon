package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.Value;

/**
* A TailCallLoop wraps the body of a function that contains tail-recursive function calls. On completion
 * of the "real" body of the function it tests whether the function has executed a tail call, and if so,
 * iterates to evaluate the tail call.
*/

public final class TailCallLoop extends UnaryExpression {

    UserFunction containingFunction;

    /**
    * Constructor
    */

    public TailCallLoop(UserFunction function) {
        super(function.getBody());
        containingFunction = function;
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
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
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return operand.getImplementationMethod();
    }


    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        final XPathContextMajor cm = (XPathContextMajor)context;
        while (true) {
            SequenceIterator iter = operand.iterate(cm);
            ValueRepresentation extent = SequenceExtent.makeSequenceExtent(iter);
            UserFunction fn = cm.getTailCallFunction();
            if (fn == null) {
               return Value.asIterator(extent, cm);
            }
            if (fn != containingFunction) {
                return Value.asIterator(tailCallDifferentFunction(fn, cm), cm);
            }
            // otherwise, loop round to execute the tail call
        }
    }

    /**
    * Evaluate as an Item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        final XPathContextMajor cm = (XPathContextMajor)context;
        while (true) {
            Item item = operand.evaluateItem(context);
            UserFunction fn = cm.getTailCallFunction();
            if (fn == null) {
               return item;
            }
            if (fn != containingFunction) {
                return Value.asItem(tailCallDifferentFunction(fn, cm));
            }
            // otherwise, loop round to execute the tail call
        }
    }

    /**
     * Process the function body
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        final XPathContextMajor cm = (XPathContextMajor)context;
        while (true) {
            operand.process(context);
            UserFunction fn = cm.getTailCallFunction();
            if (fn == null) {
               return;
            }
            if (fn != containingFunction) {
                Value.asValue(tailCallDifferentFunction(fn, cm)).process(cm);
                return;
            }
            // otherwise, loop round to execute the tail call
        }
    }

    /**
     * Make a tail call on a different function. This reuses the context object and the stack frame array
     * where possible, but it does consume some Java stack space. It's still worth it, because we don't use
     * as much stack as we would if we didn't return down to the TailCallLoop level.
     * @param fn the function to be called
     * @param cm the dynamic context
     * @return the result of calling the other function
     * @throws XPathException if the called function fails
     */

    private ValueRepresentation tailCallDifferentFunction(UserFunction fn, XPathContextMajor cm) throws XPathException {
        cm.resetStackFrameMap(fn.getStackFrameMap(), fn.getNumberOfArguments());
        try {
            return ExpressionTool.evaluate(fn.getBody(), fn.getEvaluationMode(), cm, 1);
        } catch (XPathException err) {
            if (err.getLocator() == null) {
                err.setLocator(fn);
            }
            throw err;
        }
    }


    /**
    * Determine the data type of the items returned by the expression
     * @param th
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return operand.getItemType(th);
	}

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        return "tail-recursive loop";
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
