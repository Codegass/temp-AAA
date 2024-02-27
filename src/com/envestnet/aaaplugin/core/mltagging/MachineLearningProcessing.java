package com.envestnet.aaaplugin.core.mltagging;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class MachineLearningProcessing {
	
	Bundle bundle = FrameworkUtil.getBundle(this.getClass());
	ILog log = Platform.getLog(bundle);

    private String PROJECT_ROOT;
    private static final String BUNDLE_NAME = "aaaanalyzer"; // the name of the plugin bundle

    public void setupAndExecutePythonScript() {
        try {
            Bundle bundle = Platform.getBundle(BUNDLE_NAME);
            
            // locate the model directory and environment
            URL modelDirURL = FileLocator.find(bundle, new Path("model"), null);
            File modelDir = new File(FileLocator.toFileURL(modelDirURL).getPath());
            String modelPath = modelDir.getAbsolutePath() + File.separator + "xgboost.model";
            String requirementsPath = modelDir.getAbsolutePath() + File.separator + "requirements.txt";
            
            // Create the Python Virtualenv in workspace
            String workspacePath = getWorkspacePath();
            String venvDir = workspacePath + File.separator + ".AAA" + File.separator + "venv";
            
            //print all these path for check
            log.log(new Status(Status.INFO, bundle.getSymbolicName(), "Model directory: " + modelDir));
            log.log(new Status(Status.INFO, bundle.getSymbolicName(), "Virtual environment directory: " + venvDir));
            log.log(new Status(Status.INFO, bundle.getSymbolicName(), "Model path: " + modelPath));
            log.log(new Status(Status.INFO, bundle.getSymbolicName(), "Requirements path: " + requirementsPath));

            // check if the virtual environment exists
            if (!new File(venvDir).exists()) {
                // create the virtual environment
                log.log(new Status(Status.INFO, bundle.getSymbolicName(), "Creating virtual environment"));
                executeCommand("python3", "-m", "venv", venvDir);
            }
            
            // check if pip exist
            File pipFile = new File(venvDir + File.separator +"bin" + File.separator + "pip");
            if (!pipFile.exists()) {
                // install pip
                log.log(new Status(Status.INFO, bundle.getSymbolicName(), "Installing pip"));
                executeCommand(venvDir + "/bin/python3", "-m", "ensurepip");
            }

            // install the required packages
            String[] pipInstallCommand = System.getProperty("os.name").startsWith("Windows") ?
                    new String[]{venvDir + "\\Scripts\\pip", "install", "-r", requirementsPath} :
                    new String[]{venvDir + "/bin/pip", "install", "-r", requirementsPath};

            // execute the python script
            String pythonScriptPath = modelDir.getAbsolutePath() + "/xbg.py";
            String[] pythonScriptCommand = System.getProperty("os.name").startsWith("Windows") ?
                    new String[]{venvDir + "\\Scripts\\python", pythonScriptPath, PROJECT_ROOT, modelPath} :
                    new String[]{venvDir + "/bin/python3", pythonScriptPath, PROJECT_ROOT, modelPath};

            log.log(new Status(Status.INFO, bundle.getSymbolicName(), "pipInstallCommand: " + String.join(" ", pipInstallCommand)));
            log.log(new Status(Status.INFO, bundle.getSymbolicName(), "pythonScriptCommand: " + String.join(" ", pythonScriptCommand)));
            
            executeCommand(pipInstallCommand);
            executeCommand(pythonScriptCommand);
        } catch (IOException e) {
            log.log(new Status(Status.ERROR, bundle.getSymbolicName(), "Error executing Python script", e));
        }
    }

    private void executeCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        // set the working directory to the project root
        processBuilder.directory(new File(System.getProperty("user.dir")));

        try {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                log.log(new Status(Status.INFO, bundle.getSymbolicName(), line));
            }

            int exitCode = process.waitFor();
            log.log(new Status(Status.INFO, bundle.getSymbolicName(), "Exited with error code : " + exitCode));

        } catch (IOException | InterruptedException e) {
            log.log(new Status(Status.ERROR, bundle.getSymbolicName(), "Error executing command", e));
        }
    }

    public void setProjectRoot(String projectRoot) {
        this.PROJECT_ROOT = projectRoot;
    }
    
    private String getWorkspacePath() {
        // get eclipse workspace path
        return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
    }

    public static void main(String[] args) {
        MachineLearningProcessing mlProcessing = new MachineLearningProcessing();
        mlProcessing.setupAndExecutePythonScript();
    }
}
