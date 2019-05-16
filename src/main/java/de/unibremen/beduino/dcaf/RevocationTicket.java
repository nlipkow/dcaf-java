package de.unibremen.beduino.dcaf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Norman Lipkow
 */
@JsonSerialize(using = DcafJsonSerializer.RevocationTicketSerializer.class)
class RevocationTicket {

    private TicketGrantMessage ticket;
    private long deliveryTime;

    RevocationTicket(TicketGrantMessage ticket) {
        this.ticket = ticket;
    }

    RevocationTicket(@JsonProperty("ticket") TicketGrantMessage ticket, @JsonProperty("deliveryTime") int deliveryTime) {
        this.ticket = ticket;
        this.deliveryTime = deliveryTime;
    }

    long getDeliveryTime() {
        return deliveryTime;
    }

    void setDeliveryTime(long deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    TicketGrantMessage getTicket() {
        return ticket;
    }
}
