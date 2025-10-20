package com.metalworkshop.PatientService.service;

import com.metalworkshop.PatientService.dto.PatientRequestDto;
import com.metalworkshop.PatientService.dto.PatientResponseDto;
import com.metalworkshop.PatientService.exceptions.EmailAlreadyExistsException;
import com.metalworkshop.PatientService.exceptions.PatientNotFoundException;
import com.metalworkshop.PatientService.grpc.BillingServiceGrpcClient;
import com.metalworkshop.PatientService.kafka.KafkaProducer;
import com.metalworkshop.PatientService.mappers.PatientMapper;
import com.metalworkshop.PatientService.model.Patient;
import com.metalworkshop.PatientService.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    @Autowired
    public PatientService(PatientRepository patientRepository,
                          PatientMapper patientMapper,
                          BillingServiceGrpcClient billingServiceGrpcClient,
                          KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public Optional<PatientResponseDto> findByName(String name) {
        Optional<Patient> patient = patientRepository.findByNameIgnoreCase(name);

        return patient.map(PatientMapper::toDto);
    }


    public List<PatientResponseDto> findAll() {
        List<Patient> patientList = patientRepository.findAll();

        return patientList.stream()
                .map(PatientMapper::toDto)
                .toList();
    }

    public PatientResponseDto createPatient(PatientRequestDto patientDto) {
        if (patientRepository.existsByEmail(patientDto.getEmail())) {
            throw new EmailAlreadyExistsException(patientDto.getEmail());
        }

        Patient newPatient = PatientMapper.toEntity(patientDto);
        newPatient.setRegisterDate(LocalDate.now());
        Patient patient = patientRepository.save(newPatient);

        // GRPC
        billingServiceGrpcClient.createBillingAccount(
                newPatient.getId().toString(),
                newPatient.getName(),
                newPatient.getEmail()
        );

        // Kafka
        kafkaProducer.sendEvent(newPatient);

        return PatientMapper.toDto(patient);
    }

    public PatientResponseDto updatePatient(UUID uuid, PatientRequestDto requestDto) {
        Patient patient = patientRepository.findById(uuid).orElseThrow(
                () -> new PatientNotFoundException("Patient not found with ID: " + id)
        );

        if (patientRepository.existsByEmailAndIdNot(requestDto.getEmail(), uuid)) {
            throw new EmailAlreadyExistsException("Email address already exists.");
        }

        patient.setName(requestDto.getName());
        patient.setLastName(requestDto.getLastName());
        patient.setAddress(requestDto.getAddress());
        patient.setEmail(requestDto.getEmail());
        patient.setDateOfBirth(LocalDate.parse(requestDto.getDateOfBirth()));
        Patient updatedPatient = patientRepository.save(patient);

        return PatientMapper.toDto(updatedPatient);
    }

    public void deleteByUUID(UUID uuid) {
        Patient patient = patientRepository.findById(uuid).orElseThrow(
                () -> new PatientNotFoundException("Patient not found with ID: " + id)
        );
        patientRepository.deleteById(uuid);
    }
}
