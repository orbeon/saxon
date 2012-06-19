package org.orbeon.saxon.om;

/**
 * A value that exists in memory and that can be directly addressed
 */
public interface GroundedValue extends ValueRepresentation {

    /**
     * Get the n'th item in the value, counting from 0
     * @param n the index of the required item, with 0 representing the first item in the sequence
     * @return the n'th item if it exists, or null otherwise
     */

    public Item itemAt(int n);

    /**
     * Get a subsequence of the value
     * @param start the index of the first item to be included in the result, counting from zero.
     * A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     * sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     * get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     * is returned. If the value goes off the end of the sequence, the result returns items up to the end
     * of the sequence
     * @return the required subsequence. If min is
     */

    public GroundedValue subsequence(int start, int length);
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

