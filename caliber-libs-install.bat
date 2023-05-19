:: This scripts installs all needed libraries shipped with Caliber SDK to local maven repository
mvn install:install-file -Dfile=${caliber.sdk.dir}/CaliberRMSDK115.jar -DgroupId=com.microfocus -DartifactId=CaliberRMSDK115 -Dpackaging=jar -Dversion=11.5 -DlocalRepositoryPath=${project.basedir}/caliber-libs-repo
mvn install:install-file -Dfile=${caliber.sdk.dir}/lm.jar -DgroupId=com.microfocus -DartifactId=lm -Dpackaging=jar -Dversion=11.5 -DlocalRepositoryPath=${project.basedir}/caliber-libs-repo
mvn install:install-file -Dfile=${caliber.sdk.dir}/ss.jar -DgroupId=com.microfocus -DartifactId=ss -Dpackaging=jar -Dversion=11.5 -DlocalRepositoryPath=${project.basedir}/caliber-libs-repo
mvn install:install-file -Dfile=${caliber.sdk.dir}/st-comutil.jar -DgroupId=com.microfocus -DartifactId=st-comutil -Dpackaging=jar -Dversion=11.5 -DlocalRepositoryPath=${project.basedir}/caliber-libs-repo
mvn install:install-file -Dfile=${caliber.sdk.dir}/vbjorb.jar -DgroupId=com.microfocus -DartifactId=vbjorb -Dpackaging=jar -Dversion=11.5 -DlocalRepositoryPath=${project.basedir}/caliber-libs-repo
mvn install:install-file -Dfile=${caliber.sdk.dir}/vbsec.jar -DgroupId=com.microfocus -DartifactId=vbsec -Dpackaging=jar -Dversion=11.5 -DlocalRepositoryPath=${project.basedir}/caliber-libs-repo
mvn install:install-file -Dfile=${caliber.sdk.dir}/CaliberRMSDK4COM115.jar -DgroupId=com.microfocus -DartifactId=CaliberRMSDK4COM115 -Dpackaging=jar -Dversion=11.5 -DlocalRepositoryPath=${project.basedir}/caliber-libs-repo
