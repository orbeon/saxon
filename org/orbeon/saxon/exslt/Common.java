package net.sf.saxon.exslt;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Value;

/**
* This class implements extension functions in the
* http://exslt.org/common namespace. <p>
*/



public abstract class Common  {

    /**
     * Class is not instantiated
     */
    private Common() {
    }

    /**
    * Convert a result tree fragment to a node-set. This is a hangover from XSLT 1.0;
    * it is implemented as a no-op.
    */

    public static Value nodeSet(Value frag) {
        return frag;
    }

    /**
    * Return the type of the supplied value: "sequence", "string", "number", "boolean",
    * "external". (EXSLT spec not yet modified to cater for XPath 2.0 data model)
    */

    public static String objectType(XPathContext context, Value value) {
        ItemType type = value.getItemType();
        if (Type.isSubType(type, AnyNodeTest.getInstance())) {
            return "node-set";
        } else if (Type.isSubType(type, Type.STRING_TYPE)) {
            return "string";
        } else if (Type.isSubType(type, Type.NUMBER_TYPE)) {
            return "number";
        } else if (Type.isSubType(type, Type.BOOLEAN_TYPE)) {
            return "boolean";
        } else {
            return type.toString(context.getController().getNamePool());
        }
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
