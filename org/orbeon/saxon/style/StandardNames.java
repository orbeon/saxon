package org.orbeon.saxon.style;
import org.orbeon.saxon.om.NamespaceConstant;

import java.util.HashMap;

public abstract class StandardNames {

	/**
	 * Well-known names used in XSLT processing. These names must all have
     * fingerprints in the range 0-1023, to avoid clashing with codes allocated
     * in a NamePool. We use the top three bits for the namespace, and the bottom
     * seven bits for the local name
	*/

    private static final int DFLT_NS = 0;
    private static final int XSL_NS = 1;
    private static final int SAXON_NS = 2;
    private static final int XML_NS = 3;
    private static final int XS_NS = 4;
    private static final int XDT_NS = 5;
    private static final int XSI_NS = 6;
    //private static final int FN_NS = 7;

    public static final int DFLT = 128*0;
    public static final int XSL = 128*1;
    public static final int SAXON = 128*2;
    public static final int XML = 128*3;
    public static final int XS = 128*4;
    public static final int XDT = 128*5;
    public static final int XSI = 128*6;

    public static final int XSL_ANALYZE_STRING = XSL + 0;
    public static final int XSL_APPLY_IMPORTS = XSL + 1;
    public static final int XSL_APPLY_TEMPLATES = XSL + 2;
    public static final int XSL_ATTRIBUTE = XSL + 3;
    public static final int XSL_ATTRIBUTE_SET = XSL + 4;
    public static final int XSL_CALL_TEMPLATE = XSL + 5;
    public static final int XSL_CHARACTER_MAP = XSL + 6;
    public static final int XSL_CHOOSE = XSL + 7;
    public static final int XSL_COMMENT = XSL + 10;
    public static final int XSL_COPY = XSL + 11;
    public static final int XSL_COPY_OF = XSL + 12;
    public static final int XSL_DECIMAL_FORMAT = XSL + 13;
    public static final int XSL_DOCUMENT = XSL + 14;
    public static final int XSL_ELEMENT = XSL + 15;
    public static final int XSL_FALLBACK = XSL + 16;
    public static final int XSL_FOR_EACH = XSL + 17;
    public static final int XSL_FOR_EACH_GROUP = XSL + 20;
    public static final int XSL_FUNCTION = XSL + 21;
    public static final int XSL_IF = XSL + 22;
    public static final int XSL_IMPORT = XSL + 23;
    public static final int XSL_IMPORT_SCHEMA = XSL + 24;
    public static final int XSL_INCLUDE = XSL + 25;
    public static final int XSL_KEY = XSL + 26;
    public static final int XSL_MATCHING_SUBSTRING = XSL + 27;
    public static final int XSL_MESSAGE = XSL + 30;
    public static final int XSL_NEXT_MATCH = XSL + 31;
    public static final int XSL_NUMBER = XSL + 32;
    public static final int XSL_NAMESPACE = XSL + 33;
    public static final int XSL_NAMESPACE_ALIAS = XSL + 34;
    public static final int XSL_NON_MATCHING_SUBSTRING = XSL + 35;
    public static final int XSL_OTHERWISE = XSL + 36;
    public static final int XSL_OUTPUT = XSL + 37;
    public static final int XSL_OUTPUT_CHARACTER = XSL + 41;
    public static final int XSL_PARAM = XSL + 42;
    public static final int XSL_PERFORM_SORT = XSL + 43;
    public static final int XSL_PRESERVE_SPACE = XSL + 44;
    public static final int XSL_PROCESSING_INSTRUCTION = XSL + 45;
    public static final int XSL_RESULT_DOCUMENT = XSL + 46;
    public static final int XSL_SEQUENCE = XSL + 47;
    public static final int XSL_SORT = XSL + 50;
    public static final int XSL_STRIP_SPACE = XSL + 51;
    public static final int XSL_STYLESHEET = XSL + 52;
    public static final int XSL_TEMPLATE = XSL + 53;
    public static final int XSL_TEXT = XSL + 54;
    public static final int XSL_TRANSFORM = XSL + 55;
    public static final int XSL_VALUE_OF = XSL + 56;
    public static final int XSL_VARIABLE = XSL + 57;
    public static final int XSL_WITH_PARAM = XSL + 60;
    public static final int XSL_WHEN = XSL + 61;

    public static final int XSL_XPATH_DEFAULT_NAMESPACE = XSL + 100;
    public static final int XSL_EXCLUDE_RESULT_PREFIXES = XSL + 101;
    public static final int XSL_EXTENSION_ELEMENT_PREFIXES = XSL + 102;
    public static final int XSL_INHERIT_NAMESPACES = XSL + 103;
    public static final int XSL_TYPE = XSL + 104;
    public static final int XSL_USE_ATTRIBUTE_SETS = XSL + 105;
    public static final int XSL_VALIDATION = XSL + 106;
    public static final int XSL_VERSION = XSL + 107;

    private static final String XSL_B = '{' + NamespaceConstant.XSLT + '}';
    public static final String XSL_INHERIT_NAMESPACES_CLARK =
            XSL_B + "inherit-namespaces";
    public static final String XSL_VERSION_CLARK =
            XSL_B + "version";
    public static final String XSL_XPATH_DEFAULT_NAMESPACE_CLARK =
            XSL_B + "xpath-default-namespace";
    public static final String XSL_EXTENSION_ELEMENT_PREFIXES_CLARK =
            XSL_B + "extension-element-prefixes";
    public static final String XSL_EXCLUDE_RESULT_PREFIXES_CLARK =
            XSL_B + "exclude-result-prefixes";




    public static final int SAXON_ASSIGN = SAXON + 1;
    public static final int SAXON_CALL_TEMPLATE = SAXON + 2;
    public static final int SAXON_COLLATION = SAXON + 3;
    public static final int SAXON_DOCTYPE = SAXON + 4;
    public static final int SAXON_ENTITY_REF = SAXON + 5;
    public static final int SAXON_IMPORT_QUERY = SAXON + 6;
    public static final int SAXON_SCRIPT = SAXON + 7;
    public static final int SAXON_WHILE = SAXON + 8;

    private static final String SAXON_B = '{' + NamespaceConstant.SAXON + '}';
    public static final String SAXON_ASSIGNABLE = SAXON_B + "assignable";
    public static final String SAXON_CHARACTER_REPRESENTATION = SAXON_B + "character-representation";
    public static final String SAXON_EXPLAIN = SAXON_B + "explain";
    public static final String SAXON_INDENT_SPACES = SAXON_B + "indent-spaces";
    public static final String SAXON_NEXT_IN_CHAIN = SAXON_B + "next-in-chain";
    public static final String SAXON_BYTE_ORDER_MARK = SAXON_B + "byte-order-mark";
    public static final String SAXON_REQUIRE_WELL_FORMED = SAXON_B + "require-well-formed";
    public static final String SAXON_MEMO_FUNCTION = SAXON_B + "memo-function";

    // Not sure about this one...
    public static final int SAXON_JAVA_LANG_OBJECT = SAXON + 29;

    public static final int XML_BASE = XML + 1;
    public static final int XML_SPACE = XML + 2;
    public static final int XML_LANG = XML + 3;

//    public static final int ARCHIVE = DFLT + 0;
//    public static final int AS = DFLT + 1;
//    public static final int CASE_ORDER = DFLT + 2;
//    public static final int CDATA_SECTION_ELEMENTS = DFLT + 3;
//    public static final int CHARACTER = DFLT + 4;
//    public static final int CLASS = DFLT + 5;
//    public static final int COLLATION = DFLT + 6;
//    public static final int COPY_NAMESPACES = DFLT + 7;
//    public static final int COUNT = DFLT + 10;
//    public static final int DATA_TYPE = DFLT + 11;
//    public static final int DECIMAL_SEPARATOR = DFLT + 12;
//    public static final int DECOMPOSITION = DFLT + 13;
//    public static final int DEFAULT = DFLT + 14;
//    public static final int DEFAULT_VALIDATION = DFLT + 15;
//    public static final int DIGIT = DFLT + 16;
//    public static final int DISABLE_OUTPUT_ESCAPING = DFLT + 17;
//    public static final int DOCTYPE_PUBLIC = DFLT + 18;
//    public static final int DOCTYPE_SYSTEM = DFLT + 20;
//    public static final int ELEMENTS = DFLT + 21;
//    public static final int ESCAPE_URI_ATTRIBUTES = DFLT + 22;
//    public static final int ENCODING = DFLT + 23;
//    public static final int EXCLUDE_RESULT_PREFIXES = DFLT + 24;
//    public static final int EXTENSION_ELEMENT_PREFIXES = DFLT + 25;
//    public static final int FLAGS = DFLT + 26;
//    public static final int FORMAT = DFLT + 27;
//    public static final int FROM = DFLT + 30;
//    public static final int GROUP_ADJACENT = DFLT + 31;
//    public static final int GROUP_BY = DFLT + 32;
//    public static final int GROUP_ENDING_WITH = DFLT + 33;
//    public static final int GROUP_STARTING_WITH = DFLT + 34;
//    public static final int GROUPING_SEPARATOR = DFLT + 35;
//    public static final int GROUPING_SIZE = DFLT + 36;
//    public static final int HREF = DFLT + 37;
//    public static final int ID = DFLT + 40;
//    public static final int IMPLEMENTS_PREFIX = DFLT + 41;
//    public static final int INCLUDE_CONTENT_TYPE = DFLT + 42;
//    public static final int INDENT = DFLT + 43;
//    public static final int INFINITY = DFLT + 44;
//    public static final int LANG = DFLT + 45;
//    public static final int LANGUAGE = DFLT + 46;
//    public static final int LETTER_VALUE = DFLT + 47;
//    public static final int LEVEL = DFLT + 50;
//    public static final int MATCH = DFLT + 51;
//    public static final int MEDIA_TYPE = DFLT + 52;
//    public static final int METHOD = DFLT + 53;
//    public static final int MINUS_SIGN = DFLT + 54;
//    public static final int MODE = DFLT + 55;
//    public static final int NAME = DFLT + 56;
//    public static final int NAMESPACE = DFLT + 57;
//    public static final int NAN = DFLT + 60;
//    public static final int OMIT_XML_DECLARATION = DFLT + 61;
//    public static final int ORDER = DFLT + 62;
//    public static final int ORDINAL = DFLT + 63;
//    public static final int OVERRIDE = DFLT + 64;
//    public static final int PATTERN_SEPARATOR = DFLT + 65;
//    public static final int PERCENT = DFLT + 66;
//    public static final int PER_MILLE = DFLT + 67;
//    public static final int PRIORITY = DFLT + 70;
//    public static final int REGEX = DFLT + 71;
//    public static final int REQUIRED = DFLT + 72;
//    public static final int RESULT_PREFIX = DFLT + 73;
//    public static final int RULES = DFLT + 74;
//    public static final int SCHEMA_LOCATION = DFLT + 75;
//    public static final int SELECT = DFLT + 76;
//    public static final int SEPARATOR = DFLT + 77;
//    public static final int SRC = DFLT + 80;
//    public static final int STANDALONE = DFLT + 81;
//    public static final int STRENGTH = DFLT + 82;
//    public static final int STRING = DFLT + 83;
//    public static final int STYLESHEET_PREFIX = DFLT + 84;
//    public static final int TERMINATE = DFLT + 85;
//    public static final int TEST = DFLT + 86;
//    public static final int TUNNEL = DFLT + 87;
//    public static final int TYPE = DFLT + 90;
//    public static final int UNDECLARE_NAMESPACES = DFLT + 91;
//    public static final int USE = DFLT + 92;
//    public static final int USE_ATTRIBUTE_SETS = DFLT + 93;
//    public static final int USE_CHARACTER_MAPS = DFLT + 94;
//    public static final int VALIDATION = DFLT + 95;
//    public static final int VALUE = DFLT + 96;
//    public static final int VERSION = DFLT + 97;
//    public static final int XPATH_DEFAULT_NAMESPACE = DFLT + 100;
//    public static final int ZERO_DIGIT = DFLT + 101;

    public static final String ARCHIVE = "archive";
    public static final String AS = "as";
    public static final String CASE_ORDER = "case-order";
    public static final String CDATA_SECTION_ELEMENTS = "cdata-section-elements";
    public static final String CHARACTER = "character";
    public static final String CLASS = "class";
    public static final String COLLATION = "collation";
    public static final String COPY_NAMESPACES = "copy-namespaces";
    public static final String COUNT = "count";
    public static final String DATA_TYPE = "data-type";
    public static final String DECIMAL_SEPARATOR = "decimal-separator";
    public static final String DECOMPOSITION = "decomposition";
    public static final String DEFAULT = "default";
    public static final String DEFAULT_VALIDATION = "default-validation";
    public static final String DIGIT = "digit";
    public static final String DISABLE_OUTPUT_ESCAPING = "disable-output-escaping";
    public static final String DOCTYPE_PUBLIC = "doctype-public";
    public static final String DOCTYPE_SYSTEM = "doctype-system";
    public static final String ELEMENTS = "elements";
    public static final String ESCAPE_URI_ATTRIBUTES = "escape-uri-attributes";
    public static final String ENCODING = "encoding";
    public static final String EXCLUDE_RESULT_PREFIXES = "exclude-result-prefixes";
    public static final String EXTENSION_ELEMENT_PREFIXES = "extension-element-prefixes";
    public static final String FLAGS = "flags";
    public static final String FORMAT = "format";
    public static final String FROM = "from";
    public static final String GROUP_ADJACENT = "group-adjacent";
    public static final String GROUP_BY = "group-by";
    public static final String GROUP_ENDING_WITH = "group-ending-with";
    public static final String GROUP_STARTING_WITH = "group-starting-with";
    public static final String GROUPING_SEPARATOR = "grouping-separator";
    public static final String GROUPING_SIZE = "grouping-size";
    public static final String HREF = "href";
    public static final String ID = "id";
    public static final String IMPLEMENTS_PREFIX = "implements-prefix";
    public static final String INCLUDE_CONTENT_TYPE = "include-content-type";
    public static final String INDENT = "indent";
    public static final String INFINITY = "infinity";
    public static final String INHERIT_NAMESPACES = "inherit-namespaces";
    public static final String LANG = "lang";
    public static final String LANGUAGE = "language";
    public static final String LETTER_VALUE = "letter-value";
    public static final String LEVEL = "level";
    public static final String MATCH = "match";
    public static final String MEDIA_TYPE = "media-type";
    public static final String METHOD = "method";
    public static final String MINUS_SIGN = "minus-sign";
    public static final String MODE = "mode";
    public static final String NAME = "name";
    public static final String NAMESPACE = "namespace";
    public static final String NAN = "NaN";
    public static final String OMIT_XML_DECLARATION = "omit-xml-declaration";
    public static final String ORDER = "order";
    public static final String ORDINAL = "ordinal";
    public static final String OUTPUT_VERSION = "output-version";
    public static final String OVERRIDE = "override";
    public static final String PATTERN_SEPARATOR = "pattern-separator";
    public static final String PERCENT = "percent";
    public static final String PER_MILLE = "per-mille";
    public static final String PRIORITY = "priority";
    public static final String REGEX = "regex";
    public static final String REQUIRED = "required";
    public static final String RESULT_PREFIX = "result-prefix";
    public static final String RULES = "rules";
    public static final String SCHEMA_LOCATION = "schema-location";
    public static final String SELECT = "select";
    public static final String SEPARATOR = "separator";
    public static final String SRC = "src";
    public static final String STANDALONE = "standalone";
    public static final String STRENGTH = "strength";
    public static final String STRING = "string";
    public static final String STYLESHEET_PREFIX = "stylesheet-prefix";
    public static final String TERMINATE = "terminate";
    public static final String TEST = "test";
    public static final String TUNNEL = "tunnel";
    public static final String TYPE = "type";
    public static final String UNDECLARE_NAMESPACES = "undeclare-namespaces";
    public static final String USE = "use";
    public static final String USE_ATTRIBUTE_SETS = "use-attribute-sets";
    public static final String USE_CHARACTER_MAPS = "use-character-maps";
    public static final String VALIDATION = "validation";
    public static final String VALUE = "value";
    public static final String VERSION = "version";
    public static final String XPATH_DEFAULT_NAMESPACE = "xpath-default-namespace";
    public static final String ZERO_DIGIT = "zero-digit";

    public static final int XS_STRING          = XS +  1;
    public static final int XS_BOOLEAN         = XS +  2;
    public static final int XS_DECIMAL         = XS +  3;
    public static final int XS_FLOAT           = XS +  4;
    public static final int XS_DOUBLE          = XS +  5;
    public static final int XS_DURATION        = XS +  6;
    public static final int XS_DATE_TIME       = XS +  7;
    public static final int XS_TIME            = XS +  8;
    public static final int XS_DATE            = XS +  9;
    public static final int XS_G_YEAR_MONTH    = XS +  10;
    public static final int XS_G_YEAR          = XS +  11;
    public static final int XS_G_MONTH_DAY     = XS +  12;
    public static final int XS_G_DAY           = XS +  13;
    public static final int XS_G_MONTH         = XS +  14;
    public static final int XS_HEX_BINARY      = XS +  15;
    public static final int XS_BASE64_BINARY   = XS +  16;
    public static final int XS_ANY_URI         = XS +  17;
    public static final int XS_QNAME           = XS +  18;
    public static final int XS_NOTATION        = XS +  19;
    public static final int XS_INTEGER                 = XS +  20;

    // Note that any type code <= XS_INTEGER is considered to represent a
    // primitive type: see Type.isPrimitiveType()

    public static final int XS_NON_POSITIVE_INTEGER    = XS +  21;
    public static final int XS_NEGATIVE_INTEGER        = XS +  22;
    public static final int XS_LONG                    = XS +  23;
    public static final int XS_INT                     = XS +  24;
    public static final int XS_SHORT                   = XS +  25;
    public static final int XS_BYTE                    = XS +  26;
    public static final int XS_NON_NEGATIVE_INTEGER    = XS +  27;
    public static final int XS_POSITIVE_INTEGER        = XS +  28;
    public static final int XS_UNSIGNED_LONG           = XS +  29;
    public static final int XS_UNSIGNED_INT            = XS +  30;
    public static final int XS_UNSIGNED_SHORT          = XS +  31;
    public static final int XS_UNSIGNED_BYTE           = XS +  32;
    public static final int XS_NORMALIZED_STRING       = XS +  41;
    public static final int XS_TOKEN                   = XS +  42;
    public static final int XS_LANGUAGE                = XS +  43;
    public static final int XS_NMTOKEN                 = XS +  44;
    public static final int XS_NMTOKENS                = XS +  45;      // NB: list type
    public static final int XS_NAME                    = XS +  46;
    public static final int XS_NCNAME                  = XS +  47;
    public static final int XS_ID                      = XS +  48;
    public static final int XS_IDREF                   = XS +  49;
    public static final int XS_IDREFS                  = XS +  50;      // NB: list type
    public static final int XS_ENTITY                  = XS +  51;
    public static final int XS_ENTITIES                = XS +  52;      // NB: list type

    public static final int XS_ANY_TYPE                = XS +  60;
    public static final int XS_ANY_SIMPLE_TYPE         = XS +  61;

    public static final int XS_INVALID_NAME             = XS +  62;

    public static final int XS_ALL                      = XS + 70;
    public static final int XS_ANNOTATION               = XS + 71;
    public static final int XS_ANY                      = XS + 72;
    public static final int XS_ANY_ATTRIBUTE            = XS + 73;
    public static final int XS_APPINFO                  = XS + 74;
    public static final int XS_ATTRIBUTE                = XS + 75;
    public static final int XS_ATTRIBUTE_GROUP          = XS + 76;
    public static final int XS_CHOICE                   = XS + 77;
    public static final int XS_COMPLEX_CONTENT          = XS + 80;
    public static final int XS_COMPLEX_TYPE             = XS + 81;
    public static final int XS_DOCUMENTATION            = XS + 82;
    public static final int XS_ELEMENT                  = XS + 83;
    public static final int XS_ENUMERATION              = XS + 84;
    public static final int XS_EXTENSION                = XS + 85;
    public static final int XS_FIELD                    = XS + 86;
    public static final int XS_FRACTION_DIGITS          = XS + 87;
    public static final int XS_GROUP                    = XS + 88;
    public static final int XS_IMPORT                   = XS + 90;
    public static final int XS_INCLUDE                  = XS + 91;
    public static final int XS_KEY                      = XS + 92;
    public static final int XS_KEYREF                   = XS + 93;
    public static final int XS_LENGTH                   = XS + 94;
    public static final int XS_LIST                     = XS + 95;
    public static final int XS_MAX_EXCLUSIVE            = XS + 96;
    public static final int XS_MAX_INCLUSIVE            = XS + 97;
    public static final int XS_MAX_LENGTH               = XS + 100;
    public static final int XS_MIN_EXCLUSIVE            = XS + 101;
    public static final int XS_MIN_INCLUSIVE            = XS + 102;
    public static final int XS_MIN_LENGTH               = XS + 103;
    public static final int XS_notation                 = XS + 104;
    public static final int XS_PATTERN                  = XS + 105;
    public static final int XS_REDEFINE                 = XS + 106;
    public static final int XS_RESTRICTION              = XS + 107;
    public static final int XS_SCHEMA                   = XS + 108;
    public static final int XS_SELECTOR                 = XS + 110;
    public static final int XS_SEQUENCE                 = XS + 111;
    public static final int XS_SIMPLE_CONTENT           = XS + 112;
    public static final int XS_SIMPLE_TYPE              = XS + 113;
    public static final int XS_TOTAL_DIGITS             = XS + 114;
    public static final int XS_UNION                    = XS + 115;
    public static final int XS_UNIQUE                   = XS + 116;
    public static final int XS_WHITE_SPACE              = XS + 117;

    public static final int XDT_UNTYPED                 = XDT + 1;
    public static final int XDT_UNTYPED_ATOMIC          = XDT + 2;
    public static final int XDT_ANY_ATOMIC_TYPE         = XDT + 3;
    public static final int XDT_YEAR_MONTH_DURATION     = XDT + 4;
    public static final int XDT_DAY_TIME_DURATION       = XDT + 5;
    public static final int XDT_NUMERIC                 = XDT + 6;

    public static final int XSI_TYPE                    = XSI + 1;
    public static final int XSI_NIL                     = XSI + 2;
    public static final int XSI_SCHEMA_LOCATION         = XSI + 3;
    public static final int XSI_NO_NAMESPACE_SCHEMA_LOCATION = XSI + 4;


    private static String[] localNames = new String[1023];
    private static HashMap lookup = new HashMap(1023);

    private StandardNames() {
    	//pool = namePool;
    }

    private static void bindXSLTName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.XSLT + '}' + localName, new Integer(constant));
    }

    private static void bindSaxonName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.SAXON + '}' + localName, new Integer(constant));
    }

    private static void bindXMLName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.XML + '}' + localName, new Integer(constant));
    }

//    private static void bindXSLTAttributeName(int constant, String localName) {
//        localNames[constant] = localName;
//        lookup.put("{}" + localName, new Integer(constant));
//    }

    private static void bindXSName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.SCHEMA + '}' + localName, new Integer(constant));
    }

    private static void bindXDTName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.XDT + '}' + localName, new Integer(constant));
    }

    private static void bindXSIName(int constant, String localName) {
        localNames[constant] = localName;
        lookup.put('{' + NamespaceConstant.SCHEMA_INSTANCE + '}' + localName, new Integer(constant));
    }

	static {

		bindXSLTName(XSL_ANALYZE_STRING, "analyze-string");
		bindXSLTName(XSL_APPLY_IMPORTS, "apply-imports");
		bindXSLTName(XSL_APPLY_TEMPLATES, "apply-templates");
		bindXSLTName(XSL_ATTRIBUTE, "attribute");
		bindXSLTName(XSL_ATTRIBUTE_SET, "attribute-set" );
		bindXSLTName(XSL_CALL_TEMPLATE, "call-template");
		bindXSLTName(XSL_CHARACTER_MAP, "character-map");
		bindXSLTName(XSL_CHOOSE, "choose");
		bindXSLTName(XSL_COMMENT , "comment");
		bindXSLTName(XSL_COPY, "copy");
		bindXSLTName(XSL_COPY_OF, "copy-of");
		bindXSLTName(XSL_DECIMAL_FORMAT, "decimal-format");
		bindXSLTName(XSL_DOCUMENT, "document");
		bindXSLTName(XSL_ELEMENT, "element");
		bindXSLTName(XSL_FALLBACK, "fallback");
		bindXSLTName(XSL_FOR_EACH, "for-each");
		bindXSLTName(XSL_FOR_EACH_GROUP, "for-each-group");
		bindXSLTName(XSL_FUNCTION, "function");
		bindXSLTName(XSL_IF, "if");
		bindXSLTName(XSL_IMPORT, "import");
		bindXSLTName(XSL_IMPORT_SCHEMA, "import-schema");
		bindXSLTName(XSL_INCLUDE, "include");
		bindXSLTName(XSL_KEY, "key");
		bindXSLTName(XSL_MATCHING_SUBSTRING, "matching-substring");
		bindXSLTName(XSL_MESSAGE, "message");
		bindXSLTName(XSL_NEXT_MATCH, "next-match");
		bindXSLTName(XSL_NUMBER, "number");
		bindXSLTName(XSL_NAMESPACE, "namespace");
		bindXSLTName(XSL_NAMESPACE_ALIAS, "namespace-alias");
		bindXSLTName(XSL_NON_MATCHING_SUBSTRING, "non-matching-substring");
		bindXSLTName(XSL_OTHERWISE, "otherwise");
		bindXSLTName(XSL_OUTPUT, "output");
		bindXSLTName(XSL_OUTPUT_CHARACTER, "output-character");
		bindXSLTName(XSL_PARAM, "param");
		bindXSLTName(XSL_PERFORM_SORT, "perform-sort");
		bindXSLTName(XSL_PRESERVE_SPACE, "preserve-space");
		bindXSLTName(XSL_PROCESSING_INSTRUCTION, "processing-instruction");
        bindXSLTName(XSL_RESULT_DOCUMENT, "result-document");
		bindXSLTName(XSL_SEQUENCE, "sequence");
		bindXSLTName(XSL_SORT, "sort");
		bindXSLTName(XSL_STRIP_SPACE, "strip-space");
		bindXSLTName(XSL_STYLESHEET, "stylesheet");
		bindXSLTName(XSL_TEMPLATE, "template");
		bindXSLTName(XSL_TEXT, "text");
		bindXSLTName(XSL_TRANSFORM, "transform");
		bindXSLTName(XSL_VALUE_OF, "value-of");
		bindXSLTName(XSL_VARIABLE, "variable");
		bindXSLTName(XSL_WITH_PARAM, "with-param");
		bindXSLTName(XSL_WHEN, "when");

        bindXSLTName(XSL_XPATH_DEFAULT_NAMESPACE, "xpath-default-namespace");
        bindXSLTName(XSL_EXCLUDE_RESULT_PREFIXES, "exclude-result-prefixes");
        bindXSLTName(XSL_EXTENSION_ELEMENT_PREFIXES, "extension-element-prefixes");
        bindXSLTName(XSL_INHERIT_NAMESPACES, "inherit-namespaces");
        bindXSLTName(XSL_TYPE, "type");
        bindXSLTName(XSL_USE_ATTRIBUTE_SETS, "use-attribute-sets");
        bindXSLTName(XSL_VALIDATION, "validation");
        bindXSLTName(XSL_VERSION, "version");

		bindSaxonName(SAXON_ASSIGN,  "assign");
		bindSaxonName(SAXON_CALL_TEMPLATE,  "call-template");
		bindSaxonName(SAXON_COLLATION,  "collation");
		bindSaxonName(SAXON_DOCTYPE,  "doctype");
		bindSaxonName(SAXON_ENTITY_REF,  "entity-ref");
		bindSaxonName(SAXON_IMPORT_QUERY,  "import-query");
        bindSaxonName(SAXON_SCRIPT,  "script");
		bindSaxonName(SAXON_WHILE,  "while");

        bindXMLName(XML_BASE, "base");
        bindXMLName(XML_SPACE, "space");
        bindXMLName(XML_LANG, "lang");

        bindXSName(XS_STRING, "string");
        bindXSName(XS_BOOLEAN, "boolean");
        bindXSName(XS_DECIMAL, "decimal");
        bindXSName(XS_FLOAT, "float");
        bindXSName(XS_DOUBLE, "double");
        bindXSName(XS_DURATION, "duration");
        bindXSName(XS_DATE_TIME, "dateTime");
        bindXSName(XS_TIME, "time");
        bindXSName(XS_DATE, "date");
        bindXSName(XS_G_YEAR_MONTH , "gYearMonth");
        bindXSName(XS_G_YEAR, "gYear");
        bindXSName(XS_G_MONTH_DAY, "gMonthDay");
        bindXSName(XS_G_DAY, "gDay");
        bindXSName(XS_G_MONTH, "gMonth");
        bindXSName(XS_HEX_BINARY, "hexBinary");
        bindXSName(XS_BASE64_BINARY, "base64Binary");
        bindXSName(XS_ANY_URI, "anyURI");
        bindXSName(XS_QNAME, "QName");
        bindXSName(XS_NOTATION, "NOTATION");
        bindXSName(XS_INTEGER, "integer");
        bindXSName(XS_NON_POSITIVE_INTEGER, "nonPositiveInteger");
        bindXSName(XS_NEGATIVE_INTEGER, "negativeInteger");
        bindXSName(XS_LONG, "long");
        bindXSName(XS_INT, "int");
        bindXSName(XS_SHORT, "short");
        bindXSName(XS_BYTE, "byte");
        bindXSName(XS_NON_NEGATIVE_INTEGER, "nonNegativeInteger");
        bindXSName(XS_POSITIVE_INTEGER, "positiveInteger");
        bindXSName(XS_UNSIGNED_LONG, "unsignedLong");
        bindXSName(XS_UNSIGNED_INT, "unsignedInt");
        bindXSName(XS_UNSIGNED_SHORT, "unsignedShort");
        bindXSName(XS_UNSIGNED_BYTE, "unsignedByte");
        bindXSName(XS_NORMALIZED_STRING, "normalizedString");
        bindXSName(XS_TOKEN, "token");
        bindXSName(XS_LANGUAGE, "language");
        bindXSName(XS_NMTOKEN, "NMTOKEN");
        bindXSName(XS_NMTOKENS, "NMTOKENS");      // NB: list type
        bindXSName(XS_NAME, "Name");
        bindXSName(XS_NCNAME, "NCName");
        bindXSName(XS_ID, "ID");
        bindXSName(XS_IDREF, "IDREF");
        bindXSName(XS_IDREFS, "IDREFS");      // NB: list type
        bindXSName(XS_ENTITY, "ENTITY");
        bindXSName(XS_ENTITIES, "ENTITIES");      // NB: list type

        bindXSName(XS_ANY_TYPE, "anyType");
        bindXSName(XS_ANY_SIMPLE_TYPE, "anySimpleType");
        bindXSName(XS_INVALID_NAME, "invalidName");

        bindXSName(XS_ALL, "all");
        bindXSName(XS_ANNOTATION, "annotation");
        bindXSName(XS_ANY, "any");
        bindXSName(XS_ANY_ATTRIBUTE, "anyAttribute");
        bindXSName(XS_APPINFO, "appinfo");
        bindXSName(XS_ATTRIBUTE, "attribute");
        bindXSName(XS_ATTRIBUTE_GROUP, "attributeGroup");
        bindXSName(XS_CHOICE, "choice");
        bindXSName(XS_COMPLEX_CONTENT, "complexContent");
        bindXSName(XS_COMPLEX_TYPE, "complexType");
        bindXSName(XS_DOCUMENTATION, "documentation");
        bindXSName(XS_ELEMENT, "element");
        bindXSName(XS_ENUMERATION, "enumeration");
        bindXSName(XS_EXTENSION, "extension");
        bindXSName(XS_FIELD, "field");
        bindXSName(XS_FRACTION_DIGITS, "fractionDigits");
        bindXSName(XS_GROUP, "group");
        bindXSName(XS_IMPORT, "import");
        bindXSName(XS_INCLUDE, "include");
        bindXSName(XS_KEY, "key");
        bindXSName(XS_KEYREF, "keyref");
        bindXSName(XS_LENGTH, "length");
        bindXSName(XS_LIST, "list");
        bindXSName(XS_MAX_EXCLUSIVE, "maxExclusive");
        bindXSName(XS_MAX_INCLUSIVE, "maxInclusive");
        bindXSName(XS_MAX_LENGTH, "maxLength");
        bindXSName(XS_MIN_EXCLUSIVE, "minExclusive");
        bindXSName(XS_MIN_INCLUSIVE, "minInclusive");
        bindXSName(XS_MIN_LENGTH, "minLength");
        bindXSName(XS_notation, "notation");
        bindXSName(XS_PATTERN, "pattern");
        bindXSName(XS_REDEFINE, "redefine");
        bindXSName(XS_RESTRICTION, "restriction");
        bindXSName(XS_SCHEMA, "schema");
        bindXSName(XS_SELECTOR, "selector");
        bindXSName(XS_SEQUENCE, "sequence");
        bindXSName(XS_SIMPLE_CONTENT, "simpleContent");
        bindXSName(XS_SIMPLE_TYPE, "simpleType");
        bindXSName(XS_TOTAL_DIGITS, "totalDigits");
        bindXSName(XS_UNION, "union");
        bindXSName(XS_UNIQUE, "unique");
        bindXSName(XS_WHITE_SPACE, "whiteSpace");

        bindXDTName(XDT_UNTYPED, "untyped");
        bindXDTName(XDT_UNTYPED_ATOMIC, "untypedAtomic");
        bindXDTName(XDT_ANY_ATOMIC_TYPE, "anyAtomicType");
        bindXDTName(XDT_YEAR_MONTH_DURATION, "yearMonthDuration");
        bindXDTName(XDT_DAY_TIME_DURATION, "dayTimeDuration");
        bindXDTName(XDT_NUMERIC, "numeric");

        bindXSIName(XSI_TYPE, "type");
        bindXSIName(XSI_NIL, "nil");
        bindXSIName(XSI_SCHEMA_LOCATION, "schemaLocation");
        bindXSIName(XSI_NO_NAMESPACE_SCHEMA_LOCATION, "noNamespaceSchemaLocation");
	}

    public static int getFingerprint(String uri, String localName) {
        Integer fp = (Integer)lookup.get('{' + uri + '}' + localName);
        if (fp==null) {
            return -1;
        } else {
            return fp.intValue();
        }
    }

    public static String getLocalName(int fingerprint) {
        return localNames[fingerprint];
    }

    public static String getURI(int fingerprint) {
        int c = fingerprint >> 7;
        switch (c) {
            case DFLT_NS: return "";
            case XSL_NS: return NamespaceConstant.XSLT;
            case SAXON_NS: return NamespaceConstant.SAXON;
            case XML_NS: return NamespaceConstant.XML;
            case XS_NS: return NamespaceConstant.SCHEMA;
            case XSI_NS: return NamespaceConstant.SCHEMA_INSTANCE;
            case XDT_NS: return NamespaceConstant.XDT;
            default: return null;
        }
    }

    public static short getURICode(int fingerprint) {
        int c = fingerprint >> 7;
        switch (c) {
            case DFLT_NS: return 0;
            case XSL_NS: return NamespaceConstant.XSLT_CODE;
            case SAXON_NS: return NamespaceConstant.SAXON_CODE;
            case XML_NS: return NamespaceConstant.XML_CODE;
            case XS_NS: return NamespaceConstant.SCHEMA_CODE;
            case XSI_NS: return NamespaceConstant.XSI_CODE;
            case XDT_NS: return NamespaceConstant.XDT_CODE;
            default: return -1;
        }
    }

    /**
     * Get the Clark form of a name, given its name code or fingerprint
     * @return the local name if the name is in the null namespace, or "{uri}local"
     * otherwise. The name is always interned.
     */

    public static String getClarkName(int fingerprint) {
        String uri = getURI(fingerprint);
        if (uri.equals("")) {
            return getLocalName(fingerprint);
        } else {
            return '{' + uri + '}' + getLocalName(fingerprint);
        }
    }

    public static String getPrefix(int fingerprint) {
        int c = fingerprint >> 7;
        switch (c) {
            case DFLT_NS: return "";
            case XSL_NS: return "xsl";
            case SAXON_NS: return "saxon";
            case XML_NS: return "xml";
            case XS_NS: return "xs";
            case XSI_NS: return "xsi";
            case XDT_NS: return "xdt";
            default: return null;
        }
    }

    public static String getDisplayName(int fingerprint) {
        if (fingerprint==-1) {
            return "(anonymous type)";
        }
        if ((fingerprint>>7) == DFLT) {
            return getLocalName(fingerprint);
        }
        return getPrefix(fingerprint) + ':' + getLocalName(fingerprint);
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