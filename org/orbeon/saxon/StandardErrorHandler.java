package org.orbeon.saxon;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import java.io.PrintWriter;
import java.io.Writer;

public class StandardErrorHandler implements org.xml.sax.ErrorHandler, javax.xml.transform.SourceLocator {

    ////////////////////////////////////////////////////////////////////////////
    // Implement the org.xml.sax.ErrorHandler interface.
    ////////////////////////////////////////////////////////////////////////////

    private ErrorListener errorListener;
    private String systemId;
    private int lineNumber = -1;
    private int columnNumber = -1;

    private Writer errorOutput = new PrintWriter(System.err);

    public StandardErrorHandler(ErrorListener listener) {
        errorListener = listener;
    }

    /**
    * Set output for error messages produced by the default error handler.
    * The default error handler does not throw an exception
    * for parse errors or input I/O errors, rather it returns a result code and
    * writes diagnostics to a user-specified output writer, which defaults to
    * System.err<BR>
    * This call has no effect if setErrorHandler() has been called to supply a
    * user-defined error handler
    * @param writer The Writer to use for error messages
    */

    public void setErrorOutput(Writer writer) {
        errorOutput = writer;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void warning (SAXParseException e) {
        if (errorListener != null) {
            try {
                errorListener.warning(new TransformerException(e));
            } catch (Exception err) {}
        }
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void error (SAXParseException e) throws SAXException {
        reportError(e, false);
        //failed = true;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void fatalError (SAXParseException e) throws SAXException {
        reportError(e, true);
        //failed = true;
        throw e;
    }

    /**
    * Common routine for SAX errors and fatal errors
    */

    protected void reportError (SAXParseException e, boolean isFatal) {

        if (errorListener != null) {
            try {
                systemId = e.getSystemId();
                lineNumber = e.getLineNumber();
                columnNumber = e.getColumnNumber();
                TransformerException err =
                    new TransformerException("Error reported by XML parser", this, e);
                if (isFatal) {
                    errorListener.fatalError(err);
                } else {
                    errorListener.error(err);
                }
            } catch (Exception err) {}
        } else {

            try {
                String errcat = (isFatal ? "Fatal error" : "Error");
                errorOutput.write(errcat + " reported by XML parser: " + e.getMessage() + "\n");
                errorOutput.write("  URL:    " + e.getSystemId() + "\n");
                errorOutput.write("  Line:   " + e.getLineNumber() + "\n");
                errorOutput.write("  Column: " + e.getColumnNumber() + "\n");
                errorOutput.flush();
            } catch (Exception e2) {
                System.err.println(e);
                System.err.println(e2);
                e2.printStackTrace();
            };
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Implement the SourceLocator interface.
    ////////////////////////////////////////////////////////////////////////////

    public String getSystemId() {
        return systemId;
    }

    public String getPublicId() {
        return null;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
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