package org.saulis;

import jetbrains.buildServer.ExtensionsProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class GerritSettingsTests {

    private GerritSettings settings;
    private HashMap<String,String> parameters;

    @Before
    public void setup() {
        ExtensionsProvider extensionsProvider = mock(ExtensionsProvider.class);

        parameters = new HashMap<String, String>();

        settings = new GerritSettings(extensionsProvider);
    }

    private String describeTrigger() {
        return settings.describeParameters(parameters);
    }

    @Test
    public void descriptionWithoutFiltersIsReturned() {
        parameters.put(Constants.GERRIT_SERVER, "gerrit.foo.bar");

        String description = describeTrigger();

        assertThat(description, is("Listening on gerrit.foo.bar"));
    }

    @Test
    public void descriptionWithProjectFilterIsReturned() {
        parameters.put(Constants.GERRIT_SERVER, "gerrit.foo.bar");
        parameters.put(Constants.GERRIT_PROJECT, "fooject");

        String description = describeTrigger();

        assertThat(description, is("Listening to fooject on gerrit.foo.bar"));
    }

    @Test
    public void descriptionWithProjectAndBranchFiltersIsReturned() {
        parameters.put(Constants.GERRIT_SERVER, "gerrit.foo.bar");
        parameters.put(Constants.GERRIT_PROJECT, "fooject");
        parameters.put(Constants.GERRIT_BRANCH, "barnch");

        String description = describeTrigger();

        assertThat(description, is("Listening to fooject/barnch on gerrit.foo.bar"));
    }
}
