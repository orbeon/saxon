package org.orbeon.saxon.dotnet;

import cli.System.Environment;
import cli.System.Globalization.CultureInfo;
import cli.System.Reflection.Assembly;
import cli.System.Reflection.AssemblyName;
import cli.System.Uri;
import cli.System.Xml.*;
import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.functions.FunctionLibraryList;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pull.PullProvider;
import org.orbeon.saxon.pull.PullSource;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Implementation of the Platform interface containing methods appropriate to the .NET platform
 */

public class DotNetPlatform implements Platform {

    public DotNetPlatform(){};

    private Configuration config;
    private DotNetExtensionFunctionFactory extensionFunctionFactory;

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config) {
        this.config = config;
        config.setURIResolver(new DotNetURIResolver(new XmlUrlResolver()));
        config.setModuleURIResolver(new DotNetStandardModuleURIResolver(config, new XmlUrlResolver()));
        extensionFunctionFactory = new DotNetExtensionFunctionFactory(config);
    }

    /**
     * Construct an absolute URI from a relative URI and a base URI
     *
     * @param relativeURI the relative URI
     * @param base        the base URI
     * @return the absolutized URI
     * @throws java.net.URISyntaxException
     */

    public URI makeAbsolute(String relativeURI, String base) throws URISyntaxException {

        // It's not entirely clear why the .NET product needs a different version of this method.
        // Possibly because of bugs in GNU classpath.

        try {
            if (false) {
                // dummy code to allow the exception to be caught
                throw new cli.System.UriFormatException();
            }
            XmlUrlResolver resolver = new XmlUrlResolver();
            Uri fulluri;
            if (base != null) {
                Uri baseUri = new Uri(base);
                fulluri = resolver.ResolveUri(baseUri, relativeURI);
            }
            else {
                fulluri = resolver.ResolveUri(null, relativeURI.replaceAll("file:", ""));
            }
            return new URI(fulluri.ToString());
        } catch (cli.System.UriFormatException e) {
            throw new URISyntaxException(base + " + " + relativeURI, e.getMessage());
        }
    }

    /**
     * Get the platform version
     */

    public String getPlatformVersion() {
        return ".NET " + Environment.get_Version().ToString() +
                " on " + Environment.get_OSVersion().ToString();
    }

    /**
     * Get a suffix letter to add to the Saxon version number to identify the platform
     */

    public String getPlatformSuffix() {
        return "N";
    }

    /**
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     *
     * @param input the supplied StreamSource
     * @param validation indicates whether schema validation is required, adn in what mode
     * @param dtdValidation
     * @param stripspace
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     * input Source, if now special handling is required or possible. This implementation
     * always returns either a PullSource or the original StreamSource.
     */

    public Source getParserSource(StreamSource input, int validation, boolean dtdValidation, int stripspace) {
        InputStream is = input.getInputStream();
        if (is != null) {
            if (is instanceof DotNetInputStream) {
                XmlReader parser = new XmlTextReader(input.getSystemId(),
                        ((DotNetInputStream)is).getUnderlyingStream());
                ((XmlTextReader)parser).set_WhitespaceHandling(WhitespaceHandling.wrap(WhitespaceHandling.All));
                ((XmlTextReader)parser).set_Normalization(true);
                
                // Always need a validating parser, because that's the only way to get entity references expanded
                parser = new XmlValidatingReader(parser);
                if (dtdValidation) {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.DTD));
                } else {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.None));
                }
                PullProvider provider = new DotNetPullProvider(parser);
                //provider = new PullTracer(provider);
                PullSource ps = new PullSource(provider);
                //System.err.println("Using PullSource(stream)");
                ps.setSystemId(input.getSystemId());
                if (validation == Validation.DEFAULT) {
                    return ps;
                } else {
                    AugmentedSource as = AugmentedSource.makeAugmentedSource(ps);
                    as.setSchemaValidationMode(validation);
                    return as;
                }
            } else {
                return input;
            }
        }
        Reader reader = input.getReader();
        if (reader != null) {
            if (reader instanceof DotNetReader) {
                XmlReader parser = new XmlTextReader(input.getSystemId(),
                        ((DotNetReader)reader).getUnderlyingTextReader());
                ((XmlTextReader)parser).set_Normalization(true);
                ((XmlTextReader)parser).set_WhitespaceHandling(WhitespaceHandling.wrap(WhitespaceHandling.All));

                // Always need a validating parser, because that's the only way to get entity references expanded
                parser = new XmlValidatingReader(parser);
                if (dtdValidation) {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.DTD));
                } else {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.None));
                }
                PullSource ps = new PullSource(new DotNetPullProvider(parser));
                //System.err.println("Using PullSource(reader)");
                ps.setSystemId(input.getSystemId());
                if (validation == Validation.DEFAULT) {
                    return ps;
                } else {
                    AugmentedSource as = AugmentedSource.makeAugmentedSource(ps);
                    as.setSchemaValidationMode(validation);
                    return as;
                }
            } else {
                return input;
            }
        }
        String uri = input.getSystemId();
        if (uri != null) {
            try {
                Source r = config.getURIResolver().resolve(uri, null);
                if (r == null) {
                    return input;
                } else if (r instanceof AugmentedSource) {
                    Source r2 = ((AugmentedSource)r).getContainedSource();
                    if (r2 instanceof StreamSource) {
                        r2 = getParserSource((StreamSource)r2, validation, dtdValidation, stripspace);
                        // TODO: preserve the r.pleaseCloseAfterUse() flag
                        return r2;
                    } else {
                        return r2;
                    }
                } else if (r instanceof StreamSource && r != input) {
                    return getParserSource((StreamSource)r, validation, dtdValidation, stripspace);
                } else {
                    return r;
                }
            } catch (TransformerException err) {
                return input;
            }
        }
        return input;
    }

    /**
     * Create a compiled regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param isXPath set to true if this is an XPath regular expression, false if it is XML Schema
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws XPathException if the syntax of the regular expression or flags is incorrect
     * @return the compiled regular expression
     */

    public RegularExpression compileRegularExpression(CharSequence regex, boolean isXPath, CharSequence flags)
    throws XPathException {
        return new DotNetRegularExpression(regex, isXPath, flags);
    }

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=ed-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     * @param config the configuration object
     * @param props the desired properties of the collation
     * @return a collation with these properties
     * @throws XPathException if a fatal error occurs
     */

    public Comparator makeCollation(Configuration config, Properties props) throws XPathException {
        return DotNetCollationFactory.makeCollation(config, props);
    }

    /**
     * Given a collation, determine whether it is capable of returning collation keys.
     * The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     *
     * @param collation the collation, provided as a Comparator
     * @return true if this collation can supply collation keys
     */

    public boolean canReturnCollationKeys(Comparator collation) {
        return collation instanceof DotNetComparator ||
                collation instanceof CodepointCollator;
    }

    /**
     * Given a collation, get a collation key. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     *
     * @throws ClassCastException if the collation is not one that is capable of supplying
     *                            collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(Comparator collation, String value) {
        if (collation instanceof CodepointCollator) {
            return value;
        }
        return ((DotNetComparator)collation).getCollationKey(value);
    }

    /**
     * Define an ExtensionFunctionFactory to override the standard code for generating .NET
     * ExtensionFunction calls on the expression tree
     */

    public void setExtensionFunctionFactory(DotNetExtensionFunctionFactory factory) {
        this.extensionFunctionFactory = factory;
    }

    /**
     * Get the ExtensionFunctionFactory used for generating .NET ExtensionFunction calls on the
     * expression tree
     */

    public DotNetExtensionFunctionFactory getExtensionFunctionFactory() {
        return extensionFunctionFactory;
    }

    /**
     * Add platform-specific function libraries to the function library list
     */

    public void addFunctionLibraries(FunctionLibraryList list, Configuration config) {
        list.addFunctionLibrary(new DotNetExtensionLibrary(config));
        list.addFunctionLibrary(config.getExtensionBinder());
    }

    /**
     * Dynamically load a .NET class with a given name, starting with a URI that contains information
     * about the type and the assembly
     * @param uri A URI in the form
     * <code>clitype:Full.Type.Name?assembly=name;
     */

    public cli.System.Type dynamicLoad(String uri, boolean trace) throws XPathException {
        if (uri.startsWith("clitype:")) {
            uri = uri.substring(8);
        } else {
            if (trace) {
                System.err.println("Unrecognized .NET external URI: " + uri);
            }
            throw new DynamicError("Unrecognized .NET external URI: " + uri);
        }
        String typeName;
        String queryParams;
        int q = uri.indexOf('?');
        if (q == 0 || q == uri.length()-1) {
            if (trace) {
                System.err.println("Misplaced '?' in " + uri);
            }
            throw new DynamicError("Misplaced '?' in " + uri);
        }
        if (q > 0) {
            typeName = uri.substring(0, q);
            queryParams = uri.substring(q+1);
        } else {
            typeName = uri;
            queryParams = "";
        }
        if ("".equals(queryParams)) {
            cli.System.Type type = cli.System.Type.GetType(typeName);
            if (type == null && trace) {
                try {
                    if (false) throw new cli.System.TypeLoadException();
                    cli.System.Type type2 = cli.System.Type.GetType(typeName, true);
                } catch (Exception err) {
                    System.err.println("Failed to load type " + typeName + ": " + err.getMessage());
                    return null;
                } catch (cli.System.TypeLoadException err) {
                    System.err.println("Failed to load type " + typeName + ": " + err.getMessage());
                    return null;
                }
                System.err.println("Failed to load type " + typeName);
            }
            return type;
        } else {
            AssemblyName aname = new AssemblyName();
            String loadFrom = null;
            String partialName = null;
            StringTokenizer tok = new StringTokenizer(queryParams, ";&");
            while (tok.hasMoreTokens()) {
                String kv = tok.nextToken();
                int eq = kv.indexOf('=');
                if (eq <= 0) {
                    if (trace) {
                        System.err.println("Bad keyword=value pair in " + kv);
                    }
                    throw new DynamicError("Bad keyword=value pair in " + kv);
                }
                String keyword = kv.substring(0, eq);
                String value = kv.substring(eq+1);
                if (keyword.equals("asm")) {
                    aname.set_Name(value);
                } else if (keyword.equals("ver")) {
                    try {
                        aname.set_Version(new cli.System.Version(value));
                    } catch (Exception err) {
                        if (trace) {
                            System.err.println("Invalid version " + kv);
                        }
                        throw new DynamicError("Invalid version " + kv);
                    }
                } else if (keyword.equals("loc")) {
                    try {
                        if (!"".equals(value)) {
                            aname.set_CultureInfo(new CultureInfo(value));
                        }
                    } catch (Exception err) {
                        if (trace) {
                            System.err.println("Invalid culture info " + kv);
                        }
                        throw new DynamicError("Invalid culture info " + kv);
                    }
                } else if (keyword.equals("sn")) {
                    if (value.length() != 16) {
                        if (trace) {
                            System.err.println("Strong name must be 16 hex digits");
                        }
                        throw new DynamicError("Strong name must be 16 hex digits " + kv);
                    }
                    byte[] sn = new byte[16];
                    String hex = "01234356789abcdefABCDEF";
                    for (int i=0; i<8; i++) {
                        int h1 = hex.indexOf(value.charAt(i*2));
                        if (h1 < 0) {
                            if (trace) {
                                System.err.println("Invalid hex digit in strong name");
                            }
                            throw new DynamicError("Invalid hex digit in strong name " + kv);
                        }
                        if (h1 > 15) {
                            h1 -= 6;
                        }
                        int h2 = hex.indexOf(value.charAt(i*2 + 1));
                        if (h2 < 0) {
                            if (trace) {
                                System.err.println("Invalid hex digit in strong name");
                            }
                            throw new DynamicError("Invalid hex digit in strong name " + kv);
                        }
                        if (h2 > 15) {
                            h2 -= 6;
                        }
                        sn[i] = (byte)((h1<<4 | h2) & 0xff);
                    }
                    aname.SetPublicKeyToken(sn);

                } else if (keyword.equals("from")) {
                    loadFrom = value;
                } else if (keyword.equals("partialname")) {
                    partialName = value;
                } else if (trace) {
                    System.err.println("Unrecognized keyword in URI: " + keyword + " (ignored)");
                }
            }
            Assembly asm;
            try {
                if (false) throw new cli.System.IO.FileNotFoundException();
                if (partialName != null) {
                    asm = Assembly.LoadWithPartialName(partialName);
                } else if (loadFrom != null) {
                    // TODO: allow the URI to be relative to the base URI of the stylesheet
                    asm = Assembly.LoadFrom(loadFrom);
                } else {
                    asm = Assembly.Load(aname);
                }
                if (trace) {
                    System.err.println("Assembly + " + aname + " successfully loaded");
                }
            } catch (Exception err) {
                throw new DynamicError("Failed to load assembly " + aname + ": " + err.getMessage());
            } catch (cli.System.IO.FileNotFoundException err) {
                throw new DynamicError("Failed to load assembly " + aname + ": " + err.getMessage());
            }
            cli.System.Type type = asm.GetType(typeName);
            if (type == null) {
                if (trace) {
                    System.err.println("Type " + typeName + " not found in assembly");
                }
                throw new DynamicError("Type " + typeName + " not found in assembly");
            }
            return type;
        }
    }

    public SchemaType getExternalObjectType(String uri, String localName) {
        if (uri.equals(NamespaceConstant.DOT_NET_TYPE)) {
            return new DotNetExternalObjectType(cli.System.Type.GetType(localName), config);
        } else {
            throw new IllegalArgumentException("Type is not in .NET namespace");
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
// The Initial Developer of the Original Code is Michael H. Kay, based on code written by
// M David Peterson.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//


