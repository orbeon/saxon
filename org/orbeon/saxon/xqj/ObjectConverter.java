package org.orbeon.saxon.xqj;

import org.orbeon.saxon.om.Item;

import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemAccessor;
import javax.xml.xquery.XQItemType;

/**
 * This interface is based on the "CommonHandler" concept defined in early drafts of XQJ. It defines the data
 * conversion routines used by the Saxon XQJ implementation to convert between native Java objects and XDM values.
 * Most applications will use the Saxon-supplied implementation {@link StandardObjectConverter}, but it is possible
 * to supply an alternative implementation using the method {@link SaxonXQDataFactory#setObjectConverter}
 */
public interface ObjectConverter {

    /**
     * Convert an Item to a Java object
     * @param xqItemAccessor the XQJ object representing the item to be converted
     * @return the Java object that results from the conversion
     * @throws XQException
     */

    Object toObject(XQItemAccessor xqItemAccessor) throws XQException;

    /**
     * Convert a Java object to an Item, when no information is available about the required type
     * @param value the supplied Java object
     * @return the Item that results from the conversion
     * @throws XQException if the Java object cannot be converted to an XQItem
     */

    Item convertToItem(Object value) throws XQException;

   /**
     * Convert a Java object to an Item, when a required type has been specified. Note that Saxon only calls
     * this method when none of the standard conversions defined in the XQJ specification is able to handle
     * the object.
     * @param value the supplied Java object
     * @param type the required XPath data type
     * @return the Item that results from the conversion
     * @throws XQException if the Java object cannot be converted to an XQItem
     */

    public Item convertToItem(Object value, XQItemType type) throws XQException;
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

