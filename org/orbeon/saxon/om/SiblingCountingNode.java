package net.sf.saxon.om;

/**
 * Interface that extends NodeInfo by providing a method to get the position
 * of a node relative to its siblings.
 */

public interface SiblingCountingNode extends NodeInfo {

    /**
     * Get the index position of this node among its siblings (starting from 0)
     * @return 0 for the first child, 1 for the second child, etc.
     */
    public int getSiblingPosition();
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