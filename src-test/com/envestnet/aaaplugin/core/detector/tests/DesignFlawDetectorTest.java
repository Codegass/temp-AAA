package com.envestnet.aaaplugin.core.detector.tests;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Assert;
import org.junit.Test;

import com.envestnet.aaaplugin.core.detector.*;

public class DesignFlawDetectorTest {

    @Test
    public void testDetectObscureAssert() {
        String sourceCode = "public class TestClass { void testMethod() { if (true) { assert true; } } }";
        MethodDeclaration method = getMethodDeclarationFromSource(sourceCode, "testMethod");

        DesignFlawDetector detector = new DesignFlawDetector();
        boolean result = detector.detectObscureAssert(method);
        Assert.assertTrue(result); // Adjust based on expected result
    }

    @Test
    public void testDetectSuppressedException() {
        String sourceCode = "public class TestClass { void testMethod() { try { int a = 1/0; } catch (Exception e) {} } }";
        MethodDeclaration method = getMethodDeclarationFromSource(sourceCode, "testMethod");

        DesignFlawDetector detector = new DesignFlawDetector();
        boolean result = detector.detectSuppressedException(method);
        Assert.assertTrue(result); // Adjust based on expected result
    }

    private MethodDeclaration getMethodDeclarationFromSource(String source, String methodName) {
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        for (Object type : cu.types()) {
            if (type instanceof TypeDeclaration) {
                for (MethodDeclaration method : ((TypeDeclaration) type).getMethods()) {
                    if (method.getName().getIdentifier().equals(methodName)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }
}

