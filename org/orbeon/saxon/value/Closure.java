package net.sf.saxon.value;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.Configuration;
import net.sf.saxon.trace.Location;

import java.util.ArrayList;
import java.io.PrintStream;

/**
 * A Closure represents a value that has not yet been evaluated: the value is represented
 * by an expression, together with saved values of all the context variables that the
 * expression depends on.
 *
 * <p>The Closure maintains a reservoir containing those items in the value that have
 * already been read. When a new iterator is requested to read the value, this iterator
 * first examines and returns any items already placed in the reservoir by previous
 * users of the Closure. When the reservoir is exhausted, it then uses an underlying
 * Input Iterator to read further values of the underlying expression. If the value is
 * not read to completion (for example, if the first user did exists($expr), then the
 * Input Iterator is left positioned where this user abandoned it. The next user will read
 * any values left in the reservoir by the first user, and then pick up iterating the
 * base expression where the first user left off. Eventually, all the values of the
 * expression will find their way into the reservoir, and future users simply iterate
 * over the reservoir contents. Alternatively, of course, the values may be left unread.</p>
 *
 * <p>Delayed evaluation is used only for expressions with a static type that allows
 * more than one item, so the evaluateItem() method will not normally be used, but it is
 * supported for completeness.</p>
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

// This class replaces the class SequenceIntent used in earlier releases. It can handle scalar
// values as well as sequences, and it saves the context variables explicitly without requiring
// a reduced copy of the expression to be made.

// TODO: determine statically whether a variable is used once only, and in such cases don't save
// the value when it's first evaluated.

public final class Closure extends SequenceValue {

    private Expression expression;
    private XPathContextMajor savedXPathContext;
    private ArrayList reservoir = new ArrayList(50);
    private int state;
    private int depth = 0;

    // State in which no items have yet been read
    private static final int UNREAD = 0;

    // State in which zero or more items are in the reservoir and it is not known
    // whether more items exist
    private static final int MAYBE_MORE = 1;

    // State in which all the items are in the reservoir
    private static final int ALL_READ = 3;

    // State in which we are getting the base iterator. If the closure is called in this state,
    // it indicates a recursive entry, which is only possible on an error path
    private static final int BUSY = 4;

    // The base iterator is used to copy items on demand from the underlying value
    // to the reservoir. It only ever has one instance (for each Closure) and each
    // item is read only once.

    SequenceIterator inputIterator;

    /**
     * Private constructor: instances must be created using the make() method
     */

    private Closure() {}

    /**
    * Construct a Closure by supplying the expression and the set of context variables.
    */

    public static Value make(Expression expression, XPathContext context) throws XPathException {

        // Treat tail recursion as a special case, to avoid creating a deeply-nested
        // tree of Closures. If this expression is a TailExpression, and its first
        // argument is also a TailExpression, we combine the two TailExpressions into
        // one and return a closure over that.

        if (expression instanceof TailExpression) {
            TailExpression tail = (TailExpression)expression;
            Expression base = tail.getBaseExpression();
            if (base instanceof VariableReference) {
                base = ExpressionTool.lazyEvaluate(base, context);
                if (base instanceof SequenceExtent) {
                    return new SequenceExtent(
                            (SequenceExtent)base,
                            tail.getStart() - 1,
                            ((SequenceExtent)base).getLength() - tail.getStart() + 1);
                }
            }
        }

        Closure c = new Closure();
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

        c.state = UNREAD;
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
     * Evaluate as a singleton. We don't use a Closure for singleton expressions,
     * so this method shouldn't be called, but we implement it anyway for safety.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return iterate(context).next();
    }

    /**
     * Evaluate the expression in a given context to return an iterator over a sequence
     * @param context the evaluation context. This is ignored; we use the context saved
     * as part of the Closure instead.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        switch (state) {
            case UNREAD:
                state = BUSY;
                inputIterator = expression.iterate(savedXPathContext);
                state = MAYBE_MORE;
                return new ProgressiveIterator();

            case MAYBE_MORE:
                return new ProgressiveIterator();

            case ALL_READ:
                return new ListIterator(reservoir);

            case BUSY:
                // this indicates a recursive entry, probably on an error path while printing diagnostics
                throw new DynamicError("Attempt to access a lazily-evaluated variable while it is being evaluated");

            default:
                throw new IllegalStateException("Unknown iterator state");

        }
    }

    /**
    * Get the n'th item in the sequence (starting from 0). This is defined for all
    * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
    */

    public Item itemAt(int n) throws XPathException {
        if (n < reservoir.size()) {
            return (Item)reservoir.get(n);
        } else {
            return super.itemAt(n);
        }
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

    /**
     * A ProgressiveIterator starts by reading any items already held in the reservoir;
     * when the reservoir is exhausted, it reads further items from the inputIterator,
     * copying them into the reservoir as they are read.
     */

    public final class ProgressiveIterator implements SequenceIterator {

        int position = -1;  // zero-based position in the reservoir of the
                            // item most recently read

        public ProgressiveIterator() {

        }

        public Item next() throws XPathException {
            if (++position < reservoir.size()) {
                return (Item)reservoir.get(position);
            } else {
                Item i = inputIterator.next();
                if (i==null) {
                    state = ALL_READ;
                    position--;     // leave position at last item
                    return null;
                }
                position = reservoir.size();
                reservoir.add(i);
                state = MAYBE_MORE;
                return i;
            }
        }

        public Item current() {
            return (Item)reservoir.get(position);
        }

        public int position() {
            return position + 1;    // return one-based position
        }

        public SequenceIterator getAnother() {
            return new ProgressiveIterator();
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
