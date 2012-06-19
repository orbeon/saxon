package org.orbeon.saxon.event;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.Controller;

import java.util.ArrayList;


/**
 * This outputter is used when writing a sequence of atomic values and nodes, that
 * is, when xsl:variable is used with content and an "as" attribute. The outputter
 * builds the sequence and provides access to it. (It isn't really an outputter at all,
 * it doesn't pass the events to anyone, it merely constructs the sequence in memory
 * and provides access to it). Note that the event sequence can include calls such as
 * startElement and endElement that require trees to be built. If nodes such as attributes
 * and text nodes are received while an element is being constructed, the nodes are added
 * to the tree. Otherwise, "orphan" nodes (nodes with no parent) are created and added
 * directly to the sequence.
 *
 * <p>This class is not used to build temporary trees. For that, the ComplexContentOutputter
 * is used.</p>
 *
 *
 * @author Michael H. Kay
 */

public final class SequenceOutputter extends SequenceWriter {

    private ArrayList list;
    private Controller controller;  // enables the SequenceOutputter to be reused


    /**
    * Create a new SequenceOutputter
    */

	public SequenceOutputter() {
	    this.list = new ArrayList(50);
	}

	public SequenceOutputter(Controller controller, int estimatedSize) {
	    this.list = new ArrayList(estimatedSize);
        this.controller = controller;
	}

	public SequenceOutputter(Controller controller) {
	    this.list = new ArrayList(50);
        this.controller = controller;
	}

    /**
     * Clear the contents of the SequenceOutputter and make it available for reuse
     */

    public void reset() {
        list = new ArrayList(Math.max(list.size()+10, 50));
        if (controller != null && adviseReuse()) {
            controller.reuseSequenceOutputter(this);
        }
    }

    /**
     * Abstract method to be supplied by subclasses: output one item in the sequence.
     */

    public void write(Item item) {
        list.add(item);
    }

    /**
    * Get the sequence that has been built
    */

    public ValueRepresentation getSequence() {
        switch (list.size()) {
            case 0:
                return EmptySequence.getInstance();
            case 1:
                return (Item)list.get(0);
            default:
                return new SequenceExtent(list);
        }
    }

    /**
     * Get an iterator over the sequence of items that has been constructed
     */

    public SequenceIterator iterate() {
        if (list.isEmpty()) {
            return EmptyIterator.getInstance();
        } else {
            return new ListIterator(list);
        }
    }

    /**
     * Get the list containing the sequence of items
     */

    public ArrayList getList() {
        return list;
    }

    /**
     * Get the first item in the sequence that has been built
     */

    public Item getFirstItem() {
        if (list.isEmpty()) {
            return null;
        } else {
            return (Item)list.get(0);
        }
    }

    /**
     * Get the last item in the sequence that has been built, and remove it
     */

    public Item popLastItem() {
        if (list.isEmpty()) {
            return null;
        } else {
            return (Item)list.remove(list.size()-1);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
