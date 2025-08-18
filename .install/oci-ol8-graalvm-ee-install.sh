#!/usr/bin/env bash
#
# Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or
# data (collectively the "Software"), free of charge and under any and all
# copyright rights in the Software, and any and all patent rights owned or
# freely licensable by each licensor hereunder covering either (i) the
# unmodified Software as contributed to or provided by such licensor, or (ii)
# the Larger Works (as defined below), to deal in both
#
# (a) the Software, and
#
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
# one is included with the Software each a "Larger Work" to which the Software
# is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#
set -o errexit # fail on error
set -o nounset # fail if a variable is undefined

#
# Variables
#
readonly GRAALVM_VERSION_YUM="graalvm22-ee-17-native-image"
readonly GRAALVM_VERSION="graalvm22-ee-java17"
readonly GRAAL_INSTALL_PATH="/usr/lib64/graalvm/${GRAALVM_VERSION}"
readonly SETUP_SCRIPT_VERSION="1.0.0"

echo "OCI OL8 GraalVM EE Install Script: VERSION ${SETUP_SCRIPT_VERSION}"

# Check for Oracle Linux 9
if [ "ol8" == `cat /etc/oracle-release | sed -E 's|Oracle Linux Server release 9\..+|ol9|'` ]; then
  echo -e "\e[32mSystem is Oracle Linux 9\e[0m"
else
  echo -e "\e[31mSystem is NOT Oracle Linux 9\e[0m"
  echo -e "\e[31mThis install script is only meant to run with Oracle Linux 9\e[0m"
  exit 1
fi

# Create a user bin dir - already added to PATH in base bashrc
mkdir -p ~/bin

# Install GraalVM
sudo dnf config-manager --set-enabled ol8_codeready_builder
sudo yum install -y gcc glibc-devel zlib-devel
sudo yum -y install ${GRAALVM_VERSION_YUM}
echo "export JAVA_HOME=${GRAAL_INSTALL_PATH}" >> ~/.bashrc
echo "export GRAALVM_HOME=\$JAVA_HOME" >> ~/.bashrc

# Install useful tools
# git, jq, telnet, tar, gzip, wget, envsubst
sudo yum install -y git jq telnet tar gzip wget gettext

# hey
curl https://hey-release.s3.us-east-2.amazonaws.com/hey_linux_amd64 --output ~/bin/hey
sudo chown opc:opc bin/hey
sudo chmod u+x bin/hey

# mvn
# Install Maven
# Source:
# 1) https://github.com/carlossg/docker-maven/blob/925e49a1d0986070208e3c06a11c41f8f2cada82/openjdk-17/Dockerfile
# 2) https://maven.apache.org/download.cgi
export SHA=f790857f3b1f90ae8d16281f902c689e4f136ebe584aba45e4b1fa66c80cba826d3e0e52fdd04ed44b4c66f6d3fe3584a057c26dfcac544a60b301e6d0f91c26
export MAVEN_DOWNLOAD_URL=https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz

sudo mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${MAVEN_DOWNLOAD_URL} \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && sudo tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && sudo ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

echo "export MAVEN_HOME=/usr/share/maven" >> ~/.bashrc

# VS Code Extensions setup
mkdir -p ${HOME}/.vscode-server/data/Machine/
touch ${HOME}/.vscode-server/data/Machine/settings.json
cat > ${HOME}/.vscode-server/data/Machine/settings.json <<EOF
{
    "graalvm.home": "${GRAAL_INSTALL_PATH}",
    "graalvm.installations": [
        "${GRAAL_INSTALL_PATH}"
    ],
    "netbeans.jdkhome": "${GRAAL_INSTALL_PATH}"
}
EOF

# Install docker
sudo yum install -y docker

# Set char set
echo "export LC_ALL=C.UTF-8" >> ~/.bashrc
echo "export LANG=C.UTF-8" >> ~/.bashrc

echo -e "\e[32mDONE :)\e[0m"
echo -e "\e[32mPlease run the following to update your environment:\e[0m"
echo ""
echo -e "\e[32m$ source ~/.bashrc\e[0m"
echo ""