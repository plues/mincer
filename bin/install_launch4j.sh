#!/bin/sh
mkdir -p "${EXT_DIR}"
wget https://sourceforge.net/projects/launch4j/files/launch4j-3/3.12/launch4j-3.12-linux.tgz
echo "9100d99f9f7f8d206e798a6f471d08557e264d31 launch4j-3.12-linux.tgz" | sha256sum -c
tar -C "${EXT_DIR}" -xvzf launch4j-3.12-linux.tgz
echo "${EXT_DIR}"
echo `ls -lah ${EXT_DIR}/launch4j/bin/`
