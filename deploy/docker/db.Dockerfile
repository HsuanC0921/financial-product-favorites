FROM mysql:8.4
# 此專案只使用 MySQL Server 與 mysql 用戶端，不使用 MySQL Shell
RUN microdnf remove -y mysql-shell \
 && microdnf upgrade -y && microdnf clean all \
 && rm /usr/local/bin/gosu
# 直接以 mysql 執行，不需要 root 切換身分的 gosu
USER mysql
