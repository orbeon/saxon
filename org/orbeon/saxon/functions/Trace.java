package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.InstructionDetails;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Value;

/**
* This class supports the XPath 2.0 function trace().
* The value is traced to the System.err stream, unless a TraceListener is in use,
* in which case the information is sent to the TraceListener
*/


public class Trace extends SystemFunction {

    NamespaceResolver resolver;
        // This is retained so that the static namespace context is available if required by the TraceListener


    /**
     * Simplify the function call. This implementation saves the static namespace context, in case it is
     * needed by the TraceListener.
     */

    public Expression simplify(StaticContext env) throws XPathException {
        resolver = env.getNamespaceResolver();
        return super.simplify(env);
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-significant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return argument[0].getSpecialProperties();
    }

    /**
    * Get the static cardinality
    */

    public int computeCardinality() {
        return argument[0].getCardinality();
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item val = argument[0].evaluateItem(context);
        String label = argument[1].evaluateAsString(context);
        if (context.getController().isTracing()) {
            notifyListener(label, Value.asValue(val), context);
        } else {
            traceItem(val, label);
        }
        return val;
    }

    private void notifyListener(String label, Value val, XPathContext context) {
        InstructionDetails info = (InstructionDetails)getInstructionInfo();
        info.setConstructType(Location.TRACE_CALL);
        info.setNamespaceResolver(resolver);
        info.setProperty("label", label);
        info.setProperty("value", val);
        TraceListener listener = context.getController().getTraceListener();
        listener.enter(info, context);
        listener.leave(info);
    }

    private void traceItem(Item val, String label) throws XPathException {
        if (val==null) {
            System.err.println(label + ": empty sequence");
        } else {
            if (val instanceof NodeInfo) {
                System.err.println(label + ": " + Type.displayTypeName(val) + ": "
                                    + Navigator.getPath((NodeInfo)val));
            } else {
                System.err.println(label + ": " + Type.displayTypeName(val) + ": "
                                    + val.getStringValue());
            }
        }
    }

    /**
    * Iterate over the results of the function
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (context.getController().isTracing()) {
            String label = argument[1].evaluateAsString(context);
            Value value = ExpressionTool.eagerEvaluate(argument[0], context);
            notifyListener(label, value, context);
            return value.iterate(context);
        } else {
            return new TracingIterator(argument[0].iterate(context), argument[1].evaluateAsString(context));
        }
    }


    /**
    * Tracing Iterator class
    */

    public class TracingIterator implements SequenceIterator {

        SequenceIterator base;
        String label;
        boolean empty = true;


        public TracingIterator(SequenceIterator base, String label) throws XPathException {
            this.base = base;
            this.label = label;
        }

        public Item next() throws XPathException {
            Item n = base.next();
            if (n==null) {
                if (empty) {
                    traceItem(null, label);
                }
            } else {
                traceItem(n, label + " [" + position() + "]");
                empty = false;
            }
            return n;
        }

        public Item current() {
            return base.current();
        }

        public int position() {
            return base.position();
        }

        public SequenceIterator getAnother() throws XPathException {
            return new TracingIterator(base.getAnother(), label);
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
