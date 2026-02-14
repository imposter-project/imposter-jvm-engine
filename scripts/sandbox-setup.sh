#!/bin/bash
set -e

# Only run in remote environments
if [ "$CLAUDE_CODE_REMOTE" != "true" ]; then
  exit 0
fi

echo "Setting up sandbox..."

if [ -z "${https_proxy}" ]; then
  echo "No https_proxy environment variable to parse"
  exit 1
fi

USER_GRADLE_DIR="${HOME}/.gradle"
mkdir -p "$USER_GRADLE_DIR"

PROXY_HOST=$(echo "$https_proxy" | sed 's|.*@||' | sed 's|:.*||')
PROXY_PORT=$(echo "$https_proxy" | sed 's|.*:||')
PROXY_USER=$(echo "$https_proxy" | sed 's|http://||' | sed 's|@.*||' | sed 's|:.*||')
PROXY_PASS=$(echo "$https_proxy" | sed 's|http://||' | sed 's|@.*||' | sed 's|^[^:]*:||')

cat > "${USER_GRADLE_DIR}/gradle.properties" << PROPEOF
systemProp.http.proxyHost=$PROXY_HOST
systemProp.http.proxyPort=$PROXY_PORT
systemProp.http.proxyUser=$PROXY_USER
systemProp.http.proxyPassword=$PROXY_PASS
systemProp.https.proxyHost=$PROXY_HOST
systemProp.https.proxyPort=$PROXY_PORT
systemProp.https.proxyUser=$PROXY_USER
systemProp.https.proxyPassword=$PROXY_PASS
systemProp.jdk.http.auth.tunneling.disabledSchemes=
systemProp.jdk.http.auth.proxying.disabledSchemes=
PROPEOF

echo "Sandbox setup complete"
