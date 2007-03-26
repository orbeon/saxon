package org.orbeon.saxon.dotnet;

import cli.System.Collections.IEnumerator;
import cli.System.Text.RegularExpressions.Match;
import cli.System.Text.RegularExpressions.Regex;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.value.StringValue;

/**
 * A DotNetTokenIterator is an iterator over the strings that result from tokenizing
 * a string using a regular expression
*/

public class DotNetTokenIterator implements SequenceIterator {

    private String input;
    private Regex pattern;
    private IEnumerator matches;
    private CharSequence current;
    private int position = 0;
    private int prevEnd = 0;


    /**
    * Construct a JTokenIterator.
    */

    public DotNetTokenIterator (CharSequence input, Regex pattern) {
        this.input = input.toString();
        this.pattern = pattern;
        matches = pattern.Matches(this.input).GetEnumerator();
        prevEnd = 0;
    }

    public Item next() {
        if (prevEnd < 0) {
            current = null;
            position = -1;
            return null;
        }

        if (matches.MoveNext()) {
            Match match = (Match)matches.get_Current();
            current = input.subSequence(prevEnd, match.get_Index());
            prevEnd = match.get_Index() + match.get_Length();
        } else {
            current = input.subSequence(prevEnd, input.length());
            prevEnd = -1;
        }
        position++;
        return StringValue.makeStringValue(current);
    }

    public Item current() {
        return (current==null ? null : StringValue.makeStringValue(current));
    }

    public int position() {
        return position;
    }

    public SequenceIterator getAnother() {
        return new DotNetTokenIterator(input, pattern);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link GROUNDED}, {@link LAST_POSITION_FINDER},
     *         and {@link LOOKAHEAD}. It is always
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

