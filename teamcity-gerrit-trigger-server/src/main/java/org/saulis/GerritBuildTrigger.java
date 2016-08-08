package org.saulis;

import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildCustomizer;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.serverSide.SBuildType;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class GerritBuildTrigger extends PolledBuildTrigger {

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
        try {

            GerritTriggerContext context = gerritSettings.createContext(polledTriggerContext);
            List<GerritPatchSet> newPatchSets = gerritClient.getNewPatchSets(context);

            if (!newPatchSets.isEmpty()) {
                LOG.debug(String.format("Found %s new patch set(s), triggering build(s).", newPatchSets.size()));
            } else {
                LOG.debug("No new patch set(s) found.");
            }

            for(GerritPatchSet p : newPatchSets) {
                SBuildType buildType = polledTriggerContext.getBuildType();
                BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, null);
                buildCustomizer.setDesiredBranchName(p.getChangeBranch());

                buildCustomizer.createPromotion().addToQueue("Gerrit");
            }
        } catch (Exception e) {
            LOG.error("Cannot get new patch sets.", e);
        }
    }
}
