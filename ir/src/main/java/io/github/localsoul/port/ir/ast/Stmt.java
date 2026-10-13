package io.github.localsoul.port.ir.ast;

/** Statement (Spec Abschnitt 6). */
public sealed interface Stmt extends IrNode
    permits RawNode,
        ManualNode,
        BlockStmt,
        LocalStmt,
        ExprStmt,
        IfStmt,
        WhileStmt,
        DoWhileStmt,
        ForStmt,
        ForeachStmt,
        SwitchStmt,
        BreakStmt,
        ContinueStmt,
        ReturnStmt,
        ThrowStmt,
        TryStmt,
        YieldReturnStmt,
        YieldBreakStmt,
        LockStmt,
        UsingStmt,
        LabelStmt,
        GotoStmt {}
