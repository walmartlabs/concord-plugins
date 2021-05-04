# Change log

## [1.37.0] - 2021-05-04

### Added

- terraform: support for `destroy -target [name]`
([#51](https://github.com/walmartlabs/concord-plugins/pull/51));
- git: new action `getPR`
([#54](https://github.com/walmartlabs/concord-plugins/pull/54)).

### Changed

- terraform: copy the `*.auto.tfvars.json` file generated to hold
`extraVars` to the `dir` parameter's directory
([#53](https://github.com/walmartlabs/concord-plugins/pull/53)).



## [1.36.0] - 2021-03-18

### Added

- git: on the `getCommit()` method, additionally fetch
the repository's `default_branch`
([#50](https://github.com/walmartlabs/concord-plugins/pull/50)).



## [1.35.0] - 2021-03-03

### Added

- ldap: new `securityGroupTypes` parameter to filter users based
on their LDAP groups if their DN belongs to the `security` OU
([#49](https://github.com/walmartlabs/concord-plugins/pull/49)).



## [1.34.0] - 2021-02-21

### Added

- new plugin: `tasks/hashivault`
([#46](https://github.com/walmartlabs/concord-plugins/pull/46));
- git: new `createBranch` parameter - `baseRef`
([#47](https://github.com/walmartlabs/concord-plugins/pull/47));
- git: hide sensitive data in logs
([#48](https://github.com/walmartlabs/concord-plugins/pull/48)).


## [1.33.0] - 2021-02-02

### Changed

- packer: the plugin's build is disabled until it is ported
to the runtime v2;
- git: updated for `GitClient` changes in Concord 1.78.0+
([#44](https://github.com/walmartlabs/concord-plugins/pull/44)).


## [1.32.3] - 2020-11-10

### Changed

- terraform: fixed an issue when `extraVars` and `dir` are used
together.



## [1.32.2] - 2020-11-06

### Changed

- updated for `TaskResult` changes in Concord 1.71.0+;
- confluence: fix reading of page content from task input parameters.



## [1.32.1] - 2020-10-01

### Changed

- terraform: runtime-v2 compatibility for Concord 1.67.0+.



## [1.32.0] - 2020-09-15

### Added

- s3: runtime-v2 support;
- taurus: runtime-v2 support;
- msteams: runtime-v2 support;
- zoom: runtime-v2 support;
- xml: runtime-v2 support;
- ldap: runtime-v2 support;
- jsonpath: runtime-v2 support;
- jira: runtime-v2 support;
- gremlin: runtime-v2 support;
- confluence: runtime-v2 support.

### Changed

- terraform: fix incorrect value of the `hasChanges` property of
the result object (runtime-v2 version only).



## [1.31.1] - 2020-09-04

### Changed

- terraform: fix `toolUrl` usage. Now it actually uses the downloaded
binary;
- terraform: fix duplicate `Starting [ACTION]...` messages;
- terraform: Concord 1.63.0 compatibility.



## [1.31.0] - 2020-08-20

### Added

- terraform: runtime-v2 compatibility.

### Changed

- git: Concord 1.62.0+ compatibility;
- puppet: Concord 1.62.0+ compatibility.



## [1.30.0] - 2020-08-12

### Added

- terraform: an option to use a local `terraform` binary from
`$PATH`.



## [1.29.1] - 2020-08-05

### Changed

- git: updated to work with Concord 1.58.1+.



## [1.29.0] - 2020-07-23

### Added

- puppet: runtime-v2 compatibility.

### Changed

- project: add Sisu indexing to ensure runtime-v2 compatibility.



## [1.28.0] - 2020-06-04

### Added

- new plugin: `tasks/xml`.



## [1.27.1] - 2020-06-03

### Changed

- git: improved validation of input parameters.



## [1.27.0] - 2020-05-26

### Added

- git: runtime v2 compatibility;
- git: new `shallow` parameter. Enables shallow cloning;
- terraform: new action `destroy`.

### Changed

- git: use `git` CLI for cloning instead of JGit;
- msteams: pick up the `useProxy` value from default parameters.



## [1.26.0] - 2020-04-28

### Added

- new plugin: `tasks/packer`;
- github: new action `getPrList`.



## [1.25.1] - 2020-04-14

### Changed

- terraform: write `*.auto.tfvars.json` to the specified `dir` (when
provided) instead of the process' root directory.



## [1.25.0] - 2020-04-02

### Added

- msteams: `msteamsV2` task implemeted using Azure Bot service;
- git: allow custom messages for `mergePR` actions in the `github`
task.



## [1.24.0] - 2020-03-25

### Added

- s3: allow the specification of the local path for the retrieve
object. This change allows the user to specify a dst for the path
in the workspace where the retrieved object will be place, and if
not specified defaults to the object key.

### Changed

- terraform: fixed an issue preventing the `remote` backend from
working correctly. Now backend classes are responsible for providing
the necessary environment and/or configuration files.



## [1.23.2] - 2020-03-04

### Changed

- terraform: fixed the `plan` action when the `remote` backend is
used - `out` files are no longer supported by the `remote` backend.



## [1.23.1] - 2020-02-19

### Changed

- terraform: `extraVars` now saved as `*.auto.tfvars.json` files to
improve compatibility with Terraform Enterprise.



## [1.23.0] - 2020-01-25

### Added

- git: save the list of uncommitted changes as `result.changeList`
variable;

### Changed

- terraform: enable support for the `remote` backend.



## [1.22.0] - 2019-12-06

## Breaking

- msteams: the default parameters name changed from `teamsParams` to
`msteamsParams`.



## [1.21.0] - 2019-12-04

### Added

- new plugin: `tasks/msteams`.

### Changed

- ldap: automatically retry the query in case of errors;
- github: fixed a potential NPE when using the `git` task's `commit`
action and an invalid `baseBranch` value.



## [1.20.0] - 2019-10-30

### Added

- jira: new `debug` option.
- github: new actions `createRepo` and `deleteRepo`.



## [1.19.1] - 2019-10-18

### Changed

- jira: fixed an issue with the `fields` parameter not being passed
correctly.



## [1.19.0] - 2019-10-16

### Added

- jira: new action `getIssues`.



## [1.18.0] - 2019-10-14

### Added

- new plugin: `tasks/s3`;
- new plugin: `tasks/zoom`;
- gremlin: support for attacks on containers;
- jira: new action `addAttachment`;
- jira: authentication using Concord username/password secrets.



## [1.17.0] - 2019-07-27

### Added

- terraform: ability to specify a custom URL for the CLI binary;
- terraform: the CLI binary is no longer packaged into the plugin's
JAR. Instead, the new `DependencyManager` is used to download and
cache the binary. Requires Concord 1.31.0 or higher.

### Changed

- ldap: make `isMemberOf` method public again.



## [1.16.0] - 2019-08-23

### Added

- terraform: support for additional `backend` types.



## [1.15.0] - 2019-08-21

### Added

- new plugin: `tasks/confluence`;
- new plugin: `tasks/puppet`.



## [1.14.2] - 2019-08-08

### Changed

- terraform: fixed the apply's command when `saveOutput` and `dir`
options are used.



## [1.14.1] - 2019-07-31

### Changed

- terraform: the CLI version updated to `0.12.5`.

 

## [1.14.0] - 2019-07-26

### Added

- terraform: support for user supplied var files for `plan` and
`apply` actions;
- git: new `git` task action `pull`.



## [1.13.0] - 2019-07-17

### Added

- git: new `github` task action `deleteBranch`.

### Breaking

- terraform: `dirOrPlan` has been split into two separate
parameters: `dir` and `plan`. When running `apply` with a previously
created file, the `dir` must be specified in cases when the TF files
are located in a subdirectory.



## [1.12.0] - 2019-07-14

### Added

- terraform: new `action: plan` parameter - `destroy: true`. Destroys
managed infrastructure;
- new plugin: `tasks/jsonpath`;
- git: new `github` task action `deleteTag`.

### Changed

- jira: `reporter` value can now be overridden using the
`requestorUid` parameter.



## [1.11.0] - 2019-05-15

### Added

- taurus: new parameter `downloadPlugins`. The task skips downloading
JMeter plugins unless `downloadPlugins` is `true`. This allows for
completely offline work;
- git: new `github` task action `getStatuses`.



## [1.10.0] - 2019-05-09

### Added

- new plugin: `tasks/taurus`.



## [1.9.0] - 2019-04-23

### Added

- git: new GitHub action `getLatestSha`.



## [1.8.0] - 2019-04-18

### Added

- git: new GitHub actions `getBranchList`, `getTagList`.



## [1.7.1] - 2019-04-17

### Changed

- gremlin: add the `X-Gremlin-Agent` header to requests;
- ldap: support for multivalue LDAP attributes when fetching user
details from AD/LDAP.



## [1.7.0] - 2019-04-08

### Added

- github: new action `forkRepo`.



## [1.6.0] - 2019-04-03

### Added

- gremlin: support for `halt` action.



## [1.5.0] - 2019-03-29

### Added

- new plugin: `tasks/gremlin`.



## [1.4.0] - 2019-03-28

### Added

- new plugin: `tasks/jenkins`;
- terraform: support for `terraform output`.



## [1.3.0] - 2019-03-11

### Added

- terraform: Concord secrets and private key files can now be used
for GIT modules auth.

### Changed

- terraform: updated to `0.11.12`.



## [1.2.2] - 2019-03-06

### Changed

- terraform: fix the backend configuration when using a non-default
working directory.



## [1.2.1] - 2019-02-28

### Changed

- terraform: `dirOrPlan` parameter now correctly used to run
`terraform init`.



## [1.2.0] - 2019-02-27

### Added

- new plugin: `tasks/terraform`.



## [1.1.0] - 2019-02-20

### Added

- jira: new action `createSubTask`.

### Changed

- git: minor code cleanup;
- ldap: minor code cleanup.



## [1.0.0] - 2019-02-20

### Added

- git: first public release;
- jira: first public release;
- ldap: first public release.
