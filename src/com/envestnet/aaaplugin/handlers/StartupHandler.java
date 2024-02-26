package com.envestnet.aaaplugin.handlers;

import org.eclipse.ui.IStartup;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;
import java.util.ArrayList;
import java.util.List;

import com.envestnet.aaaplugin.util.ProjectScanner;

public class StartupHandler implements IStartup {

    @Override
    public void earlyStartup() {
        List<String> projectRoots = ProjectScanner.scanWorkspaceForProjects();
        System.out.println(projectRoots);
    }

}