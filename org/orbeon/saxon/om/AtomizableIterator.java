package org.orbeon.saxon.om;


/**
 * A SequenceIterator is used to iterate over a sequence. An AtomizableIterator
 * is a SequenceIterator that can be asked to atomize any nodes encountered
 * in this sequence. It does not actually have to perform this atomization, it merely
 * has to accept the request. If atomization is requested, the iterator can atomize
 * some, all, or none of the nodes it encounters at its discretion: any that are
 * not atomized will be handled by the containing Atomizer.
 *
 * This mechanism provides an optimization, allowing atomization to occur at a lower
 * level of the system, which avoids the overheads of node creation in some tree
 * models.
 */

public interface AtomizableIterator extends SequenceIterator {

    /**
     * Indicate that any nodes returned in the sequence will be atomized. This
     * means that if it wishes to do so, the implementation can return the typed
     * values of the nodes rather than the nodes themselves. The implementation
     * is free to ignore this hint.
     * @param atomizing true if the caller of this iterator will atomize any
     * nodes that are returned, and is therefore willing to accept the typed
     * value of the nodes instead of the nodes themselves.
     */

    public void setIsAtomizing(boolean atomizing);

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
