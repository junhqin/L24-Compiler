下面是用EBNF描述的L24语言，使用到的EBNF元符号含义同教材P13。
<program> = "main" "{" <stmt_list> "}"
<stmt_list> = {<stmt> ";"}
<stmt> = <assign_stmt>| <if_stmt> |<while_stmt> |<scan_stmt> | <print_stmt>
<assign_stmt> = <ident> "=" <expr>
<if_stmt> = "if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "end"
|"if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "else" "{"<stmt_list>"}" "end"
<while_stmt> = "while" "("<bool_expr>")" "{"<stmt_list>"}"
<scan_stmt> = "scan" "(" <ident {"," <ident>} ")"
<print_stmt> = "print" "(" <expr> {"," <expr>} ")"
<bool_expr> = <expr> ("=="|"!="|"<"|"<="|">"|">=") <expr>
<expr> = ["+"|"-"] <term> {("+"|"-") <term>}
<term> = <factor> {("*"|"/") <factor>}
<factor> = <ident> | <number> | "("<expr>")"

注：
1、	所有的终结符均加粗并用双引号括起表示。
2、	该语言中的标识符都是简单的整型变量；它通过出现在assign_stmt语句左边来隐式地声明一个变量。变量的作用域是全局的。

额外需要实现的功能：
-	增加布尔类型（bool）及逻辑运算符（“与”“或”“非”）
-	增加变量声明语句，要求所有变量需要先声明后使用
