package net.sf.saxon.om;
import net.sf.saxon.event.Stripper;

/**
  * The AllElementStripper refines the Stripper class to do stripping of
  * all whitespace nodes in a document
  * @author Michael H. Kay
  */

public class AllElementStripper extends Stripper {

    private static AllElementStripper theInstance = new AllElementStripper();

    public static AllElementStripper getInstance() {
        return theInstance;
    }

    public AllElementStripper() {}

    public Stripper getAnother() {
        return theInstance;
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param nameCode identifies the element being tested
    * @return STRIP_DEFAULT: strip spaces unless xml:space tells you not to.
    */

    public byte isSpacePreserving(int nameCode) {
        return STRIP_DEFAULT;
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types.
     * This version of the method is useful in cases where getting the namecode of the
     * element is potentially expensive, e.g. with DOM nodes.
    * @param element Identifies the element whose whitespace is possibly to
     * be preserved
    */

    public byte isSpacePreserving(NodeInfo element) {
        return STRIP_DEFAULT;
    }

}   // end of class AllElementStripper

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
