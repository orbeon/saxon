package net.sf.saxon.expr;

import net.sf.saxon.Controller;
import net.sf.saxon.event.SequenceOutputter;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.SortExpression;
import net.sf.saxon.sort.TupleSorter;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.value.*;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.SourceLocator;
import java.util.Iterator;

/**
 * This class, ExpressionTool, contains a number of useful static methods
 * for manipulating expressions. Most importantly, it provides the factory
 * method make() for constructing a new expression
 */

public class ExpressionTool
 {
    private ExpressionTool() {}

    /**
     * Parse an expression. This performs the basic analysis of the expression against the
     * grammar, it binds variable references and function calls to variable definitions and
     * function definitions, and it performs context-independent expression rewriting for
     * optimization purposes.
     *
     * @exception XPathException if the expression contains a static error
     * @param expression The expression (as a character string)
     * @param env An object giving information about the compile-time
     *     context of the expression
     * @param terminator The token that marks the end of this expression; typically
     * Tokenizer.EOF, but may for example be a right curly brace
     * @param lineNumber the line number of the start of the expression
     * @return an object of type Expression
     */

    public static Expression make(String expression, StaticContext env,
                                  int start, int terminator, int lineNumber) throws XPathException {
        ExpressionParser parser = new ExpressionParser();
        if (terminator == -1) {
            terminator = Token.EOF;
        }
        Expression exp = parser.parse(expression, start, terminator, lineNumber, env);
        exp = exp.simplify(env);
        makeParentReferences(exp);
        return exp;
    }

    /**
     * Copy location information (currently the line number and parent) from one expression
     * to another
     */

    public static void copyLocationInfo(Expression from, Expression to) {
        if (from instanceof ComputedExpression && to instanceof ComputedExpression) {
            ((ComputedExpression)to).setLocationId(((ComputedExpression)from).getLocationId());
        }
    }

    /**
     * Establish the links from subexpressions to their parent expressions,
     * by means of a recursive tree walk.
     */

    public static void makeParentReferences(Expression top) {
        // TODO: this method should be unnecessary; we are generally setting the parent expression
        // as the tree is built. However, it provides a useful safety-net.
        if (top==null) {
            return;
        }
        for (Iterator children = top.iterateSubExpressions(); children.hasNext();) {
            Expression child = (Expression)children.next();
            if (child instanceof ComputedExpression) {
                ((ComputedExpression)child).setParentExpression((ComputedExpression)top);
                makeParentReferences(child);
            }
        }
    }

    /**
     * Get location information for an expression in the form of a SourceLocator
     */

    public static SourceLocator getLocator(Expression exp) {
        if (exp instanceof ComputedExpression) {
            return (ComputedExpression)exp;
        } else {
            return null;
        }
    }

    /**
     * Determine whether an expression is a repeatedly-evaluated subexpression
     * of a parent expression. For example, the predicate in a filter expression is
     * a repeatedly-evaluated subexpression of the filter expression.
     */

    public static boolean isRepeatedSubexpression(Expression parent, Expression child) {
        if (parent instanceof PathExpression) {
            return child == ((PathExpression)parent).getStepExpression();
        }
        if (parent instanceof FilterExpression) {
            return child == ((FilterExpression)parent).getFilter();
        }
        if (parent instanceof ForExpression) {
            return child == ((ForExpression)parent).getAction();
        }
        if (parent instanceof QuantifiedExpression) {
            return child == ((QuantifiedExpression)parent).getAction();
        }
        if (parent instanceof SimpleMappingExpression) {
            return child == ((SimpleMappingExpression)parent).getStepExpression();
        }
        if (parent instanceof SortExpression) {
            return ((SortExpression)parent).isSortKey(child);
        }
        if (parent instanceof TupleSorter) {
            return ((TupleSorter)parent).isSortKey(child);
        }
        if (parent instanceof AnalyzeString) {
            return child == ((AnalyzeString)parent).getMatchingExpression() ||
                    child == ((AnalyzeString)parent).getNonMatchingExpression();
        }
        if (parent instanceof ForEach) {
            return child == ((ForEach)parent).getActionExpression();
        }
        if (parent instanceof ForEachGroup) {
            return child == ((ForEachGroup)parent).getActionExpression();
        }
        if (parent instanceof While) {
            return child == ((While)parent).getActionExpression();
        }
        return false;
    }
    /**
     * Remove unwanted sorting from an expression, at compile time
     */

    public static Expression unsorted(Expression exp, boolean eliminateDuplicates)
    throws XPathException {
        if (exp instanceof Value) return exp;   // fast exit
        PromotionOffer offer = new PromotionOffer();
        offer.action = PromotionOffer.UNORDERED;
        offer.mustEliminateDuplicates = eliminateDuplicates;
        return exp.promote(offer);
    }

    /**
     * Do lazy evaluation of an expression. This will return a value, which may optionally
     * be a SequenceIntent, which is a wrapper around an iterator over the value of the expression.
     *
     * @param context the run-time evaluation context for the expression. If
     *     the expression is not evaluated immediately, then parts of the
     *     context on which the expression depends need to be saved as part of
     *      the Closure
     * @param save indicates whether the value should be saved while it is being
     * evaluated, so that it can be used more than once.
     * @exception XPathException if any error occurs in evaluating the
     *     expression
     * @return a value: either the actual value obtained by evaluating the
     *     expression, or a Closure containing all the information needed to
     *     evaluate it later
     */

    public static Value lazyEvaluate(Expression exp, XPathContext context, boolean save) throws XPathException {
        if (exp instanceof Value) {
            return (Value)exp;
        } else if (exp instanceof VariableReference) {
            // we always dereference the variable reference; this will often
            // do lazy evaluation of the expression to which the variable is bound
            return eagerEvaluate(exp, context);
        } else if (context == null) {
            // this should only happen if we are evaluating a value rather than an expression
            // so this branch is probably unnecessary
            return eagerEvaluate(exp, context);
        } else if (!Cardinality.allowsMany(exp.getCardinality())) {
            // singleton expressions are always evaluated eagerly
            return eagerEvaluate(exp, context);
        } else if ((exp.getDependencies() &
                (   StaticProperty.DEPENDS_ON_POSITION |
                    StaticProperty.DEPENDS_ON_LAST |
                    StaticProperty.DEPENDS_ON_CURRENT_ITEM |
                    StaticProperty.DEPENDS_ON_CURRENT_GROUP |
                    StaticProperty.DEPENDS_ON_REGEX_GROUP )) != 0) {
            // we can't save these values in the closure, so we evaluate
            // the expression now if they are needed
            return eagerEvaluate(exp, context);
        } else {
            // create a Closure, a wrapper for the expression and its context
            return Closure.make(exp, context, save);
        }

    }

    /**
     * Evaluate an expression now; lazy evaluation is not permitted in this case
     * @param exp the expression to be evaluated
     * @param context the run-time evaluation context
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the result of evaluating the expression
     */

    public static Value eagerEvaluate(Expression exp, XPathContext context) throws XPathException {
        if (exp instanceof Value && !(exp instanceof Closure)) {
            return (Value)exp;
        }
        int m = exp.getImplementationMethod();
        if ((m & Expression.ITERATE_METHOD) != 0) {
            SequenceIterator iterator = exp.iterate(context);
            if (iterator instanceof EmptyIterator) {
                return EmptySequence.getInstance();
            } else if (iterator instanceof SingletonIterator) {
                Item item = ((SingletonIterator)iterator).getValue();
                return Value.asValue(item);
            }
            SequenceExtent extent = new SequenceExtent(iterator);
            int len = extent.getLength();
            if (len==0) {
                return EmptySequence.getInstance();
            } else if (len==1) {
                return Value.asValue(extent.itemAt(0));
            } else {
                return extent;
            }

        } else if ((m & Expression.EVALUATE_METHOD) != 0) {
            Item item = exp.evaluateItem(context);
            return Value.asValue(item);

        } else {
            // Use PROCESS_METHOD
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin((InstructionInfoProvider)exp);
            SequenceOutputter seq = new SequenceOutputter();
            seq.setPipelineConfiguration(controller.makePipelineConfiguration());
            c2.setTemporaryReceiver(seq);
            seq.open();
            exp.process(c2);
            seq.close();
            return seq.getSequence();
        }
    }

    public static boolean markTailFunctionCalls(Expression exp) {
        if (exp instanceof ComputedExpression) {
            return ((ComputedExpression)exp).markTailFunctionCalls();
        } else {
            return false;
        }
    }

    /**
     * Construct indent string, for diagnostic output
     *
     * @param level the indentation level (the number of spaces to return)
     * @return a string of "level*2" spaces
     */

    public static String indent(int level) {
        String s = "";
        for (int i=0; i<level; i++) {
            s += "  ";
        }
        return s;
    }

    /**
     * Allocate slot numbers to range variables
     * @param exp the expression whose range variables need to have slot numbers assigned
     * @param nextFree the next slot number that is available for allocation
     * @param frame a SlotManager object that is used to track the mapping of slot numbers
     * to variable names for debugging purposes. May be null.
     * @return the next unallocated slot number.
    */

    public static int allocateSlots(Expression exp, int nextFree, SlotManager frame) {
        if (exp instanceof Assignation) {
            ((Assignation)exp).setSlotNumber(nextFree++);
            if (frame != null) {
                frame.allocateSlotNumber(((Assignation)exp).getVariableFingerprint());
            }
        }
        for (Iterator children = exp.iterateSubExpressions(); children.hasNext();) {
            Expression child = (Expression)children.next();
            nextFree = allocateSlots(child, nextFree, frame);
        }
        return nextFree;

        // Note, we allocate a distinct slot to each range variable, even if the
        // scopes don't overlap. This isn't strictly necessary, but might help
        // debugging.
    }

    /**
     * Determine the effective boolean value of a sequence, given an iterator over the sequence
     * @param iterator AN iterator over the sequence whose effective boolean value is required
     * @return the effective boolean value
     * @throws XPathException if a dynamic error occurs
     */
    public static boolean effectiveBooleanValue(SequenceIterator iterator) throws XPathException {
        Item first = iterator.next();
        if (first == null) {
            return false;
        }
        if (first instanceof NodeInfo) {
            return true;
        } else {
            if (first instanceof BooleanValue) {
                return ((BooleanValue)first).getBooleanValue() || (iterator.next() != null);
            } else if (first instanceof StringValue) {
                return (!first.getStringValue().equals("")) || (iterator.next() != null);
            } else if (first instanceof NumericValue) {
                // first==first is a test for NaN
                return (iterator.next() != null) || (!(first.equals(DoubleValue.ZERO)) && first.equals(first));
            } else {
                return true;
            }
        }
    }


    /**
     * Determine whether an expression depends on any one of a set of variables
     * @param e the expression being tested
     * @param bindingList the set of variables being tested
     * @return true if the expression depends on one of the given variables
     */

    public static boolean dependsOnVariable(Expression e, Binding[] bindingList) {
        if (e instanceof VariableReference) {
            for (int i=0; i<bindingList.length; i++) {
                if (((VariableReference)e).getBinding() == bindingList[i]) {
                    return true;
                }
            }
            return false;
        } else {
            for (Iterator children = e.iterateSubExpressions(); children.hasNext();) {
                Expression child = (Expression)children.next();
                if (dependsOnVariable(child, bindingList)) {
                    return true;
                }
            }
            return false;
        }
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
