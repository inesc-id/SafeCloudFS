#!/bin/sh

echo "###############################################"
echo "#                  RockFS                     #"
echo "###############################################"

if [ -e "$1" ]; then
	mvn exec:java -Ddir=$1 -Duid=`id -u` -Dgid=`id -g`
else
	mvn exec:java -Duid=`id -u` -Dgid=`id -g`
fi


