package net.sf.saxon.type;

import net.sf.saxon.trans.XPathException;
import org.xml.sax.Locator;

import javax.xml.transform.SourceLocator;

/**
 * This exception indicates a failure when validating an instance against a type
 * defined in a schema.
 */

public class ValidationException extends XPathException
        implements SourceLocator, Locator {

    private String systemId;
    private String publicId;
    private int lineNumber = -1;
    private int columnNumber = -1;

    /**
     * Creates a new ValidationException with the given message.
     * @param message the message for this Exception
    **/
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Creates a new ValidationException with the given nested
     * exception.
     * @param exception the nested exception
    **/
    public ValidationException(Exception exception) {
        super(exception);
    }

    /**
     * Creates a new ValidationException with the given message
     * and nested exception.
     * @param message the detail message for this exception
     * @param exception the nested exception
    **/
    public ValidationException(String message, Exception exception) {
        super(message, exception);
    }

    /**
     * Create a new XPathException from a message and a Locator.
     * @param message The error or warning message.
     * @param locator The locator object for the error or warning.
     */
    public ValidationException(String message, SourceLocator locator) {
        super(message, locator);
        // With Xerces, it's enough to store the locator as part of the exception. But with Crimson,
        // the locator is destroyed when the parse terminates, which means the location information
        // will not be available in the final error message. So we copy the location information now,
        // as part of the exception object itself.
        setSourceLocator(locator);
    }

     /**
     * Returns the String representation of this Exception
     * @return the String representation of this Exception
    **/
    public String toString() {
        StringBuffer sb = new StringBuffer("ValidationException: ");
        String message = getMessage();
        if (message != null) sb.append(message);
        return sb.toString();
    }

    public String getPublicId() {
        if (publicId == null && getLocator() != this) {
            return getLocator().getPublicId();
        } else{
            return publicId;
        }
    }

    public String getSystemId() {
        if (systemId == null && getLocator() != this) {
            return getLocator().getSystemId();
        } else{
            return systemId;
        }
    }

    public int getLineNumber() {
        if (lineNumber == -1 && getLocator() != this) {
            return getLocator().getLineNumber();
        } else{
            return lineNumber;
        }
    }

    public int getColumnNumber() {
        if (columnNumber == -1 && getLocator() != this) {
            return getLocator().getColumnNumber();
        } else{
            return columnNumber;
        }
    }

    public void setPublicId(String id) {
        publicId = id;
    }

    public void setSystemId(String id) {
        systemId = id;
    }

    public void setLineNumber(int line) {
        lineNumber = line;
    }

    public void setColumnNumber(int column) {
        columnNumber = column;
    }

    public void setLocator(Locator locator) {
        setPublicId(locator.getPublicId());
        setSystemId(locator.getSystemId());
        setLineNumber(locator.getLineNumber());
        setColumnNumber(locator.getColumnNumber());
        super.setLocator(null);
    }

    public void setSourceLocator(SourceLocator locator) {
        setPublicId(locator.getPublicId());
        setSystemId(locator.getSystemId());
        setLineNumber(locator.getLineNumber());
        setColumnNumber(locator.getColumnNumber());
        super.setLocator(null);
    }

    public SourceLocator getLocator() {
        SourceLocator loc = super.getLocator();
        if (loc != null) {
            return loc;
        } else {
            return this;
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//