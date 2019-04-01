package analyzer.visitors;

import analyzer.ast.*;
import java.io.PrintWriter;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;

    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        // Visiter les enfants
        node.childrenAccept(this, null);

        m_writer.close();
        return null;
    }


    @Override
    public Object visit(ASTNumberRegister node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }


    @Override
    public Object visit(ASTLive node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTLiveNode node, Object data) {
        // TODO: Vous voulez probablement sauvegarder les variables vives lives...

        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTInNode node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTOutNode node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);

        // TODO: Chaque variable a son emplacement en mémoire, mais si elle est déjà dans un registre, ne la rechargez pas!

        // TODO: Si une variable n'est pas vive, ne l'enregistrez pas en mémoire.

        // TODO: Si vos registres sont pleins, déterminez quelle variable que vous allez retirer et si vous devez la sauvegarder

        // TODO: Écrivez la traduction en code machine, une instruction intermédiaire peut générer plus qu'une instruction machine

        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // TODO: Chaque variable a son emplacement en mémoire, mais si elle est déjà dans un registre, ne la rechargez pas!

        // TODO: Si une variable n'est pas vive, ne l'enregistrez pas en mémoire.

        // TODO: Si vos registres sont pleins, déterminez quelle variable que vous allez retirer et si vous devez la sauvegarder

        // TODO: Écrivez la traduction en code machine, une instruction intermédiaire peut générer plus qu'une instruction machine

        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // TODO: Chaque variable a son emplacement en mémoire, mais si elle est déjà dans un registre, ne la rechargez pas!

        // TODO: Si une variable n'est pas vive, ne l'enregistrez pas en mémoire.

        // TODO: Si vos registres sont pleins, déterminez quelle variable que vous allez retirer et si vous devez la sauvegarder

        // TODO: Écrivez la traduction en code machine, une instruction intermédiaire peut générer plus qu'une instruction machine

        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return String.valueOf(node.getValue());
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        return node.getValue();
    }
}
