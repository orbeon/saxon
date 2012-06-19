package org.orbeon.saxon.functions;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.charcode.UTF16;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.StringValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;


public class UnparsedText extends SystemFunction implements XSLTFunction {

    // TODO: There is now a requirement that the results should be stable

    // TODO: Consider supporting a query parameter ?substitute-character=xFFDE

    String expressionBaseURI = null;

    public static final int UNPARSED_TEXT = 0;
    public static final int UNPARSED_TEXT_AVAILABLE = 1;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }


    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
        // in principle we could pre-evaluate any call of unparsed-text() with
        // constant arguments. But we don't, because the file contents might
        // change before the stylesheet executes.
    }

    /**
     * This method handles evaluation of the function:
     * it returns a StringValue in the case of unparsed-text(), or a BooleanValue
     * in the case of unparsed-text-available()
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

            result = new StringValue(
                    readFile(href, expressionBaseURI, encoding, context));
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

    private CharSequence readFile(String href, String baseURI, String encoding, XPathContext context)
            throws XPathException {

        final Configuration config = context.getConfiguration();
        NameChecker checker = config.getNameChecker();

        // Use the URI machinery to validate and resolve the URIs

        Platform platform = Configuration.getPlatform();
        URI absoluteURI;
        try {
            absoluteURI = platform.makeAbsolute(href, baseURI);
        } catch (java.net.URISyntaxException err) {
            XPathException e = new XPathException(err.getReason() + ": " + err.getInput(), err);
            e.setErrorCode("XTDE1170");
            throw e;
        }

        if (absoluteURI.getFragment() != null) {
            XPathException e = new XPathException("URI for unparsed-text() must not contain a fragment identifier");
            e.setErrorCode("XTDE1170");
            throw e;
        }

        // The URL dereferencing classes throw all kinds of strange exceptions if given
        // ill-formed sequences of %hh escape characters. So we do a sanity check that the
        // escaping is well-formed according to UTF-8 rules

        EscapeURI.checkPercentEncoding(absoluteURI.toString());

        Reader reader = context.getController().getUnparsedTextURIResolver().resolve(absoluteURI, encoding, config);
        try {
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
                    if (UTF16.isHighSurrogate(ch32)) {
                        if (c==actual) {
                            actual = reader.read(buffer, 0, 2048);
                            c = 0;
                        }
                        char low = buffer[c++];
                        ch32 = UTF16.combinePair((char)ch32, low);
                    }
                    if (!checker.isValidChar(ch32)) {
                        XPathException err = new XPathException("The unparsed-text file contains a character illegal in XML (line=" +
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
            XPathException e = new XPathException("Unknown encoding " + Err.wrap(encoding), encErr);
            e.setErrorCode("XTDE1190");
            throw e;
        } catch (java.io.IOException ioErr) {
//            System.err.println("ProxyHost: " + System.getProperty("http.proxyHost"));
//            System.err.println("ProxyPort: " + System.getProperty("http.proxyPort"));
            String message = "Failed to read input file";
            if (!ioErr.getMessage().equals(absoluteURI.toString())) {
                message += ' ' + absoluteURI.toString();
            }
            message += " (" + ioErr.getClass().getName() + ')';
            XPathException e = new XPathException(message, ioErr);
            String errorCode;
            if (ioErr instanceof MalformedInputException) {
                errorCode = "XTDE1200";
            } else if (ioErr instanceof CharacterCodingException) {
                errorCode = "XTDE1200";
            } else if (ioErr instanceof UnmappableCharacterException) {
                errorCode = "XTDE1190";
            } else {
                errorCode = "XTDE1170";
            }
            e.setErrorCode(errorCode);
            e.setLocator(this);
            throw e;
        }
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
