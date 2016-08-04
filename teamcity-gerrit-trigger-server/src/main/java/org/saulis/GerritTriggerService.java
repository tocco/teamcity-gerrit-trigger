package org.saulis;

import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class GerritTriggerService extends BuildTriggerService {

    @NotNull
    private final PluginDescriptor pluginDescriptor;
    private final GerritSettings gerritSettings;
    private GerritBuildTrigger triggerPolicy;

    public GerritTriggerService(@NotNull final PluginDescriptor pluginDescriptor,
                                @NotNull final GerritSettings gerritSettings,
                                @NotNull final GerritBuildTrigger triggerPolicy) {

        this.pluginDescriptor = pluginDescriptor;
        this.gerritSettings = gerritSettings;
        this.triggerPolicy = triggerPolicy;
    }

    @NotNull
    @Override
    public String getName() {
        return "gerritBuildTrigger";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Gerrit Build Trigger";
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return pluginDescriptor.getPluginResourcesPath("editGerritTrigger.jsp");
    }

    @NotNull
    @Override
    public String describeTrigger(@NotNull BuildTriggerDescriptor buildTriggerDescriptor) {
        Map<String,String> parameters = buildTriggerDescriptor.getParameters();
        return gerritSettings.describeParameters(parameters);
    }

    @NotNull
    @Override
    public PropertiesProcessor getTriggerPropertiesProcessor()
    {
        return gerritSettings.getParametersProcessor();
    }

    @NotNull
    @Override
    public BuildTriggeringPolicy getBuildTriggeringPolicy() {
        return triggerPolicy;
    }

    /**
     * Specifies if multiple Gerrit triggers can be defined for one build configuration.
     * @return True
     */
    @Override
    public boolean isMultipleTriggersPerBuildTypeAllowed() {
        return true;
    }
}
