package net.sf.saxon.instruct;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.type.ItemType;

import java.util.Iterator;
import java.io.PrintStream;


/**
* Handler for saxon:while elements in stylesheet. <br>
* The saxon:while element has a mandatory attribute test, a boolean expression.
* The content is output repeatedly so long as the test condition is true.
*/

public class While extends Instruction {

    private Expression test;
    private Expression action;

    public While(Expression test, Expression action) {
        this.test = test;
        this.action = action;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    * @return the string "saxon:while"
    */

    public int getInstructionNameCode() {
        return StandardNames.SAXON_WHILE;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        test = test.simplify(env);
        action = action.simplify(env);
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @exception XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        test = test.analyze(env, contextItemType);
        action = action.analyze(env, contextItemType);
        return this;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     */

    public ItemType getItemType() {
        return action.getItemType();
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        test = test.promote(offer);
        action = action.promote(offer);
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(test, action);
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        while (test.effectiveBooleanValue(context)) {
            action.process(context);
        }
        return null;
    }


    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "while");
        test.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level) + "do");
        action.display(level+1, pool, out);
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
