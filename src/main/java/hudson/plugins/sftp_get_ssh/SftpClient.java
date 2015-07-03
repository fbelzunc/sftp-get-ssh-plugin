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

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Felix Belzunce Arcos
 * @version v0.1
 * @since v0.1
 */
public class SftpClient {
    Session session;
    String hostname;
    Integer port;
    String credentialId;
    String sourceFile;
    PrintStream logger;

    public static final List<DomainRequirement> NO_REQUIREMENTS = Collections.<DomainRequirement> emptyList();

    public SftpClient(String hostname, Integer port, String credentialId, String sourceFile, PrintStream logger) {
        this.hostname = hostname;
        this.port = port;
        this.credentialId = credentialId;
        this.sourceFile =  sourceFile;
        this.logger = logger;

        try {
            session = createSession(logger);
        } catch (Exception e) {
            logger.println("An exception occured while creating JSch session " + ": " + e);
        }
    }

    private Session createSession(final PrintStream logger) throws JSchException, IOException, InterruptedException {
        JSch jsch = new JSch();

        StandardCredentials user = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM, NO_REQUIREMENTS),
                CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialId),
                        CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardCredentials.class),
                                CredentialsMatchers.instanceOf(SSHUserPrivateKey.class))));

        if (user == null) {
            String message = "Credentials with id '" + credentialId + "', no longer exist!";
            throw new InterruptedException(message);
        }

        if(user instanceof SSHUserPrivateKey) {
            LOGGER.log(Level.FINER, "SSHUserPrivateKey used through credentialID {0}", credentialId);
            SSHUserPrivateKey userSsh = (SSHUserPrivateKey) user;
            jsch.addIdentity(userSsh.getUsername(), userSsh.getPrivateKey().getBytes("UTF-8"), null, null);
            session = jsch.getSession(userSsh.getUsername(), hostname, port);
        } else if (user instanceof StandardUsernamePasswordCredentials) {
            LOGGER.log(Level.FINER, "StandardUsernamePasswordCredentials used through credentialID {0}", credentialId);
            StandardUsernamePasswordCredentials userNamePassword = (StandardUsernamePasswordCredentials) user;
            session = jsch.getSession(userNamePassword.getUsername(), hostname, port);
            session.setPassword(userNamePassword.getPassword().getPlainText());
        }

        session.setServerAliveInterval(20);

        final Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        logger.println("[sftp-get-plugin] Connecting to the server: " + hostname);
        session.connect(100);

        return session;
    }

    public void fetchFile(AbstractBuild build, BuildListener listener) {
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            FilePath rootFilePathOnMaster;
            rootFilePathOnMaster = CopyToSlaveUtils.getProjectWorkspaceOnMaster(build, listener.getLogger());

            logger.println("[sftp-get-plugin] Fetching file: " + sourceFile);
            sftpChannel.get(sourceFile, rootFilePathOnMaster.toString());
            logger.println("[sftp-get-plugin] File: " + sourceFile + " saved in Workspace");
            sftpChannel.exit();
            session.disconnect();
        } catch (Exception e) {
            logger.println("An exception occured while fetching file: " + sourceFile + ": " + e);
        }
    }

    public static final Logger LOGGER = Logger.getLogger(SftpClient.class.getName());
}
