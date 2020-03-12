package bit.minisys.minicc.scanner;

import java.util.ArrayList;
import java.util.HashSet;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;


enum DFA_STATE {
    DFA_STATE_INITIAL,
    DFA_STATE_ID_0,
    DFA_STATE_ADD_0,
    DFA_STATE_DI_CONSTANT_0,
    DFA_STATE_DI_CONSTANT_1,
    DFA_STATE_DI_CONSTANT_2,
    DFA_STATE_DI_CONSTANT_3,
    DFA_STATE_DI_CONSTANT_4,
    DFA_STATE_DI_CONSTANT_5,
    DFA_STATE_DI_CONSTANT_6,
    DFA_STATE_DI_CONSTANT_7,
    DFA_STATE_DI_CONSTANT_8,
    DFA_STATE_DI_CONSTANT_9,
    DFA_STATE_DI_CONSTANT_10,
    DFA_STATE_DI_CONSTANT_11,
    DFA_STATE_DI_CHAR_1,
    DFA_STATE_DI_CHAR_2,
    DFA_STATE_DI_LITERAL_1,
    DFA_STATE_DI_LITERAL_2,
    DFA_STATE_FL_CONSTANT_0,
    DFA_STATE_FL_CONSTANT_1,
    DFA_STATE_FL_CONSTANT_2,
    DFA_STATE_FL_CONSTANT_3,
    DFA_STATE_FL_CONSTANT_4,
    DFA_STATE_FL_CONSTANT_5,
    DFA_STATE_ES_1,
    DFA_STATE_ES_2,
    DFA_STATE_OP_1,
    DFA_STATE_OP_2,
    DFA_STATE_OP_3,
    DFA_STATE_UNKNW
}

public class ExampleScanner implements IMiniCCScanner {

    private int lIndex = 0;
    private int cIndex = 0;

    private ArrayList<String> srcLines;

    private HashSet<String> keywordSet;
    private HashSet<String> literalKeywordSet;
    private HashSet<Character> escapeSequence;
    private HashSet<String> prefixChar;
    private HashSet<Character> oper1;
    private HashSet<Character> preOper2;
    private HashSet<String> oper2;
    private HashSet<String> preOper3;
    private HashSet<String> oper3;

    public ExampleScanner() {
        this.keywordSet = new HashSet<String>();
        this.keywordSet.add("int");
        this.keywordSet.add("return");
        this.keywordSet.add("double");
        this.keywordSet.add("auto");
        this.keywordSet.add("break");
        this.keywordSet.add("case");
        this.keywordSet.add("char");
        this.keywordSet.add("const");
        this.keywordSet.add("continue");
        this.keywordSet.add("default");
        this.keywordSet.add("do");
        this.keywordSet.add("else");
        this.keywordSet.add("enum");
        this.keywordSet.add("extern");
        this.keywordSet.add("float");
        this.keywordSet.add("for");
        this.keywordSet.add("goto");
        this.keywordSet.add("if");
        this.keywordSet.add("inline");
        this.keywordSet.add("long");
        this.keywordSet.add("register");
        this.keywordSet.add("restrict");
        this.keywordSet.add("short");
        this.keywordSet.add("signed");
        this.keywordSet.add("sizeof");
        this.keywordSet.add("unsigned");
        this.keywordSet.add("static");
        this.keywordSet.add("struct");
        this.keywordSet.add("switch");
        this.keywordSet.add("typedef");
        this.keywordSet.add("union");
        this.keywordSet.add("void");
        this.keywordSet.add("volatile");
        this.keywordSet.add("while");

        this.literalKeywordSet = new HashSet<String>();
        this.literalKeywordSet.add("u8");
        this.literalKeywordSet.add("u");
        this.literalKeywordSet.add("U");
        this.literalKeywordSet.add("L");

        this.escapeSequence = new HashSet<Character>();
        this.escapeSequence.add('a');
        this.escapeSequence.add('b');
        this.escapeSequence.add('f');
        this.escapeSequence.add('n');
        this.escapeSequence.add('t');
        this.escapeSequence.add('r');
        this.escapeSequence.add('v');
        this.escapeSequence.add('?');
        this.escapeSequence.add('\\');
        this.escapeSequence.add('\"');
        this.escapeSequence.add('\'');

        this.prefixChar = new HashSet<String>();
        this.prefixChar.add("U");
        this.prefixChar.add("u");
        this.prefixChar.add("L");

        this.oper1 = new HashSet<Character>();
        this.oper1.add('[');
        this.oper1.add(']');
        this.oper1.add('(');
        this.oper1.add(')');
        this.oper1.add('?');
        this.oper1.add(',');
        this.oper1.add(';');
        this.oper1.add(':');
        this.oper1.add('~');
        this.oper1.add('{');
        this.oper1.add('.');
        this.oper1.add('}');

        this.preOper2 = new HashSet<Character>();
        this.preOper2.add('+');
        this.preOper2.add('-');
        this.preOper2.add('*');
        this.preOper2.add('/');
        this.preOper2.add('%');
        this.preOper2.add('<');
        this.preOper2.add('>');
        this.preOper2.add('^');
        this.preOper2.add('!');
        this.preOper2.add('|');
        this.preOper2.add('&');
        this.preOper2.add('=');

        this.oper2 = new HashSet<String>();
        this.oper2.add("++");
        this.oper2.add("--");
        this.oper2.add("<<");
        this.oper2.add("<=");
        this.oper2.add(">>");
        this.oper2.add(">=");
        this.oper2.add("==");
        this.oper2.add("!=");
        this.oper2.add("&&");
        this.oper2.add("||");
        this.oper2.add("*=");
        this.oper2.add("/=");
        this.oper2.add("%=");
        this.oper2.add("+=");
        this.oper2.add("-=");
        this.oper2.add("^=");
        this.oper2.add("|=");
        this.oper2.add("->");
        this.oper2.add("&=");

        this.preOper3 = new HashSet<String>();
        this.preOper3.add("<<");
        this.preOper3.add(">>");

        this.oper3 = new HashSet<String>();
        this.oper3.add("<<=");
        this.oper3.add(">>=");

    }

    private char getNextChar() {
        char c = Character.MAX_VALUE;
        while (true) {
            if (lIndex < this.srcLines.size()) {
                String line = this.srcLines.get(lIndex) + '\n';
                if (cIndex < line.length()) {
                    c = line.charAt(cIndex);
                    cIndex++;
                    break;
                } else {
                    lIndex++;
                    cIndex = 0;
                }
            } else {
                break;
            }
        }
        if (c == '\u001a') {
            c = Character.MAX_VALUE;
        }
        return c;
    }

    private boolean isAlpha(char c) {
        return Character.isAlphabetic(c);
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private boolean isAlphaOrDigit(char c) {
        return Character.isLetterOrDigit(c);
    }

    private boolean isDownCase(char c) {
        return Character.toString(c).equals("_");
    }

    private boolean isAlphaOrDownCase(char c) {
        return isAlpha(c) | isDownCase(c);
    }

    private boolean isAplhaOrDigitOrDownCase(char c) {
        return isAlphaOrDigit(c) | isDownCase(c);
    }

    private boolean isHexo(char c) {
        return isDigit(c) | (c >= 'a' && c <= 'f') | (c >= 'A' && c <= 'F');
    }

    private boolean isOcto(char c) {
        return c >= '0' && c <= '8';
    }

    private boolean isU(char c) {
        return Character.toString(c).equals("U") | Character.toString(c).equals("u");
    }

    private boolean isl(char c) {
        return Character.toString(c).equals("l");
    }

    private boolean isL(char c) {
        return Character.toString(c).equals("L");
    }

    private boolean isX(char c) {
        return Character.toString(c).equals("X") | Character.toString(c).equals("x");
    }

    private boolean is1to9(char c) {
        return Integer.parseInt(Character.toString(c)) > 0;
    }

    private boolean isF(char c) {
        return Character.toString(c).equals("F") | Character.toString(c).equals("f");
    }

    private boolean isFOrL(char c) {
        return isL(c) | isl(c) | isF(c);
    }

    private boolean isPoint(char c) {
        return Character.toString(c).equals(".");
    }

    private boolean isE(char c) {
        return Character.toString(c).equals("E") | Character.toString(c).equals("e");
    }

    private boolean isP(char c) {
        return Character.toString(c).equals("P") | Character.toString(c).equals("p");
    }

    private boolean isSign(char c) {
        return Character.toString(c).equals("+") | Character.toString(c).equals("-");
    }

    private String genToken(int num, String lexme, String type) {
        return genToken(num, lexme, type, this.cIndex - 1, this.lIndex);
    }

    private String genToken2(int num, String lexme, String type) {
        return genToken(num, lexme, type, this.cIndex - 2, this.lIndex);
    }

    private String genToken(int num, String lexme, String type, int cIndex, int lIndex) {
        String strToken = "";

        strToken += "[@" + num + "," + (cIndex - lexme.length() + 1) + ":" + cIndex;
        strToken += "='" + lexme + "',<" + type + ">," + (lIndex + 1) + ":" + (cIndex - lexme.length() + 1) + "]\n";

        return strToken;
    }

    @Override
    public String run(String iFile) throws Exception {

        System.out.println("Scanning...");
        String strTokens = "";
        int iTknNum = 0;
        this.srcLines = MiniCCUtil.readFile(iFile);

        DFA_STATE state = DFA_STATE.DFA_STATE_INITIAL;        //FA state
        String lexme = "";        //token lexme
        char c = ' ';        //next char
        boolean keep = false;    //keep current char
        boolean end = false;
        int hexoCont = 0;
        int octoCont = 0;
        int stringLite = 0;
        int charLite = 0;
        while (!end) {                //scanning loop
            if (!keep) {
                c = getNextChar();
            }

            keep = false;

            switch (state) {
                case DFA_STATE_INITIAL:
                    lexme = "";

                    if (isAlphaOrDownCase(c)) {
                        state = DFA_STATE.DFA_STATE_ID_0;
                        lexme = lexme + c;
                    } else if (isDigit(c)) {
                        if (is1to9(c)) {
                            state = DFA_STATE.DFA_STATE_DI_CONSTANT_1;
                            lexme = lexme + c;
                        } else {
                            state = DFA_STATE.DFA_STATE_DI_CONSTANT_0;
                            lexme = lexme + c;
                        }
                    } else if (Character.isSpace(c)) {

                    } else if (c == '"') {
                        stringLite = 1;
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                    } else if (c == '\'') {
                        charLite = 1;
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                    } else if (oper1.contains(c)) {
                        strTokens += genToken(iTknNum, Character.toString(c), "'" + c + "'");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else if (preOper2.contains(c)) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_OP_1;
                    } else if (c == Character.MAX_VALUE) {
                        cIndex = 5;
                        strTokens += genToken(iTknNum, "<EOF>", "EOF");
                        end = true;
                    }
                    break;
                case DFA_STATE_OP_1:
                    if (preOper3.contains(lexme + c)) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_OP_2;
                    } else if (oper2.contains(lexme + c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "'" + lexme + "'");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "'" + lexme + "'");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_OP_2:
                    if (oper3.contains(lexme + c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "'" + lexme + "'");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "'" + lexme + "'");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_0:
                    if (isX(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_2;
                        lexme = lexme + c;
                    } else if (isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_3;
                        lexme = lexme + c;
                    } else if (isPoint(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_2;
                        lexme = lexme + c;
                    } else if (isE(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_3;
                        lexme = lexme + c;
                    } else if (isl(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_5;
                        lexme = lexme + c;
                    } else if (isL(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_6;
                        lexme = lexme + c;
                    } else if (isU(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_7;
                        lexme = lexme + c;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_1:
                case DFA_STATE_DI_CONSTANT_4:
                    if (isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_4;
                        lexme = lexme + c;
                    } else if (isl(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_5;
                        lexme = lexme + c;
                    } else if (isL(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_6;
                        lexme = lexme + c;
                    } else if (isU(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_7;
                        lexme = lexme + c;
                    } else if (isPoint(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_2;
                        lexme = lexme + c;
                    } else if (isE(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_3;
                        lexme = lexme + c;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_2:
                    if (isHexo(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_2;
                        lexme = lexme + c;
                    } else if (isl(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_5;
                        lexme = lexme + c;
                    } else if (isL(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_6;
                        lexme = lexme + c;
                    } else if (isU(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_7;
                        lexme = lexme + c;
                    } else if (isP(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_3;
                        lexme = lexme + c;
                    } else if (isPoint(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_1;
                        lexme = lexme + c;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_3:
                    if (isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_3;
                        lexme = lexme + c;
                    } else if (isl(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_5;
                        lexme = lexme + c;
                    } else if (isL(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_6;
                        lexme = lexme + c;
                    } else if (isU(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_7;
                        lexme = lexme + c;
                    } else if (isPoint(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_2;
                        lexme = lexme + c;
                    } else if (isE(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_3;
                        lexme = lexme + c;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_5:
                    if (isl(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_10;
                        lexme = lexme + c;
                    } else if (isU(c)) {
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        lexme = lexme + c;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_6:
                    if (isL(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_11;
                        lexme = lexme + c;
                    } else if (isU(c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_7:
                    if (isl(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_8;
                        lexme = lexme + c;
                    } else if (isL(c)) {
                        state = DFA_STATE.DFA_STATE_DI_CONSTANT_9;
                        lexme = lexme + c;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_8:
                    if (isl(c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_9:
                    if (isL(c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_DI_CONSTANT_10:
                case DFA_STATE_DI_CONSTANT_11:
                    if (isU(c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_FL_CONSTANT_1:
                    if (isP(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_3;
                        lexme = lexme + c;
                    } else if (isHexo(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_1;
                        lexme = lexme + c;
                    } else {
                        state = DFA_STATE.DFA_STATE_UNKNW;
                        lexme = lexme + c;
                    }
                    break;
                case DFA_STATE_FL_CONSTANT_2:
                    if (isE(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_3;
                        lexme = lexme + c;
                    } else if (isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_2;
                        lexme = lexme + c;
                    } else if (isFOrL(c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_FL_CONSTANT_3:
                    if (isSign(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_4;
                        lexme = lexme + c;
                    } else if (isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_5;
                        lexme = lexme + c;
                    } else {
                        state = DFA_STATE.DFA_STATE_UNKNW;
                        lexme = lexme + c;
                    }
                    break;
                case DFA_STATE_FL_CONSTANT_4:
                    if (isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_5;
                        lexme = lexme + c;
                    } else {
                        state = DFA_STATE.DFA_STATE_UNKNW;
                        lexme = lexme + c;
                    }
                    break;
                case DFA_STATE_FL_CONSTANT_5:
                    if (isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_FL_CONSTANT_5;
                        lexme = lexme + c;
                    } else if (isFOrL(c)) {
                        lexme = lexme + c;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    } else {
                        strTokens += genToken2(iTknNum, lexme, "Constant");
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;
                case DFA_STATE_ID_0:
                    if (isAplhaOrDigitOrDownCase(c)) {
                        lexme = lexme + c;
                    } else {
                        if (this.keywordSet.contains(lexme)) {
                            strTokens += genToken2(iTknNum, lexme, "'" + lexme + "'");
                            iTknNum++;
                            state = DFA_STATE.DFA_STATE_INITIAL;
                            keep = true;
                        } else if (Character.toString(c).equals("\"")) {
                            if (this.literalKeywordSet.contains(lexme)) {
                                stringLite = 1;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                            } else {
                                strTokens += genToken2(iTknNum, lexme, "Identifier");
                                iTknNum++;
                                state = DFA_STATE.DFA_STATE_INITIAL;
                                keep = true;
                            }
                        } else if (c == '\'') {
                            if (this.prefixChar.contains(lexme)) {
                                charLite = 1;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                            } else {
                                strTokens += genToken2(iTknNum, lexme, "Identifier");
                                iTknNum++;
                                state = DFA_STATE.DFA_STATE_INITIAL;
                                keep = true;
                            }
                        } else {
                            strTokens += genToken2(iTknNum, lexme, "Identifier");
                            iTknNum++;
                            state = DFA_STATE.DFA_STATE_INITIAL;
                            keep = true;
                        }
                    }
                    break;
                case DFA_STATE_DI_LITERAL_1:
                    if ((c >= 32 && c <= 33) || (c >= 40 && c <= 91) || (c >= 93 && c <= 126) || (c >= 11 && c <= 12)) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                    } else if (c == 92) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_LITERAL_2;
                    } else if (c == 34) {
                        lexme = lexme + c;
                        stringLite = 0;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        strTokens += genToken(iTknNum, lexme, "StringLiteral");
                        iTknNum++;
                    } else {
                        stringLite = 0;
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_UNKNW;
                    }
                    break;
                case DFA_STATE_DI_LITERAL_2:
                    if (this.escapeSequence.contains(c)) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                    } else if (c == 'x') {
                        lexme = lexme + c;
                        hexoCont = 0;
                        state = DFA_STATE.DFA_STATE_ES_2;
                    } else if (isOcto(c)) {
                        lexme = lexme + c;
                        octoCont = 1;
                        state = DFA_STATE.DFA_STATE_ES_1;
                    } else {
                        stringLite = 0;
                        state = DFA_STATE.DFA_STATE_UNKNW;
                        lexme = lexme + c;
                    }
                    break;
                case DFA_STATE_DI_CHAR_1:
                    if ((c >= 32 && c <= 38) || (c >= 40 && c <= 91) || (c >= 93 && c <= 126) || (c >= 11 && c <= 12)) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                    } else if (c == 92) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_CHAR_2;
                    } else if (c == 39) {
                        lexme = lexme + c;
                        charLite = 0;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                    } else {
                        charLite = 0;
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_UNKNW;
                    }
                    break;
                case DFA_STATE_DI_CHAR_2:
                    if (this.escapeSequence.contains(c)) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                    } else if (c == 'x') {
                        lexme = lexme + c;
                        hexoCont = 0;
                        state = DFA_STATE.DFA_STATE_ES_2;
                    } else if (isOcto(c)) {
                        lexme = lexme + c;
                        octoCont = 1;
                        state = DFA_STATE.DFA_STATE_ES_1;
                    } else {
                        charLite = 0;
                        state = DFA_STATE.DFA_STATE_UNKNW;
                        lexme = lexme + c;
                    }
                    break;
                case DFA_STATE_ES_1:
                    if (isOcto(c)) {
                        lexme = lexme + c;
                        octoCont++;
                        state = DFA_STATE.DFA_STATE_ES_1;
                        if (octoCont == 3 && stringLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                        } else if (octoCont == 3 && charLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                        }
                    } else if (c == '\\') {
                        lexme = lexme + c;
                        if (stringLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_LITERAL_2;
                        } else if (charLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_CHAR_2;
                        }
                    } else if (c == 34 && stringLite == 1) {
                        stringLite = 0;
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        strTokens += genToken(iTknNum, lexme, "StringLiteral");
                        iTknNum++;
                    } else if (((c >= 32 && c <= 33) || (c >= 35 && c <= 91) || (c >= 93 && c <= 126) || (c >= 11 && c <= 12)) && stringLite == 1) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                    } else if (c == 39 && charLite == 1) {
                        charLite = 0;
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        strTokens += genToken(iTknNum, lexme, "Constant");
                        iTknNum++;
                    } else if (((c >= 32 && c <= 38) || (c >= 40 && c <= 91) || (c >= 93 && c <= 126) || (c >= 11 && c <= 12)) && charLite == 1) {
                        lexme = lexme + c;
                        state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                    } else {
                        stringLite = 0;
                        charLite = 0;
                        state = DFA_STATE.DFA_STATE_UNKNW;
                        lexme = lexme + c;
                    }
                    break;
                case DFA_STATE_ES_2:
                    if (isHexo(c)) {
                        lexme = lexme + c;
                        hexoCont++;
                        state = DFA_STATE.DFA_STATE_ES_2;
                        if (hexoCont == 2 && stringLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                        } else if (hexoCont == 2 && charLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                        }
                    } else if (c == '\\') {
                        if (hexoCont == 0) {
                            stringLite = 0;
                            charLite = 0;
                            lexme = lexme + c;
                            state = DFA_STATE.DFA_STATE_UNKNW;
                        } else if (stringLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_LITERAL_2;
                        } else if (charLite == 1) {
                            state = DFA_STATE.DFA_STATE_DI_CHAR_2;
                        }
                    } else if (stringLite == 1) {
                        if (c == 34) {
                            if (hexoCont == 0) {
                                stringLite = 0;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_UNKNW;
                            } else {
                                stringLite = 0;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_INITIAL;
                                strTokens += genToken(iTknNum, lexme, "StringLiteral");
                                iTknNum++;
                            }
                        } else if ((c >= 32 && c <= 33) || (c >= 35 && c <= 91) || (c >= 93 && c <= 126) || c == 11 || c == 12) {
                            if (hexoCont == 0) {
                                stringLite = 0;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_UNKNW;
                            } else {
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_DI_LITERAL_1;
                            }
                        } else {
                            stringLite = 0;
                            state = DFA_STATE.DFA_STATE_UNKNW;
                            lexme = lexme + c;
                        }
                    } else if (charLite == 1) {
                        if (c == 39) {
                            if (hexoCont == 0) {
                                charLite = 0;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_UNKNW;
                            } else {
                                charLite = 0;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_INITIAL;
                                strTokens += genToken(iTknNum, lexme, "Constant");
                                iTknNum++;
                            }
                        } else if ((c >= 32 && c <= 38) || (c >= 40 && c <= 91) || (c >= 93 && c <= 126) || (c >= 11 && c <= 12)) {
                            if (hexoCont == 0) {
                                charLite = 0;
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_UNKNW;
                            } else {
                                lexme = lexme + c;
                                state = DFA_STATE.DFA_STATE_DI_CHAR_1;
                            }
                        } else {
                            charLite = 0;
                            state = DFA_STATE.DFA_STATE_UNKNW;
                            lexme = lexme + c;
                        }
                    }
                    break;
                default:
                    System.out.println("[ERROR]Scanner:line " + (lIndex + 1) + ", column=" + cIndex + ", unreachable state!");
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
            }

        }
        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
        MiniCCUtil.createAndWriteFile(oFile, strTokens);

        return oFile;
    }

}
