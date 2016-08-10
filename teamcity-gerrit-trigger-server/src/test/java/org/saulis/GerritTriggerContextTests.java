package org.saulis;

import jetbrains.buildServer.ExtensionsProvider;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GerritTriggerContextTests {

    private GerritTriggerContext context;
    private PolledTriggerContext triggerContext;
    private GerritSettings settings;
    private HashMap<String, String> parameters;
    private CustomDataStorage customDataStorage;
    private HashMap<String, String> storedValues;

    @Before
    public void setup() {
        BuildTriggerDescriptor triggerDescriptor = mock(BuildTriggerDescriptor.class);
        triggerContext = mock(PolledTriggerContext.class);
        settings = new GerritSettings(mock(ExtensionsProvider.class));
        customDataStorage = mock(CustomDataStorage.class);

        parameters = new HashMap<String, String>();
        storedValues = new HashMap<String, String>();

        when(triggerDescriptor.getParameters()).thenReturn(parameters);
        when(customDataStorage.getValues()).thenReturn(storedValues);
        when(triggerContext.getTriggerDescriptor()).thenReturn(triggerDescriptor);
        when(triggerContext.getCustomDataStorage()).thenReturn(customDataStorage);

        context = new GerritTriggerContext(settings, triggerContext);
    }

    private String getJsonPatchSet(int age, String change) {
        String template = "{%s\"project\":\"project\",\"branch\":\"branch\",\"currentPatchSet\":{\"ref\":\"refs/changes/%s\",\"revision\":\"%s\",\"createdOn\":1470819813}}";
        String ageTemplate = "\"age\":%s,";
        String ageString = "";

        if (age >= 0) {
            ageString = String.format(ageTemplate, age);
        }

        return String.format(template, ageString, change, change.replace("/", ""));
    }

    @Test
    public void trimmedUserNameIsFetched() {
        parameters.put(Constants.GERRIT_USERNAME, "username    ");

        assertThat(context.getUsername(), is("username"));
    }

    @Test
    public void trimmedHostIsFetched() {
        parameters.put(Constants.GERRIT_SERVER, "host    ");

        assertThat(context.getServer(), is("host"));
    }

    @Test
    public void trimmedProjectIsFetched() {
        parameters.put(Constants.GERRIT_PROJECT, "foo  ");

        assertThat(context.getProjectParameter(), is("foo"));
    }

    @Test
    public void hasProject() {
        parameters.put(Constants.GERRIT_PROJECT, "foo");

        assertTrue(context.hasProjectParameter());
    }

    @Test
    public void missingProjectIsHandled() {
        parameters.put(Constants.GERRIT_PROJECT, null);

        assertFalse(context.hasProjectParameter());
    }

    @Test
    public void emptyProjectIsHandled() {
        parameters.put(Constants.GERRIT_PROJECT, "  ");

        assertFalse(context.hasProjectParameter());
    }

    @Test
    public void trimmedBranchIsFetched() {
        parameters.put(Constants.GERRIT_BRANCH, "foo  ");

        assertThat(context.getBranchParameter(), is("foo"));
    }

    @Test
    public void hasBranchParameter() {
        parameters.put(Constants.GERRIT_BRANCH, "foo");

        assertTrue(context.hasBranchParameter());
    }

    @Test
    public void missingBranchIsHandled() {
        parameters.put(Constants.GERRIT_BRANCH, null);

        assertFalse(context.hasBranchParameter());
    }

    @Test
    public void emptyBranchIsHandled() {
        parameters.put(Constants.GERRIT_BRANCH, "  ");

        assertFalse(context.hasBranchParameter());
    }

    @Test
    public void currentTimeIsSetToTimestamp() {
        when(customDataStorage.getValues()).thenReturn(null);

        context.updateTimestamp(new Date());

        assertThat(storedValues.get("timestamp"), IsNot.not("1390482249000"));
    }

    @Test
    public void getZeroPendingPatches() {

        List<GerritPatchSet> patches = context.getPendingPatches();
        assertEquals(0, patches.size());
    }

    @Test
    public void getOnePendingPatch() {
        String json = getJsonPatchSet(-1, "12/1234");
        when(customDataStorage.getValue("patches")).thenReturn(String.format("[%s]", json));

        List<GerritPatchSet> patches = context.getPendingPatches();
        assertEquals(1, patches.size());
        assertEquals("project", patches.get(0).getProject());
    }

    @Test
    public void saveNewPatchSet() {

        GerritPatchSet patch = new GerritPatchSet("project", "branch", "refs/changes/12/1234", "1234", new Date().getTime());
        context.savePatch(patch);

        verify(customDataStorage).putValue(eq("patches"), argThat(startsWith("[{")));
    }

    @Test
    public void removePendingPatch() {

        String json1 = getJsonPatchSet(-1, "12/1234");
        String json2 = getJsonPatchSet(0, "12/12345");
        when(customDataStorage.getValue("patches")).thenReturn(String.format("[%s,%s]", json1, json2));

        GerritPatchSet patch = new GerritPatchSet("project", "branch", "refs/changes/12/1234", "1234", new Date().getTime());
        context.removePatch(patch);

        verify(customDataStorage).putValue("patches", String.format("[%s]", json2));
    }

    @Test
    public void incrementAgePatchSet() {

        String json = getJsonPatchSet(20, "12/1234");
        when(customDataStorage.getValue("patches")).thenReturn(String.format("[%s]", json));

        GerritPatchSet patch = new GerritPatchSet("project", "branch", "refs/changes/12/1234", "1234", new Date().getTime());
        context.savePatch(patch);

        verify(customDataStorage).putValue(eq("patches"), argThat(startsWith("[{\"age\":21,")));
    }

    @Test
    public void removeExpiredPatchSet() {

        String json = getJsonPatchSet(29, "12/1234");
        when(customDataStorage.getValue("patches")).thenReturn(String.format("[%s]", json));

        GerritPatchSet patch = new GerritPatchSet("project", "branch", "refs/changes/12/1234", "1234", new Date().getTime());
        context.savePatch(patch);

        verify(customDataStorage).putValue("patches", "[]");
    }
}
