package org.orbeon.saxon.query;

import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.charcode.UTF16;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.transform.stream.StreamSource;
import java.io.*;

/**
 * This class contains static methods used to read a query as a byte stream, infer the encoding if
 * necessary, and return the text of the query as a string; also methods to import functions and variables
 * from one module into another, and check their consistency.
 */
public class QueryReader {

    /**
     * The class is never instantiated
     */
    private QueryReader() {}

    /**
     * Read a query module given a StreamSource
     * @param ss the supplied StreamSource. This must contain a non-null systemID which defines the base
     * URI of the query module, and either an InputStream or a Reader containing the query text. In the
     * case of an InputStream the method attempts to infer the encoding; in the case of a Reader, this has
     * already been done, and the encoding specified within the query itself is ignored.
     * <p>The method reads from the InputStream or Reader contained in the StreamSource up to the end
     * of file unless a fatal error occurs. It does not close the InputStream or Reader; this is the caller's
     * responsibility.</p>
     * @param nameChecker this checks XML names against either the XML 1.0 or XML 1.1 rules
     * @return the text of the query
     */

    public static String readSourceQuery(StreamSource ss, NameChecker nameChecker) throws XPathException {
        CharSequence queryText;
        if (ss.getInputStream() != null) {
            InputStream is = ss.getInputStream();
            if (!is.markSupported()) {
                is = new BufferedInputStream(is);
            }
            String encoding = readEncoding(is);
            queryText = readInputStream(is, encoding, nameChecker);
        } else if (ss.getReader() != null) {
            queryText = readQueryFromReader(ss.getReader(), nameChecker);
        } else {
            throw new XPathException("Module URI Resolver must supply either an InputSource or a Reader");
        }
        return queryText.toString();
    }

    /**
     * Read an input stream non-destructively to determine the encoding from the Query Prolog
     * @param is the input stream: this must satisfy the precondition is.markSupported() = true.
     * @return the encoding to be used: defaults to UTF-8 if no encoding was specified explicitly
     * in the query prolog
     * @throws XPathException if the input stream cannot be read
     */

    public static String readEncoding(InputStream is) throws XPathException {
        try {
            if (!is.markSupported()) {
                throw new IllegalArgumentException("InputStream must have markSupported() = true");
            }
            is.mark(100);
            byte[] start = new byte[100];
            int read = is.read(start, 0, 100);
            if (read == -1) {
                throw new XPathException("Query source file is empty");
            }
            is.reset();
            return inferEncoding(start, read);
        } catch (IOException e) {
            throw new XPathException("Failed to read query source file", e);
        }
    }

    /**
     * Read a query from an InputStream. The method checks that all characters are valid XML
     * characters, and also performs normalization of line endings.
     * @param is the input stream
     * @param encoding the encoding, or null if the encoding is unknown
     * @param nameChecker the nameChecker to be used for checking characters
     * @return the content of the InputStream as a string
     */

    public static String readInputStream(InputStream is, String encoding, NameChecker nameChecker) throws XPathException {
        if (encoding == null) {
            if (!is.markSupported()) {
                is = new BufferedInputStream(is);
            }
            encoding = readEncoding(is);
        }
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, encoding));
            return readQueryFromReader(reader, nameChecker);
        } catch (UnsupportedEncodingException encErr) {
            throw new XPathException("Unknown encoding " + Err.wrap(encoding), encErr);
        }
    }

    /**
     * Read a query from a Reader. The method checks that all characters are valid XML
     * characters.
     * @param reader The Reader supplying the input
     * @param nameChecker the NameChecker to be used
     * @return the text of the query module, as a string
     * @throws XPathException if the file cannot be read or contains illegal characters
     */

    private static String readQueryFromReader(Reader reader, NameChecker nameChecker) throws XPathException {
        try {
            FastStringBuffer sb = new FastStringBuffer(2048);
            char[] buffer = new char[2048];
            boolean first = true;
            int actual;
            int line = 1;   // track line/column position for reporting bad characters
            int column = 1;
            while (true) {
                actual = reader.read(buffer, 0, 2048);
                if (actual < 0) {
                    break;
                }
                for (int c=0; c<actual;) {
                    int ch32 = buffer[c++];
                    if (ch32 == '\n') {
                        line++;
                        column = 0;
                    }
                    column++;
                    if (UTF16.isHighSurrogate(ch32)) {
                        char low = buffer[c++];
                        ch32 = UTF16.combinePair((char)ch32, low);
                    }
                    if (!nameChecker.isValidChar(ch32)) {
                        XPathException err = new XPathException("The query file contains a character illegal in XML " +
                                nameChecker.getXMLVersion() +
                                " (line=" + line +
                                " column=" + column +
                                " value=x" + Integer.toHexString(ch32) + ')');
                        err.setErrorCode("XPST0003");
                        err.setIsStaticError(true);
                        throw err;
                    }
                }
                if (first) {
                    first = false;
                    if (buffer[0]=='\ufeff') {
                        sb.append(buffer, 1, actual-1);
                    } else {
                        sb.append(buffer, 0, actual);
                    }
                } else {
                    sb.append(buffer, 0, actual);
                }
            }
            return sb.condense().toString();
        } catch (IOException ioErr) {
            throw new XPathException("Failed to read input file", ioErr);
        }
    }

    /**
     * Attempt to infer the encoding of a file by reading its byte order mark and if necessary
     * the encoding declaration in the query prolog
     * @param start the bytes appearing at the start of the file
     * @param read the number of bytes supplied
     * @return the inferred encoding
     * @throws XPathException
     */

    private static String inferEncoding(byte[] start, int read) throws XPathException {
        // Debugging code
//        StringBuffer sb = new StringBuffer(read*5);
//        for (int i=0; i<read; i++) sb.append(Integer.toHexString(start[i]&255) + ", ");
//        System.err.println(sb);
        // End of debugging code

        if (read >= 2) {
            if (ch(start[0]) == 0xFE && ch(start[1]) == 0xFF) {
                return "UTF-16";
            } else if (ch(start[0]) == 0xFF && ch(start[1]) == 0xFE) {
                return "UTF-16LE";
            }
        }
        if (read >= 3) {
            if (ch(start[0]) == 0xEF && ch(start[1]) == 0xBB && ch(start[2]) == 0xBF) {
                return "UTF-8";
            }
        }

        // Try to handle a UTF-16 file with no BOM
        if (read >= 8 && start[0] == 0 && start[2] == 0 && start[4] == 0 && start[6] == 0) {
            return "UTF-16";
        }
        if (read >= 8 && start[1] == 0 && start[3] == 0 && start[5] == 0 && start[7] == 0) {
            return "UTF-16LE";
        }

        // In all other cases, we assume an encoding that has ISO646 as a subset

        // Note, we don't care about syntax errors here: they'll be reported later. We just need to
        // establish the encoding.
        int i=0;
        String tok = readToken(start, i, read);
        if (Whitespace.trim(tok).equals("xquery")) {
            i += tok.length();
        } else {
            return "UTF-8";
        }
        tok = readToken(start, i, read);
        if (Whitespace.trim(tok).equals("version")) {
            i += tok.length();
        } else {
            return "UTF-8";
        }
        tok = readToken(start, i, read);
        if (tok == null) {
            return "UTF-8";
        }
        i += tok.length();
        tok = readToken(start, i, read);
        if (Whitespace.trim(tok).equals("encoding")) {
            i += tok.length();
        } else {
            return "UTF-8";
        }
        tok = Whitespace.trim(readToken(start, i, read));
        if (tok.startsWith("\"") && tok.endsWith("\"") && tok.length()>2) {
            return tok.substring(1, tok.length()-1);
        } else if (tok.startsWith("'") && tok.endsWith("'") && tok.length()>2) {
            return tok.substring(1, tok.length()-1);
        } else {
            throw new XPathException("Unrecognized encoding " + Err.wrap(tok) + " in query prolog");
        }

    }

    /**
     * Simple tokenizer for use when reading the encoding declaration in the query prolog. A token
     * is a sequence of characters delimited either by whitespace, or by single or double quotes; the
     * quotes if present are returned as part of the token.
     * @param in the character buffer
     * @param i offset where to start reading
     * @param len the length of buffer
     * @return the next token
     */

    private static String readToken(byte[] in, int i, int len) {
        int p = i;
        while (p<len && " \n\r\t".indexOf(ch(in[p])) >= 0) {
            p++;
        }
        if (ch(in[p])=='"') {
            p++;
            while (p<len && ch(in[p]) != '"') {
                p++;
            }
        } else if (ch(in[p])=='\'') {
            p++;
            while (p<len && ch(in[p]) != '\'') {
                p++;
            }
        } else {
            while (p<len && " \n\r\t".indexOf(ch(in[p])) < 0) {
                p++;
            }
        }
        if (p>=len) {
            return new String(in, i, len-i);
        }
        FastStringBuffer sb = new FastStringBuffer(p-i+1);
        for (int c=i; c<=p; c++) {
            sb.append((char)ch(in[c]));
        }
        return sb.toString();
    }

    /**
     * Convert a byte containing an ASCII character to that character
     * @param b the input byte
     * @return the ASCII character
     */

    private static int ch(byte b) {
        return ((int)b) & 0xff;
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
