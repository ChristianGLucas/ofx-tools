package nodes;

import axiom.AxiomContext;
import gen.Messages.NormalizeDateInput;
import gen.Messages.NormalizeDateResult;
import java.util.Map;

public class NormalizeOfxDate {

    /**
     * Normalize a single raw OFX date/time string
     * (YYYYMMDD[HHMMSS[.XXX]] with an optional [gmt offset:tz name]
     * suffix) into ISO 8601 UTC, using ofx4j's own OFX date grammar. A
     * date-only input normalizes to a date-only ISO string; input carrying
     * a time normalizes to a full UTC ISO 8601 date-time. A malformed or
     * empty date returns a structured error rather than throwing.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The raw OFX date/time string.
     */
    public static NormalizeDateResult normalizeOfxDate(AxiomContext ax, NormalizeDateInput input) {
        ax.log().info("normalizeOfxDate handling", Map.of());
        return OfxSupport.normalizeDate(input.getOfxDate());
    }
}
