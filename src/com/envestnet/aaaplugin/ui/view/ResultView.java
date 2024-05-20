package com.envestnet.aaaplugin.ui.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.List;

public class ResultView extends ViewPart {

    private TreeViewer viewer;
    private Text searchText;
    
    private int currentHighlightIndex = 0;
    
    private static final int ASCENDING = 0;
    private static final int DESCENDING = 1;
    

    @Override
    public void createPartControl(Composite parent) {
    	
    	GridLayout layout = new GridLayout();
        layout.numColumns = 1; // 只有一列
        parent.setLayout(layout);
        
        searchText = new Text(parent, SWT.BORDER | SWT.SEARCH);
        searchText.setMessage("Search (e.g., s=minor, type=multipleaaa, clas=xxx, method=xxx, desc=xxx, path=xxx)");
        searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    	
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        
        Tree tree = viewer.getTree();
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        String[] titles = { "Severity", "Issue Type", "Test Class", "Test Method", "Line Number", "Description", "File Path" };
        int[] widths = {100, 100, 100, 100, 70, 200, 250}; // Specify the width of each column here
        for (int i = 0; i < titles.length; i++) {
            TreeColumn column = new TreeColumn(tree, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }
        viewer.setContentProvider(new ResultContentProvider());
        viewer.setLabelProvider(new ResultLabelProvider());
        viewer.setComparator(new ResultViewComparator());
        
        // add adapter to each line
        for (TreeColumn column : viewer.getTree().getColumns()) {
            column.addSelectionListener(getSelectionAdapter(column));
        }

        // add event listener to the tree
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                handleDoubleClick(event);
            }
        });
        
        // add search listener
        searchText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                ResultFilter filter = new ResultFilter();
                filter.setSearchText(searchText.getText());
                viewer.setFilters(new ViewerFilter[] { filter });
            }
        });
    }
    
    private void handleDoubleClick(DoubleClickEvent event) {
    	System.out.println("Double click detected!");
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        Object selectedElement = selection.getFirstElement();

        // Check if the selected element is an instance of ErrorItem
        if (selectedElement instanceof ErrorItem) {
        	System.out.println("Selected item is an ErrorItem!"); // Debug output
            ErrorItem errorItem = (ErrorItem) selectedElement;
            openAndHighlightFile(errorItem.getFilePath(), errorItem.getLineNumber());
        } else {
            System.out.println("Selected item is not an ErrorItem. It is: " + selectedElement.getClass().getName()); // Debug output
        }
    }
    
    /*
     * Highlight in each sections
     */
    private void openAndHighlightFile(String filePath, String lineNumberStr) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(filePath));

        if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
            try {
                IEditorPart editorPart = IDE.openEditorOnFileStore(page, fileStore);
                if (editorPart instanceof ITextEditor) {
                    ITextEditor textEditor = (ITextEditor) editorPart;
                    IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());

                    String[] lineNumbersStr = lineNumberStr.replaceAll("[^0-9,]", "").split(",");
                    List<Integer> lineNumbers = new ArrayList<>();
                    for (String numStr : lineNumbersStr) {
                        lineNumbers.add(Integer.parseInt(numStr.trim()) - 1);  // Convert to 0-based index
                    }

                    if (currentHighlightIndex >= lineNumbers.size() - 1) {
                        currentHighlightIndex = 0;  // Reset if we've reached the end
                    }

                    int startLine = lineNumbers.get(currentHighlightIndex);
                    int endLine = lineNumbers.get(currentHighlightIndex + 1);

                    int startOffset = document.getLineOffset(startLine);
                    int endOffset = document.getLineOffset(endLine) + document.getLineLength(endLine);

                    textEditor.selectAndReveal(startOffset, endOffset - startOffset);

                    currentHighlightIndex += 2;  // Move to the next pair of lines for the next highlight

                 // Clear previous annotations
                    IAnnotationModel annotationModel = textEditor.getDocumentProvider().getAnnotationModel(textEditor.getEditorInput());
                    if (annotationModel instanceof IAnnotationModelExtension) {
                        ((IAnnotationModelExtension) annotationModel).removeAllAnnotations();
                    } else {
                        Iterator<Annotation> it = annotationModel.getAnnotationIterator();
                        while (it.hasNext()) {
                            annotationModel.removeAnnotation(it.next());
                        }
                    }

                    // Add new annotations
                    // Iterate over line numbers in pairs
                    for (int i = 0; i < lineNumbers.size(); i += 2) {
                        int startLine_anno = lineNumbers.get(i);
                        int endLine_anno = lineNumbers.get(i + 1);

                        // Add new annotations
                        for (int line = startLine_anno; line <= endLine_anno; line++) {
                            int startOffset_anno = document.getLineOffset(line);
                            int length = document.getLineLength(line);
                            Annotation annotation = new Annotation("aaaplugin.highlight.purple", false, "Custom Highlight");
                            annotationModel.addAnnotation(annotation, new Position(startOffset_anno, length));
                        }
                    }

                }
            } catch (Exception e) {
                System.out.println("Error opening and highlighting file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("File does not exist: " + filePath);
        }
    }


    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
    
    private SelectionAdapter getSelectionAdapter(final TreeColumn column) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ResultViewComparator comparator = (ResultViewComparator) viewer.getComparator();
                
                int index = 0;
                TreeColumn[] columns = viewer.getTree().getColumns();
                for (int i = 0; i < columns.length; i++) {
                    if (column == columns[i]) {
                        index = i;
                        break;
                    }
                }
                
                if (comparator.getColumn() == index) {
                    int direction = comparator.getDirection() == ASCENDING ? DESCENDING : ASCENDING;
                    comparator.setDirection(direction);
                } else {
                    comparator.setColumn(index);
                    comparator.setDirection(ASCENDING);
                }
                viewer.refresh();
            }
        };
    }

    public void updateViewFromProjects(List<String> projectPaths) {
        List<ProjectSection> input = new ArrayList<>();
        for (String projectPath : projectPaths) {
            String projectName = new File(projectPath).getName();
            ProjectSection projectSection = new ProjectSection(projectName);

            String jsonFilePath = Paths.get(projectPath, "AAA", "results.json").toString();
            File jsonFile = new File(jsonFilePath);
            if (jsonFile.exists() && jsonFile.isFile()) {
                try (FileReader reader = new FileReader(jsonFilePath)) {
                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                    // Project Stats
                    JsonObject projectStats = jsonObject.getAsJsonObject("projectStats");
                    int totalTestSuite = projectStats.get("totalTestSuite").getAsInt();
                    int totalTestCase = projectStats.get("totalTestCase").getAsInt();
                    int totalStatement = projectStats.get("totalStatement").getAsInt();
//                    statsLabel.setText("Total Test Suite: " + totalTestSuite + ", Total Test Case: " + totalTestCase + ", Total Statement: " + totalStatement);

                    // Parsing antiPatterns and designFlaw
                    for (String key : new String[]{"antiPatterns", "designFlaws"}) {
                        JsonObject category = jsonObject.getAsJsonObject(key);
                        for (var entry : category.entrySet()) {
                            JsonArray items = entry.getValue().getAsJsonArray();
                            for (JsonElement item : items) {
                                JsonObject itemObject = item.getAsJsonObject();

                                String severity = itemObject.get("severity").getAsString();
                                String path = itemObject.get("filePath").getAsString();
                                String lineNumber = itemObject.get("lineNumber").toString();
                                String description = itemObject.get("description").getAsString();
                                String issueType = itemObject.get("issueType").getAsString();
                                String testName = itemObject.get("testName").getAsString();
                                String testSuite = itemObject.get("testSuite").getAsString();

                                projectSection.addError(new ErrorItem(severity, path, lineNumber, description, issueType, testName, testSuite));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            input.add(projectSection);
        }
        viewer.setInput(input);
        viewer.expandAll();
    }
    
    private class ResultFilter extends ViewerFilter {

        private Map<String, String> searchCriteria;

        public void setSearchText(String searchText) {
            searchCriteria = parseSearchText(searchText);
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (searchCriteria == null || searchCriteria.isEmpty()) {
                return true;
            }

            if (element instanceof ErrorItem) {
                ErrorItem error = (ErrorItem) element;
                for (Map.Entry<String, String> entry : searchCriteria.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (!matches(error, key, value)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean matches(ErrorItem error, String key, String value) {
            String targetValue = null;
            switch (key) {
                case "s":
                    targetValue = error.getSeverity().toString();
                    break;
                case "type":
                    targetValue = error.getIssueType();
                    break;
                case "class":
                    targetValue = error.getTestSuite();
                    break;
                case "method":
                    targetValue = error.getTestName();
                    break;
                case "line":
                    targetValue = error.getLineNumber();
                    break;
                case "desc":
                    targetValue = error.getDescription();
                    break;
                case "path":
                    targetValue = error.getFilePath();
                    break;
            }
            if (targetValue != null) {
                return targetValue.toLowerCase().matches(".*" + Pattern.quote(value.toLowerCase()) + ".*");
            }
            return targetValue != null && targetValue.matches(".*" + value + ".*");
        }

        private Map<String, String> parseSearchText(String searchText) {
            Map<String, String> criteria = new HashMap<>();
            for (String token : searchText.split(",")) {
                String[] parts = token.trim().split("=");
                if (parts.length == 2) {
                    criteria.put(parts[0].trim(), parts[1].trim());
                }
            }
            return criteria;
        }
    }


    private class ResultContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof List) {
                return ((List<?>) inputElement).toArray();
            }
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof ProjectSection) {
                return ((ProjectSection) parentElement).getErrors().toArray();
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return element instanceof ProjectSection && !((ProjectSection) element).getErrors().isEmpty();
        }
    }

    private class ResultLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof ProjectStats) {
                ProjectStats stats = (ProjectStats) element;
                switch (columnIndex) {
                    case 0:
                        return "Project Stats";
                    case 1:
                        return stats.toString();
                }
            } else if (element instanceof ProjectSection) {
                if (columnIndex == 0) {
                    return ((ProjectSection) element).getProjectName();
                }
            } else if (element instanceof ErrorItem) {
                ErrorItem item = (ErrorItem) element;
                switch (columnIndex) {
                    case 0:
                        return item.getSeverity().toString();
                    case 1:
                        return item.getIssueType();
                    case 2:
                    	return item.getTestSuite();
                    case 3:
                    	return item.getTestName();
                    case 4:
                        return formatLineNumber(item.getLineNumber());
                    case 5:
                        return item.getDescription();
                    case 6:
                        return item.getFilePath();
                }
            }
            return null;
        }
        
        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null; // We aren't returning any images for our columns in this example.
        }
        
        private String formatLineNumber(String lineNumberStr) {
            try {
                // Parse the lineNumberStr to get a list of integers
                String[] parts = lineNumberStr.replace("[", "").replace("]", "").split(",");
                List<Integer> lineNumbers = new ArrayList<>();
                for (String part : parts) {
                    lineNumbers.add(Integer.parseInt(part.trim()));
                }

                // Ensure that the length of lineNumbers is even
                if (lineNumbers.size() % 2 != 0) {
                    throw new IllegalArgumentException("Invalid line number pairs.");
                }

                // Convert the list of integers to the desired format
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < lineNumbers.size(); i += 2) {
                    int start = lineNumbers.get(i);
                    int end = lineNumbers.get(i + 1);
                    if (start == end) {
                        formatted.append(start);
                    } else {
                        formatted.append(start).append("-").append(end);
                    }
                    if (i < lineNumbers.size() - 2) {
                        formatted.append(", ");
                    }
                }
                return formatted.toString();
            } catch (Exception e) {
                // Log the error for debugging
                System.err.println("Error formatting line numbers: " + e.getMessage());
                // Return the original string if there's an error
                return lineNumberStr;
            }
        }




    }
    
    private class ResultViewComparator extends ViewerComparator {
        private int columnIndex;
        private int direction = ASCENDING;

        public int getDirection() {
            return direction;
        }

        public void setColumn(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        public int getColumn() {
            return columnIndex;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }

        
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (!(e1 instanceof ErrorItem) || !(e2 instanceof ErrorItem)) {
                return 0;
            }
            ErrorItem error1 = (ErrorItem) e1;
            ErrorItem error2 = (ErrorItem) e2;
            int result = 0;
            switch (columnIndex) {
                case 0: // Severity column
//                    result = error1.getSeverity().compareTo(error2.getSeverity());
                	result = Integer.compare(error1.getSeverity().getOrder(), error2.getSeverity().getOrder());
                    break;
                case 1: // Issue Type column
                    result = error1.getIssueType().compareTo(error2.getIssueType());
                    break;
                case 2: // test class name colume
                	result = error1.getTestSuite().compareTo(error2.getTestSuite());
                	break;
                case 3:
                	result = error1.getTestName().compareTo(error2.getTestName());
                	break;
                case 4: // Line Number column
                    result = error1.getLineNumber().compareTo(error2.getLineNumber());
                    break;
                case 5: // Description column
                    result = error1.getDescription().compareTo(error2.getDescription());
                    break;
                case 6: // File Path column
                    result = error1.getFilePath().compareTo(error2.getFilePath());
                    break;
            }
            // If descending order, flip the direction
            if (direction == DESCENDING) {
                result = -result;
            }
            return result;
        }
    }
    

    //Data Model
    private class ProjectStats {
        private int totalTestSuite;
        private int totalTestCase;
        private int totalStatement;

        public ProjectStats(JsonObject jsonObject) {
            this.totalTestSuite = jsonObject.get("totalTestSuite").getAsInt();
            this.totalTestCase = jsonObject.get("totalTestCase").getAsInt();
            this.totalStatement = jsonObject.get("totalStatement").getAsInt();
        }

        @Override
        public String toString() {
            return "Total Test Suite: " + totalTestSuite + ", Total Test Case: " + totalTestCase + ", Total Statement: " + totalStatement;
        }
    }

    private class ProjectSection {
        private String projectName;
        private List<ErrorItem> errors = new ArrayList<>();
    
        public ProjectSection(String projectName) {
            this.projectName = projectName;
        }
    
        public void addError(ErrorItem error) {
            errors.add(error);
        }
    
        public List<ErrorItem> getErrors() {
            return errors;
        }
    
        public String getProjectName() {
            return projectName;
        }
    }    

    private class ErrorItem {
    	private Severity severity;
//        private String severity;
        private String filePath;
        private String lineNumber;
        private String description;
        private String issueType;
        private String testName;
        private String testSuite;

        public ErrorItem(String severity, String filePath, String lineNumber, String description, String issueType, String testName, String testSuite) {
//            this.severity = severity;
        	this.severity = Severity.fromString(severity);
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.description = description;
            this.issueType = issueType;	
            this.testName = testName;
            this.testSuite = testSuite;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getLineNumber() {
            return lineNumber;
        }

        public String getDescription() {
            return description;
        }

        public String getIssueType() {
            return issueType;
        }
        
        public String getTestName() {
        	return testName;
        }
        
        public String getTestSuite() {
        	return testSuite;
        }
    }
    
    public enum Severity {
        BLOCKER("Blocker", 1),
        MAJOR("Major", 2),
        MINOR("Minor", 3),
        INFO("Info", 4),
        UNKNOWN("Unknown", 5);

        private final String name;
        private final int order;

        Severity(String name, int order) {
            this.name = name;
            this.order = order;
        }

        public int getOrder() {
            return order;
        }

        public static Severity fromString(String severityString) {
            for (Severity severity : values()) {
                if (severity.name.equalsIgnoreCase(severityString)) {
                    return severity;
                }
            }
            return UNKNOWN;  // Return a default value or throw an exception if preferred
        }
   
    }

}
