import java.sql.SQLOutput;

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
		declbegsys.set(Symbol.boolsym);

		// ������俪ʼ���ż�
		statbegsys = new SymSet(symnum);
		statbegsys.set(Symbol.ident);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);
		statbegsys.set(Symbol.printsym);
		statbegsys.set(Symbol.scansym);
		statbegsys.set(Symbol.forsym);

		// �������ӿ�ʼ���ż�
		facbegsys = new SymSet(symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.tf);
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
		//������main��ͷ
		if(sym == Symbol.mainsym) {
			nextSym();
			if(sym == Symbol.lbrace){
				nextSym();
				nxtlev.set(Symbol.rbrace);
				parseBlock(0, nxtlev);
			}else{
				Err.report(1);
			}
		}else{
			Err.report(2);
		}

	}
	
	/**
	 * �����һ���﷨���ţ�����ֻ�Ǽ򵥵���һ��getsym()
	 */
	public void nextSym() {
		lex.getsym();
		sym =lex.sym;
	}

	/**
	 * ������
	 */
	public void checkSym(Symbol s){
		if(sym == s){
			nextSym();
		} else {
			System.out.println("!!!!"+sym+s);
			Err.report(123);
		}
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
		
		if (lev > L24.levmax)
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
			//<bool����˵������>
			if(sym == Symbol.boolsym) {
				nextSym();
				parseBoolDeclaration(lev);
				while (sym == Symbol.comma){
					nextSym();
					parseBoolDeclaration(lev);
				}
				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(95);
			}

			// <����˵������>
			if (sym == Symbol.varsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do {
				parseVarDeclaration(lev);
				//֧��var a,b����д��
				while (sym == Symbol.comma)
				{
					nextSym();
					parseVarDeclaration(lev);
				}

				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(25);				// ©���˶��Ż��߷ֺ�
				// } while (sym == ident);
			}

			// <����˵������>
			while (sym == Symbol.procsym) {
				nextSym();
				if (sym == Symbol.ident) {
					table.enter(Objekt.procedure, lev, dx);
					nextSym();
				} else {
					Err.report(43);				// procedure��ӦΪ��ʶ��
				}

				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(100);				// ©���˷ֺ�

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
					Err.report(99);				// ©���˷ֺ�
				}
			}

			nxtlev = (SymSet) statbegsys.clone();
			nxtlev.set(Symbol.rbrace);
			//TODO:??
			nxtlev.set(Symbol.boolsym);
			nxtlev.set(Symbol.constsym);
			nxtlev.set(Symbol.varsym);
			test(nxtlev, declbegsys, 127);
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
		while(statbegsys.get(sym)){
			parseStatement(nxtlev, lev);
			checkSym(Symbol.semicolon);
		}
		interp.gen(Fct.OPR, 0, 0);		// ÿ�����̳��ڶ�Ҫʹ�õ��ͷ����ݶ�ָ��
		
		nxtlev = new SymSet(symnum);	// �ֳ���û�в��ȼ���
		test(fsys, nxtlev, 8);				// �����������ȷ��
		//todo:���}
		if(sym != Symbol.rbrace){
			Err.report(96);
		}
		interp.listcode(cx0);
		
		dx = dx0;							// �ָ���ջ֡������
//		table.tx = tx0;						// �ظ����ֱ�λ��
	}

	/**
	 * bool�������
	 * @param lev
	 */
	private void parseBoolDeclaration(int lev) {
		if (sym == Symbol.ident) {
			table.enter(Objekt.bool, lev, dx);
			dx ++;
			nextSym();
		}else{
			Err.report(299);
		}
	}

	/**
	 * ����<����˵������>
	 * @param lev ��ǰ���ڵĲ��
	 */
	void parseConstDeclaration(int lev) {
		if (sym == Symbol.ident) {
			nextSym();
			if (sym == Symbol.becomes) {
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

	void parseStatementList(SymSet fsys, int lev){
		checkSym(Symbol.lbrace);
		do{
			parseStatement(fsys, lev);
			checkSym(Symbol.semicolon);
		}while(sym!=Symbol.rbrace);
		checkSym(Symbol.rbrace);
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
		case scansym:
			parseScanStatement(fsys, lev);
			break;
		case printsym:
			parsePrintStatement(fsys, lev);
			break;
		case callsym:
			parseCallStatement(fsys, lev);
			break;
		case ifsym:
			parseIfStatement(fsys, lev);
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
		checkSym(Symbol.lparen);
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.rparen);
		parseBoolExpr(nxtlev, lev);			// ����<����>
		checkSym(Symbol.rparen);
		cx2 = interp.cx;						// ����ѭ����Ľ�������һ��λ��
		interp.gen(Fct.JPC, 0, 0);				// ����������ת��������ѭ���ĵ�ַδ֪
		parseStatementList(fsys, lev);
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
		checkSym(Symbol.lparen);
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.rparen);
		parseBoolExpr(nxtlev, lev);			// ����<����>
		checkSym(Symbol.rparen);
		if (sym == Symbol.thensym)
			nextSym();
		else
			Err.report(16);						// ȱ��then
		cx1 = interp.cx;						// ���浱ǰָ���ַ
		interp.gen(Fct.JPC, 0, 0);				// ����������תָ���ת��ַδ֪����ʱд0
		parseStatementList(fsys, lev);				// ����then������
		if(sym == Symbol.endsym){
			nextSym();
			interp.code[cx1].a = interp.cx;			// ��statement�����cxΪthen�����ִ�����λ�ã�������ǰ��δ������ת��ַ
		}else if(sym == Symbol.elsesym){
			nextSym();
			interp.gen(Fct.JMP, 0, 0);
			interp.code[cx1].a = interp.cx;
			cx1 = interp.cx - 1;
			parseStatementList(fsys, lev);
			interp.code[cx1].a = interp.cx;
			checkSym(Symbol.endsym);
		}else{
			//ȱ��end��else
			Err.report(85);
		}
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
				Err.report(311);					// ����δ�ҵ�
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
	private void parsePrintStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		Symbol type;
		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);
				type = parseExpression(nxtlev, lev);
				if (type == Symbol.tf){
					interp.gen(Fct.OPR, 0, 14);
				}else{
					interp.gen(Fct.OPR, 0, 14);
				}
			} while (sym == Symbol.comma);

			if (sym == Symbol.rparen)
				nextSym();
			else
				Err.report(33);				// print()��ӦΪ�������ʽ
		} else{
			Err.report(33);					// ȱ��������
		}
		interp.gen(Fct.OPR, 0, 15);//������з�
	}

	/**
	 * ����<�����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseScanStatement(SymSet fsys, int lev) {
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
						item.isInitialized = true;
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
				//��ʾֵ�Ѿ�������
				item.isInitialized = true;
				// parseExpression������һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
				interp.gen(Fct.STO, lev - item.level, item.adr);
			} else if(item.kind == Objekt.bool) {
				nextSym();
				if(sym == Symbol.becomes){
					nextSym();
				}else{
					Err.report(13);
				}
				nxtlev =  (SymSet) fsys.clone();
				parseBoolExpr(nxtlev, lev);
				item.isInitialized = true;
				interp.gen(Fct.STO, lev - item.level, item.adr);
			} else {
				Err.report(12);						// ��ֵ����ʽ����
			}
		} else {
			Err.report(219);							// ����δ�ҵ�
		}
	}

	/**
	 * ����<���ʽ>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private Symbol parseExpression(SymSet fsys, int lev) {
		Symbol addop;
		SymSet nxtlev;
		Symbol lefttype,rightype;
		// ����[+|-]<��>
		if (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			lefttype = parseTerm(nxtlev, lev);
			//�������-bool
			if(lefttype !=Symbol.number){
				Err.report(78);
			}
			if (addop == Symbol.minus)
				interp.gen(Fct.OPR, 0, 1);
		} else {
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			lefttype = parseTerm(nxtlev, lev);
		}
		
		// ����{<�ӷ������><��>}
		while (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			rightype = parseTerm(nxtlev, lev);
			//��������������Ͳ�һ��
			if(rightype != lefttype){
				Err.report(77);
			}
			if (addop == Symbol.plus)
				interp.gen(Fct.OPR, 0, 2);
			else
				interp.gen(Fct.OPR, 0, 3);
		}
		return lefttype;
	}

	/**
	 * ����<��>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private Symbol parseTerm(SymSet fsys, int lev) {
		Symbol mulop;
		SymSet nxtlev;
		Symbol leftType;
		// ����<����>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		leftType = parseFactor(nxtlev, lev);
		
		// ����{<�˷������><����>}
		while (sym == Symbol.times || sym == Symbol.slash) {
			mulop = sym;
			nextSym();
			nxtlev.set(Symbol.times);
			nxtlev.set(Symbol.slash);
			Symbol rightType;
			rightType = parseFactor(nxtlev, lev);
			if(rightType!=leftType){
				Err.report(89);
			}
			if (mulop == Symbol.times)
				interp.gen(Fct.OPR, 0, 4);
			else
				interp.gen(Fct.OPR, 0, 5);
		}
		return leftType;
	}

	/**
	 * ����<����>
	 *
	 * @param fsys ������ż�
	 * @param lev  ��ǰ���
	 * @return
	 */
	private Symbol parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;
		Symbol returnType = Symbol.nul;
		test(facbegsys, fsys, 24);			// ������ӵĿ�ʼ����
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// ����Ϊ���������
				returnType = Symbol.number;
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
						case constant:			// ����Ϊ����
							interp.gen(Fct.LIT, 0, item.val);
							break;
						case variable:// ����Ϊ����
							if(!item.isInitialized){
								Err.report(95);
							}
							interp.gen(Fct.LOD, lev - item.level, item.adr);
							break;
						case bool:
							returnType = Symbol.tf;
							if(!item.isInitialized){
								Err.report(95);
							}
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
			} else if (sym == Symbol.number) {// ����Ϊ��
				returnType = sym;
				int num = lex.num;
				if (num > L24.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// ����Ϊ���ʽ
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				returnType = parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(22);					// ȱ��������
			} else {
				// �����ȴ�ʩ
				test(fsys, facbegsys, 23);
			}
		} else{
			Err.report(86);
			return Symbol.nul;
		}
		return returnType;
	}

	/**
	 * ����<����> <bool_expr>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseBoolExpr(SymSet fsys, int lev) {
		SymSet nxtlev;
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.or);
		parseBoolTerm(fsys, lev);
		while(sym == Symbol.or){
			nextSym();
			parseBoolTerm(fsys, lev);
			interp.gen(Fct.OPR, 0, 18);
		}
	}

	private void parseBoolTerm(SymSet fsys, int lev) {
		SymSet nxtlev;
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.and);
		parseBoolFactor(fsys, lev);
		while(sym == Symbol.and){
			nextSym();
			parseBoolFactor(fsys, lev);
			interp.gen(Fct.OPR, 0, 17); // ���� && ���м����
		}
	}

	private void parseBoolFactor(SymSet fsys, int lev) {
		//����У���ʾ��ǰ��ϵ�����
		Symbol relop;
		SymSet nxtlev;
		if(sym == Symbol.not){
			nextSym();
			parseBoolFactor(fsys, lev);
			interp.gen(Fct.OPR, 0, 19); // ���� ! ���м����
		}else if(sym == Symbol.lparen){
			nextSym();
			parseBoolExpr(fsys, lev);

			if(sym == Symbol.rparen){
				nextSym();
			}else {
				Err.report(22); // ȱ��������
			}
		} else if (sym == Symbol.tf) {
			interp.gen(Fct.LIT, 0, (lex.bNum)?1:0);
			nextSym();
		} else if(sym == Symbol.ident){
  			int i = table.position(lex.id);
			if(i > 0){
				Table.Item item = table.get(i);
				if (item.kind == Objekt.bool){
					interp.gen(Fct.LOD, lev - item.level, item.adr);
					nextSym();
				}
				else {//��int���͵ı���
					nxtlev = (SymSet) fsys.clone();
					nxtlev.set(Symbol.eql);
					nxtlev.set(Symbol.neq);
					nxtlev.set(Symbol.lss);
					nxtlev.set(Symbol.leq);
					nxtlev.set(Symbol.gtr);
					nxtlev.set(Symbol.geq);
					parseExpression(nxtlev, lev);
					if(sym == Symbol.eql || sym == Symbol.neq
							|| sym == Symbol.lss || sym == Symbol.leq
							|| sym == Symbol.gtr || sym == Symbol.geq){
						relop = sym;
						nextSym();
						parseExpression(fsys, lev);
						switch(relop){
							case eql:
								interp.gen(Fct.OPR, 0, 8);
								break;
							case neq:
								interp.gen(Fct.OPR, 0, 9);
								break;
							case lss:
								interp.gen(Fct.OPR, 0, 10);
								break;
							case leq:
								interp.gen(Fct.OPR, 0, 11);
								break;
							case gtr:
								interp.gen(Fct.OPR, 0, 12);
								break;
							case geq:
								interp.gen(Fct.OPR, 0, 13);
								break;
						}
					}
				}
			}
		} else {
			Err.report(86); // �﷨����
		}
	}
}
