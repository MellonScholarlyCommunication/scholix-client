#!/bin/bash

BASE=https://zenodo.org/record/6338616

for ((i = 2; i <= 21; i++)) ; do
    URL="${BASE}/files/scholix_dump_${i}.tar?download=1"
    echo $URL
    wget --output-document=scholix_dump_${i}.tar ${URL}
done