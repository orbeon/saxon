package org.orbeon.saxon.dotnet;

import cli.System.IO.SeekOrigin;
import cli.System.IO.Stream;

import java.io.IOException;
import java.io.InputStream;

/**
 * A Java InputStream implemented as a wrapper around a .NET stream
 */
public class DotNetInputStream extends InputStream {

    Stream stream;
    long currentOffset;
    long markedOffset = 0;

    public DotNetInputStream(Stream stream) {
        this.stream = stream;
    }

    /**
     * Get the underlying .NET Stream object
     */

    public Stream getUnderlyingStream() {
        return stream;
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     * <p/>
     * <p> A subclass must provide an implementation of this method.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     *         stream is reached.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public int read() throws IOException {
        int i = stream.ReadByte();
        if (i != -1) {
            currentOffset++;
            return i;
        } else {
            return -1;
        }
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     * <p/>
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     * <p/>
     * <p> If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     * <p/>
     * <p> If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
     * thrown.
     * <p/>
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     * <p/>
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     * <p/>
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     * <p/>
     * <p> If the first byte cannot be read for any reason other than end of
     * file, then an <code>IOException</code> is thrown. In particular, an
     * <code>IOException</code> is thrown if the input stream has been closed.
     * <p/>
     * <p> The <code>read(b,</code> <code>off,</code> <code>len)</code> method
     * for class <code>InputStream</code> simply calls the method
     * <code>read()</code> repeatedly. If the first such call results in an
     * <code>IOException</code>, that exception is returned from the call to
     * the <code>read(b,</code> <code>off,</code> <code>len)</code> method.  If
     * any subsequent call to <code>read()</code> results in a
     * <code>IOException</code>, the exception is caught and treated as if it
     * were end of file; the bytes read up to that point are stored into
     * <code>b</code> and the number of bytes read before the exception
     * occurred is returned.  Subclasses are encouraged to provide a more
     * efficient implementation of this method.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in array <code>b</code>
     *            at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of
     *         the stream has been reached.
     * @throws java.io.IOException  if an I/O error occurs.
     * @throws NullPointerException if <code>b</code> is <code>null</code>.
     * @see java.io.InputStream#read()
     */
    public int read(byte b[], int off, int len) throws IOException {
        int i = stream.Read(b, off, len);
        if (i > 0) {
            currentOffset += i;
            return i;
        } else {
            // EOF returns 0 in .NET, -1 in Java
            return -1;
        }
    }

    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods. Whether or not <code>mark</code> and
     * <code>reset</code> are supported is an invariant property of a
     * particular input stream instance.
     *
     * @return <code>true</code> if this stream instance supports the mark
     *         and reset methods; <code>false</code> otherwise.
     * @see java.io.InputStream#mark(int)
     * @see java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return stream.get_CanSeek();
    }

    /**
     * Marks the current position in this input stream. A subsequent call to
     * the <code>reset</code> method repositions this stream at the last marked
     * position so that subsequent reads re-read the same bytes.
     * <p/>
     * <p> The <code>readlimit</code> arguments tells this input stream to
     * allow that many bytes to be read before the mark position gets
     * invalidated.
     * <p/>
     * <p> The general contract of <code>mark</code> is that, if the method
     * <code>markSupported</code> returns <code>true</code>, the stream somehow
     * remembers all the bytes read after the call to <code>mark</code> and
     * stands ready to supply those same bytes again if and whenever the method
     * <code>reset</code> is called.  However, the stream is not required to
     * remember any data at all if more than <code>readlimit</code> bytes are
     * read from the stream before <code>reset</code> is called.
     * <p/>
     * <p> The <code>mark</code> method of <code>InputStream</code> does
     * nothing.
     *
     * @param readlimit the maximum limit of bytes that can be read before
     *                  the mark position becomes invalid.
     * @see java.io.InputStream#reset()
     */
    public synchronized void mark(int readlimit) {
        markedOffset = currentOffset;
    }

    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     * <p/>
     * <p> The general contract of <code>reset</code> is:
     * <p/>
     * <p><ul>
     * <p/>
     * <li> If the method <code>markSupported</code> returns
     * <code>true</code>, then:
     * <p/>
     * <ul><li> If the method <code>mark</code> has not been called since
     * the stream was created, or the number of bytes read from the stream
     * since <code>mark</code> was last called is larger than the argument
     * to <code>mark</code> at that last call, then an
     * <code>IOException</code> might be thrown.
     * <p/>
     * <li> If such an <code>IOException</code> is not thrown, then the
     * stream is reset to a state such that all the bytes read since the
     * most recent call to <code>mark</code> (or since the start of the
     * file, if <code>mark</code> has not been called) will be resupplied
     * to subsequent callers of the <code>read</code> method, followed by
     * any bytes that otherwise would have been the next input data as of
     * the time of the call to <code>reset</code>. </ul>
     * <p/>
     * <li> If the method <code>markSupported</code> returns
     * <code>false</code>, then:
     * <p/>
     * <ul><li> The call to <code>reset</code> may throw an
     * <code>IOException</code>.
     * <p/>
     * <li> If an <code>IOException</code> is not thrown, then the stream
     * is reset to a fixed state that depends on the particular type of the
     * input stream and how it was created. The bytes that will be supplied
     * to subsequent callers of the <code>read</code> method depend on the
     * particular type of the input stream. </ul></ul>
     * <p/>
     * <p>The method <code>reset</code> for class <code>InputStream</code>
     * does nothing except throw an <code>IOException</code>.
     *
     * @throws java.io.IOException if this stream has not been marked or if the
     *                             mark has been invalidated.
     * @see java.io.InputStream#mark(int)
     * @see java.io.IOException
     */
    public synchronized void reset() throws IOException {
        currentOffset = markedOffset;
        stream.Seek(markedOffset, SeekOrigin.wrap(SeekOrigin.Begin));
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The general contract of <code>close</code>
     * is that it closes the output stream. A closed stream cannot perform
     * output operations and cannot be reopened.
     * <p/>
     * The <code>close</code> method of <code>OutputStream</code> does nothing.
     *
     * @throws java.io.IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        stream.Close();
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
