package analyzer.tests;

import analyzer.visitors.CodeGenVisitor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.*;

import analyzer.ast.ParserVisitor;

@RunWith(Parameterized.class)
public class CodeGenTest extends BaseTest {

    private static String m_test_suite_path = "./test-suite/CodeGenTest/data";

    public CodeGenTest(File file) {
        super(file);
    }

    @Test
    public void run() throws Exception {
        ParserVisitor algorithm = new CodeGenVisitor(m_output);
        runAndAssert(algorithm);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getFiles() {
        return getFiles(m_test_suite_path);
    }

}
