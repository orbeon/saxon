package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.xpath.XPathException;

import java.util.Iterator;
import java.io.PrintStream;


/**
 * A run-time instruction which wraps a real instruction and traces its entry and exit to the
 * TraceListener
 */

public class TraceInstruction extends TraceWrapper {

    InstructionInfo details;

    /**
     * Create a Trace instruction
     * @param child the "real" instruction to be traced
     */

    public TraceInstruction(Expression child, InstructionInfo details) {
        this.child = child;
        this.details = details;
        adoptChildExpression(child);
    }

   /**
     * Create a Trace instruction
     * @param child the "real" instruction to be traced
     */

    public TraceInstruction(Expression child) {
        this.child = child;
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
