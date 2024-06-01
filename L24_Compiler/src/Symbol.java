
public enum Symbol {
	/**
	 * nil:非法符号
	 */
	nul, ident, number, plus, minus, times, slash, and, or, not, tf, falseValue, trueValue,
	oddsym, eql, lss, leq, gtr, geq, lparen, rparen, lbrace, rbrace, neq,
	/**
	 * comma:","
	 * semicolon:";"
	 * becomes:":"
	 */
	comma, semicolon, becomes,
	mainsym, forsym, ifsym, elsesym, thensym, endsym, whilesym, printsym, scansym,
	readsym, dosym, callsym, constsym, varsym, procsym, boolsym
}