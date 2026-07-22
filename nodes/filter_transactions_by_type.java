package nodes;

import axiom.AxiomContext;
import gen.Messages.FilterByTypeInput;
import gen.Messages.Transaction;
import gen.Messages.TransactionList;
import java.util.Map;

public class FilterTransactionsByType {

    /**
     * Keep only the transactions whose type case-insensitively matches the
     * requested OFX TRNTYPE (e.g. "DEBIT", "CREDIT", "CHECK", "ATM",
     * "POS", "FEE", "INT", "DIV", "XFER", "PAYMENT"). An empty/blank type
     * filter matches nothing (not everything) — pass the input list
     * through unfiltered if no filtering is wanted.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The transactions to filter, plus the type to keep.
     */
    public static TransactionList filterTransactionsByType(AxiomContext ax, FilterByTypeInput input) {
        ax.log().info("filterTransactionsByType handling", Map.of());
        String wanted = input.getType() == null ? "" : input.getType().trim();
        TransactionList.Builder result = TransactionList.newBuilder();
        if (wanted.isEmpty()) {
            return result.build();
        }
        for (Transaction t : input.getTransactions().getTransactionsList()) {
            if (wanted.equalsIgnoreCase(t.getType())) {
                result.addTransactions(t);
            }
        }
        return result.build();
    }
}
