/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
// Checkstyle: stop
//@formatter:off
package com.oracle.truffle.r.parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import com.oracle.truffle.r.runtime.FileSystemUtils;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.Argument;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.RCodeToken;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings("all")
public class RLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMENT=1, ARROW=2, SUPER_ARROW=3, RIGHT_ARROW=4, SUPER_RIGHT_ARROW=5, 
		VARIADIC=6, EQ=7, NE=8, GE=9, LE=10, GT=11, LT=12, ASSIGN=13, NS_GET_INT=14, 
		NS_GET=15, COLON=16, SEMICOLON=17, COMMA=18, AND=19, ELEMENTWISEAND=20, 
		OR=21, ELEMENTWISEOR=22, LBRACE=23, RBRACE=24, LPAR=25, RPAR=26, LBB=27, 
		LBRAKET=28, RBRAKET=29, CARET=30, TILDE=31, NOT=32, QM=33, PLUS=34, MULT=35, 
		DIV=36, MINUS=37, FIELD=38, AT=39, FUNCTION=40, NULL=41, NA=42, NAINT=43, 
		NAREAL=44, NACHAR=45, NACOMPL=46, TRUE=47, FALSE=48, INF=49, NAN=50, WHILE=51, 
		FOR=52, REPEAT=53, IN=54, IF=55, ELSE=56, NEXT=57, BREAK=58, WS=59, NEWLINE=60, 
		INTEGER=61, COMPLEX=62, DOUBLE=63, DD=64, ID=65, OP=66, STRING=67;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"COMMENT", "ARROW", "SUPER_ARROW", "RIGHT_ARROW", "SUPER_RIGHT_ARROW", 
			"VARIADIC", "EQ", "NE", "GE", "LE", "GT", "LT", "ASSIGN", "NS_GET_INT", 
			"NS_GET", "COLON", "SEMICOLON", "COMMA", "AND", "ELEMENTWISEAND", "OR", 
			"ELEMENTWISEOR", "LBRACE", "RBRACE", "LPAR", "RPAR", "LBB", "LBRAKET", 
			"RBRAKET", "CARET", "TILDE", "NOT", "QM", "PLUS", "MULT", "DIV", "MINUS", 
			"FIELD", "AT", "FUNCTION", "NULL", "NA", "NAINT", "NAREAL", "NACHAR", 
			"NACOMPL", "TRUE", "FALSE", "INF", "NAN", "WHILE", "FOR", "REPEAT", "IN", 
			"IF", "ELSE", "NEXT", "BREAK", "WS", "NEWLINE", "INTEGER", "COMPLEX", 
			"DOUBLE", "DD", "ID", "OP", "BACKTICK_NAME", "STRING", "ESCAPE", "LINE_BREAK", 
			"EXPONENT", "HEX_EXPONENT", "OP_NAME", "ID_NAME", "ESC_SEQ", "UNICODE_ESC", 
			"HEX_ESC", "HEX_DIGIT", "OCT_DIGIT", "OCTAL_ESC"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'<<-'", "'->'", "'->>'", "'...'", "'=='", "'!='", 
			"'>='", "'<='", "'>'", "'<'", "'='", "':::'", "'::'", "':'", "';'", "','", 
			"'&&'", "'&'", "'||'", "'|'", "'{'", "'}'", "'('", "')'", "'[['", "'['", 
			"']'", null, "'~'", "'!'", "'?'", "'+'", "'*'", "'/'", "'-'", "'$'", 
			"'@'", "'function'", "'NULL'", "'NA'", "'NA_integer_'", "'NA_real_'", 
			"'NA_character_'", "'NA_complex_'", "'TRUE'", "'FALSE'", "'Inf'", "'NaN'", 
			"'while'", "'for'", "'repeat'", "'in'", "'if'", "'else'", "'next'", "'break'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMENT", "ARROW", "SUPER_ARROW", "RIGHT_ARROW", "SUPER_RIGHT_ARROW", 
			"VARIADIC", "EQ", "NE", "GE", "LE", "GT", "LT", "ASSIGN", "NS_GET_INT", 
			"NS_GET", "COLON", "SEMICOLON", "COMMA", "AND", "ELEMENTWISEAND", "OR", 
			"ELEMENTWISEOR", "LBRACE", "RBRACE", "LPAR", "RPAR", "LBB", "LBRAKET", 
			"RBRAKET", "CARET", "TILDE", "NOT", "QM", "PLUS", "MULT", "DIV", "MINUS", 
			"FIELD", "AT", "FUNCTION", "NULL", "NA", "NAINT", "NAREAL", "NACHAR", 
			"NACOMPL", "TRUE", "FALSE", "INF", "NAN", "WHILE", "FOR", "REPEAT", "IN", 
			"IF", "ELSE", "NEXT", "BREAK", "WS", "NEWLINE", "INTEGER", "COMPLEX", 
			"DOUBLE", "DD", "ID", "OP", "STRING"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	    /*
	     * The nesting level is maintained for "{", "(", "[" and "[[", so that
	     * LINE_BREAK can be ignored while the nesting is larger than zero.
	     */
	    private int incompleteNesting;
	    private final ArrayList<Integer> nestingStack = new ArrayList<>();

	    private static String hexChar(String... chars) {
	        int value = 0;
	        for (int i = 0; i < chars.length; i++) {
	            if (chars[i] == null) {
	            	// not all digits must be present, eg. "0048" vs. "48"
	            	break;
	            }
	            value = value * 16 + Integer.parseInt(chars[i], 16);
	        }
	        return new String(new int[]{value}, 0, 1);
	    }

	    private static String octChar(String... chars) {
	        int value = 0;
	        for (int i = 0; i < chars.length; i++) {
	            value = value * 8 + Integer.parseInt(chars[i], 8);
	        }
	        value &= 0xff; // octal escape sequences are clamped the 0-255 range
	        return new String(new int[]{value}, 0, 1);
	    }
	        public static String parseString(String value) {
	            if (!value.contains("\\")) {
	                if ("``".equals(value)) {
	                    throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
	                }
	                return value.substring(1, value.length() - 1);
	            } else {
	                StringBuilder str = new StringBuilder(value.length());

	                int i = 1;
	                while (i < value.length() - 1) {
	                    if (value.charAt(i) != '\\') {
	                        str.append(value.charAt(i));
	                        i++;
	                    } else {
	                        i += 2;
	                        switch (value.charAt(i - 1)) {
	                            case 't':
	                                str.append('\t');
	                                break;
	                            case 'n':
	                                str.append('\n');
	                                break;
	                            case 'a':
	                                str.appendCodePoint(7);
	                                break;
	                            case 'v':
	                                str.appendCodePoint(11);
	                                break;
	                            case 'r':
	                                str.append('\r');
	                                break;
	                            case 'b':
	                                str.append('\b');
	                                break;
	                            case 'f':
	                                str.append('\f');
	                                break;
	                            case '"':
	                                str.append('"');
	                                break;
	                            case '`':
	                                str.append('`');
	                                break;
	                            case '\'':
	                                str.append('\'');
	                                break;
	                            case ' ':
	                                str.append(' ');
	                                break;
	                            case '\\':
	                                str.append('\\');
	                                break;
	                            case '\n':
	                                str.append('\n');
	                                break;
	                            case 'x': {
	                                // up to 3 digits
	                                str.appendCodePoint(hexChar(value.charAt(i)) * 16 + hexChar(value.charAt(i + 1)));
	                                i += 2;
	                                break;
	                            }
	                            case 'u':
	                            case 'U': {
	                                int max = value.charAt(i - 1) == 'u' ? 4 : 8;
	                                int skip = value.charAt(i) == '{' ? 1 : 0;
	                                i += skip;
	                                int number = hexChar(value.charAt(i));
	                                i++;
	                                for (int j = 0; j < max - 1; j++) {
	                                    int next = hexChar(value.charAt(i));
	                                    if (next == -1) {
	                                        break;
	                                    }
	                                    number = number * 16 + next;
	                                    i++;
	                                }
	                                i += skip;
	                                str.appendCodePoint(number);
	                                break;
	                            }
	                            case '0':
	                            case '1':
	                            case '2':
	                            case '3':
	                            case '4':
	                            case '5':
	                            case '6':
	                            case '7': {
	                                // up to 3 digits
	                                int number = value.charAt(i - 1) - '0';
	                                for (int j = 0; j < 2; j++) {
	                                    if (value.charAt(i) < '0' || value.charAt(i) > '7') {
	                                        break;
	                                    }
	                                    number = number * 8 + (value.charAt(i) - '0');
	                                    i++;
	                                }
	                                str.appendCodePoint(number & 255);
	                                break;
	                            }

	                            default:
	                                throw new IllegalArgumentException("malformed string constant");

	                        }
	                    }
	                }
	                return str.toString();
	            }
	        }

	        private static int hexChar(char digit) {
	            if (digit >= '0' && digit <= '9') {
	                return digit - '0';
	            } else if (digit >= 'a' && digit <= 'f') {
	                return digit - 'a' + 0xa;
	            } else if (digit >= 'A' && digit <= 'F') {
	                return digit - 'A' + 0xa;
	            } else {
	                return -1;
	            }
	        }

	    @Override
		public void notifyListeners(LexerNoViableAltException e) {
			getErrorListenerDispatch().syntaxError(this, null, _tokenStartLine, _tokenStartCharPositionInLine, null, e);
		}


	public RLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "R.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 0:
			COMMENT_action(_localctx, actionIndex);
			break;
		case 22:
			LBRACE_action(_localctx, actionIndex);
			break;
		case 23:
			RBRACE_action(_localctx, actionIndex);
			break;
		case 24:
			LPAR_action(_localctx, actionIndex);
			break;
		case 25:
			RPAR_action(_localctx, actionIndex);
			break;
		case 26:
			LBB_action(_localctx, actionIndex);
			break;
		case 27:
			LBRAKET_action(_localctx, actionIndex);
			break;
		case 28:
			RBRAKET_action(_localctx, actionIndex);
			break;
		case 59:
			NEWLINE_action(_localctx, actionIndex);
			break;
		case 60:
			INTEGER_action(_localctx, actionIndex);
			break;
		case 61:
			COMPLEX_action(_localctx, actionIndex);
			break;
		case 64:
			ID_action(_localctx, actionIndex);
			break;
		case 67:
			STRING_action(_localctx, actionIndex);
			break;
		}
	}
	private void COMMENT_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:
			 if (incompleteNesting > 0) skip(); 
			break;
		}
	}
	private void LBRACE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 1:
			 nestingStack.add(incompleteNesting); incompleteNesting = 0; 
			break;
		}
	}
	private void RBRACE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 2:
			 if (!nestingStack.isEmpty()) { incompleteNesting = nestingStack.remove(nestingStack.size() - 1); } 
			break;
		}
	}
	private void LPAR_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 3:
			 incompleteNesting++; 
			break;
		}
	}
	private void RPAR_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 4:
			 incompleteNesting--; 
			break;
		}
	}
	private void LBB_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 5:
			 incompleteNesting+=2; 
			break;
		}
	}
	private void LBRAKET_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 6:
			 incompleteNesting++; 
			break;
		}
	}
	private void RBRAKET_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 7:
			 incompleteNesting--; 
			break;
		}
	}
	private void NEWLINE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 8:
			 if (incompleteNesting > 0) skip(); 
			break;
		}
	}
	private void INTEGER_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 9:
			 setText(getText().substring(0, getText().length()-1)); 
			break;
		case 10:
			 setText(getText().substring(0, getText().length()-1)); 
			break;
		case 11:
			 setText(getText().substring(0, getText().length()-1)); 
			break;
		}
	}
	private void COMPLEX_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 12:
			 setText(getText().substring(0, getText().length()-1)); 
			break;
		case 13:
			 setText(getText().substring(0, getText().length()-1)); 
			break;
		case 14:
			 setText(getText().substring(0, getText().length()-1)); 
			break;
		}
	}
	private void ID_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 15:
			 if (getText().startsWith("`")) setText(parseString(getText())); 
			break;
		}
	}
	private void STRING_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 16:
			 setText(parseString(getText())); 
			break;
		}
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2E\u0329\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\3\2\3\2\7\2\u00a6"+
		"\n\2\f\2\16\2\u00a9\13\2\3\2\3\2\5\2\u00ad\n\2\3\2\3\2\3\3\3\3\3\3\3\3"+
		"\5\3\u00b5\n\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7"+
		"\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3\r\3"+
		"\r\3\16\3\16\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3"+
		"\23\3\23\3\24\3\24\3\24\3\25\3\25\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3"+
		"\30\3\31\3\31\3\31\3\32\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3"+
		"\34\3\35\3\35\3\35\3\36\3\36\3\36\3\37\3\37\3\37\5\37\u0109\n\37\3 \3"+
		" \3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3)\3)\3)\3"+
		")\3)\3)\3)\3*\3*\3*\3*\3*\3+\3+\3+\3,\3,\3,\3,\3,\3,\3,\3,\3,\3,\3,\3"+
		",\3-\3-\3-\3-\3-\3-\3-\3-\3-\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3"+
		".\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\61\3"+
		"\61\3\61\3\61\3\61\3\61\3\62\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3\64\3"+
		"\64\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\66\3\66\3\66\3\66\3\66\3"+
		"\66\3\66\3\67\3\67\3\67\38\38\38\39\39\39\39\39\3:\3:\3:\3:\3:\3;\3;\3"+
		";\3;\3;\3;\3<\3<\3<\3<\3=\3=\3=\3>\6>\u019f\n>\r>\16>\u01a0\3>\3>\7>\u01a5"+
		"\n>\f>\16>\u01a8\13>\3>\3>\3>\5>\u01ad\n>\3>\6>\u01b0\n>\r>\16>\u01b1"+
		"\3>\5>\u01b5\n>\3>\3>\3>\3>\3>\3>\6>\u01bd\n>\r>\16>\u01be\3>\3>\3>\5"+
		">\u01c4\n>\3?\6?\u01c7\n?\r?\16?\u01c8\3?\3?\7?\u01cd\n?\f?\16?\u01d0"+
		"\13?\3?\5?\u01d3\n?\3?\3?\3?\5?\u01d8\n?\3?\6?\u01db\n?\r?\16?\u01dc\3"+
		"?\5?\u01e0\n?\3?\3?\3?\3?\3?\3?\6?\u01e8\n?\r?\16?\u01e9\3?\5?\u01ed\n"+
		"?\3?\7?\u01f0\n?\f?\16?\u01f3\13?\3?\5?\u01f6\n?\3?\3?\3?\5?\u01fb\n?"+
		"\3@\6@\u01fe\n@\r@\16@\u01ff\3@\3@\7@\u0204\n@\f@\16@\u0207\13@\3@\5@"+
		"\u020a\n@\3@\5@\u020d\n@\3@\6@\u0210\n@\r@\16@\u0211\3@\5@\u0215\n@\3"+
		"@\3@\3@\3@\6@\u021b\n@\r@\16@\u021c\3@\5@\u0220\n@\3@\7@\u0223\n@\f@\16"+
		"@\u0226\13@\3@\5@\u0229\n@\5@\u022b\n@\3A\3A\3A\3A\6A\u0231\nA\rA\16A"+
		"\u0232\3B\7B\u0236\nB\fB\16B\u0239\13B\3B\3B\3B\6B\u023e\nB\rB\16B\u023f"+
		"\3B\7B\u0243\nB\fB\16B\u0246\13B\3B\3B\3B\3B\3B\3B\3B\3B\3B\7B\u0251\n"+
		"B\fB\16B\u0254\13B\3B\3B\5B\u0258\nB\3B\3B\3C\3C\7C\u025e\nC\fC\16C\u0261"+
		"\13C\3C\3C\3D\3D\7D\u0267\nD\fD\16D\u026a\13D\3D\3D\3E\3E\3E\7E\u0271"+
		"\nE\fE\16E\u0274\13E\3E\3E\3E\3E\7E\u027a\nE\fE\16E\u027d\13E\3E\5E\u0280"+
		"\nE\3E\3E\3F\3F\3F\3F\5F\u0288\nF\3F\5F\u028b\nF\3F\3F\3F\3F\3F\3F\3F"+
		"\5F\u0294\nF\3F\5F\u0297\nF\3F\5F\u029a\nF\3F\3F\3F\5F\u029f\nF\3F\5F"+
		"\u02a2\nF\3F\5F\u02a5\nF\3F\5F\u02a8\nF\3F\5F\u02ab\nF\3F\5F\u02ae\nF"+
		"\3F\5F\u02b1\nF\3F\3F\3F\3F\5F\u02b7\nF\3F\5F\u02ba\nF\3F\5F\u02bd\nF"+
		"\3F\3F\3F\3F\3F\3F\5F\u02c5\nF\3F\5F\u02c8\nF\3F\5F\u02cb\nF\3F\5F\u02ce"+
		"\nF\3F\5F\u02d1\nF\3F\5F\u02d4\nF\3F\5F\u02d7\nF\3F\3F\5F\u02db\nF\3G"+
		"\5G\u02de\nG\3G\3G\5G\u02e2\nG\3G\5G\u02e5\nG\3H\3H\5H\u02e9\nH\3H\6H"+
		"\u02ec\nH\rH\16H\u02ed\3I\3I\5I\u02f2\nI\3I\6I\u02f5\nI\rI\16I\u02f6\3"+
		"J\3J\5J\u02fb\nJ\3K\3K\7K\u02ff\nK\fK\16K\u0302\13K\3L\3L\3L\3L\3L\3L"+
		"\3L\5L\u030b\nL\3M\3M\3M\3M\3M\3M\3M\3N\3N\3N\3N\3N\5N\u0319\nN\3O\3O"+
		"\3P\3P\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\5Q\u0328\nQ\2\2R\3\3\5\4\7\5\t\6\13"+
		"\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'"+
		"\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'"+
		"M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9q:s;u<w=y>{?}@\177"+
		"A\u0081B\u0083C\u0085D\u0087\2\u0089E\u008b\2\u008d\2\u008f\2\u0091\2"+
		"\u0093\2\u0095\2\u0097\2\u0099\2\u009b\2\u009d\2\u009f\2\u00a1\2\3\2\17"+
		"\4\2\f\f\16\17\5\2\13\13\"\"\u00a2\u00a2\5\2\f\f\16\17\'\'\4\2^^bb\4\2"+
		"$$^^\4\2))^^\r\2\f\f\"\"$$))^^bdhhppttvvxx\4\2GGgg\4\2--//\4\2RRrr\t\2"+
		"((,\61<<>A``~~\u0080\u0080\f\2\"\"$$))^^bdhhppttvvxx\5\2\62;CHch\4\u0297"+
		"\2C\2\\\2a\2a\2c\2|\2\u00ac\2\u00ac\2\u00b7\2\u00b7\2\u00bc\2\u00bc\2"+
		"\u00c2\2\u00d8\2\u00da\2\u00f8\2\u00fa\2\u02c3\2\u02c8\2\u02d3\2\u02e2"+
		"\2\u02e6\2\u02ee\2\u02ee\2\u02f0\2\u02f0\2\u0347\2\u0347\2\u0372\2\u0376"+
		"\2\u0378\2\u0379\2\u037c\2\u037f\2\u0381\2\u0381\2\u0388\2\u0388\2\u038a"+
		"\2\u038c\2\u038e\2\u038e\2\u0390\2\u03a3\2\u03a5\2\u03f7\2\u03f9\2\u0483"+
		"\2\u048c\2\u0531\2\u0533\2\u0558\2\u055b\2\u055b\2\u0563\2\u0589\2\u05b2"+
		"\2\u05bf\2\u05c1\2\u05c1\2\u05c3\2\u05c4\2\u05c6\2\u05c7\2\u05c9\2\u05c9"+
		"\2\u05d2\2\u05ec\2\u05f2\2\u05f4\2\u0612\2\u061c\2\u0622\2\u0659\2\u065b"+
		"\2\u0661\2\u0670\2\u06d5\2\u06d7\2\u06de\2\u06e3\2\u06ea\2\u06ef\2\u06f1"+
		"\2\u06fc\2\u06fe\2\u0701\2\u0701\2\u0712\2\u0741\2\u074f\2\u07b3\2\u07cc"+
		"\2\u07ec\2\u07f6\2\u07f7\2\u07fc\2\u07fc\2\u0802\2\u0819\2\u081c\2\u082e"+
		"\2\u0842\2\u085a\2\u0862\2\u086c\2\u08a2\2\u08b6\2\u08b8\2\u08bf\2\u08d6"+
		"\2\u08e1\2\u08e5\2\u08eb\2\u08f2\2\u093d\2\u093f\2\u094e\2\u0950\2\u0952"+
		"\2\u0957\2\u0965\2\u0973\2\u0985\2\u0987\2\u098e\2\u0991\2\u0992\2\u0995"+
		"\2\u09aa\2\u09ac\2\u09b2\2\u09b4\2\u09b4\2\u09b8\2\u09bb\2\u09bf\2\u09c6"+
		"\2\u09c9\2\u09ca\2\u09cd\2\u09ce\2\u09d0\2\u09d0\2\u09d9\2\u09d9\2\u09de"+
		"\2\u09df\2\u09e1\2\u09e5\2\u09f2\2\u09f3\2\u09fe\2\u09fe\2\u0a03\2\u0a05"+
		"\2\u0a07\2\u0a0c\2\u0a11\2\u0a12\2\u0a15\2\u0a2a\2\u0a2c\2\u0a32\2\u0a34"+
		"\2\u0a35\2\u0a37\2\u0a38\2\u0a3a\2\u0a3b\2\u0a40\2\u0a44\2\u0a49\2\u0a4a"+
		"\2\u0a4d\2\u0a4e\2\u0a53\2\u0a53\2\u0a5b\2\u0a5e\2\u0a60\2\u0a60\2\u0a72"+
		"\2\u0a77\2\u0a83\2\u0a85\2\u0a87\2\u0a8f\2\u0a91\2\u0a93\2\u0a95\2\u0aaa"+
		"\2\u0aac\2\u0ab2\2\u0ab4\2\u0ab5\2\u0ab7\2\u0abb\2\u0abf\2\u0ac7\2\u0ac9"+
		"\2\u0acb\2\u0acd\2\u0ace\2\u0ad2\2\u0ad2\2\u0ae2\2\u0ae5\2\u0afb\2\u0afe"+
		"\2\u0b03\2\u0b05\2\u0b07\2\u0b0e\2\u0b11\2\u0b12\2\u0b15\2\u0b2a\2\u0b2c"+
		"\2\u0b32\2\u0b34\2\u0b35\2\u0b37\2\u0b3b\2\u0b3f\2\u0b46\2\u0b49\2\u0b4a"+
		"\2\u0b4d\2\u0b4e\2\u0b58\2\u0b59\2\u0b5e\2\u0b5f\2\u0b61\2\u0b65\2\u0b73"+
		"\2\u0b73\2\u0b84\2\u0b85\2\u0b87\2\u0b8c\2\u0b90\2\u0b92\2\u0b94\2\u0b97"+
		"\2\u0b9b\2\u0b9c\2\u0b9e\2\u0b9e\2\u0ba0\2\u0ba1\2\u0ba5\2\u0ba6\2\u0baa"+
		"\2\u0bac\2\u0bb0\2\u0bbb\2\u0bc0\2\u0bc4\2\u0bc8\2\u0bca\2\u0bcc\2\u0bce"+
		"\2\u0bd2\2\u0bd2\2\u0bd9\2\u0bd9\2\u0c02\2\u0c05\2\u0c07\2\u0c0e\2\u0c10"+
		"\2\u0c12\2\u0c14\2\u0c2a\2\u0c2c\2\u0c3b\2\u0c3f\2\u0c46\2\u0c48\2\u0c4a"+
		"\2\u0c4c\2\u0c4e\2\u0c57\2\u0c58\2\u0c5a\2\u0c5c\2\u0c62\2\u0c65\2\u0c82"+
		"\2\u0c85\2\u0c87\2\u0c8e\2\u0c90\2\u0c92\2\u0c94\2\u0caa\2\u0cac\2\u0cb5"+
		"\2\u0cb7\2\u0cbb\2\u0cbf\2\u0cc6\2\u0cc8\2\u0cca\2\u0ccc\2\u0cce\2\u0cd7"+
		"\2\u0cd8\2\u0ce0\2\u0ce0\2\u0ce2\2\u0ce5\2\u0cf3\2\u0cf4\2\u0d02\2\u0d05"+
		"\2\u0d07\2\u0d0e\2\u0d10\2\u0d12\2\u0d14\2\u0d3c\2\u0d3f\2\u0d46\2\u0d48"+
		"\2\u0d4a\2\u0d4c\2\u0d4e\2\u0d50\2\u0d50\2\u0d56\2\u0d59\2\u0d61\2\u0d65"+
		"\2\u0d7c\2\u0d81\2\u0d84\2\u0d85\2\u0d87\2\u0d98\2\u0d9c\2\u0db3\2\u0db5"+
		"\2\u0dbd\2\u0dbf\2\u0dbf\2\u0dc2\2\u0dc8\2\u0dd1\2\u0dd6\2\u0dd8\2\u0dd8"+
		"\2\u0dda\2\u0de1\2\u0df4\2\u0df5\2\u0e03\2\u0e3c\2\u0e42\2\u0e48\2\u0e4f"+
		"\2\u0e4f\2\u0e83\2\u0e84\2\u0e86\2\u0e86\2\u0e89\2\u0e8a\2\u0e8c\2\u0e8c"+
		"\2\u0e8f\2\u0e8f\2\u0e96\2\u0e99\2\u0e9b\2\u0ea1\2\u0ea3\2\u0ea5\2\u0ea7"+
		"\2\u0ea7\2\u0ea9\2\u0ea9\2\u0eac\2\u0ead\2\u0eaf\2\u0ebb\2\u0ebd\2\u0ebf"+
		"\2\u0ec2\2\u0ec6\2\u0ec8\2\u0ec8\2\u0ecf\2\u0ecf\2\u0ede\2\u0ee1\2\u0f02"+
		"\2\u0f02\2\u0f42\2\u0f49\2\u0f4b\2\u0f6e\2\u0f73\2\u0f83\2\u0f8a\2\u0f99"+
		"\2\u0f9b\2\u0fbe\2\u1002\2\u1038\2\u103a\2\u103a\2\u103d\2\u1041\2\u1052"+
		"\2\u1064\2\u1067\2\u106a\2\u1070\2\u1088\2\u1090\2\u1090\2\u109e\2\u109f"+
		"\2\u10a2\2\u10c7\2\u10c9\2\u10c9\2\u10cf\2\u10cf\2\u10d2\2\u10fc\2\u10fe"+
		"\2\u124a\2\u124c\2\u124f\2\u1252\2\u1258\2\u125a\2\u125a\2\u125c\2\u125f"+
		"\2\u1262\2\u128a\2\u128c\2\u128f\2\u1292\2\u12b2\2\u12b4\2\u12b7\2\u12ba"+
		"\2\u12c0\2\u12c2\2\u12c2\2\u12c4\2\u12c7\2\u12ca\2\u12d8\2\u12da\2\u1312"+
		"\2\u1314\2\u1317\2\u131a\2\u135c\2\u1361\2\u1361\2\u1382\2\u1391\2\u13a2"+
		"\2\u13f7\2\u13fa\2\u13ff\2\u1403\2\u166e\2\u1671\2\u1681\2\u1683\2\u169c"+
		"\2\u16a2\2\u16ec\2\u16f0\2\u16fa\2\u1702\2\u170e\2\u1710\2\u1715\2\u1722"+
		"\2\u1735\2\u1742\2\u1755\2\u1762\2\u176e\2\u1770\2\u1772\2\u1774\2\u1775"+
		"\2\u1782\2\u17b5\2\u17b8\2\u17ca\2\u17d9\2\u17d9\2\u17de\2\u17de\2\u1822"+
		"\2\u1879\2\u1882\2\u18ac\2\u18b2\2\u18f7\2\u1902\2\u1920\2\u1922\2\u192d"+
		"\2\u1932\2\u193a\2\u1952\2\u196f\2\u1972\2\u1976\2\u1982\2\u19ad\2\u19b2"+
		"\2\u19cb\2\u1a02\2\u1a1d\2\u1a22\2\u1a60\2\u1a63\2\u1a76\2\u1aa9\2\u1aa9"+
		"\2\u1b02\2\u1b35\2\u1b37\2\u1b45\2\u1b47\2\u1b4d\2\u1b82\2\u1bab\2\u1bae"+
		"\2\u1bb1\2\u1bbc\2\u1be7\2\u1be9\2\u1bf3\2\u1c02\2\u1c37\2\u1c4f\2\u1c51"+
		"\2\u1c5c\2\u1c7f\2\u1c82\2\u1c8a\2\u1ceb\2\u1cee\2\u1cf0\2\u1cf5\2\u1cf7"+
		"\2\u1cf8\2\u1d02\2\u1dc1\2\u1de9\2\u1df6\2\u1e02\2\u1f17\2\u1f1a\2\u1f1f"+
		"\2\u1f22\2\u1f47\2\u1f4a\2\u1f4f\2\u1f52\2\u1f59\2\u1f5b\2\u1f5b\2\u1f5d"+
		"\2\u1f5d\2\u1f5f\2\u1f5f\2\u1f61\2\u1f7f\2\u1f82\2\u1fb6\2\u1fb8\2\u1fbe"+
		"\2\u1fc0\2\u1fc0\2\u1fc4\2\u1fc6\2\u1fc8\2\u1fce\2\u1fd2\2\u1fd5\2\u1fd8"+
		"\2\u1fdd\2\u1fe2\2\u1fee\2\u1ff4\2\u1ff6\2\u1ff8\2\u1ffe\2\u2073\2\u2073"+
		"\2\u2081\2\u2081\2\u2092\2\u209e\2\u2104\2\u2104\2\u2109\2\u2109\2\u210c"+
		"\2\u2115\2\u2117\2\u2117\2\u211b\2\u211f\2\u2126\2\u2126\2\u2128\2\u2128"+
		"\2\u212a\2\u212a\2\u212c\2\u212f\2\u2131\2\u213b\2\u213e\2\u2141\2\u2147"+
		"\2\u214b\2\u2150\2\u2150\2\u2162\2\u218a\2\u24b8\2\u24eb\2\u2c02\2\u2c30"+
		"\2\u2c32\2\u2c60\2\u2c62\2\u2ce6\2\u2ced\2\u2cf0\2\u2cf4\2\u2cf5\2\u2d02"+
		"\2\u2d27\2\u2d29\2\u2d29\2\u2d2f\2\u2d2f\2\u2d32\2\u2d69\2\u2d71\2\u2d71"+
		"\2\u2d82\2\u2d98\2\u2da2\2\u2da8\2\u2daa\2\u2db0\2\u2db2\2\u2db8\2\u2dba"+
		"\2\u2dc0\2\u2dc2\2\u2dc8\2\u2dca\2\u2dd0\2\u2dd2\2\u2dd8\2\u2dda\2\u2de0"+
		"\2\u2de2\2\u2e01\2\u2e31\2\u2e31\2\u3007\2\u3009\2\u3023\2\u302b\2\u3033"+
		"\2\u3037\2\u303a\2\u303e\2\u3043\2\u3098\2\u309f\2\u30a1\2\u30a3\2\u30fc"+
		"\2\u30fe\2\u3101\2\u3107\2\u3130\2\u3133\2\u3190\2\u31a2\2\u31bc\2\u31f2"+
		"\2\u3201\2\u3402\2\u4db7\2\u4e02\2\u9fec\2\ua002\2\ua48e\2\ua4d2\2\ua4ff"+
		"\2\ua502\2\ua60e\2\ua612\2\ua621\2\ua62c\2\ua62d\2\ua642\2\ua670\2\ua676"+
		"\2\ua67d\2\ua681\2\ua6f1\2\ua719\2\ua721\2\ua724\2\ua78a\2\ua78d\2\ua7b0"+
		"\2\ua7b2\2\ua7b9\2\ua7f9\2\ua803\2\ua805\2\ua807\2\ua809\2\ua80c\2\ua80e"+
		"\2\ua829\2\ua842\2\ua875\2\ua882\2\ua8c5\2\ua8c7\2\ua8c7\2\ua8f4\2\ua8f9"+
		"\2\ua8fd\2\ua8fd\2\ua8ff\2\ua8ff\2\ua90c\2\ua92c\2\ua932\2\ua954\2\ua962"+
		"\2\ua97e\2\ua982\2\ua9b4\2\ua9b6\2\ua9c1\2\ua9d1\2\ua9d1\2\ua9e2\2\ua9e6"+
		"\2\ua9e8\2\ua9f1\2\ua9fc\2\uaa00\2\uaa02\2\uaa38\2\uaa42\2\uaa4f\2\uaa62"+
		"\2\uaa78\2\uaa7c\2\uaa7c\2\uaa80\2\uaac0\2\uaac2\2\uaac2\2\uaac4\2\uaac4"+
		"\2\uaadd\2\uaadf\2\uaae2\2\uaaf1\2\uaaf4\2\uaaf7\2\uab03\2\uab08\2\uab0b"+
		"\2\uab10\2\uab13\2\uab18\2\uab22\2\uab28\2\uab2a\2\uab30\2\uab32\2\uab5c"+
		"\2\uab5e\2\uab67\2\uab72\2\uabec\2\uac02\2\ud7a5\2\ud7b2\2\ud7c8\2\ud7cd"+
		"\2\ud7fd\2\uf902\2\ufa6f\2\ufa72\2\ufadb\2\ufb02\2\ufb08\2\ufb15\2\ufb19"+
		"\2\ufb1f\2\ufb2a\2\ufb2c\2\ufb38\2\ufb3a\2\ufb3e\2\ufb40\2\ufb40\2\ufb42"+
		"\2\ufb43\2\ufb45\2\ufb46\2\ufb48\2\ufbb3\2\ufbd5\2\ufd3f\2\ufd52\2\ufd91"+
		"\2\ufd94\2\ufdc9\2\ufdf2\2\ufdfd\2\ufe72\2\ufe76\2\ufe78\2\ufefe\2\uff23"+
		"\2\uff3c\2\uff43\2\uff5c\2\uff68\2\uffc0\2\uffc4\2\uffc9\2\uffcc\2\uffd1"+
		"\2\uffd4\2\uffd9\2\uffdc\2\uffde\2\2\3\r\3\17\3(\3*\3<\3>\3?\3A\3O\3R"+
		"\3_\3\u0082\3\u00fc\3\u0142\3\u0176\3\u0282\3\u029e\3\u02a2\3\u02d2\3"+
		"\u0302\3\u0321\3\u032f\3\u034c\3\u0352\3\u037c\3\u0382\3\u039f\3\u03a2"+
		"\3\u03c5\3\u03ca\3\u03d1\3\u03d3\3\u03d7\3\u0402\3\u049f\3\u04b2\3\u04d5"+
		"\3\u04da\3\u04fd\3\u0502\3\u0529\3\u0532\3\u0565\3\u0602\3\u0738\3\u0742"+
		"\3\u0757\3\u0762\3\u0769\3\u0802\3\u0807\3\u080a\3\u080a\3\u080c\3\u0837"+
		"\3\u0839\3\u083a\3\u083e\3\u083e\3\u0841\3\u0857\3\u0862\3\u0878\3\u0882"+
		"\3\u08a0\3\u08e2\3\u08f4\3\u08f6\3\u08f7\3\u0902\3\u0917\3\u0922\3\u093b"+
		"\3\u0982\3\u09b9\3\u09c0\3\u09c1\3\u0a02\3\u0a05\3\u0a07\3\u0a08\3\u0a0e"+
		"\3\u0a15\3\u0a17\3\u0a19\3\u0a1b\3\u0a35\3\u0a62\3\u0a7e\3\u0a82\3\u0a9e"+
		"\3\u0ac2\3\u0ac9\3\u0acb\3\u0ae6\3\u0b02\3\u0b37\3\u0b42\3\u0b57\3\u0b62"+
		"\3\u0b74\3\u0b82\3\u0b93\3\u0c02\3\u0c4a\3\u0c82\3\u0cb4\3\u0cc2\3\u0cf4"+
		"\3\u1002\3\u1047\3\u1084\3\u10ba\3\u10d2\3\u10ea\3\u1102\3\u1134\3\u1152"+
		"\3\u1174\3\u1178\3\u1178\3\u1182\3\u11c1\3\u11c3\3\u11c6\3\u11dc\3\u11dc"+
		"\3\u11de\3\u11de\3\u1202\3\u1213\3\u1215\3\u1236\3\u1239\3\u1239\3\u1240"+
		"\3\u1240\3\u1282\3\u1288\3\u128a\3\u128a\3\u128c\3\u128f\3\u1291\3\u129f"+
		"\3\u12a1\3\u12aa\3\u12b2\3\u12ea\3\u1302\3\u1305\3\u1307\3\u130e\3\u1311"+
		"\3\u1312\3\u1315\3\u132a\3\u132c\3\u1332\3\u1334\3\u1335\3\u1337\3\u133b"+
		"\3\u133f\3\u1346\3\u1349\3\u134a\3\u134d\3\u134e\3\u1352\3\u1352\3\u1359"+
		"\3\u1359\3\u135f\3\u1365\3\u1402\3\u1443\3\u1445\3\u1447\3\u1449\3\u144c"+
		"\3\u1482\3\u14c3\3\u14c6\3\u14c7\3\u14c9\3\u14c9\3\u1582\3\u15b7\3\u15ba"+
		"\3\u15c0\3\u15da\3\u15df\3\u1602\3\u1640\3\u1642\3\u1642\3\u1646\3\u1646"+
		"\3\u1682\3\u16b7\3\u1702\3\u171b\3\u171f\3\u172c\3\u18a2\3\u18e1\3\u1901"+
		"\3\u1901\3\u1a02\3\u1a34\3\u1a37\3\u1a40\3\u1a52\3\u1a85\3\u1a88\3\u1a99"+
		"\3\u1ac2\3\u1afa\3\u1c02\3\u1c0a\3\u1c0c\3\u1c38\3\u1c3a\3\u1c40\3\u1c42"+
		"\3\u1c42\3\u1c74\3\u1c91\3\u1c94\3\u1ca9\3\u1cab\3\u1cb8\3\u1d02\3\u1d08"+
		"\3\u1d0a\3\u1d0b\3\u1d0d\3\u1d38\3\u1d3c\3\u1d3c\3\u1d3e\3\u1d3f\3\u1d41"+
		"\3\u1d43\3\u1d45\3\u1d45\3\u1d48\3\u1d49\3\u2002\3\u239b\3\u2402\3\u2470"+
		"\3\u2482\3\u2545\3\u3002\3\u3430\3\u4402\3\u4648\3\u6802\3\u6a3a\3\u6a42"+
		"\3\u6a60\3\u6ad2\3\u6aef\3\u6b02\3\u6b38\3\u6b42\3\u6b45\3\u6b65\3\u6b79"+
		"\3\u6b7f\3\u6b91\3\u6f02\3\u6f46\3\u6f52\3\u6f80\3\u6f95\3\u6fa1\3\u6fe2"+
		"\3\u6fe3\3\u7002\3\u87ee\3\u8802\3\u8af4\3\ub002\3\ub120\3\ub172\3\ub2fd"+
		"\3\ubc02\3\ubc6c\3\ubc72\3\ubc7e\3\ubc82\3\ubc8a\3\ubc92\3\ubc9b\3\ubca0"+
		"\3\ubca0\3\ud402\3\ud456\3\ud458\3\ud49e\3\ud4a0\3\ud4a1\3\ud4a4\3\ud4a4"+
		"\3\ud4a7\3\ud4a8\3\ud4ab\3\ud4ae\3\ud4b0\3\ud4bb\3\ud4bd\3\ud4bd\3\ud4bf"+
		"\3\ud4c5\3\ud4c7\3\ud507\3\ud509\3\ud50c\3\ud50f\3\ud516\3\ud518\3\ud51e"+
		"\3\ud520\3\ud53b\3\ud53d\3\ud540\3\ud542\3\ud546\3\ud548\3\ud548\3\ud54c"+
		"\3\ud552\3\ud554\3\ud6a7\3\ud6aa\3\ud6c2\3\ud6c4\3\ud6dc\3\ud6de\3\ud6fc"+
		"\3\ud6fe\3\ud716\3\ud718\3\ud736\3\ud738\3\ud750\3\ud752\3\ud770\3\ud772"+
		"\3\ud78a\3\ud78c\3\ud7aa\3\ud7ac\3\ud7c4\3\ud7c6\3\ud7cd\3\ue002\3\ue008"+
		"\3\ue00a\3\ue01a\3\ue01d\3\ue023\3\ue025\3\ue026\3\ue028\3\ue02c\3\ue802"+
		"\3\ue8c6\3\ue902\3\ue945\3\ue949\3\ue949\3\uee02\3\uee05\3\uee07\3\uee21"+
		"\3\uee23\3\uee24\3\uee26\3\uee26\3\uee29\3\uee29\3\uee2b\3\uee34\3\uee36"+
		"\3\uee39\3\uee3b\3\uee3b\3\uee3d\3\uee3d\3\uee44\3\uee44\3\uee49\3\uee49"+
		"\3\uee4b\3\uee4b\3\uee4d\3\uee4d\3\uee4f\3\uee51\3\uee53\3\uee54\3\uee56"+
		"\3\uee56\3\uee59\3\uee59\3\uee5b\3\uee5b\3\uee5d\3\uee5d\3\uee5f\3\uee5f"+
		"\3\uee61\3\uee61\3\uee63\3\uee64\3\uee66\3\uee66\3\uee69\3\uee6c\3\uee6e"+
		"\3\uee74\3\uee76\3\uee79\3\uee7b\3\uee7e\3\uee80\3\uee80\3\uee82\3\uee8b"+
		"\3\uee8d\3\uee9d\3\ueea3\3\ueea5\3\ueea7\3\ueeab\3\ueead\3\ueebd\3\uf132"+
		"\3\uf14b\3\uf152\3\uf16b\3\uf172\3\uf18b\3\2\4\ua6d8\4\ua702\4\ub736\4"+
		"\ub742\4\ub81f\4\ub822\4\ucea3\4\uceb2\4\uebe2\4\uf802\4\ufa1f\4\u02bb"+
		"\2\60\2\60\2\62\2;\2C\2\\\2a\2a\2c\2|\2\u00ac\2\u00ac\2\u00b7\2\u00b7"+
		"\2\u00bc\2\u00bc\2\u00c2\2\u00d8\2\u00da\2\u00f8\2\u00fa\2\u02c3\2\u02c8"+
		"\2\u02d3\2\u02e2\2\u02e6\2\u02ee\2\u02ee\2\u02f0\2\u02f0\2\u0347\2\u0347"+
		"\2\u0372\2\u0376\2\u0378\2\u0379\2\u037c\2\u037f\2\u0381\2\u0381\2\u0388"+
		"\2\u0388\2\u038a\2\u038c\2\u038e\2\u038e\2\u0390\2\u03a3\2\u03a5\2\u03f7"+
		"\2\u03f9\2\u0483\2\u048c\2\u0531\2\u0533\2\u0558\2\u055b\2\u055b\2\u0563"+
		"\2\u0589\2\u05b2\2\u05bf\2\u05c1\2\u05c1\2\u05c3\2\u05c4\2\u05c6\2\u05c7"+
		"\2\u05c9\2\u05c9\2\u05d2\2\u05ec\2\u05f2\2\u05f4\2\u0612\2\u061c\2\u0622"+
		"\2\u0659\2\u065b\2\u066b\2\u0670\2\u06d5\2\u06d7\2\u06de\2\u06e3\2\u06ea"+
		"\2\u06ef\2\u06fe\2\u0701\2\u0701\2\u0712\2\u0741\2\u074f\2\u07b3\2\u07c2"+
		"\2\u07ec\2\u07f6\2\u07f7\2\u07fc\2\u07fc\2\u0802\2\u0819\2\u081c\2\u082e"+
		"\2\u0842\2\u085a\2\u0862\2\u086c\2\u08a2\2\u08b6\2\u08b8\2\u08bf\2\u08d6"+
		"\2\u08e1\2\u08e5\2\u08eb\2\u08f2\2\u093d\2\u093f\2\u094e\2\u0950\2\u0952"+
		"\2\u0957\2\u0965\2\u0968\2\u0971\2\u0973\2\u0985\2\u0987\2\u098e\2\u0991"+
		"\2\u0992\2\u0995\2\u09aa\2\u09ac\2\u09b2\2\u09b4\2\u09b4\2\u09b8\2\u09bb"+
		"\2\u09bf\2\u09c6\2\u09c9\2\u09ca\2\u09cd\2\u09ce\2\u09d0\2\u09d0\2\u09d9"+
		"\2\u09d9\2\u09de\2\u09df\2\u09e1\2\u09e5\2\u09e8\2\u09f3\2\u09fe\2\u09fe"+
		"\2\u0a03\2\u0a05\2\u0a07\2\u0a0c\2\u0a11\2\u0a12\2\u0a15\2\u0a2a\2\u0a2c"+
		"\2\u0a32\2\u0a34\2\u0a35\2\u0a37\2\u0a38\2\u0a3a\2\u0a3b\2\u0a40\2\u0a44"+
		"\2\u0a49\2\u0a4a\2\u0a4d\2\u0a4e\2\u0a53\2\u0a53\2\u0a5b\2\u0a5e\2\u0a60"+
		"\2\u0a60\2\u0a68\2\u0a77\2\u0a83\2\u0a85\2\u0a87\2\u0a8f\2\u0a91\2\u0a93"+
		"\2\u0a95\2\u0aaa\2\u0aac\2\u0ab2\2\u0ab4\2\u0ab5\2\u0ab7\2\u0abb\2\u0abf"+
		"\2\u0ac7\2\u0ac9\2\u0acb\2\u0acd\2\u0ace\2\u0ad2\2\u0ad2\2\u0ae2\2\u0ae5"+
		"\2\u0ae8\2\u0af1\2\u0afb\2\u0afe\2\u0b03\2\u0b05\2\u0b07\2\u0b0e\2\u0b11"+
		"\2\u0b12\2\u0b15\2\u0b2a\2\u0b2c\2\u0b32\2\u0b34\2\u0b35\2\u0b37\2\u0b3b"+
		"\2\u0b3f\2\u0b46\2\u0b49\2\u0b4a\2\u0b4d\2\u0b4e\2\u0b58\2\u0b59\2\u0b5e"+
		"\2\u0b5f\2\u0b61\2\u0b65\2\u0b68\2\u0b71\2\u0b73\2\u0b73\2\u0b84\2\u0b85"+
		"\2\u0b87\2\u0b8c\2\u0b90\2\u0b92\2\u0b94\2\u0b97\2\u0b9b\2\u0b9c\2\u0b9e"+
		"\2\u0b9e\2\u0ba0\2\u0ba1\2\u0ba5\2\u0ba6\2\u0baa\2\u0bac\2\u0bb0\2\u0bbb"+
		"\2\u0bc0\2\u0bc4\2\u0bc8\2\u0bca\2\u0bcc\2\u0bce\2\u0bd2\2\u0bd2\2\u0bd9"+
		"\2\u0bd9\2\u0be8\2\u0bf1\2\u0c02\2\u0c05\2\u0c07\2\u0c0e\2\u0c10\2\u0c12"+
		"\2\u0c14\2\u0c2a\2\u0c2c\2\u0c3b\2\u0c3f\2\u0c46\2\u0c48\2\u0c4a\2\u0c4c"+
		"\2\u0c4e\2\u0c57\2\u0c58\2\u0c5a\2\u0c5c\2\u0c62\2\u0c65\2\u0c68\2\u0c71"+
		"\2\u0c82\2\u0c85\2\u0c87\2\u0c8e\2\u0c90\2\u0c92\2\u0c94\2\u0caa\2\u0cac"+
		"\2\u0cb5\2\u0cb7\2\u0cbb\2\u0cbf\2\u0cc6\2\u0cc8\2\u0cca\2\u0ccc\2\u0cce"+
		"\2\u0cd7\2\u0cd8\2\u0ce0\2\u0ce0\2\u0ce2\2\u0ce5\2\u0ce8\2\u0cf1\2\u0cf3"+
		"\2\u0cf4\2\u0d02\2\u0d05\2\u0d07\2\u0d0e\2\u0d10\2\u0d12\2\u0d14\2\u0d3c"+
		"\2\u0d3f\2\u0d46\2\u0d48\2\u0d4a\2\u0d4c\2\u0d4e\2\u0d50\2\u0d50\2\u0d56"+
		"\2\u0d59\2\u0d61\2\u0d65\2\u0d68\2\u0d71\2\u0d7c\2\u0d81\2\u0d84\2\u0d85"+
		"\2\u0d87\2\u0d98\2\u0d9c\2\u0db3\2\u0db5\2\u0dbd\2\u0dbf\2\u0dbf\2\u0dc2"+
		"\2\u0dc8\2\u0dd1\2\u0dd6\2\u0dd8\2\u0dd8\2\u0dda\2\u0de1\2\u0de8\2\u0df1"+
		"\2\u0df4\2\u0df5\2\u0e03\2\u0e3c\2\u0e42\2\u0e48\2\u0e4f\2\u0e4f\2\u0e52"+
		"\2\u0e5b\2\u0e83\2\u0e84\2\u0e86\2\u0e86\2\u0e89\2\u0e8a\2\u0e8c\2\u0e8c"+
		"\2\u0e8f\2\u0e8f\2\u0e96\2\u0e99\2\u0e9b\2\u0ea1\2\u0ea3\2\u0ea5\2\u0ea7"+
		"\2\u0ea7\2\u0ea9\2\u0ea9\2\u0eac\2\u0ead\2\u0eaf\2\u0ebb\2\u0ebd\2\u0ebf"+
		"\2\u0ec2\2\u0ec6\2\u0ec8\2\u0ec8\2\u0ecf\2\u0ecf\2\u0ed2\2\u0edb\2\u0ede"+
		"\2\u0ee1\2\u0f02\2\u0f02\2\u0f22\2\u0f2b\2\u0f42\2\u0f49\2\u0f4b\2\u0f6e"+
		"\2\u0f73\2\u0f83\2\u0f8a\2\u0f99\2\u0f9b\2\u0fbe\2\u1002\2\u1038\2\u103a"+
		"\2\u103a\2\u103d\2\u104b\2\u1052\2\u1064\2\u1067\2\u106a\2\u1070\2\u1088"+
		"\2\u1090\2\u1090\2\u1092\2\u109b\2\u109e\2\u109f\2\u10a2\2\u10c7\2\u10c9"+
		"\2\u10c9\2\u10cf\2\u10cf\2\u10d2\2\u10fc\2\u10fe\2\u124a\2\u124c\2\u124f"+
		"\2\u1252\2\u1258\2\u125a\2\u125a\2\u125c\2\u125f\2\u1262\2\u128a\2\u128c"+
		"\2\u128f\2\u1292\2\u12b2\2\u12b4\2\u12b7\2\u12ba\2\u12c0\2\u12c2\2\u12c2"+
		"\2\u12c4\2\u12c7\2\u12ca\2\u12d8\2\u12da\2\u1312\2\u1314\2\u1317\2\u131a"+
		"\2\u135c\2\u1361\2\u1361\2\u1382\2\u1391\2\u13a2\2\u13f7\2\u13fa\2\u13ff"+
		"\2\u1403\2\u166e\2\u1671\2\u1681\2\u1683\2\u169c\2\u16a2\2\u16ec\2\u16f0"+
		"\2\u16fa\2\u1702\2\u170e\2\u1710\2\u1715\2\u1722\2\u1735\2\u1742\2\u1755"+
		"\2\u1762\2\u176e\2\u1770\2\u1772\2\u1774\2\u1775\2\u1782\2\u17b5\2\u17b8"+
		"\2\u17ca\2\u17d9\2\u17d9\2\u17de\2\u17de\2\u17e2\2\u17eb\2\u1812\2\u181b"+
		"\2\u1822\2\u1879\2\u1882\2\u18ac\2\u18b2\2\u18f7\2\u1902\2\u1920\2\u1922"+
		"\2\u192d\2\u1932\2\u193a\2\u1948\2\u196f\2\u1972\2\u1976\2\u1982\2\u19ad"+
		"\2\u19b2\2\u19cb\2\u19d2\2\u19db\2\u1a02\2\u1a1d\2\u1a22\2\u1a60\2\u1a63"+
		"\2\u1a76\2\u1a82\2\u1a8b\2\u1a92\2\u1a9b\2\u1aa9\2\u1aa9\2\u1b02\2\u1b35"+
		"\2\u1b37\2\u1b45\2\u1b47\2\u1b4d\2\u1b52\2\u1b5b\2\u1b82\2\u1bab\2\u1bae"+
		"\2\u1be7\2\u1be9\2\u1bf3\2\u1c02\2\u1c37\2\u1c42\2\u1c4b\2\u1c4f\2\u1c7f"+
		"\2\u1c82\2\u1c8a\2\u1ceb\2\u1cee\2\u1cf0\2\u1cf5\2\u1cf7\2\u1cf8\2\u1d02"+
		"\2\u1dc1\2\u1de9\2\u1df6\2\u1e02\2\u1f17\2\u1f1a\2\u1f1f\2\u1f22\2\u1f47"+
		"\2\u1f4a\2\u1f4f\2\u1f52\2\u1f59\2\u1f5b\2\u1f5b\2\u1f5d\2\u1f5d\2\u1f5f"+
		"\2\u1f5f\2\u1f61\2\u1f7f\2\u1f82\2\u1fb6\2\u1fb8\2\u1fbe\2\u1fc0\2\u1fc0"+
		"\2\u1fc4\2\u1fc6\2\u1fc8\2\u1fce\2\u1fd2\2\u1fd5\2\u1fd8\2\u1fdd\2\u1fe2"+
		"\2\u1fee\2\u1ff4\2\u1ff6\2\u1ff8\2\u1ffe\2\u2073\2\u2073\2\u2081\2\u2081"+
		"\2\u2092\2\u209e\2\u2104\2\u2104\2\u2109\2\u2109\2\u210c\2\u2115\2\u2117"+
		"\2\u2117\2\u211b\2\u211f\2\u2126\2\u2126\2\u2128\2\u2128\2\u212a\2\u212a"+
		"\2\u212c\2\u212f\2\u2131\2\u213b\2\u213e\2\u2141\2\u2147\2\u214b\2\u2150"+
		"\2\u2150\2\u2162\2\u218a\2\u24b8\2\u24eb\2\u2c02\2\u2c30\2\u2c32\2\u2c60"+
		"\2\u2c62\2\u2ce6\2\u2ced\2\u2cf0\2\u2cf4\2\u2cf5\2\u2d02\2\u2d27\2\u2d29"+
		"\2\u2d29\2\u2d2f\2\u2d2f\2\u2d32\2\u2d69\2\u2d71\2\u2d71\2\u2d82\2\u2d98"+
		"\2\u2da2\2\u2da8\2\u2daa\2\u2db0\2\u2db2\2\u2db8\2\u2dba\2\u2dc0\2\u2dc2"+
		"\2\u2dc8\2\u2dca\2\u2dd0\2\u2dd2\2\u2dd8\2\u2dda\2\u2de0\2\u2de2\2\u2e01"+
		"\2\u2e31\2\u2e31\2\u3007\2\u3009\2\u3023\2\u302b\2\u3033\2\u3037\2\u303a"+
		"\2\u303e\2\u3043\2\u3098\2\u309f\2\u30a1\2\u30a3\2\u30fc\2\u30fe\2\u3101"+
		"\2\u3107\2\u3130\2\u3133\2\u3190\2\u31a2\2\u31bc\2\u31f2\2\u3201\2\u3402"+
		"\2\u4db7\2\u4e02\2\u9fec\2\ua002\2\ua48e\2\ua4d2\2\ua4ff\2\ua502\2\ua60e"+
		"\2\ua612\2\ua62d\2\ua642\2\ua670\2\ua676\2\ua67d\2\ua681\2\ua6f1\2\ua719"+
		"\2\ua721\2\ua724\2\ua78a\2\ua78d\2\ua7b0\2\ua7b2\2\ua7b9\2\ua7f9\2\ua803"+
		"\2\ua805\2\ua807\2\ua809\2\ua80c\2\ua80e\2\ua829\2\ua842\2\ua875\2\ua882"+
		"\2\ua8c5\2\ua8c7\2\ua8c7\2\ua8d2\2\ua8db\2\ua8f4\2\ua8f9\2\ua8fd\2\ua8fd"+
		"\2\ua8ff\2\ua8ff\2\ua902\2\ua92c\2\ua932\2\ua954\2\ua962\2\ua97e\2\ua982"+
		"\2\ua9b4\2\ua9b6\2\ua9c1\2\ua9d1\2\ua9db\2\ua9e2\2\ua9e6\2\ua9e8\2\uaa00"+
		"\2\uaa02\2\uaa38\2\uaa42\2\uaa4f\2\uaa52\2\uaa5b\2\uaa62\2\uaa78\2\uaa7c"+
		"\2\uaa7c\2\uaa80\2\uaac0\2\uaac2\2\uaac2\2\uaac4\2\uaac4\2\uaadd\2\uaadf"+
		"\2\uaae2\2\uaaf1\2\uaaf4\2\uaaf7\2\uab03\2\uab08\2\uab0b\2\uab10\2\uab13"+
		"\2\uab18\2\uab22\2\uab28\2\uab2a\2\uab30\2\uab32\2\uab5c\2\uab5e\2\uab67"+
		"\2\uab72\2\uabec\2\uabf2\2\uabfb\2\uac02\2\ud7a5\2\ud7b2\2\ud7c8\2\ud7cd"+
		"\2\ud7fd\2\uf902\2\ufa6f\2\ufa72\2\ufadb\2\ufb02\2\ufb08\2\ufb15\2\ufb19"+
		"\2\ufb1f\2\ufb2a\2\ufb2c\2\ufb38\2\ufb3a\2\ufb3e\2\ufb40\2\ufb40\2\ufb42"+
		"\2\ufb43\2\ufb45\2\ufb46\2\ufb48\2\ufbb3\2\ufbd5\2\ufd3f\2\ufd52\2\ufd91"+
		"\2\ufd94\2\ufdc9\2\ufdf2\2\ufdfd\2\ufe72\2\ufe76\2\ufe78\2\ufefe\2\uff12"+
		"\2\uff1b\2\uff23\2\uff3c\2\uff43\2\uff5c\2\uff68\2\uffc0\2\uffc4\2\uffc9"+
		"\2\uffcc\2\uffd1\2\uffd4\2\uffd9\2\uffdc\2\uffde\2\2\3\r\3\17\3(\3*\3"+
		"<\3>\3?\3A\3O\3R\3_\3\u0082\3\u00fc\3\u0142\3\u0176\3\u0282\3\u029e\3"+
		"\u02a2\3\u02d2\3\u0302\3\u0321\3\u032f\3\u034c\3\u0352\3\u037c\3\u0382"+
		"\3\u039f\3\u03a2\3\u03c5\3\u03ca\3\u03d1\3\u03d3\3\u03d7\3\u0402\3\u049f"+
		"\3\u04a2\3\u04ab\3\u04b2\3\u04d5\3\u04da\3\u04fd\3\u0502\3\u0529\3\u0532"+
		"\3\u0565\3\u0602\3\u0738\3\u0742\3\u0757\3\u0762\3\u0769\3\u0802\3\u0807"+
		"\3\u080a\3\u080a\3\u080c\3\u0837\3\u0839\3\u083a\3\u083e\3\u083e\3\u0841"+
		"\3\u0857\3\u0862\3\u0878\3\u0882\3\u08a0\3\u08e2\3\u08f4\3\u08f6\3\u08f7"+
		"\3\u0902\3\u0917\3\u0922\3\u093b\3\u0982\3\u09b9\3\u09c0\3\u09c1\3\u0a02"+
		"\3\u0a05\3\u0a07\3\u0a08\3\u0a0e\3\u0a15\3\u0a17\3\u0a19\3\u0a1b\3\u0a35"+
		"\3\u0a62\3\u0a7e\3\u0a82\3\u0a9e\3\u0ac2\3\u0ac9\3\u0acb\3\u0ae6\3\u0b02"+
		"\3\u0b37\3\u0b42\3\u0b57\3\u0b62\3\u0b74\3\u0b82\3\u0b93\3\u0c02\3\u0c4a"+
		"\3\u0c82\3\u0cb4\3\u0cc2\3\u0cf4\3\u1002\3\u1047\3\u1068\3\u1071\3\u1084"+
		"\3\u10ba\3\u10d2\3\u10ea\3\u10f2\3\u10fb\3\u1102\3\u1134\3\u1138\3\u1141"+
		"\3\u1152\3\u1174\3\u1178\3\u1178\3\u1182\3\u11c1\3\u11c3\3\u11c6\3\u11d2"+
		"\3\u11dc\3\u11de\3\u11de\3\u1202\3\u1213\3\u1215\3\u1236\3\u1239\3\u1239"+
		"\3\u1240\3\u1240\3\u1282\3\u1288\3\u128a\3\u128a\3\u128c\3\u128f\3\u1291"+
		"\3\u129f\3\u12a1\3\u12aa\3\u12b2\3\u12ea\3\u12f2\3\u12fb\3\u1302\3\u1305"+
		"\3\u1307\3\u130e\3\u1311\3\u1312\3\u1315\3\u132a\3\u132c\3\u1332\3\u1334"+
		"\3\u1335\3\u1337\3\u133b\3\u133f\3\u1346\3\u1349\3\u134a\3\u134d\3\u134e"+
		"\3\u1352\3\u1352\3\u1359\3\u1359\3\u135f\3\u1365\3\u1402\3\u1443\3\u1445"+
		"\3\u1447\3\u1449\3\u144c\3\u1452\3\u145b\3\u1482\3\u14c3\3\u14c6\3\u14c7"+
		"\3\u14c9\3\u14c9\3\u14d2\3\u14db\3\u1582\3\u15b7\3\u15ba\3\u15c0\3\u15da"+
		"\3\u15df\3\u1602\3\u1640\3\u1642\3\u1642\3\u1646\3\u1646\3\u1652\3\u165b"+
		"\3\u1682\3\u16b7\3\u16c2\3\u16cb\3\u1702\3\u171b\3\u171f\3\u172c\3\u1732"+
		"\3\u173b\3\u18a2\3\u18eb\3\u1901\3\u1901\3\u1a02\3\u1a34\3\u1a37\3\u1a40"+
		"\3\u1a52\3\u1a85\3\u1a88\3\u1a99\3\u1ac2\3\u1afa\3\u1c02\3\u1c0a\3\u1c0c"+
		"\3\u1c38\3\u1c3a\3\u1c40\3\u1c42\3\u1c42\3\u1c52\3\u1c5b\3\u1c74\3\u1c91"+
		"\3\u1c94\3\u1ca9\3\u1cab\3\u1cb8\3\u1d02\3\u1d08\3\u1d0a\3\u1d0b\3\u1d0d"+
		"\3\u1d38\3\u1d3c\3\u1d3c\3\u1d3e\3\u1d3f\3\u1d41\3\u1d43\3\u1d45\3\u1d45"+
		"\3\u1d48\3\u1d49\3\u1d52\3\u1d5b\3\u2002\3\u239b\3\u2402\3\u2470\3\u2482"+
		"\3\u2545\3\u3002\3\u3430\3\u4402\3\u4648\3\u6802\3\u6a3a\3\u6a42\3\u6a60"+
		"\3\u6a62\3\u6a6b\3\u6ad2\3\u6aef\3\u6b02\3\u6b38\3\u6b42\3\u6b45\3\u6b52"+
		"\3\u6b5b\3\u6b65\3\u6b79\3\u6b7f\3\u6b91\3\u6f02\3\u6f46\3\u6f52\3\u6f80"+
		"\3\u6f95\3\u6fa1\3\u6fe2\3\u6fe3\3\u7002\3\u87ee\3\u8802\3\u8af4\3\ub002"+
		"\3\ub120\3\ub172\3\ub2fd\3\ubc02\3\ubc6c\3\ubc72\3\ubc7e\3\ubc82\3\ubc8a"+
		"\3\ubc92\3\ubc9b\3\ubca0\3\ubca0\3\ud402\3\ud456\3\ud458\3\ud49e\3\ud4a0"+
		"\3\ud4a1\3\ud4a4\3\ud4a4\3\ud4a7\3\ud4a8\3\ud4ab\3\ud4ae\3\ud4b0\3\ud4bb"+
		"\3\ud4bd\3\ud4bd\3\ud4bf\3\ud4c5\3\ud4c7\3\ud507\3\ud509\3\ud50c\3\ud50f"+
		"\3\ud516\3\ud518\3\ud51e\3\ud520\3\ud53b\3\ud53d\3\ud540\3\ud542\3\ud546"+
		"\3\ud548\3\ud548\3\ud54c\3\ud552\3\ud554\3\ud6a7\3\ud6aa\3\ud6c2\3\ud6c4"+
		"\3\ud6dc\3\ud6de\3\ud6fc\3\ud6fe\3\ud716\3\ud718\3\ud736\3\ud738\3\ud750"+
		"\3\ud752\3\ud770\3\ud772\3\ud78a\3\ud78c\3\ud7aa\3\ud7ac\3\ud7c4\3\ud7c6"+
		"\3\ud7cd\3\ud7d0\3\ud801\3\ue002\3\ue008\3\ue00a\3\ue01a\3\ue01d\3\ue023"+
		"\3\ue025\3\ue026\3\ue028\3\ue02c\3\ue802\3\ue8c6\3\ue902\3\ue945\3\ue949"+
		"\3\ue949\3\ue952\3\ue95b\3\uee02\3\uee05\3\uee07\3\uee21\3\uee23\3\uee24"+
		"\3\uee26\3\uee26\3\uee29\3\uee29\3\uee2b\3\uee34\3\uee36\3\uee39\3\uee3b"+
		"\3\uee3b\3\uee3d\3\uee3d\3\uee44\3\uee44\3\uee49\3\uee49\3\uee4b\3\uee4b"+
		"\3\uee4d\3\uee4d\3\uee4f\3\uee51\3\uee53\3\uee54\3\uee56\3\uee56\3\uee59"+
		"\3\uee59\3\uee5b\3\uee5b\3\uee5d\3\uee5d\3\uee5f\3\uee5f\3\uee61\3\uee61"+
		"\3\uee63\3\uee64\3\uee66\3\uee66\3\uee69\3\uee6c\3\uee6e\3\uee74\3\uee76"+
		"\3\uee79\3\uee7b\3\uee7e\3\uee80\3\uee80\3\uee82\3\uee8b\3\uee8d\3\uee9d"+
		"\3\ueea3\3\ueea5\3\ueea7\3\ueeab\3\ueead\3\ueebd\3\uf132\3\uf14b\3\uf152"+
		"\3\uf16b\3\uf172\3\uf18b\3\2\4\ua6d8\4\ua702\4\ub736\4\ub742\4\ub81f\4"+
		"\ub822\4\ucea3\4\uceb2\4\uebe2\4\uf802\4\ufa1f\4\u037d\2\3\3\2\2\2\2\5"+
		"\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2"+
		"\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33"+
		"\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2"+
		"\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2"+
		"\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2"+
		"\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K"+
		"\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2"+
		"\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2"+
		"\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2q"+
		"\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3\2"+
		"\2\2\2\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3\2\2\2\2\u0085\3\2\2\2\2\u0089"+
		"\3\2\2\2\3\u00a3\3\2\2\2\5\u00b4\3\2\2\2\7\u00b6\3\2\2\2\t\u00ba\3\2\2"+
		"\2\13\u00bd\3\2\2\2\r\u00c1\3\2\2\2\17\u00c5\3\2\2\2\21\u00c8\3\2\2\2"+
		"\23\u00cb\3\2\2\2\25\u00ce\3\2\2\2\27\u00d1\3\2\2\2\31\u00d3\3\2\2\2\33"+
		"\u00d5\3\2\2\2\35\u00d7\3\2\2\2\37\u00db\3\2\2\2!\u00de\3\2\2\2#\u00e0"+
		"\3\2\2\2%\u00e2\3\2\2\2\'\u00e4\3\2\2\2)\u00e7\3\2\2\2+\u00e9\3\2\2\2"+
		"-\u00ec\3\2\2\2/\u00ee\3\2\2\2\61\u00f1\3\2\2\2\63\u00f4\3\2\2\2\65\u00f7"+
		"\3\2\2\2\67\u00fa\3\2\2\29\u00ff\3\2\2\2;\u0102\3\2\2\2=\u0108\3\2\2\2"+
		"?\u010a\3\2\2\2A\u010c\3\2\2\2C\u010e\3\2\2\2E\u0110\3\2\2\2G\u0112\3"+
		"\2\2\2I\u0114\3\2\2\2K\u0116\3\2\2\2M\u0118\3\2\2\2O\u011a\3\2\2\2Q\u011c"+
		"\3\2\2\2S\u0125\3\2\2\2U\u012a\3\2\2\2W\u012d\3\2\2\2Y\u0139\3\2\2\2["+
		"\u0142\3\2\2\2]\u0150\3\2\2\2_\u015c\3\2\2\2a\u0161\3\2\2\2c\u0167\3\2"+
		"\2\2e\u016b\3\2\2\2g\u016f\3\2\2\2i\u0175\3\2\2\2k\u0179\3\2\2\2m\u0180"+
		"\3\2\2\2o\u0183\3\2\2\2q\u0186\3\2\2\2s\u018b\3\2\2\2u\u0190\3\2\2\2w"+
		"\u0196\3\2\2\2y\u019a\3\2\2\2{\u01c3\3\2\2\2}\u01fa\3\2\2\2\177\u022a"+
		"\3\2\2\2\u0081\u022c\3\2\2\2\u0083\u0257\3\2\2\2\u0085\u025b\3\2\2\2\u0087"+
		"\u0268\3\2\2\2\u0089\u027f\3\2\2\2\u008b\u0283\3\2\2\2\u008d\u02e4\3\2"+
		"\2\2\u008f\u02e6\3\2\2\2\u0091\u02ef\3\2\2\2\u0093\u02fa\3\2\2\2\u0095"+
		"\u02fc\3\2\2\2\u0097\u030a\3\2\2\2\u0099\u030c\3\2\2\2\u009b\u0313\3\2"+
		"\2\2\u009d\u031a\3\2\2\2\u009f\u031c\3\2\2\2\u00a1\u0327\3\2\2\2\u00a3"+
		"\u00a7\7%\2\2\u00a4\u00a6\n\2\2\2\u00a5\u00a4\3\2\2\2\u00a6\u00a9\3\2"+
		"\2\2\u00a7\u00a5\3\2\2\2\u00a7\u00a8\3\2\2\2\u00a8\u00ac\3\2\2\2\u00a9"+
		"\u00a7\3\2\2\2\u00aa\u00ad\5\u008dG\2\u00ab\u00ad\7\2\2\3\u00ac\u00aa"+
		"\3\2\2\2\u00ac\u00ab\3\2\2\2\u00ad\u00ae\3\2\2\2\u00ae\u00af\b\2\2\2\u00af"+
		"\4\3\2\2\2\u00b0\u00b1\7>\2\2\u00b1\u00b5\7/\2\2\u00b2\u00b3\7<\2\2\u00b3"+
		"\u00b5\7?\2\2\u00b4\u00b0\3\2\2\2\u00b4\u00b2\3\2\2\2\u00b5\6\3\2\2\2"+
		"\u00b6\u00b7\7>\2\2\u00b7\u00b8\7>\2\2\u00b8\u00b9\7/\2\2\u00b9\b\3\2"+
		"\2\2\u00ba\u00bb\7/\2\2\u00bb\u00bc\7@\2\2\u00bc\n\3\2\2\2\u00bd\u00be"+
		"\7/\2\2\u00be\u00bf\7@\2\2\u00bf\u00c0\7@\2\2\u00c0\f\3\2\2\2\u00c1\u00c2"+
		"\7\60\2\2\u00c2\u00c3\7\60\2\2\u00c3\u00c4\7\60\2\2\u00c4\16\3\2\2\2\u00c5"+
		"\u00c6\7?\2\2\u00c6\u00c7\7?\2\2\u00c7\20\3\2\2\2\u00c8\u00c9\7#\2\2\u00c9"+
		"\u00ca\7?\2\2\u00ca\22\3\2\2\2\u00cb\u00cc\7@\2\2\u00cc\u00cd\7?\2\2\u00cd"+
		"\24\3\2\2\2\u00ce\u00cf\7>\2\2\u00cf\u00d0\7?\2\2\u00d0\26\3\2\2\2\u00d1"+
		"\u00d2\7@\2\2\u00d2\30\3\2\2\2\u00d3\u00d4\7>\2\2\u00d4\32\3\2\2\2\u00d5"+
		"\u00d6\7?\2\2\u00d6\34\3\2\2\2\u00d7\u00d8\7<\2\2\u00d8\u00d9\7<\2\2\u00d9"+
		"\u00da\7<\2\2\u00da\36\3\2\2\2\u00db\u00dc\7<\2\2\u00dc\u00dd\7<\2\2\u00dd"+
		" \3\2\2\2\u00de\u00df\7<\2\2\u00df\"\3\2\2\2\u00e0\u00e1\7=\2\2\u00e1"+
		"$\3\2\2\2\u00e2\u00e3\7.\2\2\u00e3&\3\2\2\2\u00e4\u00e5\7(\2\2\u00e5\u00e6"+
		"\7(\2\2\u00e6(\3\2\2\2\u00e7\u00e8\7(\2\2\u00e8*\3\2\2\2\u00e9\u00ea\7"+
		"~\2\2\u00ea\u00eb\7~\2\2\u00eb,\3\2\2\2\u00ec\u00ed\7~\2\2\u00ed.\3\2"+
		"\2\2\u00ee\u00ef\7}\2\2\u00ef\u00f0\b\30\3\2\u00f0\60\3\2\2\2\u00f1\u00f2"+
		"\7\177\2\2\u00f2\u00f3\b\31\4\2\u00f3\62\3\2\2\2\u00f4\u00f5\7*\2\2\u00f5"+
		"\u00f6\b\32\5\2\u00f6\64\3\2\2\2\u00f7\u00f8\7+\2\2\u00f8\u00f9\b\33\6"+
		"\2\u00f9\66\3\2\2\2\u00fa\u00fb\7]\2\2\u00fb\u00fc\7]\2\2\u00fc\u00fd"+
		"\3\2\2\2\u00fd\u00fe\b\34\7\2\u00fe8\3\2\2\2\u00ff\u0100\7]\2\2\u0100"+
		"\u0101\b\35\b\2\u0101:\3\2\2\2\u0102\u0103\7_\2\2\u0103\u0104\b\36\t\2"+
		"\u0104<\3\2\2\2\u0105\u0109\7`\2\2\u0106\u0107\7,\2\2\u0107\u0109\7,\2"+
		"\2\u0108\u0105\3\2\2\2\u0108\u0106\3\2\2\2\u0109>\3\2\2\2\u010a\u010b"+
		"\7\u0080\2\2\u010b@\3\2\2\2\u010c\u010d\7#\2\2\u010dB\3\2\2\2\u010e\u010f"+
		"\7A\2\2\u010fD\3\2\2\2\u0110\u0111\7-\2\2\u0111F\3\2\2\2\u0112\u0113\7"+
		",\2\2\u0113H\3\2\2\2\u0114\u0115\7\61\2\2\u0115J\3\2\2\2\u0116\u0117\7"+
		"/\2\2\u0117L\3\2\2\2\u0118\u0119\7&\2\2\u0119N\3\2\2\2\u011a\u011b\7B"+
		"\2\2\u011bP\3\2\2\2\u011c\u011d\7h\2\2\u011d\u011e\7w\2\2\u011e\u011f"+
		"\7p\2\2\u011f\u0120\7e\2\2\u0120\u0121\7v\2\2\u0121\u0122\7k\2\2\u0122"+
		"\u0123\7q\2\2\u0123\u0124\7p\2\2\u0124R\3\2\2\2\u0125\u0126\7P\2\2\u0126"+
		"\u0127\7W\2\2\u0127\u0128\7N\2\2\u0128\u0129\7N\2\2\u0129T\3\2\2\2\u012a"+
		"\u012b\7P\2\2\u012b\u012c\7C\2\2\u012cV\3\2\2\2\u012d\u012e\7P\2\2\u012e"+
		"\u012f\7C\2\2\u012f\u0130\7a\2\2\u0130\u0131\7k\2\2\u0131\u0132\7p\2\2"+
		"\u0132\u0133\7v\2\2\u0133\u0134\7g\2\2\u0134\u0135\7i\2\2\u0135\u0136"+
		"\7g\2\2\u0136\u0137\7t\2\2\u0137\u0138\7a\2\2\u0138X\3\2\2\2\u0139\u013a"+
		"\7P\2\2\u013a\u013b\7C\2\2\u013b\u013c\7a\2\2\u013c\u013d\7t\2\2\u013d"+
		"\u013e\7g\2\2\u013e\u013f\7c\2\2\u013f\u0140\7n\2\2\u0140\u0141\7a\2\2"+
		"\u0141Z\3\2\2\2\u0142\u0143\7P\2\2\u0143\u0144\7C\2\2\u0144\u0145\7a\2"+
		"\2\u0145\u0146\7e\2\2\u0146\u0147\7j\2\2\u0147\u0148\7c\2\2\u0148\u0149"+
		"\7t\2\2\u0149\u014a\7c\2\2\u014a\u014b\7e\2\2\u014b\u014c\7v\2\2\u014c"+
		"\u014d\7g\2\2\u014d\u014e\7t\2\2\u014e\u014f\7a\2\2\u014f\\\3\2\2\2\u0150"+
		"\u0151\7P\2\2\u0151\u0152\7C\2\2\u0152\u0153\7a\2\2\u0153\u0154\7e\2\2"+
		"\u0154\u0155\7q\2\2\u0155\u0156\7o\2\2\u0156\u0157\7r\2\2\u0157\u0158"+
		"\7n\2\2\u0158\u0159\7g\2\2\u0159\u015a\7z\2\2\u015a\u015b\7a\2\2\u015b"+
		"^\3\2\2\2\u015c\u015d\7V\2\2\u015d\u015e\7T\2\2\u015e\u015f\7W\2\2\u015f"+
		"\u0160\7G\2\2\u0160`\3\2\2\2\u0161\u0162\7H\2\2\u0162\u0163\7C\2\2\u0163"+
		"\u0164\7N\2\2\u0164\u0165\7U\2\2\u0165\u0166\7G\2\2\u0166b\3\2\2\2\u0167"+
		"\u0168\7K\2\2\u0168\u0169\7p\2\2\u0169\u016a\7h\2\2\u016ad\3\2\2\2\u016b"+
		"\u016c\7P\2\2\u016c\u016d\7c\2\2\u016d\u016e\7P\2\2\u016ef\3\2\2\2\u016f"+
		"\u0170\7y\2\2\u0170\u0171\7j\2\2\u0171\u0172\7k\2\2\u0172\u0173\7n\2\2"+
		"\u0173\u0174\7g\2\2\u0174h\3\2\2\2\u0175\u0176\7h\2\2\u0176\u0177\7q\2"+
		"\2\u0177\u0178\7t\2\2\u0178j\3\2\2\2\u0179\u017a\7t\2\2\u017a\u017b\7"+
		"g\2\2\u017b\u017c\7r\2\2\u017c\u017d\7g\2\2\u017d\u017e\7c\2\2\u017e\u017f"+
		"\7v\2\2\u017fl\3\2\2\2\u0180\u0181\7k\2\2\u0181\u0182\7p\2\2\u0182n\3"+
		"\2\2\2\u0183\u0184\7k\2\2\u0184\u0185\7h\2\2\u0185p\3\2\2\2\u0186\u0187"+
		"\7g\2\2\u0187\u0188\7n\2\2\u0188\u0189\7u\2\2\u0189\u018a\7g\2\2\u018a"+
		"r\3\2\2\2\u018b\u018c\7p\2\2\u018c\u018d\7g\2\2\u018d\u018e\7z\2\2\u018e"+
		"\u018f\7v\2\2\u018ft\3\2\2\2\u0190\u0191\7d\2\2\u0191\u0192\7t\2\2\u0192"+
		"\u0193\7g\2\2\u0193\u0194\7c\2\2\u0194\u0195\7m\2\2\u0195v\3\2\2\2\u0196"+
		"\u0197\t\3\2\2\u0197\u0198\3\2\2\2\u0198\u0199\b<\n\2\u0199x\3\2\2\2\u019a"+
		"\u019b\5\u008dG\2\u019b\u019c\b=\13\2\u019cz\3\2\2\2\u019d\u019f\4\62"+
		";\2\u019e\u019d\3\2\2\2\u019f\u01a0\3\2\2\2\u01a0\u019e\3\2\2\2\u01a0"+
		"\u01a1\3\2\2\2\u01a1\u01a2\3\2\2\2\u01a2\u01a6\7\60\2\2\u01a3\u01a5\4"+
		"\62;\2\u01a4\u01a3\3\2\2\2\u01a5\u01a8\3\2\2\2\u01a6\u01a4\3\2\2\2\u01a6"+
		"\u01a7\3\2\2\2\u01a7\u01a9\3\2\2\2\u01a8\u01a6\3\2\2\2\u01a9\u01aa\7N"+
		"\2\2\u01aa\u01c4\b>\f\2\u01ab\u01ad\7\60\2\2\u01ac\u01ab\3\2\2\2\u01ac"+
		"\u01ad\3\2\2\2\u01ad\u01af\3\2\2\2\u01ae\u01b0\4\62;\2\u01af\u01ae\3\2"+
		"\2\2\u01b0\u01b1\3\2\2\2\u01b1\u01af\3\2\2\2\u01b1\u01b2\3\2\2\2\u01b2"+
		"\u01b4\3\2\2\2\u01b3\u01b5\5\u008fH\2\u01b4\u01b3\3\2\2\2\u01b4\u01b5"+
		"\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6\u01b7\7N\2\2\u01b7\u01c4\b>\r\2\u01b8"+
		"\u01b9\7\62\2\2\u01b9\u01ba\7z\2\2\u01ba\u01bc\3\2\2\2\u01bb\u01bd\5\u009d"+
		"O\2\u01bc\u01bb\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\u01bc\3\2\2\2\u01be"+
		"\u01bf\3\2\2\2\u01bf\u01c0\3\2\2\2\u01c0\u01c1\7N\2\2\u01c1\u01c2\b>\16"+
		"\2\u01c2\u01c4\3\2\2\2\u01c3\u019e\3\2\2\2\u01c3\u01ac\3\2\2\2\u01c3\u01b8"+
		"\3\2\2\2\u01c4|\3\2\2\2\u01c5\u01c7\4\62;\2\u01c6\u01c5\3\2\2\2\u01c7"+
		"\u01c8\3\2\2\2\u01c8\u01c6\3\2\2\2\u01c8\u01c9\3\2\2\2\u01c9\u01ca\3\2"+
		"\2\2\u01ca\u01ce\7\60\2\2\u01cb\u01cd\4\62;\2\u01cc\u01cb\3\2\2\2\u01cd"+
		"\u01d0\3\2\2\2\u01ce\u01cc\3\2\2\2\u01ce\u01cf\3\2\2\2\u01cf\u01d2\3\2"+
		"\2\2\u01d0\u01ce\3\2\2\2\u01d1\u01d3\5\u008fH\2\u01d2\u01d1\3\2\2\2\u01d2"+
		"\u01d3\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d5\7k\2\2\u01d5\u01fb\b?\17"+
		"\2\u01d6\u01d8\7\60\2\2\u01d7\u01d6\3\2\2\2\u01d7\u01d8\3\2\2\2\u01d8"+
		"\u01da\3\2\2\2\u01d9\u01db\4\62;\2\u01da\u01d9\3\2\2\2\u01db\u01dc\3\2"+
		"\2\2\u01dc\u01da\3\2\2\2\u01dc\u01dd\3\2\2\2\u01dd\u01df\3\2\2\2\u01de"+
		"\u01e0\5\u008fH\2\u01df\u01de\3\2\2\2\u01df\u01e0\3\2\2\2\u01e0\u01e1"+
		"\3\2\2\2\u01e1\u01e2\7k\2\2\u01e2\u01fb\b?\20\2\u01e3\u01e4\7\62\2\2\u01e4"+
		"\u01e5\7z\2\2\u01e5\u01e7\3\2\2\2\u01e6\u01e8\5\u009dO\2\u01e7\u01e6\3"+
		"\2\2\2\u01e8\u01e9\3\2\2\2\u01e9\u01e7\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea"+
		"\u01f5\3\2\2\2\u01eb\u01ed\7\60\2\2\u01ec\u01eb\3\2\2\2\u01ec\u01ed\3"+
		"\2\2\2\u01ed\u01f1\3\2\2\2\u01ee\u01f0\5\u009dO\2\u01ef\u01ee\3\2\2\2"+
		"\u01f0\u01f3\3\2\2\2\u01f1\u01ef\3\2\2\2\u01f1\u01f2\3\2\2\2\u01f2\u01f4"+
		"\3\2\2\2\u01f3\u01f1\3\2\2\2\u01f4\u01f6\5\u0091I\2\u01f5\u01ec\3\2\2"+
		"\2\u01f5\u01f6\3\2\2\2\u01f6\u01f7\3\2\2\2\u01f7\u01f8\7k\2\2\u01f8\u01f9"+
		"\b?\21\2\u01f9\u01fb\3\2\2\2\u01fa\u01c6\3\2\2\2\u01fa\u01d7\3\2\2\2\u01fa"+
		"\u01e3\3\2\2\2\u01fb~\3\2\2\2\u01fc\u01fe\4\62;\2\u01fd\u01fc\3\2\2\2"+
		"\u01fe\u01ff\3\2\2\2\u01ff\u01fd\3\2\2\2\u01ff\u0200\3\2\2\2\u0200\u0201"+
		"\3\2\2\2\u0201\u0205\7\60\2\2\u0202\u0204\4\62;\2\u0203\u0202\3\2\2\2"+
		"\u0204\u0207\3\2\2\2\u0205\u0203\3\2\2\2\u0205\u0206\3\2\2\2\u0206\u0209"+
		"\3\2\2\2\u0207\u0205\3\2\2\2\u0208\u020a\5\u008fH\2\u0209\u0208\3\2\2"+
		"\2\u0209\u020a\3\2\2\2\u020a\u022b\3\2\2\2\u020b\u020d\7\60\2\2\u020c"+
		"\u020b\3\2\2\2\u020c\u020d\3\2\2\2\u020d\u020f\3\2\2\2\u020e\u0210\4\62"+
		";\2\u020f\u020e\3\2\2\2\u0210\u0211\3\2\2\2\u0211\u020f\3\2\2\2\u0211"+
		"\u0212\3\2\2\2\u0212\u0214\3\2\2\2\u0213\u0215\5\u008fH\2\u0214\u0213"+
		"\3\2\2\2\u0214\u0215\3\2\2\2\u0215\u022b\3\2\2\2\u0216\u0217\7\62\2\2"+
		"\u0217\u0218\7z\2\2\u0218\u021a\3\2\2\2\u0219\u021b\5\u009dO\2\u021a\u0219"+
		"\3\2\2\2\u021b\u021c\3\2\2\2\u021c\u021a\3\2\2\2\u021c\u021d\3\2\2\2\u021d"+
		"\u0228\3\2\2\2\u021e\u0220\7\60\2\2\u021f\u021e\3\2\2\2\u021f\u0220\3"+
		"\2\2\2\u0220\u0224\3\2\2\2\u0221\u0223\5\u009dO\2\u0222\u0221\3\2\2\2"+
		"\u0223\u0226\3\2\2\2\u0224\u0222\3\2\2\2\u0224\u0225\3\2\2\2\u0225\u0227"+
		"\3\2\2\2\u0226\u0224\3\2\2\2\u0227\u0229\5\u0091I\2\u0228\u021f\3\2\2"+
		"\2\u0228\u0229\3\2\2\2\u0229\u022b\3\2\2\2\u022a\u01fd\3\2\2\2\u022a\u020c"+
		"\3\2\2\2\u022a\u0216\3\2\2\2\u022b\u0080\3\2\2\2\u022c\u022d\7\60\2\2"+
		"\u022d\u022e\7\60\2\2\u022e\u0230\3\2\2\2\u022f\u0231\4\62;\2\u0230\u022f"+
		"\3\2\2\2\u0231\u0232\3\2\2\2\u0232\u0230\3\2\2\2\u0232\u0233\3\2\2\2\u0233"+
		"\u0082\3\2\2\2\u0234\u0236\7\60\2\2\u0235\u0234\3\2\2\2\u0236\u0239\3"+
		"\2\2\2\u0237\u0235\3\2\2\2\u0237\u0238\3\2\2\2\u0238\u023a\3\2\2\2\u0239"+
		"\u0237\3\2\2\2\u023a\u0258\5\u0095K\2\u023b\u023d\7\60\2\2\u023c\u023e"+
		"\7\60\2\2\u023d\u023c\3\2\2\2\u023e\u023f\3\2\2\2\u023f\u023d\3\2\2\2"+
		"\u023f\u0240\3\2\2\2\u0240\u0244\3\2\2\2\u0241\u0243\4\62;\2\u0242\u0241"+
		"\3\2\2\2\u0243\u0246\3\2\2\2\u0244\u0242\3\2\2\2\u0244\u0245\3\2\2\2\u0245"+
		"\u0247\3\2\2\2\u0246\u0244\3\2\2\2\u0247\u0258\5\u0095K\2\u0248\u0258"+
		"\7\60\2\2\u0249\u024a\7\60\2\2\u024a\u0258\7\60\2\2\u024b\u024c\7\60\2"+
		"\2\u024c\u024d\7\60\2\2\u024d\u024e\7\60\2\2\u024e\u0252\7\60\2\2\u024f"+
		"\u0251\7\60\2\2\u0250\u024f\3\2\2\2\u0251\u0254\3\2\2\2\u0252\u0250\3"+
		"\2\2\2\u0252\u0253\3\2\2\2\u0253\u0258\3\2\2\2\u0254\u0252\3\2\2\2\u0255"+
		"\u0256\7b\2\2\u0256\u0258\5\u0087D\2\u0257\u0237\3\2\2\2\u0257\u023b\3"+
		"\2\2\2\u0257\u0248\3\2\2\2\u0257\u0249\3\2\2\2\u0257\u024b\3\2\2\2\u0257"+
		"\u0255\3\2\2\2\u0258\u0259\3\2\2\2\u0259\u025a\bB\22\2\u025a\u0084\3\2"+
		"\2\2\u025b\u025f\7\'\2\2\u025c\u025e\n\4\2\2\u025d\u025c\3\2\2\2\u025e"+
		"\u0261\3\2\2\2\u025f\u025d\3\2\2\2\u025f\u0260\3\2\2\2\u0260\u0262\3\2"+
		"\2\2\u0261\u025f\3\2\2\2\u0262\u0263\7\'\2\2\u0263\u0086\3\2\2\2\u0264"+
		"\u0267\5\u008bF\2\u0265\u0267\n\5\2\2\u0266\u0264\3\2\2\2\u0266\u0265"+
		"\3\2\2\2\u0267\u026a\3\2\2\2\u0268\u0266\3\2\2\2\u0268\u0269\3\2\2\2\u0269"+
		"\u026b\3\2\2\2\u026a\u0268\3\2\2\2\u026b\u026c\7b\2\2\u026c\u0088\3\2"+
		"\2\2\u026d\u0272\7$\2\2\u026e\u0271\5\u008bF\2\u026f\u0271\n\6\2\2\u0270"+
		"\u026e\3\2\2\2\u0270\u026f\3\2\2\2\u0271\u0274\3\2\2\2\u0272\u0270\3\2"+
		"\2\2\u0272\u0273\3\2\2\2\u0273\u0275\3\2\2\2\u0274\u0272\3\2\2\2\u0275"+
		"\u0280\7$\2\2\u0276\u027b\7)\2\2\u0277\u027a\5\u008bF\2\u0278\u027a\n"+
		"\7\2\2\u0279\u0277\3\2\2\2\u0279\u0278\3\2\2\2\u027a\u027d\3\2\2\2\u027b"+
		"\u0279\3\2\2\2\u027b\u027c\3\2\2\2\u027c\u027e\3\2\2\2\u027d\u027b\3\2"+
		"\2\2\u027e\u0280\7)\2\2\u027f\u026d\3\2\2\2\u027f\u0276\3\2\2\2\u0280"+
		"\u0281\3\2\2\2\u0281\u0282\bE\23\2\u0282\u008a\3\2\2\2\u0283\u02da\7^"+
		"\2\2\u0284\u02db\t\b\2\2\u0285\u0287\5\u009fP\2\u0286\u0288\5\u009fP\2"+
		"\u0287\u0286\3\2\2\2\u0287\u0288\3\2\2\2\u0288\u028a\3\2\2\2\u0289\u028b"+
		"\5\u009fP\2\u028a\u0289\3\2\2\2\u028a\u028b\3\2\2\2\u028b\u02db\3\2\2"+
		"\2\u028c\u028d\7z\2\2\u028d\u028e\5\u009dO\2\u028e\u028f\5\u009dO\2\u028f"+
		"\u02db\3\2\2\2\u0290\u0291\7w\2\2\u0291\u0293\5\u009dO\2\u0292\u0294\5"+
		"\u009dO\2\u0293\u0292\3\2\2\2\u0293\u0294\3\2\2\2\u0294\u0296\3\2\2\2"+
		"\u0295\u0297\5\u009dO\2\u0296\u0295\3\2\2\2\u0296\u0297\3\2\2\2\u0297"+
		"\u0299\3\2\2\2\u0298\u029a\5\u009dO\2\u0299\u0298\3\2\2\2\u0299\u029a"+
		"\3\2\2\2\u029a\u02db\3\2\2\2\u029b\u029c\7W\2\2\u029c\u029e\5\u009dO\2"+
		"\u029d\u029f\5\u009dO\2\u029e\u029d\3\2\2\2\u029e\u029f\3\2\2\2\u029f"+
		"\u02a1\3\2\2\2\u02a0\u02a2\5\u009dO\2\u02a1\u02a0\3\2\2\2\u02a1\u02a2"+
		"\3\2\2\2\u02a2\u02a4\3\2\2\2\u02a3\u02a5\5\u009dO\2\u02a4\u02a3\3\2\2"+
		"\2\u02a4\u02a5\3\2\2\2\u02a5\u02a7\3\2\2\2\u02a6\u02a8\5\u009dO\2\u02a7"+
		"\u02a6\3\2\2\2\u02a7\u02a8\3\2\2\2\u02a8\u02aa\3\2\2\2\u02a9\u02ab\5\u009d"+
		"O\2\u02aa\u02a9\3\2\2\2\u02aa\u02ab\3\2\2\2\u02ab\u02ad\3\2\2\2\u02ac"+
		"\u02ae\5\u009dO\2\u02ad\u02ac\3\2\2\2\u02ad\u02ae\3\2\2\2\u02ae\u02b0"+
		"\3\2\2\2\u02af\u02b1\5\u009dO\2\u02b0\u02af\3\2\2\2\u02b0\u02b1\3\2\2"+
		"\2\u02b1\u02db\3\2\2\2\u02b2\u02b3\7w\2\2\u02b3\u02b4\7}\2\2\u02b4\u02b6"+
		"\5\u009dO\2\u02b5\u02b7\5\u009dO\2\u02b6\u02b5\3\2\2\2\u02b6\u02b7\3\2"+
		"\2\2\u02b7\u02b9\3\2\2\2\u02b8\u02ba\5\u009dO\2\u02b9\u02b8\3\2\2\2\u02b9"+
		"\u02ba\3\2\2\2\u02ba\u02bc\3\2\2\2\u02bb\u02bd\5\u009dO\2\u02bc\u02bb"+
		"\3\2\2\2\u02bc\u02bd\3\2\2\2\u02bd\u02be\3\2\2\2\u02be\u02bf\7\177\2\2"+
		"\u02bf\u02db\3\2\2\2\u02c0\u02c1\7W\2\2\u02c1\u02c2\7}\2\2\u02c2\u02c4"+
		"\5\u009dO\2\u02c3\u02c5\5\u009dO\2\u02c4\u02c3\3\2\2\2\u02c4\u02c5\3\2"+
		"\2\2\u02c5\u02c7\3\2\2\2\u02c6\u02c8\5\u009dO\2\u02c7\u02c6\3\2\2\2\u02c7"+
		"\u02c8\3\2\2\2\u02c8\u02ca\3\2\2\2\u02c9\u02cb\5\u009dO\2\u02ca\u02c9"+
		"\3\2\2\2\u02ca\u02cb\3\2\2\2\u02cb\u02cd\3\2\2\2\u02cc\u02ce\5\u009dO"+
		"\2\u02cd\u02cc\3\2\2\2\u02cd\u02ce\3\2\2\2\u02ce\u02d0\3\2\2\2\u02cf\u02d1"+
		"\5\u009dO\2\u02d0\u02cf\3\2\2\2\u02d0\u02d1\3\2\2\2\u02d1\u02d3\3\2\2"+
		"\2\u02d2\u02d4\5\u009dO\2\u02d3\u02d2\3\2\2\2\u02d3\u02d4\3\2\2\2\u02d4"+
		"\u02d6\3\2\2\2\u02d5\u02d7\5\u009dO\2\u02d6\u02d5\3\2\2\2\u02d6\u02d7"+
		"\3\2\2\2\u02d7\u02d8\3\2\2\2\u02d8\u02d9\7\177\2\2\u02d9\u02db\3\2\2\2"+
		"\u02da\u0284\3\2\2\2\u02da\u0285\3\2\2\2\u02da\u028c\3\2\2\2\u02da\u0290"+
		"\3\2\2\2\u02da\u029b\3\2\2\2\u02da\u02b2\3\2\2\2\u02da\u02c0\3\2\2\2\u02db"+
		"\u008c\3\2\2\2\u02dc\u02de\4\16\17\2\u02dd\u02dc\3\2\2\2\u02dd\u02de\3"+
		"\2\2\2\u02de\u02df\3\2\2\2\u02df\u02e5\7\f\2\2\u02e0\u02e2\7\f\2\2\u02e1"+
		"\u02e0\3\2\2\2\u02e1\u02e2\3\2\2\2\u02e2\u02e3\3\2\2\2\u02e3\u02e5\4\16"+
		"\17\2\u02e4\u02dd\3\2\2\2\u02e4\u02e1\3\2\2\2\u02e5\u008e\3\2\2\2\u02e6"+
		"\u02e8\t\t\2\2\u02e7\u02e9\t\n\2\2\u02e8\u02e7\3\2\2\2\u02e8\u02e9\3\2"+
		"\2\2\u02e9\u02eb\3\2\2\2\u02ea\u02ec\4\62;\2\u02eb\u02ea\3\2\2\2\u02ec"+
		"\u02ed\3\2\2\2\u02ed\u02eb\3\2\2\2\u02ed\u02ee\3\2\2\2\u02ee\u0090\3\2"+
		"\2\2\u02ef\u02f1\t\13\2\2\u02f0\u02f2\t\n\2\2\u02f1\u02f0\3\2\2\2\u02f1"+
		"\u02f2\3\2\2\2\u02f2\u02f4\3\2\2\2\u02f3\u02f5\4\62;\2\u02f4\u02f3\3\2"+
		"\2\2\u02f5\u02f6\3\2\2\2\u02f6\u02f4\3\2\2\2\u02f6\u02f7\3\2\2\2\u02f7"+
		"\u0092\3\2\2\2\u02f8\u02fb\5\u0095K\2\u02f9\u02fb\t\f\2\2\u02fa\u02f8"+
		"\3\2\2\2\u02fa\u02f9\3\2\2\2\u02fb\u0094\3\2\2\2\u02fc\u0300\t\17\2\2"+
		"\u02fd\u02ff\t\20\2\2\u02fe\u02fd\3\2\2\2\u02ff\u0302\3\2\2\2\u0300\u02fe"+
		"\3\2\2\2\u0300\u0301\3\2\2\2\u0301\u0096\3\2\2\2\u0302\u0300\3\2\2\2\u0303"+
		"\u0304\7^\2\2\u0304\u030b\t\r\2\2\u0305\u0306\7^\2\2\u0306\u030b\5\u008d"+
		"G\2\u0307\u030b\5\u0099M\2\u0308\u030b\5\u00a1Q\2\u0309\u030b\5\u009b"+
		"N\2\u030a\u0303\3\2\2\2\u030a\u0305\3\2\2\2\u030a\u0307\3\2\2\2\u030a"+
		"\u0308\3\2\2\2\u030a\u0309\3\2\2\2\u030b\u0098\3\2\2\2\u030c\u030d\7^"+
		"\2\2\u030d\u030e\7w\2\2\u030e\u030f\5\u009dO\2\u030f\u0310\5\u009dO\2"+
		"\u0310\u0311\5\u009dO\2\u0311\u0312\5\u009dO\2\u0312\u009a\3\2\2\2\u0313"+
		"\u0314\7^\2\2\u0314\u0315\7z\2\2\u0315\u0316\3\2\2\2\u0316\u0318\5\u009d"+
		"O\2\u0317\u0319\5\u009dO\2\u0318\u0317\3\2\2\2\u0318\u0319\3\2\2\2\u0319"+
		"\u009c\3\2\2\2\u031a\u031b\t\16\2\2\u031b\u009e\3\2\2\2\u031c\u031d\4"+
		"\629\2\u031d\u00a0\3\2\2\2\u031e\u031f\7^\2\2\u031f\u0320\4\62\65\2\u0320"+
		"\u0321\4\629\2\u0321\u0328\4\629\2\u0322\u0323\7^\2\2\u0323\u0324\4\62"+
		"9\2\u0324\u0328\4\629\2\u0325\u0326\7^\2\2\u0326\u0328\4\629\2\u0327\u031e"+
		"\3\2\2\2\u0327\u0322\3\2\2\2\u0327\u0325\3\2\2\2\u0328\u00a2\3\2\2\2U"+
		"\2\u00a7\u00ac\u00b4\u0108\u01a0\u01a6\u01ac\u01b1\u01b4\u01be\u01c3\u01c8"+
		"\u01ce\u01d2\u01d7\u01dc\u01df\u01e9\u01ec\u01f1\u01f5\u01fa\u01ff\u0205"+
		"\u0209\u020c\u0211\u0214\u021c\u021f\u0224\u0228\u022a\u0232\u0237\u023f"+
		"\u0244\u0252\u0257\u025f\u0266\u0268\u0270\u0272\u0279\u027b\u027f\u0287"+
		"\u028a\u0293\u0296\u0299\u029e\u02a1\u02a4\u02a7\u02aa\u02ad\u02b0\u02b6"+
		"\u02b9\u02bc\u02c4\u02c7\u02ca\u02cd\u02d0\u02d3\u02d6\u02da\u02dd\u02e1"+
		"\u02e4\u02e8\u02ed\u02f1\u02f6\u02fa\u0300\u030a\u0318\u0327\24\3\2\2"+
		"\3\30\3\3\31\4\3\32\5\3\33\6\3\34\7\3\35\b\3\36\t\b\2\2\3=\n\3>\13\3>"+
		"\f\3>\r\3?\16\3?\17\3?\20\3B\21\3E\22";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
