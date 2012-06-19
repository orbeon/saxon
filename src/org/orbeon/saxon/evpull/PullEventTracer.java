package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;

import javax.xml.transform.stream.StreamSource;
import java.io.PrintStream;
import java.io.File;

/**
 * Diagnostic class to display the sequence of events reported by an EventIterator
 */
public class PullEventTracer implements EventIterator {

    private EventIterator base;
    private String label = ("PET" + hashCode()).substring(0, 8) + ": ";
    private PrintStream out = System.err;
    //@SuppressWarnings({"FieldCanBeLocal"})
    private NamePool pool;

    /**
     * Create a tracer for pull events
     * @param base the event iterator whose events are to be traced
     * @param config the Saxon configuration
     */

    public PullEventTracer(EventIterator base, Configuration config) {
        this.base = base;
        pool = config.getNamePool();
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
        if (pe == null) {
            return null;
        }
        if (pe instanceof StartDocumentEvent) {
            out.println(label + "StartDocument");
            label = "  " + label;
        } else if (pe instanceof StartElementEvent) {
            out.println(label + "StartElement " + pool.getDisplayName(((StartElementEvent)pe).getNameCode()));
            label = "  " + label;
        } else if (pe instanceof EndDocumentEvent) {
            label = label.substring(2);
            out.println(label + "EndDocument");
        } else if (pe instanceof EndElementEvent) {
            label = label.substring(2);
            out.println(label + "EndElement");
        } else if (pe instanceof NodeInfo) {
            FastStringBuffer fsb = new FastStringBuffer(80);
            fsb.append(label);
            int kind = ((NodeInfo)pe).getNodeKind();
            fsb.append(NodeKindTest.toString(kind));
            if (kind == Type.ELEMENT || kind == Type.ATTRIBUTE) {
                fsb.append(' ');
                fsb.append(((NodeInfo)pe).getDisplayName());
            }
            fsb.append(" \"");
            fsb.append(((NodeInfo)pe).getStringValueCS());
            fsb.append('"');
            out.println(fsb.toString());
        } else if (pe instanceof AtomicValue) {
            out.println(label + Type.displayTypeName((AtomicValue)pe) + ' ' + pe);
        } else if (pe instanceof EventIterator) {
            out.println(label + "** NESTED ITERATOR **");
        } else {
            out.println(label + pe.getClass().getName());
        }

        return pe;
    }


    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return base.isFlatSequence();
    }

    /**
     * Main method for testing only
     * @param args not used
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration();
        DocumentInfo doc = config.buildDocument(new StreamSource(new File("c:/MyJava/samples/data/books.xml")));
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        pipe.setHostLanguage(Configuration.XQUERY);
        EventIterator e = new Decomposer(new SingletonEventIterator(doc), pipe);
        e = EventStackIterator.flatten(e);
        e = new PullEventTracer(e, config);
        while (true) {
            PullEvent pe = e.next();
            if (pe == null) {
                break;
            }
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

