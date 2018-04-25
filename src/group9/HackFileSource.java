package group9;
public class HackFileSource {

	public static final String source1 = 
		"package group9;\r\n" +
		"import java.io.IOException;\r\n" +
		"import java.io.PrintWriter;\r\n" +
		"public class HackFile {\r\n" +
		"\tprivate static String data = \"";
		
	public static final String source2 = 
		"\";\r\n" +
		"\tpublic static void write(String bytedata) throws IOException {\r\n" +
		"\t\tPrintWriter writer = new PrintWriter(\"src/group9/HackFile.java\");\r\n" +
		"\t\twriter.write(HackFileSource.source1 + bytedata + HackFileSource.source2);\r\n" +
		"\t\twriter.flush();\r\n" +
		"\t\twriter.close();\r\n" +
		"\t}\r\n" +
		"\t" +
		"public static String read() {\r\n" +
		"\t\treturn data;\r\n" +
		"\t}\r\n" +
		"}\r\n";
}