package org.orbeon.saxon.s9api;

import org.orbeon.saxon.value.EmptySequence;

/**
 * The class <tt>XdmEmptySequence</tt> represents an empty sequence in the XDM Data Model.
 *
 * <p>This is a singleton class: there is only one instance, which may be obtained
 * using the {@link #getInstance} method.</p>
 *
 * <p>An empty sequence may also be represented by an {@link XdmValue} whose length happens to be zero.
 * Applications should therefore not test to see whether an object is an instance of this class
 * in order to decide whether it is empty.</p>
 *
 * <p>Note: in interfaces that expect an {@link XdmItem}, an empty sequence is represented by a
 * Java null value.</p>
 */

public class XdmEmptySequence extends XdmValue {

    private static XdmEmptySequence THE_INSTANCE = new XdmEmptySequence();

    /**
     * Return the singleton instance of this class
     * @return an XdmValue representing an empty sequence
     */

    public static XdmEmptySequence getInstance() {
        return THE_INSTANCE;
    }

    private XdmEmptySequence() {
        super(EmptySequence.getInstance());
    }

    /**
     * Get the number of items in the sequence
     * @return the number of items in the value - always zero
     */

    @Override
    public int size() {
        return 0;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

