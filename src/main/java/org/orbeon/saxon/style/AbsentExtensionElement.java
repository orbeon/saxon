package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.trans.XPathException;

/**
* This element is a surrogate for an extension element (or indeed an xsl element)
* for which no implementation is available.
*/

public class AbsentExtensionElement extends StyleElement {

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Process the attributes of this element and all its children
     */

    public void processAllAttributes() throws XPathException {
        if (isTopLevel() && forwardsCompatibleModeIsEnabled()) {
            // do nothing
        } else {
            super.processAllAttributes();
        }
    }

    public void prepareAttributes() throws XPathException {
    }

    /**
     * Recursive walk through the stylesheet to validate all nodes
     */

    public void validateSubtree() throws XPathException {
        if (isTopLevel() && forwardsCompatibleModeIsEnabled()) {
            // do nothing
        } else {
            super.validateSubtree();
        }
    }

    public void validate() throws XPathException {
    }

    public Expression compile(Executable exec) throws XPathException {

        if (isTopLevel()) {
            return null;
        }

        // if there are fallback children, compile the code for the fallback elements

        if (validationError==null) {
            validationError = new XPathException("Unknown instruction");
        }
        return fallbackProcessing(exec, this);
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
// Contributor(s):
//
