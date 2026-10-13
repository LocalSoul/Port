package io.github.localsoul.port.ir.ast;

/** Mitglied einer Typdeklaration (Spec Abschnitt 5). */
public sealed interface MemberDecl extends IrNode
    permits FieldDecl, PropDecl, MethodDecl, CtorDecl, EventDecl {}
