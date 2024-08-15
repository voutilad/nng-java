#!/bin/sh
rm key.key cert.crt
openssl req -x509 -newkey rsa:4096 -keyout key.key -out cert.crt \
  -sha256 -days 3650 -nodes \
  -subj "/C=US/ST=Vermont/L=Burlington/O=NA/OU=junk/CN=localhost"
chmod 0400 key.key cert.crt