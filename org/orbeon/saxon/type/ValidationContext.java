package net.sf.saxon.type;

/**
 * This interface defines an object representing
 * a validation context, as used in XQuery to control the effect of the validate{}
 * expression.
 * The real implementation of these declarations is available in the schema-aware
 * version of the Saxon product.
 */

public interface ValidationContext {

    /**
     * Determine whether this is a void validation context (a context in which nothing will validate)
     * @return true if this validation context is void
     */

    public boolean isVoidValidationContext();

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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//