package net.sf.saxon.value;

import net.sf.saxon.Controller;
import net.sf.saxon.event.SequenceOutputter;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.event.TeeOutputter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

import java.util.ArrayList;

/**
 * A MemoClosure represents a value that has not yet been evaluated: the value is represented
 * by an expression, together with saved values of all the context variables that the
 * expression depends on.
 * <p/>
 * <p>The MemoClosure is designed for use when the value is only read several times. The
 * value is saved on the first evaluation and remembered for later use.</p>
 * <p/>
 * <p>The MemoClosure maintains a reservoir containing those items in the value that have
 * already been read. When a new iterator is requested to read the value, this iterator
 * first examines and returns any items already placed in the reservoir by previous
 * users of the MemoClosure. When the reservoir is exhausted, it then uses an underlying
 * Input Iterator to read further values of the underlying expression. If the value is
 * not read to completion (for example, if the first user did exists($expr), then the
 * Input Iterator is left positioned where this user abandoned it. The next user will read
 * any values left in the reservoir by the first user, and then pick up iterating the
 * base expression where the first user left off. Eventually, all the values of the
 * expression will find their way into the reservoir, and future users simply iterate
 * over the reservoir contents. Alternatively, of course, the values may be left unread.</p>
 * <p/>
 * <p>Delayed evaluation is used only for expressions with a static type that allows
 * more than one item, so the evaluateItem() method will not normally be used, but it is
 * supported for completeness.</p>
 * <p/>
 * <p>The expression may depend on local variables and on the context item; these values
 * are held in the saved XPathContext object that is kept as part of the Closure, and they
 * will always be read from that object. The expression may also depend on global variables;
 * these are unchanging, so they can be read from the Bindery in the normal way. Expressions
 * that depend on other contextual information, for example the values of position(), last(),
 * current(), current-group(), should not be evaluated using this mechanism: they should
 * always be evaluated eagerly. This means that the Closure does not need to keep a copy
 * of these context variables.</p>
 */

public final class MemoClosure extends Closure {

    private ArrayList reservoir = null;
    private int state;

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
     * Protected constructor: instances must be created using the make() method
     */

    MemoClosure() {
    }

    /**
     * Evaluate the expression in a given context to return an iterator over a sequence
     *
     * @param context the evaluation context. This is ignored; we use the context saved
     *                as part of the Closure instead.
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        switch (state) {
            case UNREAD:
                state = BUSY;
                inputIterator = expression.iterate(savedXPathContext);
                reservoir = new ArrayList(50);
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
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        // To evaluate the closure in push mode, we need to use the original context of the
        // expression for everything except the current output destination, which is taken from the
        // context supplied at evaluation time
        if (reservoir != null) {
            SequenceIterator iter = iterate(context);
            SequenceReceiver out = context.getReceiver();
            while (true) {
                Item it = iter.next();
                if (it==null) break;
                out.append(it, 0);
            }
        } else {
            Controller controller = context.getController();
            XPathContext c2 = savedXPathContext.newMinorContext();
            //c2.setOrigin(this);
            // Fork the output: one copy goes to a SequenceOutputter which remembers the contents for
            // use next time the variable is referenced; another copy goes to the current output destination.
            SequenceOutputter seq = new SequenceOutputter();
            seq.setPipelineConfiguration(controller.makePipelineConfiguration());
            seq.open();
            TeeOutputter tee = new TeeOutputter(context.getReceiver(), seq);
            tee.setPipelineConfiguration(controller.makePipelineConfiguration());
            c2.setTemporaryReceiver(tee);

            expression.process(c2);

            seq.close();
            reservoir = seq.getList();
            state = ALL_READ;
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
                if (i == null) {
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
