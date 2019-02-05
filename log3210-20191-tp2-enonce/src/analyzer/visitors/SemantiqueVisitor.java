package analyzer.visitors;

import analyzer.SemantiqueError;
import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.HashMap;


/**
 * Created: 19-01-10
 * Last Changed: 19-02-05
 * Authors: Félix Brunet, Sébastien Chagnon, Pascal Lacasse
 *
 * Description: Ce visiteur explorer l'AST est renvois des erreur lorqu'une erreur sémantique est détecté.
 */


 /******************************************
  * IMPORTANT *
  * ***********
  *
  * Notre analyse sémantique est déscendante.
  * Ainsi, les tests ne passent pas tous car les messages d'erreur ne sont pas les mêmes.
  * Cependant, tout les tests qui doivent passer passent et ceux qui doivent échouer échouent.
  *
  * Fonctionnement:
  * Chaque noeuds parents passe à ses enfants le type qu'ils devraient être.
  * Si un enfant n'est pas du bon type, il lance une erreur.
  */


public class SemantiqueVisitor implements ParserVisitor {

    private final PrintWriter m_writer;

    public HashMap<String, VarType> SymbolTable = new HashMap<>();

    public SemantiqueVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    /*
    //utilisation d'identifiant non défini
    throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());

    //utilisation de nombre dans la condition d'un if ou d'un while
    throw new SemantiqueError("Invalid type in condition");

    //assignation d'une valeur a une variable qui a déjà recu une valeur d'un autre type
    ex : a = 1; a = true;
    throw new SemantiqueError("Invalid type in assignment");

    //expression invalide : (note, le code qui l'utilise est déjà fournis)
    throw new SemantiqueError("Invalid type in expression got " + type.toString() + " was expecting " + expectedType);
     */

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        node.childrenAccept(this, data);
        m_writer.print("all good");
        return null;
    }


    @Override
    public Object visit(ASTDeclaration node, Object data) {
        for (int i = 0 ; i < node.jjtGetNumChildren(); i++) {
            if (SymbolTable.containsKey(((ASTIdentifier) node.jjtGetChild(0)).getValue()))
                throw new SemantiqueError("Invalid type in assignment");

            SymbolTable.put(((ASTIdentifier) node.jjtGetChild(0)).getValue(), node.getValue().equals("num") ? VarType.Number : VarType.Bool);
        }
        return null;
    }


    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTIfStmt node, Object data) {
        try {
            // It's condition must be of type Bool
            node.childrenAccept(this, VarType.Bool);
        } catch (SemantiqueError e) {
            throw new SemantiqueError("Invalid type in condition");
        }
        return null;
    }


    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        try {
            // It's condition must be of type Bool
            node.childrenAccept(this, VarType.Bool);
        } catch (SemantiqueError e) {
            throw new SemantiqueError("Invalid type in condition");
        }
        return null;
    }


    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        if (!SymbolTable.containsKey(((ASTIdentifier)node.jjtGetChild(0)).getValue()))
            throw new SemantiqueError("Invalid use of undefined Identifier " + ((ASTIdentifier)node.jjtGetChild(0)).getValue());

        // It's children must be of the type of the variable
        node.childrenAccept(this, SymbolTable.get(((ASTIdentifier)node.jjtGetChild(0)).getValue()));
        return null;
    }


    @Override
    public Object visit(ASTExpr node, Object data){
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTCompExpr node, Object data) {
        if (node.jjtGetNumChildren() == 1) {
            //PASSTHROUGH to the next children
            node.childrenAccept(this, data);

        } else {
            if (data == VarType.Number)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

            if (node.getValue().equals("==") || node.getValue().equals("!=")) {
                try {
                    node.childrenAccept(this, VarType.Bool);
                    return null;
                } catch (SemantiqueError e) {
                    // PASSTHROUGH  If it's children are not both of type Bool
                    //              they can also be of type Number
                }
            }

            node.childrenAccept(this, VarType.Number);
        }
        return null;
    }


    @Override
    public Object visit(ASTAddExpr node, Object data) {
        if (node.jjtGetNumChildren() > 1)
            if (data == VarType.Bool)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTMulExpr node, Object data) {
        if (node.jjtGetNumChildren() > 1 && data == VarType.Bool)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        if (node.jjtGetNumChildren() > 1 && data == VarType.Number)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTNotExpr node, Object data) {
        if (data == VarType.Number && !node.getOps().isEmpty())
            throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        if (data == VarType.Bool && !node.getOps().isEmpty())
            throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTGenValue node, Object data) {
        if (data == VarType.Bool) {
            if (node.jjtGetChild(0) instanceof ASTIntValue)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

            if (node.jjtGetChild(0) instanceof ASTIdentifier &&
                    SymbolTable.get(((ASTIdentifier)node.jjtGetChild(0)).getValue()) == VarType.Number )
                throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);
        }

        if (data == VarType.Number) {
            if (node.jjtGetChild(0) instanceof ASTBoolValue)
                throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

            if (node.jjtGetChild(0) instanceof ASTIdentifier &&
                    SymbolTable.get(((ASTIdentifier)node.jjtGetChild(0)).getValue()) == VarType.Bool )
                throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);
        }

        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        if (data == VarType.Number)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Bool + " was expecting " + data);

        return null;
    }


    @Override
    public Object visit(ASTIdentifier node, Object data) {
        if (node.jjtGetParent() instanceof ASTGenValue && !SymbolTable.containsKey(node.getValue()))
            throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());

        return null;
    }


    @Override
    public Object visit(ASTIntValue node, Object data) {
        if (data == VarType.Bool)
            throw new SemantiqueError("Invalid type in expression got " + VarType.Number + " was expecting " + data);

        return null;
    }


    public enum VarType {
        Bool,
        Number
    }
}
