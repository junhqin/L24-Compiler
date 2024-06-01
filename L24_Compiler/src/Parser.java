import java.sql.SQLOutput;

/**
 *　　语法分析器。这是PL/0分析器中最重要的部分，在语法分析的过程中穿插着语法错误检查和目标代码生成。
 */
public class Parser {
	private Scanner lex;					// 对词法分析器的引用
	private Table table;					// 对符号表的引用
	private Interpreter interp;				// 对目标代码生成器的引用
	
	private final int symnum = Symbol.values().length;
	
	// 表示声明开始的符号集合、表示语句开始的符号集合、表示因子开始的符号集合
	// 实际上这就是声明、语句和因子的FIRST集合
	private SymSet declbegsys, statbegsys, facbegsys;
	
	/**
	 * 当前符号，由nextsym()读入
	 * @see #nextSym()
	 */
	private Symbol sym;
	
	/**
	 * 当前作用域的堆栈帧大小，或者说数据大小（data size）
	 */
	private int dx = 0;
	
	/**
	 * 构造并初始化语法分析器，这里包含了C语言版本中init()函数的一部分代码
	 * @param l 编译器的词法分析器
	 * @param t 编译器的符号表
	 * @param i 编译器的目标代码生成器
	 */
	public Parser(Scanner l, Table t, Interpreter i) {
		lex = l;
		table = t;
		interp = i;
		
		// 设置声明开始符号集
		declbegsys = new SymSet(symnum);
		declbegsys.set(Symbol.constsym);
		declbegsys.set(Symbol.varsym);
		declbegsys.set(Symbol.procsym);
		declbegsys.set(Symbol.boolsym);

		// 设置语句开始符号集
		statbegsys = new SymSet(symnum);
		statbegsys.set(Symbol.ident);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);
		statbegsys.set(Symbol.printsym);
		statbegsys.set(Symbol.scansym);
		statbegsys.set(Symbol.forsym);

		// 设置因子开始符号集
		facbegsys = new SymSet(symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.tf);
		facbegsys.set(Symbol.lparen);


	}
	
	/**
	 * 启动语法分析过程，此前必须先调用一次nextsym()
	 * @see #nextSym()
	 */
	public void parse() {
		SymSet nxtlev = new SymSet(symnum);
		nxtlev.or(declbegsys);
		nxtlev.or(statbegsys);
		//程序以main开头
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
	 * 获得下一个语法符号，这里只是简单调用一下getsym()
	 */
	public void nextSym() {
		lex.getsym();
		sym =lex.sym;
	}

	/**
	 * 检查符号
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
	 * 测试当前符号是否合法
	 * 
	 * @param s1 我们需要的符号
	 * @param s2 如果不是我们需要的，则需要一个补救用的集合
	 * @param errcode 错误号
	 */
	void test(SymSet s1, SymSet s2, int errcode) {
		// 在某一部分（如一条语句，一个表达式）将要结束时时我们希望下一个符号属于某集合
		//（该部分的后跟符号），test负责这项检测，并且负责当检测不通过时的补救措施，程
		// 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符
		// 号），以及检测不通过时的错误号。
		if (!s1.get(sym)) {
			Err.report(errcode);
			// 当检测不通过时，不停获取符号，直到它属于需要的集合或补救的集合
			while (!s1.get(sym) && !s2.get(sym))
				nextSym();
		}
	}
	
	/**
	 * 分析<分程序>
	 * 
	 * @param lev 当前分程序所在层
	 * @param fsys 当前模块后跟符号集
	 */
	public void parseBlock(int lev, SymSet fsys) {
		// <分程序> := [<常量说明部分>][<变量说明部分>][<过程说明部分>]<语句>
		
		int dx0, tx0, cx0;				// 保留初始dx，tx和cx
		SymSet nxtlev = new SymSet(symnum);
		
		dx0 = dx;						// 记录本层之前的数据量（以便恢复）
		dx = 3;
		tx0 = table.tx;					// 记录本层名字的初始位置（以便恢复）
		table.get(table.tx).adr = interp.cx;
		
		interp.gen(Fct.JMP, 0, 0);
		
		if (lev > L24.levmax)
			Err.report(32);
		// 分析<说明部分>
		do {
			// <常量说明部分>
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
					Err.report(5);				// 漏掉了逗号或者分号
				// } while (sym == ident);
			}
			//<bool变量说明部分>
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

			// <变量说明部分>
			if (sym == Symbol.varsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do {
				parseVarDeclaration(lev);
				//支持var a,b这种写法
				while (sym == Symbol.comma)
				{
					nextSym();
					parseVarDeclaration(lev);
				}

				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(25);				// 漏掉了逗号或者分号
				// } while (sym == ident);
			}

			// <过程说明部分>
			while (sym == Symbol.procsym) {
				nextSym();
				if (sym == Symbol.ident) {
					table.enter(Objekt.procedure, lev, dx);
					nextSym();
				} else {
					Err.report(43);				// procedure后应为标识符
				}

				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(100);				// 漏掉了分号

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
					Err.report(99);				// 漏掉了分号
				}
			}

			nxtlev = (SymSet) statbegsys.clone();
			nxtlev.set(Symbol.rbrace);
			//TODO:??
			nxtlev.set(Symbol.boolsym);
			nxtlev.set(Symbol.constsym);
			nxtlev.set(Symbol.varsym);
			test(nxtlev, declbegsys, 127);
		} while (declbegsys.get(sym));		// 直到没有声明符号
		
		// 开始生成当前过程代码
		Table.Item item = table.get(tx0);
		interp.code[item.adr].a = interp.cx;
		item.adr = interp.cx;					// 当前过程代码地址
		item.size = dx;							// 声明部分中每增加一条声明都会给dx增加1，
												// 声明部分已经结束，dx就是当前过程的堆栈帧大小
		cx0 = interp.cx;
		interp.gen(Fct.INT, 0, dx);			// 生成分配内存代码
		
		table.debugTable(tx0);
			
		// 分析<语句>
		nxtlev = (SymSet) fsys.clone();		// 每个后跟符号集和都包含上层后跟符号集和，以便补救
		nxtlev.set(Symbol.semicolon);		// 语句后跟符号为分号或end
		while(statbegsys.get(sym)){
			parseStatement(nxtlev, lev);
			checkSym(Symbol.semicolon);
		}
		interp.gen(Fct.OPR, 0, 0);		// 每个过程出口都要使用的释放数据段指令
		
		nxtlev = new SymSet(symnum);	// 分程序没有补救集合
		test(fsys, nxtlev, 8);				// 检测后跟符号正确性
		//todo:检查}
		if(sym != Symbol.rbrace){
			Err.report(96);
		}
		interp.listcode(cx0);
		
		dx = dx0;							// 恢复堆栈帧计数器
//		table.tx = tx0;						// 回复名字表位置
	}

	/**
	 * bool定义语句
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
	 * 分析<常量说明部分>
	 * @param lev 当前所在的层次
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
					Err.report(2);			// 常量说明 = 后应是数字
				}
			} else {
				Err.report(3);				// 常量说明标识后应是 =
			}
		} else {
			Err.report(4);					// const 后应是标识符
		}
	}

	/**
	 * 分析<变量说明部分>
	 * @param lev 当前层次
	 */
	void parseVarDeclaration(int lev) {
		if (sym == Symbol.ident) {
			// 填写名字表并改变堆栈帧计数器
			table.enter(Objekt.variable, lev, dx);
			dx ++;
			nextSym();
		} else {
			Err.report(4);					// var 后应是标识
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
	 * 分析<语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	void parseStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		// Wirth 的 PL/0 编译器使用一系列的if...else...来处理
		// 但是你的助教认为下面的写法能够更加清楚地看出这个函数的处理逻辑
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
	 * 分析<当型循环语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWhileStatement(SymSet fsys, int lev) {
		int cx1, cx2;
		SymSet nxtlev;
		cx1 = interp.cx;						// 保存判断条件操作的位置
		nextSym();
		checkSym(Symbol.lparen);
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.rparen);
		parseBoolExpr(nxtlev, lev);			// 分析<条件>
		checkSym(Symbol.rparen);
		cx2 = interp.cx;						// 保存循环体的结束的下一个位置
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转，但跳出循环的地址未知
		parseStatementList(fsys, lev);
		interp.gen(Fct.JMP, 0, cx1);			// 回头重新判断条件
		interp.code[cx2].a = interp.cx;			// 反填跳出循环的地址，与<条件语句>类似
	}

	/**
	 * 分析<复合语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBeginStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.semicolon);
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		// 循环分析{; <语句>}，直到下一个符号不是语句开始符号或收到end
		while (statbegsys.get(sym) || sym == Symbol.semicolon) {
			if (sym == Symbol.semicolon)
				nextSym();
			else
				Err.report(10);					// 缺少分号
			parseStatement(nxtlev, lev);
		}
		if (sym == Symbol.endsym)
			nextSym();
		else
			Err.report(17);						// 缺少end或分号
	}

	/**
	 * 分析<条件语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseIfStatement(SymSet fsys, int lev) {
		int cx1;
		SymSet nxtlev;
		nextSym();
		checkSym(Symbol.lparen);
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.rparen);
		parseBoolExpr(nxtlev, lev);			// 分析<条件>
		checkSym(Symbol.rparen);
		if (sym == Symbol.thensym)
			nextSym();
		else
			Err.report(16);						// 缺少then
		cx1 = interp.cx;						// 保存当前指令地址
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转指令，跳转地址未知，暂时写0
		parseStatementList(fsys, lev);				// 处理then后的语句
		if(sym == Symbol.endsym){
			nextSym();
			interp.code[cx1].a = interp.cx;			// 经statement处理后，cx为then后语句执行完的位置，它正是前面未定的跳转地址
		}else if(sym == Symbol.elsesym){
			nextSym();
			interp.gen(Fct.JMP, 0, 0);
			interp.code[cx1].a = interp.cx;
			cx1 = interp.cx - 1;
			parseStatementList(fsys, lev);
			interp.code[cx1].a = interp.cx;
			checkSym(Symbol.endsym);
		}else{
			//缺少end或else
			Err.report(85);
		}
	}

	/**
	 * 分析<过程调用语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCallStatement(SymSet fsys, int lev) {
		int i;
		nextSym();
		if (sym == Symbol.ident) {
			i = table.position(lex.id);
			if (i == 0) {
				Err.report(311);					// 过程未找到
			} else {
				Table.Item item = table.get(i);
				if (item.kind == Objekt.procedure)
					interp.gen(Fct.CAL, lev - item.level, item.adr);
				else
					Err.report(15);				// call后标识符应为过程
			}
			nextSym();
		} else {
			Err.report(14);						// call后应为标识符
		}
	}

	/**
	 * 分析<写语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
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
				Err.report(33);				// print()中应为完整表达式
		} else{
			Err.report(33);					// 缺少左括号
		}
		interp.gen(Fct.OPR, 0, 15);//输出换行符
	}

	/**
	 * 分析<读语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
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
					Err.report(35);			// read()中应是声明过的变量名
				} else {
					Table.Item item = table.get(i);
					if (item.kind != Objekt.variable) {
						Err.report(32);		// read()中的标识符不是变量, thanks to amd
					} else {
						interp.gen(Fct.OPR, 0, 16);
						item.isInitialized = true;
						interp.gen(Fct.STO, lev-item.level, item.adr);
					}
				}
				
				nextSym();
			} while (sym == Symbol.comma);
		} else {
			Err.report(34);					// 格式错误，应是左括号
		}
		
		if (sym == Symbol.rparen) {
			nextSym();
		} else {
			Err.report(33);					// 格式错误，应是右括号
			while (!fsys.get(sym))
				nextSym();
		}
	}

	/**
	 * 分析<赋值语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
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
					Err.report(13);					// 没有检测到赋值符号
				nxtlev = (SymSet) fsys.clone();
				parseExpression(nxtlev, lev);
				//表示值已经被声明
				item.isInitialized = true;
				// parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
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
				Err.report(12);						// 赋值语句格式错误
			}
		} else {
			Err.report(219);							// 变量未找到
		}
	}

	/**
	 * 分析<表达式>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private Symbol parseExpression(SymSet fsys, int lev) {
		Symbol addop;
		SymSet nxtlev;
		Symbol lefttype,rightype;
		// 分析[+|-]<项>
		if (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			lefttype = parseTerm(nxtlev, lev);
			//避免出现-bool
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
		
		// 分析{<加法运算符><项>}
		while (sym == Symbol.plus || sym == Symbol.minus) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			rightype = parseTerm(nxtlev, lev);
			//避免出现左右类型不一致
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
	 * 分析<项>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private Symbol parseTerm(SymSet fsys, int lev) {
		Symbol mulop;
		SymSet nxtlev;
		Symbol leftType;
		// 分析<因子>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		leftType = parseFactor(nxtlev, lev);
		
		// 分析{<乘法运算符><因子>}
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
	 * 分析<因子>
	 *
	 * @param fsys 后跟符号集
	 * @param lev  当前层次
	 * @return
	 */
	private Symbol parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;
		Symbol returnType = Symbol.nul;
		test(facbegsys, fsys, 24);			// 检测因子的开始符号
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// 因子为常量或变量
				returnType = Symbol.number;
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
						case constant:			// 名字为常量
							interp.gen(Fct.LIT, 0, item.val);
							break;
						case variable:// 名字为变量
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
						case procedure:			// 名字为过程
							Err.report(21);				// 不能为过程
							break;
					}
				} else {
					Err.report(11);					// 标识符未声明
				}
				nextSym();
			} else if (sym == Symbol.number) {// 因子为数
				returnType = sym;
				int num = lex.num;
				if (num > L24.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// 因子为表达式
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				returnType = parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(22);					// 缺少右括号
			} else {
				// 做补救措施
				test(fsys, facbegsys, 23);
			}
		} else{
			Err.report(86);
			return Symbol.nul;
		}
		return returnType;
	}

	/**
	 * 分析<条件> <bool_expr>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
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
			interp.gen(Fct.OPR, 0, 17); // 生成 && 的中间代码
		}
	}

	private void parseBoolFactor(SymSet fsys, int lev) {
		//如果有，表示当前关系运算符
		Symbol relop;
		SymSet nxtlev;
		if(sym == Symbol.not){
			nextSym();
			parseBoolFactor(fsys, lev);
			interp.gen(Fct.OPR, 0, 19); // 生成 ! 的中间代码
		}else if(sym == Symbol.lparen){
			nextSym();
			parseBoolExpr(fsys, lev);

			if(sym == Symbol.rparen){
				nextSym();
			}else {
				Err.report(22); // 缺少右括号
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
				else {//是int类型的变量
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
			Err.report(86); // 语法错误
		}
	}
}
