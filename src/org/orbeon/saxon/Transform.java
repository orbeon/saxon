package org.orbeon.saxon;

import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.instruct.TerminationException;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * This <B>Transform</B> class is the entry point to the Saxon XSLT Processor. This
 * class is provided to control the processor from the command line.<p>
 * <p/>
 * The XSLT syntax supported conforms to the W3C XSLT 1.0 and XPath 1.0 recommendation.
 * Only the transformation language is implemented (not the formatting objects).
 * Saxon extensions are documented in the file extensions.html
 *
 * @author Michael H. Kay
 */

public class Transform {

    protected TransformerFactoryImpl factory;
    protected Configuration config;
    protected boolean useURLs = false;
    protected boolean showTime = false;
    protected int repeat = 1;
    String sourceParserName = null;

    /**
     * Main program, can be used directly from the command line.
     * <p>The format is:</P>
     * <p>java org.orbeon.saxon.Transform [options] <I>source-file</I> <I>style-file</I> &gt;<I>output-file</I></P>
     * <p>followed by any number of parameters in the form {keyword=value}... which can be
     * referenced from within the stylesheet.</p>
     * <p>This program applies the XSL style sheet in style-file to the source XML document in source-file.</p>
     *
     * @param args List of arguments supplied on operating system command line
     * @throws java.lang.Exception Indicates that a compile-time or
     *                             run-time error occurred
     */

    public static void main(String args[])
            throws java.lang.Exception {
        // the real work is delegated to another routine so that it can be used in a subclass
        (new Transform()).doTransform(args, "java org.orbeon.saxon.Transform");
    }

    /**
     * Set the configuration in the TransformerFactory. This is designed to be
     * overridden in a subclass
     * @param schemaAware True if the transformation is to be schema-aware
     * @param className Name of the schema-aware Configuration class to be loaded. Designed for use by .NET;
     * can normally be null.
     */

    public void setFactoryConfiguration(boolean schemaAware, String className) throws RuntimeException {
        if (schemaAware) {
            config = Configuration.makeSchemaAwareConfiguration(null, className);
        } else {
            config = new Configuration();
            // In basic XSLT, all nodes are untyped when calling from the command line
            config.setAllNodesUntyped(true);
        }
        factory = new TransformerFactoryImpl(config);
    }

    /**
     * Support method for main program. This support method can also be invoked from subclasses
     * that support the same command line interface
     *
     * @param args the command-line arguments
     * @param command the form of the command as written by the user, to be used in error messages
     */

    public void doTransform(String args[], String command) {


        String sourceFileName = null;
        String styleFileName = null;
        File outputFile = null;
        ArrayList parameterList = new ArrayList(20);
        String outputFileName = null;
        String initialMode = null;
        String initialTemplate = null;
        boolean useAssociatedStylesheet = false;
        boolean wholeDirectory = false;
        boolean precompiled = false;
        boolean dtdValidation = false;
        String styleParserName = null;
        boolean explain = false;
        String explainOutputFileName = null;
        String additionalSchemas = null;
        PrintStream traceDestination = System.err;
        boolean closeTraceDestination = false;

        boolean schemaAware = false;
        for (int i=0; i<args.length; i++) {
            if (args[i].equals("-sa") ||
                    args[i].startsWith("-sa:") ||
                    args[i].startsWith("-val:") ||
                    args[i].equals("-val") ||
                    args[i].equals("-vlax") ||
                    args[i].startsWith("-xsd:") ||
                    args[i].startsWith("-xsdversion:") ||
                    args[i].equals("-p")) {
                schemaAware = true;
                break;
            }
        }

        try {
            setFactoryConfiguration(schemaAware, null);
        } catch (Exception err) {
            err.printStackTrace();
            quit(err.getMessage(), 2);
        }
        config = factory.getConfiguration();
        config.setVersionWarning(true);  // unless suppressed by command line options
        schemaAware = config.isSchemaAware(Configuration.XSLT);

        // Check the command-line arguments.

        try {
            int i = 0;
            while (true) {
                if (i >= args.length) {
                    break;
                }

                if (args[i].charAt(0) == '-') {
                    String option;
                    String value = null;
                    int colon = args[i].indexOf(':');
                    if (colon > 0 && colon < args[i].length() - 1) {
                        option = args[i].substring(1, colon);
                        value = args[i].substring(colon+1);
                    } else {
                        option = args[i].substring(1);
                    }
                    if (option.equals("a")) {
                        useAssociatedStylesheet = true;
                        i++;
                    } else if (option.equals("c")) {
                        precompiled = true;
                        if (value != null) {
                            styleFileName = value;
                        }
                        i++;
                    } else if (option.equals("cr")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No resolver after -cr");
                            }
                            value = args[i++];
                        }
                        Object resolver = config.getInstance(value, null);
                        factory.setAttribute(FeatureKeys.COLLECTION_URI_RESOLVER, resolver);
                    } else if (option.equals("ds")) {
                        factory.setAttribute(FeatureKeys.TREE_MODEL,
                                new Integer(Builder.LINKED_TREE));
                        i++;
                    } else if (option.equals("dt")) {
                        factory.setAttribute(FeatureKeys.TREE_MODEL,
                                new Integer(Builder.TINY_TREE));
                        i++;
                    } else if (option.equals("dtd")) {
                        if (!("on".equals(value) || "off".equals(value))) {
                            badUsage(command, "-dtd option must be -dtd:on or -dtd:off");
                        }
                        factory.setAttribute(FeatureKeys.DTD_VALIDATION,
                                    Boolean.valueOf("on".equals(value)));
                        i++;
                    } else if (option.equals("expand")) {
                        if (!("on".equals(value) || "off".equals(value))) {
                            badUsage(command, "-expand option must be 'on' or 'off'");
                        }
                        factory.setAttribute(FeatureKeys.EXPAND_ATTRIBUTE_DEFAULTS,
                                    Boolean.valueOf("on".equals(value)));
                        i++;                        
                    } else if (option.equals("explain")) {
                        explain = true;
                        explainOutputFileName = value; // may be omitted/null
                        factory.setAttribute(FeatureKeys.TRACE_OPTIMIZER_DECISIONS, Boolean.TRUE);
                        i++;
                    } else if (option.equals("ext")) {
                        if (!("on".equals(value) || "off".equals(value))) {
                            badUsage(command, "-ext option must be -ext:on or -ext:off");
                        }
                        factory.setAttribute(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS,
                                    Boolean.valueOf("on".equals(value)));
                        i++;
                    } else if (option.equals("im")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No initial mode after -im");
                            }
                            value = args[i++];
                        }
                        initialMode = value;
                    } else if (option.equals("it")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No initial template after -it");
                            }
                            value = args[i++];
                        }
                        initialTemplate = value;
                    } else if (option.equals("l")) {
                        if (!(value==null || "on".equals(value) || "off".equals(value))) {
                            badUsage(command, "-l option must be -l:on or -l:off");
                        }
                        factory.setAttribute(FeatureKeys.LINE_NUMBERING,
                                Boolean.valueOf(!"off".equals(value)));
                        i++;
                    } else if (option.equals("m")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No message receiver class after -m");
                            }
                            value = args[i++];
                        }
                        factory.setAttribute(FeatureKeys.MESSAGE_EMITTER_CLASS, value);
                    } else if (option.equals("noext")) {
                        i++;
                        factory.setAttribute(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS,
                                Boolean.valueOf(false));
                    } else if (option.equals("novw")) {
                        factory.setAttribute(FeatureKeys.VERSION_WARNING,
                                Boolean.valueOf(false));
                        i++;
                    } else if (option.equals("o")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No output file name after -o");
                            }
                            value = args[i++];
                        }
                        outputFileName = value;

                    } else if (option.equals("or")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No output resolver class after -or");
                            }
                            value = args[i++];
                        }
                        String orclass = value;
                        Object resolver = config.getInstance(orclass, null);
                        factory.setAttribute(FeatureKeys.OUTPUT_URI_RESOLVER, resolver);

                    } else if (option.equals("outval")) {
                        if (schemaAware) {
                            if (!(value==null || "recover".equals(value) || "fatal".equals(value))) {
                                badUsage(command, "-outval option must be 'recover' or 'fatal'");
                            }
                            factory.setAttribute(FeatureKeys.VALIDATION_WARNINGS,
                                    Boolean.valueOf("recover".equals(value)));
                        } else {
                            quit("The -outval option requires a schema-aware processor", 2);
                        }
                        i++;
                    } else if (option.equals("p")) {
                        i++;
                        if (!(value==null || "on".equals(value) || "off".equals(value))) {
                            badUsage(command, "-p option must be -p:on or -p:off");
                        }
                        if (!"off".equals(value)) {
                            //setPOption(config);
                            config.setParameterizedURIResolver();
                            useURLs = true;
                        }
                    } else if (option.equals("r")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No URIesolver class after -r");
                            }
                            value = args[i++];
                        }
                        factory.setURIResolver(config.makeURIResolver(value));
                    } else if (option.equals("repeat")) {
                        i++;
                        if (value == null) {
                            badUsage(command, "No number after -repeat");
                        } else {
                            try {
                                repeat = Integer.parseInt(value);
                            } catch (NumberFormatException err) {
                                badUsage(command, "Bad number after -repeat");
                            }
                        }
                    } else if (option.equals("s")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No source file name after -s");
                            }
                            value = args[i++];
                        }
                        sourceFileName = value;
                    } else if (option.equals("sa")) {
                        // already handled
                        i++;
                    } else if (option.equals("snone")) {
                        factory.setAttribute(FeatureKeys.STRIP_WHITESPACE, "none");
                        i++;
                    } else if (option.equals("sall")) {
                        factory.setAttribute(FeatureKeys.STRIP_WHITESPACE, "all");
                        i++;
                    } else if (option.equals("signorable")) {
                        factory.setAttribute(FeatureKeys.STRIP_WHITESPACE, "ignorable");
                        i++;
                    } else if (option.equals("strip")) {
                        if ("none".equals(value) || "all".equals(value) || "ignorable".equals(value)) {
                            factory.setAttribute(FeatureKeys.STRIP_WHITESPACE, value);
                            i++;
                        } else {
                            badUsage(command, "-strip must be none, all, or ignorable");
                        }
                    } else if (option.equals("t")) {
                        if (!showTime) {
                            // don't do it twice if the option appears twice
                            System.err.println(config.getProductTitle());
                            System.err.println(Configuration.getPlatform().getPlatformVersion());
                            factory.setAttribute(FeatureKeys.TIMING, Boolean.valueOf(true));
                            showTime = true;
                        }
                        i++;
                    } else if (option.equals("T")) {
                        i++;
                        TraceListener traceListener;
                        if (value == null) {
                            traceListener = new org.orbeon.saxon.trace.XSLTTraceListener();
                        } else {
                            traceListener = config.makeTraceListener(value);
                        }
                        factory.setAttribute(FeatureKeys.TRACE_LISTENER, traceListener);
                        factory.setAttribute(FeatureKeys.LINE_NUMBERING, Boolean.TRUE);

                    } else if (option.equals("TJ")) {
                        i++;
                        factory.setAttribute(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS,
                                Boolean.TRUE);
                    } else if (option.equals("TL")) {
                        i++;
                        if (args.length < i + 2) {
                            badUsage(command, "No TraceListener class");
                        }
                        TraceListener traceListener = config.makeTraceListener(args[i++]);
                        factory.setAttribute(FeatureKeys.TRACE_LISTENER,
                                traceListener);
                        factory.setAttribute(FeatureKeys.LINE_NUMBERING,
                                Boolean.TRUE);
                    } else if (option.equals("TP")) {
                        i++;
                        TraceListener traceListener = new org.orbeon.saxon.trace.TimedTraceListener();
                        factory.setAttribute(FeatureKeys.TRACE_LISTENER,
                                traceListener);
                        factory.setAttribute(FeatureKeys.LINE_NUMBERING,
                                Boolean.TRUE);
                    } else if (option.equals("traceout")) {
                        i++;
                        if (value.equals("#err")) {
                            // no action, this is the default
                        } else if (value.equals("#out")) {
                            traceDestination = System.out;
                        } else if (value.equals("#null")) {
                            traceDestination = null;
                        } else {
                            traceDestination = new PrintStream(new FileOutputStream(new File(value)));
                            closeTraceDestination = true;
                        }

                    } else if (option.equals("tree")) {
                        if ("linked".equals(value)) {
                            factory.setAttribute(FeatureKeys.TREE_MODEL,
                                    new Integer(Builder.LINKED_TREE));
                        } else if ("tiny".equals(value)) {
                            factory.setAttribute(FeatureKeys.TREE_MODEL,
                                new Integer(Builder.TINY_TREE));
                        } else {
                            badUsage(command, "-tree option must be 'linked' or 'tiny'");
                        }
                        i++;
                    } else if (option.equals("u")) {
                        useURLs = true;
                        i++;
                    } else if (option.equals("v")) {
                        factory.setAttribute(FeatureKeys.DTD_VALIDATION,
                                Boolean.valueOf(true));
                        dtdValidation = true;
                        i++;
                    } else if (option.equals("val")) {
                        if (!schemaAware) {
                            badUsage(command, "The -val option requires a schema-aware processor");
                        } else if (value==null || "strict".equals(value)) {
                                factory.setAttribute(FeatureKeys.SCHEMA_VALIDATION,
                                    new Integer(Validation.STRICT));
                        } else if ("lax".equals(value)) {
                            factory.setAttribute(FeatureKeys.SCHEMA_VALIDATION,
                                new Integer(Validation.LAX));
                        } else {
                            badUsage(command, "-val option must be 'strict' or 'lax'");
                        }
                        i++;
                    } else if (option.equals("vlax")) {
                        if (schemaAware) {
                            factory.setAttribute(FeatureKeys.SCHEMA_VALIDATION,
                                    new Integer(Validation.LAX));
                        } else {
                            quit("The -vlax option requires a schema-aware processor", 2);
                        }
                        i++;
                    } else if (option.equals("versionmsg")) {
                        if (!("on".equals(value) || "off".equals(value))) {
                            badUsage(command, "-versionmsg option must be -versionmsg:on or -versionmsg:off");
                        }
                        factory.setAttribute(FeatureKeys.VERSION_WARNING,
                                    Boolean.valueOf("on".equals(value)));
                        i++;
                    } else if (option.equals("vw")) {
                        if (schemaAware) {
                            factory.setAttribute(FeatureKeys.VALIDATION_WARNINGS,
                                    Boolean.valueOf(true));
                        } else {
                            quit("The -vw option requires a schema-aware processor", 2);
                        }
                        i++;
                    } else if (option.equals("warnings")) {
                        if ("silent".equals(value)) {
                            factory.setAttribute(FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.RECOVER_SILENTLY));
                        } else if ("recover".equals(value)) {
                            factory.setAttribute(FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.RECOVER_WITH_WARNINGS));
                        } else if ("fatal".equals(value)) {
                            factory.setAttribute(FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.DO_NOT_RECOVER));
                        }
                        i++;
                    } else if (option.equals("w0")) {
                        i++;
                        factory.setAttribute(FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.RECOVER_SILENTLY));
                    } else if (option.equals("w1")) {
                        i++;
                        factory.setAttribute(FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.RECOVER_WITH_WARNINGS));
                    } else if (option.equals("w2")) {
                        i++;
                        factory.setAttribute(FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.DO_NOT_RECOVER));

                    } else if (option.equals("x")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No source parser class after -x");
                            }
                            value = args[i++];
                        }
                        sourceParserName = value;
                        factory.setAttribute(FeatureKeys.SOURCE_PARSER_CLASS, sourceParserName);
                    } else if (option.equals("xi")) {
                        if (!(value==null || "on".equals(value) || "off".equals(value))) {
                            badUsage(command, "-xi option must be -xi:on or -xi:off");
                        }
                        if (!"off".equals(value)) {
                            factory.setAttribute(FeatureKeys.XINCLUDE, Boolean.TRUE);
                        }
                        i++;
                   } else if (option.equals("xmlversion")) {    // XML 1.1
                        i++;
                        if (!("1.0".equals(value) | "1.1".equals(value))) {
                            badUsage(command, "-xmlversion must be 1.0 or 1.1");
                        }
                        factory.setAttribute(FeatureKeys.XML_VERSION, value);
                    } else if (option.equals("xsd")) {
                        i++;
                        additionalSchemas = value;
                    } else if (option.equals("xsdversion")) {    // XSD 1.1
                        i++;
                        if (!("1.0".equals(value) | "1.1".equals(value))) {
                            badUsage(command, "-xsdversion must be 1.0 or 1.1");
                        }
                        config.setConfigurationProperty(FeatureKeys.XSD_VERSION, value);
                    } else if (option.equals("xsiloc")) {
                        i++;
                        if ("off".equals(value)) {
                            config.setConfigurationProperty(FeatureKeys.USE_XSI_SCHEMA_LOCATION, Boolean.FALSE);
                        } else if ("on".equals(value)) {
                            config.setConfigurationProperty(FeatureKeys.USE_XSI_SCHEMA_LOCATION, Boolean.TRUE);
                        } else {
                            badUsage(value, "format: -xsiloc:(on|off)");
                        }
                    } else if (option.equals("xsl")) {
                        i++;
                        styleFileName = value;
                    } else if (option.equals("y")) {
                        i++;
                        if (value == null) {
                            if (args.length < i + 2) {
                                badUsage(command, "No stylesheet parser class after -y");
                            }
                            value = args[i++];
                        }
                        styleParserName = value;
                        factory.setAttribute(FeatureKeys.STYLE_PARSER_CLASS, value);

                    } else if (option.equals("1.1")) {    // XML 1.1
                        i++;
                        factory.setAttribute(FeatureKeys.XML_VERSION, "1.1");

                    } else if (args[i].equals("-?")) {
                        badUsage(command, "");
                    } else if (args[i].equals("-")) {
                        break;
                        // this means take the source from standard input
                    } else {
                        badUsage(command, "Unknown option " + args[i]);
                    }
                } else {
                    break;
                }
            }

            if (initialTemplate != null && useAssociatedStylesheet) {
                badUsage(command, "-it and -a options cannot be used together");
            }

            if (initialTemplate == null && sourceFileName == null) {
                if (args.length < i + 1) {
                    badUsage(command, "No source file name");
                }
                sourceFileName = args[i++];
            }

            if (!useAssociatedStylesheet && styleFileName == null) {
                if (args.length < i + 1) {
                    badUsage(command, "No stylesheet file name");
                }
                styleFileName = args[i++];
            }

            for (int p = i; p < args.length; p++) {
                String arg = args[p];
                int eq = arg.indexOf("=");
                if (eq < 1 || eq >= arg.length()) {
                    badUsage(command, "Bad param=value pair on command line: " + arg);
                }
                parameterList.add(arg);
            }

            config.displayLicenseMessage();

            if (additionalSchemas != null) {
                Query.loadAdditionalSchemas(config, additionalSchemas);
            }

            List sources = null;
            if (sourceFileName != null) {
                boolean useSAXSource = sourceParserName != null || dtdValidation;
                Object loaded = loadDocuments(sourceFileName, useURLs, config, useSAXSource);
                if (loaded instanceof List) {
                    wholeDirectory = true;
                    sources = (List)loaded;
                } else {
                    wholeDirectory = false;
                    sources = new ArrayList(1);
                    sources.add(loaded);
                }
                sources = preprocess(sources);
                if (wholeDirectory) {
                    if (outputFileName == null) {
                        quit("To process a directory, -o must be specified", 2);
                    } else if (outputFileName.equals(sourceFileName)) {
                        quit("Output directory must be different from input", 2);
                    } else {
                        outputFile = new File(outputFileName);
                        if (!outputFile.isDirectory()) {
                            quit("Input is a directory, but output is not", 2);
                        }
                    }
                }
            }

            if (outputFileName != null && !wholeDirectory) {
                outputFile = new File(outputFileName);
                if (outputFile.isDirectory()) {
                    quit("Output is a directory, but input is not", 2);
                }
            }

            if (useAssociatedStylesheet) {
                if (wholeDirectory) {
                    processDirectoryAssoc(sources, outputFile, parameterList,
                            initialMode, traceDestination);
                } else {
                    processFileAssoc((Source)sources.get(0), null, outputFile, parameterList,
                            initialMode, traceDestination);
                }
            } else {

                long startTime = (new Date()).getTime();

                PreparedStylesheet sheet = null;

                if (precompiled) {
                    try {
                        sheet = PreparedStylesheet.loadCompiledStylesheet(config, styleFileName);
                        if (showTime) {
                            long endTime = (new Date()).getTime();
                            System.err.println("Stylesheet loading time: " + (endTime - startTime) + " milliseconds");
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                } else {
                    Source styleSource;
                    XMLReader styleParser = null;
                    if (useURLs || styleFileName.startsWith("http:")
                            || styleFileName.startsWith("file:")) {
                        styleSource = config.getURIResolver().resolve(styleFileName, null);
                        if (styleSource == null) {
                            styleSource = config.getSystemURIResolver().resolve(styleFileName, null);
                        }
                    } else if (styleFileName.equals("-")) {
                        // take input from stdin
                        if (styleParserName == null) {
                            styleSource = new StreamSource(System.in);
                        } else if (Configuration.getPlatform().isJava()) {
                            styleParser = config.getStyleParser();
                            styleSource = new SAXSource(styleParser, new InputSource(System.in));
                        } else {
                            styleSource = new StreamSource(System.in);
                        }
                    } else {
                        File sheetFile = new File(styleFileName);
                        if (!sheetFile.exists()) {
                            quit("Stylesheet file " + sheetFile + " does not exist", 2);
                        }
                        if (styleParserName == null) {
                            styleSource = new StreamSource(sheetFile.toURI().toString());
                        } else {
                            InputSource eis = new InputSource(sheetFile.toURI().toString());
                            styleParser = config.getStyleParser();
                            styleSource = new SAXSource(styleParser, eis);
                        }
                    }

                    if (styleSource == null) {
                        quit("URIResolver for stylesheet file must return a Source", 2);
                    }

                    sheet = (PreparedStylesheet)factory.newTemplates(styleSource);
                    if (styleParser != null) {
                        config.reuseStyleParser(styleParser);
                        // pointless, because the Configuration won't be used again; but we want to set a good example
                    }
                    if (showTime) {
                        long endTime = now();
                        System.err.println("Stylesheet compilation time: " + (endTime - startTime) + " milliseconds");
                    }

                    if (explain) {
                        OutputStream explainOutput;
                        if (explainOutputFileName == null) {
                            explainOutput = System.err;
                        } else {
                            explainOutput = new FileOutputStream(new File(explainOutputFileName));
                        }
                        Properties props = new Properties();
                        props.setProperty(OutputKeys.METHOD, "xml");
                        props.setProperty(OutputKeys.INDENT, "yes");
                        props.setProperty(SaxonOutputKeys.INDENT_SPACES, "2");
                        Receiver diag = config.getSerializerFactory().getReceiver(
                                new StreamResult(explainOutput),
                                config.makePipelineConfiguration(),
                                props);
                        ExpressionPresenter expressionPresenter = new ExpressionPresenter(config, diag);
                        sheet.explain(expressionPresenter);
                        expressionPresenter.close();
                    }

                }

                if (wholeDirectory) {
                    processDirectory(sources, sheet, outputFile,
                            parameterList, initialTemplate, initialMode, traceDestination);
                } else {
                    Source source = (sources == null ? null : (Source)sources.get(0));
                    processFile(source, sheet, outputFile,
                            parameterList, initialTemplate, initialMode, traceDestination);
                }
                if (closeTraceDestination) {
                    traceDestination.close();
                }
            }
        } catch (TerminationException err) {
            quit(err.getMessage(), 1);
        } catch (TransformerConfigurationException err) {
            //err.printStackTrace();
            quit(err.getMessage(), 2);
        } catch (TransformerException err) {
            //err.printStackTrace();
            quit("Transformation failed: " + err.getMessage(), 2);
        } catch (TransformerFactoryConfigurationError err) {
            //err.printStackTrace();
            quit("Transformation failed: " + err.getMessage(), 2);
        } catch (Exception err2) {
            err2.printStackTrace();
            quit("Fatal error during transformation: " + err2.getClass().getName() + ": " + 
                    (err2.getMessage() == null ? " (no message)" : err2.getMessage()), 2);
        }


        //System.exit(0);
    }

    /**
     * Preprocess the list of sources. This method exists so that it can be
     * overridden in a subclass
     * @param sources the list of Source objects
     * @return a revised list of Source objects
     */

    public List preprocess(List sources) throws XPathException {
        return sources;
    }

    /**
     * Get the configuration.
     * @return the Saxon configuration
     */

    protected Configuration getConfiguration() {
        return config;
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

    /**
     * Load a document, or all the documents in a directory, given a filename or URL
     * @param sourceFileName the name of the source file or directory
     * @param useURLs true if the filename argument is to be treated as a URI
     * @param config the Saxon configuration
     * @param useSAXSource true if the method should use a SAXSource rather than a StreamSource
     * @return if sourceFileName represents a single source document, return a Source object representing
     *         that document. If sourceFileName represents a directory, return a List containing multiple Source
     *         objects, one for each file in the directory.
     */

    public static Object loadDocuments(String sourceFileName, boolean useURLs,
                                       Configuration config, boolean useSAXSource)
            throws TransformerException {

        Source sourceInput;
        XMLReader parser;
        if (useURLs || sourceFileName.startsWith("http:") || sourceFileName.startsWith("file:")) {
            sourceInput = config.getURIResolver().resolve(sourceFileName, null);
            if (sourceInput == null) {
                sourceInput = config.getSystemURIResolver().resolve(sourceFileName, null);
            }
            return sourceInput;
        } else if (sourceFileName.equals("-")) {
            // take input from stdin
            if (useSAXSource) {
                parser = config.getSourceParser();
                sourceInput = new SAXSource(parser, new InputSource(System.in));
            } else {
                sourceInput = new StreamSource(System.in);
            }
            return sourceInput;
        } else {
            File sourceFile = new File(sourceFileName);
            if (!sourceFile.exists()) {
                quit("Source file " + sourceFile + " does not exist", 2);
            }
            if (sourceFile.isDirectory()) {
                parser = config.getSourceParser();
                List result = new ArrayList(20);
                String[] files = sourceFile.list();
                for (int f = 0; f < files.length; f++) {
                    File file = new File(sourceFile, files[f]);
                    if (!file.isDirectory()) {
                        if (useSAXSource) {
                            InputSource eis = new InputSource(file.toURI().toString());
                            sourceInput = new SAXSource(parser, eis);
                                // it's safe to use the same parser for each document, as they
                                // will be processed one at a time.
                        } else {
                            sourceInput = new StreamSource(file.toURI().toString());
                        }
                        result.add(sourceInput);
                    }
                }
                return result;
            } else {
                if (useSAXSource) {
                    InputSource eis = new InputSource(sourceFile.toURI().toString());
                    sourceInput = new SAXSource(config.getSourceParser(), eis);
                } else {
                    sourceInput = new StreamSource(sourceFile.toURI().toString());
                }
                return sourceInput;
            }
        }
    }

    /**
     * Process each file in the source directory using its own associated stylesheet
     *
     * @param sources       The sources in the directory to be processed
     * @param outputDir     The directory in which output files are to be
     *                      created
     * @param parameterList List of parameters to be supplied to each
     *                      transformation
     * @param initialMode   Initial mode for executing each
     *                      transformation
     * @param traceDestination output destination for fn:trace() calls
     * @throws Exception when any error occurs during a transformation
     */

    public void processDirectoryAssoc(List sources, File outputDir,
                                      ArrayList parameterList, String initialMode, PrintStream traceDestination)
            throws Exception {

        int failures = 0;
        for (int f = 0; f < sources.size(); f++) {
            Source source = (Source)sources.get(f);
            String localName = getLocalFileName(source);
            try {
                processFileAssoc(source, localName, outputDir, parameterList, initialMode, traceDestination);
            } catch (XPathException err) {
                failures++;
                System.err.println("While processing " + localName +
                        ": " + err.getMessage() + '\n');
            }
        }
        if (failures > 0) {
            throw new XPathException(failures + " transformation" +
                    (failures == 1 ? "" : "s") + " failed");
        }
    }

    /**
     * Make an output file in the output directory, with filename extension derived from the
     * media-type produced by the stylesheet
     *
     * @param directory The directory in which the file is to be created
     * @param localName The local name of the file within the
     *                  directory, excluding the file type suffix
     * @param sheet     The Templates object identifying the stylesheet -
     *                  used to determine the output method, and hence the suffix to be
     *                  used for the filename
     * @return The newly created file
     */

    private File makeOutputFile(File directory, String localName, Templates sheet) {
        String mediaType = sheet.getOutputProperties().getProperty(OutputKeys.MEDIA_TYPE);
        String suffix = ".xml";
        if ("text/html".equals(mediaType)) {
            suffix = ".html";
        } else if ("text/plain".equals(mediaType)) {
            suffix = ".txt";
        }
        String prefix = localName;
        if (localName.endsWith(".xml") || localName.endsWith(".XML")) {
            prefix = localName.substring(0, localName.length() - 4);
        }
        return new File(directory, prefix + suffix);
    }


    /**
     * Process a single source file using its associated stylesheet(s)
     *
     * @param sourceInput   Identifies the source file to be transformed
     * @param localName     The local name of the file within the
     *                      directory, excluding the file type suffix
     * @param outputFile    The output file to contain the results of the
     *                      transformation
     * @param parameterList List of parameters to be supplied to the
     *                      transformation
     * @param initialMode   Initial mode for executing the transformation
     * @param traceDestination Destination for trace output
     * @throws XPathException If the transformation fails
     */

    public void processFileAssoc(Source sourceInput, String localName, File outputFile,
                                 ArrayList parameterList, String initialMode, PrintStream traceDestination)
            throws TransformerException {
        if (showTime) {
            System.err.println("Processing " + sourceInput.getSystemId() + " using associated stylesheet");
        }
        long startTime = now();

        Source style = factory.getAssociatedStylesheet(sourceInput, null, null, null);
        Templates sheet = factory.newTemplates(style);
        if (showTime) {
            System.err.println("Prepared associated stylesheet " + style.getSystemId());
        }

        Controller controller =
                newController(sheet, parameterList, traceDestination, initialMode, null);

        File outFile = outputFile;

        if (outFile != null && outFile.isDirectory()) {
            outFile = makeOutputFile(outFile, localName, sheet);
        }

        StreamResult result =
                (outFile == null ? new StreamResult(System.out) : new StreamResult(outFile.toURI().toString()));

        try {
            controller.transform(sourceInput, result);
        } catch (TerminationException err) {
            throw err;
        } catch (XPathException err) {
            // The error message will already have been displayed; don't do it twice
            throw new XPathException("Run-time errors were reported");
        }

        if (showTime) {
            long endTime = now();
            System.err.println("Execution time: " + (endTime - startTime) + " milliseconds");
        }
    }

    /**
     * Create a new Controller. This method is protected so it can be overridden in a subclass, allowing additional
     * options to be set on the Controller
     * @param sheet The Templates object representing the compiled stylesheet
     * @param parameterList A list of "keyword=value" pairs representing parameter values, in their original
     * format from the command line, including any initial "+" or "!" qualifier
     * @param traceDestination destination for trace output
     * @param initialMode the initial mode for the transformation, as a Clark name. Can be null
     * @param initialTemplate the name of the initial template for the transformation, as a Clark name. Can be null
     * @return the newly constructed Controller to be used for the transformation
     * @throws TransformerException if any error occurs
     */

    protected Controller newController(
            Templates sheet, ArrayList parameterList, PrintStream traceDestination,
            String initialMode, String initialTemplate) throws TransformerException {
        Controller controller = (Controller)sheet.newTransformer();
        setParams(controller, parameterList);
        controller.setTraceFunctionDestination(traceDestination);
        if (initialMode != null) {
            controller.setInitialMode(initialMode);
        }
        if (initialTemplate != null) {
            controller.setInitialTemplate(initialTemplate);
        }
        return controller;
    }

    /**
     * Get current time in milliseconds
     * @return the current time in milliseconds since 1970
     */

    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Process each file in the source directory using the same supplied stylesheet
     *
     * @param sources       The sources in the directory to be processed
     * @param sheet         The Templates object identifying the stylesheet
     * @param outputDir     The directory in which output files are to be
     *                      created
     * @param parameterList List of parameters to be supplied to each
     *                      transformation
     * @param initialTemplate Initial template for executing each
     *                      transformation
     * @param initialMode   Initial mode for executing each
     *                      transformation
     * @param traceDestination Destination for output from fn:trace() calls
     * @throws XPathException when any error occurs during a
     *                        transformation
     */

    public void processDirectory(List sources, Templates sheet, File outputDir, ArrayList parameterList,
                                 String initialTemplate, String initialMode, PrintStream traceDestination)
            throws TransformerException {
        int failures = 0;
        for (int f = 0; f < sources.size(); f++) {
            Source source = (Source)sources.get(f);
            String localName = getLocalFileName(source);
            try {
                File outputFile = makeOutputFile(outputDir, localName, sheet);
                processFile(source, sheet, outputFile, parameterList, initialTemplate, initialMode, traceDestination);
            } catch (XPathException err) {
                failures++;
                System.err.println("While processing " + localName + ": " + err.getMessage() + '\n');
            }
        }
        if (failures > 0) {
            throw new XPathException(failures + " transformation" +
                    (failures == 1 ? "" : "s") + " failed");
        }
    }

    private static String getLocalFileName(Source source) {
        try {
            String path = new URI(source.getSystemId()).getPath();
            while (true) {
                int sep = path.indexOf('/');
                if (sep < 0) {
                    return path;
                } else {
                    path = path.substring(sep + 1);
                }
            }
        } catch (URISyntaxException err) {
            throw new IllegalArgumentException(err.getMessage());
        }
    }

    /**
     * Process a single file using a supplied stylesheet
     *
     * @param source        The source XML document to be transformed (maybe null if an initial template
     *                      is specified)
     * @param sheet         The Templates object identifying the stylesheet
     * @param outputFile    The output file to contain the results of the
     *                      transformation
     * @param parameterList List of parameters to be supplied to the
     *                      transformation
     * @param initialTemplate Initial template for executing each
     *                      transformation
     * @param initialMode   Initial mode for executing the transformation
     * @param traceDestination Destination for output from fn:trace() function
     * @throws org.orbeon.saxon.trans.XPathException
     *          If the transformation fails
     */

    public void processFile(Source source, Templates sheet, File outputFile, ArrayList parameterList,
                            String initialTemplate, String initialMode, PrintStream traceDestination)
            throws TransformerException {

        long totalTime = 0;
        int runs = 0;
        for (int r = 0; r < repeat; r++) {      // repeat is for internal testing/timing
            if (showTime) {
                String msg = "Processing ";
                if (source != null) {
                    msg += source.getSystemId();
                } else {
                    msg += " (no source document)";
                }
                if (initialMode != null) {
                    msg += " initial mode = " + initialMode;
                }
                if (initialTemplate != null) {
                    msg += " initial template = " + initialTemplate;
                }
                System.err.println(msg);
            }
            long startTime = now();
            runs++;
            Controller controller =
                    newController(sheet, parameterList, traceDestination, initialMode, initialTemplate);

            Result result =
                    (outputFile == null ?
                    new StreamResult(System.out) :
                    new StreamResult(outputFile.toURI().toString()));

            try {
                controller.transform(source, result);
            } catch (TerminationException err) {
                throw err;
            } catch (XPathException err) {
                // The message will already have been displayed; don't do it twice
                if (!err.hasBeenReported()) {
                    err.printStackTrace();
                }
                throw new XPathException("Run-time errors were reported");
            }

            long endTime = now();
            totalTime += (endTime - startTime);
            if (showTime) {
                System.err.println("Execution time: " + (endTime - startTime) + " milliseconds");
                System.err.println("Memory used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
                config.getNamePool().statistics();
                if (repeat > 1) {
                    System.err.println("-------------------------------");
                    Runtime.getRuntime().gc();
                }
            }
            if (repeat == 999999 && totalTime > 60000) {
                break;
            }
        }
        if (repeat > 1) {
            System.err.println("*** Average execution time over " + runs + " runs: " + (totalTime / runs) + "ms");
        }
    }

    /**
     * Supply the requested parameters to the transformer. This method is protected so that it can
     * be overridden in a subclass.
     *
     * @param controller  The controller to be used for the transformation
     * @param parameterList A list of "keyword=value" pairs representing parameter values, in their original
     * format from the command line, including any initial "+" or "!" qualifier
     */
    protected void setParams(Controller controller, ArrayList parameterList)
            throws TransformerException {
        for (int i = 0; i < parameterList.size(); i++) {
            String arg = (String)parameterList.get(i);
            int eq = arg.indexOf("=");
            String argname = arg.substring(0, eq);
            String argvalue = (eq == arg.length()-1 ? "" : arg.substring(eq + 1));
            if (argname.startsWith("!")) {
                // parameters starting with "!" are taken as output properties
                controller.setOutputProperty(argname.substring(1), argvalue);
            } else if (argname.startsWith("+")) {
                // parameters starting with "+" are taken as input documents
                Object sources = loadDocuments(argvalue, useURLs, config, true);
                controller.setParameter(argname.substring(1), sources);
            } else {
                controller.setParameter(argname, new UntypedAtomicValue(argvalue));
            }
        }
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
        if (!showTime) {
            System.err.println(config.getProductTitle());
        }
        System.err.println("Usage: see http://www.saxonica.com/documentation/using-xsl/commandline.html");
        System.err.println("Options: ");
        System.err.println("  -a                    Use xml-stylesheet PI, not style-doc argument");
        System.err.println("  -c:filename           Use compiled stylesheet from file");
        System.err.println("  -cr:classname         Use collection URI resolver class");
        System.err.println("  -dtd:on|off           Validate using DTD");
        System.err.println("  -expand:on|off        Expand defaults defined in schema/DTD");
        System.err.println("  -explain[:filename]   Display compiled expression tree");
        System.err.println("  -ext:on|off           Allow|Disallow external Java functions");
        System.err.println("  -im:modename          Initial mode");
        System.err.println("  -it:template          Initial template");
        System.err.println("  -l:on|off             Line numbering for source document");
        System.err.println("  -m:classname          Use message receiver class");
        System.err.println("  -o:filename           Output file or directory");
        System.err.println("  -or:classname         Use OutputURIResolver class");
        System.err.println("  -outval:recover|fatal Handling of validation errors on result document");
        System.err.println("  -p:on|off             Recognize URI query parameters");
        System.err.println("  -r:classname          Use URIResolver class");
        System.err.println("  -repeat:N             Repeat N times for performance measurement");
        System.err.println("  -s:filename           Initial source document");
        System.err.println("  -sa                   Schema-aware transformation");
        System.err.println("  -strip:all|none|ignorable      Strip whitespace text nodes");
        System.err.println("  -t                    Display version and timing information");
        System.err.println("  -T[:classname]        Use TraceListener class");
        System.err.println("  -TJ                   Trace calls to external Java functions");
        System.err.println("  -tree:tiny|linked     Select tree model");
        System.err.println("  -traceout:file|#null  Destination for fn:trace() output");
        System.err.println("  -u                    Names are URLs not filenames");
        System.err.println("  -val:strict|lax       Validate using schema");
        System.err.println("  -versionmsg:on|off    Warn when using XSLT 1.0 stylesheet");
        System.err.println("  -warnings:silent|recover|fatal  Handling of recoverable errors");
        System.err.println("  -x:classname          Use specified SAX parser for source file");
        System.err.println("  -xi:on|off            Expand XInclude on all documents");
        System.err.println("  -xmlversion:1.0|1.1   Version of XML to be handled");
        System.err.println("  -xsd:file;file..      Additional schema documents to be loaded");
        System.err.println("  -xsdversion:1.0|1.1   Version of XML Schema to be used");
        System.err.println("  -xsiloc:on|off        Take note of xsi:schemaLocation");
        System.err.println("  -xsl:filename         Stylesheet file");
        System.err.println("  -y:classname          Use specified SAX parser for stylesheet");
        System.err.println("  -?                    Display this message ");
        System.err.println("  param=value           Set stylesheet string parameter");
        System.err.println("  +param=filename       Set stylesheet document parameter");
        System.err.println("  !option=value         Set serialization option");
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
