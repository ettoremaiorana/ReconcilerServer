java -XX:+UseConcMarkSweepGC  -XX:+PrintGCDetails -XX:+PrintGCTimeStamps  -Xloggc:gc.log -Dhttp.port=9091 -Dmail.password=&MAIL_PWD -Dlog.info=true -DENV=pred -jar reconciler.jar
