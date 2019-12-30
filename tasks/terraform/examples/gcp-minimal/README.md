### Getting GCP Project Credentials

Set up a service account key, which Terraform will use to create and manage
resources in your GCP project. Go to
the [create service account key page](https://console.cloud.google.com/apis/credentials/serviceaccountkey).
Select the default service account or create a new one, select JSON as the key
type, and click Create.

This downloads a JSON file with all the credentials that will be needed for
Terraform to manage the resources. This file should be located in a secure
place for production projects.

In this example credentials file is uploaded as file secret via the Concord UI.
Example file contents:
```yaml
{
    "type": "service_account",
    "project_id": "myGcpProjectID",
    "private_key_id": "myPrivateKeyID",
    "private_key": "myPrivateKey",
    "client_email": "myClientEmail",
    "client_id": "myClientID",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/{{client_email}}"
}
```

Run the example using
```
$ ./run.sh concord.example.com:8001 myOrg myProject
```