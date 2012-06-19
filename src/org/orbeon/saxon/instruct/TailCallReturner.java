package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;

/**
 * This interface represents an expression that is capable of being processed leaving tail calls for the
 * calling instruction to deal with.
 */

public interface TailCallReturner {

    /**
    * ProcessLeavingTail: called to do the real work of this instruction. This method
    * must be implemented in each subclass. The results of the instruction are written
    * to the current Receiver, which can be obtained via the Controller.
    * @param context The dynamic context of the transformation, giving access to the current node,
    * the current variables, etc.
    * @return null if the instruction has completed execution; or a TailCall indicating
    * a function call or template call that is delegated to the caller, to be made after the stack has
    * been unwound so as to save stack space.
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException;
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
