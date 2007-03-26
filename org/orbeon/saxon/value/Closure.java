package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;

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
     * Constructor should not be called directly, instances should be made using the make() method.
     */

    public Closure() {
    }

    /**
     * Construct a Closure over an existing SequenceIterator. This is used when an extension function
     * returns a SequenceIterator as its result (it replaces the old SequenceIntent class for this
     * purpose). There is no known expression in this case. Note that the caller must
     * ensure this is a "clean" iterator: it must be positioned at the start, and must
     * not be shared by anyone else.
     */

    public static Closure makeIteratorClosure(SequenceIterator iterator) {
        Closure c = new Closure();
        c.inputIterator = iterator;
        return c;
    }

    /**
    * Construct a Closure by supplying the expression and the set of context variables.
    */

    public static Value make(Expression expression, XPathContext context, int ref) throws XPathException {

        // special cases such as TailExpressions and shared append expressions are now picked up before
        // this method is called (where possible, at compile time)

        Closure c = context.getConfiguration().getOptimizer().makeClosure(expression, ref);
        c.expression = expression;
        c.savedXPathContext = context.newContext();
        c.savedXPathContext.setOriginatingConstructType(Location.LAZY_EVALUATION);

        // Make a copy of all local variables. If the value of any local variable is a closure
        // whose depth exceeds a certain threshold, we evaluate the closure eagerly to avoid
        // creating deeply nested lists of Closures, which consume memory unnecessarily

        // We only copy the local variables if the expression has dependencies on local variables.
        // What's more, we only copy those variables that the expression actually depends on.

        if ((expression.getDependencies() & StaticProperty.DEPENDS_ON_LOCAL_VARIABLES) != 0) {
            StackFrame localStackFrame = context.getStackFrame();
            ValueRepresentation[] local = localStackFrame.getStackFrameValues();
            int[] slotsUsed = ((ComputedExpression)expression).getSlotsUsed();  // computed on first call
            if (local != null) {
                final SlotManager stackFrameMap = localStackFrame.getStackFrameMap();
                final ValueRepresentation[] savedStackFrame = 
                        new ValueRepresentation[stackFrameMap.getNumberOfVariables()];
                for (int s=0; s<slotsUsed.length; s++) {
                    int i = slotsUsed[s];
                    if (local[i] instanceof Closure) {
                        int cdepth = ((Closure)local[i]).depth;
                        if (cdepth >= 10) {
                            local[i] = SequenceExtent.makeSequenceExtent(((Closure)local[i]).iterate(context));
                        } else if (cdepth + 1 > c.depth) {
                            c.depth = cdepth + 1;
                        }
                    }
                    savedStackFrame[i] = local[i];
                }

                c.savedXPathContext.setStackFrame(stackFrameMap, savedStackFrame);
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

        c.savedXPathContext.setReceiver(null);
        return c;
    }

    /**
    * Get the static item type
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (expression == null) {
            return AnyItemType.getInstance();
        } else {
            return expression.getItemType(th);
        }
        // This is probably rarely used, because a Closure has no static existence, and
        // run-time polymorphism applies mainly to singletons, which are rarely evaluated as Closures.
    }

    /**
    * Get the cardinality
    */

    public int getCardinality() {
        if (expression == null) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return expression.getCardinality();
        }
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int getSpecialProperties() {
        if (expression == null) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return expression.getSpecialProperties();
        }
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
            // In an ideal world this shouldn't happen: if the value is needed more than once, we should
            // have chosen a MemoClosure. In fact, this path is never taken when executing the standard
            // test suite (April 2005). However, it provides robustness in case the compile-time analysis
            // is flawed. I believe it's also possible that this path can be taken if a Closure needs to be
            // evaluated when the chain of dependencies gets too long: this was happening routinely when
            // all local variables were saved, rather than only those that the expression depends on.
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
        // expression for everything except the current output destination, which is newly created
        XPathContext c2 = savedXPathContext.newContext();
        SequenceReceiver out = context.getReceiver();
        c2.setTemporaryReceiver(out);
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

    /**
     * Determine whether this Closure is indexable
     */

    public boolean isIndexable() {
        return false;
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "Closure of expression:");
        expression.display(level+1, out, config);
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
