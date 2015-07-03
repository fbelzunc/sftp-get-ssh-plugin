/*
 * The MIT License
 *
 * Copyright (C) 2014-2015 by Felix Belzunce Arcos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.sftp_get_ssh;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.security.ACL;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import net.sf.json.JSONObject;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Felix Belzunce Arcos
 * @version v0.1
 * @since v0.1
 */
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
                StandardUsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, build.getParent(), ACL.SYSTEM, URIRequirementBuilder.fromUri(sftpConfig.getHostname()).build()),
                        CredentialsMatchers.withId(sftpConfig.getCredentialsId()));

                PrintStream logger = listener.getLogger();
                SftpClient sftClient = new SftpClient(sftpConfig.getHostname(), sftpConfig.getPort(), sftpConfig.getCredentialsId(), sourceFile, logger);
                sftClient.fetchFile(build, listener);
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
