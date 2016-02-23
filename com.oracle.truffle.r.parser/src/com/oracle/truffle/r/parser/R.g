/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
grammar R;

options {
    language = Java;
}

@header {
//Checkstyle: stop
//@formatter:off
package com.oracle.truffle.r.parser;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.impl.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.parser.ast.Call.*;
}

@lexer::header {
//@formatter:off
package com.oracle.truffle.r.parser;
}

@rulecatch {
    catch(RecognitionException re){
        throw re; // Stop at first error
    }
}

@members {
    private Source source;
    private RCodeBuilder<ASTNode, ArgNode> builder;

    /**
     * Initialization method - needs to be called before the parser is actually used.
     */
    public void initialize(Source s, RCodeBuilder<ASTNode, ArgNode> b) {
        this.source = s;
        this.builder = b;
    }
    
    /**
     * Helper function to create a function lookup for the symbol in a given token.
     */
    private ASTNode operator(Token op) {
        return builder.lookup(src(op), op.getText(), true);
    }
    
    /**
     * Create a {@link SourceSection} from a single token.
     */
    private SourceSection src(Token t) {
        CommonToken token = (CommonToken) t;
        int startIndex = token.getStartIndex();
        return source.createSection(null, token.getLine(), token.getCharPositionInLine() + 1, startIndex, token.getStopIndex() - startIndex + 1);
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
        return source.createSection(null, cstart.getLine(), cstart.getCharPositionInLine() + 1, startIndex, length);
    }

	// without this override, the parser will not throw exceptions if it can recover    
    @Override
    protected Object recoverFromMismatchedToken(IntStream input, int expected, BitSet follow) throws RecognitionException {
        throw new MismatchedTokenException(expected, input);
    }
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

script returns [List<ASTNode> v]
    @init  { $v = new ArrayList<ASTNode>(); }
    @after {
        if (input.LT(1).getType() != EOF) {
        	throw new RecognitionException(input); 
        }
    }
    : n_ ( s=statement { $v.add($s.v); })*
    ;

statement returns [ASTNode v]
    : e=expr_or_assign n_one { $v = $e.v; }
    ;

n_ : (NEWLINE | COMMENT)*;
n_one  : (NEWLINE | COMMENT)+ | EOF | SEMICOLON n_;
n_multi  : (NEWLINE | COMMENT | SEMICOLON)+ | EOF;

expr_or_assign returns [ASTNode v]
    : a=alter_assign { $v = $a.v; }
    ;

expr returns [ASTNode v]
    : a=assign { $v = $a.v; }
    ;

expr_wo_assign returns [ASTNode v]
    : w=while_expr                                      { $v = $w.v; }
    | i=if_expr                                         { $v = $i.v; }
    | f=for_expr                                        { $v = $f.v; }
    | r=repeat_expr                                     { $v = $r.v; }
    | fun=function                                      { $v = $fun.v; }
    // break/next can be accompanied by arguments, but those are simply ignored
    | op=(NEXT|BREAK) ((LPAR)=>LPAR args[null] RPAR | ) { $v = builder.call(src($op), operator($op)); }
    ;

sequence returns [ASTNode v]
    @init  { ArrayList<ArgNode> stmts = new ArrayList<>(); }
    @after { $v = builder.call(src($start, $stop), operator($op), stmts.toArray(new ArgNode[stmts.size()])); }
    : op=LBRACE n_multi?
      (
        e=expr_or_assign           { stmts.add(builder.argument($e.v)); }
        ( n_multi e=expr_or_assign { stmts.add(builder.argument($e.v)); } )*
        n_multi?
      )?
      RBRACE
    ;

assign returns [ASTNode v]
    : l=tilde_expr
      ( op=(ARROW | SUPER_ARROW) n_ r=expr { $v = builder.call(src($l.start, $r.stop), operator($op), builder.argument($l.v), builder.argument($r.v)); }
      | op=RIGHT_ARROW n_ r=expr           { $v = builder.call(src($l.start, $r.stop), builder.lookup(src($op), "<-", true), builder.argument($r.v), builder.argument($l.v)); }
      | op=SUPER_RIGHT_ARROW n_ r=expr     { $v = builder.call(src($l.start, $r.stop), builder.lookup(src($op), "<<-", true), builder.argument($r.v), builder.argument($l.v)); }
      | { $v = $l.v; }
      )
    ;

alter_assign returns [ASTNode v]
    : l=tilde_expr
      ( (ARROW|SUPER_ARROW|ASSIGN) => op=(ARROW | SUPER_ARROW | ASSIGN) n_ r=expr_or_assign
                                                                      { $v = builder.call(src($l.start, $r.stop), operator($op), builder.argument($l.v), builder.argument($r.v)); }
      | (RIGHT_ARROW)=>op=RIGHT_ARROW n_ r=expr_or_assign             { $v = builder.call(src($l.start, $r.stop), builder.lookup(src($op), "<-", true), builder.argument($r.v), builder.argument($l.v)); }
      | (SUPER_RIGHT_ARROW)=>op=SUPER_RIGHT_ARROW n_ r=expr_or_assign { $v = builder.call(src($l.start, $r.stop), builder.lookup(src($op), "<<-", true), builder.argument($r.v), builder.argument($l.v)); }
      | { $v = $l.v; }
      )
    ;

if_expr returns [ASTNode v]
    : op=IF n_ LPAR n_ cond=expr_or_assign n_ RPAR n_ t=expr_or_assign
      (
        (n_ ELSE)=>(options { greedy=false; backtrack = true; }: n_ ELSE n_ f=expr_or_assign
              { $v = builder.call(src($start, $f.stop), operator($op), builder.argument($cond.v), builder.argument($t.v), builder.argument($f.v)); })
      |       { $v = builder.call(src($start, $t.stop), operator($op), builder.argument($cond.v), builder.argument($t.v)); }
      )
    ;

while_expr returns [ASTNode v]
    @after {
        $v = builder.call(src($start, $stop), operator($op), builder.argument($c.v), builder.argument($body.v));
    }
    : op=WHILE n_ LPAR n_ c=expr_or_assign n_ RPAR n_ body=expr_or_assign
    ;

for_expr returns [ASTNode v]
    @after {
        $v = builder.call(src($start, $stop), operator($op), builder.argument(builder.lookup(src($i), $i.text, false)), builder.argument($in.v), builder.argument($body.v));
    }
    : op=FOR n_ LPAR n_ i=ID n_ IN n_ in=expr_or_assign n_ RPAR n_ body=expr_or_assign
    ;

repeat_expr returns [ASTNode v]
    @after {
        $v = builder.call(src($start, $stop), operator($op), builder.argument($body.v));
    }
    : op=REPEAT n_ body=expr_or_assign 
    ;

function returns [ASTNode v]
    @init {
        List<ArgNode> params = new ArrayList<>();
    }
    @after {
        $v = builder.function(src($start, $stop), params, $body.v);
    }
    : FUNCTION n_ LPAR  n_ (par_decl[params] (n_ COMMA n_ par_decl[params])* n_)? RPAR n_ body=expr_or_assign
    ;

par_decl [List<ArgNode> l]
    : i=ID                     { $l.add(builder.argument(src($i), $i.text, null)); }
    | i=ID n_ ASSIGN n_ e=expr { $l.add(builder.argument(src($i, $e.stop), $i.text, $e.v)); }
    | v=VARIADIC               { $l.add(builder.argument(src($v), $v.text, null)); }
    // The 3 following cases were not handled ... and everything was working fine.
    // They are added for completeness, however note that a function created
    // with such a signature will always fail if it tries to access them!
    | VARIADIC n_ ASSIGN n_ expr { throw RInternalError.shouldNotReachHere("... = value parameter"); }
    | DD                         { throw RInternalError.shouldNotReachHere("..X parameter"); }
    | DD n_ ASSIGN n_ expr       { throw RInternalError.shouldNotReachHere("..X = value parameter"); }
    ;

tilde_expr returns [ASTNode v]
    : l=utilde_expr { $v = $l.v; }
      ( ((TILDE) => op=TILDE n_ r=utilde_expr { $v = builder.call(src($l.start, $r.stop), operator($op), builder.argument($v), builder.argument($r.v)); }) )*
    ;

utilde_expr returns [ASTNode v]
    : op=TILDE n_ l=or_expr { $v = builder.call(src($op, $l.stop), operator($op), builder.argument($l.v)); }
    | l=or_expr             { $v = $l.v; }
    ;

or_expr returns [ASTNode v]
    : l=and_expr { $v = $l.v; }
      ( ((or_operator)=>op=or_operator n_ r=and_expr { $v = builder.call(src($l.start, $r.stop), operator($op.v), builder.argument($v), builder.argument($r.v)); }) )*
    ;

and_expr returns [ASTNode v]
    : l=not_expr { $v = $l.v; }
      ( ((and_operator)=>op=and_operator n_ r=not_expr { $v = builder.call(src($l.start, $r.stop), operator($op.v), builder.argument($v), builder.argument($r.v)); }) )*
    ;

not_expr returns [ASTNode v]
    : {true}? op=NOT n_ l=not_expr { $v = builder.call(src($op, $l.stop), operator($op), builder.argument($l.v)); }
    | b=comp_expr         { $v = $b.v; }
    ;

comp_expr returns [ASTNode v]
    : l=add_expr { $v = $l.v; }
      ( ((comp_operator)=>op=comp_operator n_ r=add_expr { $v = builder.call(src($l.start, $r.stop), operator($op.v), builder.argument($v), builder.argument($r.v)); }) )*
    ;

add_expr returns [ASTNode v]
    : l=mult_expr { $v = $l.v; }
      ( ((add_operator)=>op=add_operator n_ r=mult_expr { $v = builder.call(src($l.start, $r.stop), operator($op.v), builder.argument($v), builder.argument($r.v)); }) )*
    ;

mult_expr returns [ASTNode v]
    : l=operator_expr { $v = $l.v; }
      ( ((mult_operator)=>op=mult_operator n_ r=operator_expr { $v = builder.call(src($l.start, $r.stop), operator($op.v), builder.argument($v), builder.argument($r.v)); }) )*
    ;

operator_expr returns [ASTNode v]
    : l=colon_expr { $v = $l.v; }
      ( (OP)=>op=OP n_ r=colon_expr { $v = builder.call(src($l.start, $r.stop), operator($op), builder.argument($v), builder.argument($r.v)); } )*
    ;

colon_expr returns [ASTNode v] // FIXME
    : l=unary_expression { $v = $l.v; }
      ( ((COLON)=>op=COLON n_ r=unary_expression { $v = builder.call(src($l.start, $r.stop), operator($op), builder.argument($v), builder.argument($r.v)); }) )*
    ;

unary_expression returns [ASTNode v]
    : op=(PLUS | MINUS | NOT) n_ l=unary_expression { $v = builder.call(src($op, $l.stop), operator($op), builder.argument($l.v)); }
    | b=power_expr                                  { $v = $b.v; }
    ;

power_expr returns [ASTNode v]
    : l=basic_expr { $v = $l.v; }
      (
        ((power_operator)=>op=power_operator n_ r=unary_expression { $v = builder.call(src($l.start, $r.stop), operator($op.v), builder.argument($v), builder.argument($r.v)); } )
      |
      )
    ;

basic_expr returns [ASTNode v]
    :
    (
      // special case for simple function call to generate "function" mode lookups
      ((ID|DD|VARIADIC|STRING) LPAR) => (lhsToken=(ID | DD | VARIADIC | STRING) op=LPAR a=args[null] y=RPAR
                      { $v = builder.call(src($start, $y), operator($lhsToken), $a.v); } )
    |
      lhs=simple_expr { $v = $lhs.v; }
    )
    (
      ((FIELD|AT|LBRAKET|LBB|LPAR) => (
          (op=(FIELD|AT) n_ name=id                    { $v = builder.call(src($start, $name.stop), operator($op), builder.argument($v), builder.argument(builder.constant(src($name.v), $name.v.getText()))); })
        | (op=(FIELD|AT) n_ sname=conststring          { $v = builder.call(src($start, $sname.stop), operator($op), builder.argument($v), builder.argument($sname.v)); })
        | (op=LBRAKET subset=args[$v] y=RBRAKET        {
                                                           if ($subset.v.size() == 1) {
                                                               $subset.v.add(builder.argumentEmpty());
                                                           }
                                                           $v = builder.call(src($start, $y), operator($op), $subset.v); 
                                                       })
        // must use RBRAKET twice instead of RBB because this is possible: a[b[1]]
        | (op=LBB subscript=args[$v] RBRAKET y=RBRAKET {
                                                           if ($subscript.v.size() == 1) {
                                                               $subscript.v.add(builder.argumentEmpty());
                                                           }
                                                           $v = builder.call(src($start, $y), operator($op), $subscript.v);
                                                       })
        | (op=LPAR a=args[null] y=RPAR                 { $v = builder.call(src($start, $y), $v, $a.v); })
        )
      )+
    | (n_)=>
    )
    ;

simple_expr returns [ASTNode v]
    : i=id                                      { $v = builder.lookup(src($i.v), $i.text, false); }
    | b=bool                                    { $v = builder.constant(src($b.start, $b.stop), $b.v); }
    | d=DD                                      { $v = builder.lookup(src($d), $d.text, false); }
    | t=NULL                                    { $v = builder.constant(src($t), RNull.instance); }
    | t=INF                                     { $v = builder.constant(src($t), Double.POSITIVE_INFINITY); }
    | t=NAN                                     { $v = builder.constant(src($t), Double.NaN); }
    | t=NAINT                                   { $v = builder.constant(src($t), RRuntime.INT_NA); }
    | t=NAREAL                                  { $v = builder.constant(src($t), RRuntime.DOUBLE_NA); }
    | t=NACHAR                                  { $v = builder.constant(src($t), RRuntime.STRING_NA); }
    | t=NACOMPL                                 { $v = builder.constant(src($t), RComplex.NA); }
    | num=number                                { $v = $num.v; }
    | cstr=conststring                          { $v = $cstr.v; }
    | pkg=id op=(NS_GET|NS_GET_INT) n_ comp=id {
        ArgNode[] args = new ArgNode[2];
        SourceSection pkgSource = src($pkg.v);
        SourceSection compSource = src($comp.v);
        args[0] = builder.argument(pkgSource, "pkg", builder.lookup(pkgSource, $pkg.text, false));
        args[1] = builder.argument(compSource, "name", builder.lookup(compSource, $comp.text, false));
        $v = builder.call(src($pkg.v, $comp.v), operator($op), args);
    }
    | op=LPAR n_ ea=expr_or_assign n_ y=RPAR    { $v = builder.call(src($op, $y), operator($op), builder.argument($ea.v)); }
    | s=sequence                                { $v = $s.v; }
    | e=expr_wo_assign                          { $v = $e.v; }
    ;

number returns [ASTNode v]
    : i=INTEGER {
        double value = RRuntime.string2doubleNoCheck($i.text);
        if (value == (int) value) {
            if ($i.text.indexOf('.') != -1) {
                builder.warning(RError.Message.INTEGER_VALUE_UNNECESARY_DECIMAL, $i.text + "L");
            }
            $v = builder.constant(src($i), (int) value);
        } else {
            if ($i.text.indexOf('.') != -1) {
                builder.warning(RError.Message.INTEGER_VALUE_DECIAML, $i.text + "L");
            } else {
                builder.warning(RError.Message.NON_INTEGER_VALUE, $i.text + "L");
            }
            $v = builder.constant(src($i), value);
        }
      }
    | d=DOUBLE  { $v = builder.constant(src($d), RRuntime.string2doubleNoCheck($d.text)); }
    | c=COMPLEX { $v = builder.constant(src($c), RComplex.valueOf(0, RRuntime.string2doubleNoCheck($c.text))); }
    ;

conststring returns [ASTNode v]
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

args [ASTNode firstArg] returns [List<ArgNode> v]
    @init {
              $v = new ArrayList<>();
              if (firstArg != null) {
                  $v.add(builder.argument(firstArg));
              }
          }
    : n_ (arg_expr[v] n_ (COMMA ({ $v.add(builder.argumentEmpty()); } | n_ arg_expr[v]) n_)* )?
    | n_ { $v.add(builder.argumentEmpty()); } (COMMA ({ $v.add(builder.argumentEmpty()); } | n_ arg_expr[v]) n_)+
    ;

arg_expr [List<ArgNode> l]
    : e=expr                                                   { $l.add(builder.argument(src($e.start, $e.stop), (String) null, $e.v)); }
    | name=(ID | VARIADIC | NULL | STRING) n_ ASSIGN n_ e=expr { $l.add(builder.argument(src($name, $e.stop), $name.text, $e.v)); }
    | name=(ID | VARIADIC | NULL | STRING) n_ a=ASSIGN         { $l.add(builder.argument(src($name, $a), $name.text, null)); }
    ;

///
/// Lexer
///

COMMENT : '#' ~('\n'|'\r'|'\f')* (LINE_BREAK | EOF) ;

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

LBRACE  : '{' ;
RBRACE  : '}' ;
LPAR    : '(' ;
RPAR    : ')' ;
LBB     : '[[' ;
LBRAKET : '[' ;
RBRAKET : ']' ;

CARET : '^' | '**' ;
TILDE : '~' ;
NOT   : '!' ;
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
NEWLINE : LINE_BREAK ;

INTEGER
    : ('0'..'9')+ '.' ('0'..'9')* 'L' { setText(getText().substring(0, getText().length()-1)); }
    | '.'? ('0'..'9')+ EXPONENT? 'L' { setText(getText().substring(0, getText().length()-1)); }
    | '0x' HEX_DIGIT+ 'L' { setText(getText().substring(0, getText().length()-1)); }
    ;

COMPLEX
    : ('0'..'9')+ '.' ('0'..'9')* EXPONENT? 'i' { setText(getText().substring(0, getText().length()-1)); }
    | '.'? ('0'..'9')+ EXPONENT? 'i' { setText(getText().substring(0, getText().length()-1)); }
    | '0x' HEX_DIGIT 'i' { setText(getText().substring(0, getText().length()-1)); }
    ;

DOUBLE
    : ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    | '.'? ('0'..'9')+ EXPONENT?
    | '0x' HEX_DIGIT+
    ;

DD : '..' ('0'..'9')+ ;

ID
    : '.'* ID_NAME
    | '.'
    | '.' '.'
    | '`' BACKTICK_NAME
    ;

OP : '%' OP_NAME+ '%' ;

fragment BACKTICK_NAME
    @init { final StringBuilder buf = new StringBuilder(); }
    : (
        (
          ESCAPE[buf]
        | i = ~( '\\' | '`' ) { buf.appendCodePoint(i); }
        )*
        '`'
        { setText(buf.toString()); }
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
      | a = OCT_DIGIT b = OCT_DIGIT c = OCT_DIGIT { buf.append(ParseUtil.octChar($a.text, $b.text, $c.text)); }
      | a = OCT_DIGIT b = OCT_DIGIT { buf.append(ParseUtil.octChar($a.text, $b.text)); }
      | a = OCT_DIGIT { buf.append(ParseUtil.octChar($a.text)); }
      | 'x' a = HEX_DIGIT b = HEX_DIGIT { buf.append(ParseUtil.hexChar($a.text, $b.text)); }
      | 'u' a = HEX_DIGIT b = HEX_DIGIT c = HEX_DIGIT d = HEX_DIGIT { buf.append(ParseUtil.hexChar($a.text, $b.text, $c.text, $d.text)); }
      | 'U' a = HEX_DIGIT b = HEX_DIGIT c = HEX_DIGIT d = HEX_DIGIT e = HEX_DIGIT f = HEX_DIGIT g = HEX_DIGIT h = HEX_DIGIT { buf.append(ParseUtil.hexChar($a.text, $b.text, $c.text, $d.text, $e.text, $f.text, $g.text, $h.text)); }
      )
    ;

fragment LINE_BREAK
    : (('\f'|'\r')? '\n')
    | ('\n'? ('\r'|'\f')) // This rule fixes very old Mac/Dos/Windows encoded files
    ;

fragment EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment OP_NAME
    : ID_NAME
    | ('*'|'/'|'+'|'-'|'>'|'<'|'='|'|'|'&'|':'|'^'|'.'|'~'|','|'?')
    ;

fragment ID_NAME : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'.')* ;

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
