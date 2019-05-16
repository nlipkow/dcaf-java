#!/bin/bash

KEY_STORE=keyStore.jks
KEY_STORE_PWD=endPass
TRUST_STORE=trustStore.jks
TRUST_STORE_PWD=rootPass

# android support
KEY_STORE_P12=keyStore.p12
TRUST_STORE_P12=trustStore.p12

VALIDITY=365

echo "creating root key and certificate..."
keytool -genkeypair -alias root -keyalg EC -dname 'C=DE,L=Bremen,O=University Bremen,OU=WADI,CN=cf-root' \
        -ext BC=ca:true -validity $VALIDITY -keypass $TRUST_STORE_PWD -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD

echo "creating CA key and certificate..."
keytool -genkeypair -alias ca -keyalg EC -dname 'C=DE,L=Bremen,O=University Bremen,OU=WADI,CN=cf-ca' \
        -ext BC=ca:true -validity $VALIDITY -keypass $TRUST_STORE_PWD -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD
keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -certreq -alias ca | \
  keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -alias root -gencert -validity $VALIDITY -ext BC=0 -rfc | \
  keytool -alias ca -importcert -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD

echo "creating server key and certificate..."
keytool -genkeypair -alias sam -keyalg EC -dname 'C=DE,L=Bremen,O=University Bremen,OU=WADI,CN=cf-server' \
        -validity $VALIDITY -keypass $KEY_STORE_PWD -keystore $KEY_STORE -storepass $KEY_STORE_PWD
keytool -keystore $KEY_STORE -storepass $KEY_STORE_PWD -certreq -alias sam | \
  keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -alias ca -gencert -ext KU=dig -validity $VALIDITY -rfc > sam.pem
keytool -alias sam -importcert -keystore $KEY_STORE -storepass $KEY_STORE_PWD -trustcacerts -file sam.pem

echo "creating client key and certificate..."
keytool -genkeypair -alias cam -keyalg EC -dname 'C=DE,L=Bremen,O=University Bremen,OU=WADI,CN=cf-client' \
        -validity $VALIDITY -keypass $KEY_STORE_PWD -keystore $KEY_STORE -storepass $KEY_STORE_PWD
keytool -keystore $KEY_STORE -storepass $KEY_STORE_PWD -certreq -alias cam | \
  keytool -keystore $TRUST_STORE -storepass $TRUST_STORE_PWD -alias ca -gencert -ext KU=dig -validity $VALIDITY -rfc > cam.pem
keytool -alias cam -importcert -keystore $KEY_STORE -storepass $KEY_STORE_PWD -trustcacerts -file cam.pem

#echo "exporting keys into PKCS#12 format to support android"
#keytool -v -importkeystore -srckeystore $KEY_STORE -srcstorepass $KEY_STORE_PWD \
#   -destkeystore $KEY_STORE_P12 -deststorepass $KEY_STORE_PWD -deststoretype PKCS12
#keytool -v -importkeystore -srckeystore $TRUST_STORE -srcstorepass $TRUST_STORE_PWD \
#   -destkeystore $TRUST_STORE_P12 -deststorepass $TRUST_STORE_PWD -deststoretype PKCS12