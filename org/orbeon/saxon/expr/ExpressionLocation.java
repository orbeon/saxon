package org.orbeon.saxon.expr;

import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.event.SaxonLocator;
import org.orbeon.saxon.instruct.LocationMap;
import org.xml.sax.Locator;

import javax.xml.transform.SourceLocator;
import java.io.Serializable;

/**
 * Class to hold details of the location of an expression, of an error in a source file, etc.
 */

public class ExpressionLocation implements SaxonLocator, Serializable {

    private String systemId;
    private int lineNumber;
    private int columnNumber = -1;

    /**
     * Create an ExpressionLocation
     */

    public ExpressionLocation() {}

    /**
     * Create an ExpressionLocation, taking the data from a supplied JAXP SourceLocator
     * @param loc the JAXP SourceLocator
     */

    public ExpressionLocation(SourceLocator loc) {
        systemId = loc.getSystemId();
        lineNumber = loc.getLineNumber();
        columnNumber = loc.getColumnNumber();
    }

    /**
     * Create an ExpressionLocation, taking the data from a supplied SAX Locator
     * @param loc the SAX Locator
     */

    public static ExpressionLocation makeFromSax(Locator loc) {
        return new ExpressionLocation(loc.getSystemId(), loc.getLineNumber(), loc.getColumnNumber());
    }

    /**
     * Create an ExpressionLocation, taking the data from a supplied locationId along with a
     * LocationProvider to interpret its meaning
     * @param provider the LocationProvider
     * @param locationId the locationId
     */

    public ExpressionLocation(LocationProvider provider, long locationId) {
        systemId = provider.getSystemId(locationId);
        lineNumber = provider.getLineNumber(locationId);
    }

    /**
     * Create an ExpressionLocation corresponding to a given module, line number, and column number
     * @param systemId the module URI
     * @param lineNumber the line number
     * @param columnNumber the column number
     */

    public ExpressionLocation(String systemId, int lineNumber, int columnNumber) {
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Get the system ID (the module URI)
     * @return the system ID
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the Public ID
     * @return always null in this implementation
     */

    public String getPublicId() {
        return null;
    }

    /**
     * Get the line number
     * @return the line number
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number
     * @return the column number
     */

    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Set the systemId (the module URI)
     * @param systemId the systemId
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Set the line number
     * @param lineNumber the line number within the module
     */

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Set the column number
     * @param columnNumber  the column number
     */

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    /**
     * Get the system Id corresponding to a given location Id
     * @param locationId the location Id
     * @return the system Id
     */

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    /**
     * Get the line number corresponding to a given location Id
     * @param locationId the location Id
     * @return the line number
     */

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
    }

    /**
     * Construct an object holding location information for a validation error message
     * @param locationId The locationId as supplied with an event such as startElement or attribute
     * @param locationProvider The object that understands how to interpret the locationId
     * @return a SaxonLocator containing the location information
     */
    public static SaxonLocator getSourceLocator(long locationId, LocationProvider locationProvider) {
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
            return new ExpressionLocation();
        }
        return locator;
    }

    /**
     * Truncate a URI to its last component
     * @param uri the URI to be truncated
     * @return the last component of the supplied URI
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