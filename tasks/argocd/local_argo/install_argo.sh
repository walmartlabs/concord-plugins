#!/bin/bash

set -o pipefail
set -o errexit

printHelp() {
    echo "usage: $0 [-h] [---skip-manifest-download]"
}

function join_by {
    local IFS="$1";
    shift;
    echo "$*";
}

ingressHostname="host.docker.internal"
generatedFilesDir="playbook/roles/argocd/files/generated"

while [ "$1" != "" ]; do
    case $1 in
        -h | --help )             printHelp
                                  exit
                                  ;;
        -i | --ingress-hostname ) shift
                                  ingressHostname="${1}"
                                  ;;
        -s | --skip-manifest-download )
                                  skipManifestDownload="true"
                                  ;;
        * )
            echo "Invalid parameter '$1'"
            printHelp
            exit 1
    esac
    # Shift all the parameters down by one
    shift
done

# install argocd

mkdir -p "${generatedFilesDir}"

if [ -f "${generatedFilesDir}/k3s_token.txt" ]; then
  K3S_TOKEN=$(cat "${generatedFilesDir}/k3s_token.txt")
else
  K3S_TOKEN=$(head /dev/urandom | LC_ALL=C tr -dc 'A-Za-z0-9' | head -c 32 | tee "${generatedFilesDir}/k3s_token.txt")
fi

export K3S_TOKEN
docker compose up -d

sleep 5

mkdir -p "${generatedFilesDir}"

# export server ca so ansible is less cranky
docker cp local_argo-server-1:/var/lib/rancher/k3s/server/tls/server-ca.crt "${generatedFilesDir}/k3s_server_ca.crt"

if [ -f "${generatedFilesDir}/ldap_cfg.txt" ]; then
  ldapAdminPassword=$(grep 'adminPassword:' "${generatedFilesDir}/ldap_cfg.txt" | sed 's/adminPassword://')
  ldapUsername=$(grep 'username:' "${generatedFilesDir}/ldap_cfg.txt" | sed 's/username://')
  ldapPassword=$(grep 'password:' "${generatedFilesDir}/ldap_cfg.txt" | sed 's/password://')
else
  ldapAdminPassword=$(head /dev/urandom | LC_ALL=C tr -dc 'A-Za-z0-9' | head -c 32)
  read -r -p    "Enter LDAP username: " ldapUsername
  read -r -s -p "Enter LDAP password: " ldapPassword

  echo "adminPassword:${ldapAdminPassword}" >  "${generatedFilesDir}/ldap_cfg.txt"
  echo "username:${ldapUsername}"           >> "${generatedFilesDir}/ldap_cfg.txt"
  echo "password:${ldapPassword}"           >> "${generatedFilesDir}/ldap_cfg.txt"
fi

# argocd kubernetes manifest
if [[ -f "${generatedFilesDir}/argo_install_manifest.yml" || "${skipManifestDownload}" == *"true"* ]]; then
    echo "Skipping manifest download"
else
    echo "Downloading latest ArgoCD manifest"
    curl -o "${generatedFilesDir}/argo_install_manifest.yml" https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
fi

# keypair for git in-cluster git operations
if [[ ! -f "${generatedFilesDir}/test_key" ]]
then
   ssh-keygen -t rsa -b 4096 -C "test@example.com" -m pem -N '' -f "${generatedFilesDir}/test_key"
fi

# python3 -m venv localvenv
# source localvenv/bin/activate
# pip install kubernetes
# ansible-galaxy collection install kubernetes.core
ansible-playbook -i ./playbook/inventory.yml \
  -e ingress_hostname="${ingressHostname}" \
  -e ldap_admin_password="${ldapAdminPassword}" \
  -e ldap_username="${ldapUsername}" \
  -e ldap_password="${ldapPassword}" \
  ./playbook/main.yml

cat > "${generatedFilesDir}/test_input.properties" << EOF
ARGO_IT_APP_NAMESPACE=test-namespace
ARGO_IT_BASE_API=https://${ingressHostname}
ARGO_IT_BASIC_ADMIN_PASSWORD=$(< "${generatedFilesDir}/argocd_admin_password.txt")
ARGO_IT_KUBECONFIG_PATH=local_argo/${generatedFilesDir}/kubeconfig.yaml
ARGO_IT_LDAP_USERNAME=${ldapUsername}
ARGO_IT_LDAP_PASSWORD=${ldapPassword}
EOF
