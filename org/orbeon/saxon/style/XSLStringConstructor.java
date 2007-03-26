package org.orbeon.saxon.style;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SimpleContentConstructor;
import org.orbeon.saxon.instruct.SimpleNodeConstructor;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.StringValue;

/**
 * Common superclass for XSLT elements whose content template produces a text
 * value: xsl:attribute, xsl:comment, and xsl:processing-instruction
 */

public abstract class XSLStringConstructor extends StyleElement {

    //protected String stringValue = null;
    protected Expression select = null;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void validate() throws XPathException {
        if (select != null && hasChildNodes()) {
            String errorCode = getErrorCodeForSelectPlusContent();
            compileError("An " + getDisplayName() + " element with a select attribute must be empty", errorCode);
        }
        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo first = (NodeInfo)kids.next();
        if (select == null) {
            if (first == null) {
                // there are no child nodes and no select attribute
                //stringValue = "";
                select = StringValue.EMPTY_STRING;
            } else {
                if (kids.next() == null) {
                    // there is exactly one child node
                    if (first.getNodeKind() == Type.TEXT) {
                        // it is a text node: optimize for this case
                        select = StringValue.makeStringValue(first.getStringValueCS());
                    }
                }
            }
        }
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     * @return the error code defined for this condition, for this particular instruction
     */

    protected abstract String getErrorCodeForSelectPlusContent();

    protected void compileContent(Executable exec, SimpleNodeConstructor inst, Expression separator) throws XPathException {
        if (separator == null) {
            separator = StringValue.SINGLE_SPACE;
        }
        try {
            if (select != null) {
                inst.setSelect(new SimpleContentConstructor(select, separator)
                        .simplify(getStaticContext()), exec.getConfiguration());
            } else {
                Expression content = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
                inst.setSelect(new SimpleContentConstructor(content, separator)
                        .simplify(getStaticContext()), exec.getConfiguration());
            }
        } catch (StaticError err) {
            compileError(err);
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
