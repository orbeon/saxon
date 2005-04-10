package net.sf.saxon.instruct;

import net.sf.saxon.expr.ComputedExpression;
import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.event.LocationProvider;

import java.io.Serializable;

/**
 * This object represents the compiled form of a user-written function, template, attribute-set, etc
 * (the source can be either an XSLT stylesheet function or an XQuery function).
 *
 * <p>It is assumed that type-checking, of both the arguments and the results,
 * has been handled at compile time. That is, the expression supplied as the body
 * of the function must be wrapped in code to check or convert the result to the
 * required type, and calls on the function must be wrapped at compile time to check or
 * convert the supplied arguments.
 */

public class Procedure implements Serializable, Container, LocationProvider {

    private Expression body;
    private Executable executable;
    private String systemId;
    private int lineNumber;
    private SlotManager stackFrameMap;

    public Procedure() {};

    public void setBody(Expression body) {
        this.body = body;
        ExpressionTool.makeParentReferences(body);
        if (body instanceof ComputedExpression) {
            ((ComputedExpression)body).setParentExpression(this);
        }
    }

    public final Expression getBody() {
        return body;
    }

    public void setStackFrameMap(SlotManager map) {
        stackFrameMap = map;
    }

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

    public final Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        return this;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getSystemId() {
        return systemId;
    }

    public int getColumnNumber() {
        return -1;
    }

    public String getPublicId() {
        return null;
    }

    public String getSystemId(int locationId) {
        return systemId;
    }

    public int getLineNumber(int locationId) {
        return lineNumber;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//
