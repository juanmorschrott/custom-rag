package com.example.customrag.ingestion;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IngestionJobMapper {

    IngestionJobView toView(IngestionJob job);
}
