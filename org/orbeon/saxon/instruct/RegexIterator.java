package org.orbeon.saxon.instruct;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ArrayIterator;
import org.orbeon.saxon.value.StringValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Class RegexIterator - provides an iterator over matched and unmatched substrings
*/

public class RegexIterator implements SequenceIterator {

    private String theString;
    private Pattern pattern;
    private Matcher matcher;
    private String current;
    private String next;
    private int position = 0;
    private int prevEnd = 0;
    private boolean currentMatches = false;
    private boolean nextMatches = false;
    private boolean hasNext = false;
    private String[] currentGroups;
    private String[] nextGroups;

    // The contract for SequenceIterator demands that hasNext() doesn't change the state of the iterator.
    // Unfortunately, the underlying Matcher object doesn't support this well: we can't tell whether there
    // is another match except by doing matcher.find(), and after doing matcher.find(), the captured groups
    // of the previous match are no longer available. So every time a match is found, we create a copy
    // of all the captured groups, just in case they are requested.

    // TODO: no longer need this lookahead, which means the class can be significantly simplified

    /**
    * Construct a RegexIterator. Note that the underlying matcher.find() method is called once
    * to obtain each matching substring. But the iterator also returns non-matching substrings
    * if these appear between the matching substrings.
    * @param string the string to be analysed
    * @param pattern the regular expression
    */

    public RegexIterator (String string, Pattern pattern) {
        theString = string;
        this.pattern = pattern;
        matcher = pattern.matcher(string);
        nextMatches = true;
        prevEnd = 0;
        advance();
    }

    private void advance() {
        if (nextMatches) {
            // we've returned a match, so find the next one
            if (matcher.find()) {
                hasNext = true;
                int start = matcher.start();
                int end = matcher.end();
                if (prevEnd == start) {
                    // there's no intervening non-matching string to return
                    nextMatches = true;
                    next = theString.substring(start, end);
                    int g = matcher.groupCount();
                    nextGroups = new String[g+1];
                    for (int i=0; i<=g; i++) {
                        nextGroups[i] = matcher.group(i);
                    }
                    prevEnd = end;
                } else {
                    // return the non-matching substring first
                    nextMatches = false;
                    next = theString.substring(prevEnd, start);
                }
            } else {
                // there are no more regex matches, we must return the final non-matching text if any
                if (prevEnd < theString.length()) {
                    nextMatches = false;
                    next = theString.substring(prevEnd);
                    hasNext = true;
                } else {
                    hasNext = false;
                }
                prevEnd = -1;
            }
        } else {
            // we've returned a non-match, so now return the match that follows it, if there is one
            if (prevEnd >= 0) {
                nextMatches = true;
                int start = matcher.start();
                int end = matcher.end();
                next = theString.substring(start, end);
                int g = matcher.groupCount();
                nextGroups = new String[g+1];
                for (int i=0; i<=g; i++) {
                    nextGroups[i] = matcher.group(i);
                }
                prevEnd = end;
            } else {
                hasNext = false;
            }
        }
    }

    /**
    * Get the next item in the sequence
    * @return the next item in the sequence
    */

    public Item next() {
        if (!hasNext) {
            return null;
        }
        current = next;
        currentMatches = nextMatches;
        currentGroups = nextGroups;
        advance();
        position++;
        return new StringValue(current);
    }

    /**
    * Get the current item in the sequence
    * @return the item most recently returned by next()
    */

    public Item current() {
        return new StringValue(current);
    }

    /**
    * Get the position of the current item in the sequence
    * @return the position of the item most recently returned by next(), starting at 1
    */

    public int position() {
        return position;
    }

    /**
    * Get another iterator over the same items
    * @return a new iterator, positioned before the first item
    */

    public SequenceIterator getAnother() {
        return new RegexIterator(theString, pattern);
    }

    /**
    * Determine whether the current item is a matching item or a non-matching item
    * @return true if the current item (the one most recently returned by next()) is
    * an item that matches the regular expression, or false if it is an item that
    * does not match
    */

    public boolean isMatching() {
        return currentMatches;
    }

    /**
    * Get a substring that matches a parenthesised group within the regular expression
    * @param number    the number of the group to be obtained
    * @return the substring of the current item that matches the n'th parenthesized group
    * within the regular expression
    */

    public String getRegexGroup(int number) {
        if (!currentMatches) return null;
        if (number<0) return "";
        if (number>=currentGroups.length) return "";
        String s = currentGroups[number];
        if (s==null) return "";
        return s;
    }

    /**
     * Get a sequence containing all the regex groups (except group 0, because we want to use indexing from 1)
     */

    public SequenceIterator getRegexGroupIterator() {
        StringValue[] groups = new StringValue[currentGroups.length - 1];
        for (int i=0; i<groups.length; i++) {
            groups[i] = new StringValue(currentGroups[i + 1]);
        }
        return new ArrayIterator(groups);
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
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
