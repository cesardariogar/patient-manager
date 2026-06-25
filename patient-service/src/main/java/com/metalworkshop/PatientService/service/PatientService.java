package com.metalworkshop.PatientService.service;

import com.metalworkshop.PatientService.dto.PatientCreateRequestDto;
import com.metalworkshop.PatientService.dto.PatientResponseDto;
import com.metalworkshop.PatientService.dto.PatientUpdateRequestDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;
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
        return patientRepository.findByNameIgnoreCase(name)
                .map(PatientMapper::toDto);
    }

    public Optional<PatientResponseDto> findById(UUID id) {

        return patientRepository.findById(id)
                .map(PatientMapper::toDto);
    }

    public List<PatientResponseDto> findAll() {
        return patientRepository.findAll()
                .stream()
                .map(PatientMapper::toDto)
                .toList();
    }

    public Page<PatientResponseDto> findAllPageable(int page, int size, String name) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Patient> patientPage;
        if (nonNull(name) && !name.isBlank()) {
            patientPage = patientRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            patientPage = patientRepository.findAll(pageable);
        }

        return patientPage.map(PatientMapper::toDto);
    }

    public PatientResponseDto createPatient(PatientCreateRequestDto patientDto) {
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

    public PatientResponseDto updatePatient(UUID uuid, PatientUpdateRequestDto requestDto) {
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
