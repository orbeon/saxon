package org.orbeon.saxon;

import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.instruct.TerminationException;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.query.*;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trace.XQueryTraceListener;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Date;
import java.util.Properties;

/**
 * This <B>Query</B> class provides a command-line interface to the Saxon XQuery processor.<p>
 * <p/>
 * The XQuery syntax supported conforms to the W3C XQuery 1.0 drafts.
 *
 * @author Michael H. Kay
 */

public class Query {

    protected Configuration config;

    /**
     * Set the configuration. This is designed to be
     * overridden in a subclass
     */

    protected Configuration makeConfiguration(boolean schemaAware) {
        if (schemaAware) {
            config = Configuration.makeSchemaAwareConfiguration(null);
        } else {
            config = new Configuration();
            // In basic XQuery, all nodes are untyped when calling from the command line
            config.setAllNodesUntyped(true);
        }
        return config;
    }

    /**
     * Get the configuration in use
     *
     * @return the configuration
     */

    protected Configuration getConfiguration() {
        return config;
    }

    /**
     * Main program, can be used directly from the command line.
     * <p>The format is:</P>
     * <p>java org.orbeon.saxon.Query [options] <I>query-file</I> &gt;<I>output-file</I></P>
     * <p>followed by any number of parameters in the form {keyword=value}... which can be
     * referenced from within the query.</p>
     * <p>This program executes the query in query-file.</p>
     *
     * @param args List of arguments supplied on operating system command line
     * @throws Exception Indicates that a compile-time or
     *                   run-time error occurred
     */

    public static void main(String args[])
            throws Exception {
        // the real work is delegated to another routine so that it can be used in a subclass
        (new Query()).doQuery(args, "java org.orbeon.saxon.Query");
    }

    /**
     * Support method for main program. This support method can also be invoked from subclasses
     * that support the same command line interface
     *
     * @param args the command-line arguments
     * @param name name of the class, to be used in error messages
     */

    protected void doQuery(String args[], String name) {
        boolean showTime = false;
        int repeat = 1;
        String sourceFileName = null;
        String queryFileName = null;
        File sourceFile;
        File outputFile;
        boolean useURLs = false;
        String outputFileName = null;
        boolean explain = false;
        boolean wrap = false;
        boolean pullMode = false;

        boolean schemaAware = false;
        for (int i=0; i<args.length; i++) {
            if (args[i].equals("-sa")) {
                schemaAware = true;
            } else if (args[i].equals("-val")) {
                schemaAware = true;
            } else if (args[i].equals("-vlax")) {
                schemaAware = true;
            } else if (args[i].equals("-p")) {
                schemaAware = true;
            }
        }

        config = makeConfiguration(schemaAware);
        config.setHostLanguage(Configuration.XQUERY);

        StaticQueryContext staticEnv = new StaticQueryContext(config);
        DynamicQueryContext dynamicEnv = new DynamicQueryContext(config);

        Properties outputProps = new Properties();

        // Check the command-line arguments.

        try {
            int i = 0;
            while (i < args.length) {

                if (args[i].charAt(0) == '-') {

                    if (args[i].equals("-cr")) {
                        i++;
                        if (args.length < i + 1) {
                            badUsage(name, "No output file name");
                        }
                        String crclass = args[i++];
                        Object resolver = config.getInstance(crclass, null);
                        if (!(resolver instanceof CollectionURIResolver)) {
                            quit(crclass + " is not a CollectionURIResolver", 2);
                        }
                        config.setCollectionURIResolver((CollectionURIResolver)resolver);

                    } else if (args[i].equals("-ds")) {
                        config.setTreeModel(Builder.LINKED_TREE);
                        i++;
                    } else if (args[i].equals("-dt")) {
                        config.setTreeModel(Builder.TINY_TREE);
                        i++;
                    } else if (args[i].equals("-e")) {
                        explain = true;
                        i++;
                    } else if (args[i].equals("-l")) {
                        config.setLineNumbering(true);
                        i++;
                    } else if (args[i].equals("-3")) {    // undocumented option: do it thrice
                        i++;
                        repeat = 3;
                    } else if (args[i].equals("-9")) {    // undocumented option: do it nine times
                        i++;
                        repeat = 9;
                    } else if (args[i].equals("-mr")) {
                        i++;
                        if (args.length < i + 1) {
                            badUsage(name, "No ModuleURIResolver class");
                        }
                        String r = args[i++];
                        config.setModuleURIResolver(r);
                    } else if (args[i].equals("-noext")) {
                        i++;
                        config.setAllowExternalFunctions(false);
                    } else if (args[i].equals("-o")) {
                        i++;
                        if (args.length < i + 1) {
                            badUsage(name, "No output file name");
                        }
                        outputFileName = args[i++];
                    } else if (args[i].equals("-p")) {
                        i++;
                        setPOption(config);
                        useURLs = true;
                    } else if (args[i].equals("-pull")) {
                        i++;
                        pullMode = true;
                    } else if (args[i].equals("-r")) {
                        i++;
                        if (args.length < i + 1) {
                            badUsage(name, "No URIResolver class");
                        }
                        String r = args[i++];
                        config.setURIResolver(config.makeURIResolver(r));
                        dynamicEnv.setURIResolver(config.makeURIResolver(r));
                    } else if (args[i].equals("-s")) {
                        i++;
                        if (args.length < i + 1) {
                            badUsage(name, "No source file name");
                        }
                        sourceFileName = args[i++];
                    } else if (args[i].equals("-sa")) {
                        // already handled
                        i++;
                    } else if (args[i].equals("-snone")) {
                        config.setStripsWhiteSpace(Whitespace.NONE);
                        i++;
                    } else if (args[i].equals("-sall")) {
                        config.setStripsWhiteSpace(Whitespace.ALL);
                        i++;
                    } else if (args[i].equals("-signorable")) {
                        config.setStripsWhiteSpace(Whitespace.IGNORABLE);
                        i++;
                    } else if (args[i].equals("-strip")) {  // retained for compatibility
                        config.setStripsWhiteSpace(Whitespace.ALL);
                        i++;
                    } else if (args[i].equals("-t")) {
                        System.err.println(config.getProductTitle());
                        //System.err.println("Java version " + System.getProperty("java.version"));
                        System.err.println(config.getPlatform().getPlatformVersion());
                        config.setTiming(true);
                        showTime = true;
                        i++;
                    } else if (args[i].equals("-T")) {
                        config.setTraceListener(new XQueryTraceListener());
                        i++;
                    } else if (args[i].equals("-TJ")) {
                        i++;
                        config.setTraceExternalFunctions(true);
                    } else if (args[i].equals("-TL")) {
                        if (args.length < i + 2) {
                            badUsage(name, "No TraceListener class specified");
                        }
                        TraceListener traceListener = config.makeTraceListener(args[++i]);
                        config.setTraceListener(traceListener);
                        config.setLineNumbering(true);
                        i++;
                    } else if (args[i].equals("-u")) {
                        useURLs = true;
                        i++;
                    } else if (args[i].equals("-untyped")) {
                        // TODO: this is an experimental undocumented option. It should be checked for consistency
                        config.setAllNodesUntyped(true);
                        i++;
                    } else if (args[i].equals("-v")) {
                        config.setValidation(true);
                        i++;
                    } else if (args[i].equals("-val")) {
                        if (schemaAware) {
                            config.setSchemaValidationMode(Validation.STRICT);
                        } else {
                            quit("The -val option requires a schema-aware processor", 2);
                        }
                        i++;
                    } else if (args[i].equals("-vlax")) {
                        if (schemaAware) {
                            config.setSchemaValidationMode(Validation.LAX);
                        } else {
                            quit("The -vlax option requires a schema-aware processor", 2);
                        }
                        i++;
                    } else if (args[i].equals("-vw")) {
                        if (schemaAware) {
                            config.setValidationWarnings(true);
                        } else {
                            quit("The -vw option requires a schema-aware processor", 2);
                        }
                        i++;
                    } else if (args[i].equals("-wrap")) {
                        wrap = true;
                        i++;
                    } else if (args[i].equals("-1.1")) {
                        config.setXMLVersion(Configuration.XML11);
                        i++;
                    } else if (args[i].equals("-?")) {
                        badUsage(name, "");
                    } else if (args[i].equals("-")) {
                        queryFileName = "-";
                        i++;
                    } else {
                        badUsage(name, "Unknown option " + args[i]);
                    }
                } else {
                    break;
                }
            }

            if (!("-".equals(queryFileName))) {
                if (args.length < i + 1) {
                    badUsage(name, "No query file name");
                }
                queryFileName = args[i++];
            }

            for (int p = i; p < args.length; p++) {
                String arg = args[p];
                int eq = arg.indexOf("=");
                if (eq < 1 || eq >= arg.length() - 1) {
                    badUsage(name, "Bad param=value pair on command line: " + arg);
                }
                String argname = arg.substring(0, eq);
                if (argname.startsWith("!")) {
                    // parameters starting with "!" are taken as output properties
                    outputProps.setProperty(argname.substring(1), arg.substring(eq + 1));
                } else if (argname.startsWith("+")) {
                    // parameters starting with "+" are taken as input documents
                    Object sources = Transform.loadDocuments(arg.substring(eq + 1), useURLs, config, true);
                    dynamicEnv.setParameter(argname.substring(1), sources);
                } else {
                    dynamicEnv.setParameter(argname, new UntypedAtomicValue(arg.substring(eq + 1)));
                }
            }

            config.displayLicenseMessage();
            if (pullMode) {
                config.setLazyConstructionMode(true);
            }

            Source sourceInput = null;

            if (sourceFileName != null) {
                if (useURLs || sourceFileName.startsWith("http:") || sourceFileName.startsWith("file:")) {
                    sourceInput = config.getURIResolver().resolve(sourceFileName, null);
                    if (sourceInput == null) {
                        sourceInput = config.getSystemURIResolver().resolve(sourceFileName, null);
                    }
                } else if (sourceFileName.equals("-")) {
                    // take input from stdin
                    sourceInput = new StreamSource(System.in);
                } else {
                    sourceFile = new File(sourceFileName);
                    if (!sourceFile.exists()) {
                        quit("Source file " + sourceFile + " does not exist", 2);
                    }

                    if (config.getPlatform() instanceof JavaPlatform) {
                        InputSource eis = new InputSource(sourceFile.toURI().toString());
                        sourceInput = new SAXSource(eis);
                    } else {
                        sourceInput = new StreamSource(sourceFile.toURI().toString());
                    }
                }
            }

            long startTime = (new Date()).getTime();
            if (showTime) {
                System.err.println("Compiling query from " + queryFileName);
            }

            XQueryExpression exp;

            try {
                if (queryFileName.equals("-")) {
                    Reader queryReader = new InputStreamReader(System.in);
                    exp = staticEnv.compileQuery(queryReader);
                } else if (queryFileName.startsWith("{") && queryFileName.endsWith("}")) {
                    // query is inline on the command line
                    String q = queryFileName.substring(1, queryFileName.length() - 1);
                    exp = staticEnv.compileQuery(q);
                } else if (useURLs || queryFileName.startsWith("http:") || queryFileName.startsWith("file:")) {
                    ModuleURIResolver resolver = config.getModuleURIResolver();
                    String[] locations = {queryFileName};
                    Source[] sources = resolver.resolve(null, null, locations);
                    if (sources.length != 1 || !(sources[0] instanceof StreamSource)) {
                        quit("Module URI Resolver must return a single StreamSource", 2);
                    }
                    String queryText = QueryReader.readSourceQuery((StreamSource)sources[0], config.getNameChecker());
                    exp = staticEnv.compileQuery(queryText);
                } else {
                    InputStream queryStream = new FileInputStream(queryFileName);
                    staticEnv.setBaseURI(new File(queryFileName).toURI().toString());
                    exp = staticEnv.compileQuery(queryStream, null);
                }
                staticEnv = exp.getStaticContext();     // the original staticContext is copied

                if (showTime) {
                    long endTime = (new Date()).getTime();
                    System.err.println("Compilation time: " + (endTime - startTime) + " milliseconds");
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
                    quit("Failed to compile query", 2);
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
                staticEnv.getExecutable().getKeyManager().explainKeys(config);
                staticEnv.explainGlobalVariables();
                staticEnv.explainGlobalFunctions();
                exp.explain(staticEnv.getConfiguration());
            }

            OutputStream destination;
            if (outputFileName != null) {
                outputFile = new File(outputFileName);
                if (outputFile.isDirectory()) {
                    quit("Output is a directory", 2);
                }
                destination = new FileOutputStream(outputFile);
            } else {
                destination = System.out;
            }

            for (int r = 0; r < repeat; r++) {      // repeat is for internal testing/timing

                if (sourceInput != null) {
                    if (showTime) {
                        System.err.println("Processing " + sourceInput.getSystemId());
                    }
                    DocumentInfo doc = staticEnv.buildDocument(sourceInput);
                    dynamicEnv.setContextItem(doc);
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
                    } else if (pullMode) {
                        if (wrap) {
                            outputProps.setProperty(SaxonOutputKeys.WRAP, "yes");
                        }
                        try {
                            exp.pull(dynamicEnv, new StreamResult(destination), outputProps);
                        } catch (XPathException err) {
                            config.reportFatalError(err);
                            throw err;
                        }
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
                    System.err.println("Execution time: " + (endTime - startTime) + " milliseconds");
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
     * @param code    The result code to be returned to the operating
     *                system shell
     */

    protected static void quit(String message, int code) {
        System.err.println(message);
        System.exit(code);
    }

    public void setPOption(Configuration config) {
        config.getSystemURIResolver().setRecognizeQueryParameters(true);
    }

    /**
     * Report incorrect usage of the command line, with a list of the options and arguments that are available
     *
     * @param name    The name of the command being executed (allows subclassing)
     * @param message The error message
     */
    protected void badUsage(String name, String message) {
        if (!"".equals(message)) {
            System.err.println(message);
        }
        System.err.println(config.getProductTitle());
        System.err.println("Usage: " + name + " [options] query {param=value}...");
        System.err.println("Options: ");
        System.err.println("  -cr classname   Use specified CollectionURIResolver class");
        System.err.println("  -ds             Use linked tree data structure");
        System.err.println("  -dt             Use tiny tree data structure (default)");
        System.err.println("  -e              Explain optimized query expression");
        System.err.println("  -mr classname   Use specified ModuleURIResolver class");
        System.err.println("  -noext          Disallow calls to Java methods");
        System.err.println("  -o filename     Send output to named file");
        System.err.println("  -p              Recognize Saxon file extensions and query parameters");
        System.err.println("  -pull           Run query in pull mode");
        System.err.println("  -r classname    Use specified URIResolver class");
        System.err.println("  -s file|URI     Provide initial context document");
        System.err.println("  -sa             Schema-aware query (requires Saxon-SA)");
        System.err.println("  -sall           Strip all whitespace text nodes");
        System.err.println("  -signorable     Strip ignorable whitespace text nodes (default)");
        System.err.println("  -snone          Strip no whitespace text nodes");
        System.err.println("  -t              Display version and timing information");
        System.err.println("  -T              Trace query execution");
        System.err.println("  -TJ             Trace calls to external Java functions");
        System.err.println("  -TL classname   Trace query execution to user-defined trace listener");
        System.err.println("  -u              Names are URLs not filenames");
        System.err.println("  -v              Validate source documents using DTD");
        System.err.println("  -val            Validate source documents using schema");
        System.err.println("  -vlax           Lax validation of source documents using schema");
        System.err.println("  -vw             Treat validation errors on result document as warnings");
        System.err.println("  -wrap           Wrap result sequence in XML elements");
        System.err.println("  -1.1            Allow XML 1.1 documents");
        System.err.println("  -?              Display this message ");
        System.err.println("  param=value     Set query string parameter");
        System.err.println("  +param=value    Set query document parameter");
        System.err.println("  !option=value   Set serialization option");
        if ("".equals(message)) {
            System.exit(0);
        } else {
            System.exit(2);
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
// Contributor(s): changes to allow source and/or stylesheet from stdin contributed by
// Gunther Schadow [gunther@aurora.regenstrief.org]
//
