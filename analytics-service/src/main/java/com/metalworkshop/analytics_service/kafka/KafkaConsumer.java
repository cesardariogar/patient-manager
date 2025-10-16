package com.metalworkshop.analytics_service.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final String PATIENT = "patient";
    private final String GROUP_ID = "analytics-service";

    @KafkaListener(topics = PATIENT, groupId = GROUP_ID)
    public void consumeEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // Perform any business related analytics here
            logger.info("Received Patient event: [PatientId: {}, PatientName: {}, PatientEmail: {}",
                    patientEvent.getPatientId(), patientEvent.getName(), patientEvent.getEmail());

        } catch (InvalidProtocolBufferException ex) {
            logger.error("Error deserializing event: {}", ex.getMessage());
        }
    }
}
