package net.sf.saxon.trace;

import net.sf.saxon.Version;
import net.sf.saxon.value.Value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * This is the standard trace listener used when the -T option is specified on the command line.
 * There are two variants, represented by subclasses: one for XSLT, and one for XQuery. The two variants
 * differ in that they present the trace output in terms of constructs used in the relevant host language.
 */

public abstract class AbstractTraceListener implements TraceListener {
    private int indent = 0;
    private NamePool pool;
    private PrintStream out = System.err;
    private static StringBuffer spaceBuffer = new StringBuffer("                ");

    /**
     * Called at start
     */

    public void open() {
        out.println("<trace " +
                "saxon-version=\"" + Version.getProductVersion()+ "\" " +
                getOpeningAttributes() + '>');
        indent++;
    }

    protected abstract String getOpeningAttributes();

    /**
     * Called at end
     */

    public void close() {
        indent--;
        out.println("</trace>");
    }

    /**
     * Called when an instruction in the stylesheet gets processed
     */

    public void enter(InstructionInfo info, XPathContext context) {
        int infotype = info.getConstructType();
        int objectNameCode = info.getObjectNameCode();
        String tag = tag(infotype);
        if (tag==null) {
            // this TraceListener ignores some events to reduce the volume of output
            return;
        }
        String file = AbstractTraceListener.truncateURI(info.getSystemId());
        pool = context.getController().getNamePool();
        String msg = AbstractTraceListener.spaces(indent) + '<' + tag;
        String name = (String)info.getProperty("name");
        if (name!=null) {
            msg += " name=\"" + escape(name) + '"';
        } else if (objectNameCode != -1) {
            msg += " name=\"" + escape(pool.getDisplayName(objectNameCode)) + '"';
        }
        Iterator props = info.getProperties();
        while (props.hasNext()) {
            String prop = (String)props.next();
            Object val = info.getProperty(prop);
            if (prop.startsWith("{")) {
                // It's a QName in Clark notation: we'll strip off the namespace
                int rcurly = prop.indexOf('}');
                if (rcurly > 0) {
                    prop = prop.substring(rcurly+1);
                }
            }
            if (val != null && !prop.equals("name") && !prop.equals("expression")) {
                msg += ' ' + prop + "=\"" + escape(val.toString()) + '"';
            }
        }

        msg += " line=\"" + info.getLineNumber() + '"';

        int col = info.getColumnNumber();
        if (col >= 0) {
            msg += " column=\"" + info.getColumnNumber() + '"';
        }

        msg += " module=\"" + escape(file) + "\">";
        out.println(msg);
        indent++;
    }

    /**
     * Escape a string for XML output (in an attribute delimited by double quotes).
     * This method also collapses whitespace (since the value may be an XPath expression that
     * was originally written over several lines).
     */

    public String escape(String in) {
        if (in==null) {
            return "";
        }
        CharSequence collapsed = Value.collapseWhitespace(in);
        StringBuffer sb = new StringBuffer(collapsed.length() + 10);
        for (int i=0; i<collapsed.length(); i++) {
            char c = collapsed.charAt(i);
            if (c=='<') {
                sb.append("&lt;");
            } else if (c=='>') {
                sb.append("&gt;");
            } else if (c=='&') {
                sb.append("&amp;");
            } else if (c=='\"') {
                sb.append("&#34;");
            } else if (c=='\n') {
                sb.append("&#xA;");
            } else if (c=='\r') {
                sb.append("&#xD;");
            } else if (c=='\t') {
                sb.append("&#x9;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Called after an instruction of the stylesheet got processed
     */

    public void leave(InstructionInfo info) {
        int infotype = info.getConstructType();
        String tag = tag(infotype);
        if (tag==null) {
            // this TraceListener ignores some events to reduce the volume of output
            return;
        }
        indent--;
        out.println(AbstractTraceListener.spaces(indent) + "</" + tag + '>');
    }

    protected abstract String tag(int construct);

    /**
    * Called when an item becomes the context item
    */

   public void startCurrentItem(Item item) {
       if (item instanceof NodeInfo) {
           NodeInfo curr = (NodeInfo) item;
           out.println(AbstractTraceListener.spaces(indent) + "<source node=\"" + Navigator.getPath(curr)
                   + "\" line=\"" + curr.getLineNumber()
                   + "\" file=\"" + AbstractTraceListener.truncateURI(curr.getSystemId())
                   + "\">");
       }
       indent++;
   }

    /**
     * Called after a node of the source tree got processed
     */

    public void endCurrentItem(Item item) {
        indent--;
        if (item instanceof NodeInfo) {
            NodeInfo curr = (NodeInfo) item;
            out.println(AbstractTraceListener.spaces(indent) + "</source><!-- " +
                    Navigator.getPath(curr) + " -->");
        }
    }

    /**
     * Truncate a URI to its last component
     */

    private static String truncateURI(String uri) {
        String file = uri;
        if (file == null) file = "";
        while (true) {
            int i = file.indexOf('/');
            if (i >= 0 && i < file.length() - 6) {
                file = file.substring(i + 1);
            } else {
                break;
            }
        }
        return file;
    }

    /**
     * Get n spaces
     */

    private static String spaces(int n) {
        while (spaceBuffer.length() < n) {
            spaceBuffer.append(AbstractTraceListener.spaceBuffer);
        }
        return AbstractTraceListener.spaceBuffer.substring(0, n);
    }

    /**
     * Set the output destination (default is System.err)
     * @param stream the output destination for tracing output
     */

    public void setOutputDestination(PrintStream stream) {
        out = stream;
    }

    /**
     * Get the output destination
     */

    public PrintStream getOutputDestination() {
        return out;
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
// Contributor(s): none
//