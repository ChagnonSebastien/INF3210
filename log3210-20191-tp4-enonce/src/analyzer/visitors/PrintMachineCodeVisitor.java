package analyzer.visitors;

import analyzer.ast.*;
import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;
    private List<List<String>> aliveInfo = null;
    private List<List<String>> registery = null;
    private Map<String, List<String>> memory = null;
    private int regesterySize = 0;
    private int indexOfTree = 0;

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

        System.out.println("===================");
        System.out.println(memory);
        System.out.println(registery);
        System.out.println("=====SAVE=====");
        for (int i = 0; i < this.registery.size(); i++) {
            in_Loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                String variable = this.registery.get(i).get(j);

                // If the variable is not alive, continue to the next value
                if (!this.aliveInfo.get(this.aliveInfo.size() - 1).contains(variable))
                    continue in_Loop;

                // If the variable is already saved in the memory, continue to next value
                if (this.memory.get(variable).contains(variable))
                    continue in_Loop;

                // Save the variable in the memory
                System.out.println("ST " + variable + ", R" + i);
                m_writer.println("ST " + variable + ", R" + i);
                this.memory.get(variable).add(variable);
            }
        }
        System.out.println(memory);
        System.out.println(registery);
        System.out.println("===================");

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
        this.aliveInfo.add(new ArrayList<>());
        for (int i = 0; i < outNode.getLive().size() ; i++)
            this.aliveInfo.get(this.aliveInfo.size() - 1).add(outNode.getLive().get(i));

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

        System.out.println("===================");
        System.out.println(memory);
        System.out.println(registery);
        System.out.println("===================");
        node.childrenAccept(this, null);

        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        int index = indexOfTree++;
        System.out.println("Opération n." + index);
        String operation = getOppSyntax(node.getOp());

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);

        boolean leftInt = isInteger(left);
        boolean rightInt = isInteger(right);

        int leftRegistery = leftInt ? -1 : this.getReg(left, right, index);
        int rightRegistery = rightInt ? -1 : this.getReg(right, left, index);
        int result = this.getToReg(assigned, index, leftRegistery, rightRegistery);

        for (int i = 0; i < registery.get(result).size(); i++)
            if (this.memory.containsKey(registery.get(result).get(i)))
                this.memory.get(registery.get(result).get(i)).remove("R" + result);

        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + result);

        this.registery.get(result).clear();
        this.registery.get(result).add(assigned);

        System.out.println(operation + " R" + result + ", " + (leftInt ? "#" + left : "R" + leftRegistery) + ", " + (rightInt ? "#" + right : "R" + rightRegistery));
        this.m_writer.println(operation + " R" + result + ", " + (leftInt ? "#" + left : "R" + leftRegistery) + ", " + (rightInt ? "#" + right : "R" + rightRegistery));

        this.removeExpired(assigned, result);
        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        int index = indexOfTree++;
        System.out.println("Opération n." + index);

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
        System.out.println("MUL R" + result + ", R" + operande + ", -1");
        this.m_writer.println("MUL R" + result + ", R" + operande + ", -1");

        this.removeExpired(assigned, result);
        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        int index = indexOfTree++;
        System.out.println("Opération n." + index);

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        int operande = this.getReg(left, null, index);

        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + operande);
        this.registery.get(operande).add(assigned);

        this.removeExpired(assigned, operande);
        return null;
    }


    public int getReg(String variable, String locked, int opIndex) {

        // 1.
        System.out.println("1.");
        for (int i = 0; i < registery.size(); i++) {
            if (registery.get(i).contains(variable))
                return i;
        }

        // 2.
        System.out.println("2.");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (registery.get(i).size() > 0)
                continue reg_loop;

            this.registery.get(i).add(variable);
            System.out.println("LD R" + i + ", " + variable);
            this.m_writer.println("LD R" + i + ", " + variable);


            this.memory.get(variable).add("R" + i);
            return i;
        }

        // 3.a.
        System.out.println("3.a");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || (locked != null && locked.equals(this.registery.get(i).get(j))))
                    continue reg_loop;
            }

            System.out.println("LD R" + i + ", " + variable);
            this.m_writer.println("LD R" + i + ", " + variable);


            for (int j = 0; j < registery.get(i).size(); j++)
                this.memory.get(this.registery.get(i).get(j)).remove("R" + i);
            this.registery.get(i).clear();
            this.registery.get(i).add(variable);

            this.memory.get(variable).add("R" + i);
            return i;
        }

        // 3.b.
        System.out.println("3.b");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!this.registery.get(i).contains(variable))
                continue reg_loop;

            int index =  this.registery.get(i).indexOf(variable);

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (j == index)
                    continue in_loop;

                if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || (locked != null && locked.equals(this.registery.get(i).get(j))))
                    continue reg_loop;
            }

            System.out.println("LD R" + i + ", " + variable);
            this.m_writer.println("LD R" + i + ", " + variable);
            this.memory.get(variable).add("R" + i);
            return i;
        }

        // 3.c.
        System.out.println("3.c");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)) || (locked != null && locked.equals(this.registery.get(i).get(j))))
                    continue reg_loop;

            for (int j = 0; j < this.registery.get(i).size(); j++)
                this.memory.get(this.registery.get(i).get(j)).remove("R" + i);

            System.out.println("LD R" + i + ", " + variable);
            this.m_writer.println("LD R" + i + ", " + variable);
            this.memory.get(variable).add("R" + i);
            this.registery.get(i).clear();
            this.registery.get(i).add(variable);
            return i;
        }

        // 3.d.
        System.out.println("3.d");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (locked != null && locked.equals(this.registery.get(i).get(j)))
                    continue reg_loop;

            for (int j = 0; j < this.registery.get(i).size(); j++) {
                String lovalVariable = this.registery.get(i).get(j);

                if (this.memory.get(lovalVariable).size() == 1 && this.aliveInfo.get(opIndex).contains(lovalVariable)) {
                    System.out.println("ST " + lovalVariable + ", R" + i);
                    this.m_writer.println("ST " + lovalVariable + ", R" + i);
                }

                this.memory.put(lovalVariable, new ArrayList<>());
                this.memory.get(lovalVariable).add(lovalVariable);
            }

            System.out.println("LD R" + i + ", " + variable);
            this.m_writer.println("LD R" + i + ", " + variable);
            this.memory.get(variable).add("R" + i);
            this.registery.get(i).clear();
            this.registery.get(i).add(variable);
            return i;
        }

        return -1;
    }

    public int getToReg(String variable, int opIndex, int operande1, int operande2) {
        // Retourne le registre qui contient x et dont les autres varibles dans le registrer ne sont plus vivantes
        System.out.println("TO - 1");
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

        // Retourne un registre dont toutes les variables ne sont plus vivantes
        System.out.println("TO - 2");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)))
                    continue reg_loop;

            return i;
        }

        // Retourne le registre dont toutes les variables sont déjà en mémoire
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (this.memory.get(registery.get(i).get(j)).size() == 1)
                    continue reg_loop;

            return i;
        }

        // Retourne le registre qui contient x et sauvegarde les autres variables dans ce registre si elles sont vivantes
        System.out.println("TO - 3");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!registery.get(i).contains(variable))
                continue reg_loop;

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                String localVariable = registery.get(i).get(j);

                // Do not store the value which will be redefined
                if (localVariable.equals(variable))
                    continue in_loop;

                // Do not store the value if it's no longer alive
                if (!this.aliveInfo.get(opIndex).contains(localVariable))
                    continue in_loop;

                // Do not store the value if it's already in the memory
                if (this.memory.get(localVariable).contains(localVariable))
                    continue in_loop;

                // Save the alive variable in the memory
                this.memory.get(localVariable).add(localVariable);
                System.out.println("ST " + localVariable + ", R" + i);
                this.m_writer.println("ST " + localVariable + ", R" + i);
            }
        }

        // Retourne le premier registre qui n'est pas une opérande de l'inscriction et sauvegarde ses valeurs.
        System.out.println("TO - 4");
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            // Store the result somewhere else than any of the two operandes
            if (i == operande1 || i == operande2)
                continue reg_loop;

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                String localVariable = registery.get(i).get(j);

                // Do not store the value if it's no longer alive
                if (!this.aliveInfo.get(opIndex).contains(localVariable))
                    continue in_loop;

                // Do not store the value if it's already in the memory
                if (this.memory.get(localVariable).contains(localVariable))
                    continue in_loop;

                this.memory.get(localVariable).add(localVariable);
                System.out.println("ST " + localVariable + ", R" + i);
                this.m_writer.println("ST " + localVariable + ", R" + i);
            }

            return i;
        }

        System.out.println("TO - ERR");
        return -1;
    }

    public void removeExpired(String variable, int newRegIndex) {
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (i == newRegIndex)
                continue reg_loop;

            if (registery.get(i).contains(variable)) {
                registery.get(i).remove(variable);
                this.memory.get(variable).remove("R" + i);
            }
        }
    }

    public String getOppSyntax(String opp) {
        switch (opp) {
            case "+":
                return "ADD";
            case "*":
                return "MUL";
            case "/":
                return "DIV";
            case "-":
                return "SUB";
            default:
                return "ERR";
        }
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
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
