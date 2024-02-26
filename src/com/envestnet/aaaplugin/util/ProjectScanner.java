package com.envestnet.aaaplugin.util;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectScanner {

    public static List<String> scanWorkspaceForProjects() {
        List<String> projectRoots = new ArrayList<>();
        
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        
        for (IProject project : projects) {
            try {
                if (project.isOpen() && project.isAccessible()) {
                    // Get the project location in the file system
                    File projectLocation = project.getLocation().toFile();
                    
                    // Check if "AAA" folder exists
                    File potentialProjectRoot = new File(projectLocation, "AAA");
                    if (potentialProjectRoot.exists() && potentialProjectRoot.isDirectory()) {
                        // if AAA folder exist, add project root to list
                        projectRoots.add(projectLocation.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return projectRoots;
    }
}

