package net.sf.saxon.value;
import net.sf.saxon.event.SequenceOutputter;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import java.io.PrintStream;

/**
 * A Closure represents a value that has not yet been evaluated: the value is represented
 * by an expression, together with saved values of all the context variables that the
 * expression depends on.
 *
 * <p>This Closure is designed for use when the value is only read once. If the value
 * is read more than once, a new iterator over the underlying expression is obtained
 * each time: this may (for example in the case of a filter expression) involve
 * significant re-calculation.</p>
 *
 * <p>The expression may depend on local variables and on the context item; these values
 * are held in the saved XPathContext object that is kept as part of the Closure, and they
 * will always be read from that object. The expression may also depend on global variables;
 * these are unchanging, so they can be read from the Bindery in the normal way. Expressions
 * that depend on other contextual information, for example the values of position(), last(),
 * current(), current-group(), should not be evaluated using this mechanism: they should
 * always be evaluated eagerly. This means that the Closure does not need to keep a copy
 * of these context variables.</p>
 *
*/

public class Closure extends Value {

    protected Expression expression;
    protected XPathContextMajor savedXPathContext;
    protected int depth = 0;

    // The base iterator is used to copy items on demand from the underlying value
    // to the reservoir. It only ever has one instance (for each Closure) and each
    // item is read only once.

    protected SequenceIterator inputIterator;

//    private static int countClosures = 0;
//    private static int countMemoClosures = 0;

    /**
     * Private constructor: instances must be created using the make() method
     */

    protected Closure() {}

    /**
    * Construct a Closure by supplying the expression and the set of context variables.
    */

    public static Value make(Expression expression, XPathContext context, boolean save) throws XPathException {

        // Don't allow lazy evaluation of an ErrorExpression, the results are too confusing

        if (expression instanceof ErrorExpression) {
            expression.evaluateItem(context);    // throws the exception
            return null;                         // keep the compiler happy
        }

        // Treat tail recursion as a special case, to avoid creating a deeply-nested
        // tree of Closures. If this expression is a TailExpression, and its first
        // argument is also a TailExpression, we combine the two TailExpressions into
        // one and return a closure over that.

        if (expression instanceof TailExpression) {
            TailExpression tail = (TailExpression)expression;
            Expression base = tail.getBaseExpression();
            if (base instanceof VariableReference) {
                base = Value.asValue(ExpressionTool.lazyEvaluate(base, context, save));
                if (base instanceof MemoClosure) {
                    SequenceIterator it = base.iterate(null);
                    base = ((GroundedIterator)it).materialize();
                }
                if (base instanceof IntegerRange) {
                    long start = ((IntegerRange)base).getStart() + 1;
                    long end = ((IntegerRange)base).getEnd();
                    if (start == end) {
                        return new IntegerValue(end);
                    } else {
                        return new IntegerRange(start, end);
                    }
                }
                if (base instanceof SequenceExtent) {
                    return new SequenceExtent(
                            (SequenceExtent)base,
                            tail.getStart() - 1,
                            ((SequenceExtent)base).getLength() - tail.getStart() + 1);
                }
            }
        }

        Closure c = (save? new MemoClosure() : new Closure());
//        if (save) {
//            countMemoClosures++;
//            if (countMemoClosures % 100 == 0) System.err.println("MEMO_CLOSURES " + countMemoClosures);
//        } else {
//            countClosures++;
//            if (countClosures % 100 == 0) System.err.println("CLOSURES " + countClosures);
//        }
        c.expression = expression;
        c.savedXPathContext = context.newContext();
        c.savedXPathContext.setOriginatingConstructType(Location.LAZY_EVALUATION);

        // Make a copy of all local variables. If the value of any local variable is a closure
        // whose depth exceeds a certain threshold, we evaluate the closure eagerly to avoid
        // creating deeply nested lists of Closures, which consume memory unnecessarily

        // We only copy the local variables if the expression has dependencies on local variables.
        // It would be even smarter to copy only those variables that we need; but that gives
        // diminishing returns.

        if ((expression.getDependencies() & StaticProperty.DEPENDS_ON_LOCAL_VARIABLES) != 0) {
            StackFrame localStackFrame = context.getStackFrame();
            ValueRepresentation[] local = localStackFrame.getStackFrameValues();
            if (local != null) {
                ValueRepresentation[] savedStackFrame = new ValueRepresentation[local.length];
                for (int i=0; i<local.length; i++) {
                    if (local[i] instanceof Closure) {
                        int cdepth = ((Closure)local[i]).depth;
                        if (cdepth >= 10) {
                            local[i] = ExpressionTool.eagerEvaluate((Closure)local[i], context);
                        } else if (cdepth + 1 > c.depth) {
                            c.depth = cdepth + 1;
                        }
                    }
                    savedStackFrame[i] = local[i];
                }
                c.savedXPathContext.setStackFrame(localStackFrame.getStackFrameMap(), savedStackFrame);
            }
        }

        // Make a copy of the context item
        SequenceIterator currentIterator = context.getCurrentIterator();
        if (currentIterator != null) {
            Item contextItem = currentIterator.current();
            AxisIterator single = SingletonIterator.makeIterator(contextItem);
            single.next();
            c.savedXPathContext.setCurrentIterator(single);
            // we don't save position() and last() because we have no way
            // of restoring them. So the caller must ensure that a Closure is not
            // created if the expression depends on position() or last()
        }

        c.savedXPathContext.setReceiver(new SequenceOutputter());
            // TODO: creating this SequenceOutputter is expensive and in most cases it's never used: delay it

        return c;
    }

    /**
    * Get the static item type
    */

    public ItemType getItemType() {
        return expression.getItemType();
        // This is probably not used, because a Closure has no static existence
    }

    /**
    * Get the cardinality
    */

    public int getCardinality() {
        return expression.getCardinality();
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int getSpecialProperties() {
        return expression.getSpecialProperties();
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
    }

    /**
     * Evaluate the expression in a given context to return an iterator over a sequence
     * @param context the evaluation context. This is ignored; we use the context saved
     * as part of the Closure instead.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (inputIterator == null) {
            inputIterator = expression.iterate(savedXPathContext);
            return inputIterator;
        } else {
            // This is usually bad news: we have to evaluate the expression again. It would have been
            // better to use a MemoClosure. But we struggle on regardless.
            return inputIterator.getAnother();
        }
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        // To evaluate the closure in push mode, we need to use the original context of the
        // expression for everything except the current output destination, which is taken from the
        // context supplied at evaluation time
        XPathContext c2 = savedXPathContext.newContext();
        c2.setTemporaryReceiver(context.getReceiver());
        expression.process(c2);
    }

    /**
     * Reduce a value to its simplest form. If the value is a closure or some other form of deferred value
     * such as a FunctionCallPackage, then it is reduced to a SequenceExtent. If it is a SequenceExtent containing
     * a single item, then it is reduced to that item. One consequence that is exploited by class FilterExpression
     * is that if the value is a singleton numeric value, then the result will be an instance of NumericValue
     */

    public Value reduce() throws XPathException {
        return new SequenceExtent(iterate(null)).reduce();
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "Closure of expression:");
        expression.display(level+1, pool, out);
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
