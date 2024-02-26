package com.envestnet.aaaplugin.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.opencsv.CSVWriter;

public class TestDetector {

    private final File projectRoot;
    public int testCount = 0;

    public TestDetector(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public String detectTestsAndSaveToCSV(String outputPath) throws IOException {
        List<String[]> data = new ArrayList<>();
        String projectName = projectRoot.getName();
        for (File file : getAllTestJavaFiles(projectRoot)) {
            System.out.println(file);
            List<String> testCases = extractTestCasesFromFile(file);
            for (String testCase : testCases) {
                data.add(new String[]{file.getAbsolutePath(), testCase});
            }
        }
        testCount = data.size();

        //open
        String outputFilePath = outputPath + File.separator + projectName + "_" + "testCases.csv";
        System.out.println("OutPut file path:");
        System.out.println(outputFilePath);
        File outputFile = new File(outputFilePath);
        outputFile.createNewFile();
        FileWriter outputfile = new FileWriter(outputFile);
        CSVWriter writer = new CSVWriter(outputfile);
        //write
        for (String[] row : data) {
            writer.writeNext(row);
        }
        //close
        writer.close();

        return outputFilePath;
    }

    private List<File> getAllTestJavaFiles(File directory) {
        List<File> testFiles = new ArrayList<>();
        if (directory.isDirectory()) {
            System.out.println("Scanning directory " + directory.getAbsolutePath());
            // Check if the current directory path ends with src/test
            if (directory.getAbsolutePath().endsWith(File.separator + "src" + File.separator + "test") || directory.getAbsolutePath().endsWith(File.separator + "test")) {
                System.out.println("The Test folder is:");
                System.out.println(directory);
                testFiles.addAll(getAllTestJavaFilesInTestDirectory(directory));
            } else {// if not, check the subfolders
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    testFiles.addAll(getAllTestJavaFiles(file));
                }
            }
        }
        return testFiles;
    }

    private List<File> getAllTestJavaFilesInTestDirectory(File directory) {
        List<File> testFiles = new ArrayList<>();
        if (directory.isFile() && directory.getName().toLowerCase().contains("test") && directory.getName().endsWith(".java")) {
            testFiles.add(directory);
        } else if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                testFiles.addAll(getAllTestJavaFilesInTestDirectory(file));
            }
        }
        return testFiles;
    }

    private List<String> extractTestCasesFromFile(File javaFile) {
        List<String> testCases = new ArrayList<>();
        ASTParser parser = ASTParser.newParser(AST.JLS14);

        char[] fileContent;

        try {
            fileContent = readFile(javaFile).toCharArray();
        } catch (IOException e) {
            e.printStackTrace();
            return testCases;
        }

        parser.setSource(fileContent);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setEnvironment(new String[]{}, new String[]{javaFile.getParent()}, null, true);
        parser.setUnitName(javaFile.getName());
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setCompilerOptions(JavaCore.getOptions());

        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                for (Object modifier : node.modifiers()) {
                    if (modifier instanceof MarkerAnnotation) {
                        MarkerAnnotation annotation = (MarkerAnnotation) modifier;
                        if (annotation.getTypeName().getFullyQualifiedName().equals("Test")) {
                            String className = ((TypeDeclaration) node.getParent()).getName().getIdentifier();
                            testCases.add(className + ":" + node.getName().getIdentifier());
                        }
                    } else if (modifier instanceof NormalAnnotation) {
                        NormalAnnotation annotation = (NormalAnnotation) modifier;
                        if (annotation.getTypeName().getFullyQualifiedName().equals("Test")) {
                            String className = ((TypeDeclaration) node.getParent()).getName().getIdentifier();
                            testCases.add(className + ":" + node.getName().getIdentifier());
                        }
                    }
                }
                return super.visit(node);
            }
        };

        parser.createAST(null).accept(visitor);
        return testCases;
    }

    private String readFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            char[] buffer = new char[(int) file.length()];
            reader.read(buffer);
            return new String(buffer);
        }
    }

}
