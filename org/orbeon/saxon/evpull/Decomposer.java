package org.orbeon.saxon.evpull;

import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.tinytree.TinyNodeImpl;
import org.orbeon.saxon.tinytree.TinyTreeEventIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
 * This class takes a sequence of pull events and turns it into fully-decomposed form, that is, it
 * takes and document and element nodes in the sequence and turns them into a subsequence consisting of a
 * start element|document event, a content sequence, and an end element|document event, recursively.
 *
 * <p>The resulting sequence is decomposed, but not flat (it will contain nested EventIterators). To flatten
 * it, use {@link EventStackIterator#flatten(EventIterator)} </p>
 */
public class Decomposer implements EventIterator {

    private EventIterator base;
    private PipelineConfiguration pipe;

    /**
     * Create a Decomposer, which turns an event sequence into fully decomposed form
     * @param base the base sequence, which may be fully composed, fully decomposed, or
     * anything in between
     * @param pipe the Saxon pipeline configuration
     */

    public Decomposer(EventIterator base, PipelineConfiguration pipe) {
        this.pipe = pipe;
        this.base = EventStackIterator.flatten(base);
    }

    /**
     * Create a Decomposer which returns the sequence of events corresponding to
     * a particular node
     * @param node the node to be decomposed
     * @param pipe the Saxon pipeline configuration
     */

    public Decomposer(NodeInfo node, PipelineConfiguration pipe) {
        this.pipe = pipe;
        base = new SingletonEventIterator(node);
    }

    /**
     * Get the next event in the sequence
     *
     * @return the next event, or null when the sequence is exhausted. Note that since an EventIterator is
     *         itself a PullEvent, this method may return a nested iterator.
     * @throws org.orbeon.saxon.trans.XPathException
     *          if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {
        PullEvent pe = base.next();
        if (pe instanceof NodeInfo) {
            NodeInfo node = (NodeInfo)pe;
            switch (node.getNodeKind()) {
                case Type.DOCUMENT: {
                    if (node instanceof TinyNodeImpl) {
                        return new TinyTreeEventIterator(((TinyNodeImpl)node), pipe);
                    } else {
                        SequenceIterator content = node.iterateAxis(Axis.CHILD);
                        EventIterator contentEvents = new EventIteratorOverSequence(content);
                        return new BracketedDocumentIterator(
                                new Decomposer(contentEvents, pipe));
                    }
                }
                case Type.ELEMENT: {
                    if (node instanceof TinyNodeImpl) {
                        return new TinyTreeEventIterator(((TinyNodeImpl)node), pipe);
                    } else {
                        SequenceIterator content = node.iterateAxis(Axis.CHILD);
                        EventIterator contentEvents = new EventIteratorOverSequence(content);
                        StartElementEvent see = new StartElementEvent(pipe);
                        see.setNameCode(node.getNameCode());
                        see.setTypeCode(node.getTypeAnnotation());
                        see.setLocalNamespaces(node.getDeclaredNamespaces(null));
                        AxisIterator atts = node.iterateAxis(Axis.ATTRIBUTE);
                        while (true) {
                            NodeInfo att = (NodeInfo)atts.next();
                            if (att == null) {
                                break;
                            }
                            see.addAttribute(att);
                        }
                        return new BracketedElementIterator(
                                see,
                                new Decomposer(contentEvents, pipe),
                                EndElementEvent.getInstance());
                    }
                }
                default:
                    return node;
            }
        } else {
            return pe;
        }
    }

    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return false;
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

