package hudson.plugins.hello_world;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.AbortException;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.CopyOnWriteList;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.management.Descriptor;

import net.sf.json.JSONObject;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class SftpGetBuilder extends Builder {

    private String profileName;
    private String sourceFile;

    @DataBoundConstructor
    public SftpGetBuilder(String profileName, String sourceFile) {
        this.profileName = profileName;
        this.sourceFile = sourceFile;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        List<SftpGetConfig> sftpConfigs = getDescriptor().getSftpGetConfigs();

        for(SftpGetConfig sftpConfig : sftpConfigs) {
            if(profileName.equals(sftpConfig.getName())) {
                listener.getLogger().println("[sftp-get-plugin] Downloading file from " + sftpConfig.getName());

                StandardUsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, build.getParent(), ACL.SYSTEM, URIRequirementBuilder.fromUri(sftpConfig.getHostname()).build()),
                        CredentialsMatchers.withId(sftpConfig.getCredentialsId()));


                PrintStream logger = listener.getLogger();
                SftpClient sftClient = new SftpClient(sftpConfig.getHostname(), sftpConfig.getCredentialsId(), sourceFile, logger);
                listener.getLogger().println("[sftp-get-plugin] Fetching the file from " + sftpConfig.getName());
                sftClient.fetchFile(build, launcher, listener);


            }
        }
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private volatile List<SftpGetConfig> sftpGetConfigs = new ArrayList<SftpGetConfig>();
        private volatile String sourceFile;


        public List<SftpGetConfig> getSftpGetConfigs() {
            return sftpGetConfigs;
        }

        public String getSourceFile() {
            return sourceFile;
        }

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Sftp GET";
        }

        @Override
        public boolean isApplicable(Class type) {
            return true;
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillProfileNameItems() {
            ListBoxModel listBoxModel = new ListBoxModel();
            for (SftpGetConfig sftpGetConfig : sftpGetConfigs) {
                listBoxModel.add(sftpGetConfig.getName(), sftpGetConfig.getName());
            }
            return listBoxModel;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            sftpGetConfigs = req.bindJSONToList(SftpGetConfig.class, json.get("sftpGetConfigs"));
            save();
            return true;
        }

    }
}
