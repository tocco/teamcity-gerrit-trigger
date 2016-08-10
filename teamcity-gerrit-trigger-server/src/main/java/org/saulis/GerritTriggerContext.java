package org.saulis;

import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GerritTriggerContext {

    private final PolledTriggerContext context;
    private final GerritSettings settings;
    private final Map<String, String> parameters;

    public GerritTriggerContext(@NotNull final GerritSettings gerritSettings,
                                @NotNull final PolledTriggerContext polledTriggerContext) {

        this.settings = gerritSettings;
        this.context = polledTriggerContext;
        this.parameters = context.getTriggerDescriptor().getParameters();
    }

    @NotNull
    public String getUsername() {
        return settings.getTrimmedParameter(parameters, Constants.GERRIT_USERNAME);
    }

    @NotNull
    public String getServer() {
        return settings.getTrimmedParameter(parameters, Constants.GERRIT_SERVER);
    }

    @Nullable
    public TeamCitySshKey getSshKey() {
        return settings.getSshKey(context.getBuildType().getProject(), parameters);
    }

    public boolean hasProjectParameter() {
        return getProjectParameter().length() > 0;
    }

    @Nullable
    public String getProjectParameter() {
        return settings.getTrimmedParameter(parameters, Constants.GERRIT_PROJECT);
    }

    public boolean hasBranchParameter() {
        return getBranchParameter().length() > 0;
    }

    @Nullable
    public String getBranchParameter() {
        return settings.getTrimmedParameter(parameters, Constants.GERRIT_BRANCH);
    }

    public void updateTimestamp(Date timestamp) {

        Date previousTimestamp = getTimestamp();

        if(previousTimestamp != null) {
            if(timestamp.after(previousTimestamp)) {
                setTimestamp(timestamp);
            }
        } else {
            setTimestamp(timestamp);
        }
    }

    public boolean hasTimestamp() {
        return getTimestamp() != null;
    }

    public Date getTimestamp() {
        String value = getStoredValue(Constants.TIMESTAMP_KEY);
        return value == null ? null : new Date(Long.parseLong(value));
    }

    private void setTimestamp(Date timestamp) {
        setStoredValue(Constants.TIMESTAMP_KEY, String.valueOf(timestamp.getTime()));
    }

    public void savePatch(GerritPatchSet patchSet) {
        GerritPatchSet patch = null;
        List<GerritPatchSet> patches = getPendingPatches();

        // Find existing patch
        for (GerritPatchSet p : patches) {
            if (p.getRef().equals(patchSet.getRef())) {
                patch = p;
            }
        }

        if (patch != null) {
            patch.incrementAge();
        } else {
            patches.add(patchSet);
        }

        savePendingPatches(patches);
    }

    public void removePatch(GerritPatchSet patchSet) {
        GerritPatchSet patch = null;
        List<GerritPatchSet> patches = getPendingPatches();

        // Find patch
        for (GerritPatchSet p : patches) {
            if (p.getRef().equals(patchSet.getRef())) {
                patch = p;
            }
        }

        if (patch != null) {
            patches.remove(patch);
            savePendingPatches(patches);
        }
    }

    private void savePendingPatches(List<GerritPatchSet> pendingPatches) {
        List<GerritPatchSet> patches = new ArrayList<GerritPatchSet>(pendingPatches);

        // Remove expired patches
        for (GerritPatchSet patch : pendingPatches) {
            if (patch.isExpired()) {
                patches.remove(patch);
            }
        }

        String json = GerritPatchSet.toJson(patches);
        setStoredValue(Constants.PATCHES_KEY, json);
    }

    public List<GerritPatchSet> getPendingPatches() {
        String json = getStoredValue(Constants.PATCHES_KEY);
        if (json == null)
            return new ArrayList<GerritPatchSet>();

        return GerritPatchSet.parseList(json);
    }

    private String getStoredValue(String key) {
        return context.getCustomDataStorage().getValue(key);
    }

    private void setStoredValue(String key, String value) {
        context.getCustomDataStorage().putValue(key, value);
    }
}
