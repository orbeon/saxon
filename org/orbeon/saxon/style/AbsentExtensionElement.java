package org.orbeon.saxon.style;
import org.orbeon.saxon.instruct.Instruction;
import org.orbeon.saxon.instruct.Block;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.expr.Expression;

import javax.xml.transform.TransformerConfigurationException;
import java.util.List;
import java.util.ArrayList;

/**
* This element is a surrogate for an extension element (or indeed an xsl element)
* for which no implementation is available.
*/

public class AbsentExtensionElement extends StyleElement {

    /**
    * Determine whether this type of element is allowed to contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws TransformerConfigurationException {
    }

    public void validate() throws TransformerConfigurationException {
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {

        if (isTopLevel()) {
            return null;
        }

        // if there are fallback children, compile the code for the fallback elements

        List list = new ArrayList();
        if (validationError==null) {
            validationError = new TransformerConfigurationException("Unknown instruction");
        }
        fallbackProcessing(exec, this, list);
        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return (Instruction)list.get(0);
        } else {
            // We are getting back one Block for each xsl:fallback element,
            // then we are wrapping these blocks in another Block. This
            // is clumsy, but it's not a commonly-used operation...
            Block block = new Block();
            Instruction[] array = new Instruction[list.size()];
            for (int i=0; i<list.size(); i++) {
                array[i] = (Instruction)list.get(i);
            }
            block.setChildren(array);
            return block;
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
