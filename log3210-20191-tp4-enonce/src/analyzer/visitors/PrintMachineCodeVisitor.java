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
        for (int i = 0; i < this.regesterySize; i++)
            this.registery.add(new ArrayList<>());

        return null;
    }


    @Override
    public Object visit(ASTLive node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTLiveNode node, Object data) {
        ASTOutNode outNode = (ASTOutNode) node.jjtGetChild(1);
        this.aliveInfo.add(outNode.getLive());

        if (this.memory.size() == 0) {
            ASTInNode inNode = (ASTInNode) node.jjtGetChild(0);
            for (int i = 0; i < inNode.getLive().size(); i++) {
                String variable = inNode.getLive().get(i);
                this.memory.put(variable, new ArrayList<>());
                this.memory.get(variable).add(variable);
            }
        }

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
        int index = node.getId();
        String operation = node.getOp().equals("+") ? "ADD" : "MUL";

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);

        int leftRegistery = this.getReg(left, right, index);
        int rightRegistery = this.getReg(right, left, index);
        int result = this.getToReg(assigned, index, leftRegistery, rightRegistery);

        for (int i = 0; i < registery.get(result).size(); i++)
            if (this.memory.containsKey(registery.get(result).get(i)))
                this.memory.get(registery.get(result).get(i)).remove("R" + result);

        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + result);

        this.registery.get(result).clear();
        this.registery.get(result).add(assigned);

        this.m_writer.println(operation + " R" + result + ", R" + leftRegistery + ", R" + rightRegistery);

        this.removeExpired(assigned, result);
        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        int index = node.getId();

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        int operande = this.getReg(left, null, index);
        int result = this.getToReg(assigned, index, operande, -1);

        for (int i = 0; i < registery.get(result).size(); i++)
            this.memory.get(registery.get(result).get(i)).remove("R" + result);
        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + result);
        this.registery.get(result).clear();
        this.registery.get(result).add(assigned);
        this.m_writer.println("MUL R" + result + ", R" + operande + ", -1");

        this.removeExpired(assigned, result);
        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        int index = node.getId();

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        int operande = this.getReg(left, null, index);

        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + operande);

        this.removeExpired(assigned, operande);
        return null;
    }


    public int getReg(String variable, String locked, int opIndex) {

        // 1.
        for (int i = 0; i < registery.size(); i++) {
            if (registery.get(i).contains(variable))
                return i;
        }

        // 2.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (registery.get(i).size() > 0)
                continue reg_loop;

            this.registery.get(i).add(variable);
            this.m_writer.println("LD R" + i + ", " + variable);
            this.memory.get(variable).add("R" + i);
            return i;
        }

        // 3.a.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || locked.equals(this.registery.get(i).get(j)))
                    continue reg_loop;

            this.m_writer.println("LD R" + i + ", " + variable);

            this.memory.get(variable).add("R" + i);
            return i;
        }

        // 3.b.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!this.registery.get(i).contains(variable))
                continue reg_loop;

            int index =  this.registery.get(i).indexOf(variable);

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (j == index)
                    continue in_loop;

                if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || locked.equals(this.registery.get(i).get(j)))
                    continue reg_loop;
            }

            this.m_writer.println("LD R" + i + ", " + variable);
            this.memory.get(variable).add("R" + i);
            return i;
        }

        // 3.c.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)) || locked.equals(this.registery.get(i).get(j)))
                    continue reg_loop;

            this.m_writer.println("LD R" + i + ", " + variable);
            this.memory.get(variable).add("R" + i);
            return i;
        }

        // 3.d.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (locked.equals(this.registery.get(i).get(j)))
                    continue reg_loop;

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

        return -1;
    }

    public int getToReg(String variable, int opIndex, int operande1, int operande2) {
        // 1.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!registery.get(i).contains(variable))
                continue reg_loop;

            in_loop:
            for (int j = 0; j < registery.get(i).size(); j++) {
                if (registery.get(i).get(j).equals(variable))
                    continue in_loop;

                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)))
                    continue reg_loop;
            }

            return i;
        }

        // 2.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)))
                    continue reg_loop;

            return i;
        }


        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!registery.get(i).contains(variable))
                continue reg_loop;

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (registery.get(i).get(j).equals(variable))
                    continue in_loop;

                if (!this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)))
                    continue in_loop;

                this.memory.get(this.registery.get(i).get(j)).add(this.registery.get(i).get(j));
                this.m_writer.println("ST " + variable + ", R" + i);
            }
        }

        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (i == operande1 || i == operande2)
                continue reg_loop;

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (!this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)))
                    continue in_loop;

                this.memory.get(this.registery.get(i).get(j)).add(this.registery.get(i).get(j));
                this.m_writer.println("ST " + variable + ", R" + i);
            }
        }
        return -1;
    }

    public void removeExpired(String variable, int newRegIndex) {
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (i == newRegIndex)
                continue reg_loop;

            if (registery.get(i).contains(variable))
                registery.get(i).remove(variable);
        }
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
