package net.sf.saxon.instruct;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Compiled representation of an xsl:choose or xsl:if element in the stylesheet.
 * Also used for typeswitch in XQuery.
*/

public class Choose extends Instruction {

    // The class implements both xsl:choose and xsl:if. There is a list of boolean
    // expressions (conditions) and a list of corresponding actions: the conditions
    // are evaluated in turn, and when one is found that is true, the corresponding
    // action is evaluated. For xsl:if, there is always one condition and one action.
    // An xsl:otherwise is compiled as if it were xsl:when test="true()". If no
    // condition is satisfied, the instruction returns without doing anything.

    private Expression[] conditions;
    private Expression[] actions;


    /**
    * Construct an xsl:choose instruction
    * @param conditions the conditions to be tested, in order
    * @param actions the actions to be taken when the corresponding condition is true
    */

    public Choose(Expression[] conditions, Expression[] actions) {
        this.conditions = conditions;
        this.actions = actions;
        for (int i=0; i<conditions.length; i++) {
            adoptChildExpression(conditions[i]);
        }
        for (int i=0; i<actions.length; i++) {
            adoptChildExpression(actions[i]);
        }
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    * We assume that if there was
     * only one condition then it was an xsl:if; this is not necessarily so, but
     * it's adequate for tracing purposes.
    */


    public int getInstructionNameCode() {
        return (conditions.length==1 ? StandardNames.XSL_IF : StandardNames.XSL_CHOOSE);
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
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = conditions[i].simplify(env);
        }
        for (int i=0; i<actions.length; i++) {
            actions[i] = actions[i].simplify(env);
        }
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
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = conditions[i].analyze(env, contextItemType);
        }
        for (int i=0; i<actions.length; i++) {
            actions[i] = actions[i].analyze(env, contextItemType);
        }
        return this;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     */

    public ItemType getItemType() {
        ItemType type = actions[0].getItemType();
        for (int i=1; i<actions.length; i++) {
            type = Type.getCommonSuperType(type, actions[i].getItemType());
        }
        return type;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if any of the "actions" creates new nodes.
     * (Nodes created by the conditions can't contribute to the result).
     */

    public final boolean createsNewNodes() {
        for (int i=1; i<actions.length; i++) {
            int props = actions[i].getSpecialProperties();
            if ((props & StaticProperty.NON_CREATIVE) == 0) {
                return true;
            };
        }
        return false;
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(conditions.length + actions.length);
        for (int i=0; i<conditions.length; i++) {
            list.add(conditions[i]);
        }
        for (int i=0; i<actions.length; i++) {
            list.add(actions[i]);
        }
        return list.iterator();
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = conditions[i].promote(offer);
        }
        for (int i=0; i<actions.length; i++) {
            actions[i] = actions[i].promote(offer);
        }
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        for (int i=0; i<actions.length; i++) {
            actions[i].checkPermittedContents(parentType, env, whole);
        }        
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        for (int i=0; i<conditions.length; i++) {
            out.println(ExpressionTool.indent(level) + (i==0 ? "if" : "else if"));
            conditions[i].display(level+1, pool, out);
            out.println(ExpressionTool.indent(level) + "then");
            actions[i].display(level+1, pool, out);
        }
    }

    /**
    * Process this instruction, that is, choose an xsl:when or xsl:otherwise child
    * and process it.
    * @param context the dynamic context of this transformation
    * @throws XPathException if any non-recoverable dynamic error occurs
    * @return a TailCall, if the chosen branch ends with a call of call-template or
    * apply-templates. It is the caller's responsibility to execute such a TailCall.
    * If there is no TailCall, returns null.
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            if (conditions[i].effectiveBooleanValue(context)) {
                if (actions[i] instanceof Instruction) {
                    return ((Instruction)actions[i]).processLeavingTail(context);
                } else {
                    actions[i].process(context);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            if (conditions[i].effectiveBooleanValue(context)) {
                return actions[i].evaluateItem(context);
            }
        }
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation relies on the process() method: it
     * "pushes" the results of the instruction to a sequence in memory, and then
     * iterates over this in-memory sequence.
     *
     * In principle instructions should implement a pipelined iterate() method that
     * avoids the overhead of intermediate storage.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            if (conditions[i].effectiveBooleanValue(context)) {
                return (actions[i]).iterate(context);
            }
        }
        return EmptyIterator.getInstance();
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
