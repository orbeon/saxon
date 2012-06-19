package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.type.ItemType;

import java.io.Serializable;

/**
 * A RoleLocator identifies the role in which an expression is used, for example as
 * the third argument of the concat() function. This information is stored in an
 * ItemChecker or CardinalityChecker so that good diagnostics can be
 * achieved when run-time type errors are detected.
 */
public class RoleLocator implements Serializable {

    private int kind;
    private Serializable operation; // always either a String or a StructuredQName
    private int operand;
    private String errorCode = "XPTY0004";  // default error code for type errors

    public static final int FUNCTION = 0;
    public static final int BINARY_EXPR = 1;
    public static final int TYPE_OP = 2;
    public static final int VARIABLE = 3;
    public static final int INSTRUCTION = 4;
    public static final int FUNCTION_RESULT = 5;
    public static final int ORDER_BY = 6;
    public static final int TEMPLATE_RESULT = 7;
    public static final int PARAM = 8;
    public static final int UNARY_EXPR = 9;
    public static final int UPDATING_EXPR = 10;

    /**
     * Create information about the role of a subexpression within its parent expression
     * @param kind the kind of parent expression, e.g. a function call or a variable reference
     * @param operation the name of the object in the parent expression, e.g. a function name or
 * instruction name. May be expressed either as a String or as a {@link org.orbeon.saxon.om.StructuredQName}.
 * For a string, the special format element/attribute is recognized, for example xsl:for-each/select,
 * to identify the role of an XPath expression in a stylesheet.
     * @param operand Ordinal position of this subexpression, e.g. the position of an argument in
     */

    public RoleLocator(int kind, Serializable operation, int operand) {
        if (!(operation instanceof String || operation instanceof StructuredQName)) {
            throw new IllegalArgumentException("operation");
        }
        this.kind = kind;
        this.operation = operation;
        this.operand = operand;
    }

    /**
     * Set the error code to be produced if a type error is detected
     * @param code The error code
     */

    public void setErrorCode(String code) {
        if (code != null) {
            this.errorCode = code;
        }
    }

    /**
     * Get the error code to be produced if a type error is detected
     * @return code The error code
     */

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Set the source location
     */

//    public void setSourceLocator(SourceLocator locator) {
//        // this is currently used only when type-checking literals,
//        // which don't have any location information of their own
//        if (locator instanceof ExpressionLocation) {
//            this.sourceLocator = locator;
//        } else {
//            this.sourceLocator = new ExpressionLocation(locator);
//        }
//        // the supplied value isn't saved because the locator may be an expression that
//        // contains links back to the containing stylesheet, which causes the stylesheet
//        // to remain in memory at run-time (and prevents stylesheet compilation)
//    }

    /**
     * Get the source location (if known - return null if not known)
     */

//    public SourceLocator getSourceLocator() {
//        return sourceLocator;
//    }

    /**
     * Construct and return the error message indicating a type error
     * @return the constructed error message
     */
    public String getMessage() {
        String name;
        if (operation instanceof String) {
            name = (String)operation;
        } else {
            name = ((StructuredQName)operation).getDisplayName();
        }

        switch (kind) {
            case FUNCTION:
                return ordinal(operand+1) + " argument of " + name + "()";
            case BINARY_EXPR:
                return ordinal(operand+1) + " operand of '" + name + '\'';
            case UNARY_EXPR:
                return "operand of '-'";    
            case TYPE_OP:
                return "value in '" + name + "' expression";
            case VARIABLE:
                return "value of variable $" + name;
            case INSTRUCTION:
                int slash = name.indexOf('/');
                String attributeName = "";
                if (slash >= 0) {
                    attributeName = name.substring(slash+1);
                    name = name.substring(0, slash);
                }
                return '@' + attributeName + " attribute of " + name;
            case FUNCTION_RESULT:
                return "result of function " + name + "()";
            case TEMPLATE_RESULT:
                return "result of template " + name;
            case ORDER_BY:
                return ordinal(operand+1) + " sort key";
            case PARAM:
                return "value of parameter $" + name;
            case UPDATING_EXPR:
                return "value of " + ordinal(operand+1) + " operand of " + name + " expression";
            default:
                return "";
        }
    }

    /**
     * Construct the part of the message giving the required item type
     * @param requiredItemType the item type required by the context of a particular expression
     * @param pool the name pool
     * @return a message of the form "Required item type of X is Y"
     */

    public String composeRequiredMessage(ItemType requiredItemType, NamePool pool) {
        return "Required item type of " + getMessage() +
                     " is " + requiredItemType.toString(pool);
    }

    /**
     * Construct a full error message
     * @param requiredItemType the item type required by the context of a particular expression
     * @param suppliedItemType the item type inferred by static analysis of an expression
     * @param pool the name pool
     * @return a message of the form "Required item type of A is R; supplied value has item type S"
     */

    public String composeErrorMessage(ItemType requiredItemType, ItemType suppliedItemType, NamePool pool) {
        return "Required item type of " + getMessage() +
                     " is " + requiredItemType.toString(pool) +
                     "; supplied value has item type " +
                     suppliedItemType.toString(pool);
    }

    /**
     * Get the ordinal representation of a number (used to identify which argument of a function
     * is in error)
     * @param n the cardinal number
     * @return the ordinal representation
     */
    private static String ordinal(int n) {
        switch(n) {
            case 1:
                return "first";
            case 2:
                return "second";
            case 3:
                return "third";
            default:
                // we can live with 21th, 22th... How many functions have >20 arguments?
                return n + "th";
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