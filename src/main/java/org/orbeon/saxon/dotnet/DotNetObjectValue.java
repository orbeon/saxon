package org.orbeon.saxon.dotnet;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.om.StandardNames;


/**
* An XPath value that encapsulates a .NET object. Such a value can only be obtained by
* calling an extension function that returns it.
*/

public class DotNetObjectValue extends ObjectValue {

    public DotNetObjectValue(Object value) {
        super(value);
    }

    /**
    * Convert to target data type
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        Object value = getObject();
        switch(requiredType.getPrimitiveType()) {
        case StandardNames.XS_ANY_ATOMIC_TYPE:
        case StandardNames.SAXON_JAVA_LANG_OBJECT:
        case Type.ITEM:
            return this;
        case StandardNames.XS_BOOLEAN:
            return BooleanValue.get(
                    (value==null ? false : value.toString().length() > 0));
        case StandardNames.XS_STRING:
            return new StringValue(getStringValue());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        default:
            return new StringValue(getStringValue()).convert(requiredType, validate, context);
        }
    }

    /**
    * Determine the data type of the expression
    * @return Type.OBJECT
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return new DotNetExternalObjectType(((cli.System.Object)getObject()).GetType(), th.getConfiguration());
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

