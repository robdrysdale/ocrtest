#!/usr/bin/env bash

if [ ! -d target/ocr ]
then
  mkdir -p target/ocr
fi
cd target/ocr

if [ ! -f bus.3B.tar.gz ]
then
  wget https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/isri-ocr-evaluation-tools/bus.3B.tar.gz
fi

if [ ! -d bus.3B ]
then
  tar xzf bus.3B.tar.gz
fi

if [ ! -d tiffs ]
then
  mkdir tiffs
fi
rm tiffs/*

if [ ! -d pdfs ]
then
  mkdir pdfs
fi
rm pdfs/*

if [ ! -d text ]
then
  mkdir text
fi
rm text/*

ids=$(find . -name "*.tif" | cut -d "/" -f4 | cut -d "_" -f1 | sort | uniq)

for id in ${ids}
do
  echo "PDF: ${id}"
  pages=$(find . -name "${id}*.tif")
  tiffcp $pages tiffs/${id}.tif
  tiff2pdf -o pdfs/${id}.pdf tiffs/${id}.tif
done