from coapthon.client.helperclient import HelperClient
import cbor2
import logging

SAM_ENCODING = u"0"
SAI_ENCODING = u"1"
TS_ENCODING = u"5"
logging.basicConfig()

client = HelperClient(server=('127.0.0.1', 8003))
response = client.post('client-authorize', cbor2.dumps({
    SAM_ENCODING: u"127.0.0.1:5684/authorize",
    SAI_ENCODING: [[u"coaps://[2001:DB8::dcaf:1234]/update", 9],
                   [u"coaps://resource-server.com/secret/resource2", 5]]
    # ,TS_ENCODING: u"33452" # OPTIONAL
}), timeout=100)
print(response.pretty_print())
if response.payload is not None:
    print(cbor2.loads(response.payload))
client.stop()
exit()
