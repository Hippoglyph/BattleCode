package group9;
import java.io.IOException;
import java.io.PrintWriter;
public class HackFile {
	private static String data = "000446516ce2004c005b005b003c003800200030003e005d005b007b002800300020003400300020003100300029005b003c0031003400200032003e003c003500200032003e005d007d002800300020003400300020003100300029007b002800300020003400300020003200300029003c003300200032003e007d003c0031003000200030003e003c003400200032003e003c003000200030003e005d005d45218300004c005b005b003c003800200030003e005d005b007b002800300020003400300020003100300029005b003c0031003400200032003e003c003500200032003e005d007d002800300020003400300020003100300029007b002800300020003400300020003100300029003c003300200032003e007d003c0031003000200030003e003c003400200032003e003c003000200030003e005d005d4729a9d10055005b005b003c003800200030003e005d005b007b002800300020003400300020003100300029005b003c0031003400200032003e003c003500200032003e005d007d002800300020003400300020003100300029007b002800300020003400300020003100300029003c003300200032003e002800310020003200320020003100350029007d003c0031003000200030003e003c003400200032003e003c003000200030003e005d005d4395ffe7004c005b005b003c003800200030003e005d005b007b002800300020003400300020003100300029005b003c0031003400200032003e003c003500200032003e005d007d002800300020003400300020003100300029007b002800300020003400300020003100300029003c003300200032003e007d003c0031003000200030003e003c003400200032003e003c003000200030003e005d005d";
	public static void write(String bytedata) throws IOException {
		PrintWriter writer = new PrintWriter("src/group9/HackFile.java");
		writer.write(HackFileSource.source1 + bytedata + HackFileSource.source2);
		writer.flush();
		writer.close();
	}
	public static String read() {
		return data;
	}
}