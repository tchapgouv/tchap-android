#!/usr/bin/env bash

# Copy and adaptation of ./sign_all_apks.sh, which takes 2 more params: key store pass and the path of PKCS11 config file.
# It's unsafe to use it because it takes password as parameter, so passwords will
# remain in the terminal history.

set -e

if [ "$#" -ne 2 ]
then
  echo "Usage: ./tools/release/sign_all_apks_yubi \$PKCS11_CONFIG_PATH \$FOLDER"
  exit 1
fi

# Get the command line parameters
PARAM_PKCS11_CONFIG_PATH=$1
PARAM_DIRECTORY=$2
CHECKSUM_FILE="checksums.txt"

if [ ! -f "$PARAM_PKCS11_CONFIG_PATH" ]
then
    echo "$PARAM_PKCS11_CONFIG_PATH does not exist. Please install yubico-piv-tool (doc: https://developers.yubico.com/PIV/Guides/Android_code_signing.html)"
    exit 1
fi

read -p "Please enter the artifact URL: " artifactUrl
read -s -p "Please enter your GitHub token: " gitHubToken

printf "\n================================================================================\n"
printf "Downloading the artifact...\n"

# Ignore error
set +e

python3 ./tools/release/download_github_artifacts.py \
    --token ${gitHubToken} \
    --artifactUrl ${artifactUrl} \
    --directory ${PARAM_DIRECTORY} \
    --ignoreErrors

# Do not ignore error
set -e

printf "\n================================================================================\n"
printf "Unzipping the artifact...\n"

unzip ${PARAM_DIRECTORY}/GplayTchapWithdmvoipWithpinning-release-unsigned.zip -d ${PARAM_DIRECTORY}

# Flatten folder hierarchy
mv ${PARAM_DIRECTORY}/gplayTchapWithdmvoipWithpinning/release/* ${PARAM_DIRECTORY}
rm -rf ${PARAM_DIRECTORY}/gplayTchapWithdmvoipWithpinning

printf "\n================================================================================\n"
printf "Signing the APKs...\n"

read -s -p "Enter your PIN: " pin

# Sign all the apks in the directory PARAM_DIRECTORY
for file in ${PARAM_DIRECTORY}/*.apk
do
  sh ./tools/release/sign_apk_yubi.sh "${PARAM_PKCS11_CONFIG_PATH}" "${file}" "${pin}"
done

unset pin

# Rename and Hash all the apks in the directory PARAM_DIRECTORY
for file in ${PARAM_DIRECTORY}/*.apk
do
  # Rename Apk: remove unsigned by signed
  apkName="$(echo ${file} | sed -e 's/\-unsigned/-signed/')" ;
  mv "${file}" "${apkName}" ;

  # Hash application with SHA 256
  echo "Hash SHA 256 on file... ${apkName}"
  result="$(shasum "-a" "256" ${apkName})"

  # Save hash in file: Checksum.txt
  resultSplit=(${result})
  newName="$(echo ${resultSplit[1]} | sed 's/.*\///')"
  echo "SHA256(${newName})=${resultSplit[0]}" >> ${PARAM_DIRECTORY}/${CHECKSUM_FILE}
done

echo "done !! :)"
