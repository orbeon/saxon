package org.orbeon.saxon.tinytree;

import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.evpull.*;
import org.orbeon.saxon.om.NamespaceDeclarationsImpl;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
 * This implementation of the Saxon event-pull interface starts from a document, element,
 * text, comment, or processing-instruction node in a TinyTree,
 * and returns the events corresponding to that node and its descendants (including
 * their attributes and namespaces). The class performs the same function as
 * the general-purpose {@link org.orbeon.saxon.evpull.Decomposer} class, but is
 * specialized to exploit the TinyTree data structure: in particular, it never
 * materializes any Node objects.
 */

public class TinyTreeEventIterator implements EventIterator, LocationProvider {

    private int startNodeNr;
    private int currentNodeNr;
    //private int currentEvent;
    private int pendingEndEvents = 0;
    private boolean startAtDocument = false;
    private TinyTree tree;
    private PipelineConfiguration pipe;
    private int[] nsBuffer = new int[10];

    /**
     * Create a TinyTreeEventIterator to return events associated with a tree or subtree
     * @param startNode the root of the tree or subtree. Must be a document or element node.
     * @param pipe the Saxon pipeline configuration
     * @throws IllegalArgumentException if the start node is an attribute or namespace node.
     */

    public TinyTreeEventIterator(TinyNodeImpl startNode, PipelineConfiguration pipe) {
        this.pipe = pipe;
        this.pipe.setLocationProvider(this);
        int kind = startNode.getNodeKind();
        if (kind != Type.DOCUMENT && kind != Type.ELEMENT) {
            throw new IllegalArgumentException("TinyTreeEventIterator must start at a document or element node");
        }
        startNodeNr = startNode.nodeNr;
        currentNodeNr = startNodeNr;
        tree = startNode.tree;
        pendingEndEvents = 0;
        startAtDocument = (kind == Type.DOCUMENT);
        NamespaceDeclarationsImpl nsDeclarations = new NamespaceDeclarationsImpl();
        nsDeclarations.setNamePool(startNode.getNamePool());
    }

    /**
     * Set configuration information. This must only be called before any events
     * have been read.
     * @param pipe the pipeline configuration
     */

//    public void setPipelineConfiguration(PipelineConfiguration pipe) {
//        this.pipe = pipe;
//    }

    /**
     * Get configuration information.
     * @return the pipeline configuration
     */

//    public PipelineConfiguration getPipelineConfiguration() {
//        return pipe;
//    }

    /**
     * Get the next event
     * @return a PullEvent object representing the next event, or null when the sequence is exhausted
     */

    public PullEvent next() throws XPathException {

        if (startNodeNr < 0) {
            // this is a signal that we've finished
            return null;
        }

        int thisDepth = tree.depth[currentNodeNr];
        boolean lastNode = currentNodeNr + 1 >= tree.numberOfNodes;
        int nextDepth = (lastNode ? 0 : tree.depth[currentNodeNr+1]);

        boolean atEnd = (thisDepth <= tree.depth[startNodeNr] && currentNodeNr != startNodeNr);

        if (atEnd && pendingEndEvents == 1) {
            pendingEndEvents--;
            startNodeNr = -1;
            if (startAtDocument) {
                return EndDocumentEvent.getInstance();
            } else {
                return EndElementEvent.getInstance();
            }
        }

        if (pendingEndEvents > 0) {
            pendingEndEvents--;
            return EndElementEvent.getInstance();
        }
        
        byte kind = tree.nodeKind[currentNodeNr];
        switch (kind) {
            case Type.DOCUMENT:
                pendingEndEvents = thisDepth - nextDepth + 1;
                currentNodeNr++;
                return StartDocumentEvent.getInstance();
            case Type.ELEMENT:
                pendingEndEvents = thisDepth - nextDepth + 1;
                StartElementEvent see = new StartElementEvent(pipe);
                see.setNameCode(tree.nameCode[currentNodeNr]);
                see.setTypeCode(tree.getTypeAnnotation(currentNodeNr));
                see.setLocationId(currentNodeNr);
                // add the attributes
                int index = tree.alpha[currentNodeNr];
                if (index >= 0) {
                    while (index < tree.numberOfAttributes && tree.attParent[index] == currentNodeNr) {
                        see.addAttribute(tree.getAttributeNode(index++));
                    }
                }
                if (currentNodeNr == startNodeNr) {
                    // get all inscope namespaces for a top-level element in the sequence.
                    see.setLocalNamespaces(TinyElementImpl.getInScopeNamespaces(tree, currentNodeNr, nsBuffer));
                } else {
                    // only namespace declarations (and undeclarations) on this element are required
                    see.setLocalNamespaces(TinyElementImpl.getDeclaredNamespaces(tree, currentNodeNr, nsBuffer));
                }
                currentNodeNr++;
                return see;

            case Type.TEXT:
            case Type.WHITESPACE_TEXT:
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                pendingEndEvents = thisDepth - nextDepth;
                return tree.getNode(currentNodeNr++);
            case Type.PARENT_POINTER:
                currentNodeNr++;
                return next();
            default:
                throw new IllegalStateException("Unknown node kind " + tree.nodeKind[currentNodeNr]);
        }

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

    /**
     * Get location information: the system Id of the current start element event
     * @param locationId in this case, the node number in the tiny tree
     * @return the system Id of the node: that is its base URI, before taking xml:base into account
     */

    public String getSystemId(long locationId) {
        return tree.getSystemId((int)locationId);
    }

    /**
     * Get location information: the line number of the current start element event
     * @param locationId in this case, the node number in the tiny tree
     * @return the line number of the node if known, or -1 otherwise
     */

    public int getLineNumber(long locationId) {
        return tree.getLineNumber((int)locationId);
    }

    /**
     * Get location information: the column number of the current start element event
     * @param locationId in this case, the node number in the tiny tree
     * @return the column number of the node if known, or -1 otherwise
     */

    public int getColumnNumber(long locationId) {
        return tree.getColumnNumber((int)locationId);
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

