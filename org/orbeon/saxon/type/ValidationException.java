package org.orbeon.saxon.type;

import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
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
    private NodeInfo node;
    private int schemaPart = -1;
    private String constraintName;
    private String constraintClauseNumber;

    // TODO: during output validation, it would sometimes be useful to know what the position in the input file was.

    /**
     * Creates a new ValidationException with the given message.
     * @param message the message for this Exception
     */

    public ValidationException(String message) {
        super(message);
        setIsTypeError(true);
    }

    /**
     * Creates a new ValidationException with the given nested
     * exception.
     * @param exception the nested exception
     */
    public ValidationException(Exception exception) {
        super(exception);
        setIsTypeError(true);
    }

    /**
     * Creates a new ValidationException with the given message
     * and nested exception.
     * @param message the detail message for this exception
     * @param exception the nested exception
     */
    public ValidationException(String message, Exception exception) {
        super(message, exception);
        setIsTypeError(true);
    }

    /**
     * Create a new ValidationException from a message and a Locator.
     * @param message The error or warning message.
     * @param locator The locator object for the error or warning.
     */
    public ValidationException(String message, SourceLocator locator) {
        super(message, locator);
        setIsTypeError(true);
        // With Xerces, it's enough to store the locator as part of the exception. But with Crimson,
        // the locator is destroyed when the parse terminates, which means the location information
        // will not be available in the final error message. So we copy the location information now,
        // as part of the exception object itself.
        setSourceLocator(locator);
    }

    /**
     * Set a reference to the constraint in XML Schema that is not satisfied
     * @param schemaPart - 1 or 2, depending whether the constraint is in XMLSchema part 1 or part 2
     * @param constraintName - the short name of the constraint in XMLSchema, as a fragment identifier in the
     * HTML of the XML Schema Part 1 specification
     * @param clause - the clause number within the description of that constraint
     */

    public void setConstraintReference(int schemaPart, String constraintName, String clause) {
        this.schemaPart = schemaPart;
        this.constraintName = constraintName;
        this.constraintClauseNumber = clause;
    }

    /**
     * Copy the constraint reference from another exception object
     * @param e the other exception object from which to copy the information
     */

    public void setConstraintReference(ValidationException e) {
        schemaPart = e.schemaPart;
        constraintName = e.constraintName;
        constraintClauseNumber = e.constraintClauseNumber;
    }

    /**
     * Get the constraint reference as a string for inserting into an error message.
     * @return the reference as a message, or null if no information is available
     */

    public String getConstraintReferenceMessage() {
        if (schemaPart == -1) {
            return null;
        }
        return "See http://www.w3.org/TR/xmlschema-" + schemaPart + "/#" + constraintName
                + " clause " + constraintClauseNumber;
    }

    /**
     * Get the "schema part" component of the constraint reference
     * @return 1 or 2 depending on whether the violated constraint is in XML Schema Part 1 or Part 2;
     * or -1 if there is no constraint reference
     */

    public int getConstraintSchemaPart() {
        return schemaPart;
    }

    /**
     * Get the constraint name
     * @return the name of the violated constraint, in the form of a fragment identifier within
     * the published XML Schema specification; or null if the information is not available.
     */

    public String getConstraintName() {
        return constraintName;
    }

    /**
     * Get the constraint clause number
     * @return the section number of the clause containing the constraint that has been violated.
     * Generally a decimal number in the form n.n.n; possibly a sequence of such numbers separated
     * by semicolons. Or null if the information is not available.
     */

    public String getConstraintClauseNumber() {
        return constraintClauseNumber;
    }

    /**
     * Get the constraint name and clause in the format defined in XML Schema Part C (Outcome Tabulations).
     * This mandates the format validation-rule-name.clause-number
     * @return the constraint reference, for example "cos-ct-extends.1.2"; or null if the reference
     * is not known.
     */

    public String getConstraintReference() {
        return constraintName + '.' + constraintClauseNumber;
    }



     /**
     * Returns the String representation of this Exception
     * @return the String representation of this Exception
    **/
    public String toString() {
        StringBuffer sb = new StringBuffer("ValidationException: ");
        String message = getMessage();
        if (message != null) {
            sb.append(message);
        }
        return sb.toString();
    }

    public String getPublicId() {
        SourceLocator loc = getLocator();
        if (publicId == null && loc != null && loc != this) {
            return loc.getPublicId();
        } else{
            return publicId;
        }
    }

    public String getSystemId() {
        SourceLocator loc = getLocator();
        if (systemId == null && loc != null && loc != this) {
            return loc.getSystemId();
        } else{
            return systemId;
        }
    }

    public int getLineNumber() {
        SourceLocator loc = getLocator();
        if (lineNumber == -1 && loc != null && loc != this) {
            return loc.getLineNumber();
        } else{
            return lineNumber;
        }
    }

    public int getColumnNumber() {
        SourceLocator loc = getLocator();
        if (columnNumber == -1 && loc != null && loc != this) {
            return loc.getColumnNumber();
        } else{
            return columnNumber;
        }
    }

    public NodeInfo getNode() {
        return node;
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
        if (locator != null) {
            setPublicId(locator.getPublicId());
            setSystemId(locator.getSystemId());
            setLineNumber(locator.getLineNumber());
            setColumnNumber(locator.getColumnNumber());
            if (locator instanceof NodeInfo) {
                node = ((NodeInfo)locator);
            }
        }
        super.setLocator(null);
    }

    public void setSourceLocator(SourceLocator locator) {
        if (locator != null) {
            setPublicId(locator.getPublicId());
            setSystemId(locator.getSystemId());
            setLineNumber(locator.getLineNumber());
            setColumnNumber(locator.getColumnNumber());
            if (locator instanceof NodeInfo) {
                node = ((NodeInfo)locator);
            }
        }
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