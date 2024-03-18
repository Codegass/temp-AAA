package com.envestnet.aaaplugin.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.envestnet.aaaplugin.core.data.AntiPattern;
import com.envestnet.aaaplugin.core.data.DesignFlaw;
import com.envestnet.aaaplugin.core.data.ReportGenerator;
import com.envestnet.aaaplugin.core.detector.DesignFlawDetector;
import com.envestnet.aaaplugin.core.detector.AntiPatternDetector;
import com.envestnet.aaaplugin.core.mltagging.MachineLearningProcessing;
import com.envestnet.aaaplugin.util.FileUtils;
import com.envestnet.aaaplugin.util.ProjectScanner;
import com.envestnet.aaaplugin.util.TestDetector;
import com.envestnet.aaaplugin.view.ResultView;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;

public class ContextMenuHandler implements IObjectActionDelegate {
	
    private IWorkbenchWindow window;
    private ISelection selection;
    
    private static CSVWriter writer;
	private static CSVReader reader;
	
	private File projectRoot;
	private String outputBase;
	private List<String[]> testCases;
	
	private ReportGenerator reportGenerator;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        window = targetPart.getSite().getWorkbenchWindow();
    }

    @Override
    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) this.selection;
            Object firstElement = structuredSelection.getFirstElement();
            reportGenerator = new ReportGenerator();
            
            if (firstElement instanceof IResource) {
                IResource resource = (IResource) firstElement; 
        		String rootPath = resource.getLocation().toString();
        		System.out.println(rootPath);
        		if (rootPath != null) {
        			projectRoot = new File(rootPath);
        		}
        		outputBase = rootPath + File.separator + "AAA";
        		new File(rootPath, "AAA").mkdir();
        		System.out.println(outputBase);
        		
				Job aaaAnalysisJob = new Job("AAA Analysis") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						String testTargetsFile;
						try {
							TestDetector testDetector = new TestDetector(projectRoot);
							testTargetsFile = testDetector.detectTestsAndSaveToCSV(outputBase);
							
							// Initial task setup
							int totalTestCases = countTestCases(testTargetsFile);
				            monitor.beginTask("AAA Tagging...", totalTestCases);
							
							try (FileReader inputfile = new FileReader(testTargetsFile)) {
								// Setup for reading CSV
								RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
								try (CSVReader reader = new CSVReaderBuilder(inputfile)
										.withCSVParser(rfc4180Parser)
										.build()) {
									testCases = reader.readAll();
									System.out.println("CSV Reading Finished.");
									
									int count = 0;
									for (String[] line : testCases) {
										if (monitor.isCanceled()) {
											return Status.CANCEL_STATUS;
										}
										try {
											featureExtraction(line, count++);
											monitor.worked(1);
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								} catch (CsvException e) {
									e.printStackTrace();
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							if (monitor.isCanceled()) {
								return Status.CANCEL_STATUS;
							}
							
							
							return Status.OK_STATUS;
						} catch (IOException e) {
							e.printStackTrace();
							return new Status(IStatus.ERROR, "aaaanalyzer", "Error occurred during AAA Tagging", e);
						} finally {
							monitor.done();
						}
					}
				};

				Job mlProcessingJob = new Job("Machine Learning Processing") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							MachineLearningProcessing mlProcessing = new MachineLearningProcessing();
							mlProcessing.setProjectRoot(rootPath); // Ensure rootPath is accessible
							mlProcessing.setupAndExecutePythonScript();
											
							return Status.OK_STATUS;
						} catch (Exception e) {
							e.printStackTrace();
							return new Status(IStatus.ERROR, "aaaanalyzer", "Error occurred during ML processing", e);
						}
					}
				};

				Job antiPatternDetectionJob = new Job("Anti-Pattern Detection") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						// Define the body of your job here
						try {
							// Include the anti-pattern detection logic you provided here
							List<String> csvFiles = FileUtils.findCsvFiles(outputBase + File.separator + "feature");
							int totalTestCase = csvFiles.size();
							Set<String> uniqueClassNames = new HashSet<>();
							for (String csvFile : csvFiles) {
								String className = FileUtils.extractPart(csvFile);
								uniqueClassNames.add(className);
							}
							int totalClass = uniqueClassNames.size();
							int totalLines = 0;
							
							// Assume testCases is accessible, otherwise you need to make it so
							for (int i = 0; i < testCases.size(); i++) {
								String[] testCase = testCases.get(i);
								System.out.println("testCase: " + testCase[0]);
								String sourceFilePath = testCase[0];
								String methodName = testCase[1].split(":")[1].trim();
								System.out.println("methodName: " + methodName);
								String csvFile = outputBase + File.separator + "feature" + File.separator + testCase[1].replace(":", ".") + ".csv";
								System.out.println("csvFile: " + csvFile);
								try {
									AntiPatternDetector antiPatternDetector = new AntiPatternDetector();
									Map<String, Boolean> results = antiPatternDetector.detectAntiPatterns(csvFile);
									System.out.println("detecting anti-patterns with file: " + csvFile);
									System.out.println(results);
									
									if (results.get("missingAssert")) {
										String severity = "Blocker";
										String description = "Missing Assert ";
										//build the line number is to get the method
										List<Integer> lineNumbers = antiPatternDetector.getLineNumberMissingAssert();
										//add design flaw to report
										reportGenerator.addAntiPattern(new AntiPattern(sourceFilePath, methodName,
																						testCase[1].split(":")[0].trim(), lineNumbers,
																						"missingAssert", description, severity));
									} else if (results.get("multipleAAA")) {
										String severity = "Minor";
										String description = "Multiple-AAA";
										//build the line number is to get the method
										List<Integer> lineNumbers = antiPatternDetector.getLineNumberMultipleAAA();
										//add design flaw to report
										reportGenerator.addAntiPattern(new AntiPattern(sourceFilePath, methodName,
																						testCase[1].split(":")[0].trim(), lineNumbers,
																						"multipleAAA", description, severity));
									} else if (results.get("assertPrecondition")) {
										String severity = "Info";
										String description = "Assert Precondition ";
										//build the line number is to get the method
										List<Integer> lineNumbers = antiPatternDetector.getLineNumberAssertPrecondition();
										//add design flaw to report
										reportGenerator.addAntiPattern(new AntiPattern(sourceFilePath, methodName,
																						testCase[1].split(":")[0].trim(), lineNumbers,
																						"assertPrecondition", description, severity));

									}
								} catch (IOException | CsvException e) {
									e.printStackTrace();
								}
							}
							
							// Assuming reportGenerator is accessible and properly initialized
							reportGenerator.checkDefault();
							reportGenerator.generateReport(outputBase + File.separator + "results.json");
							
							// Notify user of completion, update UI as necessary
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									MessageDialog.openInformation(Display.getDefault().getActiveShell(), "AAA Analyzing", "AAA Analyzing Finished.");
									// Additional UI updates here
								}
							});

							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									try {
										// Get the current workbench page
										IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
										
										// Open or activate the ResultView
										ResultView resultView = (ResultView) page.showView("com.envestnet.aaaplugin.view.ResultView");  // Adjust the ID to your actual view ID
										
										// Update the view
										List<String> projectRoots = ProjectScanner.scanWorkspaceForProjects();
										resultView.updateViewFromProjects(projectRoots);
										
									} catch (PartInitException e) {
										e.printStackTrace();
									}
								}
							});
				
							return Status.OK_STATUS;
						} catch (Exception e) {
							e.printStackTrace();
							return new Status(IStatus.ERROR, "aaaanalyzer", "Error occurred during anti-pattern detection", e);
						}
					}
				};
				

				// AAA analysisjob listener
				aaaAnalysisJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							// Only schedule the ML processing job if the first job completed successfully
							mlProcessingJob.schedule();
						} else {
							// Handle the case where the first job did not complete successfully
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									MessageDialog.openError(window.getShell(), "Error", "AAA Analysis did not complete successfully.");
								}
							});
						}
					}
				});

				// ML processing job listener
				mlProcessingJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							// Only schedule the anti-pattern detection job if the ML processing job completed successfully
							antiPatternDetectionJob.schedule();
						} else {
							// Handle the case where the ML processing job did not complete successfully
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "ML Processing did not complete successfully.");
								}
							});
						}
					}
				});				

				// Set job properties as needed
				aaaAnalysisJob.setUser(true); // Mark the job as a user job
				aaaAnalysisJob.schedule(); // Schedule the job

            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

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
					// System.out.println("lineNumberRangeMI: " + lineNumberRangeMI);
					
					//check if this method is a recursive call
					if (thisMethodQualifiedNameAndParam.equals(currentMethodNameAndParam)) {
						System.out.println("Recursive call detected, early stop");
						continue;
					}

					if (binding == null) {
						System.out.println("bing is null");
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

					//TODO: NEED some check to make sure if it is refecting the line number of current CU or expanded CU
					//if any before isxxxx function is true, then here unit will be null.
					else {

						ICompilationUnit unit = (ICompilationUnit) binding.getJavaElement().getAncestor(IJavaElement.COMPILATION_UNIT);

						if (unit == null) {
							System.out.println("unit is null");
							continue;
						}

						CompilationUnit expanded_cu = parse(unit);
						MethodDeclaration md = (MethodDeclaration) expanded_cu.findDeclaringNode(binding.getKey());

						// if MethodDeclaration is null, give another try
						if (md == null) {
							md = getMD(binding, expanded_cu);
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
//							boolean isVisited = isVisited(level, visited, "TEST " + qualifiedName + getParameters(binding));
							if (isGetter(mi)) {
								visited.add(getSpace(level) + "TEST GET " + qualifiedName + getParameters(binding) + "#" + lineNumberRangeTS);
							} else if (isSetter(mi)) {
								visited.add(getSpace(level) + "TEST SET " + qualifiedName + getParameters(binding) + "#" + lineNumberRangeTS);
							} else {
								visited.add(getSpace(level) + "TEST " + qualifiedName + getParameters(binding) + "#" + lineNumberRangeTS);
							}

							dfs(expanded_cu, md, level + 1, visited, thisMethodQualifiedNameAndParam); // TODO: need to add early stop!
						}
					}

				} catch (Exception ex) {
					ex.printStackTrace();
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
						if (v.getName().toString().equals("expected") || v.getName().toString().contains("expectedExceptions")) {
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
		if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().startsWith("org.mockito") && !mi.resolveMethodBinding().getMethodDeclaration().getName().contains("verify")) {
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
		if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().startsWith("org.easymock") && !mi.resolveMethodBinding().getMethodDeclaration().getName().contains("verify")) {
			return true;
		} else {
			return false;
		}
	}

	private static int parameterQuantity(MethodInvocation mi) {
		IMethodBinding binding = mi.resolveMethodBinding();
		return binding.getParameterTypes().length;
	}

	private static boolean isAssert(MethodInvocation mi) {
		if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains(".Assert")) {
			return true;
		} else if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("MatcherAssert")) {
			return true;
		} else if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.hamcrest")) {
			return true;
		} else if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.junit")) {
			return true;
		} else if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("org.assertj")) {
			return true;
		} else if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().contains("com.google.common.truth")) {
			return true;
		} else if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().startsWith("org.mockito") && mi.resolveMethodBinding().getMethodDeclaration().getName().contains("verify")) {
			return true;
		} else if (mi.resolveMethodBinding().getDeclaringClass().getQualifiedName().startsWith("org.easymock") && mi.resolveMethodBinding().getMethodDeclaration().getName().contains("verify")) {
			return true;
		}

		return false;
	}

	private static List<String[]> generateOutput(String className, String methodName, String packageName, List<String> visited) {
		List<String[]> result = new ArrayList<>();
		for (String method : visited) {
			String[] methodSplit = method.split("#");
			if (method.trim().startsWith("ASSERT") || method.trim().startsWith("@EXPECTED")) {
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
	
	private int countTestCases(String filePath) throws IOException {
	    int totalCases = 0;
	    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
	        while (reader.readLine() != null) {
	            totalCases++;
	        }
	    }
	    return totalCases;
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
					DesignFlawDetector designFlawDetector = new DesignFlawDetector();
                    //detect design flaw
                    if (designFlawDetector.detectObscureAssert(md)) {
                        List<Integer> linenumbers = designFlawDetector.getLineNumbers();
                        String severity = "Major";
                        String description = "Obscure Assert ";
                        //add design flaw to report
                        reportGenerator.addDesignFlaw(new DesignFlaw(fileName.getAbsolutePath(), methodName,
                                                                        className, linenumbers, "ObscureAssert",
                                                                        description,severity));
                    } else if (designFlawDetector.detectSuppressedException(md)) {
                        List<Integer> linenumbers = designFlawDetector.getLineNumbers();
                        String severity = "Major";
                        String description = "Suppressed Exception ";
                        //add design flaw to report
                        reportGenerator.addDesignFlaw(new DesignFlaw(fileName.getAbsolutePath(), methodName,
                                                                        className, linenumbers, "suppressedException",
                                                                        description,severity));

                    }
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
//		File outputFile = new File(taggingFolder + File.separator + Integer.toString(count) + "_" + className + "." + methodName + ".csv");
		File outputFile = new File(taggingFolder + File.separator + className + "." + methodName + ".csv");
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
