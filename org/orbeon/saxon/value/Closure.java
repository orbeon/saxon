package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.SequenceOutputter;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.xpath.XPathException;

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

public class Closure extends SequenceValue {

    protected Expression expression;
    protected XPathContextMajor savedXPathContext;
    protected int depth = 0;

    // The base iterator is used to copy items on demand from the underlying value
    // to the reservoir. It only ever has one instance (for each Closure) and each
    // item is read only once.

    SequenceIterator inputIterator;

    /**
     * Private constructor: instances must be created using the make() method
     */

    protected Closure() {}

    /**
    * Construct a Closure by supplying the expression and the set of context variables.
    */

    public static Value make(Expression expression, XPathContext context, boolean save) throws XPathException {

        // Treat tail recursion as a special case, to avoid creating a deeply-nested
        // tree of Closures. If this expression is a TailExpression, and its first
        // argument is also a TailExpression, we combine the two TailExpressions into
        // one and return a closure over that.

        if (expression instanceof TailExpression) {
            TailExpression tail = (TailExpression)expression;
            Expression base = tail.getBaseExpression();
            if (base instanceof VariableReference) {
                base = ExpressionTool.lazyEvaluate(base, context, save);
                if (base instanceof SequenceExtent) {
                    return new SequenceExtent(
                            (SequenceExtent)base,
                            tail.getStart() - 1,
                            ((SequenceExtent)base).getLength() - tail.getStart() + 1);
                }
            }
        }

        Closure c = (save? new MemoClosure() : new Closure());
        c.expression = expression;
        c.savedXPathContext = context.newContext();
        c.savedXPathContext.setOriginatingConstructType(Location.LAZY_EVALUATION);

        // Make a copy of all local variables. If the value of any local variable is a closure
        // whose depth exceeds a certain threshold, we evaluate the closure eagerly to avoid
        // creating deeply nested lists of Closures, which consume memory unnecessarily

        // TODO: only save those local variables that the expression actually depends on.
        // Saving variables unnecessarily prevents the values being garbage collected.

        StackFrame localStackFrame = context.getStackFrame();
        Value[] local = localStackFrame.getStackFrameValues();
        if (local != null) {
            Value[] savedStackFrame = new Value[local.length];
            //System.arraycopy(local, 0, savedStackFrame, 0, local.length);
            for (int i=0; i<local.length; i++) {
                if (local[i] instanceof Closure) {
                    int cdepth = ((Closure)local[i]).depth;
                    if (cdepth >= 10) {
                        local[i] = ExpressionTool.eagerEvaluate(local[i], context);
                    } else if (cdepth + 1 > c.depth) {
                        c.depth = cdepth + 1;
                    }
                }
                savedStackFrame[i] = local[i];
            }
            c.savedXPathContext.setStackFrame(localStackFrame.getStackFrameMap(), savedStackFrame);
        }

        // Make a copy of the context item
        SequenceIterator currentIterator = context.getCurrentIterator();
        if (currentIterator != null) {
            Item contextItem = currentIterator.current();
            c.savedXPathContext.setCurrentIterator(SingletonIterator.makeIterator(contextItem));
            // we don't save position() and last() because we have no way
            // of restoring them. So the caller must ensure that a Closure is not
            // created if the expression depends on position() or last()
        }

        c.savedXPathContext.setReceiver(new SequenceOutputter());

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
    * Get the n'th item in the sequence (starting from 0). This is defined for all
    * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
    */

    public Item itemAt(int n) throws XPathException {
        return super.itemAt(n);
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        Value val = ExpressionTool.eagerEvaluate(this, null);
        return val.convertToJava(target, config, context);
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
