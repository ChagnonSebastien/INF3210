package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;

public class CodeGenVisitor implements ParserVisitor {

    private final PrintWriter m_writer;
    private String m_indent;

    public CodeGenVisitor(PrintWriter writer) {

        m_writer = writer;
        m_indent = "";
    }



    private void moreIndent() {
        m_indent += "  ";
    }

    private void lessIndent() {
        m_indent = m_indent.substring(0, m_indent.length() - 2);
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        data = node.childrenAccept(this, data);
        return data;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        for(int i = 0; i < node.jjtGetNumChildren(); i++) {
            Node childNode = node.jjtGetChild(i);
            if(childNode.getClass() == ASTBlock.class) {
                m_writer.print("\n" + m_indent + "{");
                moreIndent();
            }
            childNode.jjtAccept(this, data);
            if(childNode.getClass() == ASTBlock.class) {
                lessIndent();
                m_writer.print("\n" + m_indent + "}");
            }
        }
        return data;
    }


    @Override
    public Object visit(ASTStmt node, Object data) {
        if(node.jjtGetNumChildren() == 0) {
            m_writer.print("\n" + m_indent + ";");
        }
        data = node.childrenAccept(this, data);
        return data;
    }

    @Override
    public Object visit(ASTIfStmt node, Object data) {

        m_writer.print("\n" + m_indent + "if (");
        node.jjtGetChild(0).jjtAccept(this, null);
        m_writer.print(") ");
        boolean isBlock = node.jjtGetChild(1).getClass() == ASTBlock.class;
        if(isBlock) {
            m_writer.print(" {");
            moreIndent();
        }
        node.jjtGetChild(1).jjtAccept(this, null);
        if(isBlock) {
            lessIndent();
            m_writer.print("\n" + m_indent + "}");
        }
        if(node.jjtGetNumChildren()==3){
            m_writer.print("\n" + m_indent + "else {");
            moreIndent();
            node.jjtGetChild(2).jjtAccept(this, null);
            lessIndent();
            m_writer.print("\n" + m_indent + "}");
        }
        return data;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        m_writer.print("\n" + m_indent + "while (");
        node.jjtGetChild(0).jjtAccept(this, null);
        m_writer.print(") {");
        moreIndent();
        node.jjtGetChild(1).jjtAccept(this, null);
        lessIndent();
        m_writer.print("\n" + m_indent + "}");
        return data;
    }


    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        m_writer.print("\n" + m_indent);
        node.jjtGetChild(0).jjtAccept(this, null);
        m_writer.print(" = ");
        node.jjtGetChild(1).jjtAccept(this, null);
        m_writer.print(";");
        return data;
    }

    @Override
    public Object visit(ASTExpr node, Object data){
        node.jjtGetChild(0).jjtAccept(this, null);

        m_writer.print(" Expr ");

        return null;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, null);
        for(int i = 1; i < node.jjtGetNumChildren(); i++) {
            //m_writer.print(" " + node.getOps().get(i - 1) + " ");
            m_writer.print(" " + "+?" + " ");
            node.jjtGetChild(i).jjtAccept(this, null);
        }
        return null;
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        Node n = node.jjtGetChild(0);
        if(n.getClass() == ASTIdentifier.class) {
            n.jjtAccept(this, null);
        }
        else if(n.getClass() == ASTIntValue.class) {
            n.jjtAccept(this, null);
        }
        else if(n.getClass() == ASTExpr.class) {
            m_writer.print("(");
            n.jjtAccept(this, null);
            m_writer.print(")");
        }
        else if(n.getClass() == ASTBoolValue.class) {
            n.jjtAccept(this, null);
        }
        else {
            throw new RuntimeException("Unexpected child");
        }
        return null;
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        m_writer.print(node.getValue());
        return null;
    }


    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // Production: Id -> letter ( letter | digit )*
        m_writer.print(node.getValue());
        return null;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        // Production: Int -> integer
        m_writer.print(node.getValue());
        return null;
    }
}
