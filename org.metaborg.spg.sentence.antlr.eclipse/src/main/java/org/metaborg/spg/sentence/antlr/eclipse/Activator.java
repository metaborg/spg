package org.metaborg.spg.sentence.antlr.eclipse;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "spg.sentence.antlr";
    private static Activator plugin;

    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);

        plugin = this;
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;

        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public static void logError(String message, Throwable exception) {
        plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }

    public static void logError(String message) {
        plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    public static void logWarn(String message) {
        plugin.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

    public static void logInfo(String message) {
        plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }
}
