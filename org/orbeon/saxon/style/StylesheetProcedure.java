package net.sf.saxon.style;

import net.sf.saxon.instruct.SlotManager;

/**
 * This interface is implemented by all XSL elements that can contain local variable declarations.
 * Specifically, a top-level xsl:template, xsl:variable, xsl:param, or xsl:function element
 * or an xsl:attribute-set element or xsl:key element.
 */

public interface StylesheetProcedure {

    /**
     * Get the SlotManager associated with this stylesheet construct. The SlotManager contains the
     * information needed to manage the local stack frames used by run-time instances of the code.
     * @return the associated SlotManager object
     */

    public SlotManager getSlotManager();

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