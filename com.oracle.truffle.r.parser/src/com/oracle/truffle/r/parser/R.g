/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
grammar R;

/*
 * Parser for R source code
 *
 * Please note that you cannot use attributes like $start and $stop (or $var.stop, etc.),
 * because this introduces static inner classes that cannot be generic for type T.
 */

options {
    language = Java;
}

@header {
//Checkstyle: stop
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
}

@rulecatch {
    catch(RecognitionException re){
        throw re; // Stop at first error
    }
}

@parser::members {
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
	    			    String content = new String(RContext.getInstance().getEnv().getTruffleFile(path).readAllBytes(), StandardCharsets.UTF_8);
	    			    String lineEnding = detectLineEnding(initialSource.getCharacters());
	    			    content = convertToLineEnding(content, lineEnding);
                        source = RSource.fromFileName(content, path, false);
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

}
@lexer::members {
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
}

@lexer::init{
    incompleteNesting = 0;
}

/****************************************************
** Known errors (possibly outdated) :
** - foo * if(...) ... because of priority
** - %OP% not very robust, maybe allow everything
** - More than 3x '.' are handled like ...
** - '.' is a valid id
** - Line break are tolerated in strings even without a '\' !!! (ugly)
** - EOF does'nt work with unbalanced structs
** - Improve the stack of balanced structures
*****************************************************/

script returns [List<RSyntaxNode> v]
    @init  {
        assert source != null && builder != null;
        $v = new ArrayList<RSyntaxNode>();
    }
    @after {
        if (getInputStream().LT(1).getType() != EOF) {
        	throw new NoViableAltException(this);
        }
    }
    : n_ ( s=statement { $v.add($s.v); })*
    ;

root_function [String name] returns [RootCallTarget v]
    @init {
        assert source != null && builder != null;
        List<Argument<RSyntaxNode>> params = new ArrayList<>();
    }
    @after {
        if (getInputStream().LT(1).getType() != EOF) {
        	throw RInternalError.shouldNotReachHere("not at EOF after parsing deserialized function");
        }
    }
    : n_ op=FUNCTION{tok();} n_ LPAR{tok();}  n_ (par_decl[params] (n_ COMMA{tok();} n_ par_decl[params])* n_)? RPAR{tok();} n_ body=expr_or_assign { $v = builder.rootFunction(language, src($op, last()), params, $body.v, name); }
    ;

statement returns [RSyntaxNode v]
    : e=expr_or_assign n_one { $v = $e.v; }
    ;

n_ : (NEWLINE | COMMENT { checkFileDelim((CommonToken)last()); }
     )*;
n_one  : (NEWLINE | COMMENT { checkFileDelim((CommonToken)last()); }
         )+ | EOF | SEMICOLON n_;
n_multi  : (NEWLINE | COMMENT { checkFileDelim((CommonToken)last()); }
            | SEMICOLON)+ | EOF;

expr_wo_assign returns [RSyntaxNode v]
    : w=while_expr                                      { $v = $w.v; }
    | i=if_expr                                         { $v = $i.v; }
    | f=for_expr                                        { $v = $f.v; }
    | r=repeat_expr                                     { $v = $r.v; }
    | fun=function[null]                                { $v = $fun.v; }
    // break/next can be accompanied by arguments, but those are simply ignored
    | op=(NEXT|BREAK){tok();} (LPAR{tok();} args[null] RPAR{tok();} | ) { $v = builder.call(src($op), operator($op)); }
    ;

sequence returns [RSyntaxNode v]
    @init  { ArrayList<Argument<RSyntaxNode>> stmts = new ArrayList<>(); }
    : op=LBRACE{tok();} n_multi?
      (
        e=expr_or_assign           { stmts.add(RCodeBuilder.argument($e.v)); }
        ( n_multi e=expr_or_assign { stmts.add(RCodeBuilder.argument($e.v)); } )*
        n_multi?
      )?
      RBRACE{tok();}
      { $v = builder.call(src($op, last()), operator($op), stmts); }
    ;

expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); RSyntaxNode rhs = null; }
    : l=tilde_expr { $v = $l.v; }
      ( op=(ARROW | SUPER_ARROW){tok();} n_ ( r1=function[$l.v] { rhs = $r1.v; } | r2=expr { rhs = $r2.v; } )
                                           { $v = builder.call(src(start, last()), operator($op), $l.v, rhs); }
      | op=RIGHT_ARROW{tok();} n_ r3=expr           { $v = builder.call(src(start, last()), builder.lookup(src($op), "<-", true), $r3.v, $l.v); }
      | op=SUPER_RIGHT_ARROW{tok();} n_ r4=expr     { $v = builder.call(src(start, last()), builder.lookup(src($op), "<<-", true), $r4.v, $l.v); }
      )?
    ;

expr_or_assign returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); RSyntaxNode rhs = null; }
    : l=tilde_expr { $v = $l.v; }
      ( op=(ARROW | SUPER_ARROW | ASSIGN){tok();} n_  ( r1=function[$l.v] { rhs = $r1.v; } | r2=expr_or_assign { rhs = $r2.v; } )
                                                                      { $v = builder.call(src(start, last()), operator($op), $l.v, rhs); }
      | op=RIGHT_ARROW{tok();} n_ r3=expr_or_assign             { $v = builder.call(src(start, last()), builder.lookup(src($op), "<-", true), $r3.v, $l.v); }
      | op=SUPER_RIGHT_ARROW{tok();} n_ r4=expr_or_assign { $v = builder.call(src(start, last()), builder.lookup(src($op), "<<-", true), $r4.v, $l.v); }
      )?
    ;

if_expr returns [RSyntaxNode v]
    : op=IF{tok();} n_ LPAR{tok();} n_ cond=expr_or_assign n_ RPAR{tok();} n_ t=expr_or_assign
      (
        ( n_ ELSE{tok();} n_ f=expr_or_assign
              { $v = builder.call(src($op, last()), operator($op), $cond.v, $t.v, $f.v); })
      |       { $v = builder.call(src($op, last()), operator($op), $cond.v, $t.v); }
      )
    ;

while_expr returns [RSyntaxNode v]
    : op=WHILE{tok();} n_ LPAR{tok();} n_ c=expr_or_assign n_ RPAR{tok();} n_ body=expr_or_assign { $v = builder.call(src($op, last()), operator($op), $c.v, $body.v); }
    ;

for_expr returns [RSyntaxNode v]
    : op=FOR{tok();} n_ LPAR{tok();} n_ i=ID n_ IN{tok();} n_ in=expr_or_assign n_ RPAR{tok();} n_ body=expr_or_assign { $v = builder.call(src($op, last()), operator($op), builder.lookup(src($i), $i.text, false), $in.v, $body.v); }
    ;

repeat_expr returns [RSyntaxNode v]
    : op=REPEAT{tok();} n_ body=expr_or_assign { $v = builder.call(src($op, last()), operator($op), $body.v); }
    ;

function [RSyntaxNode assignedTo] returns [RSyntaxNode v]
    @init { List<Argument<RSyntaxNode>> params = new ArrayList<>(); }
    : op=FUNCTION{tok();} n_ LPAR{tok();} n_ (par_decl[params] (n_ COMMA{tok();} n_ par_decl[params])* n_)? RPAR{tok();} n_ body=expr_or_assign { $v = builder.function(language, src($op, last()), params, $body.v, assignedTo); }
    ;

par_decl [List<Argument<RSyntaxNode>> l]
    : i=ID{tok();}                                                      { $l.add(argument(src($i), $i.text, null)); }
    | i=ID{tok();} n_ a=ASSIGN{tok(RCodeToken.EQ_FORMALS);} n_ e=expr   { $l.add(argument(src($i, last()), $i.text, $e.v)); }
    | v=VARIADIC{tok();}                                                { $l.add(argument(src($v), $v.text, null)); }
    // The 3 following cases (e.g. "...=42") are weirdness of the reference implementation,
    // the formal argument must be actually created, because they play their role in positional argument matching,
    // but the expression for the default value (if any) is never executed and the value of the paremter
    // cannot be accessed (at least it seems so).
    | v=VARIADIC{tok();} n_ a=ASSIGN{tok(RCodeToken.EQ_FORMALS);} n_ e=expr { $l.add(argument(src($v), $v.text,  null)); }
    | v=DD{tok();}                                                          { $l.add(argument(src($v), $v.text, null)); }
    | v=DD{tok();} n_ a=ASSIGN{tok(RCodeToken.EQ_FORMALS);} n_ expr         { $l.add(argument(src($v), $v.text, null)); }
    ;

tilde_expr returns [RSyntaxNode v]
    : l=utilde_expr { $v = $l.v; }
      ( (op=TILDE{tok();} n_ r=utilde_expr { $v = builder.call(src($op, last()), operator($op), $v, $r.v); }) )*
    ;

utilde_expr returns [RSyntaxNode v]
    : op=TILDE{tok();} n_ l1=utilde_expr { $v = builder.call(src($op, last()), operator($op), $l1.v); }
    | l2=or_expr             { $v = $l2.v; }
    ;

or_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=and_expr { $v = $l.v; }
      ( (op=or_operator n_ r=and_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

and_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=not_expr { $v = $l.v; }
      ( (op=and_operator n_ r=not_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

not_expr returns [RSyntaxNode v]
    : {true}? op=NOT{tok();} n_ l=not_expr { $v = builder.call(src($op, last()), operator($op), $l.v); }
    | b=comp_expr         { $v = $b.v; }
    ;

comp_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=add_expr { $v = $l.v; }
      ( (op=comp_operator n_ r=add_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

add_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=mult_expr { $v = $l.v; }
      ( (op=add_operator n_ r=mult_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

mult_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=operator_expr { $v = $l.v; }
      ( (op=mult_operator n_ r=operator_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

operator_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=colon_expr { $v = $l.v; }
      ( op=OP{tok();} n_ r=colon_expr { $v = builder.call(src(start, last()), operator($op), $v, $r.v); } )*
    ;

colon_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=unary_expression { $v = $l.v; }
      ( (op=COLON{tok();} n_ r=unary_expression { $v = builder.call(src(start, last()), operator($op), $v, $r.v); }) )*
    ;

unary_expression returns [RSyntaxNode v]
    : op=(PLUS | MINUS | NOT | QM){tok();} n_ l1=unary_expression { $v = builder.call(src($op, last()), operator($op), $l1.v); }
	| op=TILDE {tok();} n_ l2=utilde_expr { $v = builder.call(src($op, last()), operator($op), $l2.v); }
    | b=power_expr                                  { $v = $b.v; }
    ;

power_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    : l=basic_expr { $v = $l.v; }
      (
        (op=power_operator n_ r=unary_expression { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); } )
      |
      )
    ;

basic_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); }
    :
    (
      // special case for simple function call to generate "function" mode lookups
      (lhsToken=(ID | DD | VARIADIC | STRING){tok(RCodeToken.SYMBOL_FUNCTION_CALL);} op=LPAR{tok();} a=args[null] y=RPAR{tok();}
                      { $v = builder.call(src(start, $y), functionLookup($lhsToken), $a.v); } )
    |
      lhs=simple_expr { $v = $lhs.v; }
    )
    (
      ( (
          (op=(FIELD|AT){tok();} n_ name=id                    { modifyTok(RCodeToken.SLOT); $v = builder.call(src(start, last()), operator($op), $v, builder.lookup(src($name.v), $name.v.getText(), false)); })
        | (op=(FIELD|AT){tok();} n_ sname=conststring          { $v = builder.call(src(start, last()), operator($op), $v, $sname.v); })
        | (op=LBRAKET{tok();} subset=args[$v] y=RBRAKET        { tok();
                                                           if ($subset.v.size() == 1) {
                                                               $subset.v.add(RCodeBuilder.argumentEmpty());
                                                           }
                                                           $v = builder.call(src(start, $y), operator($op), $subset.v);
                                                       })
        // must use RBRAKET twice instead of RBB because this is possible: a[b[1]]
        | (op=LBB{tok();} subscript=args[$v] RBRAKET{tok();} y=RBRAKET { tok();
                                                           if ($subscript.v.size() == 1) {
                                                               $subscript.v.add(RCodeBuilder.argumentEmpty());
                                                           }
                                                           $v = builder.call(src(start, $y), operator($op), $subscript.v);
                                                       })
        | (op=LPAR{tok();} a=args[null] y=RPAR{tok();}                 { $v = builder.call(src(start, $y), $v, $a.v); })
        )
      )+
    )?
    ;

simple_expr returns [RSyntaxNode v]
    @init { Token start = getInputStream().LT(1); List<Argument<RSyntaxNode>> args = new ArrayList<>(); Token compToken = null; }
    : i=id                                      { $v = builder.lookup(src($i.v), $i.text, false); }
    | b=bool                                    { $v = builder.constant(src(start, last()), $b.v); }
    | d=DD                                      { tok(); $v = builder.lookup(src($d), $d.text, false); }
    | t=NULL                                    { tok(); $v = builder.constant(src($t), RNull.instance); }
    | t=INF                                     { tok(); $v = builder.constant(src($t), Double.POSITIVE_INFINITY); }
    | t=NAN                                     { tok(); $v = builder.constant(src($t), Double.NaN); }
    | t=NAINT                                   { tok(); $v = builder.constant(src($t), RRuntime.INT_NA); }
    | t=NAREAL                                  { tok(); $v = builder.constant(src($t), RRuntime.DOUBLE_NA); }
    | t=NACHAR                                  { tok(); $v = builder.constant(src($t), RRuntime.STRING_NA); }
    | t=NACOMPL                                 { tok(); $v = builder.constant(src($t), RComplex.createNA()); }
    | num=number                                { $v = $num.v; }
    | cstr=conststring                          { $v = $cstr.v; }
    | pkg=id{modifyTok(RCodeToken.SYMBOL_PACKAGE);} op=(NS_GET|NS_GET_INT){tok();} n_          {
        SourceSection pkgSource = src($pkg.v);
        args.add(argument(pkgSource, (String) null, builder.lookup(pkgSource, $pkg.text, false)));
        }
      ( compId=id                               {
        SourceSection compSource = src($compId.v);
        compToken = $compId.v;
        args.add(argument(compSource, (String) null, builder.lookup(compSource, $compId.text, false)));
        }
      | compString=STRING{tok();}                       {
        SourceSection compSource = src($compString);
        compToken = $compString;
        args.add(argument(compSource, (String) null, builder.constant(compSource, $compString.text)));
        }
        )                                       { $v = builder.call(src($pkg.v, compToken), operator($op), args); }
    | op=LPAR{tok();} n_ ea=expr_or_assign n_ y=RPAR    { tok(); $v = builder.call(src($op, $y), operator($op), $ea.v); }
    | s=sequence                                { $v = $s.v; }
    | e=expr_wo_assign                          { $v = $e.v; }
    ;

number returns [RSyntaxNode v]
    : i=INTEGER { tok();
        double value = RRuntime.string2doubleNoCheck($i.text);
        if (value == (int) value) {
            if ($i.text.indexOf('.') != -1) {
                RError.warning(RError.NO_CALLER, RError.Message.INTEGER_VALUE_UNNECESARY_DECIMAL, $i.text + "L");
            }
            $v = builder.constant(src($i), (int) value);
        } else {
            if ($i.text.indexOf('.') != -1) {
                RError.warning(RError.NO_CALLER, RError.Message.INTEGER_VALUE_DECIMAL, $i.text + "L");
            } else if ($i.text.startsWith("0x")) {
                RError.warning(RError.NO_CALLER, RError.Message.NON_INTEGER_VALUE, $i.text);
            } else {
                RError.warning(RError.NO_CALLER, RError.Message.NON_INTEGER_VALUE, $i.text + "L");
            }
            $v = builder.constant(src($i), value);
        }
      }
    | d=DOUBLE  { tok(); $v = builder.constant(src($d), RRuntime.string2doubleNoCheck($d.text)); }
    | c=COMPLEX { tok(); $v = builder.constant(src($c), RComplex.valueOf(0, RRuntime.string2doubleNoCheck($c.text))); }
    ;

conststring returns [RSyntaxNode v]
    : s=STRING { tok(); $v = builder.constant(src($s), $s.text); }
    ;

id returns [Token v]
    : ident=ID     { tok(); $v = $ident; }
    | var=VARIADIC { tok(); $v = $var; }
    ;

bool returns [byte v]
    : t=TRUE  { tok(); $v = RRuntime.LOGICAL_TRUE; }
    | t=FALSE { tok(); $v = RRuntime.LOGICAL_FALSE; }
    | t=NA    { tok(); $v = RRuntime.LOGICAL_NA; }
    ;

or_operator returns [Token v]
    : op=(OR | ELEMENTWISEOR) { tok(); $v = $op; }
    ;

and_operator returns [Token v]
    : op=(AND | ELEMENTWISEAND) { tok(); $v = $op; }
    ;

comp_operator returns [Token v]
    : op=(GT | GE | LT | LE | EQ | NE) { tok(); $v = $op; }
    ;

add_operator returns [Token v]
    : op=(PLUS | MINUS) { tok(); $v = $op; }
    ;

mult_operator returns [Token v]
    : op=(MULT | DIV) { tok(); $v = $op; }
    ;

power_operator returns [Token v]
    : op=CARET { tok(); $v = $op; }
    ;

args [RSyntaxNode firstArg] returns [List<Argument<RSyntaxNode>> v]
    @init {
              $v = new ArrayList<>();
              if (firstArg != null) {
                  $v.add(RCodeBuilder.argument(firstArg));
              }
          }
    : n_ (arg_expr[$v] n_ (COMMA{tok();} ({ $v.add(RCodeBuilder.argumentEmpty()); } | n_ arg_expr[$v]) n_)* )?
    | n_ { $v.add(RCodeBuilder.argumentEmpty()); } (COMMA{tok();} ({ $v.add(RCodeBuilder.argumentEmpty()); } | n_ arg_expr[$v]) n_)+
    ;

arg_expr [List<Argument<RSyntaxNode>> l]
    @init { Token start = getInputStream().LT(1); }
    : e=expr                                                   { $l.add(argument(src(start, last()), (String) null, $e.v)); }
    | { Token name = null; RSyntaxNode value = null; }
      (ID{name = $ID; tok(RCodeToken.SYMBOL_SUB);} | VARIADIC{name=$VARIADIC; tok();} | NULL{name = $NULL; tok();} | STRING{name = $STRING; tok();}) n_ a=ASSIGN{tok(RCodeToken.EQ_SUB);}
      ( n_ e=expr { value = $e.v; }
      |
      )
      { $l.add(argument(src(name, last()), argName(name.getText()), value)); }
    ;

///
/// Lexer
///

COMMENT : '#' ~('\n'|'\r'|'\f')* (LINE_BREAK | EOF) { if (incompleteNesting > 0) skip(); } ;

ARROW             : '<-' | ':=' ;
SUPER_ARROW       : '<<-' ;
RIGHT_ARROW       : '->';
SUPER_RIGHT_ARROW : '->>' ;
VARIADIC          : '...' ;

EQ     : '==';
NE     : '!=' ;
GE     : '>=' ;
LE     : '<=' ;
GT     : '>' ;
LT     : '<' ;
ASSIGN : '=' ;

NS_GET_INT : ':::' ;
NS_GET     : '::' ;

COLON     : ':' ;
SEMICOLON : ';' ;
COMMA     : ',' ;

AND            : '&&' ;
ELEMENTWISEAND : '&' ;
OR             : '||' ;
ELEMENTWISEOR  : '|' ;

LBRACE  : '{' { nestingStack.add(incompleteNesting); incompleteNesting = 0; } ;
RBRACE  : '}' { if (!nestingStack.isEmpty()) { incompleteNesting = nestingStack.remove(nestingStack.size() - 1); } } ;
LPAR    : '(' { incompleteNesting++; } ;
RPAR    : ')' { incompleteNesting--; } ;
LBB     : '[[' { incompleteNesting+=2; } ;
LBRAKET : '[' { incompleteNesting++; } ;
RBRAKET : ']' { incompleteNesting--; } ;

CARET : '^' | '**' ;
TILDE : '~' ;
NOT   : '!' ;
QM    : '?' ;
PLUS  : '+' ;
MULT  : '*' ;
DIV   : '/' ;
MINUS : '-' ;

FIELD : '$' ;
AT    : '@' ;

FUNCTION : 'function' ;
NULL     : 'NULL' ;
NA       : 'NA' ;
NAINT    : 'NA_integer_' ;
NAREAL   : 'NA_real_' ;
NACHAR   : 'NA_character_' ;
NACOMPL  : 'NA_complex_' ;
TRUE     : 'TRUE' ;
FALSE    : 'FALSE' ;
INF      : 'Inf' ;
NAN      : 'NaN' ;

WHILE  : 'while' ;
FOR    : 'for' ;
REPEAT : 'repeat' ;
IN     : 'in' ;
IF     : 'if' ;
ELSE   : 'else' ;
NEXT   : 'next' ;
BREAK  : 'break' ;

WS      : ('\u0009'|'\u0020'|'\u00A0') -> skip ;
NEWLINE : LINE_BREAK { if (incompleteNesting > 0) skip(); };

INTEGER
    : ('0'..'9')+ '.' ('0'..'9')* 'L' { setText(getText().substring(0, getText().length()-1)); }
    | '.'? ('0'..'9')+ EXPONENT? 'L' { setText(getText().substring(0, getText().length()-1)); }
    | '0x' HEX_DIGIT+ 'L' { setText(getText().substring(0, getText().length()-1)); }
    ;

COMPLEX
    : ('0'..'9')+ '.' ('0'..'9')* EXPONENT? 'i' { setText(getText().substring(0, getText().length()-1)); }
    | '.'? ('0'..'9')+ EXPONENT? 'i' { setText(getText().substring(0, getText().length()-1)); }
    | '0x' HEX_DIGIT+ ('.'? HEX_DIGIT* HEX_EXPONENT)? 'i' { setText(getText().substring(0, getText().length()-1)); }
    ;

DOUBLE
    : ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    | '.'? ('0'..'9')+ EXPONENT?
    | '0x' HEX_DIGIT+ ('.'? HEX_DIGIT* HEX_EXPONENT)?
    ;

DD : '..' ('0'..'9')+ ;

ID
    :
    ( '.'* ID_NAME
    | '.' '.'+ ('0'..'9')* ID_NAME
    | '.'
    | '.' '.'
    | '.' '.' '.' '.' '.'*
    | '`' BACKTICK_NAME
    ) { if (getText().startsWith("`")) setText(parseString(getText())); }
    ;

OP : '%' (~('%' | '\n' | '\r' | '\f'))* '%' ;

fragment BACKTICK_NAME
    :
        (
          ESCAPE
        | ~( '\\' | '`' )
        )*
        '`'
    ;

STRING
    :( (
        '"'
        (
          ESCAPE
        | ~( '\\' | '"' )
        )*
        '"'
      )
    | (
        '\''
        (
          ESCAPE
        | ~( '\\' | '\'' )
        )*
        '\''
      ) ) { setText(parseString(getText())); }
    ;

fragment ESCAPE
    : '\\'
      ( 't'
      | 'n'
      | 'a'
      | 'v'
      | 'r'
      | 'b'
      | 'f'
      | '"'
      | '`'
      | '\''
      | ' '
      | '\\'
      | '\n'
      | OCT_DIGIT OCT_DIGIT? OCT_DIGIT?
      | 'x' HEX_DIGIT HEX_DIGIT
      | 'u' HEX_DIGIT HEX_DIGIT? HEX_DIGIT? HEX_DIGIT?
      | 'U' HEX_DIGIT HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT?
      | 'u' '{' HEX_DIGIT HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? '}'
      | 'U' '{' HEX_DIGIT HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? '}'
      )
      ;

fragment LINE_BREAK
    : (('\f'|'\r')? '\n')
    | ('\n'? ('\r'|'\f')) // This rule fixes very old Mac/Dos/Windows encoded files
    ;

fragment EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;
fragment HEX_EXPONENT : ('p'|'P') ('+'|'-')? ('0'..'9')+ ;

fragment OP_NAME
    : ID_NAME
    | ('*'|'/'|'+'|'-'|'>'|'<'|'='|'|'|'&'|':'|'^'|'.'|'~'|','|'?')
    ;

fragment ID_NAME : [\p{Alpha}_] [\p{Alnum}_.]* ;

fragment ESC_SEQ
    : '\\' ('b'|'t'|'n'|'f'|'r'|'"'|'\''|'`'|'\\'|' '|'a'|'v')
    | '\\' LINE_BREAK // FIXME that's an ugly way to fix this
    | UNICODE_ESC
    | OCTAL_ESC
    | HEX_ESC
    ;

fragment UNICODE_ESC : '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT ;
fragment HEX_ESC     : '\\x' HEX_DIGIT HEX_DIGIT? ;
fragment HEX_DIGIT   : ('0'..'9'|'a'..'f'|'A'..'F') ;
fragment OCT_DIGIT   : ('0'..'7') ;

fragment OCTAL_ESC
    : '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    | '\\' ('0'..'7') ('0'..'7')
    | '\\' ('0'..'7')
    ;
