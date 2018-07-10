echo "###############################################"
echo "#              RockFS installer               #"
echo "###############################################"


#echo "Installing PVSS..."
#mvn install:install-file -Dfile=lib/pvss.jar -DgroupId=com.ufsc -DartifactId=pvss -Dversion=1.0-SNAPSHOT -Dpackaging=jar


echo "Installing JReedSolEC..."
mvn install:install-file -Dfile=lib/JavaReedSolomon.jar -DgroupId=backblaze.backblaze -DartifactId=JavaReedSolomon -Dversion=1.0-SNAPSHOT -Dpackaging=jar

echo "Installing SafeCloudFS..."
mvn compile

echo "Finished installing"
