#!/usr/bin/env bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "${SCRIPT_DIR}"/../ && pwd )"
DEFAULT_DISTRO_NAME="core"
IMPOSTER_LOG_LEVEL="DEBUG"
RUN_TESTS="true"
DEBUG_MODE="true"
SUSPEND_DEBUGGER="n"
MEASURE_PERF="false"
RECURSIVE_CONFIG="false"
PORT="8080"

while getopts "m:d:c:f:l:p:rst:z:" opt; do
  case ${opt} in
    m )
      LAUNCH_MODE=$OPTARG
      ;;
    d )
      DISTRO_NAME=$OPTARG
      ;;
    c )
      CONFIG_DIR=$OPTARG
      ;;
    f )
      MEASURE_PERF=$OPTARG
      ;;
    l )
      IMPOSTER_LOG_LEVEL=$OPTARG
      ;;
    p )
      PORT=$OPTARG
      ;;
    r )
      RECURSIVE_CONFIG="true"
      ;;
    s )
      SUSPEND_DEBUGGER="y"
      ;;
    t )
      RUN_TESTS=$OPTARG
      ;;
    z )
      DEBUG_MODE=$OPTARG
      ;;
    \? )
      echo "Invalid option: $OPTARG" 1>&2
      ;;
    : )
      echo "Invalid option: $OPTARG requires an argument" 1>&2
      ;;
  esac
done
shift $((OPTIND -1))

function usage() {
  echo -e "Usage:\n  $( basename $0 ) -m <docker|java> -c config-dir [-d distro-name] [-l log-level] [-t run-tests] [-r recursive-config] [-s suspend-debugger] [-z debug-mode]"
  exit 1
}

JAVA_TOOL_OPTIONS=
if [[ "$DEBUG_MODE" == "true" ]]; then
  JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=${SUSPEND_DEBUGGER},address=8000"
fi

if [[ -z ${LAUNCH_MODE} || -z "${CONFIG_DIR}" ]]; then
  usage
else
  DISTRO_NAME="${DISTRO_NAME:-${DEFAULT_DISTRO_NAME}}"
  CONFIG_DIR="$( cd ${CONFIG_DIR} && pwd )"
fi

pushd ${ROOT_DIR}

GRADLE_ARGS=
if [[ "$RUN_TESTS" == "false" ]]; then
  GRADLE_ARGS="-xtest"
fi
# using installDist instead of dist to avoid unneeded shadow JAR for local dev
./gradlew installDist ${GRADLE_ARGS}

if [[ "true" == "${MEASURE_PERF}" ]]; then
  ./gradlew :tools:perf-monitor:shadowJar
  JAVA_TOOL_OPTIONS="-javaagent:${ROOT_DIR}/tools/perf-monitor/build/libs/imposter-perf-monitor.jar=/tmp/imposter-method-perf.csv ${JAVA_TOOL_OPTIONS}"
fi

if [[ "true" == "${RECURSIVE_CONFIG}" ]]; then
  export IMPOSTER_CONFIG_SCAN_RECURSIVE="true"
fi

# consumed below
export IMPOSTER_LOG_LEVEL
export JAVA_TOOL_OPTIONS

case ${LAUNCH_MODE} in
  docker)
    export IMAGE_DIR="${DISTRO_NAME}"
    ./scripts/docker-build.sh

    case "${DISTRO_NAME}" in
    core)
      DOCKER_IMAGE_NAME="imposter"
      ;;
    **)
      DOCKER_IMAGE_NAME="imposter-${DISTRO_NAME}"
      ;;
    esac

    docker run -ti --rm -p $PORT:8080 \
      -v "${CONFIG_DIR}":/opt/imposter/config \
      -e IMPOSTER_LOG_LEVEL \
      -e JAVA_TOOL_OPTIONS \
      "outofcoffee/${DOCKER_IMAGE_NAME}:dev"
    ;;

  java)
    cd "distro/${DISTRO_NAME}/build/install/imposter"
    "./bin/imposter" "--listenPort=${PORT}" "--configDir=${CONFIG_DIR}"
    ;;
esac
