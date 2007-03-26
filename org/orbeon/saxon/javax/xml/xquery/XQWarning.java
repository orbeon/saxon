package org.orbeon.saxon.javax.xml.xquery;

import org.orbeon.saxon.javax.xml.xquery.XQException;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public class XQWarning extends XQException {

    public XQWarning(String message) {
            super(message);
            }

    public XQWarning(java.lang.String message, java.lang.Throwable cause, java.lang.String vendorcode, XQWarning nextWarning) {
        super(message, cause, vendorcode, nextWarning);

    }
}
