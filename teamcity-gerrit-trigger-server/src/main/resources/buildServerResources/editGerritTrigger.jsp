<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<jsp:useBean id="keys" class="org.saulis.Constants" scope="request" />

<tr>
    <td colspan="2">
        <span>Gerrit Trigger will add a new build to the queue after a new patchset is detected.</span>
    </td>
</tr>

<tr>
    <th><label for="${keys.gerritServer}">Server: <l:star/></label></th>
    <td>
        <props:textProperty name="${keys.gerritServer}" style="width:10em;"/>
        <span class="smallNote">
            Example: dev.gerrit.com<br/>
        </span>
        <span class="error" id="error_${keys.gerritServer}"></span>
    </td>
</tr>

<tr>
    <th><label for="${keys.gerritProject}">Project: <l:star/></label></th>
    <td>
        <props:textProperty name="${keys.gerritProject}" style="width:10em;"/>
        <span class="error" id="error_${keys.gerritProject}"></span>
    </td>
</tr>

<tr>
    <th><label for="${keys.gerritUsername}">Username: <l:star/></label></th>
    <td>
        <props:textProperty name="${keys.gerritUsername}" style="width:10em;"/>
        <span class="error" id="error_${keys.gerritUsername}"></span>
    </td>
</tr>

<tr>
    <th><label for="${keys.sshKey}">SSH Key: <l:star/></label></th>
    <td>
        <admin:sshKeys projectId="${projectId}"/>
        <span class="error" id="error_${keys.sshKey}"></span>
    </td>
</tr>

<tr>
    <th><label for="${keys.gerritBranch}">Branch:</label></th>
    <td>
        <props:textProperty name="${keys.gerritBranch}" style="width:10em;"/>
        <span class="error" id="error_${keys.gerritBranch}"></span>
    </td>
</tr>
