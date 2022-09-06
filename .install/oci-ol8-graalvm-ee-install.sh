#!/usr/bin/env bash
echo "OCI OL8 GraalVM EE Install Script: VERSION 1.0"

# Check for Oracle Linux 8
if [ "ol8" == `cat /etc/oracle-release | sed -E 's|Oracle Linux Server release 8\..+|ol8|'` ]; then
  echo -e "\e[32mSystem is Oracle Linux 8\e[0m"
else
  echo -e "\e[31mSystem is NOT Oracle Linux 8\e[0m"
  echo -e "\e[31mThis install script is only meant to run with Oracle Linnux 8\e[0m"
  exit 1
fi

#
# Variables
#
GRAALVM_VERSION_YUM="graalvm22-ee-17-native-image"
GRAALVM_VERSION="graalvm22-ee-java17"
GRAAL_INSTALL_PATH="/usr/lib64/graalvm/${GRAALVM_VERSION}"

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