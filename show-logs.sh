#!/bin/bash

sudo docker ps --no-trunc \
  --format 'table {{.Names}}\t{{.Label "com.docker.compose.service"}}\t{{.Status}}\t{{.Ports}}\t{{.Networks}}'

