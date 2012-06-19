package org.orbeon.saxon.event;

import org.xml.sax.Locator;

import java.util.Stack;

/**
 * Implementation of Locator, used to supply location information to the ContentHandler.
 *
 * <p>When the ContentHandler is used to receive the results of a query or stylesheet,
 * the information supplied by the standard methods such as {@link #getSystemId} and
 * {@link #getLineNumber} relates to the position of the expression/instruction in the stylesheet
 * or query that caused the relevant nodes to be output.</p>
 *
 * <p>If the ContentHandler is used in other contexts, for example as the destination of an
 * IdentityTransformer, the information reflects the position in the source document.</p>
 *
 * <p>If the output property <code>saxon:supply-source-locator</code> was set to the
 * value "yes" (which in turn requires that tracing was enabled at compile time), the Locator will
 * also contain information about the current location in the source document (specifically, the position
 * of the context node). This will not always be 100% accurate, since there is some buffering of events
 * in the output pipeline: it reflects the context node at the time the event reaches the ContentHandler,
 * which may not be the same as the context node at the time the relevant instruction was executed;
 * however, it still provides some information that may be useful for diagnostics.</p>
 */

public class ContentHandlerProxyLocator implements Locator {

    private ContentHandlerProxy parent = null;

    /**
     * Create the Locator for a ContentHandlerProxy
     * @param parent the ContentHandlerProxy
     */

    public ContentHandlerProxyLocator(ContentHandlerProxy parent) {
        this.parent = parent;
    }

    // previously ContentHandlerProxy implemented Locator directly. However, this caused a clash
    // over the semantics of getSystemId(), which is inherited from both Receiver and Locator.
    // The getSystemId() method in the Receiver interface identifies the URI of the document represented
    // by the event stream. The getSystemId() method in this class typically returns the URI of the stylesheet
    // or query module that generated the event.

    /**
     * Get the Public ID
     * @return null (always)
     */

    public String getPublicId() {
        return null;
    }

    /**
     * Get the System ID
     * @return the system ID giving the location in the query or stylesheet of the most recent event notified
     */

    public String getSystemId() {
        final LocationProvider locationProvider = parent.getLocationProvider();
        if (locationProvider == null) {
            return null;
        } else {
            return locationProvider.getSystemId(parent.getCurrentLocationId());
        }
    }

    /**
     * Get the line number
     * @return the line number giving the location of the most recent event notified
     */

    public int getLineNumber() {
        final LocationProvider locationProvider = parent.getLocationProvider();
        if (locationProvider == null) {
            return -1;
        } else {
            return locationProvider.getLineNumber(parent.getCurrentLocationId());
        }
    }

    /**
     * Get the column number
     * @return -1 (always)
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the current item stack. This is a Stack, whose members are objects of class
     * {@link org.orbeon.saxon.om.Item}. The top item in the stack is the context node or atomic value; items
     * further down the stack represent previous context node or atomic value
     * @return the stack of context items
     */

    public Stack getContextItemStack() {
        final ContentHandlerProxy.ContentHandlerProxyTraceListener traceListener = parent.getTraceListener();
        if (traceListener == null) {
            return null;
        } else {
            return traceListener.getContextItemStack();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

