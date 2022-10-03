#!/bin/bash

URL=$1
TMPDIR=/tmp
ANYSTYLE=anystyle

if [[ "${URL}" == "" ]]; then
    echo "$0 url"
    exit 1
fi

wget --quiet --output-document=${TMPDIR}/download.pdf ${URL}

${ANYSTYLE} -f json find ${TMPDIR}/download.pdf

rm ${TMPDIR}/download.pdf