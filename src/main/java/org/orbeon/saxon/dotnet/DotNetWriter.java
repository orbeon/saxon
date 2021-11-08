package org.orbeon.saxon.dotnet;

import cli.System.IO.TextWriter;

import java.io.IOException;
import java.io.Writer;

/**
 * Implement a Java Writer that wraps a supplied .NET TextWriter
 */
public class DotNetWriter extends Writer {

    private TextWriter textWriter;

    /**
     * Create a Java Writer that wraps a supplied .NET TextWriter
     */

    public DotNetWriter(TextWriter writer) {
        this.textWriter = writer;
    }

    /**
     * Close the stream, flushing it first.  Once a stream has been closed,
     * further write() or flush() invocations will cause an IOException to be
     * thrown.  Closing a previously-closed stream, however, has no effect.
     *
     * @throws java.io.IOException If an I/O error occurs
     */
    public void close() throws IOException {
        try {
            textWriter.Close();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Flush the stream.  If the stream has saved any characters from the
     * various write() methods in a buffer, write them immediately to their
     * intended destination.  Then, if that destination is another character or
     * byte stream, flush it.  Thus one flush() invocation will flush all the
     * buffers in a chain of Writers and OutputStreams.
     * <p/>
     * If the intended destination of this stream is an abstraction provided by
     * the underlying operating system, for example a file, then flushing the
     * stream guarantees only that bytes previously written to the stream are
     * passed to the operating system for writing; it does not guarantee that
     * they are actually written to a physical device such as a disk drive.
     *
     * @throws java.io.IOException If an I/O error occurs
     */
    public void flush() throws IOException {
        try {
            textWriter.Flush();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param cbuf Array of characters
     * @param off  Offset from which to start writing characters
     * @param len  Number of characters to write
     * @throws java.io.IOException If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws IOException {
        try {
            textWriter.Write(cbuf, off, len);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Write a single character.  The character to be written is contained in
     * the 16 low-order bits of the given integer value; the 16 high-order bits
     * are ignored.
     * <p/>
     * <p> Subclasses that intend to support efficient single-character output
     * should override this method.
     *
     * @param c int specifying a character to be written.
     * @throws java.io.IOException If an I/O error occurs
     */
    public void write(int c) throws IOException {
        try {
            textWriter.Write((char)c);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Write an array of characters.
     *
     * @param cbuf Array of characters to be written
     * @throws java.io.IOException If an I/O error occurs
     */
    public void write(char cbuf[]) throws IOException {
        try {
            textWriter.Write(cbuf);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Write a string.
     *
     * @param str String to be written
     * @throws java.io.IOException If an I/O error occurs
     */
    public void write(String str) throws IOException {
        try {
            textWriter.Write(str);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
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
// Contributor(s): none.
//
