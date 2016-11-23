java -XX:+UseConcMarkSweepGC  -XX:+PrintGCDetails -XX:+PrintGCTimeStamps  -Xloggc:gc.log -DENV=prod -Dhttp.port=9091 -Dmail.password=%MAILPWD% -Dlog.info=true -jar reconciler.jar
