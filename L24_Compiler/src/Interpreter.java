import java.util.Stack;

/**
 * ��P-Codeָ������
 */
enum Fct {
	LIT, OPR, LOD, STO, CAL, INT, JMP, JPC
}

/**
 *����������ӦC���԰汾�е� fct ö�����ͺ� instruction �ṹ�����������ָ��
 */
class Instruction {
	/**
	 * ���������ָ��
	 */
	public Fct f;
	
	/**
	 * ���ò���������Ĳ�β�
	 */
	public int l;
	
	/**
	 * ָ�����
	 */
	public int a;
}

/**
 *������P-Code��������������������ɺ�����������������C���԰���������Ҫ��ȫ�ֱ��� cx �� code
 */
public class Interpreter {
	// ����ִ��ʱʹ�õ�ջ��С
	final int stacksize = 500;
	
	/**
	 * ���������ָ�룬ȡֵ��Χ[0, cxmax-1] 
	 */
	public int cx = 0;
	
	/**
	 * �����������������
	 */
	public Instruction[] code = new Instruction[L24.cxmax];

	/**
	 * ��ű����ķ��ű�
	 *
	 */
	public Table table;

	/**
	 * ��������ջ
	 * @param t
	 */
	Stack<Objekt> typeStack = new Stack<>();
	public Interpreter(Table t){
		this.table = t;
	}
	/**
	 * �������������
	 * @param x instruction.f
	 * @param y instruction.l
	 * @param z instruction.a
	 */
	public void gen(Fct x, int y, int z) {
		if (cx >= L24.cxmax) {
			throw new Error("Program too long");
		}
		
		code[cx] = new Instruction();
		code[cx].f = x;
		code[cx].l = y;
		code[cx].a = z;
		cx ++;
	}

	/**
	 * ���Ŀ������嵥
	 * @param start ��ʼ�����λ��
	 */
	public void listcode(int start) {
		if (L24.listswitch) {
			for (int i=start; i<cx; i++) {
				String msg = i + " " + code[i].f + " " + code[i].l + " " + code[i].a;
				System.out.println(msg);
				L24.fa.println(msg);
			}
		}
	}
	
	/**
	 * ���ͳ���
	 */
	public void interpret() {
		int p, b, t;						// ָ��ָ�룬ָ���ַ��ջ��ָ��
		Instruction i;							// ��ŵ�ǰָ��
		int[] s = new int[stacksize];		// ջ
		
		System.out.println("start pl0");
		t = b = p = 0;
		s[0] = s[1] = s[2] = 0;
		do {
			i = code[p];					// ����ǰָ��
			p ++;
			switch (i.f) {
			case LIT:				// ��a��ֵȡ��ջ��
				s[t] = i.a;
				t++;
				//Ĭ��ֻ�ܴ������
				typeStack.push(Objekt.number);
				break;
			case OPR:				// ��ѧ���߼�����
				switch (i.a)
				{
				case 0:
					t = b;
					p = s[t+2];
					b = s[t+1];
					break;
				case 1:
					s[t-1] = -s[t-1];
					break;
				case 2:
					t--;
					s[t-1] = s[t-1]+s[t];
					typeStack.pop();
					break;
				case 3:
					t--;
					s[t-1] = s[t-1]-s[t];
					typeStack.pop();
					break;
				case 4:
					t--;
					s[t-1] = s[t-1]*s[t];
					typeStack.pop();
					break;
				case 5:
					t--;
					s[t-1] = s[t-1]/s[t];
					typeStack.pop();
					break;
				case 6:
					s[t-1] = s[t-1]%2;
					typeStack.pop();
					break;
				case 8:
					t--;
					s[t-1] = (s[t-1] == s[t] ? 1 : 0);
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				case 9:
					t--;
					s[t-1] = (s[t-1] != s[t] ? 1 : 0);
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				case 10:
					t--;
					s[t-1] = (s[t-1] < s[t] ? 1 : 0);
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				case 11:
					t--;
					s[t-1] = (s[t-1] >= s[t] ? 1 : 0);
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				case 12:
					t--;
					s[t-1] = (s[t-1] > s[t] ? 1 : 0);
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				case 13:
					t--;
					s[t-1] = (s[t-1] <= s[t] ? 1 : 0);
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				case 14:
					if (typeStack.peek() == Objekt.bool) {
						System.out.print(s[t - 1] == 1 ? "true" : "false");
						L24.fa2.print(s[t - 1] == 1 ? "true" : "false");
					} else {
						System.out.print(s[t - 1]);
						L24.fa2.print(s[t - 1]);
					}
					t--;
					break;
				case 15:
					System.out.println("");
					L24.fa2.println();
					break;
				case 16:
					System.out.print("?");
					L24.fa2.print("?");
					s[t] = 0;
					try {
						s[t] = Integer.parseInt(L24.stdin.readLine());
					} catch (Exception e) {}
					L24.fa2.println(s[t]);
					t++;
					//��ʱĬ����������
					typeStack.push(Objekt.number);
					break;
				//�߼���
				case 17:
					t--;
					s[t-1] = (s[t-1]!=0 && s[t]!=0)?1:0;
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				//�߼���
				case 18:
					t--;
					s[t-1] = (s[t-1]!=0 || s[t]!=0)?1:0;
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				//�߼���
				case 19:
					s[t-1] = (s[t-1]==0)?1:0;
					typeStack.pop();
					typeStack.pop();
					typeStack.push(Objekt.bool);
					break;
				}
				break;
			case LOD:				// ȡ��Ե�ǰ���̵����ݻ���ַΪa���ڴ��ֵ��ջ��
				int type = table.getSymbol(base(i.l, s, b), i.a);
				//bool���͵ı���
				if(type == 3 ){
					typeStack.push(Objekt.bool);
				} else {
					typeStack.push(Objekt.number);
				}
				s[t] = s[base(i.l,s,b)+i.a];
				t++;
				break;
			case STO:				// ջ����ֵ�浽��Ե�ǰ���̵����ݻ���ַΪa���ڴ�
				t--;
				s[base(i.l, s, b) + i.a] = s[t];
				typeStack.pop();
				break;
			case CAL:				// �����ӹ���
				s[t] = base(i.l, s, b); 	// ����̬���������ַ��ջ
				s[t+1] = b;					// ����̬���������ַ��ջ
				s[t+2] = p;					// ����ǰָ��ָ����ջ
				b = t;  					// �ı����ַָ��ֵΪ�¹��̵Ļ���ַ
				p = i.a;   					// ��ת
				break;
			case INT:			// �����ڴ�
				t += i.a;
				break;
			case JMP:				// ֱ����ת
				p = i.a;
				break;
			case JPC:				// ������ת����ջ��Ϊ0��ʱ����ת��
				t--;
				if (s[t] == 0)
					p = i.a;
				break;
			}
		} while (p != 0);
	}
	
	/**
	 * ͨ�������Ĳ�β�����øò�Ķ�ջ֡����ַ
	 * @param l Ŀ�����뵱ǰ��εĲ�β�
	 * @param s ����ջ
	 * @param b ��ǰ���ջ֡����ַ
	 * @return Ŀ���εĶ�ջ֡����ַ
	 */
	private int base(int l, int[] s, int b) {
		int b1 = b;
		while (l > 0) {
			b1 = s[b1];
			l --;
		}
		return b1;
	}
}
