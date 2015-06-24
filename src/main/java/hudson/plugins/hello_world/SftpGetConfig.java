package hudson.plugins.hello_world;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Created by Felix on 18/05/15.
 */
public class SftpGetConfig extends AbstractDescribableImpl<SftpGetConfig> {
    public final String name;
    public final String hostname;
    public final Integer port;
    private final String credentialsId;

    @DataBoundConstructor
    public SftpGetConfig(String name, String hostname, Integer port, String credentialsId) {
        this.name = name;
        this.hostname = hostname;
        this.port = port;
        this.credentialsId = credentialsId;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port;
    }

    @SuppressWarnings("unused") // by stapler
    public String getCredentialsId() {
        return credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SftpGetConfig> {

        private CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(
                CredentialsMatchers.instanceOf(StandardUsernameCredentials.class)
        );

        @Override
        public String getDisplayName() {
            return ""; //unused
        }

        /*public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String name, @QueryParameter String hostname, @QueryParameter Integer port) {

            DomainRequirement[] domainRequirements = StringUtils.isBlank(hostname)
                    ? new DomainRequirement[]{SSH_SCHEME}
                    : new DomainRequirement[]{
                    SSH_SCHEME,
                    new HostnamePortRequirement(hostname, port)
            };
            return new StandardUsernameListBoxModel().withMatching(SSHAuthenticator.matcher(ClientSession.class),
                    CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, context,
                            ACL.SYSTEM, domainRequirements));
        }*/

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String name) {
            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            result.withMatching(CREDENTIALS_MATCHER,
                    CredentialsProvider.lookupCredentials(
                            StandardUsernameCredentials.class,
                            context,
                            ACL.SYSTEM,
                            //URIRequirementBuilder.fromUri(username).build()
                            URIRequirementBuilder.create().withHostname(name).build()
                    )
            );
            return result;
        }

        /**
         * The scheme requirement.
         */
        public static final SchemeRequirement SSH_SCHEME = new SchemeRequirement("ssh");
    }
}
