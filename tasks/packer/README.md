# Packer Plugin

This module is archived and intentionally not part of the root reactor.

The original implementation depends on `ca.vanzyl.concord.plugins` helper artifacts that are not available from the configured Maven repositories. The module POM is kept buildable so repository-wide tooling can inspect it, but it does not currently produce a plugin JAR.

Before restoring this plugin to releases, replace or restore the legacy tool-support dependency and re-enable compilation/tests.
