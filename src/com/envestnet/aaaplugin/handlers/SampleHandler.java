package com.envestnet.aaaplugin.handlers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.envestnet.aaaplugin.util.FileUtils;
import com.envestnet.aaaplugin.util.TestDetector;
import org.eclipse.core.commands.AbstractHandler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.DirectoryDialog;


import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;

public class SampleHandler extends AbstractHandler {

	private static CSVWriter writer;
	private static CSVReader reader;
	private IWorkbenchWindow window;
	private File projectRoot;
	private String outputBase;

	private static void dfs(CompilationUnit cu,MethodDeclaration mdc, int level, List<String> visited, String currentMethodNameAndParam) {
		InvocationVisitor visitor = new InvocationVisitor();
		if (mdc != null) {
			System.out.println("mdc is not null");
		}
		mdc.accept(visitor);

		//get the current method qualified name and parameters
		//this works for recursive method detection and early stop
		IMethodBinding thisMethodBinding = mdc.resolveBinding();
		String thisMethodQualifiedNameAndParam = thisMethodBinding.getDeclaringClass().getQualifiedName() + "." + mdc.getName().toString() + getParameters(thisMethodBinding);

		for (ASTNode node : visitor.getMethods()) {
			if (node instanceof MethodInvocation) {
				try {
					MethodInvocation mi = (MethodInvocation) node;
					if (mi != null) {
						System.out.println("mi is not null");
					}
					IMethodBinding binding = mi.resolveMethodBinding();
					System.out.println("MethodInvocation: " + mi);
					//get the line number of the current method invocation
					String lineNumberRangeMI = getLineNumberRange(cu, mi);
					System.out.println("lineNumberRangeMI: " + lineNumberRangeMI);
					
					//check if this method is a recursive call
					if (thisMethodQualifiedNameAndParam.equals(currentMethodNameAndParam)) {
						System.out.println("Recursive call detected, early stop");
						continue;
					}

					if (binding == null) {
						System.out.println("binding is null");
					}

					if (isAssert(mi)) {
						visited.add(getSpace(level) + "ASSERT " + binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding) + "#" + lineNumberRangeMI);
						continue;
					}

					if (isJunitExpectedException(mi)) {
						visited.add(getSpace(level) + "ExpectedException " + binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding) + "#" + lineNumberRangeMI);
						continue;
					}

					if (isEasyMock(mi)) {
						visited.add(getSpace(level) + "MOCK " + binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding) + "#" + lineNumberRangeMI);
						continue;
					}

					if (isMockito(mi)) {
						visited.add(getSpace(level) + "MOCK " + binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding) + "#" + lineNumberRangeMI);
						continue;
					} else if (isThirdParty(mi)) {
						//Add the mark for Third party package and JDK API, but JDK Constructor is not collected.
						//This else branch means the function is a method invocation, but it is not mock/test/production/Junit
						//This invocation is a third party function
						if (isGetter(mi)) {
							visited.add(getSpace(level) + "THIRD GET " + binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding) + "#" + lineNumberRangeMI);
						} else if (isSetter(mi)) {
							visited.add(getSpace(level) + "THIRD SET " + binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding) + "#" + lineNumberRangeMI);
						} else {
							visited.add(getSpace(level) + "THIRD " + binding.getDeclaringClass().getQualifiedName() + "." + mi.getName().toString() + getParameters(binding) + "#" + lineNumberRangeMI);
						}
					}

					//NEED some check to make sure if it is refecting the line number of current CU or expanded CU
					//if any before isxxxx function is true, then here unit will be null.
					else {

						ICompilationUnit unit = (ICompilationUnit) binding.getJavaElement().getAncestor(IJavaElement.COMPILATION_UNIT);

						if (unit == null) {
							System.out.println("unit is null");
							continue;
						}

						CompilationUnit external_cu = parse(unit);
						MethodDeclaration md = (MethodDeclaration) external_cu.findDeclaringNode(binding.getKey());

						// if MethodDeclaration is null, give another try
						if (md == null) {
							md = getMD(binding, external_cu);
						}
						// if MethodDeclaration is still null, skip
						if (md == null) {
							continue;
						}

						if (FileUtils.isProduction(unit.getPath().makeAbsolute().toFile())) {
							String qualifiedName = md.resolveBinding().getDeclaringClass().getQualifiedName() + "." + md.getName().toString();
							String lineNumberRangePC = getLineNumberRange(cu, mi);//here we use current cu
							if (isGetter(mi)) {
								visited.add(getSpace(level) + "GET " + qualifiedName + getParameters(binding) + "#" + lineNumberRangePC);
							} else if (isSetter(mi)) {
								visited.add(getSpace(level) + "SET " + qualifiedName + getParameters(binding) + "#" + lineNumberRangePC);
							} else {
								visited.add(getSpace(level) + qualifiedName + getParameters(binding) + "#" + lineNumberRangePC);
							}
						} else if (FileUtils.isTest(unit.getPath().makeAbsolute().toFile())) {
							String qualifiedName = md.resolveBinding().getDeclaringClass().getQualifiedName() + "." + md.getName().toString();
							String lineNumberRangeTS = getLineNumberRange(cu, mi);
//							boolean isVisited = isVisited(level, visited, "TEST " + qualifiedName + getParameters(binding) + "#" + lineNumberRange);
							if (isGetter(mi)) {
								visited.add(getSpace(level) + "TEST GET " + qualifiedName + getParameters(binding) + "#" + lineNumberRangeTS);
							} else if (isSetter(mi)) {
								visited.add(getSpace(level) + "TEST SET " + qualifiedName + getParameters(binding) + "#" + lineNumberRangeTS);
							} else {
								visited.add(getSpace(level) + "TEST " + qualifiedName + getParameters(binding) + "#" + lineNumberRangeTS);
							}

//							if (!isVisited) {
//								dfs(md, level + 1, visited);
//							}
							dfs(external_cu, md, level + 1, visited, thisMethodQualifiedNameAndParam); // TODO: need to add early stop!
						}
					}

				} catch (Exception ex) {
					// TODO: handle exception
//					ex.printStackTrace();
				}


			} else if (node instanceof ClassInstanceCreation) {
				try {
					ClassInstanceCreation ci = (ClassInstanceCreation) node;
					IMethodBinding binding = ci.resolveConstructorBinding();
					ITypeBinding declaringClass = binding.getDeclaringClass();
					String lineNumberRangeCC = getLineNumberRange(cu, ci);
					// jdk
					if (declaringClass.getQualifiedName().startsWith("java.")) {
						continue;
					}

					// anonymous
					if (declaringClass.isAnonymous()) {
						visited.add(getSpace(level) + "NEW anonymous" + getParameters(binding) + "#" + lineNumberRangeCC);
					} else {
						visited.add(getSpace(level) + "NEW " + declaringClass.getQualifiedName() + getParameters(binding) + "#" + lineNumberRangeCC);
					}

				} catch (Exception e) {
					System.out.println("constructor not resolvable");
				}
			} else if (node instanceof ExpressionMethodReference) {
				try {
					ExpressionMethodReference mr = (ExpressionMethodReference) node;

					IMethodBinding binding = mr.resolveMethodBinding();
					
					String lineNumberRangeEM = getLineNumberRange(cu, mr);

					String qualifiedName = binding.getDeclaringClass().getQualifiedName() + "." + binding.getName();
					visited.add(getSpace(level) + qualifiedName + getParameters(binding) + "#" + lineNumberRangeEM);

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (node instanceof NormalAnnotation) {
				try {
					NormalAnnotation na = (NormalAnnotation) node;
					List<MemberValuePair> values = na.values();
					for (MemberValuePair v : values) {
						if (v.getName().toString().equals("expected")) {
							String lineNumberRangeEE = getLineNumberRange(cu, na);
							String qualifiedName = v.getValue().resolveTypeBinding().getQualifiedName();
							visited.add(getSpace(level) + "@EXPECTED " + qualifiedName + "#" + lineNumberRangeEE);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		;
	}

	private static boolean isMockito(MethodInvocation mi) {
		if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().startsWith("org.mockito")) {
			return true;
		}

		return false;
	}

	private static boolean isThirdParty(MethodInvocation mi) {
		if (mi.resolveMethodBinding().getJavaElement().getPath().makeAbsolute().toString().endsWith(".jar")) {
			return true;
		}
		return false;
	}

	private static boolean isGetter(MethodInvocation mi) {
		return (mi.getName().toString().contains("get") || mi.getName().toString().contains("is")) && parameterQuantity(mi) == 0;
	}

	private static boolean isSetter(MethodInvocation mi) {
		IMethodBinding binding = mi.resolveMethodBinding();
		return mi.getName().toString().contains("set") && parameterQuantity(mi) != 0 && Objects.equals(binding.getReturnType().toString(), "void");
	}

	private static boolean isEasyMock(MethodInvocation mi) {
		if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().startsWith("org.easymock")) {
			return true;
		}

		return false;
	}

	private static int parameterQuantity(MethodInvocation mi) {
		IMethodBinding binding = mi.resolveMethodBinding();
		return binding.getParameterTypes().length;
	}

	private static boolean isAssert(MethodInvocation mc) {
		if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains(".Assert")) {
			return true;
		} else if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("MatcherAssert")) {
			return true;
		} else if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.hamcrest")) {
			return true;
		} else if (mc.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.junit")) {
			return true;
		}

		return false;
	}

	private static List<String[]> generateOutput(String className, String methodName, String packageName, List<String> visited) {
		List<String[]> result = new ArrayList<>();
		for (String method : visited) {
			String[] methodSplit = method.split("#");
			if (method.trim().startsWith("ASSERT")) {
				result.add(new String[]{packageName, className, methodName, methodSplit[0], "2", methodSplit[1]});
			} else {
				result.add(new String[]{packageName, className, methodName, methodSplit[0], "0", methodSplit[1]});
			}

		}
		return result;
	}

	/*
	 * helper method to get the line number range of a node
	 */
	private static String getLineNumberRange(CompilationUnit cu, ASTNode node) {
		int startLine = cu.getLineNumber(node.getStartPosition());
		int endLine = cu.getLineNumber(node.getStartPosition() + node.getLength() - 1);
		 System.out.println("startLine: " + startLine);
		 System.out.println("endLine: " + endLine);
		return "[" + startLine + "-" + endLine + "]";
	}
	

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java
	 * source file
	 *
	 * @param unit
	 * @return
	 */

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS14);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		//select project root folder and report output folder
		String rootPath = selectFolder("Select Project Root Folder");
		System.out.println(rootPath);
		if (rootPath != null) {
			projectRoot = new File(rootPath);
		}
		outputBase = rootPath + File.separator + "AAA";
		new File(rootPath, "AAA").mkdir();
		System.out.println(outputBase);

		TestDetector testDetector = new TestDetector(projectRoot);

		try {
			String testTargetsFile = testDetector.detectTestsAndSaveToCSV(outputBase);
			System.out.println("Detecting");
			analyzer(testTargetsFile);
		} catch (CsvValidationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//TODO: Python Runner

		MessageDialog.openInformation(window.getShell(), "AAA Analyzer", "AAA Analyzing Finished.");

		return null;
	}

	private static boolean isVisited(int level, List<String> visited, String str) {
		for (int i = level; i >= 0; i--) {
			if (visited.contains(getSpace(i) + str)) {
				return true;
			}
		}

		return false;
	}

	private static String getParameters(IMethodBinding binding) {
		String parameters = "(";

		for (ITypeBinding p : binding.getParameterTypes()) {
			parameters = parameters + p.getName() + ", ";
		}

		if (parameters.length() > 1) {
			parameters = parameters.substring(0, parameters.length() - 2);
		}

		parameters = parameters + ")";

		return parameters;
	}

	private static String getSpace(int count) {
		String space = "";
		for (int i = 1; i < count; i++) {
			space += "     ";
		}
		return space;
	}

	private static CompilationUnit getCU(MethodDeclaration mdc) {
		ICompilationUnit unit = (ICompilationUnit) mdc.resolveBinding().getJavaElement().getAncestor(IJavaElement.COMPILATION_UNIT);
		CompilationUnit cu = parse(unit);
		return cu;
	}

	private static MethodDeclaration getMD(IMethodBinding binding, CompilationUnit cu) {
		try {
			GenericMethodVisitor visitor = new GenericMethodVisitor(binding);
			cu.accept(visitor);
			return visitor.getMethod();
		} catch (Exception e) {
			return null;
		}
	}

	public static CompilationUnit getCompilationUnit(IJavaProject project, IMember member) {
		ASTParser parser = ASTParser.newParser(AST.JLS14);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(project);
		parser.setSource(member.getCompilationUnit());
		return (CompilationUnit) parser.createAST(null);
	}

	private void analyzer(String testTargetsfile) throws IOException, CsvException {
		FileReader inputfile = new FileReader(testTargetsfile);
		// create CSVWriter object filewriter object as parameter

		RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
		reader = new CSVReaderBuilder(inputfile).withCSVParser(rfc4180Parser).build();

		List<String[]> lines = reader.readAll();
		int count = 0;
		System.out.println("CSV Reading Finished.");
		for (String[] line : lines) {
			featureExtraction(line, count++);
		}
	}

	private static boolean isJunitExpectedException(MethodInvocation mi) {
		if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals("org.junit.rules.ExpectedException")) {
			return true;
		}

		return false;
	}

	/**
	 * This method uses the DirectoryDialog to allow user to select a folder
	 *
	 * @param title The title of the dialog.
	 * @return The selected folder path or null if no folder was selected.
	 */
	private String selectFolder(String title) {
		DirectoryDialog dialog = new DirectoryDialog(window.getShell());
		dialog.setText(title);
		dialog.setMessage("Select a folder");
		return dialog.open();
	}

	private void featureExtraction(String[] line, int count) throws IOException {
		File fileName = new File(line[0]);
		System.out.println(fileName);
		String className = line[1].split(":")[0].trim();
		String methodName = line[1].split(":")[1].trim();

		System.out.println(className);
		System.out.println(methodName);

		List<String> visited = new ArrayList<>();

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath path = Path.fromOSString(fileName.getAbsolutePath());
		System.out.println(path);
		IFile file = workspace.getRoot().getFileForLocation(path);
		ICompilationUnit unit = (ICompilationUnit) JavaCore.create(file);
		System.out.println("ICompilationUnit: " + unit);
		CompilationUnit cu = parse(unit);
		System.out.println("CompilationUnit: " + cu);


		MethodVisitor visitor = new MethodVisitor();
		cu.accept(visitor);

		for (MethodDeclaration md : visitor.getMethods()) {
			if (md.getName().toString().equals(methodName)) {
				System.out.println("begin dfs");
				try {
					dfs(cu, md, 1, visited, "");
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("end dfs");
				break;
			}
		}

		// open
		File taggingFolder = new File(outputBase, "parsed");
		taggingFolder.mkdir();
		new File(outputBase, "tagged").mkdir();
		File outputFile = new File(taggingFolder + File.separator + Integer.toString(count) + "_" + className + "." + methodName + ".csv");
		outputFile.createNewFile();
		FileWriter outputfile = new FileWriter(outputFile);
		writer = new CSVWriter(outputfile);
		// header
		String[] header = new String[]{"testPackage", "testClassName", "testMethodName", "potentialTargetQualifiedName", "AAA(0,1,2)", "lineNumber", "isMock", "Assert Distance", "Level", "Name Similarity"};
		// write
		writer.writeNext(header);
		// result
		List<String[]> result = generateOutput(className, methodName, cu.getPackage().getName().toString(), visited);
		for (String[] next : result) {
			writer.writeNext(next);
		}
		// close
		writer.close();

		System.out.println("end of feature extraction");
	}
}
