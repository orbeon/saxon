package net.sf.saxon;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ValidationException;
import org.xml.sax.SAXException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMLocator;
import java.io.PrintStream;

/**
  * <B>StandardErrorListener</B> is the standard error handler for XSLT processing
  * errors, used if no other ErrorListener is nominated.
  * @author Michael H. Kay
  */

public class StandardErrorListener implements ErrorListener {

    private int recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
    private int warningCount = 0;
    protected transient PrintStream errorOutput = System.err;

    public StandardErrorListener() {}

    /**
     * Make a clean copy of this ErrorListener. This is necessary because the
     * standard error listener is stateful (it remembers how many errors there have been)
     */

    public StandardErrorListener makeAnother() {
        return new StandardErrorListener();
    }

    // Note, when the standard error listener is used, a new
    // one is created for each transformation, because it holds
    // the recovery policy and the warning count.

    /**
    * Set output destination for error messages (default is System.err)
    * @param writer The PrintStream to use for error messages
    */

    public void setErrorOutput(PrintStream writer) {
        errorOutput = writer;
    }

    /**
     * Get the error output stream
     */

    public PrintStream getErrorOutput() {
        return errorOutput;
    }

    /**
    * Set the recovery policy
    */

    public void setRecoveryPolicy(int policy) {
        recoveryPolicy = policy;
    }

    /**
     * Receive notification of a warning.
     *
     * <p>Transformers can use this method to report conditions that
     * are not errors or fatal errors.  The default behaviour is to
     * take no action.</p>
     *
     * <p>After invoking this method, the Transformer must continue with
     * the transformation. It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  transformer exception.
     *
     * @throws javax.xml.transform.TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see javax.xml.transform.TransformerException
     */

    public void warning(TransformerException exception)
        throws TransformerException {

        if (recoveryPolicy==Configuration.RECOVER_SILENTLY) {
            // do nothing
            return;
        }

        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }
        String message = "";
        if (exception.getLocator()!=null) {
            message = getLocationMessage(exception) + "\n  ";
        }
        message += wordWrap(getExpandedMessage(exception));

        if (exception instanceof ValidationException) {
            errorOutput.println("Validation error " + message);

        } else {
            errorOutput.println("Warning: " + message);
            warningCount++;
            if (warningCount > 25) {
                errorOutput.println("No more warnings will be displayed");
                recoveryPolicy = Configuration.RECOVER_SILENTLY;
                warningCount = 0;
            }
        } 
    }

    /**
     * Receive notification of a recoverable error.
     *
     * <p>The transformer must continue to provide normal parsing events
     * after invoking this method.  It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * <p>The action of the standard error listener depends on the
     * recovery policy that has been set, which may be one of RECOVER_SILENTLY,
     * RECOVER_WITH_WARNING, or DO_NOT_RECOVER
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     *
     * @throws TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see TransformerException
     */

    public void error(TransformerException exception) throws TransformerException {
        if (recoveryPolicy==Configuration.RECOVER_SILENTLY) {
            // do nothing
            return;
        }
        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }        
        String message = getLocationMessage(exception) +
                         "\n  " +
                         wordWrap(getExpandedMessage(exception));

        if (exception instanceof ValidationException) {
            errorOutput.println("Validation error " + message);

        } else if (recoveryPolicy==Configuration.RECOVER_WITH_WARNINGS) {
            errorOutput.println("Recoverable error " + message);
            warningCount++;
            if (warningCount > 25) {
                errorOutput.println("No more warnings will be displayed");
                recoveryPolicy = Configuration.RECOVER_SILENTLY;
                warningCount = 0;
            }
        } else {
            errorOutput.println("Recoverable error " + message);
            errorOutput.println("Processing terminated because error recovery is disabled");
            throw new DynamicError(exception);
        }
    }

    /**
     * Receive notification of a non-recoverable error.
     *
     * <p>The application must assume that the transformation cannot
     * continue after the Transformer has invoked this method,
     * and should continue (if at all) only to collect
     * addition error messages. In fact, Transformers are free
     * to stop reporting events once this method has been invoked.</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     *
     * @throws TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see TransformerException
     */

    public void fatalError(TransformerException exception) throws TransformerException {
        if (exception instanceof XPathException && ((XPathException)exception).hasBeenReported()) {
            // don't report the same error twice
            return;
        }
        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }
        String message = (exception instanceof ValidationException ?
                            "Validation error " :
                            "Error ") +
                         getLocationMessage(exception) +
                         "\n  " +
                         wordWrap(getExpandedMessage(exception));
        errorOutput.println(message);
        if (exception instanceof XPathException) {
            ((XPathException)exception).setHasBeenReported();
        }
    }

    /**
    * Get a string identifying the location of an error.
    */

    public static String getLocationMessage(TransformerException err) {
        SourceLocator loc = err.getLocator();
        while (loc == null) {
            if (err.getException() instanceof TransformerException) {
                err = (TransformerException)err.getException();
                loc = err.getLocator();
            } else if (err.getCause() instanceof TransformerException) {
                err = (TransformerException)err.getCause();
                loc = err.getLocator();
            } else {
                return "";
            }
        }
        XPathContext context = null;
        if (err instanceof DynamicError) {
            context = ((DynamicError)err).getXPathContext();
        }
        return getLocationMessage(loc, context);
    }

    private static String getLocationMessage(SourceLocator loc, XPathContext context) {
        String locmessage = "";
        String systemId = null;
        int lineNumber = -1;
        if (loc instanceof DOMLocator) {
            locmessage += "at " + ((DOMLocator)loc).getOriginatingNode().getNodeName() + ' ';
        } else if (loc instanceof NodeInfo) {
            locmessage += "at " + ((NodeInfo)loc).getDisplayName() + ' ';
        } else if (loc instanceof InstructionInfoProvider) {
            String instructionName = getInstructionName(((InstructionInfoProvider)loc), context);
            if (!"".equals(instructionName)) {
                locmessage += "at " + instructionName + ' ';
            }
            systemId = ((InstructionInfoProvider)loc).getInstructionInfo().getSystemId();
            lineNumber = ((InstructionInfoProvider)loc).getInstructionInfo().getLineNumber();
        }
        if (lineNumber == -1) {
            lineNumber = loc.getLineNumber();
        }
        if (lineNumber != -1) {
            locmessage += "on line " + lineNumber + ' ';
        }
        if (loc.getColumnNumber() != -1) {
            locmessage += "column " + loc.getColumnNumber() + ' ';
        }
        if (systemId == null) {
            systemId = loc.getSystemId();
        }
        if (systemId != null) {
            locmessage += "of " + systemId + ':';
        }
        return locmessage;
    }

    /**
    * Get a string containing the message for this exception and all contained exceptions
    */

    public static String getExpandedMessage(TransformerException err) {
        String message = "";

        if (err instanceof XPathException) {
            String code = ((XPathException)err).getErrorCodeLocalPart();
            if (code != null) {
                message = code;
            }
        }

        Throwable e = err;
        while (true) {
            if (e == null) {
                break;
            }
            String next = e.getMessage();
            if (next==null) next="";
            if (next.startsWith("net.sf.saxon.trans.StaticError: ")) {
                next = next.substring(next.indexOf(": ") + 2);
            }
            if (!("TRaX Transform Exception".equals(next) || message.endsWith(next))) {
                if (!"".equals(message)) {
                    message += ": ";
                }
                message += next;
            }
            if (e instanceof TransformerException) {
                e = ((TransformerException)e).getException();
            } else if (e instanceof SAXException) {
                e = ((SAXException)e).getException();
            } else {
                // e.printStackTrace();
                break;
            }
        }

        return message;
    }

    /**
     * Extract a name identifying the instruction at which an error occurred
     */

    private static String getInstructionName(InstructionInfoProvider inst, XPathContext context) {
        // TODO: subclass this for XSLT and XQuery
        if (context==null) {
            return "";
        }
        try {
            InstructionInfo info = inst.getInstructionInfo();
            int construct = info.getConstructType();
            if (construct < 1024 &&
                    construct != StandardNames.XSL_FUNCTION &&
                    construct != StandardNames.XSL_TEMPLATE) {
                // it's a standard name
                if (context.getController().getConfiguration().getHostLanguage() == Configuration.XSLT) {
                    return StandardNames.getDisplayName(construct);
                } else {
                    String s = StandardNames.getDisplayName(construct);
                    int colon = s.indexOf(':');
                    if (colon > 0) {
                        String local = s.substring(colon+1);
                        if (local.equals("document")) {
                            return "document node constructor";
                        } else if (local.equals("text") || s.equals("value-of")) {
                            return "text node constructor";
                        } else if (local.equals("element")) {
                            return "computed element constructor";
                        } else if (local.equals("attribute")) {
                            return "computed attribute constructor";
                        }
                    }
                    return s;
                }
            }
            switch (construct) {
                case Location.LITERAL_RESULT_ELEMENT: {
                    int fp = info.getObjectNameCode();
                    String name = "element constructor";
                    if (context != null) {
                        name += " <" + context.getController().getNamePool().getDisplayName(fp) + '>';
                    }
                    return name;
                }
                case Location.LITERAL_RESULT_ATTRIBUTE: {
                    int fp = info.getObjectNameCode();
                    String name = "attribute constructor";
                    if (context != null) {
                        name += ' ' + context.getController().getNamePool().getDisplayName(fp) + "=\"{...}\"";
                    }
                    return name;
                }
                case StandardNames.XSL_FUNCTION: {
                    int fp = info.getObjectNameCode();
                    String name = "function";
                    if (context != null) {
                        name += ' ' + context.getController().getNamePool().getDisplayName(fp) + "()";
                    }
                    return name;
                }
                case StandardNames.XSL_TEMPLATE: {
                    int fp = info.getObjectNameCode();
                    String name = "template";
                    if (context != null && fp != -1) {
                        name += " name=\"" + context.getController().getNamePool().getDisplayName(fp) + '\"';
                    }
                    return name;
                }
                default:
                    return "";
            }

        } catch (Exception err) {
            return "";
        }
    }

    /**
     * Wordwrap an error message into lines of 72 characters or less (if possible)
     */

    private static String wordWrap(String message) {
        int nl = message.indexOf('\n');
        if (nl < 0) {
            nl = message.length();
        }
        if (nl > 100) {
            int i = 90;
            while (message.charAt(i) != ' ' && i>0) {
                i--;
            }
            if (i>10) {
                return message.substring(0, i) + "\n  " + wordWrap(message.substring(i+1));
            } else {
                return message;
            }
        } else if (nl < message.length()) {
            return message.substring(0, nl) + '\n' + wordWrap(message.substring(nl+1));
        } else {
            return message;
        }
    }


}

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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
