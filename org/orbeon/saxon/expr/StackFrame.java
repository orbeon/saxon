package net.sf.saxon.expr;

import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.value.Value;

/**
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: 16-Jul-2004
 * Time: 17:57:33
 * To change this template use Options | File Templates.
 */
public class StackFrame {
    protected SlotManager map;
    protected Value[] slots;

    public StackFrame (SlotManager map, Value[] slots) {
        this.map = map;
        this.slots = slots;
    }

    public SlotManager getStackFrameMap() {
        return map;
    }

    public Value[] getStackFrameValues() {
        return slots;
    }

    public static StackFrame EMPTY = new StackFrame(null, new Value[0]);
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