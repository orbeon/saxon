package net.sf.saxon.expr;

import net.sf.saxon.Controller;
import net.sf.saxon.value.Value;
import net.sf.saxon.event.SequenceOutputter;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;


/**
 * An abstract implementation of Expression designed to make it easy to implement new expressions,
 * in particular, expressions to support extension instructions.
 */

public abstract class SimpleExpression extends ComputedExpression {

    public static final Expression[] NO_ARGUMENTS = new Expression[0];

    protected Expression[] arguments = NO_ARGUMENTS;

    /**
     * Constructor
     */

    public SimpleExpression() {
    }


    
    /**
     * Set the immediate sub-expressions of this expression. 
     * @param sub an array containing the sub-expressions of this expression
     */
    
    public void setArguments(Expression[] sub) {
        arguments = sub;
    }

    /**
     * Get the immediate sub-expressions of this expression.
     * @return an array containing the sub-expressions of this expression
     */
    
    public Iterator iterateSubExpressions() {
        return Arrays.asList(arguments).iterator();
    }
    
    /**
     * Simplify the expression
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] != null) {
                arguments[i] = arguments[i].simplify(env);
            }
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
     * @exception net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] != null) {
                arguments[i] = arguments[i].analyze(env, contextItemType);
            }
        }
        return this;
    }


    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @exception XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] != null) {
                arguments[i] = arguments[i].promote(offer);
            }
        }
        return this;
    }


    /**
     * Determine the data type of the items returned by this expression. This implementation
     * returns "item()", which can be overridden in a subclass.
     * @return the data type
     */

    public ItemType getItemType() {
        return Type.ITEM_TYPE;
    }

    /**
     * Determine the static cardinality of the expression. This implementation
     * returns "zero or more", which can be overridden in a subclass.
     */

    public int computeCardinality() {
        if ((getImplementationMethod() & Expression.EVALUATE_METHOD) == 0) {
            return StaticProperty.ALLOWS_ONE_OR_MORE;
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        return super.computeDependencies();
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
        int m = getImplementationMethod();
        if ((m & Expression.EVALUATE_METHOD) != 0) {
            dynamicError("evaluateItem() is not implemented in the subclass " + this.getClass(), context);
        } else if ((m & Expression.ITERATE_METHOD) != 0) {
            return iterate(context).next();
        } else {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin(this);
            SequenceOutputter seq = new SequenceOutputter(1);
            seq.setPipelineConfiguration(controller.makePipelineConfiguration());
            c2.setTemporaryReceiver(seq);
            process(c2);
            return seq.getFirstItem();
        }
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        int m = getImplementationMethod();
        if ((m & Expression.EVALUATE_METHOD) != 0) {
            Item item = evaluateItem(context);
            if (item==null) {
                return EmptyIterator.getInstance();
            } else {
                return SingletonIterator.makeIterator(item);
            }
        } else if ((m & Expression.ITERATE_METHOD) != 0) {
            dynamicError("iterate() is not implemented in the subclass " + this.getClass(), context);

        } else {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin(this);
            SequenceOutputter seq = new SequenceOutputter();
            seq.setPipelineConfiguration(controller.makePipelineConfiguration());
            c2.setTemporaryReceiver(seq);

            process(c2);

            return Value.getIterator(seq.getSequence());
        }
        return null;
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     * the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        int m = getImplementationMethod();
        if ((m & Expression.EVALUATE_METHOD) == 0) {
            SequenceIterator iter = iterate(context);
            while (true) {
                Item it = iter.next();
                if (it==null) break;
                context.getReceiver().append(it, locationId, NodeInfo.ALL_NAMESPACES);
            }
        } else {
            Item item = evaluateItem(context);
            context.getReceiver().append(item, locationId, NodeInfo.ALL_NAMESPACES);
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
        out.println(ExpressionTool.indent(level) + getExpressionType());
        for (int i = 0; i < arguments.length; i++) {
            arguments[i].display(level+1, pool, out);
        }        
    }
    
    /**
     * Return a distinguishing name for the expression, for use in diagnostics. 
     * By default the class name is used.
     */ 
    
    public String getExpressionType() {
        return getClass().getName();
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
