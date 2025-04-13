# `argocd` plugin for Concord


## Integration Tests

Integration tests run against a local ArgoCD instance. Run the [`install_argo.sh`](./local_argo/install_argo.sh)
script to set up an instance including

- Local K3s cluster in Docker
- Basic ArgoCD installation
- LDAP Dex connector
- Git repository server
- Default Repository in ArgoCD, with SSH keypair auth

### Prerequisites

- `git`
- `docker` or `rancher`
- `kubectl` (optionally `k9s`)
- `ansible` with [`kubernetes` module](https://pypi.org/project/kubernetes/)
    and [`kubernetes.core` collection](https://galaxy.ansible.com/ui/repo/published/kubernetes/core/)

### Things To Know

#### Ingress Hostname

The trickiest part is getting a hostname to work both within docker (e.g. ITs executing
through Concord testcontainers) and outside (e.g. direct JUnit ITs). The default
setup uses `host.docker.internal`. That solves the internal problem. However, this
requires some manual intervention on the host machine to resolve the hostname. This
should be as simple as adding an `/etc/hosts` entry.

```
# ...leave existing entries

# this is needed for the integration tests to work
127.0.0.1 host.docker.internal
```

Alternatively, use a hostname or IP address which is accessible on your
network (e.g. your workstation's IP address).

#### Generated Files

Some required values and files must be generated at install-time, and it's just
cleaner to keep "secrets" (event for testing) out of the repository. All of these
generated files are saved to the
[`local_argo/playbook/roles/argocd/files`](./local_argo/playbook/roles/argocd/files)
directory.

- `argo_install_manifest.yml` - ArgoCD k8ts install manifest
- `k3s_server_ca.crt` - kubernetes CA certificate
- `k3s_token.txt` - init token for k3s
- `kubeconfig.yaml` - kubeconfig for the k3s cluster
- `ldap_cfg.txt` - admin password and user ldap info
- `test_key` and `test_key.pub` - ssh keypair for git repo access
- `test_input.properties` - consolidated info for integration test input

### Install ArgoCD in K3s

Use [install_argo.sh](./local_argo/install_argo.sh) to start a `k3s` instance with
ArgoCD in the `argocd` namespace. This will also generate a `kubeconfig.yaml` which
can be used to interact with the cluster (e.g. with `kubectl` or `k9s`) for debugging.

```
$ cd local_argo
$ ./install_argo.sh --ingress-host <your-hostname-or-ip>
```

Optionally, skip downloading argocd manifest on first rut. Note: download will be
skipped if the destination file already exists.

```
$ ./install_argo.sh --skip-manifest-download
```

### Run Integration Tests

```
# run mvn from project root
$ mvn clean install --pl tasks/argocd
$ mvn verify --pl tasks/argocd -DskipArgoITs=false
...
[INFO] Results:
[INFO]
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```
