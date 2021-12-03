# ldap-task

Execute LDAP queries from Concord workflows

## ITs Set Up

The project's integration tests rely on an
[openldap](https://github.com/osixia/docker-openldap) container which is set up
via [testcontainers](https://www.testcontainers.org/).

### Requirements

- maven
- docker

### Run ITs

```shell
./mvnw clean verify --pl tasks/ldap

# or, DON'T run the tests
./mvnw clean verify -DskipLdapITs=true --pl tasks/ldap
```

### Self-signed cert for TLS testing

The integration tests use self-signed certificates to validate functionality.
They're included as resource files in the project. If, for some reason, they
need to be replaced then here's how they were originally created.

```shell
# generate the private key to become a local CA
$ openssl genrsa -des3 -out ca.key 2048

# generate CA root cert
$ openssl req -x509 -new -nodes -key ca.key -sha256 -days 99999 -out ca.pem

# generate private key for server certificate
$ openssl genrsa -out server.key 2048

# create CSR
$ openssl req -new -key server.key -out server.csr

# generate server cert
# cert.ext is also a resource in the project
$ openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key \
    -CAcreateserial -out server.crt -days 99999 -sha256 -extfile cert.ext
```

The above commands can be run in the `test/resources` directory or elsewhere
and then just copy over the `ca.pem`, `server.crt`, and `server.key`.
