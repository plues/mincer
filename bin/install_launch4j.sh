#!/bin/sh
mkdir -p "${EXT_DIR}"
wget http://iweb.dl.sourceforge.net/project/launch4j/launch4j-3/3.8/launch4j-3.8-linux.tgz
echo "e0cb0240c91b3ed6ca7bb85743fd09d6d7651cc3671a5fc17f67e57b8f7a80c6  launch4j-3.8-linux.tgz" | sha256sum -c
tar -C "${EXT_DIR}" -xvzf launch4j-3.8-linux.tgz
echo "${EXT_DIR}"
echo `ls -lah ${EXT_DIR}/launch4j/bin/`
