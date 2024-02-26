package com.envestnet.aaaplugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    // plugin shared instance
    private static Activator plugin;

    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        plugin = this;
        System.out.println("AAA Plugin started");

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        System.out.println("AAA Plugin stopped");
    }

    /**
     * returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }
}
