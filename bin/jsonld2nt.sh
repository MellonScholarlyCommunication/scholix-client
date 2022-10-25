#!/bin/bash

DIRECTORY=$1

if [[ "${DIRECTORY}" == "" ]]; then
    echo "usage: $0 directory"
    exit 1
fi

find ${DIRECTORY} -type f -exec riot --output=nt {} \;