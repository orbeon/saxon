package net.sf.saxon.om;

import net.sf.saxon.tinytree.CharSlice;

import java.io.Writer;

/**
 * A simple implementation of a class similar to StringBuffer. Unlike
 * StringBuffer it is not synchronized. It also offers the capability
 * to remove unused space. (This class could possibly be replaced by
 * StringBuilder in JDK 1.5, but using our own class gives more control.)
 */

public final class FastStringBuffer implements CharSequence {

    private char[] array;
    private int used = 0;

    public FastStringBuffer(int initialSize) {
        array = new char[initialSize];
    }

    /**
     * Append the contents of a String to the buffer
     * @param s the String to be appended
     */

    public void append(String s) {
        int len = s.length();
        ensureCapacity(len);
        s.getChars(0, len, array, used);
        used += len;
    }

    /**
     * Append the contents of a CharSlice to the buffer
     * @param s the String to be appended
     */

    public void append(CharSlice s) {
        int len = s.length();
        ensureCapacity(len);
        s.copyTo(array, used);
        used += len;
    }

    /**
     * Append the contents of a FastStringBuffer to the buffer
     * @param s the FastStringBuffer to be appended
     */

    public void append(FastStringBuffer s) {
        int len = s.length();
        ensureCapacity(len);
        s.getChars(0, len, array, used);
        used += len;
    }

    /**
     * Append the contents of a StringBuffer to the buffer
     * @param s the StringBuffer to be appended
     */

    public void append(StringBuffer s) {
        int len = s.length();
        ensureCapacity(len);
        s.getChars(0, len, array, used);
        used += len;
    }

    /**
     * Append the contents of a general CharSequence to the buffer
     * @param s the CharSequence to be appended
     */

    public void append(CharSequence s) {
        // Although we provide variants of this method for different subtypes, Java decides which to used based
        // on the static type of the operand. We want to use the right method based on the dynamic type, to avoid
        // creating objects and copying strings unnecessarily. So we do a dynamic dispatch.
        final int len = s.length();
        ensureCapacity(len);
        if (s instanceof CharSlice) {
            ((CharSlice)s).copyTo(array, used);
        } else if (s instanceof String) {
            ((String)s).getChars(0, len, array, used);
        } else if (s instanceof FastStringBuffer) {
            ((FastStringBuffer)s).getChars(0, len, array, used);
        } else {
            s.toString().getChars(0, len, array, used);
        }
        used += len;
    }

    /**
     * Append the contents of a character array to the buffer
     * @param srcArray the array whose contents are to be added
     * @param start the offset of the first character in the array to be copied
     * @param length the number of characters to be copied
     */

    public void append(char[] srcArray, int start, int length) {
        ensureCapacity(length);
        System.arraycopy(srcArray, start, array, used, length);
        used += length;
    }

    /**
     * Append a character to the buffer
     * @param ch the character to be added
     */

    public void append(char ch) {
        ensureCapacity(1);
        array[used++] = ch;
    }

    /**
     * Returns the length of this character sequence.  The length is the number
     * of 16-bit <code>char</code>s in the sequence.</p>
     *
     * @return the number of <code>char</code>s in this sequence
     */
    public int length() {
        return used;
    }

    /**
     * Returns the <code>char</code> value at the specified index.  An index ranges from zero
     * to <tt>length() - 1</tt>.  The first <code>char</code> value of the sequence is at
     * index zero, the next at index one, and so on, as for array
     * indexing. </p>
     * <p/>
     * <p>If the <code>char</code> value specified by the index is a
     * <a href="Character.html#unicode">surrogate</a>, the surrogate
     * value is returned.
     *
     * @param index the index of the <code>char</code> value to be returned
     * @return the specified <code>char</code> value
     * @throws IndexOutOfBoundsException if the <tt>index</tt> argument is negative or not less than
     *                                   <tt>length()</tt>
     */
    public char charAt(int index) {
        if (index >= used) {
            throw new IndexOutOfBoundsException("" + index);
        }
        return array[index];
    }

    /**
     * Returns a new <code>CharSequence</code> that is a subsequence of this sequence.
     * The subsequence starts with the <code>char</code> value at the specified index and
     * ends with the <code>char</code> value at index <tt>end - 1</tt>.  The length
     * (in <code>char</code>s) of the
     * returned sequence is <tt>end - start</tt>, so if <tt>start == end</tt>
     * then an empty sequence is returned. </p>
     *
     * @param start the start index, inclusive
     * @param end   the end index, exclusive
     * @return the specified subsequence
     * @throws IndexOutOfBoundsException if <tt>start</tt> or <tt>end</tt> are negative,
     *                                   if <tt>end</tt> is greater than <tt>length()</tt>,
     *                                   or if <tt>start</tt> is greater than <tt>end</tt>
     */
    public CharSequence subSequence(int start, int end) {
        return new CharSlice(array, start, end - start);
    }

    /**
     * Copies characters from this FastStringBuffer into the destination character
     * array.
     * <p>
     * The first character to be copied is at index <code>srcBegin</code>;
     * the last character to be copied is at index <code>srcEnd-1</code>
     * (thus the total number of characters to be copied is
     * <code>srcEnd-srcBegin</code>). The characters are copied into the
     * subarray of <code>dst</code> starting at index <code>dstBegin</code>
     * and ending at index:
     * <p><blockquote><pre>
     *     dstbegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @param      srcBegin   index of the first character in the string
     *                        to copy.
     * @param      srcEnd     index after the last character in the string
     *                        to copy.
     * @param      dst        the destination array.
     * @param      dstBegin   the start offset in the destination array.
     * @exception IndexOutOfBoundsException If any of the following
     *            is true:
     *            <ul><li><code>srcBegin</code> is negative.
     *            <li><code>srcBegin</code> is greater than <code>srcEnd</code>
     *            <li><code>srcEnd</code> is greater than the length of this
     *                string
     *            <li><code>dstBegin</code> is negative
     *            <li><code>dstBegin+(srcEnd-srcBegin)</code> is larger than
     *                <code>dst.length</code></ul>
     */
    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > used) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        System.arraycopy(array, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    /**
     * Convert contents of the FastStringBuffer to a string
     */

    public String toString() {
        condense();
        return new String(array, 0, used);
    }

    /**
     * Set the character at a particular offset
     * @param index the index of the character to be set
     * @param ch the new character to overwrite the existing character at that location
     * @throws IndexOutOfBoundsException if int<0 or int>=length()
     */

    public void setCharAt(int index, char ch) {
        if (index<0 || index>used) {
            throw new IndexOutOfBoundsException(""+index);
        }
        array[index] = ch;
    }

    /**
     * Set the length. If this exceeds the current length, this method is a no-op.
     * If this is less than the current length, characters beyond the specified point
     * are deleted.
     * @param length the new length
     */

    public void setLength(int length) {
        if (length < 0 || length > used) {
            return;
        }
        used = length;
    }

    /**
     * Expand the character array if necessary to ensure capacity for appended data
     */

    public void ensureCapacity(int extra) {
        if (used + extra > array.length) {
            int newlen = array.length * 2;
            if (newlen < used + extra) {
                newlen = used + extra*2;
            }
            char[] array2 = new char[newlen];
            System.arraycopy(array, 0, array2, 0, used);
            array = array2;
        }
    }

    /**
     * Remove surplus space from the array
     */

    public CharSequence condense() {
        if (array.length - used > 256 || array.length > used * 2) {
            char[] array2 = new char[used];
            System.arraycopy(array, 0, array2, 0, used);
            array = array2;
        }
        return this;
    }

    /**
     * Write the value to a writer
     */

    public void write(Writer writer) throws java.io.IOException {
        writer.write(array, 0, used);
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
// Contributor(s): none
//

