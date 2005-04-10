package net.sf.saxon.expr;

import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.event.SaxonLocator;

import javax.xml.transform.SourceLocator;
import java.io.Serializable;

/**
 * Class to hold details of the location of an expression, of an error in a source file, etc.
 */

public class ExpressionLocation implements SaxonLocator, Serializable {

    public ExpressionLocation() {}

    public ExpressionLocation(SourceLocator loc) {
        systemId = loc.getSystemId();
        lineNumber = loc.getLineNumber();
    }

    public ExpressionLocation(LocationProvider provider, int locationId) {
        systemId = provider.getSystemId(locationId);
        lineNumber = provider.getLineNumber(locationId);
    }

    private String systemId;
//    private String publicId;
    private int lineNumber;
    private int columnNumber;

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

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public void setPublicId(String publicId) {
//        this.publicId = publicId;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
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