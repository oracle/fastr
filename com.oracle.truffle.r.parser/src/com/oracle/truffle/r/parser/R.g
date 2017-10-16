/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URISyntaxException;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.Argument;
}

@lexer::header {
//@formatter:off
package com.oracle.truffle.r.parser;

import com.oracle.truffle.r.runtime.RError;
}

@rulecatch {
    catch(RecognitionException re){
        throw re; // Stop at first error
    }
}

@members {
    private Source source;
    private Source initialSource;
    private RCodeBuilder<T> builder;
    private TruffleRLanguage language;
    private int fileStartOffset = 0;
    
    /**
     * Always use this constructor to initialize the R specific fields.
     */
    public RParser(Source source, RCodeBuilder<T> builder, TruffleRLanguage language) {
        super(new CommonTokenStream(new RLexer(new ANTLRStringStream(source.getCharacters().toString()))));
        assert source != null && builder != null;
        this.source = source;
        this.initialSource = source;
        this.builder = builder;
        this.language = language;
    }
    
    /**
     * Helper function that returns the last parsed token, usually used for building source sections.
     */
    private Token last() {
        return input.LT(-1);
    } 
    
    /**
     * Helper function to create a function lookup for the symbol in a given token.
     */
    private T operator(Token op) {
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
	    			    String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
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

	// without this override, the parser will not throw exceptions if it can recover    
    @Override
    protected Object recoverFromMismatchedToken(IntStream input, int expected, BitSet follow) throws RecognitionException {
        throw new MismatchedTokenException(expected, input);
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

	// without this override, the parser will not throw exceptions if it can recover    
    @Override
    public void reportError(RecognitionException e) {
        throw new IllegalArgumentException(e);
    }
}

@lexer::init{
    incompleteNesting = 0;
}

/****************************************************
** Known errors :
** - foo * if(...) ... because of priority
** - No help support '?' & '??'
** - %OP% not very robust, maybe allow everything
** - More than 3x '.' are handled like ...
** - '.' is a valid id
** - Line break are tolerated in strings even without a '\' !!! (ugly)
** - EOF does'nt work with unbalanced structs
** - Improve the stack of balanced structures
*****************************************************/

script returns [List<T> v]
    @init  {
        assert source != null && builder != null;
        $v = new ArrayList<T>();
    }
    @after {
        if (input.LT(1).getType() != EOF) {
        	throw new RecognitionException(input); 
        }
    }
    : n_ ( s=statement { $v.add($s.v); })*
    ;
    
root_function [String name] returns [RootCallTarget v]
    @init {
        assert source != null && builder != null;
        List<Argument<T>> params = new ArrayList<>();
    }
    @after {
        if (input.LT(1).getType() != EOF) {
        	throw RInternalError.shouldNotReachHere("not at EOF after parsing deserialized function"); 
        }
    }
    : n_ op=FUNCTION n_ LPAR  n_ (par_decl[params] (n_ COMMA n_ par_decl[params])* n_)? RPAR n_ body=expr_or_assign { $v = builder.rootFunction(language, src($op, last()), params, $body.v, name); }
    ;

statement returns [T v]
    : e=expr_or_assign n_one { $v = $e.v; }
    ;

n_ : (NEWLINE | COMMENT { checkFileDelim((CommonToken)last()); } 
     )*;
n_one  : (NEWLINE | COMMENT { checkFileDelim((CommonToken)last()); }
         )+ | EOF | SEMICOLON n_;
n_multi  : (NEWLINE | COMMENT { checkFileDelim((CommonToken)last()); }
            | SEMICOLON)+ | EOF;

expr_wo_assign returns [T v]
    : w=while_expr                                      { $v = $w.v; }
    | i=if_expr                                         { $v = $i.v; }
    | f=for_expr                                        { $v = $f.v; }
    | r=repeat_expr                                     { $v = $r.v; }
    | fun=function[null]                                { $v = $fun.v; }
    // break/next can be accompanied by arguments, but those are simply ignored
    | op=(NEXT|BREAK) ((LPAR)=>LPAR args[null] RPAR | ) { $v = builder.call(src($op), operator($op)); }
    ;

sequence returns [T v]
    @init  { ArrayList<Argument<T>> stmts = new ArrayList<>(); }
    : op=LBRACE n_multi?
      (
        e=expr_or_assign           { stmts.add(RCodeBuilder.argument($e.v)); }
        ( n_multi e=expr_or_assign { stmts.add(RCodeBuilder.argument($e.v)); } )*
        n_multi?
      )?
      RBRACE
      { $v = builder.call(src($op, last()), operator($op), stmts); }
    ;
    
expr returns [T v]
    @init { Token start = input.LT(1); T rhs = null; }
    : l=tilde_expr
      ( op=(ARROW | SUPER_ARROW) n_ ( (FUNCTION) => r=function[$l.v] { rhs = $r.v; } | r=expr { rhs = $r.v; } )
                                           { $v = builder.call(src(start, last()), operator($op), $l.v, rhs); }
      | op=RIGHT_ARROW n_ r=expr           { $v = builder.call(src(start, last()), builder.lookup(src($op), "<-", true), $r.v, $l.v); }
      | op=SUPER_RIGHT_ARROW n_ r=expr     { $v = builder.call(src(start, last()), builder.lookup(src($op), "<<-", true), $r.v, $l.v); }
      | { $v = $l.v; }
      )
    ;

expr_or_assign returns [T v]
    @init { Token start = input.LT(1); T rhs = null; }
    : l=tilde_expr
      ( (ARROW|SUPER_ARROW|ASSIGN) => op=(ARROW | SUPER_ARROW | ASSIGN) n_  ( (FUNCTION) => r=function[$l.v] { rhs = $r.v; } | r=expr_or_assign { rhs = $r.v; } )
                                                                      { $v = builder.call(src(start, last()), operator($op), $l.v, rhs); }
      | (RIGHT_ARROW)=>op=RIGHT_ARROW n_ r=expr_or_assign             { $v = builder.call(src(start, last()), builder.lookup(src($op), "<-", true), $r.v, $l.v); }
      | (SUPER_RIGHT_ARROW)=>op=SUPER_RIGHT_ARROW n_ r=expr_or_assign { $v = builder.call(src(start, last()), builder.lookup(src($op), "<<-", true), $r.v, $l.v); }
      | { $v = $l.v; }
      )
    ;

if_expr returns [T v]
    : op=IF n_ LPAR n_ cond=expr_or_assign n_ RPAR n_ t=expr_or_assign
      (
        (n_ ELSE)=>(options { greedy=false; backtrack = true; }: n_ ELSE n_ f=expr_or_assign
              { $v = builder.call(src($op, last()), operator($op), $cond.v, $t.v, $f.v); })
      |       { $v = builder.call(src($op, last()), operator($op), $cond.v, $t.v); }
      )
    ;

while_expr returns [T v]
    : op=WHILE n_ LPAR n_ c=expr_or_assign n_ RPAR n_ body=expr_or_assign { $v = builder.call(src($op, last()), operator($op), $c.v, $body.v); }
    ;

for_expr returns [T v]
    : op=FOR n_ LPAR n_ i=ID n_ IN n_ in=expr_or_assign n_ RPAR n_ body=expr_or_assign { $v = builder.call(src($op, last()), operator($op), builder.lookup(src($i), $i.text, false), $in.v, $body.v); }
    ;

repeat_expr returns [T v]
    : op=REPEAT n_ body=expr_or_assign { $v = builder.call(src($op, last()), operator($op), $body.v); } 
    ;

function [T assignedTo] returns [T v]
    @init { List<Argument<T>> params = new ArrayList<>(); }
    : op=FUNCTION n_ LPAR  n_ (par_decl[params] (n_ COMMA n_ par_decl[params])* n_)? RPAR n_ body=expr_or_assign { $v = builder.function(language, src($op, last()), params, $body.v, assignedTo); }
    ;

par_decl [List<Argument<T>> l]
    : i=ID                     { $l.add(RCodeBuilder.argument(src($i), $i.text, null)); }
    | i=ID n_ ASSIGN n_ e=expr { $l.add(RCodeBuilder.argument(src($i, last()), $i.text, $e.v)); }
    | v=VARIADIC               { $l.add(RCodeBuilder.argument(src($v), $v.text, null)); }
    // The 3 following cases were not handled ... and everything was working fine.
    // They are added for completeness, however note that a function created
    // with such a signature will always fail if it tries to access them!
    | VARIADIC n_ ASSIGN n_ expr { throw RInternalError.shouldNotReachHere("... = value parameter"); }
    | DD                         { throw RInternalError.shouldNotReachHere("..X parameter"); }
    | DD n_ ASSIGN n_ expr       { throw RInternalError.shouldNotReachHere("..X = value parameter"); }
    ;

tilde_expr returns [T v]
    : l=utilde_expr { $v = $l.v; }
      ( ((TILDE) => op=TILDE n_ r=utilde_expr { $v = builder.call(src($op, last()), operator($op), $v, $r.v); }) )*
    ;

utilde_expr returns [T v]
    : op=TILDE n_ l=or_expr { $v = builder.call(src($op, last()), operator($op), $l.v); }
    | l=or_expr             { $v = $l.v; }
    ;

or_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=and_expr { $v = $l.v; }
      ( ((or_operator)=>op=or_operator n_ r=and_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

and_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=not_expr { $v = $l.v; }
      ( ((and_operator)=>op=and_operator n_ r=not_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

not_expr returns [T v]
    : {true}? op=NOT n_ l=not_expr { $v = builder.call(src($op, last()), operator($op), $l.v); }
    | b=comp_expr         { $v = $b.v; }
    ;

comp_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=add_expr { $v = $l.v; }
      ( ((comp_operator)=>op=comp_operator n_ r=add_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

add_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=mult_expr { $v = $l.v; }
      ( ((add_operator)=>op=add_operator n_ r=mult_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

mult_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=operator_expr { $v = $l.v; }
      ( ((mult_operator)=>op=mult_operator n_ r=operator_expr { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); }) )*
    ;

operator_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=colon_expr { $v = $l.v; }
      ( (OP)=>op=OP n_ r=colon_expr { $v = builder.call(src(start, last()), operator($op), $v, $r.v); } )*
    ;

colon_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=unary_expression { $v = $l.v; }
      ( ((COLON)=>op=COLON n_ r=unary_expression { $v = builder.call(src(start, last()), operator($op), $v, $r.v); }) )*
    ;

unary_expression returns [T v]
    : op=(PLUS | MINUS | NOT | QM) n_ l=unary_expression { $v = builder.call(src($op, last()), operator($op), $l.v); }
    | b=power_expr                                  { $v = $b.v; }
    ;

power_expr returns [T v]
    @init { Token start = input.LT(1); }
    : l=basic_expr { $v = $l.v; }
      (
        ((power_operator)=>op=power_operator n_ r=unary_expression { $v = builder.call(src(start, last()), operator($op.v), $v, $r.v); } )
      |
      )
    ;

basic_expr returns [T v]
    @init { Token start = input.LT(1); }
    :
    (
      // special case for simple function call to generate "function" mode lookups
      ((ID|DD|VARIADIC|STRING) LPAR) => (lhsToken=(ID | DD | VARIADIC | STRING) op=LPAR a=args[null] y=RPAR
                      { $v = builder.call(src(start, $y), operator($lhsToken), $a.v); } )
    |
      lhs=simple_expr { $v = $lhs.v; }
    )
    (
      ((FIELD|AT|LBRAKET|LBB|LPAR) => (
          (op=(FIELD|AT) n_ name=id                    { $v = builder.call(src(start, last()), operator($op), $v, builder.constant(src($name.v), $name.v.getText())); })
        | (op=(FIELD|AT) n_ sname=conststring          { $v = builder.call(src(start, last()), operator($op), $v, $sname.v); })
        | (op=LBRAKET subset=args[$v] y=RBRAKET        {
                                                           if ($subset.v.size() == 1) {
                                                               $subset.v.add(RCodeBuilder.argumentEmpty());
                                                           }
                                                           $v = builder.call(src(start, $y), operator($op), $subset.v); 
                                                       })
        // must use RBRAKET twice instead of RBB because this is possible: a[b[1]]
        | (op=LBB subscript=args[$v] RBRAKET y=RBRAKET {
                                                           if ($subscript.v.size() == 1) {
                                                               $subscript.v.add(RCodeBuilder.argumentEmpty());
                                                           }
                                                           $v = builder.call(src(start, $y), operator($op), $subscript.v);
                                                       })
        | (op=LPAR a=args[null] y=RPAR                 { $v = builder.call(src(start, $y), $v, $a.v); })
        )
      )+
    | (n_)=>
    )
    ;

simple_expr returns [T v]
    @init { Token start = input.LT(1); }
    : i=id                                      { $v = builder.lookup(src($i.v), $i.text, false); }
    | b=bool                                    { $v = builder.constant(src(start, last()), $b.v); }
    | d=DD                                      { $v = builder.lookup(src($d), $d.text, false); }
    | t=NULL                                    { $v = builder.constant(src($t), RNull.instance); }
    | t=INF                                     { $v = builder.constant(src($t), Double.POSITIVE_INFINITY); }
    | t=NAN                                     { $v = builder.constant(src($t), Double.NaN); }
    | t=NAINT                                   { $v = builder.constant(src($t), RRuntime.INT_NA); }
    | t=NAREAL                                  { $v = builder.constant(src($t), RRuntime.DOUBLE_NA); }
    | t=NACHAR                                  { $v = builder.constant(src($t), RRuntime.STRING_NA); }
    | t=NACOMPL                                 { $v = builder.constant(src($t), RComplex.createNA()); }
    | num=number                                { $v = $num.v; }
    | cstr=conststring                          { $v = $cstr.v; }
    | pkg=id op=(NS_GET|NS_GET_INT) n_ comp=id {
        List<Argument<T>> args = new ArrayList<>();
        SourceSection pkgSource = src($pkg.v);
        SourceSection compSource = src($comp.v);
        args.add(RCodeBuilder.argument(pkgSource, "pkg", builder.lookup(pkgSource, $pkg.text, false)));
        args.add(RCodeBuilder.argument(compSource, "name", builder.lookup(compSource, $comp.text, false)));
        $v = builder.call(src($pkg.v, $comp.v), operator($op), args);
    }
    | op=LPAR n_ ea=expr_or_assign n_ y=RPAR    { $v = builder.call(src($op, $y), operator($op), $ea.v); }
    | s=sequence                                { $v = $s.v; }
    | e=expr_wo_assign                          { $v = $e.v; }
    ;

number returns [T v]
    : i=INTEGER {
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
    | d=DOUBLE  { $v = builder.constant(src($d), RRuntime.string2doubleNoCheck($d.text)); }
    | c=COMPLEX { $v = builder.constant(src($c), RComplex.valueOf(0, RRuntime.string2doubleNoCheck($c.text))); }
    ;

conststring returns [T v]
    : s=STRING { $v = builder.constant(src($s), $s.text); }
    ;

id returns [Token v]
    : ident=ID     { $v = $ident; }
    | var=VARIADIC { $v = $var; }
    ;

bool returns [byte v]
    : t=TRUE  { $v = RRuntime.LOGICAL_TRUE; }
    | t=FALSE { $v = RRuntime.LOGICAL_FALSE; }
    | t=NA    { $v = RRuntime.LOGICAL_NA; }
    ;

or_operator returns [Token v]
    : op=(OR | ELEMENTWISEOR) { $v = $op; }
    ;

and_operator returns [Token v]
    : op=(AND | ELEMENTWISEAND) { $v = $op; }
    ;

comp_operator returns [Token v]
    : op=(GT | GE | LT | LE | EQ | NE) { $v = $op; }
    ;

add_operator returns [Token v]
    : op=(PLUS | MINUS) { $v = $op; }
    ;

mult_operator returns [Token v]
    : op=(MULT | DIV | MOD ) { $v = $op; }
    ;

power_operator returns [Token v]
    : op=CARET { $v = $op; }
    ;

args [T firstArg] returns [List<Argument<T>> v]
    @init {
              $v = new ArrayList<>();
              if (firstArg != null) {
                  $v.add(RCodeBuilder.argument(firstArg));
              }
          }
    : n_ (arg_expr[v] n_ (COMMA ({ $v.add(RCodeBuilder.argumentEmpty()); } | n_ arg_expr[v]) n_)* )?
    | n_ { $v.add(RCodeBuilder.argumentEmpty()); } (COMMA ({ $v.add(RCodeBuilder.argumentEmpty()); } | n_ arg_expr[v]) n_)+
    ;

arg_expr [List<Argument<T>> l]
    @init { Token start = input.LT(1); }
    : e=expr                                                   { $l.add(RCodeBuilder.argument(src(start, last()), (String) null, $e.v)); }
    | name=(ID | VARIADIC | NULL | STRING) n_ ASSIGN n_ e=expr { $l.add(RCodeBuilder.argument(src($name, last()), argName($name.text), $e.v)); }
    | name=(ID | VARIADIC | NULL | STRING) n_ a=ASSIGN         { $l.add(RCodeBuilder.argument(src($name, $a), argName($name.text), null)); }
    ;

///
/// Lexer
///

COMMENT : '#' ~('\n'|'\r'|'\f')* (LINE_BREAK | EOF) { if(incompleteNesting > 0) $channel=HIDDEN; } ;

ARROW             : '<-' | ':=' ;
SUPER_ARROW       : '<<-' ;
RIGHT_ARROW       : '->' ;
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
MOD   : '%%' ;
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

WS      : ('\u0009'|'\u0020'|'\u00A0') { $channel=HIDDEN; } ;
NEWLINE : LINE_BREAK { if(incompleteNesting > 0) $channel=HIDDEN; } ;

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
    : '.'* ID_NAME
    | '.' '.'+ ('0'..'9')* ID_NAME
    | '.'
    | '.' '.'
    | '.' '.' '.' '.' '.'*
    | '`' BACKTICK_NAME
    ;

OP : '%' (~('%' | '\n' | '\r' | '\f'))+ '%' ;

fragment BACKTICK_NAME
    @init { final StringBuilder buf = new StringBuilder(); }
    : (
        (
          ESCAPE[buf]
        | i = ~( '\\' | '`' ) { buf.appendCodePoint(i); }
        )*
        '`'
        {
          if (buf.length() == 0) {
            throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
          }
          setText(buf.toString());
        }
      )
    ;

STRING
    @init { final StringBuilder buf = new StringBuilder(); }
    : (
        '\"'
        (
          ESCAPE[buf]
        | i = ~( '\\' | '"' ) { buf.appendCodePoint(i); }
        )*
        '\"'
        { setText(buf.toString()); }
      )
    | (
        '\''
        (
          ESCAPE[buf]
        | i = ~( '\\' | '\'' ) { buf.appendCodePoint(i); }
        )*
        '\''
        { setText(buf.toString()); }
      )
    ;

fragment ESCAPE [StringBuilder buf]
    : '\\'
      ( 't' { buf.append('\t'); }
      | 'n' { buf.append('\n'); }
      | 'a' { buf.appendCodePoint(7); }
      | 'v' { buf.appendCodePoint(11); }
      | 'r' { buf.append('\r'); }
      | 'b' { buf.append('\b'); }
      | 'f' { buf.append('\f'); }
      | '"' { buf.append('\"'); }
      | '`' { buf.append('`'); }
      | '\'' { buf.append('\''); }
      | ' ' { buf.append(' '); }
      | '\\' { buf.append('\\'); }
      | '\n' { buf.append('\n'); }
      | a = OCT_DIGIT b = OCT_DIGIT c = OCT_DIGIT { buf.append(octChar($a.text, $b.text, $c.text)); }
      | a = OCT_DIGIT b = OCT_DIGIT { buf.append(octChar($a.text, $b.text)); }
      | a = OCT_DIGIT { buf.append(octChar($a.text)); }
      | 'x' a = HEX_DIGIT b = HEX_DIGIT { buf.append(hexChar($a.text, $b.text)); }
      | 'u' a = HEX_DIGIT b = HEX_DIGIT? c = HEX_DIGIT? d = HEX_DIGIT? { buf.append(hexChar($a.text, $b.text, $c.text, $d.text)); }
      | 'U' a = HEX_DIGIT b = HEX_DIGIT? c = HEX_DIGIT? d = HEX_DIGIT? e = HEX_DIGIT? f = HEX_DIGIT? g = HEX_DIGIT? h = HEX_DIGIT? { buf.append(hexChar($a.text, $b.text, $c.text, $d.text, $e.text, $f.text, $g.text, $h.text)); }
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

fragment ID_NAME : ('a'..'z'|'A'..'Z'|'α'..'ω'|'Α'..'Ω'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'α'..'ω'|'Α'..'Ω'|'_'|'.')* ;

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
