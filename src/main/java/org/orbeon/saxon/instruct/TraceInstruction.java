package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.trace.InstructionInfo;


/**
 * A run-time instruction which wraps a real instruction and traces its entry and exit to the
 * TraceListener
 */

public class TraceInstruction extends TraceWrapper {

    InstructionInfo details;

    /**
     * Create a Trace instruction
     * @param child the child instruction to be evaluated
     * @param details the details to be output. This will usually relate to a function or template header.
     */

    public TraceInstruction(Expression child, InstructionInfo details) {
        this.child = child;
        this.details = details;
        adoptChildExpression(child);
    }

    /**
     * Get the instruction details
     */

    public InstructionInfo getInstructionInfo() {
        return details;
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
