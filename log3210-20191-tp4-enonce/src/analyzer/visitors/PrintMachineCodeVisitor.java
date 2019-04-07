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

        // Visit all childrens
        node.childrenAccept(this, null);

        // Save all alive values in the registery back in the memory
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
                m_writer.println("ST " + variable + ", R" + i);
                this.memory.get(variable).add(variable);
            }
        }

        m_writer.close();
        return null;
    }


    @Override
    public Object visit(ASTNumberRegister node, Object data) {
        node.childrenAccept(this, null);

        // Initialize the registery array with the right size
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

        // Add the information about which variables are alive at the end of an instruction in a static array
        ASTOutNode outNode = (ASTOutNode) node.jjtGetChild(1);
        this.aliveInfo.add(new ArrayList<>());
        for (int i = 0; i < outNode.getLive().size() ; i++)
            this.aliveInfo.get(this.aliveInfo.size() - 1).add(outNode.getLive().get(i));

        //  If it's the first instruction, save the alive variables in the memory at the start of the program
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

        // Increase the instruction number
        int index = indexOfTree++;

        // Get the instruction syntax
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);
        String operation = getOppSyntax(node.getOp());

        // Check for int values
        boolean leftInt = isInteger(left);
        boolean rightInt = isInteger(right);

        // Get the registery index of the operands
        // If the operands are not in the registery, getReg() loads the values in the memory
        int leftRegistery = leftInt ? -1 : this.getReg(left, right, index);
        int rightRegistery = rightInt ? -1 : this.getReg(right, left, index);

        // Get the registery where to put the result of the operation
        int result = this.getToReg(assigned, index, leftRegistery, rightRegistery);

        // Update the registery references in the memory
        for (int i = 0; i < registery.get(result).size(); i++)
            if (this.memory.containsKey(registery.get(result).get(i)))
                this.memory.get(registery.get(result).get(i)).remove("R" + result);

        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + result);

        // Update the registery
        this.registery.get(result).clear();
        this.registery.get(result).add(assigned);

        // Print the instruction
        this.m_writer.println(operation + " R" + result + ", " + (leftInt ? "#" + left : "R" + leftRegistery) + ", " + (rightInt ? "#" + right : "R" + rightRegistery));

        // If the variable was in another registery, remove it's reference
        this.removeExpired(assigned, result);
        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {

        // Increase the instruction number
        int index = indexOfTree++;

        // Get the instruction syntax
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // Get the registery index of the operand
        // If the operand is not in the registery, getReg() loads the value in the memory
        int operande = this.getReg(left, null, index);

        // Get the registery where to put the result of the operation
        int result = this.getToReg(assigned, index, operande, -1);

        // Update the registery references in the memory
        for (int i = 0; i < registery.get(result).size(); i++)
            if (this.memory.containsKey(registery.get(result).get(i)))
                this.memory.get(registery.get(result).get(i)).remove("R" + result);
            
        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + result);
        
        // Update the registery
        this.registery.get(result).clear();
        this.registery.get(result).add(assigned);
        
        // Print the instruction
        this.m_writer.println("MUL R" + result + ", R" + operande + ", -1");

        // If the variable was in another registery, remove it's reference
        this.removeExpired(assigned, result);
        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        
        // Increase the instruction number
        int index = indexOfTree++;

        // Get the instruction syntax
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);

        // Get the registery index of the operand
        // If the operand is not in the registery, getReg() loads the value in the memory
        int operande = this.getReg(left, null, index);

        // Update the memory by adding a reference to the registery
        this.memory.put(assigned, new ArrayList<>());
        this.memory.get(assigned).add("R" + operande);
        
        // Update the registery
        this.registery.get(operande).add(assigned);

        // If the variable was in another registery, remove it's reference
        this.removeExpired(assigned, operande);
        return null;
    }


    public int getReg(String operand, String locked, int opIndex) {

        // 1.
        // If the operand is already in the registery Rn
        // Return n
        for (int i = 0; i < registery.size(); i++) {
            if (registery.get(i).contains(operand))
                return i;
        }


        // 2.
        // If there is a free registery Rn
        // Load the operand in the registery Rn
        // Return n
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (registery.get(i).size() > 0)
                continue reg_loop;

            this.registery.get(i).add(operand);
            this.m_writer.println("LD R" + i + ", " + operand);

            this.memory.get(operand).add("R" + i);
            return i;
        }


        // 3.a.
        // If all variables in a registery Rn are also saved in memory
        // Load the operand in the registery Rn
        // Return n
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || (locked != null && locked.equals(this.registery.get(i).get(j))))
                    continue reg_loop;
            }

            this.m_writer.println("LD R" + i + ", " + operand);
=
            for (int j = 0; j < registery.get(i).size(); j++)
                this.memory.get(this.registery.get(i).get(j)).remove("R" + i);
            this.registery.get(i).clear();
            this.registery.get(i).add(operand);

            this.memory.get(operand).add("R" + i);
            return i;
        }
        

        // 3.b.
        // If the variable in which the result of the instriction will be put is in the registery Rn and the variable is not also an operand
        // Load the operand in the registery Rn
        // Return n
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!this.registery.get(i).contains(operand))
                continue reg_loop;

            int index =  this.registery.get(i).indexOf(operand);

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                if (j == index)
                    continue in_loop;

                if (this.memory.get(this.registery.get(i).get(j)).size() < 2 || (locked != null && locked.equals(this.registery.get(i).get(j))))
                    continue reg_loop;
            }

            this.m_writer.println("LD R" + i + ", " + operand);
            this.memory.get(operand).add("R" + i);
            return i;
        }


        // 3.c.
        // If all variables in registery Rn are no longer alive
        // Load the operand in the registery Rn
        // Return n
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)) || (locked != null && locked.equals(this.registery.get(i).get(j))))
                    continue reg_loop;

            for (int j = 0; j < this.registery.get(i).size(); j++)
                this.memory.get(this.registery.get(i).get(j)).remove("R" + i);

            this.m_writer.println("LD R" + i + ", " + operand);
            this.memory.get(operand).add("R" + i);
            this.registery.get(i).clear();
            this.registery.get(i).add(operand);
            return i;
        }

        
        // 3.d.
        // Load the operand in the first registery Rn which does not contain the other operand of the instruction
        // Store in the memory the alive variables in registery Rn beforehand
        // Return n
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            for (int j = 0; j < this.registery.get(i).size(); j++)
                if (locked != null && locked.equals(this.registery.get(i).get(j)))
                    continue reg_loop;

            for (int j = 0; j < this.registery.get(i).size(); j++) {
                String variable = this.registery.get(i).get(j);

                if (this.memory.get(variable).size() == 1 && this.aliveInfo.get(opIndex).contains(variable)) {
                    this.m_writer.println("ST " + variable + ", R" + i);
                }

                this.memory.put(variable, new ArrayList<>());
                this.memory.get(variable).add(variable);
            }

            this.m_writer.println("LD R" + i + ", " + operande);
            this.memory.get(operande).add("R" + i);
            this.registery.get(i).clear();
            this.registery.get(i).add(operande);
            return i;
        }

        return -1;
    }

    public int getToReg(String assigned, int opIndex, int operande1, int operande2) {
        // Retourne le registre qui contient x et dont les autres varibles dans le registrer ne sont plus vivantes
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!registery.get(i).contains(assigned))
                continue reg_loop;

            in_loop:
            for (int j = 0; j < registery.get(i).size(); j++) {
                if (registery.get(i).get(j).equals(assigned))
                    continue in_loop;

                if (this.aliveInfo.get(opIndex).contains(this.registery.get(i).get(j)))
                    continue reg_loop;
            }

            return i;
        }

        // Retourne un registre dont toutes les variables ne sont plus vivantes
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
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            if (!registery.get(i).contains(assigned))
                continue reg_loop;

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                String variable = registery.get(i).get(j);

                // Do not store the value which will be redefined
                if (variable.equals(assigned))
                    continue in_loop;

                // Do not store the value if it's no longer alive
                if (!this.aliveInfo.get(opIndex).contains(variable))
                    continue in_loop;

                // Do not store the value if it's already in the memory
                if (this.memory.get(variable).contains(variable))
                    continue in_loop;

                // Save the alive variable in the memory
                this.memory.get(variable).add(variable);
                this.m_writer.println("ST " + variable + ", R" + i);
            }
        }

        // Retourne le premier registre qui n'est pas une opérande de l'inscriction et sauvegarde ses variables.
        reg_loop:
        for (int i = 0; i < registery.size(); i++) {
            // Store the result somewhere else than any of the two operandes
            if (i == operande1 || i == operande2)
                continue reg_loop;

            in_loop:
            for (int j = 0; j < this.registery.get(i).size(); j++) {
                String variable = registery.get(i).get(j);

                // Do not store the value if it's no longer alive
                if (!this.aliveInfo.get(opIndex).contains(variable))
                    continue in_loop;

                // Do not store the value if it's already in the memory
                if (this.memory.get(variable).contains(variable))
                    continue in_loop;

                this.memory.get(variable).add(variable);
                this.m_writer.println("ST " + variable + ", R" + i);
            }

            return i;
        }

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
