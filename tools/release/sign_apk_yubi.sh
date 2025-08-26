#!/usr/bin/env bash

# Copy and adaptation of ./sign_apk.sh, which takes 2 more params: key store pass and the path of PKCS11 config file.
# It's unsafe to use it because it takes password as parameter, so passwords will
# remain in the terminal history.

set -e

if [[ -z "${ANDROID_HOME}" ]]; then
    echo "Env variable ANDROID_HOME is not set, should be set to something like ~/Library/Android/sdk"
    exit 1
fi

if [[ "$#" -ne 3 ]]; then
  echo "Usage: $0 PKCS11_CONFIG_PATH APK KS_PASS" >&2
  exit 1
fi

# Get the command line parameters
PARAM_PKCS11_CONFIG_PATH=$1
PARAM_APK=$2
PARAM_KS_PASS=$3

# Other params
BUILD_TOOLS_VERSION="36.0.0"
MIN_SDK_VERSION=21
BUILD_TOOLS_PATH=${ANDROID_HOME}/build-tools/${BUILD_TOOLS_VERSION}

if [[ ! -d ${BUILD_TOOLS_PATH} ]]; then
    printf "Fatal: ${BUILD_TOOLS_PATH} folder not found, ensure that you have installed the SDK version ${BUILD_TOOLS_VERSION}.\n"
    exit 1
fi

echo "\n\nSigning ${PARAM_APK} with build-tools version ${BUILD_TOOLS_VERSION} for min SDK version ${MIN_SDK_VERSION}..."

${BUILD_TOOLS_PATH}/apksigner -J-add-exports"=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED" sign \
    -v \
    --ks NONE \
    --ks-pass "pass:${PARAM_KS_PASS}" \
    --ks-type PKCS11 \
    --ks-key-alias "X.509 Certificate for PIV Authentication" \
    --provider-class sun.security.pkcs11.SunPKCS11 \
    --provider-arg ${PARAM_PKCS11_CONFIG_PATH} \
    --min-sdk-version ${MIN_SDK_VERSION} \
    ${PARAM_APK}

# Verify the signature
echo "\nVerifying the signature..."

# Note: we ignore warning on META-INF files
${BUILD_TOOLS_PATH}/apksigner verify \
    -v \
    --min-sdk-version ${MIN_SDK_VERSION} \
    ${PARAM_APK} \
    | grep -v "WARNING: META-INF/"

echo "\nPackage info..."
${BUILD_TOOLS_PATH}/aapt dump badging ${PARAM_APK} | grep package

echo "\nCongratulations! The APK ${PARAM_APK} is now signed!\n"
