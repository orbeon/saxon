package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.TreeReceiver;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.query.QueryResult;

import javax.xml.transform.stream.StreamSource;
import java.io.File;

/**
 * This class takes a sequence of pull events and composes them into a sequence of items. This involves building
 * any element or document nodes that are presented in decomposed form.
 *
 * <p>Note: this SequenceIterator does not implement the <code>getAnother()</code> method, which limits its use,
 * since <code>getAnother()</code> is needed to support the XPath <code>last()</code> function.
 */

public class SequenceComposer implements SequenceIterator {

    private EventIterator base;
    private int position = 0;
    private Item current = null;
    private PipelineConfiguration pipe;

    /**
     * Create a sequence composer
     * @param iter the underlying event iterator
     * @param pipe the pipeline configuration
     */

    public SequenceComposer(EventIterator iter, PipelineConfiguration pipe) {
        base = EventStackIterator.flatten(iter);
        this.pipe = pipe;
    }

    /**
     * Get the next item in the sequence. This method changes the state of the
     * iterator, in particular it affects the result of subsequent calls of
     * position() and current().
     *
     * @return the next item, or null if there are no more items. Once a call
     *         on next() has returned null, no further calls should be made. The preferred
     *         action for an iterator if subsequent calls on next() are made is to return
     *         null again, and all implementations within Saxon follow this rule.
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error occurs retrieving the next item
     * @since 8.4
     */

    public Item next() throws XPathException {
        PullEvent pe = base.next();
        if (pe == null) {
            position = -1;
            current = null;
            return null;
        }
        if (pe instanceof Item) {
            current = (Item)pe;
            position++;
            return current;
        } else if (pe instanceof StartDocumentEvent || pe instanceof StartElementEvent) {
            SubtreeIterator sub = new SubtreeIterator(base, pe);
            TinyBuilder builder = new TinyBuilder();
            builder.setPipelineConfiguration(pipe);
            TreeReceiver receiver = new TreeReceiver(builder);
            builder.setPipelineConfiguration(pipe);
            EventIteratorToReceiver.copy(sub, receiver);
            current = builder.getCurrentRoot();
            position++;
            return current;
        } else {
            throw new IllegalStateException(pe.getClass().getName());
        }
    }


    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     *
     * @return the current item, the one most recently returned by a call on
     *         next(). Returns null if next() has not been called, or if the end
     *         of the sequence has been reached.
     * @since 8.4
     */

    public Item current() {
        return current;
    }

    public void close() {

    }

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     * <p/>
     * This method allows access to all the items in the sequence without disturbing the
     * current position of the iterator. Internally, its main use is in evaluating the last()
     * function.
     * <p/>
     * This method does not change the state of the iterator.
     *
     * @return a SequenceIterator that iterates over the same items,
     *         positioned before the first item
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any error occurs
     * @since 8.4
     */

    public SequenceIterator getAnother() throws XPathException {
        throw new UnsupportedOperationException("getAnother");
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     * @since 8.6
     */

    public int getProperties() {
        return 0;
    }

    /**
     * Get the current position. This will usually be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called. Once next() has returned null, the preferred action is
     * for subsequent calls on position() to return -1, but not all existing
     * implementations follow this practice. (In particular, the EmptyIterator
     * is stateless, and always returns 0 as the value of position(), whether
     * or not next() has been called.)
     * <p/>
     * This method does not change the state of the iterator.
     *
     * @return the current position, the position of the item returned by the
     *         most recent call of next(). This is 1 after next() has been successfully
     *         called once, 2 after it has been called twice, and so on. If next() has
     *         never been called, the method returns zero. If the end of the sequence
     *         has been reached, the value returned will always be <= 0; the preferred
     *         value is -1.
     * @since 8.4
     */

    public int position() {
        return position;
    }

    private static class SubtreeIterator implements EventIterator {

        private int level = 0;
        private EventIterator base;
        private PullEvent first;

        public SubtreeIterator(EventIterator base, PullEvent first) {
            this.base = base;
            this.first = first;
        }

        /**
         * Get the next event in the sequence
         *
         * @return the next event, or null when the sequence is exhausted. Note that since an EventIterator is
         *         itself a PullEvent, this method may return a nested iterator.
         * @throws org.orbeon.saxon.trans.XPathException
         *          if a dynamic evaluation error occurs
         */

        public PullEvent next() throws XPathException {
            if (first != null) {
                PullEvent pe = first;
                first = null;
                return pe;
            }
            if (level < 0) {
                return null;
            }
            PullEvent pe = base.next();
            if (pe instanceof StartElementEvent || pe instanceof StartDocumentEvent) {
                level++;
            } else if (pe instanceof EndElementEvent || pe instanceof EndDocumentEvent) {
                level--;
            }
            return pe;
        }


        /**
         * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
         * nested event iterators
         *
         * @return true if the next() method is guaranteed never to return an EventIterator
         */

        public boolean isFlatSequence() {
            return base.isFlatSequence();
        }
    }

    /**
     * Main method for testing only
     * @param args not used
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration();
        DocumentInfo doc = config.buildDocument(new StreamSource(new File("c:/MyJava/samples/data/books.xml")));
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        pipe.setHostLanguage(Configuration.XQUERY);
        EventIterator e = new Decomposer(new SingletonEventIterator(doc), pipe);
        SequenceIterator iter = new SequenceComposer(e, pipe);
        while (true) {
            NodeInfo item = (NodeInfo)iter.next();
            if (item == null) {
                break;
            }
            System.out.println(QueryResult.serialize(item));
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

