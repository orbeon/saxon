package net.sf.saxon;
import net.sf.saxon.event.Builder;
import net.sf.saxon.instruct.TerminationException;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.Validation;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.QueryResult;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trace.XQueryTraceListener;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.UntypedAtomicValue;
import org.xml.sax.InputSource;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * This <B>Query</B> class provides a command-line interface to the Saxon XQuery processor.<p>
 *
 * The XQuery syntax supported conforms to the W3C XQuery 1.0 drafts.
 *
 * @author Michael H. Kay
 */

public class Query {

    Configuration config;

   /**
     * Set the configuration. This is designed to be
     * overridden in a subclass
     */

    protected Configuration makeConfiguration() {
        return new Configuration();
    }

    /**
     * Get the configuration in use
     * @return the configuration
     */

    protected Configuration getConfiguration() {
        return config;
    }

    /**
     * Main program, can be used directly from the command line.
     * <p>The format is:</P>
     * <p>java net.sf.saxon.Query [options] <I>query-file</I> &gt;<I>output-file</I></P>
     * <p>followed by any number of parameters in the form {keyword=value}... which can be
     * referenced from within the query.</p>
     * <p>This program executes the query in query-file.</p>
     *
     * @param args List of arguments supplied on operating system command line
     * @exception Exception Indicates that a compile-time or
     *     run-time error occurred
     */

    public static void main (String args[])
        throws Exception
    {
        // the real work is delegated to another routine so that it can be used in a subclass
        (new Query()).doMain(args, "java net.sf.saxon.Query");
    }

    /**
     * Support method for main program. This support method can also be invoked from subclasses
     * that support the same command line interface
     *
     * @param args the command-line arguments
     * @param name name of the class, to be used in error messages
     */

    protected void doMain(String args[], String name) {
        config = makeConfiguration();
        config.setHostLanguage(Configuration.XQUERY);
        boolean schemaAware = config.isSchemaAware(Configuration.XQUERY);
        boolean showTime = false;
        int repeat = 1;
        StaticQueryContext staticEnv = new StaticQueryContext(config);
        DynamicQueryContext dynamicEnv = new DynamicQueryContext(config);
        String sourceFileName = null;
        String queryFileName = null;
        File sourceFile;
        File outputFile;
        boolean useURLs = false;
        String outputFileName = null;
        boolean explain = false;
        boolean wrap = false;

        Properties outputProps = new Properties();
        outputProps.setProperty(OutputKeys.INDENT, "yes");

		// Check the command-line arguments.

        try {
            int i = 0;
            while (i < args.length) {

                if (args[i].charAt(0)=='-') {

                    if (args[i].equals("-ds")) {
                        config.setTreeModel(Builder.STANDARD_TREE);
                        i++;
                    }

                    else if (args[i].equals("-dt")) {
                        config.setTreeModel(Builder.TINY_TREE);
                        i++;
                    }

                    else if (args[i].equals("-e")) {
                        explain = true;
                        i++;
                    }

                    else if (args[i].equals("-l")) {
                        config.setLineNumbering(true);
                        i++;
                    }

                    else if (args[i].equals("-3")) {    // undocumented option: do it thrice
                        i++;
                        repeat = 3;
                    }

                    else if (args[i].equals("-9")) {    // undocumented option: do it nine times
                        i++;
                        repeat = 9;
                    }

                    else if (args[i].equals("-noext")) {
                        i++;
                        config.setAllowExternalFunctions(false);
                    }

                    else if (args[i].equals("-o")) {
                        i++;
                        if (args.length < i+1) badUsage(name, "No output file name");
                        outputFileName = args[i++];
                    }

                    else if (args[i].equals("-r")) {
                        i++;
                        if (args.length < i+1) badUsage(name, "No URIResolver class");
                        String r = args[i++];
                        config.setURIResolver(makeURIResolver(r));
                        dynamicEnv.setURIResolver(makeURIResolver(r));
                    }

                    else if (args[i].equals("-s")) {
                        i++;
                        if (args.length < i+1) badUsage(name, "No source file name");
                        sourceFileName = args[i++];
                    }

                    else if (args[i].equals("-strip")) {
                        config.setStripsAllWhiteSpace(true);
                        i++;
                    }

                    else if (args[i].equals("-t")) {
                        System.err.println(config.getProductTitle());
                        System.err.println("Java version " + System.getProperty("java.version"));
                        //Loader.setTracing(true);
                        config.setTiming(true);
                        showTime = true;
                        i++;
                    }

                    else if (args[i].equals("-T")) {
                        config.setTraceListener(new XQueryTraceListener());
                        i++;
                    }

                    else if (args[i].equals("-TJ")) {
                        i++;
                        config.setTraceExternalFunctions(true);
                    }

                    else if (args[i].equals("-TL")) {
                        if (args.length < i+2) badUsage(name, "No TraceListener class specified");
                        TraceListener traceListener = Transform.makeTraceListener(args[++i]);
                        config.setTraceListener(traceListener);
                        config.setLineNumbering(true);
                        i++;
                    }

                    else if (args[i].equals("-u")) {
                        useURLs = true;
                        i++;
                    }

                    else if (args[i].equals("-val")) {
                        if (schemaAware) {
                            config.setSchemaValidationMode(Validation.STRICT);
                        } else {
                            quit("The -val option requires a schema-aware processor", 2);
                        }
                        i++;
                    }

                    else if (args[i].equals("-vlax")) {
                        if (schemaAware) {
                            config.setSchemaValidationMode(Validation.LAX);
                        } else {
                            quit("The -vlax option requires a schema-aware processor", 2);
                        }
                        i++;
                    }

                    else if (args[i].equals("-vw")) {
                        if (schemaAware) {
                            config.setValidationWarnings(true);
                        } else {
                            quit("The -vw option requires a schema-aware processor", 2);
                        }
                        i++;
                    }

                    else if (args[i].equals("-wrap")) {
                        wrap = true;
                        i++;
                    }

                    else if (args[i].equals("-?")) {
                        badUsage(name, "");
                    }

                    else if (args[i].equals("-")) {
                        queryFileName = "-";
                        i++;
                    }

                    else badUsage(name, "Unknown option " + args[i]);
                }

                else break;
            }

            if (!("-".equals(queryFileName))) {
                if (args.length < i+1) {
                    badUsage(name, "No query file name");
                }
                queryFileName = args[i++];
            }

            for (int p=i; p<args.length; p++) {
                String arg = args[p];
                int eq = arg.indexOf("=");
                if (eq<1 || eq>=arg.length()-1) {
                    badUsage(name, "Bad param=value pair on command line");
                }
                String argname = arg.substring(0,eq);
                if (argname.startsWith("!")) {
                    // parameters starting with "!" are taken as output properties
                    outputProps.setProperty(argname.substring(1), arg.substring(eq+1));
                } else if (argname.startsWith("+")) {
                    // parameters starting with "+" are taken as input documents
                    List sources = Transform.loadDocuments(arg.substring(eq+1), useURLs, config, true);
                    dynamicEnv.setParameter(argname.substring(1), sources);
                } else {
                    dynamicEnv.setParameter(argname, new UntypedAtomicValue(arg.substring(eq+1)));
                }
            }

            config.displayLicenseMessage();

            Source sourceInput = null;

            if (sourceFileName != null) {
                if (useURLs || sourceFileName.startsWith("http:") || sourceFileName.startsWith("file:")) {
                    sourceInput = dynamicEnv.getURIResolver().resolve(sourceFileName, null);
                } else if (sourceFileName.equals("-")) {
                    // take input from stdin
                    sourceInput = new SAXSource(new InputSource(System.in));
                } else {
                    sourceFile = new File(sourceFileName);
                    if (!sourceFile.exists()) {
                        quit("Source file " + sourceFile + " does not exist", 2);
                    }

                    InputSource eis = new InputSource(sourceFile.toURI().toString());
                    sourceInput = new SAXSource(eis);
                }
            }

            Reader queryReader;
            if (queryFileName.equals("-")) {
                queryReader = new InputStreamReader(System.in);
            } else if (queryFileName.startsWith("{") && queryFileName.endsWith("}")) {
                // query is inline on the command line
                String q = queryFileName.substring(1, queryFileName.length()-1);
                queryReader = new StringReader(q);
            } else {
                queryReader = new FileReader(queryFileName);
                staticEnv.setBaseURI(new File(queryFileName).toURI().toString());
            }

            OutputStream destination;
            if (outputFileName!=null) {
                outputFile = new File(outputFileName);
                if (outputFile.isDirectory()) {
                        quit("Output is a directory", 2);
                }
                destination = new FileOutputStream(outputFile);
            } else {
                destination = System.out;
            }

            long startTime = (new Date()).getTime();

            XQueryExpression exp;

            try {

                if (showTime) {
                    System.err.println("Compiling query from " + queryFileName);
                }

                exp = staticEnv.compileQuery(queryReader);

                if (showTime) {
                    long endTime = (new Date()).getTime();
                    System.err.println("Compilation time: " + (endTime-startTime) + " milliseconds");
                    startTime = endTime;
                }

            } catch (XPathException err) {
                int line = -1;
                String module = null;
                if (err.getLocator() != null) {
                    line = err.getLocator().getLineNumber();
                    module = err.getLocator().getSystemId();
                }
                if (err.hasBeenReported()) {
                    quit(err.getMessage(), 2);
                } else {
                    if (line == -1) {
                        System.err.println("Failed to compile query: " + err.getMessage());
                    } else {
                        System.err.println("Static error at line " + line + " of " + module + ':');
                        System.err.println(err.getMessage());
                    }
                }
                exp = null;
                System.exit(2);
            }

            if (explain) {
                staticEnv.explainGlobalVariables();
                staticEnv.explainGlobalFunctions();
                exp.explain(staticEnv.getNamePool());
            }

            for (int r=0; r<repeat; r++) {      // repeat is for internal testing/timing

                if (sourceInput != null) {
                    if (showTime) {
                        System.err.println("Processing " + sourceInput.getSystemId());
                    }
                    DocumentInfo doc = staticEnv.buildDocument(sourceInput);
                    dynamicEnv.setContextNode(doc);
                }

                try {
                    if (wrap) {
                        SequenceIterator results = exp.iterator(dynamicEnv);
                        DocumentInfo resultDoc = QueryResult.wrap(results, config);
                        QueryResult.serialize(resultDoc,
                                new StreamResult(destination),
                                outputProps,
                                config);
                        destination.close();
                    } else {
                        exp.run(dynamicEnv, new StreamResult(destination), outputProps);
                    }
                } catch (TerminationException err) {
                    throw err;
                } catch (XPathException err) {
                    if (err.hasBeenReported()) {
                        throw new DynamicError("Run-time errors were reported");
                    } else {
                        throw err;
                    }
                }

                if (showTime) {
                    long endTime = (new Date()).getTime();
                    System.err.println("Execution time: " + (endTime-startTime) + " milliseconds");
                    startTime = endTime;
                }
            }

        } catch (TerminationException err) {
            quit(err.getMessage(), 1);
        } catch (XPathException err) {
            quit("Query processing failed: " + err.getMessage(), 2);
        } catch (TransformerFactoryConfigurationError err) {
            err.printStackTrace();
            quit("Query processing failed", 2);
        } catch (Exception err2) {
            err2.printStackTrace();
            quit("Fatal error during transformation: " + err2.getMessage(), 2);
        }
    }

    /**
     * Exit with a message
     *
     * @param message The message to be output
     * @param code The result code to be returned to the operating
     *     system shell
     */

    protected static void quit(String message, int code) {
        System.err.println(message);
        System.exit(code);
    }

    /**
     * Report incorrect usage of the command line, with a list of the options and arguments that are available
     *
     * @param name The name of the command being executed (allows subclassing)
     * @param message The error message
     */
    protected void badUsage(String name, String message) {
        if (!"".equals(message)) {
            System.err.println(message);
        }
        System.err.println(config.getProductTitle());
        System.err.println("Usage: " + name + " [options] query {param=value}...");
        System.err.println("Options: ");
        System.err.println("  -ds             Use standard tree data structure");
        System.err.println("  -dt             Use tinytree data structure (default)");
        System.err.println("  -e              Explain optimized query expression");
        System.err.println("  -noext          Disallow calls to Java methods");
        System.err.println("  -o filename     Send output to named file");
        System.err.println("  -r classname    Use specified URIResolver class");
        System.err.println("  -s file|URI     Provide initial context document");
        System.err.println("  -strip          Strip whitespace text nodes");
        System.err.println("  -t              Display version and timing information");
        System.err.println("  -T              Trace query execution");
        System.err.println("  -TJ             Trace calls to external Java functions");
        System.err.println("  -TL classname   Trace query execution to user-defined trace listener");
        System.err.println("  -u              Names are URLs not filenames");
        if (config.isSchemaAware(Configuration.XQUERY)) {
            System.err.println("  -val            Validate source documents using schema");
            System.err.println("  -vlax           Lax validation of source documents using schema");                  
            System.err.println("  -vw             Treat validation errors on result document as warnings");
        }
        System.err.println("  -wrap           Wraps result sequence in XML elements");
        System.err.println("  -?              Display this message ");
        System.err.println("  param=value     Set stylesheet string parameter");
        System.err.println("  +param=value    Set stylesheet document parameter");
        System.err.println("  !option=value   Set serialization option");
        if ("".equals(message)) {
            System.exit(0);
        } else {
            System.exit(2);
        }
    }

    /**
     * Create an instance of a URIResolver with a specified class name
     *
     * @exception XPathException if the requested class does not
     *     implement the javax.xml.transform.URIResolver interface
     * @param className The fully-qualified name of the URIResolver class
     * @return The newly created URIResolver
     */
    public static URIResolver makeURIResolver (String className)
    throws XPathException
    {
        Object obj = Loader.getInstance(className);
        if (obj instanceof URIResolver) {
            return (URIResolver)obj;
        }
        throw new DynamicError("Class " + className + " is not a URIResolver");
    }

    /**
     * Load a document, or all the documents in a directory, given a filename or URL
     */

//    protected List loadDocuments(String sourceFileName, boolean useURLs)
//    throws XPathException {
//
//        List result = new ArrayList();
//        Source sourceInput;
//        if (useURLs || sourceFileName.startsWith("http:") || sourceFileName.startsWith("file:")) {
//            sourceInput = config.getURIResolver().resolve(sourceFileName, null);
//            if (sourceInput==null) {
//                sourceInput = new StandardURIResolver().resolve(sourceFileName, null);
//            }
//            result.add(sourceInput);
//        } else if (sourceFileName.equals("-")) {
//            // take input from stdin
//            sourceInput = new SAXSource(config.getSourceParser(), new InputSource(System.in));
//            result.add(sourceInput);
//        } else {
//            File sourceFile = new File(sourceFileName);
//            if (!sourceFile.exists()) {
//                quit("Source file " + sourceFile + " does not exist", 2);
//            }
//            if (sourceFile.isDirectory()) {
//                String[] files = sourceFile.list();
//                for (int f=0; f<files.length; f++) {
//                    File file = new File(sourceFile, files[f]);
//                    if (!file.isDirectory()) {
//                        InputSource eis = new InputSource(file.toURI().toString());
//                        Source source = new SAXSource(config.getSourceParser(), eis);
//                        result.add(source);
//                    }
//                }
//            } else {
//                InputSource eis = new InputSource(sourceFile.toURI().toString());
//                sourceInput = new SAXSource(config.getSourceParser(), eis);
//                result.add(sourceInput);
//            }
//        }
//        return result;
//    }


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
// Contributor(s): changes to allow source and/or stylesheet from stdin contributed by
// Gunther Schadow [gunther@aurora.regenstrief.org]
//
