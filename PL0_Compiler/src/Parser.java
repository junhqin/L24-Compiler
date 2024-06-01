/**
 *�����﷨������������PL/0������������Ҫ�Ĳ��֣����﷨�����Ĺ����д������﷨�������Ŀ��������ɡ�
 */
public class Parser {
	private Scanner lex;					// �Դʷ�������������
	private Table table;					// �Է��ű������
	private Interpreter interp;				// ��Ŀ�����������������
	
	private final int symnum = Symbol.values().length;
	
	// ��ʾ������ʼ�ķ��ż��ϡ���ʾ��俪ʼ�ķ��ż��ϡ���ʾ���ӿ�ʼ�ķ��ż���
	// ʵ����������������������ӵ�FIRST����
	private SymSet declbegsys, statbegsys, facbegsys;
	
	/**
	 * ��ǰ���ţ���nextsym()����
	 * @see #nextSym()
	 */
	private Symbol sym;
	
	/**
	 * ��ǰ������Ķ�ջ֡��С������˵���ݴ�С��data size��
	 */
	private int dx = 0;
	
	/**
	 * ���첢��ʼ���﷨�����������������C���԰汾��init()������һ���ִ���
	 * @param l �������Ĵʷ�������
	 * @param t �������ķ��ű�
	 * @param i ��������Ŀ�����������
	 */
	public Parser(Scanner l, Table t, Interpreter i) {
		lex = l;
		table = t;
		interp = i;
		
		// ����������ʼ���ż�
		declbegsys = new SymSet(symnum);
		declbegsys.set(Symbol.constsym);
		declbegsys.set(Symbol.varsym);
		declbegsys.set(Symbol.procsym);

		// ������俪ʼ���ż�
		statbegsys = new SymSet(symnum);
		statbegsys.set(Symbol.beginsym);
		statbegsys.set(Symbol.callsym);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);
		statbegsys.set(Symbol.readsym);			// thanks to elu
		statbegsys.set(Symbol.writesym);

		// �������ӿ�ʼ���ż�
		facbegsys = new SymSet(symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.lparen);

	}
	
	/**
	 * �����﷨�������̣���ǰ�����ȵ���һ��nextsym()
	 * @see #nextSym()
	 */
	public void parse() {
		SymSet nxtlev = new SymSet(symnum);
		nxtlev.or(declbegsys);
		nxtlev.or(statbegsys);
		nxtlev.set(Symbol.period);
		parseBlock(0, nxtlev);
		
		if (sym != Symbol.period)
			Err.report(9);
	}
	
	/**
	 * �����һ���﷨���ţ�����ֻ�Ǽ򵥵���һ��getsym()
	 */
	public void nextSym() {
		lex.getsym();
		sym =lex.sym;
	}
	
	/**
	 * ���Ե�ǰ�����Ƿ�Ϸ�
	 * 
	 * @param s1 ������Ҫ�ķ���
	 * @param s2 �������������Ҫ�ģ�����Ҫһ�������õļ���
	 * @param errcode �����
	 */
	void test(SymSet s1, SymSet s2, int errcode) {
		// ��ĳһ���֣���һ����䣬һ�����ʽ����Ҫ����ʱʱ����ϣ����һ����������ĳ����
		//���ò��ֵĺ�����ţ���test���������⣬���Ҹ��𵱼�ⲻͨ��ʱ�Ĳ��ȴ�ʩ����
		// ������Ҫ���ʱָ����ǰ��Ҫ�ķ��ż��ϺͲ����õļ��ϣ���֮ǰδ��ɲ��ֵĺ����
		// �ţ����Լ���ⲻͨ��ʱ�Ĵ���š�
		if (!s1.get(sym)) {
			Err.report(errcode);
			// ����ⲻͨ��ʱ����ͣ��ȡ���ţ�ֱ����������Ҫ�ļ��ϻ򲹾ȵļ���
			while (!s1.get(sym) && !s2.get(sym))
				nextSym();
		}
	}
	
	/**
	 * ����<�ֳ���>
	 * 
	 * @param lev ��ǰ�ֳ������ڲ�
	 * @param fsys ��ǰģ�������ż�
	 */
	public void parseBlock(int lev, SymSet fsys) {
		// <�ֳ���> := [<����˵������>][<����˵������>][<����˵������>]<���>
		
		int dx0, tx0, cx0;				// ������ʼdx��tx��cx
		SymSet nxtlev = new SymSet(symnum);
		
		dx0 = dx;						// ��¼����֮ǰ�����������Ա�ָ���
		dx = 3;
		tx0 = table.tx;					// ��¼�������ֵĳ�ʼλ�ã��Ա�ָ���
		table.get(table.tx).adr = interp.cx;
		
		interp.gen(Fct.JMP, 0, 0);
		
		if (lev > PL0.levmax)
			Err.report(32);
		
		// ����<˵������>
		do {
			// <����˵������>
			if (sym == Symbol.constsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do
				parseConstDeclaration(lev);
				while (sym == Symbol.comma) {
					nextSym();
					parseConstDeclaration(lev);
				}
				
				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// ©���˶��Ż��߷ֺ�
				// } while (sym == ident);
			}
			
			// <����˵������>
			if (sym == Symbol.varsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do {
				parseVarDeclaration(lev);
				while (sym == Symbol.comma)
				{
					nextSym();
					parseVarDeclaration(lev);
				}
				
				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// ©���˶��Ż��߷ֺ�
				// } while (sym == ident);
			}
			
			// <����˵������>
			while (sym == Symbol.procsym) {
				nextSym();
				if (sym == Symbol.ident) {
					table.enter(Objekt.procedure, lev, dx);
					nextSym();
				} else { 
					Err.report(4);				// procedure��ӦΪ��ʶ��
				}

				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// ©���˷ֺ�
				
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.semicolon);
				parseBlock(lev+1, nxtlev);
				
				if (sym == Symbol.semicolon) {
					nextSym();
					nxtlev = (SymSet) statbegsys.clone();
					nxtlev.set(Symbol.ident);
					nxtlev.set(Symbol.procsym);
					test(nxtlev, fsys, 6);
				} else { 
					Err.report(5);				// ©���˷ֺ�
				}
			}
			
			nxtlev = (SymSet) statbegsys.clone(); 
			nxtlev.set(Symbol.ident);
			test(nxtlev, declbegsys, 7);
		} while (declbegsys.get(sym));		// ֱ��û����������
		
		// ��ʼ���ɵ�ǰ���̴���
		Table.Item item = table.get(tx0);
		interp.code[item.adr].a = interp.cx;
		item.adr = interp.cx;					// ��ǰ���̴����ַ
		item.size = dx;							// ����������ÿ����һ�����������dx����1��
												// ���������Ѿ�������dx���ǵ�ǰ���̵Ķ�ջ֡��С
		cx0 = interp.cx;
		interp.gen(Fct.INT, 0, dx);			// ���ɷ����ڴ����
		
		table.debugTable(tx0);
			
		// ����<���>
		nxtlev = (SymSet) fsys.clone();		// ÿ��������ż��Ͷ������ϲ������ż��ͣ��Ա㲹��
		nxtlev.set(Symbol.semicolon);		// ���������Ϊ�ֺŻ�end
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		interp.gen(Fct.OPR, 0, 0);		// ÿ�����̳��ڶ�Ҫʹ�õ��ͷ����ݶ�ָ��
		
		nxtlev = new SymSet(symnum);	// �ֳ���û�в��ȼ���
		test(fsys, nxtlev, 8);				// �����������ȷ��
		
		interp.listcode(cx0);
		
		dx = dx0;							// �ָ���ջ֡������
		table.tx = tx0;						// �ظ����ֱ�λ��
	}

	/**
	 * ����<����˵������>
	 * @param lev ��ǰ���ڵĲ��
	 */
	void parseConstDeclaration(int lev) {
		if (sym == Symbol.ident) {
			nextSym();
			if (sym == Symbol.eql || sym == Symbol.becomes) {
				if (sym == Symbol.becomes) 
					Err.report(1);			// �� = д���� :=
				nextSym();
				if (sym == Symbol.number) {
					table.enter(Objekt.constant, lev, dx);
					nextSym();
				} else {
					Err.report(2);			// ����˵�� = ��Ӧ������
				}
			} else {
				Err.report(3);				// ����˵����ʶ��Ӧ�� =
			}
		} else {
			Err.report(4);					// const ��Ӧ�Ǳ�ʶ��
		}
	}

	/**
	 * ����<����˵������>
	 * @param lev ��ǰ���
	 */
	void parseVarDeclaration(int lev) {
		if (sym == Symbol.ident) {
			// ��д���ֱ��ı��ջ֡������
			table.enter(Objekt.variable, lev, dx);
			dx ++;
			nextSym();
		} else {
			Err.report(4);					// var ��Ӧ�Ǳ�ʶ
		}
	}

	/**
	 * ����<���>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	void parseStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		// Wirth �� PL/0 ������ʹ��һϵ�е�if...else...������
		// �������������Ϊ�����д���ܹ���������ؿ�����������Ĵ����߼�
		switch (sym) {
		case ident:
			parseAssignStatement(fsys, lev);
			break;
		case readsym:
			parseReadStatement(fsys, lev);
			break;
		case writesym:
			parseWriteStatement(fsys, lev);
			break;
		case callsym:
			parseCallStatement(fsys, lev);
			break;
		case ifsym:
			parseIfStatement(fsys, lev);
			break;
		case beginsym:
			parseBeginStatement(fsys, lev);
			break;
		case whilesym:
			parseWhileStatement(fsys, lev);
			break;
		default:
			nxtlev = new SymSet(symnum);
			test(fsys, nxtlev, 19);
			break;
		}
	}

	/**
	 * ����<����ѭ�����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseWhileStatement(SymSet fsys, int lev) {
		int cx1, cx2;
		SymSet nxtlev;
		
		cx1 = interp.cx;						// �����ж�����������λ��
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.dosym);				// �������Ϊdo
		parseCondition(nxtlev, lev);			// ����<����>
		cx2 = interp.cx;						// ����ѭ����Ľ�������һ��λ��
		interp.gen(Fct.JPC, 0, 0);				// ����������ת��������ѭ���ĵ�ַδ֪
		if (sym == Symbol.dosym)
			nextSym();
		else
			Err.report(18);						// ȱ��do
		parseStatement(fsys, lev);				// ����<���>
		interp.gen(Fct.JMP, 0, cx1);			// ��ͷ�����ж�����
		interp.code[cx2].a = interp.cx;			// ��������ѭ���ĵ�ַ����<�������>����
	}

	/**
	 * ����<�������>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseBeginStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.semicolon);
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		// ѭ������{; <���>}��ֱ����һ�����Ų�����俪ʼ���Ż��յ�end
		while (statbegsys.get(sym) || sym == Symbol.semicolon) {
			if (sym == Symbol.semicolon)
				nextSym();
			else
				Err.report(10);					// ȱ�ٷֺ�
			parseStatement(nxtlev, lev);
		}
		if (sym == Symbol.endsym)
			nextSym();
		else
			Err.report(17);						// ȱ��end��ֺ�
	}

	/**
	 * ����<�������>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseIfStatement(SymSet fsys, int lev) {
		int cx1;
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.thensym);				// �������Ϊthen��do ???
		nxtlev.set(Symbol.dosym);
		parseCondition(nxtlev, lev);			// ����<����>
		if (sym == Symbol.thensym)
			nextSym();
		else
			Err.report(16);						// ȱ��then
		cx1 = interp.cx;						// ���浱ǰָ���ַ
		interp.gen(Fct.JPC, 0, 0);				// ����������תָ���ת��ַδ֪����ʱд0
		parseStatement(fsys, lev);				// ����then������
		interp.code[cx1].a = interp.cx;			// ��statement�����cxΪthen�����ִ��
												// ���λ�ã�������ǰ��δ������ת��ַ
	}

	/**
	 * ����<���̵������>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseCallStatement(SymSet fsys, int lev) {
		int i;
		nextSym();
		if (sym == Symbol.ident) {
			i = table.position(lex.id);
			if (i == 0) {
				Err.report(11);					// ����δ�ҵ�
			} else {
				Table.Item item = table.get(i);
				if (item.kind == Objekt.procedure)
					interp.gen(Fct.CAL, lev - item.level, item.adr);
				else
					Err.report(15);				// call���ʶ��ӦΪ����
			}
			nextSym();
		} else {
			Err.report(14);						// call��ӦΪ��ʶ��
		}
	}

	/**
	 * ����<д���>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseWriteStatement(SymSet fsys, int lev) {
		SymSet nxtlev;

		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR, 0, 14);
			} while (sym == Symbol.comma);
			
			if (sym == Symbol.rparen)
				nextSym();
			else
				Err.report(33);				// write()��ӦΪ�������ʽ
		}
		interp.gen(Fct.OPR, 0, 15);
	}

	/**
	 * ����<�����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseReadStatement(SymSet fsys, int lev) {
		int i;
		
		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				if (sym == Symbol.ident)
					i = table.position(lex.id);
				else
					i = 0;
				
				if (i == 0) {
					Err.report(35);			// read()��Ӧ���������ı�����
				} else {
					Table.Item item = table.get(i);
					if (item.kind != Objekt.variable) {
						Err.report(32);		// read()�еı�ʶ�����Ǳ���, thanks to amd
					} else {
						interp.gen(Fct.OPR, 0, 16);
						interp.gen(Fct.STO, lev-item.level, item.adr);
					}
				}
				
				nextSym();
			} while (sym == Symbol.comma);
		} else {
			Err.report(34);					// ��ʽ����Ӧ��������
		}
		
		if (sym == Symbol.rparen) {
			nextSym();
		} else {
			Err.report(33);					// ��ʽ����Ӧ��������
			while (!fsys.get(sym))
				nextSym();
		}
	}

	/**
	 * ����<��ֵ���>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseAssignStatement(SymSet fsys, int lev) {
		int i;
		SymSet nxtlev;
		
		i = table.position(lex.id);
		if (i > 0) {
			Table.Item item = table.get(i);
			if (item.kind == Objekt.variable) {
				nextSym();
				if (sym == Symbol.becomes)
					nextSym();
				else
					Err.report(13);					// û�м�⵽��ֵ����
				nxtlev = (SymSet) fsys.clone();
				parseExpression(nxtlev, lev);
				// parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
				interp.gen(Fct.STO, lev - item.level, item.adr);
			} else {
				Err.report(12);						// ��ֵ����ʽ����
			}
		} else {
			Err.report(11);							// ����δ�ҵ�
		}
	}

	/**
	 * ����<���ʽ>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseExpression(SymSet fsys, int lev) {
		Symbol addop;
		SymSet nxtlev;
		
		// ����[+|-]<��>
		if (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.minus)
				interp.gen(Fct.OPR, 0, 1);
		} else {
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
		}
		
		// ����{<�ӷ������><��>}
		while (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.plus)
				interp.gen(Fct.OPR, 0, 2);
			else
				interp.gen(Fct.OPR, 0, 3);
		}
	}

	/**
	 * ����<��>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseTerm(SymSet fsys, int lev) {
		Symbol mulop;
		SymSet nxtlev;

		// ����<����>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		parseFactor(nxtlev, lev);
		
		// ����{<�˷������><����>}
		while (sym == Symbol.times || sym == Symbol.slash) {
			mulop = sym;
			nextSym();
			parseFactor(nxtlev, lev);
			if (mulop == Symbol.times)
				interp.gen(Fct.OPR, 0, 4);
			else
				interp.gen(Fct.OPR, 0, 5);
		}
	}

	/**
	 * ����<����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		test(facbegsys, fsys, 24);			// ������ӵĿ�ʼ����
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// ����Ϊ���������
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
					case constant:			// ����Ϊ����
						interp.gen(Fct.LIT, 0, item.val);
						break;
					case variable:			// ����Ϊ����
						interp.gen(Fct.LOD, lev - item.level, item.adr);
						break;
					case procedure:			// ����Ϊ����
						Err.report(21);				// ����Ϊ����
						break;
					}
				} else {
					Err.report(11);					// ��ʶ��δ����
				}
				nextSym();
			} else if (sym == Symbol.number) {	// ����Ϊ�� 
				int num = lex.num;
				if (num > PL0.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// ����Ϊ���ʽ
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(22);					// ȱ��������
			} else {
				// �����ȴ�ʩ
				test(fsys, facbegsys, 23);
			}
		}
	}

	/**
	 * ����<����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseCondition(SymSet fsys, int lev) {
		Symbol relop;
		SymSet nxtlev;
		
		if (sym == Symbol.oddsym) {
			// ���� ODD<���ʽ>
			nextSym();
			parseExpression(fsys, lev);
			interp.gen(Fct.OPR, 0, 6);
		} else {
			// ����<���ʽ><��ϵ�����><���ʽ>
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.eql);
			nxtlev.set(Symbol.neq);
			nxtlev.set(Symbol.lss);
			nxtlev.set(Symbol.leq);
			nxtlev.set(Symbol.gtr);
			nxtlev.set(Symbol.geq);
			parseExpression(nxtlev, lev);
			if (sym == Symbol.eql || sym == Symbol.neq 
					|| sym == Symbol.lss || sym == Symbol.leq
					|| sym == Symbol.gtr || sym == Symbol.geq) {
				relop = sym;
				nextSym();
				parseExpression(fsys, lev);
				switch (relop) {
				case eql:
					interp.gen(Fct.OPR, 0, 8);
					break;
				case neq:
					interp.gen(Fct.OPR, 0, 9);
					break;
				case lss:
					interp.gen(Fct.OPR, 0, 10);
					break;
				case geq:
					interp.gen(Fct.OPR, 0, 11);
					break;
				case gtr:
					interp.gen(Fct.OPR, 0, 12);
					break;
				case leq:
					interp.gen(Fct.OPR, 0, 13);
					break;
				}
			} else {
				Err.report(20);
			}
		}
	}
}
