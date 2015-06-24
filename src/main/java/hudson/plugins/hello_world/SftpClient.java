package hudson.plugins.hello_world;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.JSchConnector;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by Felix on 29/04/15.
 */
public class SftpClient {

    JSch jsch;
    Session session;
    String hostname;
    Integer port;
    String credentialId;
    String sourceFile;
    PrintStream logger;

    public static final List<DomainRequirement> NO_REQUIREMENTS = Collections.<DomainRequirement> emptyList();

    public SftpClient(String hostname,String credentialId, String sourceFile, PrintStream logger) {
        this.hostname = hostname;
        this.credentialId = credentialId;
        this.sourceFile =  sourceFile;
        this.logger = logger;
        this.port = 2222;
        //jsch = new JSch();

        try {
            session = createSession(logger);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Session createSession(final PrintStream logger) throws JSchException, IOException, InterruptedException {
        StandardUsernameCredentials user = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, (Item) null, ACL.SYSTEM, NO_REQUIREMENTS),
                CredentialsMatchers.withId(credentialId));

        if (user == null) {
            String message = "Credentials with id '" + credentialId + "', no longer exist!";
            throw new InterruptedException(message);
        }

        final JSchConnector connector = new JSchConnector(user.getUsername(), hostname, port);

        final SSHAuthenticator<JSchConnector, StandardUsernameCredentials> authenticator = SSHAuthenticator
                .newInstance(connector, user);
        authenticator.authenticate(new StreamTaskListener(logger, Charset.defaultCharset()));

        final Session session = connector.getSession();

        session.setServerAliveInterval(20);

        final Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(100);

        return session;
    }



    public void fetchFile(AbstractBuild build, final Launcher launcher, BuildListener listener) {
        try {
            //session = jsch.getSession(username, hostname, 2222);
            //session.setConfig("StrictHostKeyChecking", "no");
            //session.setPassword(password.getPlainText());
            //session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            System.out.println("pwd " + sftpChannel.pwd());
            FilePath rootFilePathOnMaster;
            rootFilePathOnMaster = CopyToSlaveUtils.getProjectWorkspaceOnMaster(build, listener.getLogger());
            System.out.println("workspace " + rootFilePathOnMaster.toString());
            sftpChannel.get(sourceFile, rootFilePathOnMaster.toString());
            sftpChannel.exit();
            session.disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    public static final Logger LOGGER = Logger.getLogger(SftpClient.class.getName());
}
