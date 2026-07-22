package nodes;

import axiom.AxiomContext;
import gen.Messages.NormalizeDateInput;
import gen.Messages.NormalizeDateResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NormalizeOfxDateTest {

    @Test
    public void dateOnlyInputNormalizesToDateOnlyIso() {
        AxiomContext ax = TestSupport.testContext();
        NormalizeDateResult result = NormalizeOfxDate.normalizeOfxDate(ax, NormalizeDateInput.newBuilder().setOfxDate("20250714").build());
        assertTrue(result.getError().getOk());
        assertEquals("2025-07-14", result.getIsoDate());
    }

    @Test
    public void dateTimeInputNormalizesToFullUtcInstant() {
        AxiomContext ax = TestSupport.testContext();
        NormalizeDateResult result = NormalizeOfxDate.normalizeOfxDate(ax, NormalizeDateInput.newBuilder().setOfxDate("20250714120000").build());
        assertTrue(result.getError().getOk());
        assertEquals("2025-07-14T12:00:00Z", result.getIsoDate());
    }

    @Test
    public void timezoneOffsetSuffixIsResolvedIntoUtc() {
        // 12:00 in GMT-5 (EST) is 17:00 UTC — a hand-computed oracle from
        // the OFX date grammar's own [gmt offset:tz name] rule.
        AxiomContext ax = TestSupport.testContext();
        NormalizeDateResult result = NormalizeOfxDate.normalizeOfxDate(ax, NormalizeDateInput.newBuilder().setOfxDate("20250714120000[-5:EST]").build());
        assertTrue(result.getError().getOk());
        assertEquals("2025-07-14T17:00:00Z", result.getIsoDate());
    }

    @Test
    public void malformedDateReturnsStructuredError() {
        AxiomContext ax = TestSupport.testContext();
        NormalizeDateResult result = NormalizeOfxDate.normalizeOfxDate(ax, NormalizeDateInput.newBuilder().setOfxDate("not-a-date").build());
        assertFalse(result.getError().getOk());
        assertEquals("MALFORMED", result.getError().getCode());
    }

    @Test
    public void emptyDateReturnsStructuredError() {
        AxiomContext ax = TestSupport.testContext();
        NormalizeDateResult result = NormalizeOfxDate.normalizeOfxDate(ax, NormalizeDateInput.newBuilder().setOfxDate("").build());
        assertFalse(result.getError().getOk());
        assertEquals("EMPTY_INPUT", result.getError().getCode());
    }
}
