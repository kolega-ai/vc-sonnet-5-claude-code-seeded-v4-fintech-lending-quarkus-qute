package com.loanportal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void escapesCommasQuotesAndNewlines() {
        assertEquals("plain", Csv.escape("plain"));
        assertEquals("\"has,comma\"", Csv.escape("has,comma"));
        assertEquals("\"has \"\"quote\"\"\"", Csv.escape("has \"quote\""));
        assertEquals("\"line1\nline2\"", Csv.escape("line1\nline2"));
    }

    @Test
    void rowJoinsWithCrlf() {
        assertEquals("a,b,3\r\n", Csv.row("a", "b", 3));
    }

    @Test
    void nullValuesBecomeEmptyStrings() {
        assertEquals(",b\r\n", Csv.row(null, "b"));
    }

    @Test
    void parseLineHandlesQuotedCommasAndEscapedQuotes() {
        List<String> fields = Csv.parseLine("\"Acme, Inc.\",\"He said \"\"hi\"\"\",plain");
        assertEquals(List.of("Acme, Inc.", "He said \"hi\"", "plain"), fields);
    }

    @Test
    void parseLineHandlesUnquotedFields() {
        assertEquals(List.of("a", "b", "c"), Csv.parseLine("a,b,c"));
    }
}
