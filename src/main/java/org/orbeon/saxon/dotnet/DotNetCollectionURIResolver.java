package org.orbeon.saxon.dotnet;

import cli.System.Collections.IEnumerable;
import cli.System.Collections.IEnumerator;
import cli.System.Uri;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.functions.StandardCollectionURIResolver;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.value.StringValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * This class implements the CollectionURIResolver interface by wrapping an IEnumerable which
 * returns Uri values (the URIs of the documents in the collection)
 */

public class DotNetCollectionURIResolver extends StandardCollectionURIResolver {

    private HashMap registeredCollections = new HashMap(20);

    public DotNetCollectionURIResolver() {}

    public void registerCollection(String uri, IEnumerable enumerable) {
        if (enumerable == null) {
            registeredCollections.remove(uri);
        } else if (uri == null) {
            registeredCollections.put("", enumerable);
        } else {
            registeredCollections.put(uri, enumerable);
        }
    }

    /**
     * Resolve a URI.
     *
     * @param href    The relative URI of the collection. This corresponds to the
     *                argument supplied to the collection() function. If the collection() function
     *                was called with no arguments (to get the "default collection") this argument
     *                will be null.
     * @param base    The base URI that should be used. This is the base URI of the
     *                static context in which the call to collection() was made, typically the URI
     *                of the stylesheet or query module
     * @param context The dynamic execution context
     * @return an Iterator over the documents in the collection. The items returned
     *         by this iterator must be instances either of xs:anyURI, or of node() (specifically,
     *         {@link org.orbeon.saxon.om.NodeInfo}.). If xs:anyURI values are returned, the corresponding
     *         document will be retrieved as if by a call to the doc() function: this means that
     *         the system first checks to see if the document is already loaded, and if not, calls
     *         the registered URIResolver to dereference the URI. This is the recommended approach
     *         to ensure that the resulting collection is stable: however, it has the consequence
     *         that the documents will by default remain in memory for the duration of the query
     *         or transformation.
     *         <p/>
     *         If the URI is not recognized, the method may either return an empty iterator,
     *         in which case no error is reported, or it may throw an exception, in which case
     *         the query or transformation fails. Returning null has the same effect as returning
     *         an empty iterator.
     */

    public SequenceIterator resolve(String href, String base, XPathContext context) throws XPathException {
        if (href == null) {
            IEnumerable ie = (IEnumerable)registeredCollections.get("");
            if (ie == null) {
                return super.resolve(href, base, context);
//                XPathException de = new XPathException("Default collection is undefined");
//                de.setErrorCode("FODC0002");
//                de.setXPathContext(context);
//                throw de;
            }
            return new UriIterator(ie.GetEnumerator());
        }
        URI abs;
        try {
            abs = new URI(base).resolve(href);
        } catch (URISyntaxException err) {
            XPathException de = new XPathException("Invalid collection URI " + base + ", " + href);
            de.setErrorCode("FODC0002");
            de.setXPathContext(context);
            throw de;
        }

        IEnumerable ie = (IEnumerable)registeredCollections.get(abs.toString());
        if (ie == null) {
            return super.resolve(href, base, context);
//            XPathException err = new XPathException("Unknown collection " + abs);
//            err.setErrorCode("FODC0004");
//            err.setXPathContext(context);
//            throw err;
        }
        return new UriIterator(ie.GetEnumerator());
    }

    private static class UriIterator implements SequenceIterator {

        private IEnumerator enumerator;
        private StringValue current;
        private int position = 0;

        public UriIterator(IEnumerator enumerator) {
            this.enumerator = enumerator;
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
            if (enumerator.MoveNext()) {
                Uri u = (Uri)enumerator.get_Current();
                current = new AnyURIValue(u.ToString());
                position++;
                return current;
            } else {
                position = -1;
                return null;
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
            if (position < 1) {
                enumerator.Reset();
                return this;
            } else {
                // TODO: this is a problem if someone does collection()[last()]
                throw new UnsupportedOperationException("Can't replicate collection iterator");
            }
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

