package org.saulis;

import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class GerritTriggerServiceTests {

    private GerritTriggerService service;
    private BuildTriggeringPolicy buildTrigger;

    @Before
    public void setup() {
        GerritSettings settings = mock(GerritSettings.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        GerritBuildTrigger trigger = mock(GerritBuildTrigger.class);

        buildTrigger = trigger;

        service = new GerritTriggerService(pluginDescriptor, settings, trigger);
    }

    @Test
    public void sameTriggerPolicyInstanceIsReturned() {
        BuildTriggeringPolicy actual = service.getBuildTriggeringPolicy();
        assertThat(actual, is(buildTrigger));
    }
}
