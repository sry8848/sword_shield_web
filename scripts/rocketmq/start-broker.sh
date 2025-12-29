#!/usr/bin/env bash
set -e

: "${ROCKETMQ_NAME_SERVER:=rocketmq-namesrv:9876}"
: "${ROCKETMQ_BROKER_IP1:=127.0.0.1}"
: "${ROCKETMQ_DISK_MAX_USED_SPACE_RATIO:=75}"
: "${ROCKETMQ_DISK_SPACE_WARNING_LEVEL_RATIO:=90}"
: "${ROCKETMQ_DISK_SPACE_CLEAN_FORCIBLY_RATIO:=85}"

sed -e "s/__BROKER_IP1__/${ROCKETMQ_BROKER_IP1}/g" \
    -e "s/__DISK_MAX_USED_SPACE_RATIO__/${ROCKETMQ_DISK_MAX_USED_SPACE_RATIO}/g" \
    -e "s/__DISK_SPACE_WARNING_LEVEL_RATIO__/${ROCKETMQ_DISK_SPACE_WARNING_LEVEL_RATIO}/g" \
    -e "s/__DISK_SPACE_CLEAN_FORCIBLY_RATIO__/${ROCKETMQ_DISK_SPACE_CLEAN_FORCIBLY_RATIO}/g" \
    /home/rocketmq/broker.conf.template > /home/rocketmq/broker.conf

exec sh mqbroker -n "${ROCKETMQ_NAME_SERVER}" -c /home/rocketmq/broker.conf
