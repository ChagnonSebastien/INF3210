package analyzer.visitors;

import analyzer.ast.*;
import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;
    private List<Vector<String>> aliveInfo = null;
    private List<List<String>> registery = null;
    private Map<String, List<String>> memory = null;
    private int regesterySize = 0;

    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;
        aliveInfo = new ArrayList<>();
        registery = new ArrayList<>();
        memory = new HashMap<>();
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
        this.regesterySize = ((ASTIntValue)node.jjtGetChild(0)).getValue();
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

        ASTOutNode outNode = (ASTOutNode) node.jjtGetChild(1);
        this.aliveInfo.add(outNode.getLive());

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
        int index = node.getId();
        String operation = node.getOp();

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);

        int leftRegistery = this.getReg(left, right, index);
        int rightRegistery = this.getReg(right, left, index);


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
        int index = node.getId();

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        int operande = this.getReg(left, null, index);
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
        int index = node.getId();

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        int operande = this.getReg(left, null, index);

        // TODO: Chaque variable a son emplacement en mémoire, mais si elle est déjà dans un registre, ne la rechargez pas!

        // TODO: Si une variable n'est pas vive, ne l'enregistrez pas en mémoire.

        // TODO: Si vos registres sont pleins, déterminez quelle variable que vous allez retirer et si vous devez la sauvegarder

        // TODO: Écrivez la traduction en code machine, une instruction intermédiaire peut générer plus qu'une instruction machine

        return null;
    }

    public int getReg(String variable, String locked, int opIndex) {

        // 1.
        for (int i = 0; i < registery.size(); i++) {
            if (registery.get(i).contains(variable))
                return i;
        }

        // 2.
        if (this.registery.size() < this.regesterySize) {
            this.registery.add(new ArrayList<>());
            this.registery.get(this.registery.size() - 1).add(variable);
            this.m_writer.println("LD R" + (this.registery.size() - 1) + ", " + variable);
            this.memory.get(variable).add("R" + (this.registery.size() - 1));
            return this.registery.size() - 1;
        }

        // 3.a.
        for (int i = 0; i < registery.size(); i++) {
            boolean ok = true;
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || locked.equals(this.registery.get(i).get(j)))
                    ok = false;
            }
            if (ok) {
                this.m_writer.println("LD R" + i + ", " + variable);
                this.memory.get(variable).add("R" + i);
                return i;
            }
        }

        // 3.b.
        for (int i = 0; i < registery.size(); i++) {
            if (this.registery.get(i).contains(variable)) {
                int index =  this.registery.get(i).indexOf(variable);

                boolean ok = true;
                for (int j = 0; j < this.registery.get(i).size(); j++) {
                    if (j == index)
                        continue;

                    if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || locked.equals(this.registery.get(i).get(j)))
                        ok = false;
                }
                if (ok) {
                    this.m_writer.println("LD R" + i + ", " + variable);
                    this.memory.get(variable).add("R" + i);
                    return i;
                }
            }

        }

        // 3.c.
        for (int i = 0; i < registery.size(); i++) {
            boolean ok = true;
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)) || locked.equals(this.registery.get(i).get(j)))
                    ok = false;
            }
            if (ok) {
                this.m_writer.println("LD R" + i + ", " + variable);
                this.memory.get(variable).add("R" + i);
                return i;
            }
        }

        // 3.d.
        for (int i = 0; i < registery.size(); i++) {
            boolean ok = true;
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (locked.equals(this.registery.get(i).get(j)))
                    ok = false;
            }
            if (ok) {

                for (int j = 0; j < this.registery.get(i).size(); j++) {
                    String v = this.registery.get(i).get(j);


                    if (this.memory.get(v).size() < 2)
                        this.m_writer.println("ST " + variable + ", R" + i);

                    this.memory.put(v, new ArrayList<>());
                    this.memory.get(v).add(v);
                }

                this.m_writer.println("LD R" + i + ", " + variable);
                this.memory.get(variable).add("R" + i);
                return i;
            }
        }

        return -1;
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
