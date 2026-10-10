package com.example.customrag.catalog;

import java.util.List;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookMapper {

    BookSummary toSummary(Book book);

    List<BookSummary> toSummaries(List<Book> books);
}
