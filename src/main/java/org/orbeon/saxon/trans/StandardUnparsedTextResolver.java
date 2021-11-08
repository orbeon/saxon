package org.orbeon.saxon.trans;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.FastStringBuffer;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.zip.GZIPInputStream;

/**
 * Default implementation of the UnparsedTextURIResolver, used if no other implementation
 * is nominated to the Configuration.
 */

public class StandardUnparsedTextResolver implements UnparsedTextURIResolver {

    private boolean debug = false;

    /**
     * Set debugging on or off. In debugging mode, information is written to System.err
     * to trace the process of deducing an encoding.
     *
     * @param debug set to true to enable debugging
     */

    public void setDebugging(boolean debug) {
        this.debug = debug;
    }

    /**
     * Resolve the URI passed to the XSLT unparsed-text() function, after resolving
     * against the base URI.
     *
     * @param absoluteURI the absolute URI obtained by resolving the supplied
     *                    URI against the base URI
     * @param encoding    the encoding requested in the call of unparsed-text(), if any. Otherwise null.
     * @param config      The configuration. Provided in case the URI resolver
     *                    needs it.
     * @return a Reader, which Saxon will use to read the unparsed text. After the text has been read,
     *         the close() method of the Reader will be called.
     * @throws XPathException if any failure occurs
     * @since 8.9
     */

    public Reader resolve(URI absoluteURI, String encoding, Configuration config) throws XPathException {
        URL absoluteURL;
        if (debug) {
            System.err.println("unparsed-text(): processing " + absoluteURI);
            System.err.println("unparsed-text(): requested encoding = " + encoding);
        }
        try {
            absoluteURL = absoluteURI.toURL();
        } catch (MalformedURLException err) {
            XPathException e = new XPathException("Cannot convert absolute URI to URL", err);
            e.setErrorCode("XTDE1170");
            throw e;
        }
        try {
            InputStream is;
            URLConnection connection = absoluteURL.openConnection();
            connection.setRequestProperty("Accept-Encoding","gzip");
            connection.connect();

            is = connection.getInputStream();
            String contentEncoding = connection.getContentEncoding();

            if ("gzip".equals(contentEncoding)) {
                is = new GZIPInputStream(is);
            }
            if (debug) {
                System.err.println("unparsed-text(): established connection " +
                        ("gzip".equals(contentEncoding) ? " (zipped)" : ""));
            }
            try {

                if (!is.markSupported()) {
                    is = new BufferedInputStream(is);
                }

                // Get any external (HTTP) encoding label.
                boolean isXmlMediaType = false;

                // The file:// URL scheme gives no useful information...
                if (!"file".equals(connection.getURL().getProtocol())) {

                    // Use the contentType from the HTTP header if available
                    String contentType = connection.getContentType();
                    if (debug) {
                        System.err.println("unparsed-text(): content type = " + contentType);
                    }
                    if (contentType != null) {
                        String mediaType;
                        int pos = contentType.indexOf(';');
                        if (pos >= 0) {
                            mediaType = contentType.substring(0, pos);
                        } else {
                            mediaType = contentType;
                        }
                        mediaType = mediaType.trim();
                        if (debug) {
                            System.err.println("unparsed-text(): media type = " + mediaType);
                        }
                        isXmlMediaType = (mediaType.startsWith("application/") || mediaType.startsWith("text/")) &&
                                (mediaType.endsWith("/xml") || mediaType.endsWith("+xml"));

                        String charset = "";
                        pos = contentType.toLowerCase().indexOf("charset");
                        if (pos >= 0) {
                            pos = contentType.indexOf('=', pos + 7);
                            if (pos >= 0) {
                                charset = contentType.substring(pos + 1);
                            }
                            if ((pos = charset.indexOf(';')) > 0) {
                                charset = charset.substring(0, pos);
                            }

                            // attributes can have comment fields (RFC 822)
                            if ((pos = charset.indexOf('(')) > 0) {
                                charset = charset.substring(0, pos);
                            }
                            // ... and values may be quoted
                            if ((pos = charset.indexOf('"')) > 0) {
                                charset = charset.substring(pos + 1,
                                        charset.indexOf('"', pos + 2));
                            }
                            if (debug) {
                                System.err.println("unparsed-text(): charset = " + charset.trim());
                            }
                            encoding = charset.trim();
                        }
                    }
                }

                if (encoding == null || isXmlMediaType) {
                    // Try to detect the encoding from the start of the content
                    is.mark(100);
                    byte[] start = new byte[100];
                    int read = is.read(start, 0, 100);
                    is.reset();
                    encoding = inferEncoding(start, read);
                    if (debug) {
                        System.err.println("unparsed-text(): inferred encoding = " + encoding);
                    }
                }

            } catch (IOException e) {
                encoding = "UTF-8";
            }

            //}

            // The following appears to be necessary to ensure that encoding errors are not recovered.
            Charset charset = Charset.forName(encoding);
            CharsetDecoder decoder = charset.newDecoder();
            decoder = decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder = decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            return new BufferedReader(new InputStreamReader(is, decoder));


        } catch (IOException err) {
            throw new XPathException(err);
        }
    }

    /**
     * Infer the encoding of a file by reading the first few bytes of the file
     *
     * @param start the first few bytes of the file
     * @param read  the number of bytes that have been read
     * @return the inferred encoding
     */

    private String inferEncoding(byte[] start, int read) {
        if (read >= 2) {
            if (ch(start[0]) == 0xFE && ch(start[1]) == 0xFF) {
                if (debug) {
                    System.err.println("unparsed-text(): found UTF-16 byte order mark");
                }
                return "UTF-16";
            } else if (ch(start[0]) == 0xFF && ch(start[1]) == 0xFE) {
                if (debug) {
                    System.err.println("unparsed-text(): found UTF-16LE byte order mark");
                }
                return "UTF-16LE";
            }
        }
        if (read >= 3) {
            if (ch(start[0]) == 0xEF && ch(start[1]) == 0xBB && ch(start[2]) == 0xBF) {
                if (debug) {
                    System.err.println("unparsed-text(): found UTF-8 byte order mark");
                }
                return "UTF-8";
            }
        }
        if (read >= 4) {
            if (ch(start[0]) == '<' && ch(start[1]) == '?' &&
                    ch(start[2]) == 'x' && ch(start[3]) == 'm' && ch(start[4]) == 'l') {
                if (debug) {
                    System.err.println("unparsed-text(): found XML declaration");
                }
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
                    if (debug) {
                        System.err.println("unparsed-text(): encoding in XML declaration = " + sb.toString());
                    }
                    return sb.toString();
                }
                if (debug) {
                    System.err.println("unparsed-text(): no encoding found in XML declaration");
                }
            }
        } else if (read > 0 && start[0] == 0 && start[2] == 0 && start[4] == 0 && start[6] == 0) {
            if (debug) {
                System.err.println("unparsed-text(): even-numbered bytes are zero, inferring UTF-16");
            }
            return "UTF-16";
        } else if (read > 1 && start[1] == 0 && start[3] == 0 && start[5] == 0 && start[7] == 0) {
            if (debug) {
                System.err.println("unparsed-text(): odd-numbered bytes are zero, inferring UTF-16LE");
            }
            return "UTF-16LE";
        }
        // If all else fails, assume UTF-8
        if (debug) {
            System.err.println("unparsed-text(): assuming fallback encoding (UTF-8)");
        }
        return "UTF-8";
    }

    private int ch(byte b) {
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

