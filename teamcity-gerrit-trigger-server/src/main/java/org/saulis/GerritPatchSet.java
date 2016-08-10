package org.saulis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GerritPatchSet {
    public static final int MAX_AGE = 30; // Trigger rounds

    private final String project;
    private final String branch;
    private final String ref;
    private final String revision;
    private final Date createdOn;

    private int age;

    public GerritPatchSet(String project, String branch, String ref, String revision, long createdOn) {
        this(project, branch, ref, revision, createdOn, 0);
    }

    private GerritPatchSet(String project, String branch, String ref, String revision, long createdOn, int age) {

        this.project = project;
        this.branch = branch;
        this.ref = ref;
        this.revision = revision;
        this.createdOn = new Date(createdOn);
        this.age = age;
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getRef() {
        return ref;
    }

    public String getRevision() {
        return revision;
    }

    public String getChangeBranch() {
        String prefix = "refs/";
        String branch = ref;
        if (branch.startsWith(prefix)) {
            return branch.substring(prefix.length());
        } else {
            return branch;
        }
    }

    public String getChangeName() {
        String prefix = "changes/";
        String branch = getChangeBranch();
        if (branch.startsWith(prefix)) {
            return branch.substring(prefix.length());
        } else {
            return branch;
        }
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public boolean isExpired() {
        return age >= MAX_AGE;
    }

    public void incrementAge() {
        age += 1;
    }

    public static List<GerritPatchSet> parseList(String json) {
        JsonStreamParser parser = new JsonStreamParser(json);
        JsonArray jsonArr = parser.next().getAsJsonArray();
        return parseList(jsonArr);
    }

    public static List<GerritPatchSet> parseList(JsonArray json) {
        List<GerritPatchSet> patches = new ArrayList<GerritPatchSet>();
        for (JsonElement el : json) {
            patches.add(parse(el.getAsJsonObject()));
        }

        return patches;
    }

    public static GerritPatchSet parse(String json) {
        JsonStreamParser parser = new JsonStreamParser(json);
        JsonObject jsonObj = parser.next().getAsJsonObject();
        return parse(jsonObj);
    }

    public static GerritPatchSet parse(JsonObject json) {
        String project = json.get("project").getAsString();
        String branch = json.get("branch").getAsString();
        JsonObject currentPatchSet = json.get("currentPatchSet").getAsJsonObject();
        String ref = currentPatchSet.get("ref").getAsString();
        String revision = currentPatchSet.get("revision").getAsString();
        long createdOn = currentPatchSet.get("createdOn").getAsLong() * 1000L;
        int tries = 0;

        if (json.get("age") != null) {
            tries = json.get("age").getAsInt();
        }

        return new GerritPatchSet(project, branch, ref, revision, createdOn, tries);
    }

    public static String toJson(List<GerritPatchSet> patches) {
        JsonArray jsonArr = new JsonArray();

        for (GerritPatchSet patch : patches) {
            jsonArr.add(patch.toJsonObject());
        }

        return jsonArr.toString();
    }

    public String toJson()
    {
        return toJsonObject().toString();
    }

    private JsonObject toJsonObject()
    {
        JsonObject patchSetObj = new JsonObject();
        patchSetObj.addProperty("ref", ref);
        patchSetObj.addProperty("revision", revision);
        patchSetObj.addProperty("createdOn", createdOn.getTime() / 1000L);

        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("age", age);
        jsonObj.addProperty("project", project);
        jsonObj.addProperty("branch", branch);
        jsonObj.add("currentPatchSet", patchSetObj);

        return jsonObj;
    }
}
