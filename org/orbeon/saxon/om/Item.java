package org.orbeon.saxon.om;
import org.orbeon.saxon.trans.XPathException;

/**
 * An Item is an object that can occur as a member of a sequence.
 * There are two kinds of Item: atomic values, and nodes.
 */

public interface Item extends ValueRepresentation {

    /**
     * Get the value of the item as a string
     * @return the string value of the item
     */

    public String getStringValue();

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS();


    /**
     * Get the typed value of the item
     * @return the typed value of the item. In general this will be a sequence
     * @throws net.sf.saxon.trans.XPathException where no typed value is available, e.g. for
     *     an element with complex content
     */

    public SequenceIterator getTypedValue() throws XPathException;


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
