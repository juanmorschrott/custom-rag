package com.example.customrag.catalog;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {

    BookSummary toSummary(Book book);

    List<BookSummary> toSummaries(List<Book> books);
}
