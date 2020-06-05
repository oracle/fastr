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

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings("all")
public class RParser extends Parser {
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
	public static final int
		RULE_script = 0, RULE_root_function = 1, RULE_statement = 2, RULE_n_ = 3, 
		RULE_n_one = 4, RULE_n_multi = 5, RULE_expr_wo_assign = 6, RULE_sequence = 7, 
		RULE_expr = 8, RULE_expr_or_assign = 9, RULE_if_expr = 10, RULE_while_expr = 11, 
		RULE_for_expr = 12, RULE_repeat_expr = 13, RULE_function = 14, RULE_par_decl = 15, 
		RULE_tilde_expr = 16, RULE_utilde_expr = 17, RULE_or_expr = 18, RULE_and_expr = 19, 
		RULE_not_expr = 20, RULE_comp_expr = 21, RULE_add_expr = 22, RULE_mult_expr = 23, 
		RULE_operator_expr = 24, RULE_colon_expr = 25, RULE_unary_expression = 26, 
		RULE_power_expr = 27, RULE_basic_expr = 28, RULE_simple_expr = 29, RULE_number = 30, 
		RULE_conststring = 31, RULE_id = 32, RULE_bool = 33, RULE_or_operator = 34, 
		RULE_and_operator = 35, RULE_comp_operator = 36, RULE_add_operator = 37, 
		RULE_mult_operator = 38, RULE_power_operator = 39, RULE_args = 40, RULE_arg_expr = 41;
	private static String[] makeRuleNames() {
		return new String[] {
			"script", "root_function", "statement", "n_", "n_one", "n_multi", "expr_wo_assign", 
			"sequence", "expr", "expr_or_assign", "if_expr", "while_expr", "for_expr", 
			"repeat_expr", "function", "par_decl", "tilde_expr", "utilde_expr", "or_expr", 
			"and_expr", "not_expr", "comp_expr", "add_expr", "mult_expr", "operator_expr", 
			"colon_expr", "unary_expression", "power_expr", "basic_expr", "simple_expr", 
			"number", "conststring", "id", "bool", "or_operator", "and_operator", 
			"comp_operator", "add_operator", "mult_operator", "power_operator", "args", 
			"arg_expr"
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

	@Override
	public String getGrammarFileName() { return "R.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }


	    private Source source;
	    private Source initialSource;
	    private RCodeBuilder<RSyntaxNode> builder;
	    private TruffleRLanguage language;
	    private int fileStartOffset = 0;
	    private Map<String, Source> sourceCache;

	    public RParser(Source source, RLexer lexer, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language, Map<String, Source> sourceCache) {
	        super(new CommonTokenStream(lexer));
			_interp = new com.oracle.truffle.r.parser.DefaultRParserFactory.ThrowImmediatelyANTSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	        assert source != null && builder != null;
	        this.initialSource = source;
	        this.builder = builder;
	        this.language = language;
	        this.source = source;
	    }

	    public RParser(Source source, RLexer lexer, Source fullSource, int startLine, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language, Map<String, Source> sourceCache) {
	        super(new CommonTokenStream(lexer));
	        _interp = new com.oracle.truffle.r.parser.DefaultRParserFactory.ThrowImmediatelyANTSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	        assert source != null && builder != null;
	        this.initialSource = source;
	        this.builder = builder;
	        this.language = language;
	        this.source = fullSource;
	        fileStartOffset = -fullSource.getLineStartOffset(startLine);
	    }

	    private <T extends Throwable> boolean rethrow(T e) throws T {
	    	throw e;
	    }

	    /**
	     * Helper function that reports the last token to the AST builder. If keep.source=T,
	     * then the AST builder should create parse metadata even for individual tokens.
	     *
	     * There are two things that complicate the matter: ANTLR is backtracking the tokens stream,
	     * so if we simply hook into that, we would have to handle this to not report tokens twice.
	     * Moreover, FastR maintains internal state w.r.t. the '#line' directives (see checkFileDelim),
	     * so when getting the correct source sections for tokens, we would have to synchronize
	     * that state w.r.t. to the backtracking. For this reasons, manually reporting all tokens
	     * in every semantic action seems as way simpler solution.
	     */
	    private void tok() {
	        Token t = last();
	        builder.token(src(t), TokensMap.MAP[t.getType()], t.getText());
	    }

	    private void tok(RCodeToken tok) {
	        Token t = last();
	        builder.token(src(t), tok, t.getText());
	    }

	    private void modifyTok(RCodeToken tok) {
	        Token t = last();
	        builder.modifyLastToken(tok);
	    }

	    private void modifyTokIf(RCodeToken oldTok, RCodeToken newTok) {
	        Token t = last();
	        builder.modifyLastTokenIf(oldTok, newTok);
	    }

	    static <RSyntaxNode> Argument<RSyntaxNode> argument(SourceSection source, String name, RSyntaxNode expression) {
	        return RCodeBuilder.argument(source, name, expression);
	    }

	    /**
	     * Helper function that returns the last parsed token, usually used for building source sections.
	     */
	    private Token last() {
	        return getInputStream().LT(-1);
	    }

	    /**
	     * Helper function to create a special function lookup for the symbol in a given token.
	     * Special function lookup should be used to functions like infix '+'.
	     */
	    private RSyntaxNode operator(Token op) {
	        return builder.specialLookup(src(op), argName(op.getText()), true);
	    }

	    /**
	     * Helper function to create a function lookup for the symbol in a given token.
	     */
	    private RSyntaxNode functionLookup(Token op) {
	        return builder.lookup(src(op), argName(op.getText()), true);
	    }

	    /**
	     * Helper to check for empty lookups.
	     */
	     private String argName(String name) {
	         if (name.length() == 0) {
	             throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
	         }
	         return name;
	     }

	    /**
	     * Create a {@link SourceSection} from a single token.
	     */
	    private SourceSection src(Token t) {
	        CommonToken token = (CommonToken) t;
	        try {
	        	return source.createSection(token.getStartIndex() - fileStartOffset, token.getStopIndex() - token.getStartIndex() + 1);
	        } catch(IllegalArgumentException e) {
	        	// fall back and use the initial source (the file being parsed)
	        	resetSource();
	        	return source.createSection(token.getStartIndex(), token.getStopIndex() - token.getStartIndex() + 1);
	        }
	    }

	    /**
	     * Create a {@link SourceSection} from a start and end token.
	     */
	    private SourceSection src(Token start, Token stop) {
	        CommonToken cstart = (CommonToken) start;
	        CommonToken cstop = stop == null ? (CommonToken) start : (CommonToken) stop;
	        int startIndex = cstart.getStartIndex();
	        int stopIndex = cstop.getStopIndex();
	        int length = stopIndex - startIndex + (cstop.getType() == Token.EOF ? 0 : 1);
	        try {
	        	return source.createSection(startIndex - fileStartOffset, length);
	        } catch(IllegalArgumentException e) {
	        	// fall back and use the initial source (the file being parsed)
	        	resetSource();
	        	return source.createSection(startIndex, length);
	        }
	    }

	    /**
	     * Checks if the token is a comment token indicating that the following part of the source file was
	     * pasted from another file.
	     * The format of this file delimiter is '#line 1 "filename"'
	     */
	    private void checkFileDelim(CommonToken commentToken) {
				String commentLine = commentToken.getText();
		    	if(commentLine.startsWith("#line 1")) {
		    		int q0 = commentLine.indexOf("\"");
		    		int q1 = commentLine.indexOf("\"", q0+1);
		    		if(q0 != -1 && q1 != -1) {
		    			String path = commentLine.substring(q0+1, q1);
		    			try {
	                                    RContext context = RContext.getInstance();
		    			    String content = new String(context.getSafeTruffleFile(path).readAllBytes(), StandardCharsets.UTF_8);
		    			    String lineEnding = detectLineEnding(initialSource.getCharacters());
		    			    content = convertToLineEnding(content, lineEnding);
	                        source = RSource.fromFileName(context, content, path, false);
	                        fileStartOffset = commentToken.getStopIndex() + 1;
	                    } catch (IOException e) {
	                    	resetSource();
	                    } catch (URISyntaxException e) {
	                    	resetSource();
	                    }
		    		} else {
		    			// fall back and use the initial source (the file being parsed)
		    			resetSource();
		    		}
		    	}
		    }

		private void resetSource() {
			source = initialSource;
			fileStartOffset = 0;
		}

	        private String detectLineEnding(CharSequence code) {
	                int codeLen = code.length();
	                for (int i = 0; i < codeLen; i++) {
	                    switch (code.charAt(i)) {
	                        case '\r':
	                            if (i + 1 < codeLen && code.charAt(i+1) == '\n') {
	                                return "\r\n";
	                            }
	                            break;
	                        case '\n':
	                            return "\n";
	                    }
	                }
	                return "\n";
	        }

		private String convertToLineEnding(String content, String lineEnding) {
			if("\n".equals(lineEnding)) {
				return content.replaceAll("\\r\\n", "\n");
			} else if("\r\n".equals(lineEnding)) {
				return content.replaceAll("\\n", "\r\n");
			}
			return content;
		}


	public RParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ScriptContext extends ParserRuleContext {
		public List<RSyntaxNode> v;
		public StatementContext s;
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ScriptContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_script; }
	}

	public final ScriptContext script() throws RecognitionException {
		ScriptContext _localctx = new ScriptContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_script);

		        assert source != null && builder != null;
		        _localctx.v =  new ArrayList<RSyntaxNode>();
		    
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			n_();
			setState(90);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(85);
					_localctx.s = statement();
					 _localctx.v.add(_localctx.s.v); 
					}
					} 
				}
				setState(92);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			}
			_ctx.stop = _input.LT(-1);

			        if (getInputStream().LT(1).getType() != EOF) {
			        	throw new NoViableAltException(this);
			        }
			    
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Root_functionContext extends ParserRuleContext {
		public String name;
		public RootCallTarget v;
		public Token op;
		public Expr_or_assignContext body;
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode LPAR() { return getToken(RParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(RParser.RPAR, 0); }
		public TerminalNode FUNCTION() { return getToken(RParser.FUNCTION, 0); }
		public Expr_or_assignContext expr_or_assign() {
			return getRuleContext(Expr_or_assignContext.class,0);
		}
		public List<Par_declContext> par_decl() {
			return getRuleContexts(Par_declContext.class);
		}
		public Par_declContext par_decl(int i) {
			return getRuleContext(Par_declContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RParser.COMMA, i);
		}
		public Root_functionContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Root_functionContext(ParserRuleContext parent, int invokingState, String name) {
			super(parent, invokingState);
			this.name = name;
		}
		@Override public int getRuleIndex() { return RULE_root_function; }
	}

	public final Root_functionContext root_function(String name) throws RecognitionException {
		Root_functionContext _localctx = new Root_functionContext(_ctx, getState(), name);
		enterRule(_localctx, 2, RULE_root_function);

		        assert source != null && builder != null;
		        List<Argument<RSyntaxNode>> params = new ArrayList<>();
		    
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(93);
			n_();
			setState(94);
			_localctx.op = match(FUNCTION);
			tok();
			setState(96);
			n_();
			setState(97);
			match(LPAR);
			tok();
			setState(99);
			n_();
			setState(114);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 6)) & ~0x3f) == 0 && ((1L << (_la - 6)) & ((1L << (VARIADIC - 6)) | (1L << (DD - 6)) | (1L << (ID - 6)))) != 0)) {
				{
				setState(100);
				par_decl(params);
				setState(109);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(101);
						n_();
						setState(102);
						match(COMMA);
						tok();
						setState(104);
						n_();
						setState(105);
						par_decl(params);
						}
						} 
					}
					setState(111);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				}
				setState(112);
				n_();
				}
			}

			setState(116);
			match(RPAR);
			tok();
			setState(118);
			n_();
			setState(119);
			_localctx.body = expr_or_assign();
			 _localctx.v =  builder.rootFunction(language, src(_localctx.op, last()), params, _localctx.body.v, name); 
			}
			_ctx.stop = _input.LT(-1);

			        if (getInputStream().LT(1).getType() != EOF) {
			        	throw RInternalError.shouldNotReachHere("not at EOF after parsing deserialized function");
			        }
			    
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Expr_or_assignContext e;
		public N_oneContext n_one() {
			return getRuleContext(N_oneContext.class,0);
		}
		public Expr_or_assignContext expr_or_assign() {
			return getRuleContext(Expr_or_assignContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			_localctx.e = expr_or_assign();
			setState(123);
			n_one();
			 _localctx.v =  _localctx.e.v; 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class N_Context extends ParserRuleContext {
		public List<TerminalNode> NEWLINE() { return getTokens(RParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(RParser.NEWLINE, i);
		}
		public List<TerminalNode> COMMENT() { return getTokens(RParser.COMMENT); }
		public TerminalNode COMMENT(int i) {
			return getToken(RParser.COMMENT, i);
		}
		public N_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_; }
	}

	public final N_Context n_() throws RecognitionException {
		N_Context _localctx = new N_Context(_ctx, getState());
		enterRule(_localctx, 6, RULE_n_);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(131);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(129);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case NEWLINE:
						{
						setState(126);
						match(NEWLINE);
						}
						break;
					case COMMENT:
						{
						setState(127);
						match(COMMENT);
						 checkFileDelim((CommonToken)last()); 
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(133);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class N_oneContext extends ParserRuleContext {
		public List<TerminalNode> NEWLINE() { return getTokens(RParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(RParser.NEWLINE, i);
		}
		public List<TerminalNode> COMMENT() { return getTokens(RParser.COMMENT); }
		public TerminalNode COMMENT(int i) {
			return getToken(RParser.COMMENT, i);
		}
		public TerminalNode EOF() { return getToken(RParser.EOF, 0); }
		public TerminalNode SEMICOLON() { return getToken(RParser.SEMICOLON, 0); }
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public N_oneContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_one; }
	}

	public final N_oneContext n_one() throws RecognitionException {
		N_oneContext _localctx = new N_oneContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_n_one);
		try {
			int _alt;
			setState(144);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COMMENT:
			case NEWLINE:
				enterOuterAlt(_localctx, 1);
				{
				setState(137); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						setState(137);
						_errHandler.sync(this);
						switch (_input.LA(1)) {
						case NEWLINE:
							{
							setState(134);
							match(NEWLINE);
							}
							break;
						case COMMENT:
							{
							setState(135);
							match(COMMENT);
							 checkFileDelim((CommonToken)last()); 
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(139); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case EOF:
				enterOuterAlt(_localctx, 2);
				{
				setState(141);
				match(EOF);
				}
				break;
			case SEMICOLON:
				enterOuterAlt(_localctx, 3);
				{
				setState(142);
				match(SEMICOLON);
				setState(143);
				n_();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class N_multiContext extends ParserRuleContext {
		public List<TerminalNode> NEWLINE() { return getTokens(RParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(RParser.NEWLINE, i);
		}
		public List<TerminalNode> COMMENT() { return getTokens(RParser.COMMENT); }
		public TerminalNode COMMENT(int i) {
			return getToken(RParser.COMMENT, i);
		}
		public List<TerminalNode> SEMICOLON() { return getTokens(RParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(RParser.SEMICOLON, i);
		}
		public TerminalNode EOF() { return getToken(RParser.EOF, 0); }
		public N_multiContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_multi; }
	}

	public final N_multiContext n_multi() throws RecognitionException {
		N_multiContext _localctx = new N_multiContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_n_multi);
		try {
			int _alt;
			setState(155);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COMMENT:
			case SEMICOLON:
			case NEWLINE:
				enterOuterAlt(_localctx, 1);
				{
				setState(150); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						setState(150);
						_errHandler.sync(this);
						switch (_input.LA(1)) {
						case NEWLINE:
							{
							setState(146);
							match(NEWLINE);
							}
							break;
						case COMMENT:
							{
							setState(147);
							match(COMMENT);
							 checkFileDelim((CommonToken)last()); 
							}
							break;
						case SEMICOLON:
							{
							setState(149);
							match(SEMICOLON);
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(152); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case EOF:
				enterOuterAlt(_localctx, 2);
				{
				setState(154);
				match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Expr_wo_assignContext extends ParserRuleContext {
		public RSyntaxNode v;
		public While_exprContext w;
		public If_exprContext i;
		public For_exprContext f;
		public Repeat_exprContext r;
		public FunctionContext fun;
		public Token op;
		public While_exprContext while_expr() {
			return getRuleContext(While_exprContext.class,0);
		}
		public If_exprContext if_expr() {
			return getRuleContext(If_exprContext.class,0);
		}
		public For_exprContext for_expr() {
			return getRuleContext(For_exprContext.class,0);
		}
		public Repeat_exprContext repeat_expr() {
			return getRuleContext(Repeat_exprContext.class,0);
		}
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public TerminalNode NEXT() { return getToken(RParser.NEXT, 0); }
		public TerminalNode BREAK() { return getToken(RParser.BREAK, 0); }
		public TerminalNode LPAR() { return getToken(RParser.LPAR, 0); }
		public ArgsContext args() {
			return getRuleContext(ArgsContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(RParser.RPAR, 0); }
		public Expr_wo_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr_wo_assign; }
	}

	public final Expr_wo_assignContext expr_wo_assign() throws RecognitionException {
		Expr_wo_assignContext _localctx = new Expr_wo_assignContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_expr_wo_assign);
		int _la;
		try {
			setState(184);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WHILE:
				enterOuterAlt(_localctx, 1);
				{
				setState(157);
				_localctx.w = while_expr();
				 _localctx.v =  _localctx.w.v; 
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 2);
				{
				setState(160);
				_localctx.i = if_expr();
				 _localctx.v =  _localctx.i.v; 
				}
				break;
			case FOR:
				enterOuterAlt(_localctx, 3);
				{
				setState(163);
				_localctx.f = for_expr();
				 _localctx.v =  _localctx.f.v; 
				}
				break;
			case REPEAT:
				enterOuterAlt(_localctx, 4);
				{
				setState(166);
				_localctx.r = repeat_expr();
				 _localctx.v =  _localctx.r.v; 
				}
				break;
			case FUNCTION:
				enterOuterAlt(_localctx, 5);
				{
				setState(169);
				_localctx.fun = function(null);
				 _localctx.v =  _localctx.fun.v; 
				}
				break;
			case NEXT:
			case BREAK:
				enterOuterAlt(_localctx, 6);
				{
				setState(172);
				_localctx.op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==NEXT || _la==BREAK) ) {
					_localctx.op = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok();
				setState(181);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
				case 1:
					{
					setState(174);
					match(LPAR);
					tok();
					setState(176);
					args(null);
					setState(177);
					match(RPAR);
					tok();
					}
					break;
				case 2:
					{
					}
					break;
				}
				 _localctx.v =  builder.call(src(_localctx.op), operator(_localctx.op)); 
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SequenceContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Expr_or_assignContext e;
		public TerminalNode RBRACE() { return getToken(RParser.RBRACE, 0); }
		public TerminalNode LBRACE() { return getToken(RParser.LBRACE, 0); }
		public List<N_multiContext> n_multi() {
			return getRuleContexts(N_multiContext.class);
		}
		public N_multiContext n_multi(int i) {
			return getRuleContext(N_multiContext.class,i);
		}
		public List<Expr_or_assignContext> expr_or_assign() {
			return getRuleContexts(Expr_or_assignContext.class);
		}
		public Expr_or_assignContext expr_or_assign(int i) {
			return getRuleContext(Expr_or_assignContext.class,i);
		}
		public SequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sequence; }
	}

	public final SequenceContext sequence() throws RecognitionException {
		SequenceContext _localctx = new SequenceContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_sequence);
		 ArrayList<Argument<RSyntaxNode>> stmts = new ArrayList<>(); 
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(186);
			_localctx.op = match(LBRACE);
			tok();
			setState(189);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				{
				setState(188);
				n_multi();
				}
				break;
			}
			setState(205);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(191);
				_localctx.e = expr_or_assign();
				 stmts.add(RCodeBuilder.argument(_localctx.e.v)); 
				setState(199);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(193);
						n_multi();
						setState(194);
						_localctx.e = expr_or_assign();
						 stmts.add(RCodeBuilder.argument(_localctx.e.v)); 
						}
						} 
					}
					setState(201);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
				}
				setState(203);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - -1)) & ~0x3f) == 0 && ((1L << (_la - -1)) & ((1L << (EOF - -1)) | (1L << (COMMENT - -1)) | (1L << (SEMICOLON - -1)) | (1L << (NEWLINE - -1)))) != 0)) {
					{
					setState(202);
					n_multi();
					}
				}

				}
				break;
			}
			setState(207);
			match(RBRACE);
			tok();
			 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), stmts); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Tilde_exprContext l;
		public Token op;
		public FunctionContext r1;
		public ExprContext r2;
		public ExprContext r3;
		public ExprContext r4;
		public Tilde_exprContext tilde_expr() {
			return getRuleContext(Tilde_exprContext.class,0);
		}
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public TerminalNode RIGHT_ARROW() { return getToken(RParser.RIGHT_ARROW, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode SUPER_RIGHT_ARROW() { return getToken(RParser.SUPER_RIGHT_ARROW, 0); }
		public TerminalNode ARROW() { return getToken(RParser.ARROW, 0); }
		public TerminalNode SUPER_ARROW() { return getToken(RParser.SUPER_ARROW, 0); }
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_expr);
		 Token start = getInputStream().LT(1); RSyntaxNode rhs = null; 
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(211);
			_localctx.l = tilde_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(238);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ARROW:
			case SUPER_ARROW:
				{
				setState(213);
				_localctx.op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==ARROW || _la==SUPER_ARROW) ) {
					_localctx.op = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok();
				setState(215);
				n_();
				setState(222);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
				case 1:
					{
					setState(216);
					_localctx.r1 = function(_localctx.l.v);
					 rhs = _localctx.r1.v; 
					}
					break;
				case 2:
					{
					setState(219);
					_localctx.r2 = expr();
					 rhs = _localctx.r2.v; 
					}
					break;
				}
				 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.l.v, rhs); 
				}
				break;
			case RIGHT_ARROW:
				{
				setState(226);
				_localctx.op = match(RIGHT_ARROW);
				tok();
				setState(228);
				n_();
				setState(229);
				_localctx.r3 = expr();
				 _localctx.v =  builder.call(src(start, last()), builder.lookup(src(_localctx.op), "<-", true), _localctx.r3.v, _localctx.l.v); 
				}
				break;
			case SUPER_RIGHT_ARROW:
				{
				setState(232);
				_localctx.op = match(SUPER_RIGHT_ARROW);
				tok();
				setState(234);
				n_();
				setState(235);
				_localctx.r4 = expr();
				 _localctx.v =  builder.call(src(start, last()), builder.lookup(src(_localctx.op), "<<-", true), _localctx.r4.v, _localctx.l.v); 
				}
				break;
			case COMMENT:
			case COMMA:
			case RPAR:
			case RBRAKET:
			case NEWLINE:
				break;
			default:
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Expr_or_assignContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Tilde_exprContext l;
		public Token op;
		public FunctionContext r1;
		public Expr_or_assignContext r2;
		public Expr_or_assignContext r3;
		public Expr_or_assignContext r4;
		public Tilde_exprContext tilde_expr() {
			return getRuleContext(Tilde_exprContext.class,0);
		}
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public TerminalNode RIGHT_ARROW() { return getToken(RParser.RIGHT_ARROW, 0); }
		public Expr_or_assignContext expr_or_assign() {
			return getRuleContext(Expr_or_assignContext.class,0);
		}
		public TerminalNode SUPER_RIGHT_ARROW() { return getToken(RParser.SUPER_RIGHT_ARROW, 0); }
		public TerminalNode ARROW() { return getToken(RParser.ARROW, 0); }
		public TerminalNode SUPER_ARROW() { return getToken(RParser.SUPER_ARROW, 0); }
		public TerminalNode ASSIGN() { return getToken(RParser.ASSIGN, 0); }
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public Expr_or_assignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr_or_assign; }
	}

	public final Expr_or_assignContext expr_or_assign() throws RecognitionException {
		Expr_or_assignContext _localctx = new Expr_or_assignContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_expr_or_assign);
		 Token start = getInputStream().LT(1); RSyntaxNode rhs = null; 
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(240);
			_localctx.l = tilde_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(267);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(242);
				_localctx.op = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARROW) | (1L << SUPER_ARROW) | (1L << ASSIGN))) != 0)) ) {
					_localctx.op = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok();
				setState(244);
				n_();
				setState(251);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(245);
					_localctx.r1 = function(_localctx.l.v);
					 rhs = _localctx.r1.v; 
					}
					break;
				case 2:
					{
					setState(248);
					_localctx.r2 = expr_or_assign();
					 rhs = _localctx.r2.v; 
					}
					break;
				}
				 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.l.v, rhs); 
				}
				break;
			case 2:
				{
				setState(255);
				_localctx.op = match(RIGHT_ARROW);
				tok();
				setState(257);
				n_();
				setState(258);
				_localctx.r3 = expr_or_assign();
				 _localctx.v =  builder.call(src(start, last()), builder.lookup(src(_localctx.op), "<-", true), _localctx.r3.v, _localctx.l.v); 
				}
				break;
			case 3:
				{
				setState(261);
				_localctx.op = match(SUPER_RIGHT_ARROW);
				tok();
				setState(263);
				n_();
				setState(264);
				_localctx.r4 = expr_or_assign();
				 _localctx.v =  builder.call(src(start, last()), builder.lookup(src(_localctx.op), "<<-", true), _localctx.r4.v, _localctx.l.v); 
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class If_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Expr_or_assignContext cond;
		public Expr_or_assignContext t;
		public Expr_or_assignContext f;
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode LPAR() { return getToken(RParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(RParser.RPAR, 0); }
		public TerminalNode IF() { return getToken(RParser.IF, 0); }
		public List<Expr_or_assignContext> expr_or_assign() {
			return getRuleContexts(Expr_or_assignContext.class);
		}
		public Expr_or_assignContext expr_or_assign(int i) {
			return getRuleContext(Expr_or_assignContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(RParser.ELSE, 0); }
		public If_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_if_expr; }
	}

	public final If_exprContext if_expr() throws RecognitionException {
		If_exprContext _localctx = new If_exprContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_if_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			_localctx.op = match(IF);
			tok();
			setState(271);
			n_();
			setState(272);
			match(LPAR);
			tok();
			setState(274);
			n_();
			setState(275);
			_localctx.cond = expr_or_assign();
			setState(276);
			n_();
			setState(277);
			match(RPAR);
			tok();
			setState(279);
			n_();
			setState(280);
			_localctx.t = expr_or_assign();
			setState(289);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				{
				setState(281);
				n_();
				setState(282);
				match(ELSE);
				tok();
				setState(284);
				n_();
				setState(285);
				_localctx.f = expr_or_assign();
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.cond.v, _localctx.t.v, _localctx.f.v); 
				}
				}
				break;
			case 2:
				{
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.cond.v, _localctx.t.v); 
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class While_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Expr_or_assignContext c;
		public Expr_or_assignContext body;
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode LPAR() { return getToken(RParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(RParser.RPAR, 0); }
		public TerminalNode WHILE() { return getToken(RParser.WHILE, 0); }
		public List<Expr_or_assignContext> expr_or_assign() {
			return getRuleContexts(Expr_or_assignContext.class);
		}
		public Expr_or_assignContext expr_or_assign(int i) {
			return getRuleContext(Expr_or_assignContext.class,i);
		}
		public While_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_while_expr; }
	}

	public final While_exprContext while_expr() throws RecognitionException {
		While_exprContext _localctx = new While_exprContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_while_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(291);
			_localctx.op = match(WHILE);
			tok();
			setState(293);
			n_();
			setState(294);
			match(LPAR);
			tok();
			setState(296);
			n_();
			setState(297);
			_localctx.c = expr_or_assign();
			setState(298);
			n_();
			setState(299);
			match(RPAR);
			tok();
			setState(301);
			n_();
			setState(302);
			_localctx.body = expr_or_assign();
			 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.c.v, _localctx.body.v); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class For_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Token i;
		public Expr_or_assignContext in;
		public Expr_or_assignContext body;
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode LPAR() { return getToken(RParser.LPAR, 0); }
		public TerminalNode IN() { return getToken(RParser.IN, 0); }
		public TerminalNode RPAR() { return getToken(RParser.RPAR, 0); }
		public TerminalNode FOR() { return getToken(RParser.FOR, 0); }
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public List<Expr_or_assignContext> expr_or_assign() {
			return getRuleContexts(Expr_or_assignContext.class);
		}
		public Expr_or_assignContext expr_or_assign(int i) {
			return getRuleContext(Expr_or_assignContext.class,i);
		}
		public For_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_for_expr; }
	}

	public final For_exprContext for_expr() throws RecognitionException {
		For_exprContext _localctx = new For_exprContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_for_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(305);
			_localctx.op = match(FOR);
			tok();
			setState(307);
			n_();
			setState(308);
			match(LPAR);
			tok();
			setState(310);
			n_();
			setState(311);
			_localctx.i = match(ID);
			setState(312);
			n_();
			setState(313);
			match(IN);
			tok();
			setState(315);
			n_();
			setState(316);
			_localctx.in = expr_or_assign();
			setState(317);
			n_();
			setState(318);
			match(RPAR);
			tok();
			setState(320);
			n_();
			setState(321);
			_localctx.body = expr_or_assign();
			 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), builder.lookup(src(_localctx.i), (_localctx.i!=null?_localctx.i.getText():null), false), _localctx.in.v, _localctx.body.v); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Repeat_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Expr_or_assignContext body;
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public TerminalNode REPEAT() { return getToken(RParser.REPEAT, 0); }
		public Expr_or_assignContext expr_or_assign() {
			return getRuleContext(Expr_or_assignContext.class,0);
		}
		public Repeat_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_repeat_expr; }
	}

	public final Repeat_exprContext repeat_expr() throws RecognitionException {
		Repeat_exprContext _localctx = new Repeat_exprContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_repeat_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(324);
			_localctx.op = match(REPEAT);
			tok();
			setState(326);
			n_();
			setState(327);
			_localctx.body = expr_or_assign();
			 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.body.v); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionContext extends ParserRuleContext {
		public RSyntaxNode assignedTo;
		public RSyntaxNode v;
		public Token op;
		public Expr_or_assignContext body;
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode LPAR() { return getToken(RParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(RParser.RPAR, 0); }
		public TerminalNode FUNCTION() { return getToken(RParser.FUNCTION, 0); }
		public Expr_or_assignContext expr_or_assign() {
			return getRuleContext(Expr_or_assignContext.class,0);
		}
		public List<Par_declContext> par_decl() {
			return getRuleContexts(Par_declContext.class);
		}
		public Par_declContext par_decl(int i) {
			return getRuleContext(Par_declContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RParser.COMMA, i);
		}
		public FunctionContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public FunctionContext(ParserRuleContext parent, int invokingState, RSyntaxNode assignedTo) {
			super(parent, invokingState);
			this.assignedTo = assignedTo;
		}
		@Override public int getRuleIndex() { return RULE_function; }
	}

	public final FunctionContext function(RSyntaxNode assignedTo) throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState(), assignedTo);
		enterRule(_localctx, 28, RULE_function);
		 List<Argument<RSyntaxNode>> params = new ArrayList<>(); 
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(330);
			_localctx.op = match(FUNCTION);
			tok();
			setState(332);
			n_();
			setState(333);
			match(LPAR);
			tok();
			setState(335);
			n_();
			setState(350);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 6)) & ~0x3f) == 0 && ((1L << (_la - 6)) & ((1L << (VARIADIC - 6)) | (1L << (DD - 6)) | (1L << (ID - 6)))) != 0)) {
				{
				setState(336);
				par_decl(params);
				setState(345);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(337);
						n_();
						setState(338);
						match(COMMA);
						tok();
						setState(340);
						n_();
						setState(341);
						par_decl(params);
						}
						} 
					}
					setState(347);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				}
				setState(348);
				n_();
				}
			}

			setState(352);
			match(RPAR);
			tok();
			setState(354);
			n_();
			setState(355);
			_localctx.body = expr_or_assign();
			 _localctx.v =  builder.function(language, src(_localctx.op, last()), params, _localctx.body.v, assignedTo); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Par_declContext extends ParserRuleContext {
		public List<Argument<RSyntaxNode>> l;
		public Token i;
		public Token a;
		public ExprContext e;
		public Token v;
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode ASSIGN() { return getToken(RParser.ASSIGN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode VARIADIC() { return getToken(RParser.VARIADIC, 0); }
		public TerminalNode DD() { return getToken(RParser.DD, 0); }
		public Par_declContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Par_declContext(ParserRuleContext parent, int invokingState, List<Argument<RSyntaxNode>> l) {
			super(parent, invokingState);
			this.l = l;
		}
		@Override public int getRuleIndex() { return RULE_par_decl; }
	}

	public final Par_declContext par_decl(List<Argument<RSyntaxNode>> l) throws RecognitionException {
		Par_declContext _localctx = new Par_declContext(_ctx, getState(), l);
		enterRule(_localctx, 30, RULE_par_decl);
		try {
			setState(394);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(358);
				_localctx.i = match(ID);
				tok();
				 _localctx.l.add(argument(src(_localctx.i), (_localctx.i!=null?_localctx.i.getText():null), null)); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(361);
				_localctx.i = match(ID);
				tok();
				setState(363);
				n_();
				setState(364);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_FORMALS);
				setState(366);
				n_();
				setState(367);
				_localctx.e = expr();
				 _localctx.l.add(argument(src(_localctx.i, last()), (_localctx.i!=null?_localctx.i.getText():null), _localctx.e.v)); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(370);
				_localctx.v = match(VARIADIC);
				tok();
				 _localctx.l.add(argument(src(_localctx.v), (_localctx.v!=null?_localctx.v.getText():null), null)); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(373);
				_localctx.v = match(VARIADIC);
				tok();
				setState(375);
				n_();
				setState(376);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_FORMALS);
				setState(378);
				n_();
				setState(379);
				_localctx.e = expr();
				 _localctx.l.add(argument(src(_localctx.v), (_localctx.v!=null?_localctx.v.getText():null),  null)); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(382);
				_localctx.v = match(DD);
				tok();
				 _localctx.l.add(argument(src(_localctx.v), (_localctx.v!=null?_localctx.v.getText():null), null)); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(385);
				_localctx.v = match(DD);
				tok();
				setState(387);
				n_();
				setState(388);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_FORMALS);
				setState(390);
				n_();
				setState(391);
				expr();
				 _localctx.l.add(argument(src(_localctx.v), (_localctx.v!=null?_localctx.v.getText():null), null)); 
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tilde_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Utilde_exprContext l;
		public Token op;
		public Utilde_exprContext r;
		public List<Utilde_exprContext> utilde_expr() {
			return getRuleContexts(Utilde_exprContext.class);
		}
		public Utilde_exprContext utilde_expr(int i) {
			return getRuleContext(Utilde_exprContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<TerminalNode> TILDE() { return getTokens(RParser.TILDE); }
		public TerminalNode TILDE(int i) {
			return getToken(RParser.TILDE, i);
		}
		public Tilde_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tilde_expr; }
	}

	public final Tilde_exprContext tilde_expr() throws RecognitionException {
		Tilde_exprContext _localctx = new Tilde_exprContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_tilde_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(396);
			_localctx.l = utilde_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(406);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(398);
					_localctx.op = match(TILDE);
					tok();
					setState(400);
					n_();
					setState(401);
					_localctx.r = utilde_expr();
					 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(408);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Utilde_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Utilde_exprContext l1;
		public Or_exprContext l2;
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public TerminalNode TILDE() { return getToken(RParser.TILDE, 0); }
		public Utilde_exprContext utilde_expr() {
			return getRuleContext(Utilde_exprContext.class,0);
		}
		public Or_exprContext or_expr() {
			return getRuleContext(Or_exprContext.class,0);
		}
		public Utilde_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_utilde_expr; }
	}

	public final Utilde_exprContext utilde_expr() throws RecognitionException {
		Utilde_exprContext _localctx = new Utilde_exprContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_utilde_expr);
		try {
			setState(418);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(409);
				_localctx.op = match(TILDE);
				tok();
				setState(411);
				n_();
				setState(412);
				_localctx.l1 = utilde_expr();
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.l1.v); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(415);
				_localctx.l2 = or_expr();
				 _localctx.v =  _localctx.l2.v; 
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Or_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public And_exprContext l;
		public Or_operatorContext op;
		public And_exprContext r;
		public List<And_exprContext> and_expr() {
			return getRuleContexts(And_exprContext.class);
		}
		public And_exprContext and_expr(int i) {
			return getRuleContext(And_exprContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<Or_operatorContext> or_operator() {
			return getRuleContexts(Or_operatorContext.class);
		}
		public Or_operatorContext or_operator(int i) {
			return getRuleContext(Or_operatorContext.class,i);
		}
		public Or_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or_expr; }
	}

	public final Or_exprContext or_expr() throws RecognitionException {
		Or_exprContext _localctx = new Or_exprContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_or_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(420);
			_localctx.l = and_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(429);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(422);
					_localctx.op = or_operator();
					setState(423);
					n_();
					setState(424);
					_localctx.r = and_expr();
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(431);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class And_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Not_exprContext l;
		public And_operatorContext op;
		public Not_exprContext r;
		public List<Not_exprContext> not_expr() {
			return getRuleContexts(Not_exprContext.class);
		}
		public Not_exprContext not_expr(int i) {
			return getRuleContext(Not_exprContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<And_operatorContext> and_operator() {
			return getRuleContexts(And_operatorContext.class);
		}
		public And_operatorContext and_operator(int i) {
			return getRuleContext(And_operatorContext.class,i);
		}
		public And_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_expr; }
	}

	public final And_exprContext and_expr() throws RecognitionException {
		And_exprContext _localctx = new And_exprContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_and_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(432);
			_localctx.l = not_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(441);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(434);
					_localctx.op = and_operator();
					setState(435);
					n_();
					setState(436);
					_localctx.r = not_expr();
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(443);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Not_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Not_exprContext l;
		public Comp_exprContext b;
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public TerminalNode NOT() { return getToken(RParser.NOT, 0); }
		public Not_exprContext not_expr() {
			return getRuleContext(Not_exprContext.class,0);
		}
		public Comp_exprContext comp_expr() {
			return getRuleContext(Comp_exprContext.class,0);
		}
		public Not_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_not_expr; }
	}

	public final Not_exprContext not_expr() throws RecognitionException {
		Not_exprContext _localctx = new Not_exprContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_not_expr);
		try {
			setState(454);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(444);
				if (!(true)) throw new FailedPredicateException(this, "true");
				setState(445);
				_localctx.op = match(NOT);
				tok();
				setState(447);
				n_();
				setState(448);
				_localctx.l = not_expr();
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.l.v); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(451);
				_localctx.b = comp_expr();
				 _localctx.v =  _localctx.b.v; 
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Comp_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Add_exprContext l;
		public Comp_operatorContext op;
		public Add_exprContext r;
		public List<Add_exprContext> add_expr() {
			return getRuleContexts(Add_exprContext.class);
		}
		public Add_exprContext add_expr(int i) {
			return getRuleContext(Add_exprContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<Comp_operatorContext> comp_operator() {
			return getRuleContexts(Comp_operatorContext.class);
		}
		public Comp_operatorContext comp_operator(int i) {
			return getRuleContext(Comp_operatorContext.class,i);
		}
		public Comp_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comp_expr; }
	}

	public final Comp_exprContext comp_expr() throws RecognitionException {
		Comp_exprContext _localctx = new Comp_exprContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_comp_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(456);
			_localctx.l = add_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(465);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(458);
					_localctx.op = comp_operator();
					setState(459);
					n_();
					setState(460);
					_localctx.r = add_expr();
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(467);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Add_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Mult_exprContext l;
		public Add_operatorContext op;
		public Mult_exprContext r;
		public List<Mult_exprContext> mult_expr() {
			return getRuleContexts(Mult_exprContext.class);
		}
		public Mult_exprContext mult_expr(int i) {
			return getRuleContext(Mult_exprContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<Add_operatorContext> add_operator() {
			return getRuleContexts(Add_operatorContext.class);
		}
		public Add_operatorContext add_operator(int i) {
			return getRuleContext(Add_operatorContext.class,i);
		}
		public Add_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_add_expr; }
	}

	public final Add_exprContext add_expr() throws RecognitionException {
		Add_exprContext _localctx = new Add_exprContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_add_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(468);
			_localctx.l = mult_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(477);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(470);
					_localctx.op = add_operator();
					setState(471);
					n_();
					setState(472);
					_localctx.r = mult_expr();
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(479);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Mult_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Operator_exprContext l;
		public Mult_operatorContext op;
		public Operator_exprContext r;
		public List<Operator_exprContext> operator_expr() {
			return getRuleContexts(Operator_exprContext.class);
		}
		public Operator_exprContext operator_expr(int i) {
			return getRuleContext(Operator_exprContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<Mult_operatorContext> mult_operator() {
			return getRuleContexts(Mult_operatorContext.class);
		}
		public Mult_operatorContext mult_operator(int i) {
			return getRuleContext(Mult_operatorContext.class,i);
		}
		public Mult_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mult_expr; }
	}

	public final Mult_exprContext mult_expr() throws RecognitionException {
		Mult_exprContext _localctx = new Mult_exprContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_mult_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(480);
			_localctx.l = operator_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(489);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,32,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(482);
					_localctx.op = mult_operator();
					setState(483);
					n_();
					setState(484);
					_localctx.r = operator_expr();
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(491);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,32,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Operator_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Colon_exprContext l;
		public Token op;
		public Colon_exprContext r;
		public List<Colon_exprContext> colon_expr() {
			return getRuleContexts(Colon_exprContext.class);
		}
		public Colon_exprContext colon_expr(int i) {
			return getRuleContext(Colon_exprContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<TerminalNode> OP() { return getTokens(RParser.OP); }
		public TerminalNode OP(int i) {
			return getToken(RParser.OP, i);
		}
		public Operator_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operator_expr; }
	}

	public final Operator_exprContext operator_expr() throws RecognitionException {
		Operator_exprContext _localctx = new Operator_exprContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_operator_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(492);
			_localctx.l = colon_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(502);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(494);
					_localctx.op = match(OP);
					tok();
					setState(496);
					n_();
					setState(497);
					_localctx.r = colon_expr();
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, _localctx.r.v); 
					}
					} 
				}
				setState(504);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Colon_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Unary_expressionContext l;
		public Token op;
		public Unary_expressionContext r;
		public List<Unary_expressionContext> unary_expression() {
			return getRuleContexts(Unary_expressionContext.class);
		}
		public Unary_expressionContext unary_expression(int i) {
			return getRuleContext(Unary_expressionContext.class,i);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<TerminalNode> COLON() { return getTokens(RParser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(RParser.COLON, i);
		}
		public Colon_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_colon_expr; }
	}

	public final Colon_exprContext colon_expr() throws RecognitionException {
		Colon_exprContext _localctx = new Colon_exprContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_colon_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(505);
			_localctx.l = unary_expression();
			 _localctx.v =  _localctx.l.v; 
			setState(515);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(507);
					_localctx.op = match(COLON);
					tok();
					setState(509);
					n_();
					setState(510);
					_localctx.r = unary_expression();
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(517);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Unary_expressionContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token op;
		public Unary_expressionContext l1;
		public Utilde_exprContext l2;
		public Power_exprContext b;
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public TerminalNode PLUS() { return getToken(RParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(RParser.MINUS, 0); }
		public TerminalNode NOT() { return getToken(RParser.NOT, 0); }
		public TerminalNode QM() { return getToken(RParser.QM, 0); }
		public TerminalNode TILDE() { return getToken(RParser.TILDE, 0); }
		public Utilde_exprContext utilde_expr() {
			return getRuleContext(Utilde_exprContext.class,0);
		}
		public Power_exprContext power_expr() {
			return getRuleContext(Power_exprContext.class,0);
		}
		public Unary_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unary_expression; }
	}

	public final Unary_expressionContext unary_expression() throws RecognitionException {
		Unary_expressionContext _localctx = new Unary_expressionContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_unary_expression);
		int _la;
		try {
			setState(533);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOT:
			case QM:
			case PLUS:
			case MINUS:
				enterOuterAlt(_localctx, 1);
				{
				setState(518);
				_localctx.op = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NOT) | (1L << QM) | (1L << PLUS) | (1L << MINUS))) != 0)) ) {
					_localctx.op = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok();
				setState(520);
				n_();
				setState(521);
				_localctx.l1 = unary_expression();
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.l1.v); 
				}
				break;
			case TILDE:
				enterOuterAlt(_localctx, 2);
				{
				setState(524);
				_localctx.op = match(TILDE);
				tok();
				setState(526);
				n_();
				setState(527);
				_localctx.l2 = utilde_expr();
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.l2.v); 
				}
				break;
			case VARIADIC:
			case LBRACE:
			case LPAR:
			case FUNCTION:
			case NULL:
			case NA:
			case NAINT:
			case NAREAL:
			case NACHAR:
			case NACOMPL:
			case TRUE:
			case FALSE:
			case INF:
			case NAN:
			case WHILE:
			case FOR:
			case REPEAT:
			case IF:
			case NEXT:
			case BREAK:
			case INTEGER:
			case COMPLEX:
			case DOUBLE:
			case DD:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(530);
				_localctx.b = power_expr();
				 _localctx.v =  _localctx.b.v; 
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Power_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Basic_exprContext l;
		public Power_operatorContext op;
		public Unary_expressionContext r;
		public Basic_exprContext basic_expr() {
			return getRuleContext(Basic_exprContext.class,0);
		}
		public N_Context n_() {
			return getRuleContext(N_Context.class,0);
		}
		public Power_operatorContext power_operator() {
			return getRuleContext(Power_operatorContext.class,0);
		}
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public Power_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_power_expr; }
	}

	public final Power_exprContext power_expr() throws RecognitionException {
		Power_exprContext _localctx = new Power_exprContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_power_expr);
		 Token start = getInputStream().LT(1); 
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(535);
			_localctx.l = basic_expr();
			 _localctx.v =  _localctx.l.v; 
			setState(543);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				{
				{
				setState(537);
				_localctx.op = power_operator();
				setState(538);
				n_();
				setState(539);
				_localctx.r = unary_expression();
				 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
				}
				}
				break;
			case 2:
				{
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Basic_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token lhsToken;
		public Token op;
		public ArgsContext a;
		public Token y;
		public Simple_exprContext lhs;
		public IdContext name;
		public ConststringContext sname;
		public ArgsContext subset;
		public ArgsContext subscript;
		public Simple_exprContext simple_expr() {
			return getRuleContext(Simple_exprContext.class,0);
		}
		public List<TerminalNode> LPAR() { return getTokens(RParser.LPAR); }
		public TerminalNode LPAR(int i) {
			return getToken(RParser.LPAR, i);
		}
		public List<ArgsContext> args() {
			return getRuleContexts(ArgsContext.class);
		}
		public ArgsContext args(int i) {
			return getRuleContext(ArgsContext.class,i);
		}
		public List<TerminalNode> RPAR() { return getTokens(RParser.RPAR); }
		public TerminalNode RPAR(int i) {
			return getToken(RParser.RPAR, i);
		}
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public TerminalNode DD() { return getToken(RParser.DD, 0); }
		public TerminalNode VARIADIC() { return getToken(RParser.VARIADIC, 0); }
		public TerminalNode STRING() { return getToken(RParser.STRING, 0); }
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<TerminalNode> RBRAKET() { return getTokens(RParser.RBRAKET); }
		public TerminalNode RBRAKET(int i) {
			return getToken(RParser.RBRAKET, i);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<ConststringContext> conststring() {
			return getRuleContexts(ConststringContext.class);
		}
		public ConststringContext conststring(int i) {
			return getRuleContext(ConststringContext.class,i);
		}
		public List<TerminalNode> LBRAKET() { return getTokens(RParser.LBRAKET); }
		public TerminalNode LBRAKET(int i) {
			return getToken(RParser.LBRAKET, i);
		}
		public List<TerminalNode> LBB() { return getTokens(RParser.LBB); }
		public TerminalNode LBB(int i) {
			return getToken(RParser.LBB, i);
		}
		public List<TerminalNode> FIELD() { return getTokens(RParser.FIELD); }
		public TerminalNode FIELD(int i) {
			return getToken(RParser.FIELD, i);
		}
		public List<TerminalNode> AT() { return getTokens(RParser.AT); }
		public TerminalNode AT(int i) {
			return getToken(RParser.AT, i);
		}
		public Basic_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_basic_expr; }
	}

	public final Basic_exprContext basic_expr() throws RecognitionException {
		Basic_exprContext _localctx = new Basic_exprContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_basic_expr);
		 Token start = getInputStream().LT(1); 
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(557);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				{
				{
				setState(545);
				_localctx.lhsToken = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 6)) & ~0x3f) == 0 && ((1L << (_la - 6)) & ((1L << (VARIADIC - 6)) | (1L << (DD - 6)) | (1L << (ID - 6)) | (1L << (STRING - 6)))) != 0)) ) {
					_localctx.lhsToken = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok(RCodeToken.SYMBOL_FUNCTION_CALL);
				setState(547);
				_localctx.op = match(LPAR);
				tok();
				setState(549);
				_localctx.a = args(null);
				setState(550);
				_localctx.y = match(RPAR);
				tok();
				 _localctx.v =  builder.call(src(start, _localctx.y), functionLookup(_localctx.lhsToken), _localctx.a.v); 
				}
				}
				break;
			case 2:
				{
				setState(554);
				_localctx.lhs = simple_expr();
				 _localctx.v =  _localctx.lhs.v; 
				}
				break;
			}
			setState(598);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				{
				setState(594); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(592);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
						case 1:
							{
							{
							setState(559);
							_localctx.op = _input.LT(1);
							_la = _input.LA(1);
							if ( !(_la==FIELD || _la==AT) ) {
								_localctx.op = _errHandler.recoverInline(this);
							}
							else {
								if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
								_errHandler.reportMatch(this);
								consume();
							}
							tok();
							setState(561);
							n_();
							setState(562);
							_localctx.name = id();
							 modifyTok(RCodeToken.SLOT); _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, builder.lookup(src(_localctx.name.v), _localctx.name.v.getText(), false)); 
							}
							}
							break;
						case 2:
							{
							{
							setState(565);
							_localctx.op = _input.LT(1);
							_la = _input.LA(1);
							if ( !(_la==FIELD || _la==AT) ) {
								_localctx.op = _errHandler.recoverInline(this);
							}
							else {
								if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
								_errHandler.reportMatch(this);
								consume();
							}
							tok();
							setState(567);
							n_();
							setState(568);
							_localctx.sname = conststring();
							 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, _localctx.sname.v); 
							}
							}
							break;
						case 3:
							{
							{
							setState(571);
							_localctx.op = match(LBRAKET);
							tok();
							setState(573);
							_localctx.subset = args(_localctx.v);
							setState(574);
							_localctx.y = match(RBRAKET);
							 tok();
							                                                           if (_localctx.subset.v.size() == 1) {
							                                                               _localctx.subset.v.add(RCodeBuilder.argumentEmpty());
							                                                           }
							                                                           _localctx.v =  builder.call(src(start, _localctx.y), operator(_localctx.op), _localctx.subset.v);
							                                                       
							}
							}
							break;
						case 4:
							{
							{
							setState(577);
							_localctx.op = match(LBB);
							tok();
							setState(579);
							_localctx.subscript = args(_localctx.v);
							setState(580);
							match(RBRAKET);
							tok();
							setState(582);
							_localctx.y = match(RBRAKET);
							 tok();
							                                                           if (_localctx.subscript.v.size() == 1) {
							                                                               _localctx.subscript.v.add(RCodeBuilder.argumentEmpty());
							                                                           }
							                                                           _localctx.v =  builder.call(src(start, _localctx.y), operator(_localctx.op), _localctx.subscript.v);
							                                                       
							}
							}
							break;
						case 5:
							{
							{
							setState(585);
							_localctx.op = match(LPAR);
							tok();
							setState(587);
							_localctx.a = args(null);
							setState(588);
							_localctx.y = match(RPAR);
							tok();
							 _localctx.v =  builder.call(src(start, _localctx.y), _localctx.v, _localctx.a.v); 
							}
							}
							break;
						}
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(596); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Simple_exprContext extends ParserRuleContext {
		public RSyntaxNode v;
		public IdContext i;
		public BoolContext b;
		public Token d;
		public Token t;
		public NumberContext num;
		public ConststringContext cstr;
		public IdContext pkg;
		public Token op;
		public IdContext compId;
		public Token compString;
		public Expr_or_assignContext ea;
		public Token y;
		public SequenceContext s;
		public Expr_wo_assignContext e;
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public BoolContext bool() {
			return getRuleContext(BoolContext.class,0);
		}
		public TerminalNode DD() { return getToken(RParser.DD, 0); }
		public TerminalNode NULL() { return getToken(RParser.NULL, 0); }
		public TerminalNode INF() { return getToken(RParser.INF, 0); }
		public TerminalNode NAN() { return getToken(RParser.NAN, 0); }
		public TerminalNode NAINT() { return getToken(RParser.NAINT, 0); }
		public TerminalNode NAREAL() { return getToken(RParser.NAREAL, 0); }
		public TerminalNode NACHAR() { return getToken(RParser.NACHAR, 0); }
		public TerminalNode NACOMPL() { return getToken(RParser.NACOMPL, 0); }
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public ConststringContext conststring() {
			return getRuleContext(ConststringContext.class,0);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode NS_GET() { return getToken(RParser.NS_GET, 0); }
		public TerminalNode NS_GET_INT() { return getToken(RParser.NS_GET_INT, 0); }
		public TerminalNode STRING() { return getToken(RParser.STRING, 0); }
		public TerminalNode LPAR() { return getToken(RParser.LPAR, 0); }
		public Expr_or_assignContext expr_or_assign() {
			return getRuleContext(Expr_or_assignContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(RParser.RPAR, 0); }
		public SequenceContext sequence() {
			return getRuleContext(SequenceContext.class,0);
		}
		public Expr_wo_assignContext expr_wo_assign() {
			return getRuleContext(Expr_wo_assignContext.class,0);
		}
		public Simple_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simple_expr; }
	}

	public final Simple_exprContext simple_expr() throws RecognitionException {
		Simple_exprContext _localctx = new Simple_exprContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_simple_expr);
		 Token start = getInputStream().LT(1); List<Argument<RSyntaxNode>> args = new ArrayList<>(); Token compToken = null; 
		int _la;
		try {
			setState(658);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(600);
				_localctx.i = id();
				 _localctx.v =  builder.lookup(src(_localctx.i.v), (_localctx.i!=null?_input.getText(_localctx.i.start,_localctx.i.stop):null), false); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(603);
				_localctx.b = bool();
				 _localctx.v =  builder.constant(src(start, last()), _localctx.b.v); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(606);
				_localctx.d = match(DD);
				 tok(); _localctx.v =  builder.lookup(src(_localctx.d), (_localctx.d!=null?_localctx.d.getText():null), false); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(608);
				_localctx.t = match(NULL);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RNull.instance); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(610);
				_localctx.t = match(INF);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), Double.POSITIVE_INFINITY); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(612);
				_localctx.t = match(NAN);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), Double.NaN); 
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(614);
				_localctx.t = match(NAINT);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RRuntime.INT_NA); 
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(616);
				_localctx.t = match(NAREAL);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RRuntime.DOUBLE_NA); 
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(618);
				_localctx.t = match(NACHAR);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RRuntime.STRING_NA); 
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(620);
				_localctx.t = match(NACOMPL);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RComplex.createNA()); 
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(622);
				_localctx.num = number();
				 _localctx.v =  _localctx.num.v; 
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(625);
				_localctx.cstr = conststring();
				 _localctx.v =  _localctx.cstr.v; 
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(628);
				_localctx.pkg = id();
				modifyTok(RCodeToken.SYMBOL_PACKAGE);
				setState(630);
				_localctx.op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==NS_GET_INT || _la==NS_GET) ) {
					_localctx.op = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok();
				setState(632);
				n_();

				        SourceSection pkgSource = src(_localctx.pkg.v);
				        args.add(argument(pkgSource, (String) null, builder.lookup(pkgSource, (_localctx.pkg!=null?_input.getText(_localctx.pkg.start,_localctx.pkg.stop):null), false)));
				        
				setState(640);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case VARIADIC:
				case ID:
					{
					setState(634);
					_localctx.compId = id();

					        SourceSection compSource = src(_localctx.compId.v);
					        compToken = _localctx.compId.v;
					        args.add(argument(compSource, (String) null, builder.lookup(compSource, (_localctx.compId!=null?_input.getText(_localctx.compId.start,_localctx.compId.stop):null), false)));
					        
					}
					break;
				case STRING:
					{
					setState(637);
					_localctx.compString = match(STRING);
					tok();

					        SourceSection compSource = src(_localctx.compString);
					        compToken = _localctx.compString;
					        args.add(argument(compSource, (String) null, builder.constant(compSource, (_localctx.compString!=null?_localctx.compString.getText():null))));
					        
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				 _localctx.v =  builder.call(src(_localctx.pkg.v, compToken), operator(_localctx.op), args); 
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(644);
				_localctx.op = match(LPAR);
				tok();
				setState(646);
				n_();
				setState(647);
				_localctx.ea = expr_or_assign();
				setState(648);
				n_();
				setState(649);
				_localctx.y = match(RPAR);
				 tok(); _localctx.v =  builder.call(src(_localctx.op, _localctx.y), operator(_localctx.op), _localctx.ea.v); 
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(652);
				_localctx.s = sequence();
				 _localctx.v =  _localctx.s.v; 
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(655);
				_localctx.e = expr_wo_assign();
				 _localctx.v =  _localctx.e.v; 
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumberContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token i;
		public Token d;
		public Token c;
		public TerminalNode INTEGER() { return getToken(RParser.INTEGER, 0); }
		public TerminalNode DOUBLE() { return getToken(RParser.DOUBLE, 0); }
		public TerminalNode COMPLEX() { return getToken(RParser.COMPLEX, 0); }
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_number);
		try {
			setState(666);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTEGER:
				enterOuterAlt(_localctx, 1);
				{
				setState(660);
				_localctx.i = match(INTEGER);
				 tok();
				        assert (_localctx.i!=null?_localctx.i.getText():null) != null; // to make spotbugs happy
				        double value = RRuntime.string2doubleNoCheck((_localctx.i!=null?_localctx.i.getText():null));
				        if (value == (int) value) {
				            if ((_localctx.i!=null?_localctx.i.getText():null).indexOf('.') != -1) {
				                RError.warning(RError.NO_CALLER, RError.Message.INTEGER_VALUE_UNNECESARY_DECIMAL, (_localctx.i!=null?_localctx.i.getText():null) + "L");
				            }
				            _localctx.v =  builder.constant(src(_localctx.i), (int) value);
				        } else {
				            if ((_localctx.i!=null?_localctx.i.getText():null).indexOf('.') != -1) {
				                RError.warning(RError.NO_CALLER, RError.Message.INTEGER_VALUE_DECIMAL, (_localctx.i!=null?_localctx.i.getText():null) + "L");
				            } else if ((_localctx.i!=null?_localctx.i.getText():null).startsWith("0x")) {
				                RError.warning(RError.NO_CALLER, RError.Message.NON_INTEGER_VALUE, (_localctx.i!=null?_localctx.i.getText():null));
				            } else {
				                RError.warning(RError.NO_CALLER, RError.Message.NON_INTEGER_VALUE, (_localctx.i!=null?_localctx.i.getText():null) + "L");
				            }
				            _localctx.v =  builder.constant(src(_localctx.i), value);
				        }
				      
				}
				break;
			case DOUBLE:
				enterOuterAlt(_localctx, 2);
				{
				setState(662);
				_localctx.d = match(DOUBLE);
				 tok(); _localctx.v =  builder.constant(src(_localctx.d), RRuntime.string2doubleNoCheck((_localctx.d!=null?_localctx.d.getText():null))); 
				}
				break;
			case COMPLEX:
				enterOuterAlt(_localctx, 3);
				{
				setState(664);
				_localctx.c = match(COMPLEX);
				 tok(); _localctx.v =  builder.constant(src(_localctx.c), RComplex.valueOf(0, RRuntime.string2doubleNoCheck((_localctx.c!=null?_localctx.c.getText():null)))); 
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConststringContext extends ParserRuleContext {
		public RSyntaxNode v;
		public Token s;
		public TerminalNode STRING() { return getToken(RParser.STRING, 0); }
		public ConststringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conststring; }
	}

	public final ConststringContext conststring() throws RecognitionException {
		ConststringContext _localctx = new ConststringContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_conststring);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(668);
			_localctx.s = match(STRING);
			 tok(); _localctx.v =  builder.constant(src(_localctx.s), (_localctx.s!=null?_localctx.s.getText():null)); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdContext extends ParserRuleContext {
		public Token v;
		public Token ident;
		public Token var;
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public TerminalNode VARIADIC() { return getToken(RParser.VARIADIC, 0); }
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_id);
		try {
			setState(675);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(671);
				_localctx.ident = match(ID);
				 tok(); _localctx.v =  _localctx.ident; 
				}
				break;
			case VARIADIC:
				enterOuterAlt(_localctx, 2);
				{
				setState(673);
				_localctx.var = match(VARIADIC);
				 tok(); _localctx.v =  _localctx.var; 
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BoolContext extends ParserRuleContext {
		public byte v;
		public Token t;
		public TerminalNode TRUE() { return getToken(RParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(RParser.FALSE, 0); }
		public TerminalNode NA() { return getToken(RParser.NA, 0); }
		public BoolContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bool; }
	}

	public final BoolContext bool() throws RecognitionException {
		BoolContext _localctx = new BoolContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_bool);
		try {
			setState(683);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(677);
				_localctx.t = match(TRUE);
				 tok(); _localctx.v =  RRuntime.LOGICAL_TRUE; 
				}
				break;
			case FALSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(679);
				_localctx.t = match(FALSE);
				 tok(); _localctx.v =  RRuntime.LOGICAL_FALSE; 
				}
				break;
			case NA:
				enterOuterAlt(_localctx, 3);
				{
				setState(681);
				_localctx.t = match(NA);
				 tok(); _localctx.v =  RRuntime.LOGICAL_NA; 
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Or_operatorContext extends ParserRuleContext {
		public Token v;
		public Token op;
		public TerminalNode OR() { return getToken(RParser.OR, 0); }
		public TerminalNode ELEMENTWISEOR() { return getToken(RParser.ELEMENTWISEOR, 0); }
		public Or_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or_operator; }
	}

	public final Or_operatorContext or_operator() throws RecognitionException {
		Or_operatorContext _localctx = new Or_operatorContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_or_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(685);
			_localctx.op = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==OR || _la==ELEMENTWISEOR) ) {
				_localctx.op = _errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			 tok(); _localctx.v =  _localctx.op; 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class And_operatorContext extends ParserRuleContext {
		public Token v;
		public Token op;
		public TerminalNode AND() { return getToken(RParser.AND, 0); }
		public TerminalNode ELEMENTWISEAND() { return getToken(RParser.ELEMENTWISEAND, 0); }
		public And_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_operator; }
	}

	public final And_operatorContext and_operator() throws RecognitionException {
		And_operatorContext _localctx = new And_operatorContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_and_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(688);
			_localctx.op = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==AND || _la==ELEMENTWISEAND) ) {
				_localctx.op = _errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			 tok(); _localctx.v =  _localctx.op; 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Comp_operatorContext extends ParserRuleContext {
		public Token v;
		public Token op;
		public TerminalNode GT() { return getToken(RParser.GT, 0); }
		public TerminalNode GE() { return getToken(RParser.GE, 0); }
		public TerminalNode LT() { return getToken(RParser.LT, 0); }
		public TerminalNode LE() { return getToken(RParser.LE, 0); }
		public TerminalNode EQ() { return getToken(RParser.EQ, 0); }
		public TerminalNode NE() { return getToken(RParser.NE, 0); }
		public Comp_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comp_operator; }
	}

	public final Comp_operatorContext comp_operator() throws RecognitionException {
		Comp_operatorContext _localctx = new Comp_operatorContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_comp_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(691);
			_localctx.op = _input.LT(1);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << EQ) | (1L << NE) | (1L << GE) | (1L << LE) | (1L << GT) | (1L << LT))) != 0)) ) {
				_localctx.op = _errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			 tok(); _localctx.v =  _localctx.op; 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Add_operatorContext extends ParserRuleContext {
		public Token v;
		public Token op;
		public TerminalNode PLUS() { return getToken(RParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(RParser.MINUS, 0); }
		public Add_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_add_operator; }
	}

	public final Add_operatorContext add_operator() throws RecognitionException {
		Add_operatorContext _localctx = new Add_operatorContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_add_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(694);
			_localctx.op = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==PLUS || _la==MINUS) ) {
				_localctx.op = _errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			 tok(); _localctx.v =  _localctx.op; 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Mult_operatorContext extends ParserRuleContext {
		public Token v;
		public Token op;
		public TerminalNode MULT() { return getToken(RParser.MULT, 0); }
		public TerminalNode DIV() { return getToken(RParser.DIV, 0); }
		public Mult_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mult_operator; }
	}

	public final Mult_operatorContext mult_operator() throws RecognitionException {
		Mult_operatorContext _localctx = new Mult_operatorContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_mult_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(697);
			_localctx.op = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==MULT || _la==DIV) ) {
				_localctx.op = _errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			 tok(); _localctx.v =  _localctx.op; 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Power_operatorContext extends ParserRuleContext {
		public Token v;
		public Token op;
		public TerminalNode CARET() { return getToken(RParser.CARET, 0); }
		public Power_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_power_operator; }
	}

	public final Power_operatorContext power_operator() throws RecognitionException {
		Power_operatorContext _localctx = new Power_operatorContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_power_operator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(700);
			_localctx.op = match(CARET);
			 tok(); _localctx.v =  _localctx.op; 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgsContext extends ParserRuleContext {
		public RSyntaxNode firstArg;
		public List<Argument<RSyntaxNode>> v;
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public List<Arg_exprContext> arg_expr() {
			return getRuleContexts(Arg_exprContext.class);
		}
		public Arg_exprContext arg_expr(int i) {
			return getRuleContext(Arg_exprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RParser.COMMA, i);
		}
		public ArgsContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ArgsContext(ParserRuleContext parent, int invokingState, RSyntaxNode firstArg) {
			super(parent, invokingState);
			this.firstArg = firstArg;
		}
		@Override public int getRuleIndex() { return RULE_args; }
	}

	public final ArgsContext args(RSyntaxNode firstArg) throws RecognitionException {
		ArgsContext _localctx = new ArgsContext(_ctx, getState(), firstArg);
		enterRule(_localctx, 80, RULE_args);

		              _localctx.v =  new ArrayList<>();
		              if (firstArg != null) {
		                  _localctx.v.add(RCodeBuilder.argument(firstArg));
		              }
		          
		int _la;
		try {
			setState(737);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(703);
				n_();
				setState(720);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
				case 1:
					{
					setState(704);
					arg_expr(_localctx.v);
					setState(705);
					n_();
					setState(717);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(706);
						match(COMMA);
						tok();
						setState(712);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,46,_ctx) ) {
						case 1:
							{
							 _localctx.v.add(RCodeBuilder.argumentEmpty()); 
							}
							break;
						case 2:
							{
							setState(709);
							n_();
							setState(710);
							arg_expr(_localctx.v);
							}
							break;
						}
						setState(714);
						n_();
						}
						}
						setState(719);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(722);
				n_();
				 _localctx.v.add(RCodeBuilder.argumentEmpty()); 
				setState(733); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(724);
					match(COMMA);
					tok();
					setState(730);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,49,_ctx) ) {
					case 1:
						{
						 _localctx.v.add(RCodeBuilder.argumentEmpty()); 
						}
						break;
					case 2:
						{
						setState(727);
						n_();
						setState(728);
						arg_expr(_localctx.v);
						}
						break;
					}
					setState(732);
					n_();
					}
					}
					setState(735); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==COMMA );
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Arg_exprContext extends ParserRuleContext {
		public List<Argument<RSyntaxNode>> l;
		public ExprContext e;
		public Token ID;
		public Token VARIADIC;
		public Token NULL;
		public Token STRING;
		public Token a;
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public List<N_Context> n_() {
			return getRuleContexts(N_Context.class);
		}
		public N_Context n_(int i) {
			return getRuleContext(N_Context.class,i);
		}
		public TerminalNode ASSIGN() { return getToken(RParser.ASSIGN, 0); }
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public TerminalNode VARIADIC() { return getToken(RParser.VARIADIC, 0); }
		public TerminalNode NULL() { return getToken(RParser.NULL, 0); }
		public TerminalNode STRING() { return getToken(RParser.STRING, 0); }
		public Arg_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Arg_exprContext(ParserRuleContext parent, int invokingState, List<Argument<RSyntaxNode>> l) {
			super(parent, invokingState);
			this.l = l;
		}
		@Override public int getRuleIndex() { return RULE_arg_expr; }
	}

	public final Arg_exprContext arg_expr(List<Argument<RSyntaxNode>> l) throws RecognitionException {
		Arg_exprContext _localctx = new Arg_exprContext(_ctx, getState(), l);
		enterRule(_localctx, 82, RULE_arg_expr);
		 Token start = getInputStream().LT(1); 
		try {
			setState(765);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,54,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(739);
				_localctx.e = expr();
				 _localctx.l.add(argument(src(start, last()), (String) null, _localctx.e.v)); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				 Token name = null; RSyntaxNode value = null; 
				setState(751);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ID:
					{
					setState(743);
					_localctx.ID = match(ID);
					name = _localctx.ID; tok(RCodeToken.SYMBOL_SUB);
					}
					break;
				case VARIADIC:
					{
					setState(745);
					_localctx.VARIADIC = match(VARIADIC);
					name=_localctx.VARIADIC; tok();
					}
					break;
				case NULL:
					{
					setState(747);
					_localctx.NULL = match(NULL);
					name = _localctx.NULL; tok();
					}
					break;
				case STRING:
					{
					setState(749);
					_localctx.STRING = match(STRING);
					name = _localctx.STRING; tok();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(753);
				n_();
				setState(754);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_SUB);
				setState(761);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,53,_ctx) ) {
				case 1:
					{
					setState(756);
					n_();
					setState(757);
					_localctx.e = expr();
					 value = _localctx.e.v; 
					}
					break;
				case 2:
					{
					}
					break;
				}
				 _localctx.l.add(argument(src(name, last()), argName(name.getText()), value)); 
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 20:
			return not_expr_sempred((Not_exprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean not_expr_sempred(Not_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return true;
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3E\u0302\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\3"+
		"\2\3\2\3\2\3\2\7\2[\n\2\f\2\16\2^\13\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\7\3n\n\3\f\3\16\3q\13\3\3\3\3\3\5\3u\n\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\7\5\u0084\n\5\f\5\16"+
		"\5\u0087\13\5\3\6\3\6\3\6\6\6\u008c\n\6\r\6\16\6\u008d\3\6\3\6\3\6\5\6"+
		"\u0093\n\6\3\7\3\7\3\7\3\7\6\7\u0099\n\7\r\7\16\7\u009a\3\7\5\7\u009e"+
		"\n\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u00b8\n\b\3\b\5\b\u00bb\n\b\3\t\3\t"+
		"\3\t\5\t\u00c0\n\t\3\t\3\t\3\t\3\t\3\t\3\t\7\t\u00c8\n\t\f\t\16\t\u00cb"+
		"\13\t\3\t\5\t\u00ce\n\t\5\t\u00d0\n\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u00e1\n\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u00f1\n\n\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\5\13\u00fe\n\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u010e\n\13\3\f\3\f"+
		"\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3"+
		"\f\5\f\u0124\n\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3"+
		"\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\7\20\u015a\n\20"+
		"\f\20\16\20\u015d\13\20\3\20\3\20\5\20\u0161\n\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\5\21\u018d\n\21\3\22"+
		"\3\22\3\22\3\22\3\22\3\22\3\22\3\22\7\22\u0197\n\22\f\22\16\22\u019a\13"+
		"\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\5\23\u01a5\n\23\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\7\24\u01ae\n\24\f\24\16\24\u01b1\13\24"+
		"\3\25\3\25\3\25\3\25\3\25\3\25\3\25\7\25\u01ba\n\25\f\25\16\25\u01bd\13"+
		"\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u01c9\n\26"+
		"\3\27\3\27\3\27\3\27\3\27\3\27\3\27\7\27\u01d2\n\27\f\27\16\27\u01d5\13"+
		"\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\7\30\u01de\n\30\f\30\16\30\u01e1"+
		"\13\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\7\31\u01ea\n\31\f\31\16\31\u01ed"+
		"\13\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\7\32\u01f7\n\32\f\32\16"+
		"\32\u01fa\13\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\7\33\u0204\n\33"+
		"\f\33\16\33\u0207\13\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3"+
		"\34\3\34\3\34\3\34\3\34\3\34\5\34\u0218\n\34\3\35\3\35\3\35\3\35\3\35"+
		"\3\35\3\35\3\35\5\35\u0222\n\35\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\5\36\u0230\n\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\5\36\u0253"+
		"\n\36\6\36\u0255\n\36\r\36\16\36\u0256\5\36\u0259\n\36\3\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\5\37\u0283\n\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\5\37\u0295\n\37\3 \3 \3 \3 \3 \3 \5 \u029d\n \3!\3!\3!\3\"\3\"\3\"\3"+
		"\"\5\"\u02a6\n\"\3#\3#\3#\3#\3#\3#\5#\u02ae\n#\3$\3$\3$\3%\3%\3%\3&\3"+
		"&\3&\3\'\3\'\3\'\3(\3(\3(\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3*\3*\5*\u02cb"+
		"\n*\3*\7*\u02ce\n*\f*\16*\u02d1\13*\5*\u02d3\n*\3*\3*\3*\3*\3*\3*\3*\3"+
		"*\5*\u02dd\n*\3*\6*\u02e0\n*\r*\16*\u02e1\5*\u02e4\n*\3+\3+\3+\3+\3+\3"+
		"+\3+\3+\3+\3+\3+\3+\5+\u02f2\n+\3+\3+\3+\3+\3+\3+\3+\3+\5+\u02fc\n+\3"+
		"+\3+\5+\u0300\n+\3+\2\2,\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&("+
		"*,.\60\62\64\668:<>@BDFHJLNPRT\2\16\3\2;<\3\2\4\5\4\2\4\5\17\17\4\2\""+
		"$\'\'\5\2\b\bBCEE\3\2()\3\2\20\21\3\2\27\30\3\2\25\26\3\2\t\16\4\2$$\'"+
		"\'\3\2%&\2\u0332\2V\3\2\2\2\4_\3\2\2\2\6|\3\2\2\2\b\u0085\3\2\2\2\n\u0092"+
		"\3\2\2\2\f\u009d\3\2\2\2\16\u00ba\3\2\2\2\20\u00bc\3\2\2\2\22\u00d5\3"+
		"\2\2\2\24\u00f2\3\2\2\2\26\u010f\3\2\2\2\30\u0125\3\2\2\2\32\u0133\3\2"+
		"\2\2\34\u0146\3\2\2\2\36\u014c\3\2\2\2 \u018c\3\2\2\2\"\u018e\3\2\2\2"+
		"$\u01a4\3\2\2\2&\u01a6\3\2\2\2(\u01b2\3\2\2\2*\u01c8\3\2\2\2,\u01ca\3"+
		"\2\2\2.\u01d6\3\2\2\2\60\u01e2\3\2\2\2\62\u01ee\3\2\2\2\64\u01fb\3\2\2"+
		"\2\66\u0217\3\2\2\28\u0219\3\2\2\2:\u022f\3\2\2\2<\u0294\3\2\2\2>\u029c"+
		"\3\2\2\2@\u029e\3\2\2\2B\u02a5\3\2\2\2D\u02ad\3\2\2\2F\u02af\3\2\2\2H"+
		"\u02b2\3\2\2\2J\u02b5\3\2\2\2L\u02b8\3\2\2\2N\u02bb\3\2\2\2P\u02be\3\2"+
		"\2\2R\u02e3\3\2\2\2T\u02ff\3\2\2\2V\\\5\b\5\2WX\5\6\4\2XY\b\2\1\2Y[\3"+
		"\2\2\2ZW\3\2\2\2[^\3\2\2\2\\Z\3\2\2\2\\]\3\2\2\2]\3\3\2\2\2^\\\3\2\2\2"+
		"_`\5\b\5\2`a\7*\2\2ab\b\3\1\2bc\5\b\5\2cd\7\33\2\2de\b\3\1\2et\5\b\5\2"+
		"fo\5 \21\2gh\5\b\5\2hi\7\24\2\2ij\b\3\1\2jk\5\b\5\2kl\5 \21\2ln\3\2\2"+
		"\2mg\3\2\2\2nq\3\2\2\2om\3\2\2\2op\3\2\2\2pr\3\2\2\2qo\3\2\2\2rs\5\b\5"+
		"\2su\3\2\2\2tf\3\2\2\2tu\3\2\2\2uv\3\2\2\2vw\7\34\2\2wx\b\3\1\2xy\5\b"+
		"\5\2yz\5\24\13\2z{\b\3\1\2{\5\3\2\2\2|}\5\24\13\2}~\5\n\6\2~\177\b\4\1"+
		"\2\177\7\3\2\2\2\u0080\u0084\7>\2\2\u0081\u0082\7\3\2\2\u0082\u0084\b"+
		"\5\1\2\u0083\u0080\3\2\2\2\u0083\u0081\3\2\2\2\u0084\u0087\3\2\2\2\u0085"+
		"\u0083\3\2\2\2\u0085\u0086\3\2\2\2\u0086\t\3\2\2\2\u0087\u0085\3\2\2\2"+
		"\u0088\u008c\7>\2\2\u0089\u008a\7\3\2\2\u008a\u008c\b\6\1\2\u008b\u0088"+
		"\3\2\2\2\u008b\u0089\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008b\3\2\2\2\u008d"+
		"\u008e\3\2\2\2\u008e\u0093\3\2\2\2\u008f\u0093\7\2\2\3\u0090\u0091\7\23"+
		"\2\2\u0091\u0093\5\b\5\2\u0092\u008b\3\2\2\2\u0092\u008f\3\2\2\2\u0092"+
		"\u0090\3\2\2\2\u0093\13\3\2\2\2\u0094\u0099\7>\2\2\u0095\u0096\7\3\2\2"+
		"\u0096\u0099\b\7\1\2\u0097\u0099\7\23\2\2\u0098\u0094\3\2\2\2\u0098\u0095"+
		"\3\2\2\2\u0098\u0097\3\2\2\2\u0099\u009a\3\2\2\2\u009a\u0098\3\2\2\2\u009a"+
		"\u009b\3\2\2\2\u009b\u009e\3\2\2\2\u009c\u009e\7\2\2\3\u009d\u0098\3\2"+
		"\2\2\u009d\u009c\3\2\2\2\u009e\r\3\2\2\2\u009f\u00a0\5\30\r\2\u00a0\u00a1"+
		"\b\b\1\2\u00a1\u00bb\3\2\2\2\u00a2\u00a3\5\26\f\2\u00a3\u00a4\b\b\1\2"+
		"\u00a4\u00bb\3\2\2\2\u00a5\u00a6\5\32\16\2\u00a6\u00a7\b\b\1\2\u00a7\u00bb"+
		"\3\2\2\2\u00a8\u00a9\5\34\17\2\u00a9\u00aa\b\b\1\2\u00aa\u00bb\3\2\2\2"+
		"\u00ab\u00ac\5\36\20\2\u00ac\u00ad\b\b\1\2\u00ad\u00bb\3\2\2\2\u00ae\u00af"+
		"\t\2\2\2\u00af\u00b7\b\b\1\2\u00b0\u00b1\7\33\2\2\u00b1\u00b2\b\b\1\2"+
		"\u00b2\u00b3\5R*\2\u00b3\u00b4\7\34\2\2\u00b4\u00b5\b\b\1\2\u00b5\u00b8"+
		"\3\2\2\2\u00b6\u00b8\3\2\2\2\u00b7\u00b0\3\2\2\2\u00b7\u00b6\3\2\2\2\u00b8"+
		"\u00b9\3\2\2\2\u00b9\u00bb\b\b\1\2\u00ba\u009f\3\2\2\2\u00ba\u00a2\3\2"+
		"\2\2\u00ba\u00a5\3\2\2\2\u00ba\u00a8\3\2\2\2\u00ba\u00ab\3\2\2\2\u00ba"+
		"\u00ae\3\2\2\2\u00bb\17\3\2\2\2\u00bc\u00bd\7\31\2\2\u00bd\u00bf\b\t\1"+
		"\2\u00be\u00c0\5\f\7\2\u00bf\u00be\3\2\2\2\u00bf\u00c0\3\2\2\2\u00c0\u00cf"+
		"\3\2\2\2\u00c1\u00c2\5\24\13\2\u00c2\u00c9\b\t\1\2\u00c3\u00c4\5\f\7\2"+
		"\u00c4\u00c5\5\24\13\2\u00c5\u00c6\b\t\1\2\u00c6\u00c8\3\2\2\2\u00c7\u00c3"+
		"\3\2\2\2\u00c8\u00cb\3\2\2\2\u00c9\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca"+
		"\u00cd\3\2\2\2\u00cb\u00c9\3\2\2\2\u00cc\u00ce\5\f\7\2\u00cd\u00cc\3\2"+
		"\2\2\u00cd\u00ce\3\2\2\2\u00ce\u00d0\3\2\2\2\u00cf\u00c1\3\2\2\2\u00cf"+
		"\u00d0\3\2\2\2\u00d0\u00d1\3\2\2\2\u00d1\u00d2\7\32\2\2\u00d2\u00d3\b"+
		"\t\1\2\u00d3\u00d4\b\t\1\2\u00d4\21\3\2\2\2\u00d5\u00d6\5\"\22\2\u00d6"+
		"\u00f0\b\n\1\2\u00d7\u00d8\t\3\2\2\u00d8\u00d9\b\n\1\2\u00d9\u00e0\5\b"+
		"\5\2\u00da\u00db\5\36\20\2\u00db\u00dc\b\n\1\2\u00dc\u00e1\3\2\2\2\u00dd"+
		"\u00de\5\22\n\2\u00de\u00df\b\n\1\2\u00df\u00e1\3\2\2\2\u00e0\u00da\3"+
		"\2\2\2\u00e0\u00dd\3\2\2\2\u00e1\u00e2\3\2\2\2\u00e2\u00e3\b\n\1\2\u00e3"+
		"\u00f1\3\2\2\2\u00e4\u00e5\7\6\2\2\u00e5\u00e6\b\n\1\2\u00e6\u00e7\5\b"+
		"\5\2\u00e7\u00e8\5\22\n\2\u00e8\u00e9\b\n\1\2\u00e9\u00f1\3\2\2\2\u00ea"+
		"\u00eb\7\7\2\2\u00eb\u00ec\b\n\1\2\u00ec\u00ed\5\b\5\2\u00ed\u00ee\5\22"+
		"\n\2\u00ee\u00ef\b\n\1\2\u00ef\u00f1\3\2\2\2\u00f0\u00d7\3\2\2\2\u00f0"+
		"\u00e4\3\2\2\2\u00f0\u00ea\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\23\3\2\2"+
		"\2\u00f2\u00f3\5\"\22\2\u00f3\u010d\b\13\1\2\u00f4\u00f5\t\4\2\2\u00f5"+
		"\u00f6\b\13\1\2\u00f6\u00fd\5\b\5\2\u00f7\u00f8\5\36\20\2\u00f8\u00f9"+
		"\b\13\1\2\u00f9\u00fe\3\2\2\2\u00fa\u00fb\5\24\13\2\u00fb\u00fc\b\13\1"+
		"\2\u00fc\u00fe\3\2\2\2\u00fd\u00f7\3\2\2\2\u00fd\u00fa\3\2\2\2\u00fe\u00ff"+
		"\3\2\2\2\u00ff\u0100\b\13\1\2\u0100\u010e\3\2\2\2\u0101\u0102\7\6\2\2"+
		"\u0102\u0103\b\13\1\2\u0103\u0104\5\b\5\2\u0104\u0105\5\24\13\2\u0105"+
		"\u0106\b\13\1\2\u0106\u010e\3\2\2\2\u0107\u0108\7\7\2\2\u0108\u0109\b"+
		"\13\1\2\u0109\u010a\5\b\5\2\u010a\u010b\5\24\13\2\u010b\u010c\b\13\1\2"+
		"\u010c\u010e\3\2\2\2\u010d\u00f4\3\2\2\2\u010d\u0101\3\2\2\2\u010d\u0107"+
		"\3\2\2\2\u010d\u010e\3\2\2\2\u010e\25\3\2\2\2\u010f\u0110\79\2\2\u0110"+
		"\u0111\b\f\1\2\u0111\u0112\5\b\5\2\u0112\u0113\7\33\2\2\u0113\u0114\b"+
		"\f\1\2\u0114\u0115\5\b\5\2\u0115\u0116\5\24\13\2\u0116\u0117\5\b\5\2\u0117"+
		"\u0118\7\34\2\2\u0118\u0119\b\f\1\2\u0119\u011a\5\b\5\2\u011a\u0123\5"+
		"\24\13\2\u011b\u011c\5\b\5\2\u011c\u011d\7:\2\2\u011d\u011e\b\f\1\2\u011e"+
		"\u011f\5\b\5\2\u011f\u0120\5\24\13\2\u0120\u0121\b\f\1\2\u0121\u0124\3"+
		"\2\2\2\u0122\u0124\b\f\1\2\u0123\u011b\3\2\2\2\u0123\u0122\3\2\2\2\u0124"+
		"\27\3\2\2\2\u0125\u0126\7\65\2\2\u0126\u0127\b\r\1\2\u0127\u0128\5\b\5"+
		"\2\u0128\u0129\7\33\2\2\u0129\u012a\b\r\1\2\u012a\u012b\5\b\5\2\u012b"+
		"\u012c\5\24\13\2\u012c\u012d\5\b\5\2\u012d\u012e\7\34\2\2\u012e\u012f"+
		"\b\r\1\2\u012f\u0130\5\b\5\2\u0130\u0131\5\24\13\2\u0131\u0132\b\r\1\2"+
		"\u0132\31\3\2\2\2\u0133\u0134\7\66\2\2\u0134\u0135\b\16\1\2\u0135\u0136"+
		"\5\b\5\2\u0136\u0137\7\33\2\2\u0137\u0138\b\16\1\2\u0138\u0139\5\b\5\2"+
		"\u0139\u013a\7C\2\2\u013a\u013b\5\b\5\2\u013b\u013c\78\2\2\u013c\u013d"+
		"\b\16\1\2\u013d\u013e\5\b\5\2\u013e\u013f\5\24\13\2\u013f\u0140\5\b\5"+
		"\2\u0140\u0141\7\34\2\2\u0141\u0142\b\16\1\2\u0142\u0143\5\b\5\2\u0143"+
		"\u0144\5\24\13\2\u0144\u0145\b\16\1\2\u0145\33\3\2\2\2\u0146\u0147\7\67"+
		"\2\2\u0147\u0148\b\17\1\2\u0148\u0149\5\b\5\2\u0149\u014a\5\24\13\2\u014a"+
		"\u014b\b\17\1\2\u014b\35\3\2\2\2\u014c\u014d\7*\2\2\u014d\u014e\b\20\1"+
		"\2\u014e\u014f\5\b\5\2\u014f\u0150\7\33\2\2\u0150\u0151\b\20\1\2\u0151"+
		"\u0160\5\b\5\2\u0152\u015b\5 \21\2\u0153\u0154\5\b\5\2\u0154\u0155\7\24"+
		"\2\2\u0155\u0156\b\20\1\2\u0156\u0157\5\b\5\2\u0157\u0158\5 \21\2\u0158"+
		"\u015a\3\2\2\2\u0159\u0153\3\2\2\2\u015a\u015d\3\2\2\2\u015b\u0159\3\2"+
		"\2\2\u015b\u015c\3\2\2\2\u015c\u015e\3\2\2\2\u015d\u015b\3\2\2\2\u015e"+
		"\u015f\5\b\5\2\u015f\u0161\3\2\2\2\u0160\u0152\3\2\2\2\u0160\u0161\3\2"+
		"\2\2\u0161\u0162\3\2\2\2\u0162\u0163\7\34\2\2\u0163\u0164\b\20\1\2\u0164"+
		"\u0165\5\b\5\2\u0165\u0166\5\24\13\2\u0166\u0167\b\20\1\2\u0167\37\3\2"+
		"\2\2\u0168\u0169\7C\2\2\u0169\u016a\b\21\1\2\u016a\u018d\b\21\1\2\u016b"+
		"\u016c\7C\2\2\u016c\u016d\b\21\1\2\u016d\u016e\5\b\5\2\u016e\u016f\7\17"+
		"\2\2\u016f\u0170\b\21\1\2\u0170\u0171\5\b\5\2\u0171\u0172\5\22\n\2\u0172"+
		"\u0173\b\21\1\2\u0173\u018d\3\2\2\2\u0174\u0175\7\b\2\2\u0175\u0176\b"+
		"\21\1\2\u0176\u018d\b\21\1\2\u0177\u0178\7\b\2\2\u0178\u0179\b\21\1\2"+
		"\u0179\u017a\5\b\5\2\u017a\u017b\7\17\2\2\u017b\u017c\b\21\1\2\u017c\u017d"+
		"\5\b\5\2\u017d\u017e\5\22\n\2\u017e\u017f\b\21\1\2\u017f\u018d\3\2\2\2"+
		"\u0180\u0181\7B\2\2\u0181\u0182\b\21\1\2\u0182\u018d\b\21\1\2\u0183\u0184"+
		"\7B\2\2\u0184\u0185\b\21\1\2\u0185\u0186\5\b\5\2\u0186\u0187\7\17\2\2"+
		"\u0187\u0188\b\21\1\2\u0188\u0189\5\b\5\2\u0189\u018a\5\22\n\2\u018a\u018b"+
		"\b\21\1\2\u018b\u018d\3\2\2\2\u018c\u0168\3\2\2\2\u018c\u016b\3\2\2\2"+
		"\u018c\u0174\3\2\2\2\u018c\u0177\3\2\2\2\u018c\u0180\3\2\2\2\u018c\u0183"+
		"\3\2\2\2\u018d!\3\2\2\2\u018e\u018f\5$\23\2\u018f\u0198\b\22\1\2\u0190"+
		"\u0191\7!\2\2\u0191\u0192\b\22\1\2\u0192\u0193\5\b\5\2\u0193\u0194\5$"+
		"\23\2\u0194\u0195\b\22\1\2\u0195\u0197\3\2\2\2\u0196\u0190\3\2\2\2\u0197"+
		"\u019a\3\2\2\2\u0198\u0196\3\2\2\2\u0198\u0199\3\2\2\2\u0199#\3\2\2\2"+
		"\u019a\u0198\3\2\2\2\u019b\u019c\7!\2\2\u019c\u019d\b\23\1\2\u019d\u019e"+
		"\5\b\5\2\u019e\u019f\5$\23\2\u019f\u01a0\b\23\1\2\u01a0\u01a5\3\2\2\2"+
		"\u01a1\u01a2\5&\24\2\u01a2\u01a3\b\23\1\2\u01a3\u01a5\3\2\2\2\u01a4\u019b"+
		"\3\2\2\2\u01a4\u01a1\3\2\2\2\u01a5%\3\2\2\2\u01a6\u01a7\5(\25\2\u01a7"+
		"\u01af\b\24\1\2\u01a8\u01a9\5F$\2\u01a9\u01aa\5\b\5\2\u01aa\u01ab\5(\25"+
		"\2\u01ab\u01ac\b\24\1\2\u01ac\u01ae\3\2\2\2\u01ad\u01a8\3\2\2\2\u01ae"+
		"\u01b1\3\2\2\2\u01af\u01ad\3\2\2\2\u01af\u01b0\3\2\2\2\u01b0\'\3\2\2\2"+
		"\u01b1\u01af\3\2\2\2\u01b2\u01b3\5*\26\2\u01b3\u01bb\b\25\1\2\u01b4\u01b5"+
		"\5H%\2\u01b5\u01b6\5\b\5\2\u01b6\u01b7\5*\26\2\u01b7\u01b8\b\25\1\2\u01b8"+
		"\u01ba\3\2\2\2\u01b9\u01b4\3\2\2\2\u01ba\u01bd\3\2\2\2\u01bb\u01b9\3\2"+
		"\2\2\u01bb\u01bc\3\2\2\2\u01bc)\3\2\2\2\u01bd\u01bb\3\2\2\2\u01be\u01bf"+
		"\6\26\2\2\u01bf\u01c0\7\"\2\2\u01c0\u01c1\b\26\1\2\u01c1\u01c2\5\b\5\2"+
		"\u01c2\u01c3\5*\26\2\u01c3\u01c4\b\26\1\2\u01c4\u01c9\3\2\2\2\u01c5\u01c6"+
		"\5,\27\2\u01c6\u01c7\b\26\1\2\u01c7\u01c9\3\2\2\2\u01c8\u01be\3\2\2\2"+
		"\u01c8\u01c5\3\2\2\2\u01c9+\3\2\2\2\u01ca\u01cb\5.\30\2\u01cb\u01d3\b"+
		"\27\1\2\u01cc\u01cd\5J&\2\u01cd\u01ce\5\b\5\2\u01ce\u01cf\5.\30\2\u01cf"+
		"\u01d0\b\27\1\2\u01d0\u01d2\3\2\2\2\u01d1\u01cc\3\2\2\2\u01d2\u01d5\3"+
		"\2\2\2\u01d3\u01d1\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4-\3\2\2\2\u01d5\u01d3"+
		"\3\2\2\2\u01d6\u01d7\5\60\31\2\u01d7\u01df\b\30\1\2\u01d8\u01d9\5L\'\2"+
		"\u01d9\u01da\5\b\5\2\u01da\u01db\5\60\31\2\u01db\u01dc\b\30\1\2\u01dc"+
		"\u01de\3\2\2\2\u01dd\u01d8\3\2\2\2\u01de\u01e1\3\2\2\2\u01df\u01dd\3\2"+
		"\2\2\u01df\u01e0\3\2\2\2\u01e0/\3\2\2\2\u01e1\u01df\3\2\2\2\u01e2\u01e3"+
		"\5\62\32\2\u01e3\u01eb\b\31\1\2\u01e4\u01e5\5N(\2\u01e5\u01e6\5\b\5\2"+
		"\u01e6\u01e7\5\62\32\2\u01e7\u01e8\b\31\1\2\u01e8\u01ea\3\2\2\2\u01e9"+
		"\u01e4\3\2\2\2\u01ea\u01ed\3\2\2\2\u01eb\u01e9\3\2\2\2\u01eb\u01ec\3\2"+
		"\2\2\u01ec\61\3\2\2\2\u01ed\u01eb\3\2\2\2\u01ee\u01ef\5\64\33\2\u01ef"+
		"\u01f8\b\32\1\2\u01f0\u01f1\7D\2\2\u01f1\u01f2\b\32\1\2\u01f2\u01f3\5"+
		"\b\5\2\u01f3\u01f4\5\64\33\2\u01f4\u01f5\b\32\1\2\u01f5\u01f7\3\2\2\2"+
		"\u01f6\u01f0\3\2\2\2\u01f7\u01fa\3\2\2\2\u01f8\u01f6\3\2\2\2\u01f8\u01f9"+
		"\3\2\2\2\u01f9\63\3\2\2\2\u01fa\u01f8\3\2\2\2\u01fb\u01fc\5\66\34\2\u01fc"+
		"\u0205\b\33\1\2\u01fd\u01fe\7\22\2\2\u01fe\u01ff\b\33\1\2\u01ff\u0200"+
		"\5\b\5\2\u0200\u0201\5\66\34\2\u0201\u0202\b\33\1\2\u0202\u0204\3\2\2"+
		"\2\u0203\u01fd\3\2\2\2\u0204\u0207\3\2\2\2\u0205\u0203\3\2\2\2\u0205\u0206"+
		"\3\2\2\2\u0206\65\3\2\2\2\u0207\u0205\3\2\2\2\u0208\u0209\t\5\2\2\u0209"+
		"\u020a\b\34\1\2\u020a\u020b\5\b\5\2\u020b\u020c\5\66\34\2\u020c\u020d"+
		"\b\34\1\2\u020d\u0218\3\2\2\2\u020e\u020f\7!\2\2\u020f\u0210\b\34\1\2"+
		"\u0210\u0211\5\b\5\2\u0211\u0212\5$\23\2\u0212\u0213\b\34\1\2\u0213\u0218"+
		"\3\2\2\2\u0214\u0215\58\35\2\u0215\u0216\b\34\1\2\u0216\u0218\3\2\2\2"+
		"\u0217\u0208\3\2\2\2\u0217\u020e\3\2\2\2\u0217\u0214\3\2\2\2\u0218\67"+
		"\3\2\2\2\u0219\u021a\5:\36\2\u021a\u0221\b\35\1\2\u021b\u021c\5P)\2\u021c"+
		"\u021d\5\b\5\2\u021d\u021e\5\66\34\2\u021e\u021f\b\35\1\2\u021f\u0222"+
		"\3\2\2\2\u0220\u0222\3\2\2\2\u0221\u021b\3\2\2\2\u0221\u0220\3\2\2\2\u0222"+
		"9\3\2\2\2\u0223\u0224\t\6\2\2\u0224\u0225\b\36\1\2\u0225\u0226\7\33\2"+
		"\2\u0226\u0227\b\36\1\2\u0227\u0228\5R*\2\u0228\u0229\7\34\2\2\u0229\u022a"+
		"\b\36\1\2\u022a\u022b\b\36\1\2\u022b\u0230\3\2\2\2\u022c\u022d\5<\37\2"+
		"\u022d\u022e\b\36\1\2\u022e\u0230\3\2\2\2\u022f\u0223\3\2\2\2\u022f\u022c"+
		"\3\2\2\2\u0230\u0258\3\2\2\2\u0231\u0232\t\7\2\2\u0232\u0233\b\36\1\2"+
		"\u0233\u0234\5\b\5\2\u0234\u0235\5B\"\2\u0235\u0236\b\36\1\2\u0236\u0253"+
		"\3\2\2\2\u0237\u0238\t\7\2\2\u0238\u0239\b\36\1\2\u0239\u023a\5\b\5\2"+
		"\u023a\u023b\5@!\2\u023b\u023c\b\36\1\2\u023c\u0253\3\2\2\2\u023d\u023e"+
		"\7\36\2\2\u023e\u023f\b\36\1\2\u023f\u0240\5R*\2\u0240\u0241\7\37\2\2"+
		"\u0241\u0242\b\36\1\2\u0242\u0253\3\2\2\2\u0243\u0244\7\35\2\2\u0244\u0245"+
		"\b\36\1\2\u0245\u0246\5R*\2\u0246\u0247\7\37\2\2\u0247\u0248\b\36\1\2"+
		"\u0248\u0249\7\37\2\2\u0249\u024a\b\36\1\2\u024a\u0253\3\2\2\2\u024b\u024c"+
		"\7\33\2\2\u024c\u024d\b\36\1\2\u024d\u024e\5R*\2\u024e\u024f\7\34\2\2"+
		"\u024f\u0250\b\36\1\2\u0250\u0251\b\36\1\2\u0251\u0253\3\2\2\2\u0252\u0231"+
		"\3\2\2\2\u0252\u0237\3\2\2\2\u0252\u023d\3\2\2\2\u0252\u0243\3\2\2\2\u0252"+
		"\u024b\3\2\2\2\u0253\u0255\3\2\2\2\u0254\u0252\3\2\2\2\u0255\u0256\3\2"+
		"\2\2\u0256\u0254\3\2\2\2\u0256\u0257\3\2\2\2\u0257\u0259\3\2\2\2\u0258"+
		"\u0254\3\2\2\2\u0258\u0259\3\2\2\2\u0259;\3\2\2\2\u025a\u025b\5B\"\2\u025b"+
		"\u025c\b\37\1\2\u025c\u0295\3\2\2\2\u025d\u025e\5D#\2\u025e\u025f\b\37"+
		"\1\2\u025f\u0295\3\2\2\2\u0260\u0261\7B\2\2\u0261\u0295\b\37\1\2\u0262"+
		"\u0263\7+\2\2\u0263\u0295\b\37\1\2\u0264\u0265\7\63\2\2\u0265\u0295\b"+
		"\37\1\2\u0266\u0267\7\64\2\2\u0267\u0295\b\37\1\2\u0268\u0269\7-\2\2\u0269"+
		"\u0295\b\37\1\2\u026a\u026b\7.\2\2\u026b\u0295\b\37\1\2\u026c\u026d\7"+
		"/\2\2\u026d\u0295\b\37\1\2\u026e\u026f\7\60\2\2\u026f\u0295\b\37\1\2\u0270"+
		"\u0271\5> \2\u0271\u0272\b\37\1\2\u0272\u0295\3\2\2\2\u0273\u0274\5@!"+
		"\2\u0274\u0275\b\37\1\2\u0275\u0295\3\2\2\2\u0276\u0277\5B\"\2\u0277\u0278"+
		"\b\37\1\2\u0278\u0279\t\b\2\2\u0279\u027a\b\37\1\2\u027a\u027b\5\b\5\2"+
		"\u027b\u0282\b\37\1\2\u027c\u027d\5B\"\2\u027d\u027e\b\37\1\2\u027e\u0283"+
		"\3\2\2\2\u027f\u0280\7E\2\2\u0280\u0281\b\37\1\2\u0281\u0283\b\37\1\2"+
		"\u0282\u027c\3\2\2\2\u0282\u027f\3\2\2\2\u0283\u0284\3\2\2\2\u0284\u0285"+
		"\b\37\1\2\u0285\u0295\3\2\2\2\u0286\u0287\7\33\2\2\u0287\u0288\b\37\1"+
		"\2\u0288\u0289\5\b\5\2\u0289\u028a\5\24\13\2\u028a\u028b\5\b\5\2\u028b"+
		"\u028c\7\34\2\2\u028c\u028d\b\37\1\2\u028d\u0295\3\2\2\2\u028e\u028f\5"+
		"\20\t\2\u028f\u0290\b\37\1\2\u0290\u0295\3\2\2\2\u0291\u0292\5\16\b\2"+
		"\u0292\u0293\b\37\1\2\u0293\u0295\3\2\2\2\u0294\u025a\3\2\2\2\u0294\u025d"+
		"\3\2\2\2\u0294\u0260\3\2\2\2\u0294\u0262\3\2\2\2\u0294\u0264\3\2\2\2\u0294"+
		"\u0266\3\2\2\2\u0294\u0268\3\2\2\2\u0294\u026a\3\2\2\2\u0294\u026c\3\2"+
		"\2\2\u0294\u026e\3\2\2\2\u0294\u0270\3\2\2\2\u0294\u0273\3\2\2\2\u0294"+
		"\u0276\3\2\2\2\u0294\u0286\3\2\2\2\u0294\u028e\3\2\2\2\u0294\u0291\3\2"+
		"\2\2\u0295=\3\2\2\2\u0296\u0297\7?\2\2\u0297\u029d\b \1\2\u0298\u0299"+
		"\7A\2\2\u0299\u029d\b \1\2\u029a\u029b\7@\2\2\u029b\u029d\b \1\2\u029c"+
		"\u0296\3\2\2\2\u029c\u0298\3\2\2\2\u029c\u029a\3\2\2\2\u029d?\3\2\2\2"+
		"\u029e\u029f\7E\2\2\u029f\u02a0\b!\1\2\u02a0A\3\2\2\2\u02a1\u02a2\7C\2"+
		"\2\u02a2\u02a6\b\"\1\2\u02a3\u02a4\7\b\2\2\u02a4\u02a6\b\"\1\2\u02a5\u02a1"+
		"\3\2\2\2\u02a5\u02a3\3\2\2\2\u02a6C\3\2\2\2\u02a7\u02a8\7\61\2\2\u02a8"+
		"\u02ae\b#\1\2\u02a9\u02aa\7\62\2\2\u02aa\u02ae\b#\1\2\u02ab\u02ac\7,\2"+
		"\2\u02ac\u02ae\b#\1\2\u02ad\u02a7\3\2\2\2\u02ad\u02a9\3\2\2\2\u02ad\u02ab"+
		"\3\2\2\2\u02aeE\3\2\2\2\u02af\u02b0\t\t\2\2\u02b0\u02b1\b$\1\2\u02b1G"+
		"\3\2\2\2\u02b2\u02b3\t\n\2\2\u02b3\u02b4\b%\1\2\u02b4I\3\2\2\2\u02b5\u02b6"+
		"\t\13\2\2\u02b6\u02b7\b&\1\2\u02b7K\3\2\2\2\u02b8\u02b9\t\f\2\2\u02b9"+
		"\u02ba\b\'\1\2\u02baM\3\2\2\2\u02bb\u02bc\t\r\2\2\u02bc\u02bd\b(\1\2\u02bd"+
		"O\3\2\2\2\u02be\u02bf\7 \2\2\u02bf\u02c0\b)\1\2\u02c0Q\3\2\2\2\u02c1\u02d2"+
		"\5\b\5\2\u02c2\u02c3\5T+\2\u02c3\u02cf\5\b\5\2\u02c4\u02c5\7\24\2\2\u02c5"+
		"\u02ca\b*\1\2\u02c6\u02cb\b*\1\2\u02c7\u02c8\5\b\5\2\u02c8\u02c9\5T+\2"+
		"\u02c9\u02cb\3\2\2\2\u02ca\u02c6\3\2\2\2\u02ca\u02c7\3\2\2\2\u02cb\u02cc"+
		"\3\2\2\2\u02cc\u02ce\5\b\5\2\u02cd\u02c4\3\2\2\2\u02ce\u02d1\3\2\2\2\u02cf"+
		"\u02cd\3\2\2\2\u02cf\u02d0\3\2\2\2\u02d0\u02d3\3\2\2\2\u02d1\u02cf\3\2"+
		"\2\2\u02d2\u02c2\3\2\2\2\u02d2\u02d3\3\2\2\2\u02d3\u02e4\3\2\2\2\u02d4"+
		"\u02d5\5\b\5\2\u02d5\u02df\b*\1\2\u02d6\u02d7\7\24\2\2\u02d7\u02dc\b*"+
		"\1\2\u02d8\u02dd\b*\1\2\u02d9\u02da\5\b\5\2\u02da\u02db\5T+\2\u02db\u02dd"+
		"\3\2\2\2\u02dc\u02d8\3\2\2\2\u02dc\u02d9\3\2\2\2\u02dd\u02de\3\2\2\2\u02de"+
		"\u02e0\5\b\5\2\u02df\u02d6\3\2\2\2\u02e0\u02e1\3\2\2\2\u02e1\u02df\3\2"+
		"\2\2\u02e1\u02e2\3\2\2\2\u02e2\u02e4\3\2\2\2\u02e3\u02c1\3\2\2\2\u02e3"+
		"\u02d4\3\2\2\2\u02e4S\3\2\2\2\u02e5\u02e6\5\22\n\2\u02e6\u02e7\b+\1\2"+
		"\u02e7\u0300\3\2\2\2\u02e8\u02f1\b+\1\2\u02e9\u02ea\7C\2\2\u02ea\u02f2"+
		"\b+\1\2\u02eb\u02ec\7\b\2\2\u02ec\u02f2\b+\1\2\u02ed\u02ee\7+\2\2\u02ee"+
		"\u02f2\b+\1\2\u02ef\u02f0\7E\2\2\u02f0\u02f2\b+\1\2\u02f1\u02e9\3\2\2"+
		"\2\u02f1\u02eb\3\2\2\2\u02f1\u02ed\3\2\2\2\u02f1\u02ef\3\2\2\2\u02f2\u02f3"+
		"\3\2\2\2\u02f3\u02f4\5\b\5\2\u02f4\u02f5\7\17\2\2\u02f5\u02fb\b+\1\2\u02f6"+
		"\u02f7\5\b\5\2\u02f7\u02f8\5\22\n\2\u02f8\u02f9\b+\1\2\u02f9\u02fc\3\2"+
		"\2\2\u02fa\u02fc\3\2\2\2\u02fb\u02f6\3\2\2\2\u02fb\u02fa\3\2\2\2\u02fc"+
		"\u02fd\3\2\2\2\u02fd\u02fe\b+\1\2\u02fe\u0300\3\2\2\2\u02ff\u02e5\3\2"+
		"\2\2\u02ff\u02e8\3\2\2\2\u0300U\3\2\2\29\\ot\u0083\u0085\u008b\u008d\u0092"+
		"\u0098\u009a\u009d\u00b7\u00ba\u00bf\u00c9\u00cd\u00cf\u00e0\u00f0\u00fd"+
		"\u010d\u0123\u015b\u0160\u018c\u0198\u01a4\u01af\u01bb\u01c8\u01d3\u01df"+
		"\u01eb\u01f8\u0205\u0217\u0221\u022f\u0252\u0256\u0258\u0282\u0294\u029c"+
		"\u02a5\u02ad\u02ca\u02cf\u02d2\u02dc\u02e1\u02e3\u02f1\u02fb\u02ff";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
