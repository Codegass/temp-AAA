/**
 * @author chenhao
 * We don't need to share the value between java and python, 
 * so here we don't use the JPython. Here we use the apache
 * commons-exec lib.
 */
package com.envestnet.aaaplugin.util;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PythonRunner {

    private static final String PYTHON_PATH = "python"; // Or "python3" depending on environment setup

    
    /**
     * Checks if the Python version installed on the system is at least 3.6.
     *
     * @return true if the Python version is 3.6 or higher, false otherwise.
     * @throws IOException If an I/O error occurs during the version check.
     */
    public boolean isPythonVersionAtLeast36() throws IOException {
        CommandLine cmdLine = new CommandLine(PYTHON_PATH);
        cmdLine.addArgument("--version");
    
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);
        executor.setExitValues(null);  // to accept any exit value

        try {
            executor.execute(cmdLine);
            String versionOutput = outputStream.toString().trim();
            String[] parts = versionOutput.split("\\s+");
            if (parts.length > 1) {
                String version = parts[1];
                String[] versionNumbers = version.split("\\.");
                if (versionNumbers.length >= 2) {
                    int major = Integer.parseInt(versionNumbers[0]);
                    int minor = Integer.parseInt(versionNumbers[1]);
                    return major > 3 || (major == 3 && minor >= 6);
                }
            }
        } catch (ExecuteException e) {
            // Python command might not be available or there was an error executing it
            return false;
        }

        return false;
    }
    
    /**
     * Executes a given Python script.
     *
     * @param scriptPath Path to the Python script.
     * @return The output of the script.
     * @throws IOException          If an I/O error occurs.
     * @throws ExecuteException     If the script execution fails.
     */
    public String executeScript(String scriptPath) throws IOException {
        CommandLine cmdLine = new CommandLine(PYTHON_PATH);
        cmdLine.addArgument(scriptPath);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);
        executor.setExitValues(null);  // to accept any exit value

        // If you want to set a timeout, uncomment the following line
        // executor.setWatchdog(new ExecuteWatchdog(60 * 1000)); // e.g., 60 seconds

        executor.execute(cmdLine);

        return outputStream.toString().trim();
    }

    public static void main(String[] args) {
        PythonRunner runner = new PythonRunner();

        try {
            String output = runner.executeScript("path_to_your_python_script.py");
            System.out.println("Output from Python script: " + output);
        } catch (Exception e) {
            System.err.println("Error executing Python script: " + e.getMessage());
        }
    }
}

