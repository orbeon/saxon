package org.orbeon.saxon.expr;

import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.event.SaxonLocator;
import org.orbeon.saxon.instruct.LocationMap;
import org.orbeon.saxon.instruct.InstructionDetails;

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

    public ExpressionLocation(String systemId, int lineNumber, int columnNumber) {
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    private String systemId;
//    private String publicId;
    private int lineNumber;
    private int columnNumber = -1;

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

    /**
     * Construct an object holding location information for a validation error message
     * @param locationId The locationId as supplied with an event such as startElement or attribute
     * @param locationProvider The object that understands how to interpret the locationId
     * @return a SaxonLocator containing the location information
     */
    public static SaxonLocator getSourceLocator(int locationId, LocationProvider locationProvider) {
        SaxonLocator locator;
        if (locationProvider instanceof LocationMap && locationId != 0) {
            // this is typically true when validating output documents
            ExpressionLocation loc = new ExpressionLocation();
            loc.setLineNumber(locationProvider.getLineNumber(locationId));
            loc.setSystemId(locationProvider.getSystemId(locationId));
            locator = loc;
        } else if (locationProvider instanceof SaxonLocator) {
            // this is typically true when validating input documents
            locator = (SaxonLocator)locationProvider;
        } else {
            // return a dummy location object providing no information. This can happen for example
            // if a built-in template rule writes invalid output before the transformation properly begins.
            return new InstructionDetails();
        }
        return locator;
    }

    /**
     * Truncate a URI to its last component
     */

    public static String truncateURI(String uri) {
        String file = uri;
        if (file == null) file = "";
        while (true) {
            int i = file.indexOf('/');
            if (i >= 0 && i < file.length() - 6) {
                file = file.substring(i + 1);
            } else {
                break;
            }
        }
        return file;
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