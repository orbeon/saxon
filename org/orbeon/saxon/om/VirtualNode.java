package org.orbeon.saxon.om;

/**
 * This interface is implemented by NodeInfo implementations that act as wrappers
 * on some underlying tree. It provides a method to access the real node underlying
 * the virtual node, for use by applications that need to drill down to the
 * underlying data.
 */

public interface VirtualNode {

    /**
     * Get the real node undelying this virtual node.
     * @return The underlying node. Note that this may itself be
     * a VirtualNode; you may have to drill down through several layers of
     * wrapping.
     */

    public Object getUnderlyingNode();

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