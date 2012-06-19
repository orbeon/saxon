package org.orbeon.saxon.dotnet;
import cli.System.Collections.IEnumerator;
import cli.System.Text.RegularExpressions.Group;
import cli.System.Text.RegularExpressions.GroupCollection;
import cli.System.Text.RegularExpressions.Match;
import cli.System.Text.RegularExpressions.Regex;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.regex.RegexIterator;
import org.orbeon.saxon.value.StringValue;

/**
 * Class JRegexIterator - provides an iterator over matched and unmatched substrings.
 * This implementation of RegexIterator uses the JDK regular expression engine.
*/

public class DotNetRegexIterator implements RegexIterator {

    private String theString;   // the input string being matched
    private Regex pattern;    // the regex against which the string is matched
    private IEnumerator matcher;    // the Matcher object that does the matching, and holds the state
    private Match match;        // the current match
    private String current;     // the string most recently returned by the iterator
    private String next;        // if the last string was a matching string, null; otherwise the next substring
                                //        matched by the regex
    private int position = 0;   // the value of XPath position()
    private int prevEnd = 0;    // the position in the input string of the end of the last match or non-match

    /**
    * Construct a RegexIterator. Note that the underlying matcher.find() method is called once
    * to obtain each matching substring. But the iterator also returns non-matching substrings
    * if these appear between the matching substrings.
    * @param string the string to be analysed
    * @param pattern the regular expression
    */

    public DotNetRegexIterator (String string, Regex pattern) {
        theString = string;
        this.pattern = pattern;
        matcher = pattern.Matches(string).GetEnumerator();
        next = null;
    }

    /**
    * Get the next item in the sequence
    * @return the next item in the sequence
    */

    public Item next() {
        if (next == null && prevEnd >= 0) {
            // we've returned a match (or we're at the start), so find the next match
            if (matcher.MoveNext()) {
                match = (Match)matcher.get_Current();
                int start = match.get_Index();
                int end = match.get_Index() + match.get_Length();
                if (prevEnd == start) {
                    // there's no intervening non-matching string to return
                    next = null;
                    current = theString.substring(start, end);
                    prevEnd = end;
                } else {
                    // return the non-matching substring first
                    current = theString.substring(prevEnd, start);
                    next = theString.substring(start, end);
                }
            } else {
                // there are no more regex matches, we must return the final non-matching text if any
                if (prevEnd < theString.length()) {
                    current = theString.substring(prevEnd);
                    next = null;
                } else {
                    // this really is the end...
                    current = null;
                    position = -1;
                    prevEnd = -1;
                    return null;
                }
                prevEnd = -1;
            }
        } else {
            // we've returned a non-match, so now return the match that follows it, if there is one
            if (prevEnd >= 0) {
                current = next;
                next = null;
                prevEnd = match.get_Index() + match.get_Length();
            } else {
                current = null;
                position = -1;
                return null;
            }
        }
        position++;
        return StringValue.makeStringValue(current);
    }

    /**
    * Get the current item in the sequence
    * @return the item most recently returned by next()
    */

    public Item current() {
        return StringValue.makeStringValue(current);
    }

    /**
    * Get the position of the current item in the sequence
    * @return the position of the item most recently returned by next(), starting at 1
    */

    public int position() {
        return position;
    }

    public void close() {
    }

    /**
    * Get another iterator over the same items
    * @return a new iterator, positioned before the first item
    */

    public SequenceIterator getAnother() {
        return new DotNetRegexIterator(theString, pattern);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }

    /**
    * Determine whether the current item is a matching item or a non-matching item
    * @return true if the current item (the one most recently returned by next()) is
    * an item that matches the regular expression, or false if it is an item that
    * does not match
    */

    public boolean isMatching() {
        return next == null && prevEnd >= 0;
    }

    /**
    * Get a substring that matches a parenthesised group within the regular expression
    * @param number    the number of the group to be obtained
    * @return the substring of the current item that matches the n'th parenthesized group
    * within the regular expression
    */

    public String getRegexGroup(int number) {
        if (!isMatching()) return null;
        GroupCollection groups = match.get_Groups();
        if (number > groups.get_Count() || number < 0) return "";
        String s = groups.get_Item(number).get_Value();
        if (s==null) return "";
        return s;
    }

    /**
     * Get a sequence containing all the regex groups (except group 0, because we want to use indexing from 1).
     * This is used by the saxon:analyze-string() higher-order extension function.
     */

    public SequenceIterator getRegexGroupIterator() {
        //System.err.println("getRegexGroupIterator");
        if (!isMatching()) {
            //System.err.println("no match");
            return null;
        }
        GroupCollection groups = match.get_Groups();
        int c = groups.get_Count();
        //System.err.println("groups: " + c);
        if (c == 0) {
            return EmptyIterator.getInstance();
        } else {
            StringValue[] groupArray = new StringValue[c-1];
            IEnumerator e = groups.GetEnumerator();
            int i=0;
            // we're not interested in group 0
            e.MoveNext();
            e.get_Current();
            while (e.MoveNext()) {
                Group g = (Group)e.get_Current();
                //System.err.println("group: " + i + " " + g.get_Value());
                groupArray[i++] = StringValue.makeStringValue(g.get_Value());
            }
            return new ArrayIterator(groupArray);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
//
