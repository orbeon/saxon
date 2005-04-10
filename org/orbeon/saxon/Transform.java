package net.sf.saxon;
import net.sf.saxon.event.Builder;
import net.sf.saxon.instruct.TerminationException;
import net.sf.saxon.om.Validation;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.UntypedAtomicValue;
import org.xml.sax.InputSource;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This <B>Transform</B> class is the entry point to the Saxon XSLT Processor. This
 * class is provided to control the processor from the command line.<p>
 *
 * The XSLT syntax supported conforms to the W3C XSLT 1.0 and XPath 1.0 recommendation.
 * Only the transformation language is implemented (not the formatting objects).
 * Saxon extensions are documented in the file extensions.html
 *
 * @author Michael H. Kay
 */

public class Transform {

	protected TransformerFactoryImpl factory = new TransformerFactoryImpl();
    protected Configuration config;
    protected boolean useURLs = false;
    protected boolean showTime = false;
    protected int repeat = 1;
    String sourceParserName = null;

    /**
     * Main program, can be used directly from the command line.
     * <p>The format is:</P>
     * <p>java net.sf.saxon.Transform [options] <I>source-file</I> <I>style-file</I> &gt;<I>output-file</I></P>
     * <p>followed by any number of parameters in the form {keyword=value}... which can be
     * referenced from within the stylesheet.</p>
     * <p>This program applies the XSL style sheet in style-file to the source XML document in source-file.</p>
     *
     * @param args List of arguments supplied on operating system command line
     * @exception java.lang.Exception Indicates that a compile-time or
     *     run-time error occurred
     */

    public static void main (String args[])
        throws java.lang.Exception
    {
        // the real work is delegated to another routine so that it can be used in a subclass
        (new Transform()).doMain(args, "java net.sf.saxon.Transform");
    }

    /**
     * Set the configuration in the TransformerFactory. This is designed to be
     * overridden in a subclass
     */

    protected void setFactoryConfiguration() {
        Configuration config = new Configuration();
        factory.setConfiguration(config);
        // In basic XSLT, all nodes are untyped by definition
        config.setAllNodesUntyped(true);
    }

    /**
     * Support method for main program. This support method can also be invoked from subclasses
     * that support the same command line interface
     *
     * @param args the command-line arguments
     * @param name name of the class, to be used in error messages
     */

    protected void doMain(String args[], String name) {


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

        setFactoryConfiguration();
        config = factory.getConfiguration();
        boolean schemaAware = config.isSchemaAware(Configuration.XSLT);

				// Check the command-line arguments.

        try {
            int i = 0;
            while (true) {
                if (i>=args.length) badUsage(name, "No source file name");

                if (args[i].charAt(0)=='-') {

                    if (args[i].equals("-a")) {
                        useAssociatedStylesheet = true;
                        i++;
                    }

                    else if (args[i].equals("-c")) {
                        precompiled = true;
                        i++;
                    }

                    else if (args[i].equals("-ds")) {
                        factory.setAttribute(
                            FeatureKeys.TREE_MODEL,
                            new Integer(Builder.STANDARD_TREE));
                        i++;
                    }

                    else if (args[i].equals("-dt")) {
                        factory.setAttribute(
                            FeatureKeys.TREE_MODEL,
                            new Integer(Builder.TINY_TREE));
                        i++;
                    }

                    else if (args[i].equals("-im")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No initial mode after -im");
                        initialMode = args[i++];
                    }

                    else if (args[i].equals("-it")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No initial template after -it");
                        initialTemplate = args[i++];
                    }

                    else if (args[i].equals("-l")) {
                        factory.setAttribute(
                            FeatureKeys.LINE_NUMBERING,
                                Boolean.valueOf(true));
                        i++;
                    }

                    else if (args[i].equals("-novw")) {
                        factory.setAttribute(
                            FeatureKeys.VERSION_WARNING,
                                Boolean.valueOf(false));
                        i++;
                    }


                    else if (args[i].equals("-u")) {
                        useURLs = true;
                        i++;
                    }

                    else if (args[i].equals("-v")) {
                        factory.setAttribute(
                                FeatureKeys.DTD_VALIDATION,
                                Boolean.valueOf(true));
                        dtdValidation = true;
                        i++;
                    }

                    else if (args[i].equals("-vw")) {
                       if (schemaAware) {
                            factory.setAttribute(
                                    FeatureKeys.VALIDATION_WARNINGS,
                                    Boolean.valueOf(true));
                        } else {
                            quit("The -vw option requires a schema-aware processor", 2);
                        }
                        i++;
                    }

                    else if (args[i].equals("-val")) {
                        if (schemaAware) {
                            factory.setAttribute(
                                    FeatureKeys.SCHEMA_VALIDATION,
                                    new Integer(Validation.STRICT));
                        } else {
                            quit("The -val option requires a schema-aware processor", 2);
                        }
                        i++;
                    }

                    else if (args[i].equals("-vlax")) {
                        if (schemaAware) {
                            factory.setAttribute(
                                    FeatureKeys.SCHEMA_VALIDATION,
                                    new Integer(Validation.LAX));
                        } else {
                            quit("The -vlax option requires a schema-aware processor", 2);
                        }
                        i++;
                    }

                    else if (args[i].equals("-t")) {
                        System.err.println(config.getProductTitle());
                        System.err.println("Java version " + System.getProperty("java.version"));
                        factory.setAttribute(
                                FeatureKeys.TIMING,
                                Boolean.valueOf(true));

                        //Loader.setTracing(true);
                        showTime = true;
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

                    else if (args[i].equals("-o")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No output file name");
                        outputFileName = args[i++];
                    }

                    else if (args[i].equals("-x")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No source parser class");
                        sourceParserName = args[i++];
                        factory.setAttribute(
                                FeatureKeys.SOURCE_PARSER_CLASS,
                                sourceParserName);
                    }

                    else if (args[i].equals("-y")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No style parser class");
                        styleParserName = args[i++];
                        factory.setAttribute(
                                FeatureKeys.STYLE_PARSER_CLASS,
                                styleParserName);
                    }

                    else if (args[i].equals("-r")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No URIResolver class");
                        String r = args[i++];
                        factory.setURIResolver(config.makeURIResolver(r));
                    }

                    else if (args[i].equals("-T")) {
                        i++;
                        TraceListener traceListener = new net.sf.saxon.trace.XSLTTraceListener();
                        factory.setAttribute(
                                FeatureKeys.TRACE_LISTENER,
                                traceListener);
                        factory.setAttribute(
                                FeatureKeys.LINE_NUMBERING,
                                Boolean.TRUE);
                    }

                    else if (args[i].equals("-TP")) {
                        i++;
                        TraceListener traceListener = new net.sf.saxon.trace.TimedTraceListener();
                        factory.setAttribute(
                                FeatureKeys.TRACE_LISTENER,
                                traceListener);
                        factory.setAttribute(
                                FeatureKeys.LINE_NUMBERING,
                                Boolean.TRUE);
                    }

                    else if (args[i].equals("-TJ")) {
                        i++;
                        factory.setAttribute(
                                FeatureKeys.TRACE_EXTERNAL_FUNCTIONS,
                                Boolean.TRUE);
                    }

                    else if (args[i].equals("-TL")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No TraceListener class");
                        TraceListener traceListener = config.makeTraceListener(args[i++]);
                        factory.setAttribute(
                                FeatureKeys.TRACE_LISTENER,
                                traceListener);
                        factory.setAttribute(
                                FeatureKeys.LINE_NUMBERING,
                                Boolean.TRUE);
                    }

                    else if (args[i].equals("-w0")) {
                        i++;
                        factory.setAttribute(
                                FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.RECOVER_SILENTLY));
                    }
                    else if (args[i].equals("-w1")) {
                        i++;
                        factory.setAttribute(
                                FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.RECOVER_WITH_WARNINGS));
                    }
                    else if (args[i].equals("-w2")) {
                        i++;
                        factory.setAttribute(
                                FeatureKeys.RECOVERY_POLICY,
                                new Integer(Configuration.DO_NOT_RECOVER));
                    }

                    else if (args[i].equals("-m")) {
                        i++;
                        if (args.length < i+2) badUsage(name, "No message Emitter class");
                        factory.setAttribute(
                                FeatureKeys.MESSAGE_EMITTER_CLASS,
                                args[i++]);
                    }

                    else if (args[i].equals("-noext")) {
                        i++;
                        factory.setAttribute(
                                FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS,
                                Boolean.valueOf(false));
                    }

                    else if (args[i].equals("-?")) {
                        badUsage(name, "");
                    }

                    else if (args[i].equals("-")) {
                        break;
                        // this means take the source from standard input
                    }

                    else badUsage(name, "Unknown option " + args[i]);
                }

                else break;
            }

            if (initialTemplate!=null && useAssociatedStylesheet) {
                badUsage(name, "-it and -a options cannot be used together");
            }

            if (initialTemplate == null) {
                if (args.length < i+1 ) badUsage(name, "No source file name");
                sourceFileName = args[i++];
            }

            if (!useAssociatedStylesheet) {
                if (args.length < i+1 ) badUsage(name, "No stylesheet file name");
                styleFileName = args[i++];
            }

            for (int p=i; p<args.length; p++) {
                String arg = args[p];
                int eq = arg.indexOf("=");
                if (eq<1 || eq>=arg.length()-1) {
                    badUsage(name, "Bad param=value pair on command line: " + arg);
                }
                parameterList.add(arg);
            }

            config.displayLicenseMessage();

            List sources = null;
            if (initialTemplate==null) {
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
                    if (outputFileName==null) {
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

            if (outputFileName!=null && !wholeDirectory) {
                 outputFile = new File(outputFileName);
                 if (outputFile.isDirectory()) {
                            quit("Output is a directory, but input is not", 2);
                 }
            }

            if (useAssociatedStylesheet) {
                if (wholeDirectory) {
                    processDirectoryAssoc(sources, outputFile, parameterList, initialMode);
                } else {
                    processFileAssoc((Source)sources.get(0), null, outputFile, parameterList, initialMode);
                }
            } else {

                long startTime = (new Date()).getTime();

                Source styleSource;
                if (useURLs || styleFileName.startsWith("http:")
                                 || styleFileName.startsWith("file:")) {
                    styleSource = factory.getURIResolver().resolve(styleFileName, null);
                    if (styleSource==null) {
                        styleSource = new StandardURIResolver().resolve(styleFileName, null);
                    }
                } else if (styleFileName.equals("-")) {
                    // take input from stdin
                    if (styleParserName == null) {
                        styleSource = new StreamSource(System.in);
                    } else {
                        styleSource = new SAXSource(config.getStyleParser(), new InputSource(System.in));
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
                        styleSource = new SAXSource(config.getStyleParser(), eis);
                    }
                }

                if (styleSource==null) {
                    quit("URIResolver for stylesheet file must return a Source", 2);
                }

                PreparedStylesheet sheet = null;

                if (precompiled) {
                    try {
                        sheet = PreparedStylesheet.loadCompiledStylesheet(config, styleFileName);
                        if (showTime) {
                            long endTime = (new Date()).getTime();
                            System.err.println("Stylesheet loading time: " + (endTime-startTime) + " milliseconds");
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                } else {
                    sheet = (PreparedStylesheet)factory.newTemplates(styleSource);
                    if (showTime) {
                        long endTime = (new Date()).getTime();
                        System.err.println("Stylesheet compilation time: " + (endTime-startTime) + " milliseconds");
                    }
                }

                if (initialTemplate != null) {
                    execute(initialTemplate, sheet, outputFile, parameterList, initialMode);
                } else if (wholeDirectory) {
                    processDirectory(sources, sheet, outputFile, parameterList, initialMode);
                } else {
                    processFile((Source)sources.get(0), sheet, outputFile, parameterList, initialMode);
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
            quit("Fatal error during transformation: " + err2.getMessage(), 2);
        }


        //System.exit(0);
    }

    /**
     * Preprocess the list of sources. This method exists so that it can be
     * overridden in a subclass
     */

    public List preprocess(List sources) throws XPathException {
        return sources;
    }

    /**
     * Get the configuration.
     */

    protected Configuration getConfiguration() {
        return config;
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
     * Load a document, or all the documents in a directory, given a filename or URL
     * @return if sourceFileName represents a single source document, return a Source object representing
     * that document. If sourceFileName represents a directoru, return a List containing multiple Source
     * objects, one for each file in the directory.
     */

    public static Object loadDocuments(String sourceFileName, boolean useURLs, Configuration config, boolean useSAXSource)
    throws TransformerException {

        Source sourceInput;
        if (useURLs || sourceFileName.startsWith("http:") || sourceFileName.startsWith("file:")) {
            sourceInput = config.getURIResolver().resolve(sourceFileName, null);
            if (sourceInput==null) {
                sourceInput = new StandardURIResolver().resolve(sourceFileName, null);
            }
            return sourceInput;
        } else if (sourceFileName.equals("-")) {
            // take input from stdin
            if (useSAXSource) {
                sourceInput = new SAXSource(config.getSourceParser(), new InputSource(System.in));
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
                List result = new ArrayList(20);
                String[] files = sourceFile.list();
                for (int f=0; f<files.length; f++) {
                    File file = new File(sourceFile, files[f]);
                    if (!file.isDirectory()) {
                        if (useSAXSource) {
                            InputSource eis = new InputSource(file.toURI().toString());
                            sourceInput = new SAXSource(config.getSourceParser(), eis);
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
     * @param sources The sources in the directory to be processed
     * @param outputDir The directory in which output files are to be
     *      created
     * @param parameterList List of parameters to be supplied to each
     *      transformation
     * @param initialMode Initial mode for executing each
     *     transformation
     * @exception Exception when any error occurs during a transformation
     */

    public void processDirectoryAssoc(
        List sources, File outputDir, ArrayList parameterList, String initialMode)
        throws Exception {

        int failures = 0;
        for (int f=0; f<sources.size(); f++) {
            Source source = (Source)sources.get(f);
            String localName = getLocalFileName(source);
            try {
                processFileAssoc(source, localName, outputDir, parameterList, initialMode);
            } catch (XPathException err) {
                failures++;
                System.err.println("While processing " + localName +
                     ": " + err.getMessage() + '\n');
            }
        }
        if (failures>0) {
            throw new DynamicError(failures + " transformation" +
                 (failures==1?"":"s") + " failed");
        }
    }

    /**
     * Make an output file in the output directory, with filename extension derived from the
     * media-type produced by the stylesheet
     *
     * @param directory The directory in which the file is to be created
     * @param localName The local name of the file within the
     *     directory, excluding the file type suffix
     * @param sheet The Templates object identifying the stylesheet -
     *      used to determine the output method, and hence the suffix to be
     *     used for the filename
     * @return The newly created file
     */

    private File makeOutputFile(File directory, String localName,
                                 Templates sheet) {
        String mediaType = sheet.getOutputProperties().getProperty(
                                    OutputKeys.MEDIA_TYPE);
        String suffix = ".xml";
        if ("text/html".equals(mediaType)) {
            suffix = ".html";
        } else if ("text/plain".equals(mediaType)) {
            suffix = ".txt";
        }
        String prefix = localName;
        if (localName.endsWith(".xml") || localName.endsWith(".XML")) {
            prefix = localName.substring(0, localName.length()-4);
        }
        return new File(directory, prefix+suffix);
    }


    /**
     * Process a single source file using its associated stylesheet(s)
     *
     * @param sourceInput Identifies the source file to be transformed
     * @param localName The local name of the file within the
     *     directory, excluding the file type suffix
     * @param outputFile The output file to contain the results of the
     *      transformation
     * @param parameterList List of parameters to be supplied to the
     *     transformation
     * @param initialMode Initial mode for executing the transformation
     * @exception XPathException If the transformation fails
     */

    public void processFileAssoc(
        Source sourceInput, String localName, File outputFile, ArrayList parameterList, String initialMode)
        throws TransformerException
    {
        if (showTime) {
            System.err.println("Processing " + sourceInput.getSystemId() + " using associated stylesheet");
        }
        long startTime = (new Date()).getTime();

        Source style = factory.getAssociatedStylesheet(sourceInput, null, null, null);
		Templates sheet = factory.newTemplates(style);
        if (showTime) {
            System.err.println("Prepared associated stylesheet " + style.getSystemId());
        }

		Transformer instance = sheet.newTransformer();
		setParams(instance, parameterList);
		if (initialMode!=null) {
            ((Controller)instance).setInitialMode(initialMode);
        }

        File outFile = outputFile;

        if (outFile!=null && outFile.isDirectory()) {
            outFile = makeOutputFile(outFile, localName, sheet);
        }

        StreamResult result =
            (outFile==null ? new StreamResult(System.out) : new StreamResult(outFile.toURI().toString()));

        try {
            instance.transform(sourceInput, result);
        } catch (TerminationException err) {
            throw err;
        } catch (XPathException err) {
            // The error message will already have been displayed; don't do it twice
            throw new DynamicError("Run-time errors were reported");
        }

        if (showTime) {
            long endTime = (new Date()).getTime();
            System.err.println("Execution time: " + (endTime-startTime) + " milliseconds");
        }
    }

    /**
     * Process each file in the source directory using the same supplied stylesheet
     *
     * @param sources The sources in the directory to be processed
     * @param sheet The Templates object identifying the stylesheet
     * @param outputDir The directory in which output files are to be
     *      created
     * @param parameterList List of parameters to be supplied to each
     *      transformation
     * @param initialMode Initial mode for executing each
     *     transformation
     * @exception XPathException when any error occurs during a
     *     transformation
     */

    public void processDirectory(
        List sources, Templates sheet, File outputDir, ArrayList parameterList, String initialMode)
        throws TransformerException
    {
        int failures = 0;
        for (int f=0; f<sources.size(); f++) {
            Source source = (Source)sources.get(f);
            String localName = getLocalFileName(source);
            try {
                File outputFile = makeOutputFile(outputDir, localName, sheet);
                processFile(source, sheet, outputFile, parameterList, initialMode);
            } catch (XPathException err) {
                failures++;
                System.err.println("While processing " + localName + ": " + err.getMessage() + '\n');
            }
        }
        if (failures>0) {
            throw new DynamicError(failures + " transformation" +
                 (failures==1?"":"s") + " failed");
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
                    path = path.substring(sep+1);
                }
            }
        } catch (URISyntaxException err) {
            throw new IllegalArgumentException(err.getMessage());
        }
    }

    /**
     * Process a single file using a supplied stylesheet
     *
     * @param source The source XML document to be transformed
     * @param sheet The Templates object identifying the stylesheet
     * @param outputFile The output file to contain the results of the
     *      transformation
     * @param parameterList List of parameters to be supplied to the
     *     transformation
     * @param initialMode Initial mode for executing the transformation
     * @exception net.sf.saxon.trans.XPathException If the transformation fails
     */

    public void processFile(
        Source source, Templates sheet, File outputFile, ArrayList parameterList, String initialMode)
        throws TransformerException {

        for (int r=0; r<repeat; r++) {      // repeat is for internal testing/timing
            if (showTime) {
                System.err.println("Processing " + source.getSystemId());
            }
            long startTime = (new Date()).getTime();
            Transformer instance = sheet.newTransformer();
            setParams(instance, parameterList);
    		if (initialMode!=null) {
                ((Controller)instance).setInitialMode(initialMode);
            }
            if (outputFile==null) {
                ((Controller)instance).setBaseOutputURI(new File(System.getProperty("user.dir")).toURI().toString());
            }
            Result result =
                (outputFile==null ?
                    new StreamResult(System.out) :
                    new StreamResult(outputFile.toURI().toString()));

            try {
                instance.transform(source, result);
            } catch (TerminationException err) {
                throw err;
            } catch (XPathException err) {
                // The message will already have been displayed; don't do it twice
                throw new DynamicError("Run-time errors were reported");
            }

            if (showTime) {
                long endTime = (new Date()).getTime();
                System.err.println("Execution time: " + (endTime-startTime) + " milliseconds");
                System.err.println("Memory used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
                config.getNamePool().statistics();
                if (repeat>1) {
                    System.err.println("-------------------------------");
                    Runtime.getRuntime().gc();
                }
            }
        }
    }

    /**
     * Invoke a supplied stylesheet with no source document
     *
     * @param initialTemplate The entry point to the stylesheet
     * @param sheet The Templates object identifying the stylesheet
     * @param outputFile The output file to contain the results of the
     *      transformation
     * @param parameterList List of parameters to be supplied to the
     *     transformation
     * @param initialMode Initial mode for executing the transformation
     * @exception net.sf.saxon.trans.XPathException If the transformation fails
     */

    public void execute(
        String initialTemplate, Templates sheet, File outputFile, ArrayList parameterList, String initialMode)
        throws TransformerException {

        for (int r=0; r<repeat; r++) {      // repeat is for internal testing/timing
            if (showTime) {
                System.err.println("Calling template " + initialTemplate);
            }
            long startTime = (new Date()).getTime();
            Transformer instance = sheet.newTransformer();
            setParams(instance, parameterList);
    		if (initialMode!=null) {
                ((Controller)instance).setInitialMode(initialMode);
            }
            ((Controller)instance).setInitialTemplate(initialTemplate);
            Result result =
                (outputFile==null ?
                    new StreamResult(System.out) :
                    new StreamResult(outputFile.toURI().toString()));

            try {
                instance.transform(null, result);
            } catch (TerminationException err) {
                throw err;
            } catch (XPathException err) {
                // The message will already have been displayed; don't do it twice
                throw new DynamicError("Run-time errors were reported");
            }

            if (showTime) {
                long endTime = (new Date()).getTime();
                System.err.println("Execution time: " + (endTime-startTime) + " milliseconds");
            }
        }
    }

    /** Supply the requested parameters to the transformer
     *
     * @param t The transformer to be used for the transformation
     * @param parameterList List of parameters to be supplied to the
     *     transformation
     */
    private void setParams(Transformer t, ArrayList parameterList)
    throws TransformerException {
        for (int i=0; i<parameterList.size(); i++) {
            String arg = (String)parameterList.get(i);
            int eq = arg.indexOf("=");
			String argname = arg.substring(0,eq);
			if (argname.startsWith("!")) {
			    // parameters starting with "!" are taken as output properties
			    t.setOutputProperty(argname.substring(1), arg.substring(eq+1));
            } else if (argname.startsWith("+")) {
                // parameters starting with "+" are taken as input documents
                Object sources = loadDocuments(arg.substring(eq+1), useURLs, config, true);
                t.setParameter(argname.substring(1), sources);
			} else {
			    t.setParameter(argname, new UntypedAtomicValue(arg.substring(eq+1)));
			}
        }
    }

    /** Report incorrect usage of the command line, with a list of the options and arguments that are available
     *
     * @param name The name of the command being executed (allows subclassing)
     * @param message The error message
     */
    protected void badUsage(String name, String message) {
        if (!"".equals(message)) {
            System.err.println(message);
        }
        if (!showTime) {
            System.err.println(config.getProductTitle());
        }
        System.err.println("Usage: " + name + " [options] source-doc style-doc {param=value}...");
        System.err.println("Options: ");
        System.err.println("  -a              Use xml-stylesheet PI, not style-doc argument");
        System.err.println("  -c              Indicates that style-doc is a compiled stylesheet");
        System.err.println("  -ds             Use standard tree data structure");
        System.err.println("  -dt             Use tinytree data structure (default)");
        System.err.println("  -im modename    Start transformation in specified mode");
        System.err.println("  -it template    Start transformation by calling named template");
        System.err.println("  -l              Retain line numbers in source document tree");
        System.err.println("  -o filename     Send output to named file or directory");
        System.err.println("  -m classname    Use specified Emitter class for xsl:message output");
        System.err.println("  -novw           Suppress warning when running with an XSLT 1.0 stylesheet");
        System.err.println("  -r classname    Use specified URIResolver class");
        System.err.println("  -t              Display version and timing information");
        System.err.println("  -T              Set standard TraceListener");
        System.err.println("  -TJ             Trace calls to external Java functions");
        System.err.println("  -TL classname   Set a specific TraceListener");
        System.err.println("  -u              Names are URLs not filenames");
        System.err.println("  -v              Validate source documents using DTD");
        if (config.isSchemaAware(Configuration.XSLT)) {
            System.err.println("  -val            Validate source documents using schema");
            System.err.println("  -vlax           Lax validation of source documents using schema");
            System.err.println("  -vw             Treat validation errors on result document as warnings");
        }
        System.err.println("  -w0             Recover silently from recoverable errors");
        System.err.println("  -w1             Report recoverable errors and continue (default)");
        System.err.println("  -w2             Treat recoverable errors as fatal");
        System.err.println("  -x classname    Use specified SAX parser for source file");
        System.err.println("  -y classname    Use specified SAX parser for stylesheet");
        System.err.println("  -?              Display this message ");
        System.err.println("  param=value     Set stylesheet string parameter");
        System.err.println("  +param=file     Set stylesheet document parameter");
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
