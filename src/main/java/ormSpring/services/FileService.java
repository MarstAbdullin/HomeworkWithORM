package ormSpring.services;

import ormSpring.dto.InformationDto;

public interface FileService {

    void init();

    void convert();

    void convertPngByUser(Long userId);

    InformationDto getInformation(Long userId);
}