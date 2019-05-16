package hello;

import de.unibremen.beduino.dcaf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SpringBootApplication
@RestController
public class SpringBoot {

    private static Logger logger = LoggerFactory.getLogger(SpringBoot.class);

    private static RemoteServerAuthorizationManager sam;

    @GetMapping(value = "/cams")
    public ResponseEntity<?> getClientAuthorizationManagers() {
        List<CamInfo> sams = sam.getClientAuthorizationManagers();

        return ResponseEntity.ok().body(sams);
    }

    @GetMapping(value = "/authorize")
    public ResponseEntity<?> authorize() {

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/cams")
    public ResponseEntity<?> addCam(@RequestBody CamInfo cam) {
        if (cam != null) {
            if (sam.addCamInfo(cam)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.unprocessableEntity().body("Id already taken");
            }
        }

        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping(value = "/cams")
    public ResponseEntity<?> deleteClientAuthorizationManagers(@RequestParam("id") String id,
            @RequestParam(value = "revoke", required = false) boolean revoke) {
        sam.deleteCam(id, revoke);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/cams")
    public ResponseEntity<?> updateCam(@RequestBody CamInfo cam) {
        if (cam != null) {
            sam.updateCam(cam);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping(value = "/rules")
    public ResponseEntity<?> getRules() {
        List<AccessRule> rules = sam.getAccessRules();

        return ResponseEntity.ok(rules);
    }

    @PostMapping(value = "/rules")
    public ResponseEntity<?> addRule(@RequestBody AccessRule rule) {
        if (rule != null) {
            if (sam.addAccessRule(rule)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.unprocessableEntity().body("Id already taken");
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @PatchMapping(value = "/rules")
    public ResponseEntity<?>  updateRule(@RequestBody AccessRule rule,
                                         @RequestParam(value = "revoke", required = false) boolean revoke) {
        if (rule != null) {
            sam.updateAccessRule(rule, revoke);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping(value = "/server")
    public ResponseEntity<?>  addServer(@RequestBody ServerInfo serverInfo) {
        if (serverInfo != null) {
            if (sam.addServer(serverInfo)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.unprocessableEntity().body("Id already taken");
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping(value = "/server")
    public ResponseEntity<?> getServer() {
        List<ServerInfo> server = sam.getServer();
        return ResponseEntity.ok(server);
    }

    @DeleteMapping(value = "/server")
    public ResponseEntity<?>  deleteServer(@RequestParam("id") String host,
                                           @RequestParam(value = "revoke", required = false) boolean revoke) {
        sam.deleteServer(host, revoke);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/server")
    public ResponseEntity<?>  updateServer(@RequestBody ServerInfo serverInfo) {
        if (serverInfo != null) {
            sam.updateServer(serverInfo);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping(value = "/rules")
    public ResponseEntity<?>  deleteRule(@RequestParam("id") String id) {
        sam.deleteAccessRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/tickets")
    public ResponseEntity<?> getTickets() {
        List<TicketGrantMessage> tickets = sam.getTickets();
        return ResponseEntity.ok(tickets);
    }

    @DeleteMapping(value = "/tickets")
    public ResponseEntity<?>  revokeTicket(@RequestParam("id") String id) {
        sam.revokeTicket(id);
        return ResponseEntity.noContent().build();
    }

    public static void main(String[] args) {

        ClientAuthorizationManager cam = new ClientAuthorizationManager(8002);
        cam.addEndpoints(8003);
        cam.start();
        cam.addPsk("TEST_CLIENT", "secretPSK");

        sam = new RemoteServerAuthorizationManager();
        sam.start();

        logger.info("CAM and SAM were set up");

        Utils.initializeTestData();
        LocalCoapClient client = new LocalCoapClient();
        client.testAccessRequestToCam(8002);
        SpringApplication.run(SpringBoot.class, args);
    }

}
