/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.Argument;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.RCodeToken;
import com.oracle.truffle.r.runtime.parsermetadata.FunctionScope;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "this-escape"})
public class RParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION); }

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
		RULE_n_one = 4, RULE_n_multi = 5, RULE_n_multi_no_eof = 6, RULE_expr_wo_assign = 7, 
		RULE_sequence = 8, RULE_expr = 9, RULE_expr_or_assign = 10, RULE_if_expr = 11, 
		RULE_while_expr = 12, RULE_for_expr = 13, RULE_repeat_expr = 14, RULE_function = 15, 
		RULE_par_decl = 16, RULE_tilde_expr = 17, RULE_utilde_expr = 18, RULE_or_expr = 19, 
		RULE_and_expr = 20, RULE_not_expr = 21, RULE_comp_expr = 22, RULE_add_expr = 23, 
		RULE_mult_expr = 24, RULE_operator_expr = 25, RULE_colon_expr = 26, RULE_unary_expression = 27, 
		RULE_power_expr = 28, RULE_basic_expr = 29, RULE_simple_expr = 30, RULE_number = 31, 
		RULE_conststring = 32, RULE_id = 33, RULE_bool = 34, RULE_or_operator = 35, 
		RULE_and_operator = 36, RULE_comp_operator = 37, RULE_add_operator = 38, 
		RULE_mult_operator = 39, RULE_power_operator = 40, RULE_args = 41, RULE_arg_expr = 42;
	private static String[] makeRuleNames() {
		return new String[] {
			"script", "root_function", "statement", "n_", "n_one", "n_multi", "n_multi_no_eof", 
			"expr_wo_assign", "sequence", "expr", "expr_or_assign", "if_expr", "while_expr", 
			"for_expr", "repeat_expr", "function", "par_decl", "tilde_expr", "utilde_expr", 
			"or_expr", "and_expr", "not_expr", "comp_expr", "add_expr", "mult_expr", 
			"operator_expr", "colon_expr", "unary_expression", "power_expr", "basic_expr", 
			"simple_expr", "number", "conststring", "id", "bool", "or_operator", 
			"and_operator", "comp_operator", "add_operator", "mult_operator", "power_operator", 
			"args", "arg_expr"
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
	    * Adds an argument to a function call as a local variable to the given {@code functionScope}.
	    * This is valid because in the function body the supplied arguments behave as if they were
	    * local variables initialized with the value supplied and the name of the corresponding
	    * formal argument [R-lang-specification].
	    */
	    private static void addArgumentAsLocalVariable(FunctionScope functionScope, String argIdentifier) {
	        assert functionScope != null;
	        assert argIdentifier != null;
	        functionScope.addLocalVariable(argIdentifier, FrameSlotKind.Illegal);
	    }

	    private static FrameSlotKind infereType(RSyntaxNode rhs) {
	        // TODO
	        return FrameSlotKind.Illegal;
	    }

	    /**
	    * Helper function that potentially adds a local variable to the given set of local
	    * variables iff {@code lhs} is a syntax lookup and if the set of local variables is
	    * not null.
	    *
	    * Note that this method should only be called from assignment expressions.
	    */
	    private void maybeAddLocalVariable(FunctionScope functionScope, RSyntaxNode lhs, RSyntaxNode rhs) {
	        if (functionScope != null) {
	            if (lhs instanceof RSyntaxLookup) {
	                String identifier = ((RSyntaxLookup) lhs).getIdentifier();
	                FrameSlotKind type = infereType(rhs);
	                functionScope.addLocalVariable(identifier, type);
	            }
	        }
	    }

	    private RSyntaxNode lookup(SourceSection src, String symbol, boolean functionLookup) {
	        return builder.lookup(src, symbol, functionLookup, null);
	    }

	    private RSyntaxNode lookup(SourceSection src, String symbol, boolean functionLookup, FunctionScope functionScope) {
	        return builder.lookup(src, symbol, functionLookup, functionScope);
	    }

	    /**
	     * Helper function to create a function lookup for the symbol in a given token.
	     */
	    private RSyntaxNode functionLookup(Token op) {
	        return builder.lookup(src(op), argName(op.getText()), true, null);
	    }

	    private static String getSimpleFunctionName(RSyntaxNode assignedTo) {
	        if (assignedTo != null && assignedTo instanceof RSyntaxLookup) {
	            return ((RSyntaxLookup) assignedTo).getIdentifier();
	        } else {
	            return "null";
	        }
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(86);
			n_();
			setState(92);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(87);
					_localctx.s = statement(null);
					 _localctx.v.add(_localctx.s.v); 
					}
					} 
				}
				setState(94);
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

	@SuppressWarnings("CheckReturnValue")
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
		        FunctionScope functionScope = new FunctionScope(name);
		    
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(95);
			n_();
			setState(96);
			_localctx.op = match(FUNCTION);
			tok();
			setState(98);
			n_();
			setState(99);
			match(LPAR);
			tok();
			setState(101);
			n_();
			setState(116);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 6)) & ~0x3f) == 0 && ((1L << (_la - 6)) & 864691128455135233L) != 0)) {
				{
				setState(102);
				par_decl(params, functionScope);
				setState(111);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(103);
						n_();
						setState(104);
						match(COMMA);
						tok();
						setState(106);
						n_();
						setState(107);
						par_decl(params, functionScope);
						}
						} 
					}
					setState(113);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				}
				setState(114);
				n_();
				}
			}

			setState(118);
			match(RPAR);
			tok();
			setState(120);
			n_();
			setState(121);
			_localctx.body = expr_or_assign(functionScope);
			 _localctx.v =  builder.rootFunction(language, src(_localctx.op, last()), params, _localctx.body.v, name, functionScope); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public FunctionScope functionScope;
		public RSyntaxNode v;
		public Expr_or_assignContext e;
		public N_oneContext n_one() {
			return getRuleContext(N_oneContext.class,0);
		}
		public Expr_or_assignContext expr_or_assign() {
			return getRuleContext(Expr_or_assignContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public StatementContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_statement; }
	}

	public final StatementContext statement(FunctionScope functionScope) throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 4, RULE_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(124);
			_localctx.e = expr_or_assign(functionScope);
			setState(125);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(133);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(131);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case NEWLINE:
						{
						setState(128);
						match(NEWLINE);
						}
						break;
					case COMMENT:
						{
						setState(129);
						match(COMMENT);
						 checkFileDelim((CommonToken)last()); 
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(135);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(146);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COMMENT:
			case NEWLINE:
				enterOuterAlt(_localctx, 1);
				{
				setState(139); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						setState(139);
						_errHandler.sync(this);
						switch (_input.LA(1)) {
						case NEWLINE:
							{
							setState(136);
							match(NEWLINE);
							}
							break;
						case COMMENT:
							{
							setState(137);
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
					setState(141); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case EOF:
				enterOuterAlt(_localctx, 2);
				{
				setState(143);
				match(EOF);
				}
				break;
			case SEMICOLON:
				enterOuterAlt(_localctx, 3);
				{
				setState(144);
				match(SEMICOLON);
				setState(145);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(157);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COMMENT:
			case SEMICOLON:
			case NEWLINE:
				enterOuterAlt(_localctx, 1);
				{
				setState(152); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						setState(152);
						_errHandler.sync(this);
						switch (_input.LA(1)) {
						case NEWLINE:
							{
							setState(148);
							match(NEWLINE);
							}
							break;
						case COMMENT:
							{
							setState(149);
							match(COMMENT);
							 checkFileDelim((CommonToken)last()); 
							}
							break;
						case SEMICOLON:
							{
							setState(151);
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
					setState(154); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case EOF:
				enterOuterAlt(_localctx, 2);
				{
				setState(156);
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

	@SuppressWarnings("CheckReturnValue")
	public static class N_multi_no_eofContext extends ParserRuleContext {
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
		public N_multi_no_eofContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_multi_no_eof; }
	}

	public final N_multi_no_eofContext n_multi_no_eof() throws RecognitionException {
		N_multi_no_eofContext _localctx = new N_multi_no_eofContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_n_multi_no_eof);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(163); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(163);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case NEWLINE:
						{
						setState(159);
						match(NEWLINE);
						}
						break;
					case COMMENT:
						{
						setState(160);
						match(COMMENT);
						 checkFileDelim((CommonToken)last()); 
						}
						break;
					case SEMICOLON:
						{
						setState(162);
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
				setState(165); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	@SuppressWarnings("CheckReturnValue")
	public static class Expr_wo_assignContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Expr_wo_assignContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Expr_wo_assignContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_expr_wo_assign; }
	}

	public final Expr_wo_assignContext expr_wo_assign(FunctionScope functionScope) throws RecognitionException {
		Expr_wo_assignContext _localctx = new Expr_wo_assignContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 14, RULE_expr_wo_assign);
		int _la;
		try {
			setState(194);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WHILE:
				enterOuterAlt(_localctx, 1);
				{
				setState(167);
				_localctx.w = while_expr(functionScope);
				 _localctx.v =  _localctx.w.v; 
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 2);
				{
				setState(170);
				_localctx.i = if_expr(functionScope);
				 _localctx.v =  _localctx.i.v; 
				}
				break;
			case FOR:
				enterOuterAlt(_localctx, 3);
				{
				setState(173);
				_localctx.f = for_expr(functionScope);
				 _localctx.v =  _localctx.f.v; 
				}
				break;
			case REPEAT:
				enterOuterAlt(_localctx, 4);
				{
				setState(176);
				_localctx.r = repeat_expr(functionScope);
				 _localctx.v =  _localctx.r.v; 
				}
				break;
			case FUNCTION:
				enterOuterAlt(_localctx, 5);
				{
				setState(179);
				_localctx.fun = function(null);
				 _localctx.v =  _localctx.fun.v; 
				}
				break;
			case NEXT:
			case BREAK:
				enterOuterAlt(_localctx, 6);
				{
				setState(182);
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
				setState(191);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(184);
					match(LPAR);
					tok();
					setState(186);
					args(null);
					setState(187);
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

	@SuppressWarnings("CheckReturnValue")
	public static class SequenceContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public List<N_multi_no_eofContext> n_multi_no_eof() {
			return getRuleContexts(N_multi_no_eofContext.class);
		}
		public N_multi_no_eofContext n_multi_no_eof(int i) {
			return getRuleContext(N_multi_no_eofContext.class,i);
		}
		public SequenceContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public SequenceContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_sequence; }
	}

	public final SequenceContext sequence(FunctionScope functionScope) throws RecognitionException {
		SequenceContext _localctx = new SequenceContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 16, RULE_sequence);

		        ArrayList<Argument<RSyntaxNode>> stmts = new ArrayList<>();
		    
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(196);
			_localctx.op = match(LBRACE);
			tok();
			setState(199);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(198);
				n_multi();
				}
				break;
			}
			setState(215);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(201);
				_localctx.e = expr_or_assign(functionScope);
				 stmts.add(RCodeBuilder.argument(_localctx.e.v)); 
				setState(209);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(203);
						n_multi_no_eof();
						setState(204);
						_localctx.e = expr_or_assign(functionScope);
						 stmts.add(RCodeBuilder.argument(_localctx.e.v)); 
						}
						} 
					}
					setState(211);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
				}
				setState(213);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - -1)) & ~0x3f) == 0 && ((1L << (_la - -1)) & 2305843009213956101L) != 0)) {
					{
					setState(212);
					n_multi();
					}
				}

				}
				break;
			}
			setState(217);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public ExprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	}

	public final ExprContext expr(FunctionScope functionScope) throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 18, RULE_expr);
		 Token start = getInputStream().LT(1); RSyntaxNode rhs = null; 
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			_localctx.l = tilde_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(248);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ARROW:
			case SUPER_ARROW:
				{
				setState(223);
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
				setState(225);
				n_();
				setState(232);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(226);
					_localctx.r1 = function(_localctx.l.v);
					 rhs = _localctx.r1.v; 
					}
					break;
				case 2:
					{
					setState(229);
					_localctx.r2 = expr(functionScope);
					 rhs = _localctx.r2.v; 
					}
					break;
				}

				         _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.l.v, rhs);
				         maybeAddLocalVariable(functionScope, _localctx.l.v, rhs);
				       
				}
				break;
			case RIGHT_ARROW:
				{
				setState(236);
				_localctx.op = match(RIGHT_ARROW);
				tok();
				setState(238);
				n_();
				setState(239);
				_localctx.r3 = expr(functionScope);

				          _localctx.v =  builder.call(src(start, last()), lookup(src(_localctx.op), "<-", true, functionScope), _localctx.r3.v, _localctx.l.v);
				          maybeAddLocalVariable(functionScope, _localctx.r3.v, _localctx.l.v);
				        
				}
				break;
			case SUPER_RIGHT_ARROW:
				{
				setState(242);
				_localctx.op = match(SUPER_RIGHT_ARROW);
				tok();
				setState(244);
				n_();
				setState(245);
				_localctx.r4 = expr(functionScope);
				 _localctx.v =  builder.call(src(start, last()), lookup(src(_localctx.op), "<<-", true, functionScope), _localctx.r4.v, _localctx.l.v); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class Expr_or_assignContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Expr_or_assignContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Expr_or_assignContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_expr_or_assign; }
	}

	public final Expr_or_assignContext expr_or_assign(FunctionScope functionScope) throws RecognitionException {
		Expr_or_assignContext _localctx = new Expr_or_assignContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 20, RULE_expr_or_assign);
		 Token start = getInputStream().LT(1); RSyntaxNode rhs = null; 
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(250);
			_localctx.l = tilde_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(277);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				{
				setState(252);
				_localctx.op = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 8204L) != 0)) ) {
					_localctx.op = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok();
				setState(254);
				n_();
				setState(261);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
				case 1:
					{
					setState(255);
					_localctx.r1 = function(_localctx.l.v);
					 rhs = _localctx.r1.v; 
					}
					break;
				case 2:
					{
					setState(258);
					_localctx.r2 = expr_or_assign(functionScope);
					 rhs = _localctx.r2.v; 
					}
					break;
				}

				          _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.l.v, rhs);
				          maybeAddLocalVariable(functionScope, _localctx.l.v, rhs);
				        
				}
				break;
			case 2:
				{
				setState(265);
				_localctx.op = match(RIGHT_ARROW);
				tok();
				setState(267);
				n_();
				setState(268);
				_localctx.r3 = expr_or_assign(functionScope);

				          _localctx.v =  builder.call(src(start, last()), lookup(src(_localctx.op), "<-", true, functionScope), _localctx.r3.v, _localctx.l.v);
				          maybeAddLocalVariable(functionScope, _localctx.r3.v, _localctx.l.v);
				        
				}
				break;
			case 3:
				{
				setState(271);
				_localctx.op = match(SUPER_RIGHT_ARROW);
				tok();
				setState(273);
				n_();
				setState(274);
				_localctx.r4 = expr_or_assign(functionScope);
				 _localctx.v =  builder.call(src(start, last()), lookup(src(_localctx.op), "<<-", true, functionScope), _localctx.r4.v, _localctx.l.v); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class If_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public If_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public If_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_if_expr; }
	}

	public final If_exprContext if_expr(FunctionScope functionScope) throws RecognitionException {
		If_exprContext _localctx = new If_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 22, RULE_if_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(279);
			_localctx.op = match(IF);
			tok();
			setState(281);
			n_();
			setState(282);
			match(LPAR);
			tok();
			setState(284);
			n_();
			setState(285);
			_localctx.cond = expr_or_assign(functionScope);
			setState(286);
			n_();
			setState(287);
			match(RPAR);
			tok();
			setState(289);
			n_();
			setState(290);
			_localctx.t = expr_or_assign(functionScope);
			setState(299);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				{
				{
				setState(291);
				n_();
				setState(292);
				match(ELSE);
				tok();
				setState(294);
				n_();
				setState(295);
				_localctx.f = expr_or_assign(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
	public static class While_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public While_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public While_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_while_expr; }
	}

	public final While_exprContext while_expr(FunctionScope functionScope) throws RecognitionException {
		While_exprContext _localctx = new While_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 24, RULE_while_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			_localctx.op = match(WHILE);
			tok();
			setState(303);
			n_();
			setState(304);
			match(LPAR);
			tok();
			setState(306);
			n_();
			setState(307);
			_localctx.c = expr_or_assign(functionScope);
			setState(308);
			n_();
			setState(309);
			match(RPAR);
			tok();
			setState(311);
			n_();
			setState(312);
			_localctx.body = expr_or_assign(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
	public static class For_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public For_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public For_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_for_expr; }
	}

	public final For_exprContext for_expr(FunctionScope functionScope) throws RecognitionException {
		For_exprContext _localctx = new For_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 26, RULE_for_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(315);
			_localctx.op = match(FOR);
			tok();
			setState(317);
			n_();
			setState(318);
			match(LPAR);
			tok();
			setState(320);
			n_();
			setState(321);
			_localctx.i = match(ID);
			setState(322);
			n_();
			setState(323);
			match(IN);
			tok();
			setState(325);
			n_();
			setState(326);
			_localctx.in = expr_or_assign(functionScope);
			setState(327);
			n_();
			setState(328);
			match(RPAR);
			tok();
			setState(330);
			n_();
			setState(331);
			_localctx.body = expr_or_assign(functionScope);
			 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), lookup(src(_localctx.i), _localctx.i.getText(), false, functionScope), _localctx.in.v, _localctx.body.v); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class Repeat_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Repeat_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Repeat_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_repeat_expr; }
	}

	public final Repeat_exprContext repeat_expr(FunctionScope functionScope) throws RecognitionException {
		Repeat_exprContext _localctx = new Repeat_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 28, RULE_repeat_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(334);
			_localctx.op = match(REPEAT);
			tok();
			setState(336);
			n_();
			setState(337);
			_localctx.body = expr_or_assign(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 30, RULE_function);

		        List<Argument<RSyntaxNode>> params = new ArrayList<>();
		        FunctionScope functionScope = new FunctionScope(getSimpleFunctionName(assignedTo));
		    
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			_localctx.op = match(FUNCTION);
			tok();
			setState(342);
			n_();
			setState(343);
			match(LPAR);
			tok();
			setState(345);
			n_();
			setState(360);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 6)) & ~0x3f) == 0 && ((1L << (_la - 6)) & 864691128455135233L) != 0)) {
				{
				setState(346);
				par_decl(params, functionScope);
				setState(355);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(347);
						n_();
						setState(348);
						match(COMMA);
						tok();
						setState(350);
						n_();
						setState(351);
						par_decl(params, functionScope);
						}
						} 
					}
					setState(357);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
				}
				setState(358);
				n_();
				}
			}

			setState(362);
			match(RPAR);
			tok();
			setState(364);
			n_();
			setState(365);
			_localctx.body = expr_or_assign(functionScope);
			 _localctx.v =  builder.function(language, src(_localctx.op, last()), params, _localctx.body.v, assignedTo, functionScope); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class Par_declContext extends ParserRuleContext {
		public List<Argument<RSyntaxNode>> l;
		public FunctionScope functionScope;
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
		public Par_declContext(ParserRuleContext parent, int invokingState, List<Argument<RSyntaxNode>> l, FunctionScope functionScope) {
			super(parent, invokingState);
			this.l = l;
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_par_decl; }
	}

	public final Par_declContext par_decl(List<Argument<RSyntaxNode>> l,FunctionScope functionScope) throws RecognitionException {
		Par_declContext _localctx = new Par_declContext(_ctx, getState(), l, functionScope);
		enterRule(_localctx, 32, RULE_par_decl);
		try {
			setState(404);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(368);
				_localctx.i = match(ID);
				tok();
				 addArgumentAsLocalVariable(functionScope, _localctx.i.getText()); _localctx.l.add(argument(src(_localctx.i), _localctx.i.getText(), null)); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(371);
				_localctx.i = match(ID);
				tok();
				setState(373);
				n_();
				setState(374);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_FORMALS);
				setState(376);
				n_();
				setState(377);
				_localctx.e = expr(functionScope);
				 addArgumentAsLocalVariable(functionScope, _localctx.i.getText()); _localctx.l.add(argument(src(_localctx.i, last()), _localctx.i.getText(), _localctx.e.v)); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(380);
				_localctx.v = match(VARIADIC);
				tok();
				 _localctx.l.add(argument(src(_localctx.v), _localctx.v.getText(), null)); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(383);
				_localctx.v = match(VARIADIC);
				tok();
				setState(385);
				n_();
				setState(386);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_FORMALS);
				setState(388);
				n_();
				setState(389);
				_localctx.e = expr(null);
				 _localctx.l.add(argument(src(_localctx.v), _localctx.v.getText(),  null)); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(392);
				_localctx.v = match(DD);
				tok();
				 _localctx.l.add(argument(src(_localctx.v), _localctx.v.getText(), null)); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(395);
				_localctx.v = match(DD);
				tok();
				setState(397);
				n_();
				setState(398);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_FORMALS);
				setState(400);
				n_();
				setState(401);
				expr(null);
				 _localctx.l.add(argument(src(_localctx.v), _localctx.v.getText(), null)); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class Tilde_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Tilde_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Tilde_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_tilde_expr; }
	}

	public final Tilde_exprContext tilde_expr(FunctionScope functionScope) throws RecognitionException {
		Tilde_exprContext _localctx = new Tilde_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 34, RULE_tilde_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(406);
			_localctx.l = utilde_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(416);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(408);
					_localctx.op = match(TILDE);
					tok();
					setState(410);
					n_();
					setState(411);
					_localctx.r = utilde_expr(functionScope);
					 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(418);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Utilde_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Utilde_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Utilde_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_utilde_expr; }
	}

	public final Utilde_exprContext utilde_expr(FunctionScope functionScope) throws RecognitionException {
		Utilde_exprContext _localctx = new Utilde_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 36, RULE_utilde_expr);
		try {
			setState(428);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(419);
				_localctx.op = match(TILDE);
				tok();
				setState(421);
				n_();
				setState(422);
				_localctx.l1 = utilde_expr(functionScope);
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.l1.v); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(425);
				_localctx.l2 = or_expr(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Or_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Or_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Or_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_or_expr; }
	}

	public final Or_exprContext or_expr(FunctionScope functionScope) throws RecognitionException {
		Or_exprContext _localctx = new Or_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 38, RULE_or_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(430);
			_localctx.l = and_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(439);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(432);
					_localctx.op = or_operator();
					setState(433);
					n_();
					setState(434);
					_localctx.r = and_expr(functionScope);
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(441);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
	public static class And_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public And_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public And_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_and_expr; }
	}

	public final And_exprContext and_expr(FunctionScope functionScope) throws RecognitionException {
		And_exprContext _localctx = new And_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 40, RULE_and_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(442);
			_localctx.l = not_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(451);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(444);
					_localctx.op = and_operator();
					setState(445);
					n_();
					setState(446);
					_localctx.r = not_expr(functionScope);
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(453);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Not_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Not_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Not_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_not_expr; }
	}

	public final Not_exprContext not_expr(FunctionScope functionScope) throws RecognitionException {
		Not_exprContext _localctx = new Not_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 42, RULE_not_expr);
		try {
			setState(464);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(454);
				if (!(true)) throw new FailedPredicateException(this, "true");
				setState(455);
				_localctx.op = match(NOT);
				tok();
				setState(457);
				n_();
				setState(458);
				_localctx.l = not_expr(functionScope);
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.l.v); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(461);
				_localctx.b = comp_expr(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Comp_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Comp_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Comp_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_comp_expr; }
	}

	public final Comp_exprContext comp_expr(FunctionScope functionScope) throws RecognitionException {
		Comp_exprContext _localctx = new Comp_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 44, RULE_comp_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(466);
			_localctx.l = add_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(475);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,32,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(468);
					_localctx.op = comp_operator();
					setState(469);
					n_();
					setState(470);
					_localctx.r = add_expr(functionScope);
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(477);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Add_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Add_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Add_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_add_expr; }
	}

	public final Add_exprContext add_expr(FunctionScope functionScope) throws RecognitionException {
		Add_exprContext _localctx = new Add_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 46, RULE_add_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(478);
			_localctx.l = mult_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(487);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(480);
					_localctx.op = add_operator();
					setState(481);
					n_();
					setState(482);
					_localctx.r = mult_expr(functionScope);
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(489);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Mult_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Mult_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Mult_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_mult_expr; }
	}

	public final Mult_exprContext mult_expr(FunctionScope functionScope) throws RecognitionException {
		Mult_exprContext _localctx = new Mult_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 48, RULE_mult_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(490);
			_localctx.l = operator_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(499);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(492);
					_localctx.op = mult_operator();
					setState(493);
					n_();
					setState(494);
					_localctx.r = operator_expr(functionScope);
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op.v), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(501);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Operator_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Operator_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Operator_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_operator_expr; }
	}

	public final Operator_exprContext operator_expr(FunctionScope functionScope) throws RecognitionException {
		Operator_exprContext _localctx = new Operator_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 50, RULE_operator_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(502);
			_localctx.l = colon_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(512);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(504);
					_localctx.op = match(OP);
					tok();
					setState(506);
					n_();
					setState(507);
					_localctx.r = colon_expr(functionScope);
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, _localctx.r.v); 
					}
					} 
				}
				setState(514);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Colon_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Colon_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Colon_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_colon_expr; }
	}

	public final Colon_exprContext colon_expr(FunctionScope functionScope) throws RecognitionException {
		Colon_exprContext _localctx = new Colon_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 52, RULE_colon_expr);
		 Token start = getInputStream().LT(1); 
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(515);
			_localctx.l = unary_expression(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(525);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					{
					setState(517);
					_localctx.op = match(COLON);
					tok();
					setState(519);
					n_();
					setState(520);
					_localctx.r = unary_expression(functionScope);
					 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, _localctx.r.v); 
					}
					}
					} 
				}
				setState(527);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Unary_expressionContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Unary_expressionContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Unary_expressionContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_unary_expression; }
	}

	public final Unary_expressionContext unary_expression(FunctionScope functionScope) throws RecognitionException {
		Unary_expressionContext _localctx = new Unary_expressionContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 54, RULE_unary_expression);
		int _la;
		try {
			setState(543);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOT:
			case QM:
			case PLUS:
			case MINUS:
				enterOuterAlt(_localctx, 1);
				{
				setState(528);
				_localctx.op = _input.LT(1);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 167503724544L) != 0)) ) {
					_localctx.op = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok();
				setState(530);
				n_();
				setState(531);
				_localctx.l1 = unary_expression(functionScope);
				 _localctx.v =  builder.call(src(_localctx.op, last()), operator(_localctx.op), _localctx.l1.v); 
				}
				break;
			case TILDE:
				enterOuterAlt(_localctx, 2);
				{
				setState(534);
				_localctx.op = match(TILDE);
				tok();
				setState(536);
				n_();
				setState(537);
				_localctx.l2 = utilde_expr(functionScope);
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
				setState(540);
				_localctx.b = power_expr(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Power_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Power_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Power_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_power_expr; }
	}

	public final Power_exprContext power_expr(FunctionScope functionScope) throws RecognitionException {
		Power_exprContext _localctx = new Power_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 56, RULE_power_expr);
		 Token start = getInputStream().LT(1); 
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(545);
			_localctx.l = basic_expr(functionScope);
			 _localctx.v =  _localctx.l.v; 
			setState(553);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				{
				{
				setState(547);
				_localctx.op = power_operator();
				setState(548);
				n_();
				setState(549);
				_localctx.r = unary_expression(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Basic_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Basic_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Basic_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_basic_expr; }
	}

	public final Basic_exprContext basic_expr(FunctionScope functionScope) throws RecognitionException {
		Basic_exprContext _localctx = new Basic_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 58, RULE_basic_expr);
		 Token start = getInputStream().LT(1); 
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(567);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				{
				{
				setState(555);
				_localctx.lhsToken = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 6)) & ~0x3f) == 0 && ((1L << (_la - 6)) & 3170534137668829185L) != 0)) ) {
					_localctx.lhsToken = _errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				tok(RCodeToken.SYMBOL_FUNCTION_CALL);
				setState(557);
				_localctx.op = match(LPAR);
				tok();
				setState(559);
				_localctx.a = args(null);
				setState(560);
				_localctx.y = match(RPAR);
				tok();
				 _localctx.v =  builder.call(src(start, _localctx.y), functionLookup(_localctx.lhsToken), _localctx.a.v); 
				}
				}
				break;
			case 2:
				{
				setState(564);
				_localctx.lhs = simple_expr(functionScope);
				 _localctx.v =  _localctx.lhs.v; 
				}
				break;
			}
			setState(608);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				{
				setState(604); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(602);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
						case 1:
							{
							{
							setState(569);
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
							setState(571);
							n_();
							setState(572);
							_localctx.name = id();
							 modifyTok(RCodeToken.SLOT); _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, lookup(src(_localctx.name.v), _localctx.name.v.getText(), false, functionScope)); 
							}
							}
							break;
						case 2:
							{
							{
							setState(575);
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
							setState(577);
							n_();
							setState(578);
							_localctx.sname = conststring();
							 _localctx.v =  builder.call(src(start, last()), operator(_localctx.op), _localctx.v, _localctx.sname.v); 
							}
							}
							break;
						case 3:
							{
							{
							setState(581);
							_localctx.op = match(LBRAKET);
							tok();
							setState(583);
							_localctx.subset = args(_localctx.v);
							setState(584);
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
							setState(587);
							_localctx.op = match(LBB);
							tok();
							setState(589);
							_localctx.subscript = args(_localctx.v);
							setState(590);
							match(RBRAKET);
							tok();
							setState(592);
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
							setState(595);
							_localctx.op = match(LPAR);
							tok();
							setState(597);
							_localctx.a = args(null);
							setState(598);
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
					setState(606); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
	public static class Simple_exprContext extends ParserRuleContext {
		public FunctionScope functionScope;
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
		public Simple_exprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public Simple_exprContext(ParserRuleContext parent, int invokingState, FunctionScope functionScope) {
			super(parent, invokingState);
			this.functionScope = functionScope;
		}
		@Override public int getRuleIndex() { return RULE_simple_expr; }
	}

	public final Simple_exprContext simple_expr(FunctionScope functionScope) throws RecognitionException {
		Simple_exprContext _localctx = new Simple_exprContext(_ctx, getState(), functionScope);
		enterRule(_localctx, 60, RULE_simple_expr);
		 Token start = getInputStream().LT(1); List<Argument<RSyntaxNode>> args = new ArrayList<>(); Token compToken = null; 
		int _la;
		try {
			setState(668);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(610);
				_localctx.i = id();
				 _localctx.v =  lookup(src(_localctx.i.v), (_localctx.i!=null?_input.getText(_localctx.i.start,_localctx.i.stop):null), false, functionScope); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(613);
				_localctx.b = bool();
				 _localctx.v =  builder.constant(src(start, last()), _localctx.b.v); 
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(616);
				_localctx.d = match(DD);
				 tok(); _localctx.v =  lookup(src(_localctx.d), _localctx.d.getText(), false, functionScope); 
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(618);
				_localctx.t = match(NULL);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RNull.instance); 
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(620);
				_localctx.t = match(INF);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), Double.POSITIVE_INFINITY); 
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(622);
				_localctx.t = match(NAN);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), Double.NaN); 
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(624);
				_localctx.t = match(NAINT);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RRuntime.INT_NA); 
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(626);
				_localctx.t = match(NAREAL);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RRuntime.DOUBLE_NA); 
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(628);
				_localctx.t = match(NACHAR);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RRuntime.STRING_NA); 
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(630);
				_localctx.t = match(NACOMPL);
				 tok(); _localctx.v =  builder.constant(src(_localctx.t), RComplex.createNA()); 
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(632);
				_localctx.num = number();
				 _localctx.v =  _localctx.num.v; 
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(635);
				_localctx.cstr = conststring();
				 _localctx.v =  _localctx.cstr.v; 
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(638);
				_localctx.pkg = id();
				modifyTok(RCodeToken.SYMBOL_PACKAGE);
				setState(640);
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
				setState(642);
				n_();

				        SourceSection pkgSource = src(_localctx.pkg.v);
				        args.add(argument(pkgSource, (String) null, lookup(pkgSource, (_localctx.pkg!=null?_input.getText(_localctx.pkg.start,_localctx.pkg.stop):null), false, functionScope)));
				        
				setState(650);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case VARIADIC:
				case ID:
					{
					setState(644);
					_localctx.compId = id();

					        SourceSection compSource = src(_localctx.compId.v);
					        compToken = _localctx.compId.v;
					        args.add(argument(compSource, (String) null, lookup(compSource, (_localctx.compId!=null?_input.getText(_localctx.compId.start,_localctx.compId.stop):null), false, functionScope)));
					        
					}
					break;
				case STRING:
					{
					setState(647);
					_localctx.compString = match(STRING);
					tok();

					        SourceSection compSource = src(_localctx.compString);
					        compToken = _localctx.compString;
					        args.add(argument(compSource, (String) null, builder.constant(compSource, _localctx.compString.getText())));
					        
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
				setState(654);
				_localctx.op = match(LPAR);
				tok();
				setState(656);
				n_();
				setState(657);
				_localctx.ea = expr_or_assign(functionScope);
				setState(658);
				n_();
				setState(659);
				_localctx.y = match(RPAR);
				 tok(); _localctx.v =  builder.call(src(_localctx.op, _localctx.y), operator(_localctx.op), _localctx.ea.v); 
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(662);
				_localctx.s = sequence(functionScope);
				 _localctx.v =  _localctx.s.v; 
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(665);
				_localctx.e = expr_wo_assign(functionScope);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 62, RULE_number);
		try {
			setState(676);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INTEGER:
				enterOuterAlt(_localctx, 1);
				{
				setState(670);
				_localctx.i = match(INTEGER);
				 tok();
				        double value = RRuntime.string2doubleNoCheck(_localctx.i.getText());
				        if (value == (int) value) {
				            if (_localctx.i.getText().indexOf('.') != -1) {
				                RError.warning(RError.NO_CALLER, RError.Message.INTEGER_VALUE_UNNECESARY_DECIMAL, _localctx.i.getText() + "L");
				            }
				            _localctx.v =  builder.constant(src(_localctx.i), (int) value);
				        } else {
				            if (_localctx.i.getText().indexOf('.') != -1) {
				                RError.warning(RError.NO_CALLER, RError.Message.INTEGER_VALUE_DECIMAL, _localctx.i.getText() + "L");
				            } else if (_localctx.i.getText().startsWith("0x")) {
				                RError.warning(RError.NO_CALLER, RError.Message.NON_INTEGER_VALUE, _localctx.i.getText());
				            } else {
				                RError.warning(RError.NO_CALLER, RError.Message.NON_INTEGER_VALUE, _localctx.i.getText() + "L");
				            }
				            _localctx.v =  builder.constant(src(_localctx.i), value);
				        }
				      
				}
				break;
			case DOUBLE:
				enterOuterAlt(_localctx, 2);
				{
				setState(672);
				_localctx.d = match(DOUBLE);
				 tok(); _localctx.v =  builder.constant(src(_localctx.d), RRuntime.string2doubleNoCheck(_localctx.d.getText())); 
				}
				break;
			case COMPLEX:
				enterOuterAlt(_localctx, 3);
				{
				setState(674);
				_localctx.c = match(COMPLEX);
				 tok(); _localctx.v =  builder.constant(src(_localctx.c), RComplex.valueOf(0, RRuntime.string2doubleNoCheck(_localctx.c.getText()))); 
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 64, RULE_conststring);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(678);
			_localctx.s = match(STRING);
			 tok(); _localctx.v =  builder.constant(src(_localctx.s), _localctx.s.getText()); 
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 66, RULE_id);
		try {
			setState(685);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(681);
				_localctx.ident = match(ID);
				 tok(); _localctx.v =  _localctx.ident; 
				}
				break;
			case VARIADIC:
				enterOuterAlt(_localctx, 2);
				{
				setState(683);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 68, RULE_bool);
		try {
			setState(693);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(687);
				_localctx.t = match(TRUE);
				 tok(); _localctx.v =  RRuntime.LOGICAL_TRUE; 
				}
				break;
			case FALSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(689);
				_localctx.t = match(FALSE);
				 tok(); _localctx.v =  RRuntime.LOGICAL_FALSE; 
				}
				break;
			case NA:
				enterOuterAlt(_localctx, 3);
				{
				setState(691);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 70, RULE_or_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(695);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 72, RULE_and_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(698);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 74, RULE_comp_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(701);
			_localctx.op = _input.LT(1);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 8064L) != 0)) ) {
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 76, RULE_add_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(704);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 78, RULE_mult_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(707);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 80, RULE_power_operator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(710);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 82, RULE_args);

		              _localctx.v =  new ArrayList<>();
		              if (firstArg != null) {
		                  _localctx.v.add(RCodeBuilder.argument(firstArg));
		              }
		          
		int _la;
		try {
			setState(747);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,53,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(713);
				n_();
				setState(730);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,50,_ctx) ) {
				case 1:
					{
					setState(714);
					arg_expr(_localctx.v);
					setState(715);
					n_();
					setState(727);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(716);
						match(COMMA);
						tok();
						setState(722);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
						case 1:
							{
							 _localctx.v.add(RCodeBuilder.argumentEmpty()); 
							}
							break;
						case 2:
							{
							setState(719);
							n_();
							setState(720);
							arg_expr(_localctx.v);
							}
							break;
						}
						setState(724);
						n_();
						}
						}
						setState(729);
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
				setState(732);
				n_();
				 _localctx.v.add(RCodeBuilder.argumentEmpty()); 
				setState(743); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(734);
					match(COMMA);
					tok();
					setState(740);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
					case 1:
						{
						 _localctx.v.add(RCodeBuilder.argumentEmpty()); 
						}
						break;
					case 2:
						{
						setState(737);
						n_();
						setState(738);
						arg_expr(_localctx.v);
						}
						break;
					}
					setState(742);
					n_();
					}
					}
					setState(745); 
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 84, RULE_arg_expr);
		 Token start = getInputStream().LT(1); 
		try {
			setState(775);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(749);
				_localctx.e = expr(null);
				 _localctx.l.add(argument(src(start, last()), (String) null, _localctx.e.v)); 
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				 Token name = null; RSyntaxNode value = null; 
				setState(761);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ID:
					{
					setState(753);
					_localctx.ID = match(ID);
					name = _localctx.ID; tok(RCodeToken.SYMBOL_SUB);
					}
					break;
				case VARIADIC:
					{
					setState(755);
					_localctx.VARIADIC = match(VARIADIC);
					name=_localctx.VARIADIC; tok();
					}
					break;
				case NULL:
					{
					setState(757);
					_localctx.NULL = match(NULL);
					name = _localctx.NULL; tok();
					}
					break;
				case STRING:
					{
					setState(759);
					_localctx.STRING = match(STRING);
					name = _localctx.STRING; tok();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(763);
				n_();
				setState(764);
				_localctx.a = match(ASSIGN);
				tok(RCodeToken.EQ_SUB);
				setState(771);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
				case 1:
					{
					setState(766);
					n_();
					setState(767);
					_localctx.e = expr(null);
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
		case 21:
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
		"\u0004\u0001C\u030a\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0005\u0000[\b\u0000\n\u0000\f\u0000^\t\u0000\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0005\u0001n\b\u0001\n\u0001\f\u0001q\t\u0001\u0001\u0001"+
		"\u0001\u0001\u0003\u0001u\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0005\u0003\u0084\b\u0003"+
		"\n\u0003\f\u0003\u0087\t\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0004"+
		"\u0004\u008c\b\u0004\u000b\u0004\f\u0004\u008d\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004\u0093\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0004\u0005\u0099\b\u0005\u000b\u0005\f\u0005\u009a\u0001"+
		"\u0005\u0003\u0005\u009e\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0004\u0006\u00a4\b\u0006\u000b\u0006\f\u0006\u00a5\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007"+
		"\u00c0\b\u0007\u0001\u0007\u0003\u0007\u00c3\b\u0007\u0001\b\u0001\b\u0001"+
		"\b\u0003\b\u00c8\b\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0005"+
		"\b\u00d0\b\b\n\b\f\b\u00d3\t\b\u0001\b\u0003\b\u00d6\b\b\u0003\b\u00d8"+
		"\b\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003\t\u00e9\b\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003\t\u00f9\b\t\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0003"+
		"\n\u0106\b\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0003\n\u0116\b\n\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0003\u000b\u012c\b\u000b\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0005\u000f\u0162\b\u000f\n\u000f\f\u000f"+
		"\u0165\t\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u0169\b\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0003"+
		"\u0010\u0195\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u019f\b\u0011\n"+
		"\u0011\f\u0011\u01a2\t\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0003"+
		"\u0012\u01ad\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0005\u0013\u01b6\b\u0013\n\u0013\f\u0013"+
		"\u01b9\t\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0005\u0014\u01c2\b\u0014\n\u0014\f\u0014\u01c5"+
		"\t\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u01d1"+
		"\b\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0005\u0016\u01da\b\u0016\n\u0016\f\u0016\u01dd\t\u0016"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0005\u0017\u01e6\b\u0017\n\u0017\f\u0017\u01e9\t\u0017\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0005\u0018\u01f2\b\u0018\n\u0018\f\u0018\u01f5\t\u0018\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019"+
		"\u0001\u0019\u0005\u0019\u01ff\b\u0019\n\u0019\f\u0019\u0202\t\u0019\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001"+
		"\u001a\u0001\u001a\u0005\u001a\u020c\b\u001a\n\u001a\f\u001a\u020f\t\u001a"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u0220\b\u001b\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0003\u001c\u022a\b\u001c\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0003\u001d\u0238\b\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0003\u001d\u025b\b\u001d\u0004\u001d\u025d\b"+
		"\u001d\u000b\u001d\f\u001d\u025e\u0003\u001d\u0261\b\u001d\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u028b\b\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u029d\b\u001e\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f"+
		"\u02a5\b\u001f\u0001 \u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0003"+
		"!\u02ae\b!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0003\"\u02b6"+
		"\b\"\u0001#\u0001#\u0001#\u0001$\u0001$\u0001$\u0001%\u0001%\u0001%\u0001"+
		"&\u0001&\u0001&\u0001\'\u0001\'\u0001\'\u0001(\u0001(\u0001(\u0001)\u0001"+
		")\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0003)\u02d3\b)\u0001"+
		")\u0005)\u02d6\b)\n)\f)\u02d9\t)\u0003)\u02db\b)\u0001)\u0001)\u0001)"+
		"\u0001)\u0001)\u0001)\u0001)\u0001)\u0003)\u02e5\b)\u0001)\u0004)\u02e8"+
		"\b)\u000b)\f)\u02e9\u0003)\u02ec\b)\u0001*\u0001*\u0001*\u0001*\u0001"+
		"*\u0001*\u0001*\u0001*\u0001*\u0001*\u0001*\u0001*\u0003*\u02fa\b*\u0001"+
		"*\u0001*\u0001*\u0001*\u0001*\u0001*\u0001*\u0001*\u0003*\u0304\b*\u0001"+
		"*\u0001*\u0003*\u0308\b*\u0001*\u0000\u0000+\u0000\u0002\u0004\u0006\b"+
		"\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02"+
		"468:<>@BDFHJLNPRT\u0000\f\u0001\u00009:\u0001\u0000\u0002\u0003\u0002"+
		"\u0000\u0002\u0003\r\r\u0002\u0000 \"%%\u0003\u0000\u0006\u0006@ACC\u0001"+
		"\u0000&\'\u0001\u0000\u000e\u000f\u0001\u0000\u0015\u0016\u0001\u0000"+
		"\u0013\u0014\u0001\u0000\u0007\f\u0002\u0000\"\"%%\u0001\u0000#$\u033c"+
		"\u0000V\u0001\u0000\u0000\u0000\u0002_\u0001\u0000\u0000\u0000\u0004|"+
		"\u0001\u0000\u0000\u0000\u0006\u0085\u0001\u0000\u0000\u0000\b\u0092\u0001"+
		"\u0000\u0000\u0000\n\u009d\u0001\u0000\u0000\u0000\f\u00a3\u0001\u0000"+
		"\u0000\u0000\u000e\u00c2\u0001\u0000\u0000\u0000\u0010\u00c4\u0001\u0000"+
		"\u0000\u0000\u0012\u00dd\u0001\u0000\u0000\u0000\u0014\u00fa\u0001\u0000"+
		"\u0000\u0000\u0016\u0117\u0001\u0000\u0000\u0000\u0018\u012d\u0001\u0000"+
		"\u0000\u0000\u001a\u013b\u0001\u0000\u0000\u0000\u001c\u014e\u0001\u0000"+
		"\u0000\u0000\u001e\u0154\u0001\u0000\u0000\u0000 \u0194\u0001\u0000\u0000"+
		"\u0000\"\u0196\u0001\u0000\u0000\u0000$\u01ac\u0001\u0000\u0000\u0000"+
		"&\u01ae\u0001\u0000\u0000\u0000(\u01ba\u0001\u0000\u0000\u0000*\u01d0"+
		"\u0001\u0000\u0000\u0000,\u01d2\u0001\u0000\u0000\u0000.\u01de\u0001\u0000"+
		"\u0000\u00000\u01ea\u0001\u0000\u0000\u00002\u01f6\u0001\u0000\u0000\u0000"+
		"4\u0203\u0001\u0000\u0000\u00006\u021f\u0001\u0000\u0000\u00008\u0221"+
		"\u0001\u0000\u0000\u0000:\u0237\u0001\u0000\u0000\u0000<\u029c\u0001\u0000"+
		"\u0000\u0000>\u02a4\u0001\u0000\u0000\u0000@\u02a6\u0001\u0000\u0000\u0000"+
		"B\u02ad\u0001\u0000\u0000\u0000D\u02b5\u0001\u0000\u0000\u0000F\u02b7"+
		"\u0001\u0000\u0000\u0000H\u02ba\u0001\u0000\u0000\u0000J\u02bd\u0001\u0000"+
		"\u0000\u0000L\u02c0\u0001\u0000\u0000\u0000N\u02c3\u0001\u0000\u0000\u0000"+
		"P\u02c6\u0001\u0000\u0000\u0000R\u02eb\u0001\u0000\u0000\u0000T\u0307"+
		"\u0001\u0000\u0000\u0000V\\\u0003\u0006\u0003\u0000WX\u0003\u0004\u0002"+
		"\u0000XY\u0006\u0000\uffff\uffff\u0000Y[\u0001\u0000\u0000\u0000ZW\u0001"+
		"\u0000\u0000\u0000[^\u0001\u0000\u0000\u0000\\Z\u0001\u0000\u0000\u0000"+
		"\\]\u0001\u0000\u0000\u0000]\u0001\u0001\u0000\u0000\u0000^\\\u0001\u0000"+
		"\u0000\u0000_`\u0003\u0006\u0003\u0000`a\u0005(\u0000\u0000ab\u0006\u0001"+
		"\uffff\uffff\u0000bc\u0003\u0006\u0003\u0000cd\u0005\u0019\u0000\u0000"+
		"de\u0006\u0001\uffff\uffff\u0000et\u0003\u0006\u0003\u0000fo\u0003 \u0010"+
		"\u0000gh\u0003\u0006\u0003\u0000hi\u0005\u0012\u0000\u0000ij\u0006\u0001"+
		"\uffff\uffff\u0000jk\u0003\u0006\u0003\u0000kl\u0003 \u0010\u0000ln\u0001"+
		"\u0000\u0000\u0000mg\u0001\u0000\u0000\u0000nq\u0001\u0000\u0000\u0000"+
		"om\u0001\u0000\u0000\u0000op\u0001\u0000\u0000\u0000pr\u0001\u0000\u0000"+
		"\u0000qo\u0001\u0000\u0000\u0000rs\u0003\u0006\u0003\u0000su\u0001\u0000"+
		"\u0000\u0000tf\u0001\u0000\u0000\u0000tu\u0001\u0000\u0000\u0000uv\u0001"+
		"\u0000\u0000\u0000vw\u0005\u001a\u0000\u0000wx\u0006\u0001\uffff\uffff"+
		"\u0000xy\u0003\u0006\u0003\u0000yz\u0003\u0014\n\u0000z{\u0006\u0001\uffff"+
		"\uffff\u0000{\u0003\u0001\u0000\u0000\u0000|}\u0003\u0014\n\u0000}~\u0003"+
		"\b\u0004\u0000~\u007f\u0006\u0002\uffff\uffff\u0000\u007f\u0005\u0001"+
		"\u0000\u0000\u0000\u0080\u0084\u0005<\u0000\u0000\u0081\u0082\u0005\u0001"+
		"\u0000\u0000\u0082\u0084\u0006\u0003\uffff\uffff\u0000\u0083\u0080\u0001"+
		"\u0000\u0000\u0000\u0083\u0081\u0001\u0000\u0000\u0000\u0084\u0087\u0001"+
		"\u0000\u0000\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0085\u0086\u0001"+
		"\u0000\u0000\u0000\u0086\u0007\u0001\u0000\u0000\u0000\u0087\u0085\u0001"+
		"\u0000\u0000\u0000\u0088\u008c\u0005<\u0000\u0000\u0089\u008a\u0005\u0001"+
		"\u0000\u0000\u008a\u008c\u0006\u0004\uffff\uffff\u0000\u008b\u0088\u0001"+
		"\u0000\u0000\u0000\u008b\u0089\u0001\u0000\u0000\u0000\u008c\u008d\u0001"+
		"\u0000\u0000\u0000\u008d\u008b\u0001\u0000\u0000\u0000\u008d\u008e\u0001"+
		"\u0000\u0000\u0000\u008e\u0093\u0001\u0000\u0000\u0000\u008f\u0093\u0005"+
		"\u0000\u0000\u0001\u0090\u0091\u0005\u0011\u0000\u0000\u0091\u0093\u0003"+
		"\u0006\u0003\u0000\u0092\u008b\u0001\u0000\u0000\u0000\u0092\u008f\u0001"+
		"\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0093\t\u0001\u0000"+
		"\u0000\u0000\u0094\u0099\u0005<\u0000\u0000\u0095\u0096\u0005\u0001\u0000"+
		"\u0000\u0096\u0099\u0006\u0005\uffff\uffff\u0000\u0097\u0099\u0005\u0011"+
		"\u0000\u0000\u0098\u0094\u0001\u0000\u0000\u0000\u0098\u0095\u0001\u0000"+
		"\u0000\u0000\u0098\u0097\u0001\u0000\u0000\u0000\u0099\u009a\u0001\u0000"+
		"\u0000\u0000\u009a\u0098\u0001\u0000\u0000\u0000\u009a\u009b\u0001\u0000"+
		"\u0000\u0000\u009b\u009e\u0001\u0000\u0000\u0000\u009c\u009e\u0005\u0000"+
		"\u0000\u0001\u009d\u0098\u0001\u0000\u0000\u0000\u009d\u009c\u0001\u0000"+
		"\u0000\u0000\u009e\u000b\u0001\u0000\u0000\u0000\u009f\u00a4\u0005<\u0000"+
		"\u0000\u00a0\u00a1\u0005\u0001\u0000\u0000\u00a1\u00a4\u0006\u0006\uffff"+
		"\uffff\u0000\u00a2\u00a4\u0005\u0011\u0000\u0000\u00a3\u009f\u0001\u0000"+
		"\u0000\u0000\u00a3\u00a0\u0001\u0000\u0000\u0000\u00a3\u00a2\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a3\u0001\u0000"+
		"\u0000\u0000\u00a5\u00a6\u0001\u0000\u0000\u0000\u00a6\r\u0001\u0000\u0000"+
		"\u0000\u00a7\u00a8\u0003\u0018\f\u0000\u00a8\u00a9\u0006\u0007\uffff\uffff"+
		"\u0000\u00a9\u00c3\u0001\u0000\u0000\u0000\u00aa\u00ab\u0003\u0016\u000b"+
		"\u0000\u00ab\u00ac\u0006\u0007\uffff\uffff\u0000\u00ac\u00c3\u0001\u0000"+
		"\u0000\u0000\u00ad\u00ae\u0003\u001a\r\u0000\u00ae\u00af\u0006\u0007\uffff"+
		"\uffff\u0000\u00af\u00c3\u0001\u0000\u0000\u0000\u00b0\u00b1\u0003\u001c"+
		"\u000e\u0000\u00b1\u00b2\u0006\u0007\uffff\uffff\u0000\u00b2\u00c3\u0001"+
		"\u0000\u0000\u0000\u00b3\u00b4\u0003\u001e\u000f\u0000\u00b4\u00b5\u0006"+
		"\u0007\uffff\uffff\u0000\u00b5\u00c3\u0001\u0000\u0000\u0000\u00b6\u00b7"+
		"\u0007\u0000\u0000\u0000\u00b7\u00bf\u0006\u0007\uffff\uffff\u0000\u00b8"+
		"\u00b9\u0005\u0019\u0000\u0000\u00b9\u00ba\u0006\u0007\uffff\uffff\u0000"+
		"\u00ba\u00bb\u0003R)\u0000\u00bb\u00bc\u0005\u001a\u0000\u0000\u00bc\u00bd"+
		"\u0006\u0007\uffff\uffff\u0000\u00bd\u00c0\u0001\u0000\u0000\u0000\u00be"+
		"\u00c0\u0001\u0000\u0000\u0000\u00bf\u00b8\u0001\u0000\u0000\u0000\u00bf"+
		"\u00be\u0001\u0000\u0000\u0000\u00c0\u00c1\u0001\u0000\u0000\u0000\u00c1"+
		"\u00c3\u0006\u0007\uffff\uffff\u0000\u00c2\u00a7\u0001\u0000\u0000\u0000"+
		"\u00c2\u00aa\u0001\u0000\u0000\u0000\u00c2\u00ad\u0001\u0000\u0000\u0000"+
		"\u00c2\u00b0\u0001\u0000\u0000\u0000\u00c2\u00b3\u0001\u0000\u0000\u0000"+
		"\u00c2\u00b6\u0001\u0000\u0000\u0000\u00c3\u000f\u0001\u0000\u0000\u0000"+
		"\u00c4\u00c5\u0005\u0017\u0000\u0000\u00c5\u00c7\u0006\b\uffff\uffff\u0000"+
		"\u00c6\u00c8\u0003\n\u0005\u0000\u00c7\u00c6\u0001\u0000\u0000\u0000\u00c7"+
		"\u00c8\u0001\u0000\u0000\u0000\u00c8\u00d7\u0001\u0000\u0000\u0000\u00c9"+
		"\u00ca\u0003\u0014\n\u0000\u00ca\u00d1\u0006\b\uffff\uffff\u0000\u00cb"+
		"\u00cc\u0003\f\u0006\u0000\u00cc\u00cd\u0003\u0014\n\u0000\u00cd\u00ce"+
		"\u0006\b\uffff\uffff\u0000\u00ce\u00d0\u0001\u0000\u0000\u0000\u00cf\u00cb"+
		"\u0001\u0000\u0000\u0000\u00d0\u00d3\u0001\u0000\u0000\u0000\u00d1\u00cf"+
		"\u0001\u0000\u0000\u0000\u00d1\u00d2\u0001\u0000\u0000\u0000\u00d2\u00d5"+
		"\u0001\u0000\u0000\u0000\u00d3\u00d1\u0001\u0000\u0000\u0000\u00d4\u00d6"+
		"\u0003\n\u0005\u0000\u00d5\u00d4\u0001\u0000\u0000\u0000\u00d5\u00d6\u0001"+
		"\u0000\u0000\u0000\u00d6\u00d8\u0001\u0000\u0000\u0000\u00d7\u00c9\u0001"+
		"\u0000\u0000\u0000\u00d7\u00d8\u0001\u0000\u0000\u0000\u00d8\u00d9\u0001"+
		"\u0000\u0000\u0000\u00d9\u00da\u0005\u0018\u0000\u0000\u00da\u00db\u0006"+
		"\b\uffff\uffff\u0000\u00db\u00dc\u0006\b\uffff\uffff\u0000\u00dc\u0011"+
		"\u0001\u0000\u0000\u0000\u00dd\u00de\u0003\"\u0011\u0000\u00de\u00f8\u0006"+
		"\t\uffff\uffff\u0000\u00df\u00e0\u0007\u0001\u0000\u0000\u00e0\u00e1\u0006"+
		"\t\uffff\uffff\u0000\u00e1\u00e8\u0003\u0006\u0003\u0000\u00e2\u00e3\u0003"+
		"\u001e\u000f\u0000\u00e3\u00e4\u0006\t\uffff\uffff\u0000\u00e4\u00e9\u0001"+
		"\u0000\u0000\u0000\u00e5\u00e6\u0003\u0012\t\u0000\u00e6\u00e7\u0006\t"+
		"\uffff\uffff\u0000\u00e7\u00e9\u0001\u0000\u0000\u0000\u00e8\u00e2\u0001"+
		"\u0000\u0000\u0000\u00e8\u00e5\u0001\u0000\u0000\u0000\u00e9\u00ea\u0001"+
		"\u0000\u0000\u0000\u00ea\u00eb\u0006\t\uffff\uffff\u0000\u00eb\u00f9\u0001"+
		"\u0000\u0000\u0000\u00ec\u00ed\u0005\u0004\u0000\u0000\u00ed\u00ee\u0006"+
		"\t\uffff\uffff\u0000\u00ee\u00ef\u0003\u0006\u0003\u0000\u00ef\u00f0\u0003"+
		"\u0012\t\u0000\u00f0\u00f1\u0006\t\uffff\uffff\u0000\u00f1\u00f9\u0001"+
		"\u0000\u0000\u0000\u00f2\u00f3\u0005\u0005\u0000\u0000\u00f3\u00f4\u0006"+
		"\t\uffff\uffff\u0000\u00f4\u00f5\u0003\u0006\u0003\u0000\u00f5\u00f6\u0003"+
		"\u0012\t\u0000\u00f6\u00f7\u0006\t\uffff\uffff\u0000\u00f7\u00f9\u0001"+
		"\u0000\u0000\u0000\u00f8\u00df\u0001\u0000\u0000\u0000\u00f8\u00ec\u0001"+
		"\u0000\u0000\u0000\u00f8\u00f2\u0001\u0000\u0000\u0000\u00f8\u00f9\u0001"+
		"\u0000\u0000\u0000\u00f9\u0013\u0001\u0000\u0000\u0000\u00fa\u00fb\u0003"+
		"\"\u0011\u0000\u00fb\u0115\u0006\n\uffff\uffff\u0000\u00fc\u00fd\u0007"+
		"\u0002\u0000\u0000\u00fd\u00fe\u0006\n\uffff\uffff\u0000\u00fe\u0105\u0003"+
		"\u0006\u0003\u0000\u00ff\u0100\u0003\u001e\u000f\u0000\u0100\u0101\u0006"+
		"\n\uffff\uffff\u0000\u0101\u0106\u0001\u0000\u0000\u0000\u0102\u0103\u0003"+
		"\u0014\n\u0000\u0103\u0104\u0006\n\uffff\uffff\u0000\u0104\u0106\u0001"+
		"\u0000\u0000\u0000\u0105\u00ff\u0001\u0000\u0000\u0000\u0105\u0102\u0001"+
		"\u0000\u0000\u0000\u0106\u0107\u0001\u0000\u0000\u0000\u0107\u0108\u0006"+
		"\n\uffff\uffff\u0000\u0108\u0116\u0001\u0000\u0000\u0000\u0109\u010a\u0005"+
		"\u0004\u0000\u0000\u010a\u010b\u0006\n\uffff\uffff\u0000\u010b\u010c\u0003"+
		"\u0006\u0003\u0000\u010c\u010d\u0003\u0014\n\u0000\u010d\u010e\u0006\n"+
		"\uffff\uffff\u0000\u010e\u0116\u0001\u0000\u0000\u0000\u010f\u0110\u0005"+
		"\u0005\u0000\u0000\u0110\u0111\u0006\n\uffff\uffff\u0000\u0111\u0112\u0003"+
		"\u0006\u0003\u0000\u0112\u0113\u0003\u0014\n\u0000\u0113\u0114\u0006\n"+
		"\uffff\uffff\u0000\u0114\u0116\u0001\u0000\u0000\u0000\u0115\u00fc\u0001"+
		"\u0000\u0000\u0000\u0115\u0109\u0001\u0000\u0000\u0000\u0115\u010f\u0001"+
		"\u0000\u0000\u0000\u0115\u0116\u0001\u0000\u0000\u0000\u0116\u0015\u0001"+
		"\u0000\u0000\u0000\u0117\u0118\u00057\u0000\u0000\u0118\u0119\u0006\u000b"+
		"\uffff\uffff\u0000\u0119\u011a\u0003\u0006\u0003\u0000\u011a\u011b\u0005"+
		"\u0019\u0000\u0000\u011b\u011c\u0006\u000b\uffff\uffff\u0000\u011c\u011d"+
		"\u0003\u0006\u0003\u0000\u011d\u011e\u0003\u0014\n\u0000\u011e\u011f\u0003"+
		"\u0006\u0003\u0000\u011f\u0120\u0005\u001a\u0000\u0000\u0120\u0121\u0006"+
		"\u000b\uffff\uffff\u0000\u0121\u0122\u0003\u0006\u0003\u0000\u0122\u012b"+
		"\u0003\u0014\n\u0000\u0123\u0124\u0003\u0006\u0003\u0000\u0124\u0125\u0005"+
		"8\u0000\u0000\u0125\u0126\u0006\u000b\uffff\uffff\u0000\u0126\u0127\u0003"+
		"\u0006\u0003\u0000\u0127\u0128\u0003\u0014\n\u0000\u0128\u0129\u0006\u000b"+
		"\uffff\uffff\u0000\u0129\u012c\u0001\u0000\u0000\u0000\u012a\u012c\u0006"+
		"\u000b\uffff\uffff\u0000\u012b\u0123\u0001\u0000\u0000\u0000\u012b\u012a"+
		"\u0001\u0000\u0000\u0000\u012c\u0017\u0001\u0000\u0000\u0000\u012d\u012e"+
		"\u00053\u0000\u0000\u012e\u012f\u0006\f\uffff\uffff\u0000\u012f\u0130"+
		"\u0003\u0006\u0003\u0000\u0130\u0131\u0005\u0019\u0000\u0000\u0131\u0132"+
		"\u0006\f\uffff\uffff\u0000\u0132\u0133\u0003\u0006\u0003\u0000\u0133\u0134"+
		"\u0003\u0014\n\u0000\u0134\u0135\u0003\u0006\u0003\u0000\u0135\u0136\u0005"+
		"\u001a\u0000\u0000\u0136\u0137\u0006\f\uffff\uffff\u0000\u0137\u0138\u0003"+
		"\u0006\u0003\u0000\u0138\u0139\u0003\u0014\n\u0000\u0139\u013a\u0006\f"+
		"\uffff\uffff\u0000\u013a\u0019\u0001\u0000\u0000\u0000\u013b\u013c\u0005"+
		"4\u0000\u0000\u013c\u013d\u0006\r\uffff\uffff\u0000\u013d\u013e\u0003"+
		"\u0006\u0003\u0000\u013e\u013f\u0005\u0019\u0000\u0000\u013f\u0140\u0006"+
		"\r\uffff\uffff\u0000\u0140\u0141\u0003\u0006\u0003\u0000\u0141\u0142\u0005"+
		"A\u0000\u0000\u0142\u0143\u0003\u0006\u0003\u0000\u0143\u0144\u00056\u0000"+
		"\u0000\u0144\u0145\u0006\r\uffff\uffff\u0000\u0145\u0146\u0003\u0006\u0003"+
		"\u0000\u0146\u0147\u0003\u0014\n\u0000\u0147\u0148\u0003\u0006\u0003\u0000"+
		"\u0148\u0149\u0005\u001a\u0000\u0000\u0149\u014a\u0006\r\uffff\uffff\u0000"+
		"\u014a\u014b\u0003\u0006\u0003\u0000\u014b\u014c\u0003\u0014\n\u0000\u014c"+
		"\u014d\u0006\r\uffff\uffff\u0000\u014d\u001b\u0001\u0000\u0000\u0000\u014e"+
		"\u014f\u00055\u0000\u0000\u014f\u0150\u0006\u000e\uffff\uffff\u0000\u0150"+
		"\u0151\u0003\u0006\u0003\u0000\u0151\u0152\u0003\u0014\n\u0000\u0152\u0153"+
		"\u0006\u000e\uffff\uffff\u0000\u0153\u001d\u0001\u0000\u0000\u0000\u0154"+
		"\u0155\u0005(\u0000\u0000\u0155\u0156\u0006\u000f\uffff\uffff\u0000\u0156"+
		"\u0157\u0003\u0006\u0003\u0000\u0157\u0158\u0005\u0019\u0000\u0000\u0158"+
		"\u0159\u0006\u000f\uffff\uffff\u0000\u0159\u0168\u0003\u0006\u0003\u0000"+
		"\u015a\u0163\u0003 \u0010\u0000\u015b\u015c\u0003\u0006\u0003\u0000\u015c"+
		"\u015d\u0005\u0012\u0000\u0000\u015d\u015e\u0006\u000f\uffff\uffff\u0000"+
		"\u015e\u015f\u0003\u0006\u0003\u0000\u015f\u0160\u0003 \u0010\u0000\u0160"+
		"\u0162\u0001\u0000\u0000\u0000\u0161\u015b\u0001\u0000\u0000\u0000\u0162"+
		"\u0165\u0001\u0000\u0000\u0000\u0163\u0161\u0001\u0000\u0000\u0000\u0163"+
		"\u0164\u0001\u0000\u0000\u0000\u0164\u0166\u0001\u0000\u0000\u0000\u0165"+
		"\u0163\u0001\u0000\u0000\u0000\u0166\u0167\u0003\u0006\u0003\u0000\u0167"+
		"\u0169\u0001\u0000\u0000\u0000\u0168\u015a\u0001\u0000\u0000\u0000\u0168"+
		"\u0169\u0001\u0000\u0000\u0000\u0169\u016a\u0001\u0000\u0000\u0000\u016a"+
		"\u016b\u0005\u001a\u0000\u0000\u016b\u016c\u0006\u000f\uffff\uffff\u0000"+
		"\u016c\u016d\u0003\u0006\u0003\u0000\u016d\u016e\u0003\u0014\n\u0000\u016e"+
		"\u016f\u0006\u000f\uffff\uffff\u0000\u016f\u001f\u0001\u0000\u0000\u0000"+
		"\u0170\u0171\u0005A\u0000\u0000\u0171\u0172\u0006\u0010\uffff\uffff\u0000"+
		"\u0172\u0195\u0006\u0010\uffff\uffff\u0000\u0173\u0174\u0005A\u0000\u0000"+
		"\u0174\u0175\u0006\u0010\uffff\uffff\u0000\u0175\u0176\u0003\u0006\u0003"+
		"\u0000\u0176\u0177\u0005\r\u0000\u0000\u0177\u0178\u0006\u0010\uffff\uffff"+
		"\u0000\u0178\u0179\u0003\u0006\u0003\u0000\u0179\u017a\u0003\u0012\t\u0000"+
		"\u017a\u017b\u0006\u0010\uffff\uffff\u0000\u017b\u0195\u0001\u0000\u0000"+
		"\u0000\u017c\u017d\u0005\u0006\u0000\u0000\u017d\u017e\u0006\u0010\uffff"+
		"\uffff\u0000\u017e\u0195\u0006\u0010\uffff\uffff\u0000\u017f\u0180\u0005"+
		"\u0006\u0000\u0000\u0180\u0181\u0006\u0010\uffff\uffff\u0000\u0181\u0182"+
		"\u0003\u0006\u0003\u0000\u0182\u0183\u0005\r\u0000\u0000\u0183\u0184\u0006"+
		"\u0010\uffff\uffff\u0000\u0184\u0185\u0003\u0006\u0003\u0000\u0185\u0186"+
		"\u0003\u0012\t\u0000\u0186\u0187\u0006\u0010\uffff\uffff\u0000\u0187\u0195"+
		"\u0001\u0000\u0000\u0000\u0188\u0189\u0005@\u0000\u0000\u0189\u018a\u0006"+
		"\u0010\uffff\uffff\u0000\u018a\u0195\u0006\u0010\uffff\uffff\u0000\u018b"+
		"\u018c\u0005@\u0000\u0000\u018c\u018d\u0006\u0010\uffff\uffff\u0000\u018d"+
		"\u018e\u0003\u0006\u0003\u0000\u018e\u018f\u0005\r\u0000\u0000\u018f\u0190"+
		"\u0006\u0010\uffff\uffff\u0000\u0190\u0191\u0003\u0006\u0003\u0000\u0191"+
		"\u0192\u0003\u0012\t\u0000\u0192\u0193\u0006\u0010\uffff\uffff\u0000\u0193"+
		"\u0195\u0001\u0000\u0000\u0000\u0194\u0170\u0001\u0000\u0000\u0000\u0194"+
		"\u0173\u0001\u0000\u0000\u0000\u0194\u017c\u0001\u0000\u0000\u0000\u0194"+
		"\u017f\u0001\u0000\u0000\u0000\u0194\u0188\u0001\u0000\u0000\u0000\u0194"+
		"\u018b\u0001\u0000\u0000\u0000\u0195!\u0001\u0000\u0000\u0000\u0196\u0197"+
		"\u0003$\u0012\u0000\u0197\u01a0\u0006\u0011\uffff\uffff\u0000\u0198\u0199"+
		"\u0005\u001f\u0000\u0000\u0199\u019a\u0006\u0011\uffff\uffff\u0000\u019a"+
		"\u019b\u0003\u0006\u0003\u0000\u019b\u019c\u0003$\u0012\u0000\u019c\u019d"+
		"\u0006\u0011\uffff\uffff\u0000\u019d\u019f\u0001\u0000\u0000\u0000\u019e"+
		"\u0198\u0001\u0000\u0000\u0000\u019f\u01a2\u0001\u0000\u0000\u0000\u01a0"+
		"\u019e\u0001\u0000\u0000\u0000\u01a0\u01a1\u0001\u0000\u0000\u0000\u01a1"+
		"#\u0001\u0000\u0000\u0000\u01a2\u01a0\u0001\u0000\u0000\u0000\u01a3\u01a4"+
		"\u0005\u001f\u0000\u0000\u01a4\u01a5\u0006\u0012\uffff\uffff\u0000\u01a5"+
		"\u01a6\u0003\u0006\u0003\u0000\u01a6\u01a7\u0003$\u0012\u0000\u01a7\u01a8"+
		"\u0006\u0012\uffff\uffff\u0000\u01a8\u01ad\u0001\u0000\u0000\u0000\u01a9"+
		"\u01aa\u0003&\u0013\u0000\u01aa\u01ab\u0006\u0012\uffff\uffff\u0000\u01ab"+
		"\u01ad\u0001\u0000\u0000\u0000\u01ac\u01a3\u0001\u0000\u0000\u0000\u01ac"+
		"\u01a9\u0001\u0000\u0000\u0000\u01ad%\u0001\u0000\u0000\u0000\u01ae\u01af"+
		"\u0003(\u0014\u0000\u01af\u01b7\u0006\u0013\uffff\uffff\u0000\u01b0\u01b1"+
		"\u0003F#\u0000\u01b1\u01b2\u0003\u0006\u0003\u0000\u01b2\u01b3\u0003("+
		"\u0014\u0000\u01b3\u01b4\u0006\u0013\uffff\uffff\u0000\u01b4\u01b6\u0001"+
		"\u0000\u0000\u0000\u01b5\u01b0\u0001\u0000\u0000\u0000\u01b6\u01b9\u0001"+
		"\u0000\u0000\u0000\u01b7\u01b5\u0001\u0000\u0000\u0000\u01b7\u01b8\u0001"+
		"\u0000\u0000\u0000\u01b8\'\u0001\u0000\u0000\u0000\u01b9\u01b7\u0001\u0000"+
		"\u0000\u0000\u01ba\u01bb\u0003*\u0015\u0000\u01bb\u01c3\u0006\u0014\uffff"+
		"\uffff\u0000\u01bc\u01bd\u0003H$\u0000\u01bd\u01be\u0003\u0006\u0003\u0000"+
		"\u01be\u01bf\u0003*\u0015\u0000\u01bf\u01c0\u0006\u0014\uffff\uffff\u0000"+
		"\u01c0\u01c2\u0001\u0000\u0000\u0000\u01c1\u01bc\u0001\u0000\u0000\u0000"+
		"\u01c2\u01c5\u0001\u0000\u0000\u0000\u01c3\u01c1\u0001\u0000\u0000\u0000"+
		"\u01c3\u01c4\u0001\u0000\u0000\u0000\u01c4)\u0001\u0000\u0000\u0000\u01c5"+
		"\u01c3\u0001\u0000\u0000\u0000\u01c6\u01c7\u0004\u0015\u0000\u0000\u01c7"+
		"\u01c8\u0005 \u0000\u0000\u01c8\u01c9\u0006\u0015\uffff\uffff\u0000\u01c9"+
		"\u01ca\u0003\u0006\u0003\u0000\u01ca\u01cb\u0003*\u0015\u0000\u01cb\u01cc"+
		"\u0006\u0015\uffff\uffff\u0000\u01cc\u01d1\u0001\u0000\u0000\u0000\u01cd"+
		"\u01ce\u0003,\u0016\u0000\u01ce\u01cf\u0006\u0015\uffff\uffff\u0000\u01cf"+
		"\u01d1\u0001\u0000\u0000\u0000\u01d0\u01c6\u0001\u0000\u0000\u0000\u01d0"+
		"\u01cd\u0001\u0000\u0000\u0000\u01d1+\u0001\u0000\u0000\u0000\u01d2\u01d3"+
		"\u0003.\u0017\u0000\u01d3\u01db\u0006\u0016\uffff\uffff\u0000\u01d4\u01d5"+
		"\u0003J%\u0000\u01d5\u01d6\u0003\u0006\u0003\u0000\u01d6\u01d7\u0003."+
		"\u0017\u0000\u01d7\u01d8\u0006\u0016\uffff\uffff\u0000\u01d8\u01da\u0001"+
		"\u0000\u0000\u0000\u01d9\u01d4\u0001\u0000\u0000\u0000\u01da\u01dd\u0001"+
		"\u0000\u0000\u0000\u01db\u01d9\u0001\u0000\u0000\u0000\u01db\u01dc\u0001"+
		"\u0000\u0000\u0000\u01dc-\u0001\u0000\u0000\u0000\u01dd\u01db\u0001\u0000"+
		"\u0000\u0000\u01de\u01df\u00030\u0018\u0000\u01df\u01e7\u0006\u0017\uffff"+
		"\uffff\u0000\u01e0\u01e1\u0003L&\u0000\u01e1\u01e2\u0003\u0006\u0003\u0000"+
		"\u01e2\u01e3\u00030\u0018\u0000\u01e3\u01e4\u0006\u0017\uffff\uffff\u0000"+
		"\u01e4\u01e6\u0001\u0000\u0000\u0000\u01e5\u01e0\u0001\u0000\u0000\u0000"+
		"\u01e6\u01e9\u0001\u0000\u0000\u0000\u01e7\u01e5\u0001\u0000\u0000\u0000"+
		"\u01e7\u01e8\u0001\u0000\u0000\u0000\u01e8/\u0001\u0000\u0000\u0000\u01e9"+
		"\u01e7\u0001\u0000\u0000\u0000\u01ea\u01eb\u00032\u0019\u0000\u01eb\u01f3"+
		"\u0006\u0018\uffff\uffff\u0000\u01ec\u01ed\u0003N\'\u0000\u01ed\u01ee"+
		"\u0003\u0006\u0003\u0000\u01ee\u01ef\u00032\u0019\u0000\u01ef\u01f0\u0006"+
		"\u0018\uffff\uffff\u0000\u01f0\u01f2\u0001\u0000\u0000\u0000\u01f1\u01ec"+
		"\u0001\u0000\u0000\u0000\u01f2\u01f5\u0001\u0000\u0000\u0000\u01f3\u01f1"+
		"\u0001\u0000\u0000\u0000\u01f3\u01f4\u0001\u0000\u0000\u0000\u01f41\u0001"+
		"\u0000\u0000\u0000\u01f5\u01f3\u0001\u0000\u0000\u0000\u01f6\u01f7\u0003"+
		"4\u001a\u0000\u01f7\u0200\u0006\u0019\uffff\uffff\u0000\u01f8\u01f9\u0005"+
		"B\u0000\u0000\u01f9\u01fa\u0006\u0019\uffff\uffff\u0000\u01fa\u01fb\u0003"+
		"\u0006\u0003\u0000\u01fb\u01fc\u00034\u001a\u0000\u01fc\u01fd\u0006\u0019"+
		"\uffff\uffff\u0000\u01fd\u01ff\u0001\u0000\u0000\u0000\u01fe\u01f8\u0001"+
		"\u0000\u0000\u0000\u01ff\u0202\u0001\u0000\u0000\u0000\u0200\u01fe\u0001"+
		"\u0000\u0000\u0000\u0200\u0201\u0001\u0000\u0000\u0000\u02013\u0001\u0000"+
		"\u0000\u0000\u0202\u0200\u0001\u0000\u0000\u0000\u0203\u0204\u00036\u001b"+
		"\u0000\u0204\u020d\u0006\u001a\uffff\uffff\u0000\u0205\u0206\u0005\u0010"+
		"\u0000\u0000\u0206\u0207\u0006\u001a\uffff\uffff\u0000\u0207\u0208\u0003"+
		"\u0006\u0003\u0000\u0208\u0209\u00036\u001b\u0000\u0209\u020a\u0006\u001a"+
		"\uffff\uffff\u0000\u020a\u020c\u0001\u0000\u0000\u0000\u020b\u0205\u0001"+
		"\u0000\u0000\u0000\u020c\u020f\u0001\u0000\u0000\u0000\u020d\u020b\u0001"+
		"\u0000\u0000\u0000\u020d\u020e\u0001\u0000\u0000\u0000\u020e5\u0001\u0000"+
		"\u0000\u0000\u020f\u020d\u0001\u0000\u0000\u0000\u0210\u0211\u0007\u0003"+
		"\u0000\u0000\u0211\u0212\u0006\u001b\uffff\uffff\u0000\u0212\u0213\u0003"+
		"\u0006\u0003\u0000\u0213\u0214\u00036\u001b\u0000\u0214\u0215\u0006\u001b"+
		"\uffff\uffff\u0000\u0215\u0220\u0001\u0000\u0000\u0000\u0216\u0217\u0005"+
		"\u001f\u0000\u0000\u0217\u0218\u0006\u001b\uffff\uffff\u0000\u0218\u0219"+
		"\u0003\u0006\u0003\u0000\u0219\u021a\u0003$\u0012\u0000\u021a\u021b\u0006"+
		"\u001b\uffff\uffff\u0000\u021b\u0220\u0001\u0000\u0000\u0000\u021c\u021d"+
		"\u00038\u001c\u0000\u021d\u021e\u0006\u001b\uffff\uffff\u0000\u021e\u0220"+
		"\u0001\u0000\u0000\u0000\u021f\u0210\u0001\u0000\u0000\u0000\u021f\u0216"+
		"\u0001\u0000\u0000\u0000\u021f\u021c\u0001\u0000\u0000\u0000\u02207\u0001"+
		"\u0000\u0000\u0000\u0221\u0222\u0003:\u001d\u0000\u0222\u0229\u0006\u001c"+
		"\uffff\uffff\u0000\u0223\u0224\u0003P(\u0000\u0224\u0225\u0003\u0006\u0003"+
		"\u0000\u0225\u0226\u00036\u001b\u0000\u0226\u0227\u0006\u001c\uffff\uffff"+
		"\u0000\u0227\u022a\u0001\u0000\u0000\u0000\u0228\u022a\u0001\u0000\u0000"+
		"\u0000\u0229\u0223\u0001\u0000\u0000\u0000\u0229\u0228\u0001\u0000\u0000"+
		"\u0000\u022a9\u0001\u0000\u0000\u0000\u022b\u022c\u0007\u0004\u0000\u0000"+
		"\u022c\u022d\u0006\u001d\uffff\uffff\u0000\u022d\u022e\u0005\u0019\u0000"+
		"\u0000\u022e\u022f\u0006\u001d\uffff\uffff\u0000\u022f\u0230\u0003R)\u0000"+
		"\u0230\u0231\u0005\u001a\u0000\u0000\u0231\u0232\u0006\u001d\uffff\uffff"+
		"\u0000\u0232\u0233\u0006\u001d\uffff\uffff\u0000\u0233\u0238\u0001\u0000"+
		"\u0000\u0000\u0234\u0235\u0003<\u001e\u0000\u0235\u0236\u0006\u001d\uffff"+
		"\uffff\u0000\u0236\u0238\u0001\u0000\u0000\u0000\u0237\u022b\u0001\u0000"+
		"\u0000\u0000\u0237\u0234\u0001\u0000\u0000\u0000\u0238\u0260\u0001\u0000"+
		"\u0000\u0000\u0239\u023a\u0007\u0005\u0000\u0000\u023a\u023b\u0006\u001d"+
		"\uffff\uffff\u0000\u023b\u023c\u0003\u0006\u0003\u0000\u023c\u023d\u0003"+
		"B!\u0000\u023d\u023e\u0006\u001d\uffff\uffff\u0000\u023e\u025b\u0001\u0000"+
		"\u0000\u0000\u023f\u0240\u0007\u0005\u0000\u0000\u0240\u0241\u0006\u001d"+
		"\uffff\uffff\u0000\u0241\u0242\u0003\u0006\u0003\u0000\u0242\u0243\u0003"+
		"@ \u0000\u0243\u0244\u0006\u001d\uffff\uffff\u0000\u0244\u025b\u0001\u0000"+
		"\u0000\u0000\u0245\u0246\u0005\u001c\u0000\u0000\u0246\u0247\u0006\u001d"+
		"\uffff\uffff\u0000\u0247\u0248\u0003R)\u0000\u0248\u0249\u0005\u001d\u0000"+
		"\u0000\u0249\u024a\u0006\u001d\uffff\uffff\u0000\u024a\u025b\u0001\u0000"+
		"\u0000\u0000\u024b\u024c\u0005\u001b\u0000\u0000\u024c\u024d\u0006\u001d"+
		"\uffff\uffff\u0000\u024d\u024e\u0003R)\u0000\u024e\u024f\u0005\u001d\u0000"+
		"\u0000\u024f\u0250\u0006\u001d\uffff\uffff\u0000\u0250\u0251\u0005\u001d"+
		"\u0000\u0000\u0251\u0252\u0006\u001d\uffff\uffff\u0000\u0252\u025b\u0001"+
		"\u0000\u0000\u0000\u0253\u0254\u0005\u0019\u0000\u0000\u0254\u0255\u0006"+
		"\u001d\uffff\uffff\u0000\u0255\u0256\u0003R)\u0000\u0256\u0257\u0005\u001a"+
		"\u0000\u0000\u0257\u0258\u0006\u001d\uffff\uffff\u0000\u0258\u0259\u0006"+
		"\u001d\uffff\uffff\u0000\u0259\u025b\u0001\u0000\u0000\u0000\u025a\u0239"+
		"\u0001\u0000\u0000\u0000\u025a\u023f\u0001\u0000\u0000\u0000\u025a\u0245"+
		"\u0001\u0000\u0000\u0000\u025a\u024b\u0001\u0000\u0000\u0000\u025a\u0253"+
		"\u0001\u0000\u0000\u0000\u025b\u025d\u0001\u0000\u0000\u0000\u025c\u025a"+
		"\u0001\u0000\u0000\u0000\u025d\u025e\u0001\u0000\u0000\u0000\u025e\u025c"+
		"\u0001\u0000\u0000\u0000\u025e\u025f\u0001\u0000\u0000\u0000\u025f\u0261"+
		"\u0001\u0000\u0000\u0000\u0260\u025c\u0001\u0000\u0000\u0000\u0260\u0261"+
		"\u0001\u0000\u0000\u0000\u0261;\u0001\u0000\u0000\u0000\u0262\u0263\u0003"+
		"B!\u0000\u0263\u0264\u0006\u001e\uffff\uffff\u0000\u0264\u029d\u0001\u0000"+
		"\u0000\u0000\u0265\u0266\u0003D\"\u0000\u0266\u0267\u0006\u001e\uffff"+
		"\uffff\u0000\u0267\u029d\u0001\u0000\u0000\u0000\u0268\u0269\u0005@\u0000"+
		"\u0000\u0269\u029d\u0006\u001e\uffff\uffff\u0000\u026a\u026b\u0005)\u0000"+
		"\u0000\u026b\u029d\u0006\u001e\uffff\uffff\u0000\u026c\u026d\u00051\u0000"+
		"\u0000\u026d\u029d\u0006\u001e\uffff\uffff\u0000\u026e\u026f\u00052\u0000"+
		"\u0000\u026f\u029d\u0006\u001e\uffff\uffff\u0000\u0270\u0271\u0005+\u0000"+
		"\u0000\u0271\u029d\u0006\u001e\uffff\uffff\u0000\u0272\u0273\u0005,\u0000"+
		"\u0000\u0273\u029d\u0006\u001e\uffff\uffff\u0000\u0274\u0275\u0005-\u0000"+
		"\u0000\u0275\u029d\u0006\u001e\uffff\uffff\u0000\u0276\u0277\u0005.\u0000"+
		"\u0000\u0277\u029d\u0006\u001e\uffff\uffff\u0000\u0278\u0279\u0003>\u001f"+
		"\u0000\u0279\u027a\u0006\u001e\uffff\uffff\u0000\u027a\u029d\u0001\u0000"+
		"\u0000\u0000\u027b\u027c\u0003@ \u0000\u027c\u027d\u0006\u001e\uffff\uffff"+
		"\u0000\u027d\u029d\u0001\u0000\u0000\u0000\u027e\u027f\u0003B!\u0000\u027f"+
		"\u0280\u0006\u001e\uffff\uffff\u0000\u0280\u0281\u0007\u0006\u0000\u0000"+
		"\u0281\u0282\u0006\u001e\uffff\uffff\u0000\u0282\u0283\u0003\u0006\u0003"+
		"\u0000\u0283\u028a\u0006\u001e\uffff\uffff\u0000\u0284\u0285\u0003B!\u0000"+
		"\u0285\u0286\u0006\u001e\uffff\uffff\u0000\u0286\u028b\u0001\u0000\u0000"+
		"\u0000\u0287\u0288\u0005C\u0000\u0000\u0288\u0289\u0006\u001e\uffff\uffff"+
		"\u0000\u0289\u028b\u0006\u001e\uffff\uffff\u0000\u028a\u0284\u0001\u0000"+
		"\u0000\u0000\u028a\u0287\u0001\u0000\u0000\u0000\u028b\u028c\u0001\u0000"+
		"\u0000\u0000\u028c\u028d\u0006\u001e\uffff\uffff\u0000\u028d\u029d\u0001"+
		"\u0000\u0000\u0000\u028e\u028f\u0005\u0019\u0000\u0000\u028f\u0290\u0006"+
		"\u001e\uffff\uffff\u0000\u0290\u0291\u0003\u0006\u0003\u0000\u0291\u0292"+
		"\u0003\u0014\n\u0000\u0292\u0293\u0003\u0006\u0003\u0000\u0293\u0294\u0005"+
		"\u001a\u0000\u0000\u0294\u0295\u0006\u001e\uffff\uffff\u0000\u0295\u029d"+
		"\u0001\u0000\u0000\u0000\u0296\u0297\u0003\u0010\b\u0000\u0297\u0298\u0006"+
		"\u001e\uffff\uffff\u0000\u0298\u029d\u0001\u0000\u0000\u0000\u0299\u029a"+
		"\u0003\u000e\u0007\u0000\u029a\u029b\u0006\u001e\uffff\uffff\u0000\u029b"+
		"\u029d\u0001\u0000\u0000\u0000\u029c\u0262\u0001\u0000\u0000\u0000\u029c"+
		"\u0265\u0001\u0000\u0000\u0000\u029c\u0268\u0001\u0000\u0000\u0000\u029c"+
		"\u026a\u0001\u0000\u0000\u0000\u029c\u026c\u0001\u0000\u0000\u0000\u029c"+
		"\u026e\u0001\u0000\u0000\u0000\u029c\u0270\u0001\u0000\u0000\u0000\u029c"+
		"\u0272\u0001\u0000\u0000\u0000\u029c\u0274\u0001\u0000\u0000\u0000\u029c"+
		"\u0276\u0001\u0000\u0000\u0000\u029c\u0278\u0001\u0000\u0000\u0000\u029c"+
		"\u027b\u0001\u0000\u0000\u0000\u029c\u027e\u0001\u0000\u0000\u0000\u029c"+
		"\u028e\u0001\u0000\u0000\u0000\u029c\u0296\u0001\u0000\u0000\u0000\u029c"+
		"\u0299\u0001\u0000\u0000\u0000\u029d=\u0001\u0000\u0000\u0000\u029e\u029f"+
		"\u0005=\u0000\u0000\u029f\u02a5\u0006\u001f\uffff\uffff\u0000\u02a0\u02a1"+
		"\u0005?\u0000\u0000\u02a1\u02a5\u0006\u001f\uffff\uffff\u0000\u02a2\u02a3"+
		"\u0005>\u0000\u0000\u02a3\u02a5\u0006\u001f\uffff\uffff\u0000\u02a4\u029e"+
		"\u0001\u0000\u0000\u0000\u02a4\u02a0\u0001\u0000\u0000\u0000\u02a4\u02a2"+
		"\u0001\u0000\u0000\u0000\u02a5?\u0001\u0000\u0000\u0000\u02a6\u02a7\u0005"+
		"C\u0000\u0000\u02a7\u02a8\u0006 \uffff\uffff\u0000\u02a8A\u0001\u0000"+
		"\u0000\u0000\u02a9\u02aa\u0005A\u0000\u0000\u02aa\u02ae\u0006!\uffff\uffff"+
		"\u0000\u02ab\u02ac\u0005\u0006\u0000\u0000\u02ac\u02ae\u0006!\uffff\uffff"+
		"\u0000\u02ad\u02a9\u0001\u0000\u0000\u0000\u02ad\u02ab\u0001\u0000\u0000"+
		"\u0000\u02aeC\u0001\u0000\u0000\u0000\u02af\u02b0\u0005/\u0000\u0000\u02b0"+
		"\u02b6\u0006\"\uffff\uffff\u0000\u02b1\u02b2\u00050\u0000\u0000\u02b2"+
		"\u02b6\u0006\"\uffff\uffff\u0000\u02b3\u02b4\u0005*\u0000\u0000\u02b4"+
		"\u02b6\u0006\"\uffff\uffff\u0000\u02b5\u02af\u0001\u0000\u0000\u0000\u02b5"+
		"\u02b1\u0001\u0000\u0000\u0000\u02b5\u02b3\u0001\u0000\u0000\u0000\u02b6"+
		"E\u0001\u0000\u0000\u0000\u02b7\u02b8\u0007\u0007\u0000\u0000\u02b8\u02b9"+
		"\u0006#\uffff\uffff\u0000\u02b9G\u0001\u0000\u0000\u0000\u02ba\u02bb\u0007"+
		"\b\u0000\u0000\u02bb\u02bc\u0006$\uffff\uffff\u0000\u02bcI\u0001\u0000"+
		"\u0000\u0000\u02bd\u02be\u0007\t\u0000\u0000\u02be\u02bf\u0006%\uffff"+
		"\uffff\u0000\u02bfK\u0001\u0000\u0000\u0000\u02c0\u02c1\u0007\n\u0000"+
		"\u0000\u02c1\u02c2\u0006&\uffff\uffff\u0000\u02c2M\u0001\u0000\u0000\u0000"+
		"\u02c3\u02c4\u0007\u000b\u0000\u0000\u02c4\u02c5\u0006\'\uffff\uffff\u0000"+
		"\u02c5O\u0001\u0000\u0000\u0000\u02c6\u02c7\u0005\u001e\u0000\u0000\u02c7"+
		"\u02c8\u0006(\uffff\uffff\u0000\u02c8Q\u0001\u0000\u0000\u0000\u02c9\u02da"+
		"\u0003\u0006\u0003\u0000\u02ca\u02cb\u0003T*\u0000\u02cb\u02d7\u0003\u0006"+
		"\u0003\u0000\u02cc\u02cd\u0005\u0012\u0000\u0000\u02cd\u02d2\u0006)\uffff"+
		"\uffff\u0000\u02ce\u02d3\u0006)\uffff\uffff\u0000\u02cf\u02d0\u0003\u0006"+
		"\u0003\u0000\u02d0\u02d1\u0003T*\u0000\u02d1\u02d3\u0001\u0000\u0000\u0000"+
		"\u02d2\u02ce\u0001\u0000\u0000\u0000\u02d2\u02cf\u0001\u0000\u0000\u0000"+
		"\u02d3\u02d4\u0001\u0000\u0000\u0000\u02d4\u02d6\u0003\u0006\u0003\u0000"+
		"\u02d5\u02cc\u0001\u0000\u0000\u0000\u02d6\u02d9\u0001\u0000\u0000\u0000"+
		"\u02d7\u02d5\u0001\u0000\u0000\u0000\u02d7\u02d8\u0001\u0000\u0000\u0000"+
		"\u02d8\u02db\u0001\u0000\u0000\u0000\u02d9\u02d7\u0001\u0000\u0000\u0000"+
		"\u02da\u02ca\u0001\u0000\u0000\u0000\u02da\u02db\u0001\u0000\u0000\u0000"+
		"\u02db\u02ec\u0001\u0000\u0000\u0000\u02dc\u02dd\u0003\u0006\u0003\u0000"+
		"\u02dd\u02e7\u0006)\uffff\uffff\u0000\u02de\u02df\u0005\u0012\u0000\u0000"+
		"\u02df\u02e4\u0006)\uffff\uffff\u0000\u02e0\u02e5\u0006)\uffff\uffff\u0000"+
		"\u02e1\u02e2\u0003\u0006\u0003\u0000\u02e2\u02e3\u0003T*\u0000\u02e3\u02e5"+
		"\u0001\u0000\u0000\u0000\u02e4\u02e0\u0001\u0000\u0000\u0000\u02e4\u02e1"+
		"\u0001\u0000\u0000\u0000\u02e5\u02e6\u0001\u0000\u0000\u0000\u02e6\u02e8"+
		"\u0003\u0006\u0003\u0000\u02e7\u02de\u0001\u0000\u0000\u0000\u02e8\u02e9"+
		"\u0001\u0000\u0000\u0000\u02e9\u02e7\u0001\u0000\u0000\u0000\u02e9\u02ea"+
		"\u0001\u0000\u0000\u0000\u02ea\u02ec\u0001\u0000\u0000\u0000\u02eb\u02c9"+
		"\u0001\u0000\u0000\u0000\u02eb\u02dc\u0001\u0000\u0000\u0000\u02ecS\u0001"+
		"\u0000\u0000\u0000\u02ed\u02ee\u0003\u0012\t\u0000\u02ee\u02ef\u0006*"+
		"\uffff\uffff\u0000\u02ef\u0308\u0001\u0000\u0000\u0000\u02f0\u02f9\u0006"+
		"*\uffff\uffff\u0000\u02f1\u02f2\u0005A\u0000\u0000\u02f2\u02fa\u0006*"+
		"\uffff\uffff\u0000\u02f3\u02f4\u0005\u0006\u0000\u0000\u02f4\u02fa\u0006"+
		"*\uffff\uffff\u0000\u02f5\u02f6\u0005)\u0000\u0000\u02f6\u02fa\u0006*"+
		"\uffff\uffff\u0000\u02f7\u02f8\u0005C\u0000\u0000\u02f8\u02fa\u0006*\uffff"+
		"\uffff\u0000\u02f9\u02f1\u0001\u0000\u0000\u0000\u02f9\u02f3\u0001\u0000"+
		"\u0000\u0000\u02f9\u02f5\u0001\u0000\u0000\u0000\u02f9\u02f7\u0001\u0000"+
		"\u0000\u0000\u02fa\u02fb\u0001\u0000\u0000\u0000\u02fb\u02fc\u0003\u0006"+
		"\u0003\u0000\u02fc\u02fd\u0005\r\u0000\u0000\u02fd\u0303\u0006*\uffff"+
		"\uffff\u0000\u02fe\u02ff\u0003\u0006\u0003\u0000\u02ff\u0300\u0003\u0012"+
		"\t\u0000\u0300\u0301\u0006*\uffff\uffff\u0000\u0301\u0304\u0001\u0000"+
		"\u0000\u0000\u0302\u0304\u0001\u0000\u0000\u0000\u0303\u02fe\u0001\u0000"+
		"\u0000\u0000\u0303\u0302\u0001\u0000\u0000\u0000\u0304\u0305\u0001\u0000"+
		"\u0000\u0000\u0305\u0306\u0006*\uffff\uffff\u0000\u0306\u0308\u0001\u0000"+
		"\u0000\u0000\u0307\u02ed\u0001\u0000\u0000\u0000\u0307\u02f0\u0001\u0000"+
		"\u0000\u0000\u0308U\u0001\u0000\u0000\u00009\\ot\u0083\u0085\u008b\u008d"+
		"\u0092\u0098\u009a\u009d\u00a3\u00a5\u00bf\u00c2\u00c7\u00d1\u00d5\u00d7"+
		"\u00e8\u00f8\u0105\u0115\u012b\u0163\u0168\u0194\u01a0\u01ac\u01b7\u01c3"+
		"\u01d0\u01db\u01e7\u01f3\u0200\u020d\u021f\u0229\u0237\u025a\u025e\u0260"+
		"\u028a\u029c\u02a4\u02ad\u02b5\u02d2\u02d7\u02da\u02e4\u02e9\u02eb\u02f9"+
		"\u0303\u0307";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
