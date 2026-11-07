package fr.example.rvisionscours;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class QuizCsvParserTest {
    @Test
    public void parsesQuotedQuestionsAndEscapedQuotes() {
        QuizQuestion question = QuizCsvParser.INSTANCE.parse(
                "\"Qui a dit \"\"bonjour\"\" ?\",A,B,C,D,3"
        );

        assertEquals("Qui a dit \"bonjour\" ?", question.getQuestion());
        assertEquals(java.util.Arrays.asList("A", "B", "C", "D"), question.getOptions());
        assertEquals(2, question.getCorrectIndex());
    }

    @Test
    public void rejectsMalformedRows() {
        assertNull(QuizCsvParser.INSTANCE.parse("Question,A,B,C,D,0"));
        assertNull(QuizCsvParser.INSTANCE.parse("Question,A,B,C,D"));
    }

    @Test
    public void preservesUtf8Characters() {
        String utf8Line = new String(
                "Quelle est la capitale de la Grèce ?,Athènes,Rome,Paris,Lisbonne,1"
                        .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        QuizQuestion question = QuizCsvParser.INSTANCE.parse(utf8Line);

        assertEquals("Quelle est la capitale de la Grèce ?", question.getQuestion());
        assertEquals("Athènes", question.getOptions().get(0));
    }
}
