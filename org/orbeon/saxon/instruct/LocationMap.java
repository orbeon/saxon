package org.orbeon.saxon.instruct;

import org.orbeon.saxon.event.LocationProvider;

import java.io.Serializable;

/**
 * A LocationMap allocates integer codes to (systemId, lineNumber) pairs. The integer
 * codes are held inside an Expression object to track the location of the expression
 * in the source code
 */

public class LocationMap implements LocationProvider, Serializable {

    private String[] modules = new String[10];
    private int numberOfModules = 0;

    /**
     * Allocate a location identifier to an expression
     */

    public int allocateLocationId(String module, int lineNumber) {
        if (module == null) {
            // the module has no base URI
            module = "module-" + numberOfModules + " (no base URI)";
        }
        int mod = -1;
        for (int m=numberOfModules-1; m>=0; m--) {
            if (modules[m].equals(module)) {
                mod = m;
            }
        }
        if (mod == -1) {
            if (numberOfModules >= modules.length) {
                String[] m2 = new String[numberOfModules*2];
                System.arraycopy(modules, 0, m2, 0, numberOfModules);
                modules = m2;
            }
            mod = numberOfModules;
            modules[numberOfModules++] = module;
        }
        if (mod >= 1024) {
            modules[mod] = "*unknown module*";
            mod = 1023;
        }
        if (lineNumber > 999999) {
            lineNumber = 999999;
        }
        return (mod<<20) + lineNumber;
    }

    /**
     * Get the system identifier corresponding to a locationId
     */

    public String getSystemId(int locationId) {
        int m = locationId>>20;
        if (m < 0 || m >= numberOfModules) {
            return null;
        }
        return modules[m];
    }

    /**
     * Get the line number corresponding to a locationId
     */

    public int getLineNumber(int locationId) {
        return locationId & 0xfffff;
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