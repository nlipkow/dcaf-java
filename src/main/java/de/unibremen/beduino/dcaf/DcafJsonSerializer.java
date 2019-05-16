package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Norman Lipkow
 */
class DcafJsonSerializer {

    protected static class AccessRequestSerializer extends JsonSerializer<AccessRequest> {

        @Override
        public void serialize(AccessRequest value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField(DcafEncodingType.SAM, value.getSam());
            serializeAuthorizeListToObjectArray(jgen, value.getSai());
            if (value.getTimestamp() != 0) {
                jgen.writeNumberField(DcafEncodingType.TS, value.getTimestamp());
            }
            jgen.writeEndObject();
        }
    }

    protected static class AccessRuleSerializer extends JsonSerializer<AccessRule> {

        @Override
        public void serialize(AccessRule value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("id", value.getId());
            jgen.writeStringField("camIdentifier", value.getCamIdentifier());
            jgen.writeFieldName("accessRules");
            jgen.writeStartArray();
            for (ServerAccessRule accessRule : value.getServerAccessRules()) {
                jgen.writeObject(accessRule);
            }
            jgen.writeEndArray();
            if (value.getExpirationTime() != 0) {
                jgen.writeNumberField("expiration", value.getExpirationTime());
            }
            jgen.writeEndObject();
        }

    }
    protected static class AuthorizationSerializer extends JsonSerializer<Authorization> {

        @Override
        public void serialize(Authorization value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {

            jgen.writeStartArray();
            jgen.writeString(value.getUri());
            jgen.writeNumber(value.getMethods());
            jgen.writeEndArray();
        }

    }
    protected static class CamInfoSerializer extends JsonSerializer<CamInfo> {

        @Override
        public void serialize(CamInfo value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("name", value.getName());
            jgen.writeStringField("id", value.getId());
            jgen.writeEndObject();
        }

    }
    protected static class FaceSerializer extends com.fasterxml.jackson.databind.JsonSerializer<Face> {

        @Override
        public void serialize(Face value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            serializeAuthorizeListToObjectArray(jgen, value.getSai());
            jgen.writeNumberField(DcafEncodingType.TS, value.getTimestamp());
            jgen.writeNumberField(DcafEncodingType.L, value.getLifetime());
            jgen.writeObjectField(DcafEncodingType.G, value.getMacMethod().getEncoding());
            if (value.getUpdateHashEncrypted() != null) {
                jgen.writeStringField(DcafEncodingType.UH, value.getUpdateHashEncrypted());
            }
            jgen.writeEndObject();
        }

    }
    protected static class ResourceSerializer extends JsonSerializer<Resource> {

        @Override
        public void serialize(Resource value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("url", value.getResourcePath());
            jgen.writeNumberField("methods", value.getMethods());
            jgen.writeEndObject();
        }

    }
    protected static class RevocationTicketSerializer extends JsonSerializer<RevocationTicket> {

        @Override
        public void serialize(RevocationTicket value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeObjectField("ticket", value.getTicket());
            jgen.writeNumberField("deliveryTime", value.getDeliveryTime());
            jgen.writeEndObject();
        }

    }
    protected static class ServerAccessRuleSerializer extends JsonSerializer<ServerAccessRule> {

        @Override
        public void serialize(ServerAccessRule value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeObjectField("serverInfo", value.getServerInfo());
            jgen.writeStringField("resource", value.getResource());
            jgen.writeNumberField("methods", value.getMethods());
            jgen.writeFieldName("updateAttributes");
            jgen.writeStartArray();
            if (value.getUpdateAttributes() != null) {
                for (UpdateAttribute updateAttribute : value.getUpdateAttributes()) {
                    jgen.writeObject(updateAttribute);
                }
            }
            jgen.writeEndArray();
            jgen.writeEndObject();
        }

    }

    protected static class UpdateAttributeSerializer extends JsonSerializer<UpdateAttribute> {

        @Override
        public void serialize(UpdateAttribute value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("type", value.getType());

            if (value.isGpio()) {
                jgen.writeNumberField("pin", value.getPin());
                jgen.writeNumberField("port", value.getPort());
            } else {
                jgen.writeStringField("method", value.getMethod());
                jgen.writeStringField("url", value.getUrl());
            }

            jgen.writeEndObject();
        }
    }
    protected static class ServerInfoSerializer extends JsonSerializer<ServerInfo> {

        @Override
        public void serialize(ServerInfo value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("host", value.getHost());
            jgen.writeStringField("preSharedKey", value.getPreSharedKey());
            jgen.writeNumberField("seqNumber", value.getSeqNumber());
            jgen.writeFieldName("resources");
            jgen.writeStartArray();
            for (Resource resource : value.getResources()) {
                jgen.writeObject(resource);
            }
            jgen.writeEndArray();
            jgen.writeEndObject();
        }

    }
    protected static class TicketGrantMessageSerializer extends JsonSerializer<TicketGrantMessage> {

        @Override
        public void serialize(TicketGrantMessage value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("id", value.getId());
            jgen.writeObjectField(DcafEncodingType.F, value.getFace());
            jgen.writeObjectField(DcafEncodingType.V, value.getVerifier().getVerifier());
            if (StringUtils.isNotEmpty(value.getCamIdentifier()) && StringUtils.isNotEmpty(value.getServerHost())) {
                jgen.writeStringField("cam", value.getCamIdentifier());
                jgen.writeStringField("server", value.getServerHost());
            }
            jgen.writeEndObject();
        }

    }
    protected static class TicketRequestMessageSerializer extends JsonSerializer<TicketRequestMessage> {

        @Override
        public void serialize(TicketRequestMessage value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {

            jgen.writeStartObject();
            jgen.writeStringField(DcafEncodingType.SAM, value.getSamUrl());
            serializeAuthorizeListToObjectArray(jgen, value.getSai());
            if (value.getTimestamp() > 0) {
                jgen.writeNumberField(DcafEncodingType.TS, value.getTimestamp());
            }
            jgen.writeEndObject();
        }

    }

    private static void serializeAuthorizeListToObjectArray(JsonGenerator jgen, List<Authorization> sai) throws IOException {
        jgen.writeFieldName(DcafEncodingType.SAI);
        jgen.writeStartArray();
        for (Authorization auth : sai) {
            jgen.writeObject(auth);
        }
        jgen.writeEndArray();
    }
}
