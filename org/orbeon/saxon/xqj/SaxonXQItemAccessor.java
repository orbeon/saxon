package org.orbeon.saxon.xqj;

import org.orbeon.saxon.om.Item;

import javax.xml.xquery.XQException;

/**
 * All Saxon implementations of XQItemAccessor must implement this interface
 */
public interface SaxonXQItemAccessor {

    /**
     * Get the current item
     * @return the current item
     */

    public Item getSaxonItem() throws XQException;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

