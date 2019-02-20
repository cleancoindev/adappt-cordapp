
#!/bin/bash
#
#   Copyright 2019, Dapps Incorporated.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

# Starts Adappt node. Creates node.conf if missing

set -e

echo "

    ___       __                  __
   /   | ____/ /___ _____  ____  / /_
  / /| |/ __  / __ `/ __ \/ __ \/ __/
 / ___ / /_/ / /_/ / /_/ / /_/ / /_
/_/  |_\__,_/\__,_/ .___/ .___/\__/
                 /_/   /_/

Insurance Underwriting Solution"

if [ -f ./build-info.txt ]; then
   cat build-info.txt
fi

echo
echo

printenv

# because bash and corda HOCON sometimes have a tiff about quotes!
defaulturl='"jdbc:h2:file:"${baseDirectory}"/db/persistence;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000;WRITE_DELAY=100;AUTO_SERVER_PORT="${h2port}'

# Variables used to create node.conf, defaulted if not set
ADAPPT_LEGAL_NAME=${ADAPPT_LEGAL_NAME:-O=Adappt-$(od -x /dev/urandom | head -1 | awk '{print $7$8$9}'), OU=Dapps Inc., L=San Francisco, C=US}
ADAPPT_P2P_ADDRESS=${ADAPPT_P2P_ADDRESS:-localhost:10012}
# ADAPPT_DB_DIR see below for defaulting
ADAPPT_COMPATIBILITY_ZONE_URL=${ADAPPT_COMPATIBILITY_ZONE_URL:=http://dsoa.network:8080}
ADAPPT_KEY_STORE_PASSWORD=${ADAPPT_KEY_STORE_PASSWORD:=cordacadevpass}
ADAPPT_TRUST_STORE_PASSWORD=${ADAPPT_TRUST_STORE_PASSWORD:=trustpass}
ADAPPT_DB_USER=${ADAPPT_DB_USER:=sa}
ADAPPT_DB_PASS=${ADAPPT_DB_PASS:=dbpass}
ADAPPT_DB_DRIVER=${ADAPPT_DB_DRIVER:=org.h2.jdbcx.JdbcDataSource}
ADAPPT_BRAID_PORT=${ADAPPT_BRAID_PORT:=8080}
ADAPPT_DEV_MODE=${ADAPPT_DEV_MODE:=true}
ADAPPT_DETECT_IP=${ADAPPT_DETECT_IP:=false}
ADAPPT_CACHE_NODEINFO=${ADAPPT_CACHE_NODEINFO:=false}
if [[ -z "${ADAPPT_DB_URL}" ]]; then
    echo "ADAPPT_DB_URL is not set"
    if [[ -z "${ADAPPT_DB_DIR}" ]]; then
        echo "ADAPPT_DB_DIR is not set"
        ADAPPT_DB_URL=${defaulturl}
    else
        echo "ADAPPT_DB_DIR is set to ${ADAPPT_DB_DR}"
        ADAPPT_DB_URL="\"jdbc:h2:file:${ADAPPT_DB_DIR};DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000;WRITE_DELAY=100;AUTO_SERVER_PORT=\""'${h2port}'
    fi
else
    echo "ADAPPT_DB_URL is already set to ${ADAPPT_DB_URL}"
    ADAPPT_DB_URL="\"${ADAPPT_DB_URL}\""
fi

echo "ADAPPT_DB_URL is: ${ADAPPT_DB_URL}"
# Create node.conf if it does not exist and default if variables not set
if [ ! -f ./node.conf ]; then
  echo "./node.conf not found, creating"
  basedir=\"\${baseDirectory}\"
  braidhost=${ADAPPT_LEGAL_NAME#*O=} && braidhost=${braidhost%%,*} && braidhost=$(echo $braidhost | sed 's/ //g')
cat > node.conf <<EOL
myLegalName : "${ADAPPT_LEGAL_NAME}"
p2pAddress : "${ADAPPT_P2P_ADDRESS}"
compatibilityZoneURL : "${ADAPPT_COMPATIBILITY_ZONE_URL}"
dataSourceProperties : {
    "dataSourceClassName" : "${ADAPPT_DB_DRIVER}"
    "dataSource.url" : ${ADAPPT_DB_URL}
    "dataSource.user" : "${ADAPPT_DB_USER}"
    "dataSource.password" : "${ADAPPT_DB_PASS}"
}
keyStorePassword : "${ADAPPT_KEY_STORE_PASSWORD}"
trustStorePassword : "${ADAPPT_TRUST_STORE_PASSWORD}"
devMode : ${ADAPPT_DEV_MODE}
detectPublicIp: ${ADAPPT_DETECT_IP}
jvmArgs : [ "-Dbraid.${braidhost}.port=${ADAPPT_BRAID_PORT}" ]
jarDirs=[
    ${basedir}/postgres
]
EOL
fi

echo "node.conf contents:"
cat node.conf

# Configure notaries
# for the moment we're dealing with two systems - later we can do this in a slightly different way
if [ "$ADAPPT_NOTARY" == "true" ] || [ "$ADAPPT_NOTARY" == "validating" ] || [ "$ADAPPT_NOTARY" == "non-validating" ] ; then
    NOTARY_VAL=false
    if [ "$ADAPPT_NOTARY" == "true" ] || [ "$ADAPPT_NOTARY" == "validating" ]; then
    NOTARY_VAL=true
    fi
    echo "ADAPPT_NOTARY set to ${ADAPPT_NOTARY}. Configuring node to be a notary with validating ${NOTARY_VAL}"
cat >> node.conf <<EOL
notary {
    validating=${NOTARY_VAL}
}
EOL
fi

# use cached node info if there and we want to
if [ "${ADAPPT_CACHE_NODEINFO}" = "true" ]; then
    mkdir -p config
    if [ ! -f nodeInfo* ]; then
        echo there is no node info file in the basedir
        if [ ! -f config/nodeInfo* ]; then
            echo there is also no nodeInfo file cached, creating...
            java -jar corda.jar --just-generate-node-info
            echo caching config file
            mv nodeInfo* config/
            echo is it there
            ls config
        fi
        echo copy config file back to basedir
        cp config/nodeInfo* .
        echo check dir
        ls -latr
    fi
fi

# start ADAPPT Node, if in docker container use CMD from docker to allow override
if [ -f /.dockerenv ]; then
    "$@"
else
    java -Dcapsule.jvm.args="-javaagent:./libs/jolokia-jvm-1.6.0-agent.jar=port=7005,host=localhost" -jar corda.jar --log-to-console --no-local-shell
fi


