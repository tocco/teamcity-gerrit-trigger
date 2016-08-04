package org.saulis;

import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class GerritClient {

    private static final Logger LOG = Logger.getLogger(Loggers.VCS_CATEGORY + GerritClient.class);
    private final JSch jsch;

    public GerritClient()
    {
        this.jsch = new JSch();
    }

    public List<GerritPatchSet> getNewPatchSets(GerritTriggerContext context) {
        ChannelExec channel = null;
        Session session = null;

        try {
            session = openSession(context);
            channel = openChannel(context, session);

            return readPatchSets(context, channel);
        }
        catch (Exception e) {
            LOG.error("Gerrit trigger failed while getting patch sets.", e);
        }
        finally {
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }

        return new ArrayList<GerritPatchSet>();
    }

    private Session openSession(GerritTriggerContext context) throws JSchException {

        TeamCitySshKey key = context.getSshKey();
        jsch.addIdentity(context.getUsername(), key.getPrivateKey(), null, null);

        String server = context.getServer().replace("ssh://" , "");
        int port = 29418;

        int idx = server.indexOf(":");
        if (idx != -1) {
            port = Integer.valueOf(server.substring(idx + 1, server.length()));
            server = server.substring(0, idx);
        }

        Session session = jsch.getSession(context.getUsername(), server, port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        return session;
    }

    private ChannelExec openChannel(GerritTriggerContext context, Session session) throws JSchException {
        String command = createCommand(context);

        LOG.debug("Execute Gerrit command: " + command);

        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        channel.setPty(false);
        channel.setCommand(command);
        channel.connect();

        return channel;
    }

    private String createCommand(GerritTriggerContext context) {
        StringBuilder command = new StringBuilder();
        command.append("gerrit query");

        command.append(" --current-patch-set");
        command.append(" --format=JSON status:open");

        if(context.hasProjectParameter()) {
            command.append(" project:" + context.getProjectParameter());
        }

        if(context.hasBranchParameter()) {
            command.append(" branch:" + context.getBranchParameter());
        }

        // Optimizing the query.
        // Assuming that no more than <limit> new patch sets are created during a single poll interval.
        // Adjust if needed.
        command.append(" limit:10");

        return command.toString();
    }

    private List<GerritPatchSet> readPatchSets(GerritTriggerContext context, ChannelExec channel) throws IOException {
        JsonStreamParser parser = new JsonStreamParser(new InputStreamReader(channel.getInputStream()));

        List<GerritPatchSet> patchSets = new ArrayList<GerritPatchSet>();

        if(context.hasTimestamp()) {
            Date timestamp = context.getTimestamp();

            while(parser.hasNext()) {
               JsonObject row = parser.next().getAsJsonObject();

              if(isStatsRow(row)) {
                break;
              }

              GerritPatchSet patchSet = parsePatchSet(row);

              if(patchSet.getCreatedOn().after(timestamp)) {
                patchSets.add(patchSet);
                context.updateTimestamp(patchSet.getCreatedOn());
              }
            }
        } else {
            context.updateTimestamp(new Date());
        }

        return patchSets;
    }

    private GerritPatchSet parsePatchSet(JsonObject row) {
        String project = row.get("project").getAsString();
        String branch = row.get("branch").getAsString();
        JsonObject currentPatchSet = row.get("currentPatchSet").getAsJsonObject();
        String ref = currentPatchSet.get("ref").getAsString();
        long createdOn = currentPatchSet.get("createdOn").getAsLong() * 1000L;

        return new GerritPatchSet(project, branch, ref, createdOn);
    }

    private boolean isStatsRow(JsonObject ticket) {
        return ticket.has("rowCount");
    }
}
