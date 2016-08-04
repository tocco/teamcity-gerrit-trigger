teamcity-gerrit-trigger
=======================

Plugin for Teamcity which polls Gerrit to trigger builds.

#### How it works

The trigger connects to Gerrit using Gerrit's SSH command line API.
It polls for new patch sets every 20 seconds (the query is limited to fetch only the last 10 patch sets).
A new build is queued for every new patch set found (new as in created after the last build was queued).

#### Usage

First of all, ensure you have `+:refs/changes/*` in your VCS root's branch specification.
After installation, add the _Gerrit Build Trigger_ into your build configuration.

Configure the build trigger:
- Host: hostname of your Gerrit instance in the form `dev.gerrit.com:29418` (you may skip the default port)
- Username: SSH username that will be used to open connection
- Private key: Select the private key to use for the SSH connection
- Project: Filter for querying patch sets (optional)
- Branch: Filter for querying patch sets (optional)

#### Local plugin build

To build the plugin locally run the
```
mvn package
```
command in the root directory.

The target directory of the project root will contain the `teamcity-gerrit-trigger.zip` file, which
is ready [to be installed](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins).
