package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;

/**
* A SequenceIntent is sequence value that uses deferred evaluation. It
* can be used as a value, but it is actually a wrapper around an iterator and
* a context, so the items in the sequence are not evaluated until they are required.
*/

// TODO: The last remaining usage is in ExtensionFunctionCall.java - this allows a Java extension
// function to return a SequenceIterator which is properly pipelined. Some of the
// Saxon and EXSLT extension functions exploit this mechanism. Perhaps they should be
// changed to return a Closure instead.

public class SequenceIntent extends Value {

    private SequenceIterator iterator;
    private SequenceExtent extent = null;
    private int useCount = 0;

    /**
    * Construct a SequenceIntent by supplying an iterator. Note that the caller must
    * ensure this is a "clean" iterator: it must be positioned at the start, and must
    * not be shared by anyone else. It is also required (at present) that the iterator
    * should return items in the correct sequence.
    * @param iterator the iterator that delivers the items in the sequence. It must deliver
    * them in the correct order, with no duplicates.
    */

    public SequenceIntent(SequenceIterator iterator) {
        this.iterator = iterator;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD;
    }

    /**
    * Get the item type
    */

    public ItemType getItemType() {
        return AnyItemType.getInstance();
    }

    /**
    * Evaluate the expression in a given context to return a sequence
    * @param context the evaluation context.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (extent!=null) {
            return extent.iterate(null);
        }
        // The third time a SequenceIntent is used, expand it to a SequenceExtent.
        // This is a space/time trade-off.
        if (useCount++ >= 2) {
            extent = new SequenceExtent(iterator.getAnother());
            return extent.iterate(null);
        }
        return iterator.getAnother();
    }

    /**
    * Get the n'th item in the sequence (starting from 0). This is defined for all
    * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
    */

    public Item itemAt(int n) throws XPathException {
        if (extent!=null) {
            return extent.itemAt(n);
        } else {
            return super.itemAt(n);
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
