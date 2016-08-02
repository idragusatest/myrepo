package eu.domibus;

import backend.ecodex.org._1_1.*;
import org.apache.commons.io.IOUtils;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;

import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.*;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.commons.codec.binary.*;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;

/**
 * Created by idragusa on 4/30/16.
 */
public class SendMessage {
    public static void main(String[] args) throws Exception {

        final CyclicBarrier gate = new CyclicBarrier(200);

        // first one is in main
        for(int i = 1 ; i < 200 ; i++) {
            sendOneMessage(gate, 0);
        }

        String filename = "src/main/resources/payloads/1.txt";
        System.out.println("main");
        gate.await();
        System.out.println("main passed");
        System.out.println("sending ... main");
        SendResponse response = sendMessage(filename);
        System.out.println("main" + " - messageId: " + response.getMessageID().get(0));
    }

    public static void sendOneMessage(final CyclicBarrier gate, final int delay) {
        Thread t1 = new Thread() {
            public void run() {
                try {
                    String filename = "src/main/resources/payloads/1.txt";
                    System.out.println("" + delay);
                    gate.await();
                    System.out.println("" + delay + " passed");
                    Thread.sleep(delay * 1000);
                    System.out.println("sending ... " + delay);
                    SendResponse response = sendMessage(filename);
                    System.out.println("" + delay + " - messageId: " + response.getMessageID().get(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t1.start();
    }


    public static SendResponse sendMessage(String filename) throws Exception {
        String wsdl = "http://localhost:8180/domibus/services/backend?wsdl";
        URL wsdlURL = new URL(wsdl);
        QName SERVICE_NAME = new QName("http://org.ecodex.backend/1_1/", "BackendService_1_1");
        Service service = Service.create(wsdlURL, SERVICE_NAME);
        BackendInterface client = service.getPort(BackendInterface.class);
        String payloadHref = "cid:message";

        SendRequest sendRequest = createSendRequest(payloadHref, filename);
        Messaging ebMSHeaderInfo = createMessage(payloadHref, null);

        return client.sendMessage(sendRequest, ebMSHeaderInfo);

    }

    protected static SendRequest createSendRequest(String payloadHref, String filename) {
        SendRequest sendRequest = new SendRequest();
        PayloadType payload = new PayloadType();
        payload.setPayloadId(payloadHref);
        payload.setContentType("text/xml");
        byte[] payloadBytes = null;
        try {
            payloadBytes = IOUtils.toByteArray(new FileInputStream(new File(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        payload.setValue(Base64.decodeBase64(payloadBytes));
        sendRequest.getPayload().add(payload);

        return sendRequest;
    }


    protected static Messaging createMessage(String payloadHref, String mimeType) {
        Messaging ebMSHeaderInfo = new Messaging();
        UserMessage userMessage = new UserMessage();
        CollaborationInfo collaborationInfo = new CollaborationInfo();
        collaborationInfo.setAction("http://domibus.eu/bris/action");
        org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service service = new org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service();
        service.setValue("http://domibus.eu/bris/service");
        service.setType("http://domibus.eu/bris/service/type");
        collaborationInfo.setService(service);
        userMessage.setCollaborationInfo(collaborationInfo);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getProperty().add(createProperty("originalSender", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1", "string"));
        messageProperties.getProperty().add(createProperty("finalRecipient", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4", "string"));
        userMessage.setMessageProperties(messageProperties);
        PartyInfo partyInfo = new PartyInfo();
        From from = new From();
        from.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");
        PartyId sender = new PartyId();
        sender.setValue("red");
        sender.setType("http://www.domibus.eu/bris/partyid/type");
        from.getPartyId().add(sender);
        partyInfo.setFrom(from);
        To to = new To();
        to.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder");
        PartyId receiver = new PartyId();
        receiver.setType("http://www.domibus.eu/bris/partyid/type");
        receiver.setValue("blue");
        to.getPartyId().add(receiver);
        partyInfo.setTo(to);
        userMessage.setPartyInfo(partyInfo);
        PayloadInfo payloadInfo = new PayloadInfo();
        PartInfo partInfo = new PartInfo();
        partInfo.setHref(payloadHref);
        payloadInfo.getPartInfo().add(partInfo);
        userMessage.setPayloadInfo(payloadInfo);
        ebMSHeaderInfo.setUserMessage(userMessage);
        return ebMSHeaderInfo;
    }

    protected static Property createProperty(String name, String value, String type) {
        Property aProperty = new Property();
        aProperty.setValue(value);
        aProperty.setName(name);
        aProperty.setType(type);
        return aProperty;
    }

}
