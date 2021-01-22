### KeK-Language Grammar Summary
```bnf
variable-declaration ::= "var" variable-identifier ":" type-identifier
variable-identifier ::= identifier
type-identifier ::= identifier
while-loop ::= "while" "(" condition ")" while-body
while-body ::= "{" statements "}"
whitespace ::= " "
whitespace ::= line-break
line-comment ::= "#" <any> line-break
line-break ::= "\n"
line-break ::= "\r\n"
number ::= { "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" }
arrow ::= "->"
colon ::= ":"
operator ::= { "/" | "=" | "-" | "+" | "!" | "*" | "%" | "<" | ">" | "&" | "|" | "^" | "~" | "?" | ":" }
keyword ::= "func"
keyword ::= "operator"
keyword ::= "if"
keyword ::= "else"
keyword ::= "var"
keyword ::= "while"
keyword ::= "builtin"
string-literal ::= "\"" <any> "\""
```
