package net.sf.saxon.type;

/**
 * Interface representing a simple type of variety List
 */

public interface ListType extends SimpleType {
    
    /**
     * Returns the simpleType of the items in this ListType. This method assumes that the
     * item type has been fully resolved
     * @return the simpleType of the items in this ListType.
     * @throws IllegalStateException if the item type has not been fully resolved
    */

    SimpleType getItemType();
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

