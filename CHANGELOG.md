# Change log

## [2.3.1] - Unreleased

### Changed

- aws: use paginator API, fix timestamp serialization
([#156](https://github.com/walmartlabs/concord-plugins/pull/156)).


## [2.3.0] - 2024-06-20

- aws: add a basic ecr describe-images wrapper
([#154](https://github.com/walmartlabs/concord-plugins/pull/154)).



## [2.2.0] - 2024-06-12

### Changed

- argocd: new params for UpdateSpec and CreateUpdate actions. fix timeout issues
([#151](https://github.com/walmartlabs/concord-plugins/pull/151)).

## [2.1.0] - 2024-05-23

### Changed

- project: rollback to client1 until wider support for 2.6.0 exists
([#146](https://github.com/walmartlabs/concord-plugins/pull/146))
- confluence: handle page IDs as long values
([#147](https://github.com/walmartlabs/concord-plugins/pull/147)).

## [2.0.0] - 2024-02-07

### Added

- argocd: Add retry logic for waitForSync operation to support for new version of argocd (2.8.0+)
([#143](https://github.com/walmartlabs/concord-plugins/pull/143)).

### Changed

- update dependencies for concord 2.x 
([#139](https://github.com/walmartlabs/concord-plugins/pull/139)).

## [1.49.0] - 2023-09-12

### Added

- argocd: update updateSpec event data, add upsert flag for create action
([#133](https://github.com/walmartlabs/concord-plugins/pull/133))
- argocd: add functionality to list appset in a project
([#136](https://github.com/walmartlabs/concord-plugins/pull/136))

## [1.48.0] - 2023-08-11

### Added
- github: allow get PR files
([#134](https://github.com/walmartlabs/concord-plugins/pull/134))

### Changed

- argocd-task: make result serializable for setparams action
([#131](https://github.com/walmartlabs/concord-plugins/pull/131))
- git: ignore empty token/password in hideSensitiveData
([#123](https://github.com/walmartlabs/concord-plugins/pull/123))
- github: allow replace existing hooks
([#124](https://github.com/walmartlabs/concord-plugins/pull/124))

## [1.47.0] - 2023-05-30

### Added

- argocd: add support for project create, delete actions
  ([#118](https://github.com/walmartlabs/concord-plugins/pull/118))
- argocd: add support for applicationSet create, delete, get actions
  ([#125](https://github.com/walmartlabs/concord-plugins/pull/125))
- argocd: optional spec option to provide additional params for app creation
  ([#128](https://github.com/walmartlabs/concord-plugins/pull/128))

### Changed

- ghaction: jdk8/11 builds removed 
  ([#121](https://github.com/walmartlabs/concord-plugins/pull/121))
- argocd: migrate to open api client
  ([#118](https://github.com/walmartlabs/concord-plugins/pull/118))
- git: jgit/git versions up
  ([#120](https://github.com/walmartlabs/concord-plugins/pull/120))

## [1.46.0] - 2022-02-23

### Added

- akeyless: add ldap auth support. fix custom apiBaseUrl parameter
([#113](https://github.com/walmartlabs/concord-plugins/pull/113));
- github: create web hook action
([#99](https://github.com/walmartlabs/concord-plugins/pull/99));
- argocd: add support for azure ad auth
([#116](https://github.com/walmartlabs/concord-plugins/pull/116));
- ldap-task: support for dns SRV resource record
([#115](https://github.com/walmartlabs/concord-plugins/pull/115)).

### Changed

- git-task: hide sensitive data for commit errors
([#111](https://github.com/walmartlabs/concord-plugins/pull/111)).

## [1.45.0] - 2022-11-01

### Added

- argocd: add support for jwt tokens 
([#102](https://github.com/walmartlabs/concord-plugins/pull/102)).

### Changed

- s3: exclude unnecessary transitive dependencies
([#101](https://github.com/walmartlabs/concord-plugins/pull/101)).
- argocd: make helm parameters as a list, instead of a class
([#108](https://github.com/walmartlabs/concord-plugins/pull/108)).

## [1.44.0] - 2022-10-10

### Added

- argocd: Plugin to perform various actions on ArgoCD
([#97](https://github.com/walmartlabs/concord-plugins/pull/97)).

### Changed

- akeyless: testcontainers task integration tests
([#96](https://github.com/walmartlabs/concord-plugins/pull/96)).

## [1.43.0] - 2022-08-31

### Changed

- akeyless: support direct usage of access token
([#94](https://github.com/walmartlabs/concord-plugins/pull/94)).

## [1.42.0] - 2022-08-26

### Added

- akeyless: akeyless-task: new plugin for Akeyless API
([#83](https://github.com/walmartlabs/concord-plugins/pull/83)).

### Changed

- github: new createIssue, getContent, getPrCommitList actions
([#78](https://github.com/walmartlabs/concord-plugins/pull/78));
- xml: remove workdir param from runtime-v2 public methods
([#75](https://github.com/walmartlabs/concord-plugins/pull/75));
- git: Fix accidental regex matching in hideSensitiveData
([#84](https://github.com/walmartlabs/concord-plugins/pull/84));
- taurus: update jmeter version to 5.5
([#85](https://github.com/walmartlabs/concord-plugins/pull/85));
- terraform: support for executing in a container
([#89](https://github.com/walmartlabs/concord-plugins/pull/89));
- hashivault: fix verifySsl option
([#74](https://github.com/walmartlabs/concord-plugins/pull/74));
- taurus: plugin removed
([#91](https://github.com/walmartlabs/concord-plugins/pull/91));
- git: log error message even if ignoreErrors specified
([#81](https://github.com/walmartlabs/concord-plugins/pull/81));
- git-task: default variables
([#76](https://github.com/walmartlabs/concord-plugins/pull/76)).

## [1.41.0] - 2021-11-29

### Changed

- github: allows to specify mergeMethod for mergePR action 
([#72](https://github.com/walmartlabs/concord-plugins/pull/72));
- terraform: convert `extraEnv` values to string. Fixes
ClassCastException when `extraEnv` contains non-string values
([#68](https://github.com/walmartlabs/concord-plugins/pull/68));
- terraform: support for Terraform binary 0.15.0+
([#73](https://github.com/walmartlabs/concord-plugins/pull/73)).



## [1.40.0] - 2021-09-23

### Added

- gremlin: support for kubernetes targets
([#65](https://github.com/walmartlabs/concord-plugins/pull/65));
- gremlin: add teamId parameter to specify team identifier
([#66](https://github.com/walmartlabs/concord-plugins/pull/66)).



## [1.39.0] - 2021-08-17

### Added

- jenkins: runtime-v2 support
([#63](https://github.com/walmartlabs/concord-plugins/pull/63));
- gremlin: allow specifying percentage of the targets
([#64](https://github.com/walmartlabs/concord-plugins/pull/64)).



## [1.38.0] - 2021-05-27

### Added

- git: add `init` action, update jgit version
([#55](https://github.com/walmartlabs/concord-plugins/pull/55));
- git: return HEAD SHA after performing action
([#56](https://github.com/walmartlabs/concord-plugins/pull/56)).

### Changed

- ldap: NPE fix
([#57](https://github.com/walmartlabs/concord-plugins/pull/57)).



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
