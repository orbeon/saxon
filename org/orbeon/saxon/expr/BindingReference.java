package net.sf.saxon.expr;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

/**
* BindingReference is a interface used to mark references to a variable declaration. The main
* implementation is VariableReference, which represents a reference to a variable in an XPath
* expression, but it is also used to represent a reference to a variable in a saxon:assign instruction.
*/

public interface BindingReference  {

    /**
    * Fix up the static type of this variable reference; optionally, supply a constant value for
    * the variable. Also supplies other static properties of the expression to which the variable
    * is bound, for example whether it is an ordered node-set.
    */

    public void setStaticType(SequenceType type, Value constantValue, int properties);

    /**
    * Fix up this binding reference to a binding
    */

    public void fixup(Binding binding);

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
