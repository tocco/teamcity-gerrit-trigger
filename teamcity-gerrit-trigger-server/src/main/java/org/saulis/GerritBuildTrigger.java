package org.saulis;

import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.*;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GerritBuildTrigger extends PolledBuildTrigger {

    private static final Lock lock = new ReentrantLock();
    private static final Logger LOG = Logger.getLogger(Loggers.VCS_CATEGORY + GerritBuildTrigger.class);

    private final GerritClient gerritClient;
    private final GerritSettings gerritSettings;
    private final BuildCustomizerFactory buildCustomizerFactory;

    public GerritBuildTrigger(@NotNull final GerritClient gerritClient,
                              @NotNull final GerritSettings gerritSettings,
                              @NotNull final BuildCustomizerFactory buildCustomizerFactory) {

        this.gerritClient = gerritClient;
        this.gerritSettings = gerritSettings;
        this.buildCustomizerFactory = buildCustomizerFactory;
    }

    @Override
    public void triggerBuild(@NotNull PolledTriggerContext polledTriggerContext) throws BuildTriggerException {
        if (!lock.tryLock()) {
            LOG.warn("Trigger is already running.");
            return;
        }

        try {
            GerritTriggerContext context = gerritSettings.createContext(polledTriggerContext);

            // Get patches
            List<GerritPatchSet> newPatchSets = gerritClient.getNewPatchSets(context);
            List<GerritPatchSet> pendingPatchSets = context.getPendingPatches();
            List<GerritPatchSet> patchSets = new ArrayList<GerritPatchSet>(newPatchSets);
            patchSets.addAll(pendingPatchSets);

            if (patchSets.isEmpty()) {
                LOG.debug("No new patch set(s) found.");
                return;
            }

            LOG.info(getPatchSetLogMessage(newPatchSets, pendingPatchSets));

            // Ensure you have +:refs/changes/* in your branch specification
            BuildTypeEx buildType = (BuildTypeEx)polledTriggerContext.getBuildType();
            List<BranchEx> branches = buildType.getBranches(BranchesPolicy.ALL_BRANCHES, false);

            for(GerritPatchSet p : patchSets) {

                String branchName = p.getChangeName();
                BranchEx branch = getBranch(branches, branchName);

                if (branch == null) {
                    // Wait until the branch is fetched by TeamCity
                    LOG.info(String.format("Branch %s has not been checked out yet.", branchName));
                    context.savePatch(p);
                    continue;
                }

                context.removePatch(p);

                SUser committer = null;
                SVcsModification lastModification = getChange(p.getRevision(),
                    branch.getDummyBuild().getChanges(SelectPrevBuildPolicy.SINCE_NULL_BUILD, true));

                if (lastModification != null) {
                    committer = getCommitter(lastModification);
                }

                BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, committer);

                if (lastModification != null) {
                    buildCustomizer.setChangesUpTo(lastModification);
                }

                if (committer != null) {
                    buildCustomizer.setPersonal(true);
                }

                buildCustomizer.setCleanSources(true);
                buildCustomizer.setDesiredBranchName(branchName);
                buildCustomizer.createPromotion().addToQueue("Gerrit");
            }
        } catch (Exception e) {
            LOG.error("Cannot get new patch sets.", e);
        } finally {
            lock.unlock();
        }
    }

    private SUser getCommitter(@NotNull SVcsModification change) {
        Collection<SUser> users = change.getCommitters();
        if (!users.isEmpty()) {
            return users.iterator().next();
        }

        return null;
    }

    private BranchEx getBranch(@NotNull List<BranchEx> branches, String branchName) {
        for (BranchEx branch : branches) {
            if (branch.getName().equals(branchName))
                return branch;
        }

        return null;
    }

    private SVcsModification getChange(String commitHash, List<SVcsModification> changes) {
        for (SVcsModification sVcsModification : changes) {
            if (sVcsModification.getVersion().startsWith(commitHash)) {
                return sVcsModification;
            }
        }

        return null;
    }

    private String getPatchSetLogMessage(List<GerritPatchSet> newPatchSets, List<GerritPatchSet> pendingPatchSets) {
        StringBuilder logBuilder = new StringBuilder();

        if (!newPatchSets.isEmpty()) {
            logBuilder.append(String.format("Found %s new patch set(s). ", newPatchSets.size()));
        }

        if (!pendingPatchSets.isEmpty()) {
            logBuilder.append(String.format("Found %s pending patch set(s). ", pendingPatchSets.size()));
        }

        logBuilder.append("Triggering build(s).");
        return logBuilder.toString();
    }
}
