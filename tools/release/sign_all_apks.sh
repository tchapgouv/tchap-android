#!/usr/bin/env bash

set -e

# Get the command line parameters
PARAM_KEYSTORE_PATH=$1
PARAM_DIRECTORY=$2
CHECKSUM_FILE="checksum.txt"

if [[ $PARAM_KEYSTORE_PATH == ""  && $PARAM_DIRECTORY == "" ]]
then
  echo "Usage: ./sign_all_apks \$KEY \$FOLDER"
  exit 1
fi
if [[ $PARAM_KEYSTORE_PATH == "" || $PARAM_DIRECTORY == "" ]]
then
  echo "Usage: ./sign_all_apks \$KEY \$FOLDER"
  exit 1
fi

# Sign all the apks in the directory PARAM_DIRECTORY
for file in ${PARAM_DIRECTORY}/*
do
  if [[ ${file: -4} == ".apk" ]]
  then
    echo "Signing.... ${file}"
    sh ./sign_apk.sh "${PARAM_KEYSTORE_PATH}" "${file}"
  fi
done

# Rename and Hash all the apks in the directory PARAM_DIRECTORY
for file in ${PARAM_DIRECTORY}/*
do
  if [[ ${file: -4} == ".apk" ]]
  then
    # Rename Apk: remove unsigned by signed
    apkName="$(echo ${file} | sed -e 's/\-unsigned/-signed/')" ;
    mv "${file}" "${apkName}" ;

    # Hash application with SHA 256
    echo "Hash SHA 256 on file... ${apkName}"
    result="$(shasum "-a" "256" ${apkName})"

    # Save hash in file: Checksum.txt
    resultSplit=(${result})
    newName="$(echo ${resultSplit[1]} | sed 's/.*\///')"
    echo "SHA256(${newName})=${resultSplit[0]}" > ${PARAM_DIRECTORY}/${CHECKSUM_FILE}
  fi
done

echo "done !! :)"
