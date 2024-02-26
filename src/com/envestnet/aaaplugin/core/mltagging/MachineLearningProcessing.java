package com.envestnet.aaaplugin.core.mltagging;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class MachineLearningProcessing {

    private String PROJECT_ROOT;
    private static final String BUNDLE_NAME = "aaaanalyzer"; // the name of the plugin bundle

    public void setupAndExecutePythonScript() {
        try {
            Bundle bundle = Platform.getBundle(BUNDLE_NAME);
            
            // locate the model directory and environment
            URL modelDirURL = FileLocator.find(bundle, new Path("model"), null);
            File modelDir = new File(FileLocator.toFileURL(modelDirURL).getPath());
            String venvDir = modelDir.getAbsolutePath() + "/venv";
            String modelPath = modelDir.getAbsolutePath() + "/xgboost.model";
            String requirementsPath = modelDir.getAbsolutePath() + "/requirements.txt";
            
            //print all these path for check
            System.out.println("modelDir: " + modelDir);
            System.out.println("venvDir: " + venvDir);
            System.out.println("modelPath: " + modelPath);
            System.out.println("requirementsPath: " + requirementsPath);
            

            // check if the virtual environment exists
            if (!new File(venvDir).exists()) {
            	// create the virtual environment
                System.err.println("Creating virtual environment");
                executeCommand("python3", "-m", "venv", venvDir);
            }
            
            // check if pip exist
            File pipFile = new File(venvDir + "/bin/pip");
            if (!pipFile.exists()) {
                // install pip
            	System.err.println("installing pip");
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

            System.out.println("pipInstallCommand: " + pipInstallCommand);
            System.out.println("pythonScriptCommand: " + pythonScriptCommand);
            
            executeCommand(pipInstallCommand);
            executeCommand(pythonScriptCommand);
        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setProjectRoot(String projectRoot) {
        this.PROJECT_ROOT = projectRoot;
    }

    public static void main(String[] args) {
        MachineLearningProcessing mlProcessing = new MachineLearningProcessing();
        mlProcessing.setupAndExecutePythonScript();
    }
}
