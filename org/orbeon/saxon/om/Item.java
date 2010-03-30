package org.orbeon.saxon.om;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.evpull.PullEvent;

/**
 * An Item is an object that can occur as a member of a sequence.
 * It corresponds directly to the concept of an item in the XPath 2.0 data model.
 * There are two kinds of Item: atomic values, and nodes.
 * <p>
 * This interface is part of the public Saxon API. As such (starting from Saxon 8.4),
 * methods that form part of the stable API are labelled with a JavaDoc "since" tag
 * to identify the Saxon release at which they were introduced.
 * <p>
 * Note: there is no method getItemType(). This is to avoid having to implement it
 * on every implementation of NodeInfo. Instead, use the static method Type.getItemType(Item).
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public interface Item extends ValueRepresentation, PullEvent {

    /**
     * Get the value of the item as a string. For nodes, this is the string value of the
     * node as defined in the XPath 2.0 data model, except that all nodes are treated as being
     * untyped: it is not an error to get the string value of a node with a complex type.
     * For atomic values, the method returns the result of casting the atomic value to a string.
     * <p>
     * If the calling code can handle any CharSequence, the method {@link #getStringValueCS} should
     * be used. If the caller requires a string, this method is preferred.
     *
     * @return the string value of the item
     * @see #getStringValueCS
     * @since 8.4
     */

    public String getStringValue();

    /**
     * Get the string value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String. The method satisfies the rule that
     * <code>X.getStringValueCS().toString()</code> returns a string that is equal to
     * <code>X.getStringValue()</code>.
     * <p>
     * Note that two CharSequence values of different types should not be compared using equals(), and
     * for the same reason they should not be used as a key in a hash table.
     * <p>
     * If the calling code can handle any CharSequence, this method should
     * be used. If the caller requires a string, the {@link #getStringValue} method is preferred.
     *
     * @return the string value of the item
     * @see #getStringValue
     * @since 8.4
     */

    public CharSequence getStringValueCS();

    /**
     * Get the typed value of the item.
     * <p>
     * For a node, this is the typed value as defined in the XPath 2.0 data model. Since a node
     * may have a list-valued data type, the typed value is in general a sequence, and it is returned
     * in the form of a SequenceIterator.
     * <p>
     * If the node has not been validated against a schema, the typed value
     * will be the same as the string value, either as an instance of xs:string or as an instance
     * of xs:untypedAtomic, depending on the node kind.
     * <p>
     * For an atomic value, this method returns an iterator over a singleton sequence containing
     * the atomic value itself.
     *
     * @return an iterator over the items in the typed value of the node or atomic value. The
     * items returned by this iterator will always be atomic values.
     * @throws XPathException where no typed value is available, for example in the case of
     *     an element with complex content
     * @since 8.4
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
