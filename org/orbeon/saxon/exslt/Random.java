package org.orbeon.saxon.exslt;

import org.orbeon.saxon.om.AxisIteratorImpl;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.value.DoubleValue;
import org.orbeon.saxon.xpath.XPathException;

/**
 * This class implements extension functions in the
 * http://exslt.org/random namespace.
 *
 * @author Martin Szugat
 * @version 1.0, 30.06.2004
 * Rewritten by Michael Kay to generate a SequenceIterator
 */
public abstract class Random {

	/**
	 * Returns a sequence of random numbers
	 * between 0 and 1.
	 * @param numberOfItems number of random items
	 * in the sequence.
	 * @param seed the initial seed.
	 * @return sequence of random numbers as an iterator.
	 * @throws IllegalArgumentException
	 * <code>numberOfItems</code> is not positive.
	 */
	public static SequenceIterator randomSequence(int numberOfItems, double seed)
	throws IllegalArgumentException {
		if (numberOfItems < 1) {
			throw new IllegalArgumentException("numberOfItems supplied to randomSequence() must be positive");
		}
        long javaSeed = Double.doubleToLongBits(seed);
        return new RandomIterator(numberOfItems, javaSeed);
	}

	/**
	 * Returns a sequence of random numbers
	 * between 0 and 1.
	 * @param numberOfItems number of random items
	 * in the sequence.
	 * @return sequence of random numbers.
	 * @throws IllegalArgumentException
	 * <code>numberOfItems</code> is not positive.
	 */
	public static SequenceIterator randomSequence(int numberOfItems)
	throws IllegalArgumentException {
		return randomSequence(numberOfItems, System.currentTimeMillis());
	}

	/**
	 * Returns a single random number                                                               X
	 * between 0 and 1.
	 * @return sequence random number.
	 */
	public static DoubleValue randomSequence() throws XPathException {
		return (DoubleValue)randomSequence(1).next();
	}

    /**
     * Iterator over a sequence of random numbers
     */

    private static class RandomIterator extends AxisIteratorImpl {

        private int count;
        private long seed;
        private java.util.Random generator;

        public RandomIterator(int count, long seed) {
            this.count = count;
            this.seed = seed;
            generator = new java.util.Random(seed);
        }

        /**
         * Get the next item in the sequence. <BR>
         * @return the next item, or null if there are no more items.
         */

        public Item next() {
            if (position++ >= count) {
                return null;
            } else {
                current = new DoubleValue(generator.nextDouble());
                return current;
            }
        }

        /**
         * Get another SequenceIterator that iterates over the same items as the original,
         * but which is repositioned at the start of the sequence.
         *
         * @return a SequenceIterator that iterates over the same items,
         *     positioned before the first item
         */

        public SequenceIterator getAnother() {
            return new RandomIterator(count, seed);
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
// The Initial Developer of the Original Code is Martin Szugat.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): Michael H. Kay.
//