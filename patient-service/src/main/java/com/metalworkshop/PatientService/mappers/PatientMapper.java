package com.metalworkshop.PatientService.mappers;

import com.metalworkshop.PatientService.dto.PatientCreateRequestDto;
import com.metalworkshop.PatientService.model.Patient;
import com.metalworkshop.PatientService.dto.PatientResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PatientMapper {
    private static final Logger logger = LoggerFactory.getLogger(PatientMapper.class);

    public static PatientResponseDto toDto(Patient patient) {
        PatientResponseDto patientResponseDto = new PatientResponseDto();
        try {
            patientResponseDto.setId(patient.getId());
            patientResponseDto.setName(patient.getName());
            patientResponseDto.setLastName(patient.getLastName());
            patientResponseDto.setEmail(patient.getEmail());
            patientResponseDto.setAddress(patient.getAddress());
            patientResponseDto.setDateOfBirth(patient.getDateOfBirth());
            patientResponseDto.setRegisteredDate(patient.getRegisterDate());
        } catch (Exception ex) {
            logger.info("Error, could not parse the Patient:\n{}", patient.toString());
            throw ex;
        }

        return patientResponseDto;
    }

    public static Patient toEntity(PatientCreateRequestDto patientCreateRequestDto) {
        Patient newPatient = new Patient();
        try {
            newPatient.setName(patientCreateRequestDto.getName());
            newPatient.setLastName(patientCreateRequestDto.getLastName());
            newPatient.setEmail(patientCreateRequestDto.getEmail());
            newPatient.setAddress(patientCreateRequestDto.getAddress());
            newPatient.setDateOfBirth(LocalDate.parse(patientCreateRequestDto.getDateOfBirth()));
            // id
            // registeredDate
        } catch (Exception ex) {
            logger.info("Error, could not parse the PatientRequestDto:\n{}", patientCreateRequestDto.toString());
            throw ex;
        }

        return newPatient;
    }
}
