package hudson.plugins.hello_world;

/**
 * Created by Felix on 29/04/15.
 */

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import java.io.File;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CopyToSlaveUtils {

    public static FilePath getProjectWorkspaceOnMaster(AbstractBuild build, PrintStream logger) {
        return getProjectWorkspaceOnMaster(build, build.getProject(), logger);
    }

    public static FilePath getProjectWorkspaceOnMaster(AbstractBuild build, AbstractProject project, PrintStream logger) {
        FilePath projectWorkspaceOnMaster;

        // free-style projects
        if(project instanceof FreeStyleProject) {
            FreeStyleProject freeStyleProject = (FreeStyleProject) project;

            // do we use a custom workspace?
            if(freeStyleProject.getCustomWorkspace() != null && freeStyleProject.getCustomWorkspace().length() > 0) {
                projectWorkspaceOnMaster = new FilePath(new File(freeStyleProject.getCustomWorkspace()));
            }
            else {
                projectWorkspaceOnMaster = Jenkins.getInstance().getWorkspaceFor(freeStyleProject);
            }
        }
        else {
            String pathOnMaster = Jenkins.getInstance().getWorkspaceFor((TopLevelItem)project.getRootProject()).getRemote();
            String parts[] = build.getWorkspace().getRemote().
                    split("workspace" + File.separator + project.getRootProject().getName());
            if (parts.length > 1) {
                // This happens for the non free style projects (like multi configuration projects, etc)
                // So we'll just add the extra part of the path to the workspace
                pathOnMaster += parts[1];
            }
            projectWorkspaceOnMaster = new FilePath(new File(pathOnMaster));
        }

        try {
            // create the workspace if it doesn't exist yet
            projectWorkspaceOnMaster.mkdirs();
        }
        catch (Exception e) {
            if(logger != null) {
                logger.println("An exception occured while creating " + projectWorkspaceOnMaster.getName() + ": " + e);
            }
            LOGGER.log(Level.SEVERE, "An exception occured while creating " + projectWorkspaceOnMaster.getName(), e);
        }

        return projectWorkspaceOnMaster;
    }

    private final static Logger LOGGER = Logger.getLogger(CopyToSlaveUtils.class.getName());

}