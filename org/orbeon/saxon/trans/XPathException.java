package org.orbeon.saxon.trans;

import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

/**
 * XPathException is used to indicate an error in an XPath expression.
 * It will generally be either a StaticError or a DynamicError;
 * ValidationExceptions (arising from schema validation) form a third category
*/

public abstract class XPathException extends TransformerException {

    private boolean isTypeError = false;
    String errorCodeNamespace;
    String errorCode;
    Value errorObject;
    private boolean hasBeenReported = false;

    public XPathException(String message) {
        super(message);
    }

    public XPathException(Throwable err) {
        super(err);
    }

    public XPathException(String message, Throwable err) {
        super(message, err);
    }

    public XPathException(String message, SourceLocator loc) {
        super(message, loc);
    }

    public XPathException(String message, SourceLocator loc, Throwable err) {
        super(message, loc, err);
    }

    /**
     * Force an exception to a static error
     */

    public StaticError makeStatic() {
        StaticError err = new StaticError(this);
        err.setErrorCode(getErrorCodeNamespace(), getErrorCodeLocalPart());
        return err;
    }

    public void setIsTypeError(boolean is) {
        isTypeError = is;
    }

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

    public void setErrorObject(Value value) {
        errorObject = value;
    }

    public Value getErrorObject() {
        return errorObject;
    }

    public void setHasBeenReported() {
        hasBeenReported = true;
    }

    public boolean hasBeenReported() {
        return hasBeenReported;
    }

    /**
     * Subclass used to report circularities
     */

    public static class Circularity extends DynamicError {
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
