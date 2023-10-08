import java.io.IOException;
import java.io.PushbackInputStream;
enum Relop {
    LPAREN, RPAREN, PLUS, MINUS, MULTI, DIVIDE,
    AND, OR, NUMBER, NOT,
    EQUAL, LT, LTEQ, GT, GTEQ, NOTEQ, NLINE
};
/*
    Meaning of enum symbols
	LPAREN("("), RPAREN(")"), PLUS("+"), MINUS("-"), MULTI("*"),DIVIDE("/"),
	AND("&"), OR("|"), NUMBER(""), NOT("!"), -> 논리 연산자
	EQUAL("=="), LT("<"), LTEQ("<="), GT(">"), GTEQ(">="), NOTEQ("!="), NLINE("\n"); -> 비교 연산자
*/
public class Calculator {
    Relop token; int value; int ch; int ch2; //토큰 관련 함수는 함수를 타고 들어가면서 모든 함수가 공유하기 때문에 반드시 전역변수여야 함.
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
                    value = number( ); // 변수 value에 숫자로 변환되어 저장
                    input.unread(ch); //하나 읽기 전으로 되돌림.
                    return Relop.NUMBER; //열거형 NUMBER 반
                }
                else if (ch == '='){
                    ch2 = input.read();
                    if (ch2 == '=')
                        return Relop.EQUAL;
                    else error("EQUAL error");
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
                    else {
                        input.unread(ch2);  // ! 뒤에 =가 아닌 다른 문자가 나온 경우
                        return Relop.NOT;
                    }
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
                else
                    error("getToken error");
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    void match(Relop c) {
        if (token == c)
            token = getToken();
        else
            error("Match error");
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
    int expr() { // TURE OR FALSE를 연산하여 TRUE OR FALSE 반환
        if (token == Relop.NOT) {
            match(Relop.NOT);
            return Math.abs(expr()-1);
        }
        int result = bexp();
        System.out.printf("[expr] result1: %d\n", result);
        while (true) {
            if (token == Relop.AND){ // And 연산자가 나오면
                match(Relop.AND);
                if(result == TRUE && bexp() == TRUE) result = TRUE;
                else result = FALSE;
            } else if (token == Relop.OR){ // OR 연산자가 나오면
                match(Relop.OR);
                if (result == TRUE || bexp() == TRUE) result = TRUE;
                else result = FALSE;
            } else { // AND, OR 연산자가 아닌 경우
                break;
            }
        }
        System.out.printf("[expr] result2: %d,\n", result);
        return result;
    }
    int bexp() { // 연산의 TRUE or FALSE를 반환
        int result = aexp();
        System.out.printf("[bexp] result1: %d\n", result);
        // [ ] 의 표현은 0번 or 1번의 개념이므로 조건문을 사용함.
        if (token == Relop.EQUAL) {
            match(Relop.EQUAL);
            result = (result == aexp()) ? TRUE : FALSE;// 소괄호 안의 비교 연산이 참인경우 true, 거짓인 경우 false 반환.
        } else if (token == Relop.NOTEQ) {
            match(Relop.NOTEQ);
            result = (result != aexp()) ? TRUE : FALSE;
        } else if (token == Relop.LT) {
            match(Relop.LT);
            result = (result < aexp()) ? TRUE : FALSE;
        } else if (token == Relop.GT) {
            match(Relop.GT);
            result = (result > aexp()) ? TRUE : FALSE;
        } else if (token == Relop.LTEQ) {
            match(Relop.LTEQ);
            result = (result <= aexp()) ? TRUE : FALSE;
        } else if (token == Relop.GTEQ) {
            match(Relop.GTEQ);
            result = (result >= aexp()) ? TRUE : FALSE;
        }
        System.out.printf("[bexp] result2: %d\n", result);
        return result;
    }

    // 수 연산 +,-
    int aexp() {
        int result = term();
        System.out.printf("[aexp] result1: %d\n", result);
        while (true) {
            if (token == Relop.PLUS) {
                match(Relop.PLUS);
                result += term();
            } else if(token == Relop.MINUS){
                match(Relop.MINUS);
                result -= term();
            } else {
                break;
            }
        }
        System.out.printf("[aexp] result2: %d\n", result);
        return result;
    }

    int term() {
        int result = factor();
        System.out.printf("[term] result1: %d\n", result);
        while (true) {
            if(token == Relop.MULTI) {
                match(Relop.MULTI);
                result *= factor();
            } else if(token == Relop.DIVIDE){
                match(Relop.DIVIDE);
                result /= factor();
            } else {
                break;
            }
        }
        System.out.printf("[term] result2: %d\n", result);
        return result;
    }

    int factor() {
        if (token == Relop.LPAREN) {
            match(Relop.LPAREN);
            int result = aexp();
            System.out.printf("[factor] result: %d\n", result);
            match(Relop.RPAREN);
            return result;
        } else {
            match(Relop.NUMBER);
            return value;
        }
    }
    void error(String msg) {
        System.out.println(msg);
        System.out.printf("parse error : %d, %c\n", ch, ch);
        System.exit(1);
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
        else error("Command error");
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