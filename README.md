��������EBNF������L24���ԣ�ʹ�õ���EBNFԪ���ź���ͬ�̲�P13
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
<bool_factor> = "true" | "false" | <ident> ��<expr> ("=="|"!="|"<"|"<="|">"|">=") <expr> | "!" <bool_factor> | "(" <bool_expr> ")"
<expr> = ["+"|"-"] <term> {("+"|"-") <term>}
<term> = <factor> {("*"|"/") <factor>}
<factor> = <ident> | <number> | "("<expr>")"
<const_declaration> = "const" <ident> "=" <number> {"," <ident> "=" <number>} ";"
<var_declaration> = "var" <ident> {"," <ident>} ";"
<bool_declaration> = "bool" <ident> {"," <ident>} ";"
````

ע��
1��	���е��ս�����Ӵֲ���˫���������ʾ��
2��	�������еı�ʶ�����Ǽ򵥵����ͱ�������ͨ��������assign_stmt����������ʽ������һ����������������������ȫ�ֵġ�

������Ҫʵ�ֵĹ��ܣ�
-	���Ӳ������ͣ�bool�����߼�����������롱���򡱡��ǡ���
-	���ӱ���������䣬Ҫ�����б�����Ҫ��������ʹ��

����չ��
-	�������κ��������͹����ȷ���Զ�ά���飨������ά�����������������飬���������顢������Ԫ�ظ�ֵ���ڱ��ʽ����������Ԫ�ص�
-	���Ӽ�¼���ṹ�壩�Ķ����ʹ��
-	����ָ�����͵Ķ����ʹ��
-	����������ͷ���ֵ�Ĺ��̣����й涨�������Ƿ��������Ƕ��
-	�ṩ�Ѻõı��������棬�����ɱ༭�Ĵ���򡢱����Ŀ�����ָ����ʾ�򡢱��������ʱ���ݹ�����ʾ�򡢱������ʾ������н����ʾ��ȣ���֧�ֵ���ͱ�������ļ�
-	�ڱ�������ʵ����ʹ�ñ��γ�δ���ܹ���ҵ����빤��/��ܣ���ANTLR��LLVM��

![img.png](img.png)