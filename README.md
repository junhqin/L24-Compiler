��������EBNF������L24���ԣ�ʹ�õ���EBNFԪ���ź���ͬ�̲�P13��
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

ע��
1��	���е��ս�����Ӵֲ���˫���������ʾ��
2��	�������еı�ʶ�����Ǽ򵥵����ͱ�������ͨ��������assign_stmt����������ʽ������һ����������������������ȫ�ֵġ�

������Ҫʵ�ֵĹ��ܣ�
-	���Ӳ������ͣ�bool�����߼�����������롱���򡱡��ǡ���
-	���ӱ���������䣬Ҫ�����б�����Ҫ��������ʹ��
