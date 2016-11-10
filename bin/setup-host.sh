#!/bin/bash

if [[ "$(id -u)" != "0" ]]; then
  echo "This script must be run as root" 1>&2
  exit 1
fi

usage="usage: setup-host.sh /path/to/mysql/binary"

if [[ -z "$1" ]]
then
  echo ${usage}
  exit 1
fi

binary=$1

if [[ ! -f ${binary} ]]
then
  echo "${binary} does not exist."
  echo ${usage}
  exit 1
fi

base_dir=/opt/mysql-rule
binary_dir=/opt/mysql-rule/binary
template_dir=/opt/mysql-rule/template
template_data_dir=${template_dir}/data
mysqld=${binary_dir}/bin/mysqld

echo "Creating directories: ${base_dir} ${template_dir} ..."
mkdir -p ${template_data_dir}

pushd .

cd ${base_dir}

echo "Unpacking binary to ${base_dir} ..."
tar -xf ${binary}

echo "Linking binary to ${binary_dir} ..."
ln -s mysql-* ${binary_dir}

echo "Initializing mysql template in: ${template_dir} ..."
${mysqld} --initialize-insecure --basedir=${template_dir} \
 --datadir=${template_data_dir} --default-time-zone='+00:00'
chmod -R 555 ${binary_dir}
chmod -R 777 ${template_dir}

popd
