package net.sf.saxon;


/**
  * FeatureKeys defines a set of constants, names of Saxon configuration
  * options which can be supplied to the TransformerFactoryImpl interface
  *
  * @author Michael H. Kay
  */


public class FeatureKeys {

	/**
	* ALLOW_EXTERNAL_FUNCTIONS must be a Boolean; it determines whether calls to external functions are allowed
	*/

	public static final String ALLOW_EXTERNAL_FUNCTIONS =
	        "http://saxon.sf.net/feature/allow-external-functions";

	/**
	* TRACE_EXTERNAL_FUNCTIONS must be a Boolean; it determines whether the loading and binding of extension
     * functions is traced
	*/

	public static final String TRACE_EXTERNAL_FUNCTIONS =
	        "http://saxon.sf.net/feature/trace-external-functions";

	/**
	* TIMING must be an Boolean; it determines whether basic timing information is output to System.err
	*/

	public static final String TIMING =
	        "http://saxon.sf.net/feature/timing";

	/**
	* TREE_MODEL must be an Integer: Builder.STANDARD_TREE or Builder.TINY_TREE
	*/

	public static final String TREE_MODEL =
	        "http://saxon.sf.net/feature/treeModel";

	/**
	* TRACE_LISTENER must be a class that implements net.sf.saxon.trace.TraceListener
	*/

	public static final String TRACE_LISTENER =
	        "http://saxon.sf.net/feature/traceListener";

	/**
	* LINE_NUMBERING must be a Boolean(); it determines whether line numbers are maintained for the
     * source document
	*/

	public static final String LINE_NUMBERING =
	        "http://saxon.sf.net/feature/linenumbering";

	/**
	* RECOVERY_POLICY must be an Integer: Controller.RECOVER_SILENTLY,
	* Controller.RECOVER_WITH_WARNINGS, or Controller.DO_NOT_RECOVER
	*/

	public static final String RECOVERY_POLICY =
	        "http://saxon.sf.net/feature/recoveryPolicy";

	/**
	* MESSAGE_EMITTER_CLASS must be the class name of an Emitter
	*/

	public static final String MESSAGE_EMITTER_CLASS =
	        "http://saxon.sf.net/feature/messageEmitterClass";

    /**
    * SOURCE_PARSER_CLASS must be the full class name of an XMLReader
    */

    public static final String SOURCE_PARSER_CLASS =
            "http://saxon.sf.net/feature/sourceParserClass";

    /**
    * STYLE_PARSER_CLASS must be an XMLReader
    */

    public static final String STYLE_PARSER_CLASS =
            "http://saxon.sf.net/feature/styleParserClass";

    /**
    * NAME_POOL must be an instance of net.sf.saxon.om.NamePool
    */

    public static final String NAME_POOL =
            "http://saxon.sf.net/feature/namePool";

    /**
    * OUTPUT_URI_RESOLVER must be an instance of net.sf.saxon.OutputURIResolver
    */

    public static final String OUTPUT_URI_RESOLVER =
            "http://saxon.sf.net/feature/outputURIResolver";

	/**
	* DTD_VALIDATION must be a Boolean. This determines whether source documents should be
	* parsed with DTD-validation enabled.
	*/

	public static final String DTD_VALIDATION =
	        "http://saxon.sf.net/feature/validation";

	/**
	* SCHEMA_VALIDATION must be a Boolean. This determines whether source documents should be
	* parsed with schema-validation enabled.
	*/

	public static final String SCHEMA_VALIDATION =
	        "http://saxon.sf.net/feature/schema-validation";

    /**
    * VALIDATION_WARNINGS must be a Boolean. This determines whether validation errors in result
    * documents should be treated as fatal. By default they are fatal; with this option set, they
    * are treated as warnings.
    */

    public static final String VALIDATION_WARNINGS =
            "http://saxon.sf.net/feature/validation-warnings";


    /**
    * VERSION_WARNING must be a Boolean. This determines whether a warning should be output when
     * running an XSLT 2.0 processor against an XSLT 1.0 stylesheet. The XSLT specification requires
     * this to be done by default.
    */

    public static final String VERSION_WARNING =
            "http://saxon.sf.net/feature/version-warning";

    private FeatureKeys() {
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
