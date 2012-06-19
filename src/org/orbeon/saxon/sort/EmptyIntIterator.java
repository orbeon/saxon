package org.orbeon.saxon.sort;

/**
 * An iterator over a zero-length sequence of integers
 */
public class EmptyIntIterator implements IntIterator {

    private static EmptyIntIterator THE_INSTANCE = new EmptyIntIterator();

    /**
     * Get the singular instance of this class
     * @return the singular instance
     */

    public static EmptyIntIterator getInstance() {
        return THE_INSTANCE;
    }

    private EmptyIntIterator() {}


    /**
     * Test whether there are any more integers in the sequence
     *
     * @return true if there are more integers to come
     */

    public boolean hasNext() {
        return false;
    }

    /**
     * Return the next integer in the sequence. The result is undefined unless hasNext() has been called
     * and has returned true.
     *
     * @return the next integer in the sequence
     */

    public int next() {
        return 0;
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

