package org.orbeon.saxon.instruct;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.XPathException;


/**
* Interface representing a Tail Call. This is a package of information passed back by a called
* instruction to its caller, representing a call (and its arguments) that needs to be made
* by the caller. This saves stack space by unwinding the stack before making the call.
*/

public interface TailCall {

    /**
    * Process this TailCall (that is, executed the template call that is packaged in this
    * object). This may return a further TailCall, which should also be processed: this
    * is the mechanism by which a nested set of recursive calls is converted into an iteration.
    * @param context The dynamic context of the transformation
    * @return a further TailCall, if the recursion continues, or null, indicating that the
    * recursion has terminated.
    * @throws net.sf.saxon.trans.XPathException if any error occurs processing the tail call
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
// Contributor(s): none.
//
