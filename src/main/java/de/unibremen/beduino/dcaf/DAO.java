package de.unibremen.beduino.dcaf;

import java.util.List;

/**
 * @author Norman Lipkow
 */
public interface DAO {

    List<TicketGrantMessage> getTickets();

    TicketGrantMessage getTicket(String ticketId);

    boolean deleteTicket(String ticketId);

    boolean saveTicket(TicketGrantMessage ticket);

    boolean updateTicket(TicketGrantMessage ticket);

    List<AccessRule> getAccessRules();

    AccessRule getAccessRule(String ruleId);

    boolean deleteAccessRule(String accessRuleId);

    boolean saveAccessRule(AccessRule accessRule);

    boolean updateAccessRule(AccessRule accessRule);

    List<CamInfo> getClientAuthorizationManagers();

    CamInfo getClientAuthorizationManager(String camIdentifier);

    boolean deleteClientAuthorizationManager(String camIdentifier);

    boolean saveClientAuthorizationManager(CamInfo camInfo);

    boolean updateClientAuthorizationManager(CamInfo camInfo);

    List<RevocationTicket> getRevocationTickets();

    RevocationTicket getRevocationTicket(String revocationId);

    boolean deleteRevocationTicket(String revocationId);

    boolean saveRevocationTicket(RevocationTicket revocationTicket);

    boolean updateRevocationTicket(RevocationTicket revocationTicket);

    List<ServerInfo> getServerInformations();

    ServerInfo getServerInformation(String host);

    boolean deleteServerInformation(String host);

    boolean saveServerInformation(ServerInfo serverInfo);

    boolean updateServerInformation(ServerInfo serverInfo);
}
