package nodes;

import axiom.AxiomContext;
import gen.Messages.FilterByDateRangeInput;
import gen.Messages.Transaction;
import gen.Messages.TransactionList;
import java.util.Map;

public class FilterTransactionsByDateRange {

    /**
     * Keep only the transactions whose date_posted_iso falls within an
     * inclusive [start_date_iso, end_date_iso] range. Either bound may be
     * left empty for an open-ended range. Bounds may be a bare date
     * ("yyyy-MM-dd") or a full ISO instant — a date-only end bound is
     * treated as inclusive through the end of that UTC day. A transaction
     * with no posted date, or an unparseable bound, is excluded from a
     * range-filtered result rather than causing a crash.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The transactions to filter, plus the ISO date-range bounds.
     */
    public static TransactionList filterTransactionsByDateRange(AxiomContext ax, FilterByDateRangeInput input) {
        ax.log().info("filterTransactionsByDateRange handling", Map.of());
        Long startMillis = OfxSupport.parseIsoBoundEpochMillis(input.getStartDateIso(), false);
        Long endMillis = OfxSupport.parseIsoBoundEpochMillis(input.getEndDateIso(), true);

        TransactionList.Builder result = TransactionList.newBuilder();
        for (Transaction t : input.getTransactions().getTransactionsList()) {
            String posted = t.getDatePostedIso();
            if (startMillis == null && endMillis == null) {
                result.addTransactions(t);
                continue;
            }
            if (posted == null || posted.isEmpty()) {
                continue;
            }
            Long postedMillis = OfxSupport.parseIsoBoundEpochMillis(posted, false);
            if (postedMillis == null) {
                continue;
            }
            if (startMillis != null && postedMillis < startMillis) {
                continue;
            }
            if (endMillis != null && postedMillis > endMillis) {
                continue;
            }
            result.addTransactions(t);
        }
        return result.build();
    }
}
