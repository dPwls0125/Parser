import java.io.IOException;

import java.io.PushbackInputStream;
enum Relop {
    LPAREN, RPAREN, PLUS, MINUS, MULTI, DIVIDE,
    AND, OR, NUMBER, NOT,
    EQUAL, LT, LTEQ, GT, GTEQ, NOTEQ, NLINE
};
/*  Meaning of enum symbols
	LPAREN("("), RPAREN(")"), PLUS("+"), MINUS("-"), MULTI("*"),	 DIVIDE("/"),
	AND("&"), OR("|"), NUMBER(""), NOT("!"),
	EQUAL("=="), LT("<"), LTEQ("<="), GT(">"), GTEQ(">="), NOTEQ("!="), NLINE("\n");
*/
public class Calculator {
    Relop token; int value; int ch; int ch2;
    boolean bool;
    private PushbackInputStream input;
    final int TRUE = 1;
    final int FALSE = 0;

    Calculator(PushbackInputStream is) {
        input = is;
    }

    Relop getToken( )  {
        while(true) {
            try  {
                ch = input.read();
                if (ch == ' ' || ch == '\t' || ch == '\r') ;
                else if (Character.isDigit(ch)) {
                    value = number( ); // 여기서 ch와 그 이후의 숫자들을 읽어서 value에 저장함.
                    input.unread(ch);
                    return Relop.NUMBER;
                }
                else if (ch == '='){
                    ch2 = input.read();
                    if (ch2 == '=')
                        return Relop.EQUAL;
                    else {
                        /*
                        여기서 unread를 추가해주어햐 함.
                        unread하지 않으면 후에 getToken함수를 시행할 때, '='다음의 값이 하나가 무시되는 상황이 발생함. 왜냐면 ch2에 담긴
                        값이 버퍼에서 사라져 다시 읽히지 않기 때문.
                        */
                        input.unread(ch2);
                        error();
                    }
                }
                else if (ch == '>'){
                    ch2 = input.read();
                    if (ch2 == '=')
                        return Relop.GTEQ;
                    else {
                        input.unread(ch2);
                        return Relop.GT;
                    }
                }
                else if (ch == '<'){
                    ch2 = input.read();
                    if (ch2 == '=')
                        return Relop.LTEQ;
                    else {
                        input.unread(ch2);
                        return Relop.LT;
                    }
                }
                else if (ch == '!'){
                    ch2 = input.read();
                    if (ch2 == '=')
                        return Relop.NOTEQ;
                    else
                        /*
                        여기서 unread를 추가해주어햐 함.
                        이유(예제 1을 예시로 들음) : ch = !(op) , ch2 = 3(정수)인 상태에서 token은 NOT을 반환 그렇다면 후에 getToken()을 통해 다시 ch를 읽어들일 때 어떤 값이 읽힐까?
                        버퍼에는 3이 존재하지 않을거임. 이미 stream 상태에서 읽힌 상태니까. 그러면 결국 3을 읽지 못하고 >를 읽게 됨. 따라서 3이 무시되는 상황이 발생함.
                        */
                        input.unread(ch2);
                    return Relop.NOT;
                }
                else if (ch == '&')
                    return Relop.AND;
                else if (ch == '|')
                    return Relop.OR;
                else if (ch == '+')
                    return Relop.PLUS;
                else if (ch == '-')
                    return Relop.MINUS;
                else if (ch == '*')
                    return Relop.MULTI;
                else if (ch == '/')
                    return Relop.DIVIDE;
                else if (ch == '(')
                    return Relop.LPAREN;
                else if (ch == ')')
                    return Relop.RPAREN;
                else if (ch == '\n')
                    return Relop.NLINE;
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    void match(Relop c) { // 토큰이 일치하면 다음 토큰을 읽어들이고, 일치하지 않으면 에러를 출력하고 종료한다.
        if (token == c)
            token = getToken();
        else error();
    }

    int number( )  {
        /* number -> digit { digit } */
        int result = ch - '0';
        try  {
            ch = input.read();
            while (Character.isDigit(ch)) {
                result = 10 * result + ch -'0';
                ch = input.read();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        return result;
    }

    void error( ) {
        System.out.printf("parse error : %d\n", ch);
        System.exit(1);
    }

    int expr( ) {
        /* <expr> → <bexp> {& <bexp> | ‘|’ <bexp>} | !<expr> | true | false */
        if (token == Relop.NOT) {
            match(Relop.NOT);
            /*token = null;*/
            return Math.abs(expr() - 1);
        }
        int result = bexp();
        if (token == Relop.NUMBER) { // 1번 예제에서 해당 token = 3이 맞음
            match(Relop.NUMBER); // 다음 토큰을 읽음
            while (token == Relop.AND || token == Relop.OR) {
                if (token == Relop.AND) {
                    match(Relop.AND);
                    result = result & bexp();
                } else if (token == Relop.OR) {
                    match(Relop.OR);
                    result = result | bexp();
                }
            }
        }
        return result;
    }

    int bexp(){ //<bexp> → <aexp> [(== | != | < | > | <= | >=) <aexp>]
        int result = aexp();
        if (token == Relop.EQUAL) {
            match(Relop.EQUAL);
            result = (result == aexp()) ? TRUE : FALSE;
        } else if (token == Relop.NOTEQ) {
            match(Relop.NOTEQ);
            result = (result != aexp()) ? TRUE : FALSE;
        } else if (token == Relop.LT) {
            match(Relop.LT);
            result = (result < aexp()) ? TRUE : FALSE;
        } else if (token == Relop.LTEQ) {
            match(Relop.LTEQ);
            result = (result <= aexp()) ? TRUE : FALSE;
        } else if (token == Relop.GT) {
            match(Relop.GT);
            result = (result > aexp()) ? TRUE : FALSE;
        } else if (token == Relop.GTEQ) {
            match(Relop.GTEQ);
            result = (result >= aexp()) ? TRUE : FALSE;
        }
        return result;
    }

    int aexp() {
        /* <aexp> → <term> {+ <term> | - <term>} */
        int result = term();
        while (token == Relop.PLUS || token == Relop.MINUS) {
            if (token == Relop.PLUS) {
                match(Relop.PLUS);
                result = result + term();
            } else if (token == Relop.MINUS) {
                match(Relop.MINUS);
                result = result - term();
            }
        }
        return result;
    }

    int term() {
        /* <term> → <factor> {* <factor> | / <factor>} */
        int result = factor();
        while (token == Relop.MULTI || token == Relop.DIVIDE) {
            if (token == Relop.MULTI) {
                match(Relop.MULTI);
                result = result * factor();
            } else if (token == Relop.DIVIDE) {
                match(Relop.DIVIDE);
                result = result / factor();
            }
        }
        return result;
    }

    int factor() {
        /* <factor> → <number> | (<expr>) */
        int result = 0;
        if (token == Relop.NUMBER) {
            result = value;
            match(Relop.NUMBER);
        } else if (token == Relop.LPAREN) {
            match(Relop.LPAREN);
            result = expr();
            match(Relop.RPAREN);
        }
        return result;
    }
    void command( ) {
        /* command -> expr '\n' */
        int result = expr();
        if (token == Relop.NLINE)
            if (result == TRUE)
                System.out.println(true);
            else if(result == FALSE)
                System.out.println(false);
            else
                System.out.printf("The result is: %d\n", result);
        else error();
    }

    void parse( ) {
        token = getToken();
        command();
    }

    public static void main(String args[]) {
        Calculator calc = new Calculator(new PushbackInputStream(System.in));
        while(true) {
            System.out.print(">> ");
            calc.parse();
        }
    }
}