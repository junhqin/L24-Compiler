下面是用EBNF描述的L24语言，使用到的EBNF元符号含义同教材P13
````
<program> = "main" <block>
<block> = "{"[<const_declaration>] [<var_declaration>] [<bool_declaration>] <stmt_list> "}"
<stmt_list> = {<stmt> ";"}
<stmt> = <assign_stmt>| <if_stmt> |<while_stmt> |<scan_stmt> | <print_stmt>
<assign_stmt> = <ident> "=" <assign_expr>
<assign_expr> = <expr> | <bool_expr>
<if_stmt> = "if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "end"
|"if" "("<bool_expr>")" "then" "{"<stmt_list>"}" "else" "{"<stmt_list>"}" "end"
<while_stmt> = "while" "("<bool_expr>")" "{"<stmt_list>"}"
<scan_stmt> = "scan" "(" <ident> {"," <ident>} ")"
<print_stmt> = "print" "(" <expr> {"," <expr>} ")"
<bool_expr> = <bool_term> {("||") <bool_term>}
<bool_term> = <bool_factor> {("&&") <bool_factor>}
<bool_factor> = "true" | "false" | <ident> ｜<expr> ("=="|"!="|"<"|"<="|">"|">=") <expr> | "!" <bool_factor> | "(" <bool_expr> ")"
<expr> = ["+"|"-"] <term> {("+"|"-") <term>}
<term> = <factor> {("*"|"/") <factor>}
<factor> = <ident> | <number> | "("<expr>")"
<const_declaration> = "const" <ident> "=" <number> {"," <ident> "=" <number>} ";"
<var_declaration> = "var" <ident> {"," <ident>} ";"
<bool_declaration> = "bool" <ident> {"," <ident>} ";"
````

注：
1、	所有的终结符均加粗并用双引号括起表示。
2、	该语言中的标识符都是简单的整型变量；它通过出现在assign_stmt语句左边来隐式地声明一个变量。变量的作用域是全局的。

额外需要实现的功能：
-	增加布尔类型（bool）及逻辑运算符（“与”“或”“非”）
-	增加变量声明语句，要求所有变量需要先声明后使用

可扩展：
-	增加由任何数据类型构造的确定性多维数组（至少三维），如整型数据数组，允许定义数组、对数组元素赋值、在表达式中引用数组元素等
-	增加记录（结构体）的定义和使用
-	增加指针类型的定义和使用
-	扩充带参数和返回值的过程，自行规定编译器是否允许过程嵌套
-	提供友好的编译器界面，包括可编辑的代码框、编译后目标机器指令显示框、编译后及运行时数据管理显示框、编译错误警示框和运行结果显示框等，并支持导入和保存程序文件
-	在编译器的实现中使用本课程未介绍过的业界编译工具/框架，如ANTLR、LLVM等

src
├── Err.java
├── Interpreter.java
├── L24.java
├── Parser.java
├── Scanner.java
├── SymSet.java
├── Symbol.java
└── Table.java