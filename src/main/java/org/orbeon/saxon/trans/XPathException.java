package org.orbeon.saxon.trans;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

/**
 * XPathException is used to indicate an error in an XPath expression.
 * It will generally be either a StaticError or a DynamicError;
 * ValidationExceptions (arising from schema validation) form a third category
*/

public class XPathException extends TransformerException {

    private boolean isTypeError = false;
    private boolean isStaticError = false;
    String errorCodeNamespace; // TODO: implement error codes as QNames throughout
    String errorCode;
    Value errorObject;
    private boolean hasBeenReported = false;
    transient XPathContext context;
    // declared transient because a compiled stylesheet might contain a "deferred action" dynamic error
    // and the EarlyEvaluationContext links back to the source stylesheet.

    /**
     * Create an XPathException with an error message
     * @param message the message explaining what is wrong. This should not include location information.
     */

    public XPathException(String message) {
        super(message);
    }

    /**
     * Create an XPathException that wraps another exception
     * @param err the wrapped error or exception
     */

    public XPathException(Throwable err) {
        super(err);
    }

    /**
     * Create an XPathException that supplies an error message and wraps an underlying exception
     * @param message the error message (which should generally explain what Saxon was doing when the
     * underlying exception occurred)
     * @param err the underlying exception (the cause of this exception)
     */

    public XPathException(String message, Throwable err) {
        super(message, err);
    }

    /**
     * Create an XPathException that supplies an error message and supplies location information
     * @param message the error message
     * @param loc indicates where in the user-written query or stylesheet (or sometimes in a source
     * document) the error occurred
     */

    public XPathException(String message, SourceLocator loc) {
        super(message, loc);
    }

    /**
     * Create an XPathException that supplies an error message and wraps an underlying exception
     * and supplies location information
     * @param message the error message (which should generally explain what Saxon was doing when the
     * underlying exception occurred)
     * @param loc indicates where in the user-written query or stylesheet (or sometimes in a source
     * document) the error occurred
     * @param err the underlying exception (the cause of this exception)
     */

    public XPathException(String message, SourceLocator loc, Throwable err) {
        super(message, loc, err);
    }

    /**
     * Create an XPathException that supplies an error message and an error code
     * @param message the error message
     * @param errorCode the error code - an eight-character code, which is taken to be in the standard
     * system error code namespace
     */

    public XPathException(String message, String errorCode) {
        super(message);
        setErrorCode(errorCode);
    }

    /**
     * Create an XPathException that supplies an error message and an error code and provides the
     * dynamic context
     * @param message the error message
     * @param errorCode the error code - an eight-character code, which is taken to be in the standard
     * system error code namespace
     * @param context the dynamic evaluation context
     */

    public XPathException(String message, String errorCode, XPathContext context) {
        super(message);
        setErrorCode(errorCode);
        setXPathContext(context);
    }

    /**
     * Create an XPathException from a JAXP TransformerException. If the TransformerException is an XPathException,
     * or if its cause is an XPathException, that XPathException is returned unchanged; otherwise the
     * TransformerException is wrapped.
     * @param err the supplied JAXP TransformerException
     * @return an XPathException obtained from the supplied TransformerException
     */

    public static XPathException makeXPathException(TransformerException err) {
        if (err instanceof XPathException) {
            return (XPathException)err;
        } else if (err.getException() instanceof XPathException) {
            return (XPathException)err.getException();
        } else {
            return new XPathException(err);
        }
    }

    /**
     * Force an exception to a static error
     * @return this exception, marked as a static error
     */

    public XPathException makeStatic() {
        setIsStaticError(true);
        return this;
    }

    /**
     * Set dynamic context information in the exception object
     * @param context the dynamic context at the time the exception occurred
     */

    public void setXPathContext(XPathContext context) {
        this.context = context;
    }

    /**
     * Get the dynamic context at the time the exception occurred
     * @return the dynamic context if known; otherwise null
     */

    public XPathContext getXPathContext() {
        return context;
    }

    /**
     * Mark this exception to indicate that it represents (or does not represent) a static error
     * @param is true if this exception is a static error
     */

    public void setIsStaticError(boolean is) {
        isStaticError = is;
    }

    /**
     * Ask whether this exception represents a static error
     * @return true if this exception is a static error
     */

    public boolean isStaticError() {
        return isStaticError;
    }

    /**
     * Mark this exception to indicate that it represents (or does not represent) a type error
     * @param is true if this exception is a type error
     */

    public void setIsTypeError(boolean is) {
        isTypeError = is;
    }

    /**
     * Ask whether this exception represents a type error
     * @return true if this exception is a type error
     */

    public boolean isTypeError() {
        return isTypeError;
    }

    /**
     * Set the error code. The error code is a QName; this method sets the local part of the name,
     * and if no other namespace has been set, it sets the namespace of the error code to the standard
     * system namespace {@link NamespaceConstant#ERR}
     * @param code The local part of the name of the error code
     */

    public void setErrorCode(String code) {
        if (code != null) {
            this.errorCode = code;
            if (errorCodeNamespace == null) {
                errorCodeNamespace = NamespaceConstant.ERR;
            }
        }
    }

    /**
     * Set the error code. The error code is a QName; this method sets both parts of the name.
     * @param namespace The namespace URI part of the name of the error code
     * @param code The local part of the name of the error code
     */

    public void setErrorCode(String namespace, String code) {
        this.errorCode = code;
        this.errorCodeNamespace = namespace;
    }

    /**
     * Get the local part of the name of the error code
     * @return the local part of the name of the error code
     */

    public String getErrorCodeLocalPart() {
        return errorCode;
    }

    /**
     * Get the namespace URI part of the name of the error code
     * @return the namespace URI part of the name of the error code
     */

    public String getErrorCodeNamespace() {
        return errorCodeNamespace;
    }

    /**
     * Set the error object associated with this error. This is used by the standard XPath fn:error() function
     * @param value the error object, as supplied to the fn:error() function
     */

    public void setErrorObject(Value value) {
        errorObject = value;
    }

    /**
     * Get the error object associated with this error. This is used by the standard XPath fn:error() function
     * @return the error object, as supplied to the fn:error() function
     */

    public Value getErrorObject() {
        return errorObject;
    }

    /**
     * Mark this error to indicate that it has already been reported to the error listener, and should not be
     * reported again
     */

    public void setHasBeenReported() {
        hasBeenReported = true;
    }

    /**
     * Ask whether this error is marked to indicate that it has already been reported to the error listener,
     * and should not be reported again
     * @return true if this error has already been reported
     */

    public boolean hasBeenReported() {
        return hasBeenReported;
    }

    /**
     * Set the location of a message, only if it is not already set
     * @param locator the current location (or null)
     */

    public void maybeSetLocation(SourceLocator locator) {
        if ((getLocator() == null || getLocator().getLineNumber() == -1) && locator != null) {
            setLocator(locator);
        }
    }

    /**
     * Set the context of a message, only if it is not already set
     * @param context the current XPath context (or null)
     */

    public void maybeSetContext(XPathContext context) {
        if (getXPathContext() == null) {
            setXPathContext(context);
        }
    }

    /**
     * Subclass of XPathException used to report circularities
     */

    public static class Circularity extends XPathException {

        /**
         * Create an exception indicating that a circularity was detected
         * @param message the error message
         */
        public Circularity(String message) {
            super(message);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
