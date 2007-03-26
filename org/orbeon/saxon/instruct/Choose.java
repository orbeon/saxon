package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.SourceLocator;
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

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = conditions[i].typeCheck(env, contextItemType);
            adoptChildExpression(conditions[i]);
            XPathException err = TypeChecker.ebvError(conditions[i], env.getConfiguration().getTypeHierarchy());
            if (err != null) {
                if (conditions[i] instanceof ComputedExpression) {
                    err.setLocator((ComputedExpression)conditions[i]);
                } else if (actions[i] instanceof ComputedExpression) {
                    err.setLocator((ComputedExpression)actions[i]);
                } else {
                    err.setLocator(this);
                }
                throw err;
            }
        }
        for (int i=0; i<actions.length; i++) {
            actions[i] = actions[i].typeCheck(env, contextItemType);
            adoptChildExpression(actions[i]);
        }
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        for (int i=0; i<conditions.length; i++) {
            conditions[i] = conditions[i].optimize(opt, env, contextItemType);
            //adoptChildExpression(conditions[i]);
            if (conditions[i] instanceof Value) {
                final boolean b;
                try {
                    b = conditions[i].effectiveBooleanValue(env.makeEarlyEvaluationContext());
                } catch (XPathException err) {
                    err.setLocator(this);
                    throw err;
                }
                if (b) {
                    // if condition is always true, remove all the subsequent conditions and actions
                    if (i==0) {
                        ComputedExpression.setParentExpression(actions[0], getParentExpression());
                        return actions[0];
                    } else if (i != conditions.length - 1) {
                        Expression[] c2 = new Expression[i+1];
                        Expression[] a2 = new Expression[i+1];
                        System.arraycopy(conditions, 0, c2, 0, i+1);
                        System.arraycopy(actions, 0, a2, 0, i+1);
                        conditions = c2;
                        actions = a2;
                        break;
                    }
                } else {
                    // if condition is false, skip this test
                    Expression[] c2 = new Expression[conditions.length - 1];
                    Expression[] a2 = new Expression[conditions.length - 1];
                    System.arraycopy(conditions, 0, c2, 0, i);
                    System.arraycopy(actions, 0, a2, 0, i);
                    System.arraycopy(conditions, i+1, c2, i, conditions.length - i - 1);
                    System.arraycopy(actions, i+1, a2, i, conditions.length - i - 1);
                    conditions = c2;
                    actions = a2;
                    i--;
                }
            }
        }
        for (int i=0; i<actions.length; i++) {
            actions[i] = actions[i].optimize(opt, env, contextItemType);
            //adoptChildExpression(actions[i]);
        }
        if (actions.length == 0) {
            return EmptySequence.getInstance();
        }
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.ITERATE_METHOD;
    }

    /**
    * Mark tail calls on used-defined functions. For most expressions, this does nothing.
    */

    public boolean markTailFunctionCalls(int nameCode, int arity) {
        boolean result = false;
        for (int i=0; i<actions.length; i++) {
            if (actions[i] instanceof ComputedExpression) {
                result |= ((ComputedExpression)actions[i]).markTailFunctionCalls(nameCode, arity);
            }
        }
        return result;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType type = actions[0].getItemType(th);
        for (int i=1; i<actions.length; i++) {
            type = Type.getCommonSuperType(type, actions[i].getItemType(th), th);
        }
        return type;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if any of the "actions" creates new nodes.
     * (Nodes created by the conditions can't contribute to the result).
     */

    public final boolean createsNewNodes() {
        for (int i=0; i<actions.length; i++) {
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
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        for (int i=0; i<conditions.length; i++) {
            if (conditions[i] == original) {
                conditions[i] = replacement;
                found = true;
            };
        }
        for (int i=0; i<actions.length; i++) {
            if (actions[i] == original) {
                actions[i] = replacement;
                found = true;
            }
        }
                return found;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        // xsl:when acts as a guard: expressions inside the when mustn't be evaluated if the when is false,
        // and conditions after the first mustn't be evaluated if a previous condition is true. So we
        // don't pass all promotion offers on
        if (offer.action == PromotionOffer.UNORDERED ||
                offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES ||
                offer.action == PromotionOffer.REPLACE_CURRENT) {
            for (int i=0; i<conditions.length; i++) {
                conditions[i] = doPromotion(conditions[i], offer);
            }
            for (int i=0; i<actions.length; i++) {
                actions[i] = doPromotion(actions[i], offer);
            }
        } else {
            // in other cases, only the first xsl:when condition is promoted
            conditions[0]  = doPromotion(conditions[0], offer);
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
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        for (int i=0; i<conditions.length; i++) {
            out.println(ExpressionTool.indent(level) + (i==0 ? "if" : "else if"));
            conditions[i].display(level+1, out, config);
            out.println(ExpressionTool.indent(level) + "then");
            actions[i].display(level+1, out, config);
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
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                if (e.getLocator() == null) {
                    if (conditions[i] instanceof SourceLocator) {
                        e.setLocator((SourceLocator)conditions[i]);
                    } else {
                        e.setLocator(this);
                    }
                }
                throw e;
            }
            if (b) {
                if (actions[i] instanceof TailCallReturner) {
                    return ((TailCallReturner)actions[i]).processLeavingTail(context);
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
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                e.setLocator(this);
                throw e;
            }
            if (b) {
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
            final boolean b;
            try {
                b = conditions[i].effectiveBooleanValue(context);
            } catch (XPathException e) {
                e.setLocator(this);
                throw e;
            }
            if (b) {
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
