package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.sort.GenericAtomicComparer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Int64Value;


/**
* The XPath 2.0 index-of() function
*/


public class IndexOf extends CollatingFunction {

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        GenericAtomicComparer comparer = getAtomicComparer(2, context);
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue val = (AtomicValue)argument[1].evaluateItem(context);
        return new IndexIterator(seq, val, comparer);
    }

    /**
     * Iterator to return the index positions of selected items in a sequence
     */

    public static class IndexIterator implements SequenceIterator {

        SequenceIterator base;
        AtomicValue value;
        GenericAtomicComparer comparer;
        int index = 0;
        int position = 0;
        Item current = null;
        BuiltInAtomicType primitiveTypeRequired;
        TypeHierarchy typeHierarchy;

        /**
         * Get an iterator returning the index positions of selected items in a sequence
         * @param base The sequence to be searched
         * @param value The value being sought
         * @param comparer Comparer used to determine whether values match
         */

        public IndexIterator(SequenceIterator base, AtomicValue value, GenericAtomicComparer comparer) {
            this.base = base;
            this.value = value;
            this.comparer = comparer;
            primitiveTypeRequired = value.getPrimitiveType();
        }

        public Item next() throws XPathException {
            while (true) {
                AtomicValue i = (AtomicValue)base.next();
                if (i==null) break;
                index++;
                if (Type.isComparable(primitiveTypeRequired,
                            i.getPrimitiveType(), false)) {
                    try {
                        if (comparer.comparesEqual(i, value)) {
                            current = Int64Value.makeIntegerValue(index);
                            position++;
                            return current;
                        }
                    } catch (ClassCastException err) {
                        // non-comparable values are treated as not equal
                        // Exception shouldn't happen but we catch it anyway
                    }
                }
            }
            current = null;
            position = -1;
            return null;
        }

        public Item current() {
            return current;
        }

        public int position() {
            return position;
        }

        public void close() {
            base.close();
        }

        public SequenceIterator getAnother() throws XPathException {
            return new IndexIterator(base.getAnother(), value, comparer);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link SequenceIterator#GROUNDED}, {@link SequenceIterator#LAST_POSITION_FINDER},
         *         and {@link SequenceIterator#LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
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
