package net.sf.saxon.instruct;

import java.io.Serializable;


/**
 * A SlotManager supports functions, templates, etc: specifically, any executable code that
 * requires a stack frame containing local variables. In XSLT a SlotManager underpins any
 * top-level element that can contain local variable declarations,
 * specifically, a top-level xsl:template, xsl:variable, xsl:param, or xsl:function element
 * or an xsl:attribute-set element or xsl:key element. In XQuery it underpins functions and
 * global variables. The purpose of the SlotManager is to allocate slot numbers to variables
 * in the stack, and to record how many slots are needed. A Debugger may define a subclass
 * with additional functionality.
*/

public class SlotManager implements Serializable {

    private int numberOfVariables = 0;

    /**
     * The constructor should not be called directly. A new SlotManager should be obtained using
     * the factory method in the Configuration object.
     */

    public SlotManager(){}

    /**
    * Get number of variables (size of stack frame)
    */

    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    /**
     * Set the number of variables
     * @param numberOfVariables
     */

    public void setNumberOfVariables(int numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
    }

    /**
    * Allocate a slot number for a variable
    */

    public int allocateSlotNumber(int fingerprint) {
        return numberOfVariables++;
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
