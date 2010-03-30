package org.orbeon.saxon.evpull;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Orphan;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;

/**
 * The ComplexContentProcessor is an EventIterator that deals with the events occurring between
 * a startElement and endElement (or startDocument and endDocument) according to the XSLT/XQuery
 * rules for constructing complex content. This includes:
 *
 * <ul>
 * <li>Converting atomic values to text nodes (inserting space as a separator between adjacent nodes)</li>
 * <li>Replacing nested document nodes by their children</li>
 * <li>Merging adjacent text nodes and dropping zero-length text nodes</li>
 * <li>Detecting mispositioned or duplicated attribute and namespace nodes</li>
 *
 * </ul>
 *
 * <p>Note that if the content includes nodes such as element nodes, these will not be decomposed into
 * a sequence of tree events, they will simply be returned as nodes.</p>
 */
public class ComplexContentProcessor implements EventIterator {

    private Configuration config;
    private EventIterator base;
    private PullEvent[] startEventStack;  // contains either startElement or startDocument events
    private int depth;
    private NodeInfo pendingTextNode;
    private boolean pendingTextNodeIsMutable;
    private boolean prevAtomic = false;
    private PullEvent pendingOutput = null;

    /**
     * Create the ComplexContentProcessor
     * @param config the Saxon Configuration
     * @param base the EventIterator that delivers the content of the element or document node
     */

    public ComplexContentProcessor(Configuration config, EventIterator base) {
        this.config = config;
        this.base = EventStackIterator.flatten(base);
        startEventStack = new PullEvent[20];
        depth = 0;
    }

    /**
     * Get the next event in the sequence. This will never be an EventIterator.
     *
     * @return the next event, or null when the sequence is exhausted
     * @throws org.orbeon.saxon.trans.XPathException
     *          if a dynamic evaluation error occurs
     */

    public PullEvent next() throws XPathException {
        if (pendingOutput != null) {
            PullEvent next = pendingOutput;
            pendingOutput = null;
            return next;
        } else {
            return advance();
        }
    }

    private PullEvent advance() throws XPathException {
        while (true) {
            if (depth == 0) {
                PullEvent e = base.next();
                if (e instanceof StartElementEvent) {
                    push(e);
                } else if (e instanceof StartDocumentEvent) {
                    push(e);
                }
                return e;
            } else {
                PullEvent e = base.next();
                if (e instanceof StartElementEvent) {
                    prevAtomic = false;
                    push(e);
                    if (pendingTextNode != null) {
                        pendingOutput = e;
                        PullEvent next = pendingTextNode;
                        pendingTextNode = null;
                        return next;
                    } else {
                        return e;
                    }
                } else if (e instanceof StartDocumentEvent) {
                    prevAtomic = false;
                    push(e);
                    if (pendingTextNode != null) {
                        pendingOutput = e;
                        PullEvent next = pendingTextNode;
                        pendingTextNode = null;
                        return next;
                    } else {
                        //continue;
                    }
                } else if (e instanceof EndElementEvent) {
                    prevAtomic = false;
                    pop();
                    if (pendingTextNode != null) {
                        pendingOutput = e;
                        PullEvent next = pendingTextNode;
                        pendingTextNode = null;
                        return next;
                    } else {
                        return e;
                    }
                } else if (e instanceof EndDocumentEvent) {
                    prevAtomic = false;
                    pop();
                    if (pendingTextNode != null) {
                        pendingOutput = e;
                        PullEvent next = pendingTextNode;
                        pendingTextNode = null;
                        return next;
                    } else {
                        return e;
                    }
                } else if (e instanceof NodeInfo) {
                    prevAtomic = false;
                    switch (((NodeInfo)e).getNodeKind()) {
                        case Type.TEXT:
                            if (pendingTextNode == null) {
                                pendingTextNode = (NodeInfo)e;
                                pendingTextNodeIsMutable = false;
                            } else if (pendingTextNodeIsMutable) {
                                FastStringBuffer sb = (FastStringBuffer)((Orphan)pendingTextNode).getStringValueCS();
                                sb.append(((NodeInfo)e).getStringValueCS());
                            } else {
                                Orphan o = new Orphan(config);
                                o.setNodeKind(Type.TEXT);
                                FastStringBuffer sb = new FastStringBuffer(40);
                                sb.append(pendingTextNode.getStringValueCS());
                                sb.append(((NodeInfo)e).getStringValueCS());
                                o.setStringValue(sb);
                                pendingTextNode = o;
                                pendingTextNodeIsMutable = true;
                            }
                            continue;
                        default:
                            if (pendingTextNode != null) {
                                pendingOutput = e;
                                PullEvent next = pendingTextNode;
                                pendingTextNode = null;
                                return next;
                            } else {
                                return e;
                            }
                    }
                } else if (e instanceof AtomicValue) {
                    if (prevAtomic) {
                        FastStringBuffer sb = (FastStringBuffer)((Orphan)pendingTextNode).getStringValueCS();
                        sb.append(' ');
                        sb.append(((AtomicValue)e).getStringValueCS());
                    } else if (pendingTextNode != null) {
                        prevAtomic = true;
                        if (pendingTextNodeIsMutable) {
                            FastStringBuffer sb = (FastStringBuffer)((Orphan)pendingTextNode).getStringValueCS();
                            sb.append(((AtomicValue)e).getStringValueCS());
                        } else {
                            Orphan o = new Orphan(config);
                            o.setNodeKind(Type.TEXT);
                            FastStringBuffer sb = new FastStringBuffer(40);
                            sb.append(pendingTextNode.getStringValueCS());
                            sb.append(((AtomicValue)e).getStringValueCS());
                            o.setStringValue(sb);
                            pendingTextNode = o;
                            pendingTextNodeIsMutable = true;
                        }
                    } else {
                        prevAtomic = true;
                        Orphan o = new Orphan(config);
                        o.setNodeKind(Type.TEXT);
                        FastStringBuffer sb = new FastStringBuffer(40);
                        sb.append(((AtomicValue)e).getStringValueCS());
                        o.setStringValue(sb);
                        pendingTextNode = o;
                        pendingTextNodeIsMutable = true;
                    }
                    //continue;
                } else {
                    throw new AssertionError("Unknown event");
                }
            }

        }
    }

    /**
     * Push a startElement or startDocument event onto the stack. At the same time, if it is a startElement
     * event, remove any redundant namespace declarations
     * @param p the startElement or startDocument event
     */

    private void push(PullEvent p) {
        if (depth >= startEventStack.length - 1) {
            PullEvent[] b2 = new PullEvent[depth*2];
            System.arraycopy(startEventStack, 0, b2, 0, startEventStack.length);
            startEventStack = b2;
        }
        if (p instanceof StartElementEvent) {
            int retained = 0;
            int[] nsp = ((StartElementEvent)p).getLocalNamespaces();
            for (int nspi = 0; nspi < nsp.length; nspi++) {
                if (nsp[nspi] == -1) {
                    break;
                }
                retained++;
              outer:
                for (int i=depth-1; i>=0; i--) {
                    PullEvent q = startEventStack[i];
                    if (q instanceof StartElementEvent) {
                        int[] nsq = ((StartElementEvent)q).getLocalNamespaces();
                        for (int nsqi = 0; nsqi < nsq.length; nsqi++) {
                            if (nsp[nspi] == nsq[nsqi]) {
                                nsp[nspi] = -1;
                                retained--;
                                break outer;
                            } else if (nsp[nspi]>>16 == nsq[nsqi]>>16) {
                                break outer;
                            }
                        }
                    }
                }
            }
            if (retained < nsp.length) {
                int[] nsr = new int[retained];
                int nsri = 0;
                for (int nspi=0; nspi<nsp.length; nspi++) {
                    if (nsp[nspi] != -1) {
                        nsr[nsri++] = nsp[nspi];
                        if (nsri == retained) {
                            break;
                        }
                    }
                }
                ((StartElementEvent)p).setLocalNamespaces(nsr);
            }
        }
        startEventStack[depth++] = p;
        prevAtomic = false;
    }

    private void pop() {
        depth--;
        prevAtomic = false;
    }


    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return true;
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

