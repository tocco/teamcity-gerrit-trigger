package org.saulis;

import jetbrains.buildServer.ssh.ServerSshKeyManager;
import org.jetbrains.annotations.NotNull;

public class Constants {

    public static final String GERRIT_SERVER = "gerritServer";
    public static final String GERRIT_PROJECT = "gerritProject";
    public static final String GERRIT_USERNAME = "gerritUsername";
    public static final String GERRIT_BRANCH = "gerritBranch";

    public static final String TIMESTAMP_KEY = "timestamp";

    @NotNull
    public String getSshKey() {
        return ServerSshKeyManager.TEAMCITY_SSH_KEY_PROP;
    }

    @NotNull
    public String getGerritServer() {
        return GERRIT_SERVER;
    }

    @NotNull
    public String getGerritProject() {
        return GERRIT_PROJECT;
    }

    @NotNull
    public String getGerritUsername() {
        return GERRIT_USERNAME;
    }

    @NotNull
    public String getGerritBranch() {
        return GERRIT_BRANCH;
    }

}
