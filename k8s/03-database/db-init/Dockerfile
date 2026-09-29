FROM postgres:15-alpine

COPY dental-clinic-app/oris3_sql.txt /db-init/init.sql
COPY k8s/03-database/db-init/run-init.sh /usr/local/bin/run-init.sh

RUN chmod 0444 /db-init/init.sql \
    && chmod 0555 /usr/local/bin/run-init.sh

ENTRYPOINT ["/usr/local/bin/run-init.sh"]
