package net.sf.saxon;
import net.sf.saxon.instruct.TerminationException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

/**
 * This <B>Compile</B> class provides a command-line interface allowing a
 * stylesheet to be compiled.<p>
 *
 * @author M.H.Kay
 */

public class Compile {

	private TransformerFactoryImpl factory = new TransformerFactoryImpl();

    private boolean showTime = false;

    /**
     * Main program, can be used directly from the command line.
     * <p>The format is:</P>
     * <p>java net.sf.saxon.Compile [options] <I>style-file</I> <I>output-file</I></P>
     * <p>This program compiles the XSL style sheet in style-file to the output-file.</p>
     *
     * @param args Arguments supplied on the command line
     * @exception java.lang.Exception Any compilation error occurs
     */

    public static void main (String args[])
        throws java.lang.Exception
    {
        // the real work is delegated to another routine so that it can be used in a subclass
        (new Compile()).doMain(args);
    }

    /**
     * Support method for main program. This support method can also be invoked from subclasses
     * that support the same command line interface
     *
     * @param args the command-line arguments
     */

    protected void doMain(String args[]) {

        String styleFileName;
        boolean useURLs = false;
        String outputFileName;

				// Check the command-line arguments.

        try {
            int i = 0;
            while (true) {
                if (i>=args.length) badUsage("No stylesheet file name");

                if (args[i].charAt(0)=='-') {

                    if (args[i].equals("-u")) {
                        useURLs = true;
                        i++;
                    }

                    else if (args[i].equals("-t")) {
                        System.err.println(factory.getConfiguration().getProductTitle());
                        System.err.println("Java version " + System.getProperty("java.version"));
                        factory.setAttribute(
                            FeatureKeys.TIMING,
                            Boolean.TRUE);

                        //Loader.setTracing(true);
                        showTime = true;
                        i++;
                    }

                    else if (args[i].equals("-y")) {
                        i++;
                        if (args.length < i+2) badUsage("No style parser class");
                        String styleParserName = args[i++];
                        factory.setAttribute(
                                FeatureKeys.STYLE_PARSER_CLASS,
                                styleParserName);
                    }

                    else if (args[i].equals("-r")) {
                        i++;
                        if (args.length < i+2) badUsage("No URIResolver class");
                        String r = args[i++];
                        factory.setURIResolver(makeURIResolver(r));
                    }

                    else badUsage("Unknown option " + args[i]);
                }

                else break;
            }

            if (args.length < i+1 ) badUsage("No stylesheet file name");
            styleFileName = args[i++];

            if (args.length < i+1 ) badUsage("No output file name");
            outputFileName = args[i++];


            long startTime = (new Date()).getTime();

            Source styleSource;
            if (useURLs || styleFileName.startsWith("http:")
                             || styleFileName.startsWith("file:")) {
                styleSource = factory.getURIResolver().resolve(styleFileName, null);

            } else {
                File sheetFile = new File(styleFileName);
                if (!sheetFile.exists()) {
                    quit("Stylesheet file " + sheetFile + " does not exist", 2);
                }
                InputSource eis = new InputSource(sheetFile.toURL().toString());
                styleSource = new SAXSource(factory.getConfiguration().getStyleParser(), eis);
            }

            if (styleSource==null) {
                quit("URIResolver for stylesheet file must return a Source", 2);
            }

            Templates sheet = factory.newTemplates(styleSource);

            if (showTime) {
                long endTime = (new Date()).getTime();
                System.err.println("Stylesheet compilation time: " + (endTime-startTime) + " milliseconds");
            }

            try {
                System.err.println("Serializing compiled stylesheet");
                ((PreparedStylesheet)sheet).setTargetNamePool(
                        ((PreparedStylesheet)sheet).getConfiguration().getNamePool());
                ObjectOutputStream oos = new ObjectOutputStream(
                                             new FileOutputStream(outputFileName));
                oos.writeObject(sheet);
                oos.close();
                System.err.println("Finished serializing stylesheet");
            } catch (Exception err) {
                err.printStackTrace();
            }

        } catch (TerminationException err) {
            quit(err.getMessage(), 1);
        } catch (XPathException err) {
            quit("Transformation failed: " + err.getMessage(), 2);
        } catch (TransformerFactoryConfigurationError err) {
            quit("Transformation failed: " + err.getMessage(), 2);
        } catch (Exception err2) {
            err2.printStackTrace();
        }

    }

    /**
     * Exit with a message
     *
     * @param message Message to be output
     * @param code Result code to be returned to the operating system
     */

    protected static void quit(String message, int code) {
        System.err.println(message);
        System.exit(code);
    }


    /** Output error message when incorrect command line options/arguments are used
     *
     * @param message Error message to be displayed
     */
    protected void badUsage(String message) {
        System.err.println(message);
        System.err.println(factory.getConfiguration().getProductTitle());
        System.err.println("Usage: java net.sf.saxon.Compile [options] stylesheet-file output-file");
        System.err.println("Options: ");
        System.err.println("  -r classname    Use specified URIResolver class");
        System.err.println("  -t              Display version and timing information");
        System.err.println("  -u              Names are URLs not filenames");
        System.err.println("  -y classname    Use specified SAX parser for stylesheet");
        System.err.println("  -?              Display this message ");
        System.exit(2);
    }

    /** Load a URIResolver given its class name
     *
     * @param className The fully-qualified name of the URIResolver class to
     *     be loaded
     * @exception XPathException The class specified is not an instance
     *     of javax.xml.transform.URIResolver
     * @return A URIResolver that is an instance of the specified class
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
