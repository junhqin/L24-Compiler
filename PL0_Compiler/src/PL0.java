import java.io.*;

/**
 *<p>����汾�� PL/0 ���������� C ���Եİ汾��д���ɡ������汾�ڻ����߼�����һ��
 *�ģ���Щ�ط����������Ķ�������getsym()��statement()����������������ע��C����
 *�汾�е�ȫ�ֱ�����ɢ�����ɱ������������У�Ϊ���ڲ��ң���������Щȫ�ֱ���ԭ�������֡�</p>
 *
 *<p>�Ķ����������������뼰ʱ��ѯ������̡�</p>
 */
public class PL0 {
	// �������ĳ���
	public static final int al = 10;			// ���ŵ���󳤶�
	public static final int amax = 2047;		// ����������ֵ
	public static final int cxmax = 500;		// ���������������
	public static final int levmax = 3;			// ����������Ƕ���������� [0, levmax]
	public static final int nmax = 14;			// number�����λ��
	public static final int norw = 32;			// �ؼ��ָ���
	public static final int txmax = 100;		// ���ֱ�����
	
	// һЩȫ�ֱ����������ؼ��ı����ֲ����£�
	// cx, code : Interpreter
	// dx : Parser
	// tx, table : Table
	public static PrintStream fa;				// ������������
	public static PrintStream fa1;				// ���Դ�ļ�������ж�Ӧ���׵�ַ
	public static PrintStream fa2;				// ������
	public static PrintStream fas;				// ������ֱ�
	public static boolean listswitch;			// ��ʾ������������
	public static boolean tableswitch;			// ��ʾ���ֱ����
	
	// һ�����͵ı���������ɲ���
	public static Scanner lex;					// �ʷ�������
	public static Parser  parser;				// �﷨������
	public static Interpreter interp;			// ��P-Code����������Ŀ��������ɹ��ߣ�
	public static Table table;					// ���ֱ�
	
	// Ϊ�����δ���BufferedReader������ʹ��ȫ��ͳһ��Reader
	public static BufferedReader stdin;			// ��׼����
	
	/**
	 * ���캯������ʼ��������������ɲ���
	 * @param fin PL/0 Դ�ļ���������
	 */
	public PL0(BufferedReader fin) {
		// �������Ĺ��캯���ж�����C���԰汾�� init() ������һ���ִ���
		table = new Table();
		interp = new Interpreter();
		lex = new Scanner(fin);
		parser = new Parser(lex, table, interp);
	}

	/**
	 * ִ�б��붯��
	 * @return �Ƿ����ɹ�
	 */
	boolean compile() {
		boolean abort = false;
		
		try {
			PL0.fa = new PrintStream("fa.tmp");
			PL0.fas = new PrintStream("fas.tmp");
			parser.nextSym();		// ǰհ������ҪԤ�ȶ���һ������
			parser.parse();			// ��ʼ�﷨�������̣���ͬ�﷨��顢Ŀ��������ɣ�
		} catch (Error e) {
			// ����Ƿ������ش�����ֱ����ֹ
			abort = true;
		} catch (IOException e) {
		} finally { 
			PL0.fa.close();
			PL0.fa1.close();
			PL0.fas.close();
		}
		if (abort)
			System.exit(0);
				
		// ����ɹ���ָ��ɱ�����̲���û�д���
		return (Err.err == 0);
	}

	/**
	 * ������
	 */
	public static void main(String[] args) {
		// ԭ�� C ���԰��һЩ��仮�ֵ�compile()��Parser.parse()��
		boolean t;
		t = 123==1223;
		System.out.println(t);
		String fname = "";
		stdin = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader fin;
		try {
			// �����ļ���
			fname = "";
			System.out.print("Input pl/0 file?   ");
			while (fname.equals(""))
				fname = stdin.readLine();
			fin = new BufferedReader(new FileReader(fname), 4096);

			// �Ƿ�������������
			fname = "";
			System.out.print("List object code?(Y/N)");
			while (fname.equals(""))
				fname = stdin.readLine();
			PL0.listswitch = (fname.charAt(0)=='y' || fname.charAt(0)=='Y');
			
			// �Ƿ�������ֱ�
			fname = "";
			System.out.print("List symbol table?(Y/N)");
			while (fname.equals(""))
				fname = stdin.readLine();
			PL0.tableswitch = (fname.charAt(0)=='y' || fname.charAt(0)=='Y');
			
			PL0.fa1 = new PrintStream("fa1.tmp");
			PL0.fa1.println("Input pl/0 file?   " + fname);

			// �������������ʼ��
			PL0 pl0 = new PL0(fin);
			
			if (pl0.compile()) {
				// ����ɹ���������Ž�������
				PL0.fa2 = new PrintStream("fa2.tmp");
				interp.interpret();
				PL0.fa2.close();
			} else {
				System.out.print("Errors in pl/0 program");
			}
			
		} catch (IOException e) {
			System.out.println("Can't open file!");
		}

		System.out.println();
	}
}
