package net.sf.saxon.trace;

/**
 * An InstructionInfoProvider is an object that is capable of providing an InstructionInfo
 * object which provides diagnostic information about an instruction or other construct such
 * as a function, template, expression, or pattern.
*/

public interface InstructionInfoProvider {

    /**
     * Get the InstructionInfo details about the construct
     */

    public InstructionInfo getInstructionInfo();
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
// Contributor(s): none.
//
