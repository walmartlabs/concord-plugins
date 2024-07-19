# Puppet Task for Concord

## Create an API Token

An API token for use with other API endpoints can be created by using the 
[PE RBAC API](https://puppet.com/docs/pe/2019.1/rbac_api_v1.html).

```yaml
- task: puppet   # create the token
  in:
    action: "createApiToken"
    username: 'puppet-username'
    password: 'password'
    lifetime: '1y'  # number followed by y (years), d (days), h (hours), m (minutes), or s (seconds)
    label: '1year token'  # max 200 characters
    description: 'created by Puppet Task for Concord'
- if: ${result.ok}
  then:
  - log: "api key is: ${result.data}"
```

## PuppetDB Query

Use PQL to query information via the [PuppetDB API](https://puppet.com/docs/puppetdb/6.4/api/index.html)

```yaml
- task: puppet   # execute it
  in:
    action: "pql"
    queryString: 'inventory{ limit 5 }'
- if: ${result.ok}
  then:
  - log: "data returned: ${result.data}"
```

## Certificate Verification

The Puppet "Master of Masters" (MoM) may use a self-signed certificate for
generating the certificates for the rest of the API endpoints. The Puppet Task
offers settings to provide the certificate which needs to be trusted.

```
# Get the public cert from the MoM
curl -k https://mom-endpoint:8140/puppet-ca/v1/certificate/ca

# output (not a real cert here)
-----BEGIN CERTIFICATE-----
MIICsDCCAZgCCQDw4hBBzMyVRzANBgkqhkiG9w0BAQsFADAaMRgwFgYDVQQDDA93
d3cuZXhhbXBsZS5jb20wHhcNMTkwNTIxMTMxMjQ3WhcNMjkwNTE4MTMxMjQ3WjAa
MRgwFgYDVQQDDA93d3cuZXhhbXBsZS5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQC9Ll8J5ravkkCIw0szg3LPH7crfHdnJ0QHPHJUCuu3+7YPfXAA
PLu59bEasI/Hfa6LiW1YTYVrhnuA82OFLmuNqhmgHIvUDNJH5Xu/scn9r7srN67Q
x0duM0XkHi5FFbYh8lgvEUXOjfFVWkNUVmQvhd6AWHjyrw1d1GEAfMS4NhBQLfov
asP3AHEHZt8JZAs5VeG3wtcwRkAiild2OTEqVtP4lhgedfR2C10lj43b7LtxnY6k
Z2h1yedFsmKsZ+tsrP2I350qf9BDmpt5rrV3qblx6MXaHTdoV1xl5bKXqWzDcXXX
cBhy0wEKIQNNX+qPtGo461oWDDbWddajPfcFAgMBAAEwDQYJKoZIhvcNAQELBQAD
ggEBAGdy6scvRQOWvSJ1gcKgIXrhgd6RbGq7ccyZusOYOvg2pKxPKDiTpaRx9zr4
HDyryfXQmQsmcahuGcO3EroQh+KPCHrMOZgUTrZEGNct6na/eCHm5rJB1uY7dkyt
a/lSBtgE/jjmsRS4vSN6DXPFmkpFGsY4gUu0v/66NaWWY+Ak6NzvXoEys4eKJ4k6
aC1fpp7rBer1wSgzFxkmnS+aPl9Yic46BLk1mPMSEn3BabnYzDjC/Q/+CTNINoR2
r2xDuuKuhiCgxevHQ48w+QoxMNgtdfaWLD+A9uV3Ds+hN2eJCh/sVzisjechX89s
xZHfg5zRgZavH0uRF/FEkjnXD1I=
-----END CERTIFICATE-----
```

Use one of three ways to provide the cert to the task.

```yaml
# Provide the cert from a Concord secret (single value, file)
- task: puppet
  in:
    certificate: # public cert for puppet endpoints
      secret: 
        org: myOrg
        name: my-secret
        password: secret-pass # or null if no password

# Provided the cert in a repo or payload file
- task: puppet
  in:
    certificate: # public cert for puppet endpoints
      # relative path in concord project files (repo)
      path: path/to/cert

# Provide the cert in-line
- task: puppet
  in:
    certificate: # public cert for puppet endpoints
      text: |
        -----BEGIN CERTIFICATE-----
        MIICsDCCAZgCCQDw4hBBzMyVRzANBgkqhkiG9w0BAQsFADAaMRgwFgYDVQQDDA93
        d3cuZXhhbXBsZS5jb20wHhcNMTkwNTIxMTMxMjQ3WhcNMjkwNTE4MTMxMjQ3WjAa
        MRgwFgYDVQQDDA93d3cuZXhhbXBsZS5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IB
        DwAwggEKAoIBAQC9Ll8J5ravkkCIw0szg3LPH7crfHdnJ0QHPHJUCuu3+7YPfXAA
        PLu59bEasI/Hfa6LiW1YTYVrhnuA82OFLmuNqhmgHIvUDNJH5Xu/scn9r7srN67Q
        x0duM0XkHi5FFbYh8lgvEUXOjfFVWkNUVmQvhd6AWHjyrw1d1GEAfMS4NhBQLfov
        asP3AHEHZt8JZAs5VeG3wtcwRkAiild2OTEqVtP4lhgedfR2C10lj43b7LtxnY6k
        Z2h1yedFsmKsZ+tsrP2I350qf9BDmpt5rrV3qblx6MXaHTdoV1xl5bKXqWzDcXXX
        cBhy0wEKIQNNX+qPtGo461oWDDbWddajPfcFAgMBAAEwDQYJKoZIhvcNAQELBQAD
        ggEBAGdy6scvRQOWvSJ1gcKgIXrhgd6RbGq7ccyZusOYOvg2pKxPKDiTpaRx9zr4
        HDyryfXQmQsmcahuGcO3EroQh+KPCHrMOZgUTrZEGNct6na/eCHm5rJB1uY7dkyt
        a/lSBtgE/jjmsRS4vSN6DXPFmkpFGsY4gUu0v/66NaWWY+Ak6NzvXoEys4eKJ4k6
        aC1fpp7rBer1wSgzFxkmnS+aPl9Yic46BLk1mPMSEn3BabnYzDjC/Q/+CTNINoR2
        r2xDuuKuhiCgxevHQ48w+QoxMNgtdfaWLD+A9uV3Ds+hN2eJCh/sVzisjechX89s
        xZHfg5zRgZavH0uRF/FEkjnXD1I=
        -----END CERTIFICATE-----
```

## Result object

Task results are returned as a Map with three entries:

- `ok`: true when task executes without error
- `data`: the data from the task
- `error`: error message when `ok` is `false`

## Error handling

Puppet Task's default behavior is to throw exceptions when they are encountered.
Exception throwing can be disabled by setting the `ignoreErrors` parameter to
`true`. If not ignoring exceptions, use a `try/error` block. Otherwise, check
the results `ok` value to see if the task was successful

```yaml
# default exception handling
- try:
  - task: puppet
    in:
      action: "pql"
      queryString: 'inventory{ limit 5 }'
  - log: "Puppet query result: ${result.data}"
  error:
    - log: "Error with task: ${result.error}"


# ignoring exceptions
- task: puppet
  in:
    action: "pql"
    ignoreErrors: true
    queryString: 'inventory{ limit 5 }'
- if: ${result.ok}
  then:
     - log: "Puppet query result: ${result.data}"
   else:
    - log: "Error with task: ${result.error}"
```

## Local Testing

[PupppetDB](https://puppet.com/docs/puppetdb/6.4/index.html) API functionality
can be tested against the OSS Puppet images via 
[pupperware](https://github.com/puppetlabs/pupperware) along with
[Concord](http://concord.walmart.com/docs/getting-started/install/docker.html).

Make sure to connect the Concord agent(s) to the pupperware network to enable
connectivity from a process.

```
docker network connect pupperware_default agent
```

## Wiremock Certificate

A certificate and private key are included in [`./wiremock_cert`](./wiremock_cert)
for unit testing https termination. The tests depend on validation of connections
to `localhost`.

To recreate the cert and private key, if needed, execute:

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
