/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
grammar R;

options {
    language = Java;
    memoize = true;
}

@header {
//Checkstyle: stop
//@formatter:off
package com.oracle.truffle.r.parser;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.impl.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.parser.ast.Call.*;
import com.oracle.truffle.r.parser.ast.UnaryOperation.*;
import com.oracle.truffle.r.parser.ast.BinaryOperation.*;
}

@lexer::header {
//Checkstyle: stop
//@formatter:off
package com.oracle.truffle.r.parser;
}

@rulecatch {
    catch(RecognitionException re){
        throw re; // Stop at first error
    }
}

@members {
    Source source;

    public void setSource(Source s) {
        source = s;
    }

    private final boolean DUMP_SRC = false;

    /**
     * Create a {@link SourceSection} from a start and end token.
     */
    private SourceSection sourceSection(String id, Token start, Token stop) {
        CommonToken cstart = (CommonToken) start;
        CommonToken cstop = (CommonToken) stop;
        int startIndex = cstart.getStartIndex();
        int stopIndex = cstop.getStopIndex();
        int length = stopIndex - startIndex + (stop.getType() == Token.EOF ? 0 : 1);
        if (DUMP_SRC) {
            System.out.print("<<" + id + "," + cstart.getLine() + "," + (cstart.getCharPositionInLine() + 1) + "," + startIndex + "," + length);
        }
        SourceSection src = new DefaultSourceSection(source, id, cstart.getLine(), cstart.getCharPositionInLine() + 1, startIndex, length);
        if (DUMP_SRC) {
            System.out.println("=>" + src.getCode() + ">>");
        }
        return src;
    }

    /**
     * Create a {@link SourceSection} from a single token, e.g., one returned from the lexer.
     */
    private SourceSection sourceSection(String id, Token tok) {
        return sourceSection(id, tok, tok);
    }

    /**
     * Create a {@link SourceSection} from two {@link ASTNode}s, spanning their source.
     */
    private SourceSection sourceSection(String id, ASTNode a, ASTNode b) {
        SourceSection as = a.getSource();
        int ai = as.getCharIndex();
        return new DefaultSourceSection(source, id, as.getStartLine(), as.getStartColumn(), ai, b.getSource().getCharEndIndex() - ai);
    }
    
    /**
     * Create a {@link SourceSection} from a token and an {@link ASTNode}, spanning their source.
     */
    private SourceSection sourceSection(String id, Token a, ASTNode b) {
        CommonToken ta = (CommonToken) a;
        int startIndex = ta.getStartIndex();
        int length = b.getSource().getCharEndIndex() - startIndex;
        return new DefaultSourceSection(source, id, ta.getLine(), ta.getCharPositionInLine() + 1, startIndex, length);
    }

    public void display_next_tokens(){
        System.err.print("Expected tokens: ");
        for(int next: next_tokens()) {
            if(next > 3)
                System.err.print(tokenNames[next] + " ");
            }
        System.err.println("");
    }

    public int[] next_tokens(){
        return state.following[state._fsp].toArray();
    }
}

@lexer::members {
    public final int MAX_INCOMPLETE_SIZE = 1000;

    int incomplete_stack[] = new int[MAX_INCOMPLETE_SIZE]; // TODO probably go for an ArrayList of int :S

    int incomplete_depth;

    public void resetIncomplete() {
        incomplete_stack[incomplete_depth = 0] = 0;
    }

    @Override
    public void reportError(RecognitionException e) {
        throw new IllegalArgumentException(e);
    }
}

@lexer::init{
    incomplete_depth = 0;
    incomplete_stack[incomplete_depth] = 0;
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

script returns [ASTNode v]
    @init  { ArrayList<ASTNode> stmts = new ArrayList<ASTNode>(); }
    @after { $v = Sequence.create(sourceSection("script", $start, $stop), stmts); }
    : n_ (s=statement { stmts.add(s); })*
    ;

interactive returns [ASTNode v]
    @init  { ArrayList<ASTNode> stmts = new ArrayList<ASTNode>(); }
    @after {
        switch(stmts.size()) {
        case 0:
            $v = null;
            break;
        case 1:
            $v = stmts.get(0);
            break;
        default:
            $v = Sequence.create(sourceSection("interactive", $start, $stop), stmts);
        }
    }
    : n_ (s=statement { stmts.add(s); })*
    ;

statement returns [ASTNode v]
    : e=expr_or_assign n { $v = $e.v; }
    ;

n_ : (NEWLINE | COMMENT)*;
n  : (NEWLINE | COMMENT)+ | EOF | SEMICOLON n_;

expr_or_assign returns [ASTNode v]
    : a=alter_assign { $v = $a.v; }
    ;

expr returns [ASTNode v]
    : a=assign { $v = $a.v; }
    ;

expr_wo_assign returns [ASTNode v]
    : w=while_expr                          { $v = $w.v; }
    | i=if_expr                             { $v = $i.v; }
    | f=for_expr                            { $v = $f.v; }
    | r=repeat_expr                         { $v = $r.v; }
    | fun=function                          { $v = $fun.v; }
    | t=NEXT  /* ((LPAR)=>LPAR n_ RPAR)? */ { $v = Next.create(sourceSection("expr_wo_assign/NEXT", t)); }
    | t=BREAK /* ((LPAR)=>LPAR n_ RPAR)? */ { $v = Break.create(sourceSection("expr_wo_assign/BREAK", t)); }
    ;

sequence returns [ASTNode v]
    @init  { ArrayList<ASTNode> stmts = new ArrayList<ASTNode>(); }
    @after { $v = Sequence.create(sourceSection("sequence", $start, $stop), stmts); }
    : LBRACE n_ (e=expr_or_assign { stmts.add($e.v); } (n e=expr_or_assign { stmts.add($e.v); })* n?)? RBRACE
    ;

assign returns [ASTNode v]
    @init { ASTNode rr = null; }
    @after {
        if (rr != null) {
            // assign source to span l..r
            $v.setSource(sourceSection("assign", $l.v, rr));
        }
    }
    : l=tilde_expr
      ( ARROW n_ r=expr             { rr = $r.v; $v = AssignVariable.create(null, false, $l.v, $r.v); }
      | SUPER_ARROW n_ r=expr       { rr = $r.v; $v = AssignVariable.create(null, true, $l.v, $r.v); }
      | RIGHT_ARROW n_ r=expr       { rr = $r.v; $v = AssignVariable.create(null, false, $r.v, $l.v); }
      | SUPER_RIGHT_ARROW n_ r=expr { rr = $r.v; $v = AssignVariable.create(null, true, $r.v, $l.v); }
      | { $v = $l.v; }
      )
    ;

alter_assign returns [ASTNode v]
    @init { ASTNode rr = null; }
    @after {
        if (rr != null) {
            // assign source to span l..r
            $v.setSource(sourceSection("alter_assign", $l.v, rr));
        }
    }
    : l=tilde_expr
      ( (ARROW)=>ARROW n_ r=expr_or_assign                         { rr = $r.v; $v = AssignVariable.create(null, false, $l.v, $r.v); }
      | (SUPER_ARROW)=>SUPER_ARROW n_ r=expr_or_assign             { rr = $r.v; $v = AssignVariable.create(null, true, $l.v, $r.v); }
      | (RIGHT_ARROW)=>RIGHT_ARROW n_ r=expr_or_assign             { rr = $r.v; $v = AssignVariable.create(null, false, $r.v, $l.v); }
      | (SUPER_RIGHT_ARROW)=>SUPER_RIGHT_ARROW n_ r=expr_or_assign { rr = $r.v; $v = AssignVariable.create(null, true, $r.v, $l.v); }
      | (ASSIGN)=>ASSIGN n_ r=expr_or_assign                       { rr = $r.v; $v = AssignVariable.create(null, false, $l.v, $r.v); }
      | { $v = $l.v; }
      )
    ;

if_expr returns [ASTNode v]
    @after { $v.setSource(sourceSection("if", $start, $stop)); }
    : IF n_ LPAR n_ cond=expr_or_assign n_ RPAR n_ t=expr_or_assign
      (
        (n_ ELSE)=>(options { greedy=false; backtrack = true; }: n_ ELSE n_ f=expr_or_assign { $v = If.create(null, $cond.v, $t.v, $f.v); })
      | { $v = If.create(null, $cond.v, $t.v); }
      )
    ;

while_expr returns [ASTNode v]
    @init { boolean blockBody = false; }
    @after {
        SourceSection src;
        if (blockBody) {
            src = sourceSection("while_expr/BLOCK", $start, $body.start);
        } else {
            src = sourceSection("while_expr/EXPR", $start, $stop);
        }
        $v = Loop.create(src, $c.v, $body.v);
    }
    : WHILE n_ LPAR n_ c=expr_or_assign n_ RPAR n_
      ( (LBRACE)=>body=expr_or_assign { blockBody = true; }
      |           body=expr_or_assign
      )
    ;

for_expr returns [ASTNode v]
    @after { $v = Loop.create(sourceSection("for_expr", $start, $stop), $i.text, $in.v, $body.v); }
    : FOR n_ LPAR n_ i=ID n_ IN n_ in=expr_or_assign n_ RPAR n_ body=expr_or_assign
    ;

repeat_expr returns [ASTNode v]
    @after { $v = Loop.create(sourceSection("repeat_expr", $start, $stop), $body.v); }
    : REPEAT n_ body=expr_or_assign
    ;

function returns [ASTNode v]
    @init {
        List<ArgNode> l = new ArrayList<>();
        ASTNode vv = null;
    }
    @after {
        SourceSection srcs = sourceSection("function", $start, $stop);
        vv = Function.create(l, $body.v, srcs);
        $v = vv;
    }
    : FUNCTION n_ LPAR  n_ (par_decl[l] (n_ COMMA n_ par_decl[l])* n_)? RPAR n_ body=expr_or_assign
    ;

par_decl [List<ArgNode> l]
    : i=ID                     { $l.add(ArgNode.create(null, $i.text, null)); }
    | i=ID n_ ASSIGN n_ e=expr { $l.add(ArgNode.create(null, $i.text, e)); }
    | v=VARIADIC               { $l.add(ArgNode.create(null, $v.text, null)); } // FIXME This is not quite good, since `...` is a special token - for this reason let's call RSymbol.xxxx(...)
    // The 3 following cases were not handled ... and everything was working fine.
    // They are added for completeness, however note that a function created
    // with such a signature will always fail if it tries to access them!
    | VARIADIC n_ ASSIGN n_ expr
    | DD
    | DD n_ ASSIGN n_ expr
    ;

tilde_expr returns [ASTNode v]
    @init { boolean hasTilde = false; }
    @after {
        if (hasTilde) {
            // In other cases, source info is in l.
            $v.setSource(sourceSection("tilde_expr", $start, $stop));
        }
    }
    : l=utilde_expr { $v = $l.v; }
      ( ((TILDE) => t=TILDE n_ r=utilde_expr { hasTilde = true; $v = BinaryOperation.create(sourceSection("tilde_expr/binop", $t, $r.stop), BinaryOperator.ADD, $tilde_expr.v, $r.v); }) )*
    ;

utilde_expr returns [ASTNode v]
    : t=TILDE n_ l=or_expr { $v = UnaryOperation.create(sourceSection("utilde_expr", $t, $l.stop), UnaryOperator.MODEL, $l.v); }
    | l=or_expr            { $v = $l.v; }
    ;

or_expr returns [ASTNode v]
    @init { boolean hasOr = false; }
    @after {
        if (hasOr) {
            // In other cases, source info is in l.
            $v.setSource(sourceSection("or_expr", $start, $stop));
        }
    }
    : l=and_expr { $v = $l.v; }
      ( ((or_operator)=>op=or_operator n_ r=and_expr { hasOr = true; $v = BinaryOperation.create(sourceSection("or_expr/binop", $op.start, $r.stop), $op.v, $or_expr.v, $r.v); }) )*
    ;

and_expr returns [ASTNode v]
    @init { boolean hasAnd = false; }
    @after {
        if (hasAnd) {
            // In other cases, source info is in l.
            $v.setSource(sourceSection("and_expr", $start, $stop));
        }
    }
    : l=not_expr { $v = $l.v; }
      ( ((and_operator)=>op=and_operator n_ r=not_expr { hasAnd = true; $v = BinaryOperation.create(sourceSection("and_expr/binop", $op.start, $r.stop), $op.v, $and_expr.v, $r.v); }) )*
    ;

not_expr returns [ASTNode v]
    : t=NOT n_ l=not_expr { $v = UnaryOperation.create(sourceSection("not_expr", $t, $l.stop), UnaryOperator.NOT, $l.v); }
    | b=comp_expr         { $v = $b.v; }
    ;

comp_expr returns [ASTNode v]
    @init { boolean hasComp = false; }
    @after {
        if (hasComp) {
            // In other cases, source info is in l.
            $v.setSource(sourceSection("comp_expr", $start, $stop));
        }
    }
    : l=add_expr { $v = $l.v; }
      ( ((comp_operator)=>op=comp_operator n_ r=add_expr { hasComp = true; $v = BinaryOperation.create(sourceSection("comp_expr/binop", $op.start, $r.stop), $op.v, $comp_expr.v, $r.v); }) )*
    ;

add_expr returns [ASTNode v]
    @init { boolean hasAdd = false; }
    @after {
        if (hasAdd) {
            // In other cases, source info is in l.
            $v.setSource(sourceSection("add_expr", $start, $stop));
        }
    }
    : l=mult_expr { $v = $l.v; }
      ( ((add_operator)=>op=add_operator n_ r=mult_expr { hasAdd = true; $v = BinaryOperation.create(sourceSection("add_expr/binop", $op.start, $r.stop), $op.v, $add_expr.v, $r.v); }) )*
    ;

mult_expr returns [ASTNode v]
    @init { boolean hasMult = false; }
    @after {
        if (hasMult) {
            // In other cases, source info is in l.
            $v.setSource(sourceSection("mult_expr", $start, $stop));
        }
    }
    : l=operator_expr { $v = $l.v; }
      ( ((mult_operator)=>op=mult_operator n_ r=operator_expr { hasMult = true; $v = BinaryOperation.create(sourceSection("mult_expr/binop", $op.start, $r.stop), $op.v, $mult_expr.v, $r.v); }) )*
    ;

operator_expr returns [ASTNode v]
    @init { boolean hasOp = false; }
    @after {
        if (hasOp) {
            // In other cases, source information is in l.
            $v.setSource(sourceSection("operator_expr", $start, $stop));
        }
    }
    : l=colon_expr { $v = $l.v; }
      ( (OP)=>opc=OP n_ r=colon_expr { hasOp = true; $v = BinaryOperation.create(sourceSection("operator_expr/binop", $opc, $r.stop), $opc.text, $operator_expr.v, $r.v); } )*
    ;

colon_expr returns [ASTNode v] // FIXME
    @init { boolean hasColon = false; }
    @after {
        if (hasColon) {
            // In other cases, source information is in l.
            $v.setSource(sourceSection("colon_expr", $start, $stop));
        }
    }
    : l=unary_expression { $v = $l.v; }
      ( ((COLON)=>op=COLON n_ r=unary_expression { hasColon = true; $v = BinaryOperation.create(sourceSection("colon_expr/binop", $op, $r.stop), BinaryOperator.COLON, $colon_expr.v, $r.v); }) )*
    ;

unary_expression returns [ASTNode v]
    @init { boolean plusOrMinus = false; }
    @after {
        if (plusOrMinus) {
            // In other cases, source information is in b.
            $v.setSource(sourceSection("unary_expression", $start, $stop));
        }
    }
    : p=PLUS n_ { plusOrMinus = true; }
      ( (number) => num=number { $v = num; }
      | l=unary_expression     { $v = UnaryOperation.create(sourceSection("unary_expression/PLUS", $p, $l.stop), UnaryOperator.PLUS, $l.v); }
      )
    | m=MINUS n_ { plusOrMinus = true; }
      ( (number) => num=number { ((Constant) num).addNegativeSign(); $v = num; }
      | l=unary_expression     { $v = UnaryOperation.create(sourceSection("unary_expression/MINUS", $m, $l.stop), UnaryOperator.MINUS, $l.v); }
      )
    | b=power_expr             { $v = $b.v; }
    ;

power_expr returns [ASTNode v]
    @init { boolean hasPowerOp = false; }
    @after {
        if (hasPowerOp) {
            // In other cases, v already has the source information from l.
            $v.setSource(sourceSection("power_expr", $start, $stop));
        }
    }
    : l=basic_expr { $v = $l.v; }
      (
        ((power_operator)=>op=power_operator n_ r=power_expr { hasPowerOp = true; $v = BinaryOperation.create(sourceSection("power_expr/pow", $op.start, $r.stop), $op.v, $l.v, $r.v); } )
      |
      )
    ;

basic_expr returns [ASTNode v]
    @init  {
        // NOTE: Using vv is used to work around a bug in ANTLR that occurs when
        // solely working with $v - ANTLR is unhappy about passing it to expr_subset.
        ASTNode vv = null;
        boolean hasSubset = false;
    }
    @after {
        if (hasSubset) {
            // In other cases, vv already has the source from lhs.
            vv.setSource(sourceSection("basic_expr/WITH_SUBSET", $start, $stop));
        }
        $v = vv;
    }
    : lhs=simple_expr { vv = lhs; }
      (
        ((FIELD|AT|LBRAKET|LBB|LPAR)=>subset=expr_subset[vv] { vv = subset; hasSubset = true; })+
      | (n_)=>
      )
    ;

expr_subset [ASTNode i] returns [ASTNode v]
    : (t=FIELD n_ name=id                     { $v = FieldAccess.create(sourceSection("expr_subset/FIELD", $t, name), FieldOperator.FIELD, i, name.getText()); })
    | (t=AT n_ name=id                        { $v = FieldAccess.create(sourceSection("expr_subset/AT", $t, name), FieldOperator.AT, i, name.getText()); })
    | (t=LBRAKET subset=args y=RBRAKET        { $v = Call.create(sourceSection("expr_subset/LBRAKET", $t, $y), CallOperator.SUBSET, i, subset); })
    | (t=LBB subscript=args RBRAKET y=RBRAKET { $v = Call.create(sourceSection("expr_subset/LBB", $t, $y), CallOperator.SUBSCRIPT, i, subscript); })
    // Must use RBRAKET twice instead of RBB beacause this is possible: a[b[1]]
    | (t=LPAR a=args y=RPAR                   { $v = Call.create(sourceSection("expr_subset/LPAR", $t, $y), i, a); })
    //| { $v = i; }
    ;

simple_expr returns [ASTNode v]
    : i=id                                      { $v = AccessVariable.create(sourceSection("simple_expr/id", i), i.getText(), false); }
    | b=bool                                    { $v = b; }
    | d=DD                                      { $v = AccessVariable.create(sourceSection("simple_expr/DD", d), d.getText(), false); }
    | t=NULL                                    { $v = Constant.getNull(sourceSection("simple_expr/NULL", t)); }
    | t=INF                                     { $v = Constant.createDoubleConstant(sourceSection("simple_expr/INF", t), "Inf"); }
    | t=NAN                                     { $v = Constant.createDoubleConstant(sourceSection("simple_expr/NAN", t), "NaN"); }
    | t=NAINT                                   { $v = Constant.createIntConstant(sourceSection("simple_expr/NAINT", t), "NA_integer_"); }
    | num=number                                { $v = num; }
    | cstr=conststring                          { $v = cstr; }
    | pkg=id nsg=(NS_GET|NS_GET_INT) n_ comp=id {
        List<ArgNode> args = new ArrayList<>();
        ASTNode pkgNode = Constant.createStringConstant(sourceSection("simple_expr/NSG/pkg", pkg), pkg.getText());
        ASTNode compNode = Constant.createStringConstant(sourceSection("simple_expr/NSG/comp", comp), comp.getText());
        args.add(ArgNode.create(pkgNode.getSource(), "pkg", pkgNode));
        args.add(ArgNode.create(compNode.getSource(), "name", compNode));
        $v = Call.create(sourceSection("simple_expr/NSG", pkg, comp), Symbol.getSymbol(nsg.getText()), args);
    }
    | LPAR n_ ea=expr_or_assign n_ RPAR         { $v = $ea.v; $v.setSource(sourceSection("simple_expr/OMIT_PAR", $ea.start, $ea.stop)); }
    | s=sequence                                { $v = $s.v; }
    | e=expr_wo_assign                          { $v = e; }
    ;

number returns [ASTNode n]
    : i=INTEGER { $n = Constant.createIntConstant(sourceSection("number/INTEGER", i), $i.text); }
    | d=DOUBLE  { $n = Constant.createDoubleConstant(sourceSection("number/DOUBLE", d), $d.text); }
    | c=COMPLEX { $n = Constant.createComplexConstant(sourceSection("number/COMPLEX", c), $c.text); }
    ;

conststring returns [ASTNode n]
    : s=STRING { $n = Constant.createStringConstant(sourceSection("conststring", s), $s.text); }
    ;

id returns [Token t]
    : i=ID       { $t = $i; }
    | v=VARIADIC { $t = $v; }
    ;

bool returns [ASTNode v]
    : t=TRUE  { $v = Constant.createBoolConstant(sourceSection("bool/TRUE", t), RRuntime.LOGICAL_TRUE); }
    | t=FALSE { $v = Constant.createBoolConstant(sourceSection("bool/FALSE", t), RRuntime.LOGICAL_FALSE); }
    | t=NA    { $v = Constant.createBoolConstant(sourceSection("bool/NA", t), RRuntime.LOGICAL_NA); }
    ;

or_operator returns [BinaryOperator v]
    : OR            { $v = BinaryOperator.OR; }
    | ELEMENTWISEOR { $v = BinaryOperator.ELEMENTWISEOR; }
    ;

and_operator returns [BinaryOperator v]
    : AND            { $v = BinaryOperator.AND; }
    | ELEMENTWISEAND { $v = BinaryOperator.ELEMENTWISEAND; }
    ;

comp_operator returns [BinaryOperator v]
    : GT { $v = BinaryOperator.GT; }
    | GE { $v = BinaryOperator.GE; }
    | LT { $v = BinaryOperator.LT; }
    | LE { $v = BinaryOperator.LE; }
    | EQ { $v = BinaryOperator.EQ; }
    | NE { $v = BinaryOperator.NE; }
    ;

add_operator returns [BinaryOperator v]
    : PLUS  { $v = BinaryOperator.ADD; }
    | MINUS { $v = BinaryOperator.SUB; }
    ;

mult_operator returns [BinaryOperator v]
    : MULT { $v = BinaryOperator.MULT; }
    | DIV  { $v = BinaryOperator.DIV; }
    | MOD  { $v = BinaryOperator.MOD; }
    ;

power_operator returns [BinaryOperator v]
    : CARET { $v = BinaryOperator.POW; }
    ;

args returns [List<ArgNode> v]
    @init { $v = new ArrayList<>(); }
    : n_ (arg_expr[v] n_ (COMMA ({ $v.add(ArgNode.create(null, (Symbol) null, (ASTNode) null)); } | n_ arg_expr[v]) n_)* )?
    | n_ { $v.add(ArgNode.create(null, (Symbol) null, (ASTNode) null)); } (COMMA ({ $v.add(ArgNode.create(null, (Symbol) null, (ASTNode) null)); } | n_ arg_expr[v]) n_)+
    ;

arg_expr [List<ArgNode> l]
    : e=expr                                   { $l.add(ArgNode.create(sourceSection("arg_expr/expr", e, e), (Symbol) null, e)); }
    | name=(id | STRING) n_ ASSIGN n_ val=expr { $l.add(ArgNode.create(sourceSection("arg_expr/name=expr", name, val), name.getText(), val)); }
    | name=(id | STRING) n_ a=ASSIGN           { $l.add(ArgNode.create(sourceSection("arg_expr/name=", name, a), name.getText(), null)); }
    | NULL n_ ASSIGN n_ val=expr               { Utils.nyi(); }
    | NULL n_ ASSIGN                           { Utils.nyi(); }
    ;

///
/// Lexer
///

COMMENT : '#' ~('\n'|'\r'|'\f')* (LINE_BREAK | EOF) { if(incomplete_stack[incomplete_depth] > 0) $channel=HIDDEN; } ;

ARROW             : '<-' | ':=' ;
SUPER_ARROW       : '<<-' ;
RIGHT_ARROW       : '->' ;
SUPER_RIGHT_ARROW : '->>' ;
VARIADIC          : '..' '.'+ ; // FIXME

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

LBRACE  : '{'  { incomplete_stack[++incomplete_depth] = 0; } ; // TODO grow the stack
RBRACE  : '}'  { incomplete_depth--; } ;
LPAR    : '('  { incomplete_stack[incomplete_depth]++; } ;
RPAR    : ')'  { incomplete_stack[incomplete_depth]--; } ;
LBB     : '[[' { incomplete_stack[incomplete_depth] += 2; } ; // Must increase by two because of ']'']' used for closing
LBRAKET : '['  { incomplete_stack[incomplete_depth]++; } ;
RBRAKET : ']'  { incomplete_stack[incomplete_depth]--; } ;

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
///NAREAL   : 'NA_real_' ;
///NACHAR   : 'NA_character_' ;
///NACOMPL  : 'NA_complex_' ;
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
NEWLINE : LINE_BREAK { if(incomplete_stack[incomplete_depth] > 0) $channel=HIDDEN; } ;

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
    | '`' ( ESC_SEQ | ~('\\'|'`') )* '`' { setText(getText().substring(1, getText().length()-1)); }
    ;

OP : '%' OP_NAME+ '%' ;

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

/* not supporting \v and \a */
fragment ESCAPE [StringBuilder buf]
    : '\\'
      ( 't' { buf.append('\t'); }
      | 'n' { buf.append('\n'); }
      | 'r' { buf.append('\r'); }
      | 'b' { buf.append('\b'); }
      | 'f' { buf.append('\f'); }
      | '"' { buf.append('\"'); }
      | '\'' { buf.append('\''); }
      | '\\' { buf.append('\\'); }
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
    | ('*'|'/'|'+'|'-'|'>'|'<'|'='|'|'|'&'|':'|'^'|'.'|'~'|',')
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

fragment OCTAL_ESC
    : '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    | '\\' ('0'..'7') ('0'..'7')
    | '\\' ('0'..'7')
    ;
