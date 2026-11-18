#!/bin/sh
set -eu

# Derive URLs at container start so -e LLCONFIG=... takes effect.
LOGGER_ID=${LOGGER_ID:-ait.all.log4j2}
LOGGER_CONFIG="-Dlog4j.configurationFile=${LLCONFIG}${LOGGER_ID}"
CONFIG_FILE_URI="${LLCONFIG}${FMUSIM_CONFIG}"

# IPv4 configuration.
IPV4_CONFIG=-Djava.net.preferIPv4Stack=true

# JNI library fmippim_wrap_java and the SUNDIALS libraries it links.
LIBRARY_PATH_CONFIG="-Djava.library.path=/usr/local/lib"

# Optional base directory for fmusim:// URIs in the FMU configuration.
FMU_DIR_FLAG=
if [ -n "${FMU_DIR:-}" ]; then
  FMU_DIR_FLAG="-DfmuDir=${FMU_DIR}"
fi

echo LLCONFIG: $LLCONFIG
echo LOGGER_CONFIG: $LOGGER_CONFIG
echo MEMORY_FLAG: $MEMORY_FLAG
echo FMUSIM_JAR_FILE: $FMUSIM_JAR_FILE
echo FMUSIM_CLASS: $FMUSIM_CLASS
echo FMU_DIR_FLAG: $FMU_DIR_FLAG
echo CONFIG_FILE_URI: $CONFIG_FILE_URI

# Start the FMU simulator. Extra arguments are passed through.
# JDK 25 treats System.loadLibrary as restricted native access.
exec java --enable-native-access=ALL-UNNAMED $IPV4_CONFIG $LIBRARY_PATH_CONFIG $LOGGER_CONFIG $FMU_DIR_FLAG $MEMORY_FLAG -cp "$FMUSIM_JAR_FILE" "$FMUSIM_CLASS" -c "$CONFIG_FILE_URI" "$@"
