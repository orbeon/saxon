package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.value.StringValue;

import java.util.StringTokenizer;

/**
* StringTokenIterator: breaks a string up into tokens,
* and returns the tokens as a sequence of strings.
*/

public class StringTokenIterator implements SequenceIterator {

    private String theString;
    private String delimiters;  // null implies use whitespace as delimiter
    private StringTokenizer tokenizer;
    private String current;
    private int position = 0;

    /**
    * Construct a StringTokenIterator that will break the supplied
    * string into tokens at whitespace boundaries
    */

    public StringTokenIterator (String string) {
        theString = string;
        delimiters = null;
        tokenizer = new StringTokenizer(string);
    }

    /**
    * Construct a StringTokenIterator that will break the supplied
    * string into tokens at any of the delimiter characters included in the
    * delimiter string.
    */

    public StringTokenIterator (String string, String delimiters) {
        theString = string;
        this.delimiters = delimiters;
        tokenizer = new StringTokenizer(string, delimiters);
    }

    public Item next() {
        if (tokenizer.hasMoreElements()) {
            current = (String)tokenizer.nextElement();
            position++;
            return new StringValue(current);
        } else {
            current = null;
            position = -1;
            return null;
        }
    }

    public Item current() {
        return (current == null ? null : new StringValue(current));
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() {
        if (delimiters==null) {
            return new StringTokenIterator(theString);
        } else {
            return new StringTokenIterator(theString, delimiters);
        }
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
