package org.orbeon.saxon.functions;

import org.orbeon.saxon.Err;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.BooleanValue;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;


public class UnparsedText extends SystemFunction implements XSLTFunction {

    // TODO: Add some kind of uri resolver mechanism

    // TODO: There is now a requirement that the results should be stable

    // TODO: Consider supporting a query parameter ?substitute-character=xFFDE

    String expressionBaseURI = null;

    public static final int UNPARSED_TEXT = 0;
    public static final int UNPARSED_TEXT_AVAILABLE = 1;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
        }
    }


    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     */

    public Expression preEvaluate(StaticContext env) {
        return this;
        // in principle we could pre-evaluate any call of unparsed-text() with
        // constant arguments. But we don't, because the file contents might
        // change before the stylesheet executes.
    }


    /**
     * evaluateItem() handles evaluation of the function:
     * it returns a String
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue result;
        try {
            StringValue hrefVal = (StringValue)argument[0].evaluateItem(context);
            if (hrefVal == null) {
                return null;
            }
            String href = hrefVal.getStringValue();

            String encoding = null;
            if (getNumberOfArguments() == 2) {
                encoding = argument[1].evaluateItem(context).getStringValue();
            }

            result = new StringValue(readFile(href, expressionBaseURI, encoding,
                    context.getConfiguration().getNameChecker()));
        } catch (XPathException err) {
            if (operation == UNPARSED_TEXT_AVAILABLE) {
                return BooleanValue.FALSE;
            } else {
                throw err;
            }
        }
        if (operation == UNPARSED_TEXT_AVAILABLE) {
            return BooleanValue.TRUE;
        } else {
            return result;
        }
    }

    /**
     * Supporting routine to load one external file given a URI (href) and a baseURI
     */

    private CharSequence readFile(String href, String baseURI, String encoding, NameChecker checker)
            throws XPathException {

        // Resolve relative URI

        URL absoluteURL;
        if (baseURI == null) {    // no base URI available
            try {
                // the href might be an absolute URL
                absoluteURL = new URL(href);
            } catch (MalformedURLException err) {
                // it isn't
                DynamicError e = new DynamicError("Cannot resolve absolute URI", err);
                e.setErrorCode("XTDE1170");
                throw e;
            }
        } else {
            try {
                absoluteURL = new URL(new URL(baseURI), href);
            } catch (MalformedURLException err) {
                DynamicError e = new DynamicError("Cannot resolve relative URI", err);
                e.setErrorCode("XTDE1170");
                throw e;
            }
        }
        try {
            InputStream is;
            if (encoding != null) {
                is = absoluteURL.openStream();
            } else {
                URLConnection connection = absoluteURL.openConnection();
                connection.connect();
                is = connection.getInputStream();

                try {

                    if (!is.markSupported()) {
                        is = new BufferedInputStream(is);
                    }

                    // Get any external (HTTP) encoding label.
                    String contentType;

                    // The file:// URL scheme gives no useful information...
                    if (!"file".equals(connection.getURL().getProtocol())) {

                        // Use the contentType from the HTTP header if available
                        contentType = connection.getContentType();

                        if (contentType != null) {
                            int pos = contentType.indexOf("charset");
                            if (pos>=0) {
                                pos = contentType.indexOf('=', pos + 7);
                                if (pos>=0) {
                                    contentType = contentType.substring(pos + 1);
                                }
                                if ((pos = contentType.indexOf(';')) > 0) {
                                    contentType = contentType.substring(0, pos);
                                }

                                // attributes can have comment fields (RFC 822)
                                if ((pos = contentType.indexOf('(')) > 0) {
                                    contentType = contentType.substring(0, pos);
                                }
                                // ... and values may be quoted
                                if ((pos = contentType.indexOf('"')) > 0) {
                                    contentType = contentType.substring(pos + 1,
                                            contentType.indexOf('"', pos + 2));
                                }
                                encoding = contentType.trim();
                            }
                        }
                    }

                    if (encoding == null) {
                        // Try to detect the encoding from the start of the content
                        is.mark(100);
                        byte[] start = new byte[100];
                        int read = is.read(start, 0, 100);
                        is.reset();
                        encoding = inferEncoding(start, read);
                    }

                } catch (IOException e) {
                    encoding = "UTF-8";
                }

            }

            // The following appears to be necessary to ensure that encoding errors are not recovered.
            Charset charset = Charset.forName(encoding);
            CharsetDecoder decoder = charset.newDecoder();
            decoder = decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder = decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            Reader reader = new BufferedReader(new InputStreamReader(is, decoder));
            
            FastStringBuffer sb = new FastStringBuffer(2048);
            char[] buffer = new char[2048];
            boolean first = true;
            int actual;
            int line = 1;
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
                    if (XMLChar.isHighSurrogate(ch32)) {
                        if (c==actual) {
                            actual = reader.read(buffer, 0, 2048);
                            c = 0;
                        }
                        char low = buffer[c++];
                        ch32 = XMLChar.supplemental((char)ch32, low);
                    }
                    if (!checker.isValidChar(ch32)) {
                        DynamicError err = new DynamicError(
                                "The unparsed-text file contains a character illegal in XML (line=" +
                        line + " column=" + column + " value=hex " + Integer.toHexString(ch32) + ')');
                        err.setErrorCode("XTDE1190");
                        throw err;
                    }
                }
                if (first) {
                    first = false;
                    if (buffer[0]=='\ufeff') {
                        // don't include the BOM in the result
                        sb.append(buffer, 1, actual-1);
                    } else {
                        sb.append(buffer, 0, actual);
                    }
                } else {
                    sb.append(buffer, 0, actual);
                }
            }
            reader.close();
            return sb.condense();
        } catch (java.io.UnsupportedEncodingException encErr) {
            DynamicError e = new DynamicError("Unknown encoding " + Err.wrap(encoding), encErr);
            e.setErrorCode("XTDE1190");
            throw e;
        } catch (java.io.IOException ioErr) {
//            System.err.println("ProxyHost: " + System.getProperty("http.proxyHost"));
//            System.err.println("ProxyPort: " + System.getProperty("http.proxyPort"));
            String message = "Failed to read input file";
            if (!ioErr.getMessage().equals(absoluteURL.toString())) {
                message += ' ' + absoluteURL.toString();
            }
            message += " (" + ioErr.getClass().getName() + ')';
            DynamicError e = new DynamicError(message, ioErr);
            String errorCode;
            if (ioErr instanceof java.nio.charset.MalformedInputException) {
                errorCode = "XTDE1200";
            } else if (ioErr instanceof java.nio.charset.CharacterCodingException) {
                errorCode = "XTDE1200";
            } else if (ioErr instanceof java.nio.charset.UnmappableCharacterException) {
                errorCode = "XTDE1190";
            } else {
               errorCode = "XTDE1170";
            }
            e.setErrorCode(errorCode);
            e.setLocator(this);
            throw e;
        }
    }

    private String inferEncoding(byte[] start, int read) {
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
        if (read >= 4) {
            if (ch(start[0]) == '<' && ch(start[1]) == '?' &&
                    ch(start[2]) == 'x' && ch(start[3]) == 'm' && ch(start[4]) == 'l') {
                FastStringBuffer sb = new FastStringBuffer(read);
                for (int b = 0; b < read; b++) {
                    sb.append((char)start[b]);
                }
                String p = sb.toString();
                int v = p.indexOf("encoding");
                if (v >= 0) {
                    v += 8;
                    while (v < p.length() && " \n\r\t=\"'".indexOf(p.charAt(v)) >= 0) {
                        v++;
                    }
                    sb.setLength(0);
                    while (v < p.length() && p.charAt(v) != '"' && p.charAt(v) != '\'') {
                        sb.append(p.charAt(v++));
                    }
                    return sb.toString();
                }
            }
        } else if (read > 0 && start[0] == 0 && start[2] == 0 && start[4] == 0 && start[6] == 0) {
            return "UTF-16";
        } else if (read > 1 && start[1] == 0 && start[3] == 0 && start[5] == 0 && start[7] == 0) {
            return "UTF-16LE";
        }
        // If all else fails, assume UTF-8
        return "UTF-8";
    }

    private int ch(byte b) {
        return ((int)b) & 0xff;
    }

// diagnostic method to output the octets of a file

    public static void main(String[] args) throws Exception {
        FastStringBuffer sb1 = new FastStringBuffer(100);
        FastStringBuffer sb2 = new FastStringBuffer(100);
        File file = new File(args[0]);
        InputStream is = new FileInputStream(file);
        while (true) {
            int b = is.read();
            if (b<0) {
                System.out.println(sb1.toString());
                System.out.println(sb2.toString()); break;
            }
            sb1.append(Integer.toHexString(b)+" ");
            sb2.append((char)b + " ");
            if (sb1.length() > 80) {
                System.out.println(sb1.toString());
                System.out.println(sb2.toString());
                sb1 = new FastStringBuffer(100);
                sb2 = new FastStringBuffer(100);
            }
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
// The Initial Developer of the Original Code is Michael H. Kay. The detectEncoding() method includes
// code fragments taken from the AElfred XML Parser developed by David Megginson.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
