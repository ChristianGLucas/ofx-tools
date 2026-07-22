package nodes;

import axiom.AxiomContext;
import gen.Messages.FilterByAmountThresholdInput;
import gen.Messages.Transaction;
import gen.Messages.TransactionList;
import java.util.Map;

public class FilterTransactionsByAmountThreshold {

    /**
     * Keep only the transactions whose signed amount falls within an
     * inclusive [min, max] threshold. Set has_min/has_max false on the
     * input to leave that bound open (e.g. has_min=false, has_max=true,
     * max_amount=0 keeps every transaction with a non-positive amount).
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input The transactions to filter, plus the inclusive bounds.
     */
    public static TransactionList filterTransactionsByAmountThreshold(AxiomContext ax, FilterByAmountThresholdInput input) {
        ax.log().info("filterTransactionsByAmountThreshold handling", Map.of());
        TransactionList.Builder result = TransactionList.newBuilder();
        for (Transaction t : input.getTransactions().getTransactionsList()) {
            double amount = t.getAmount();
            if (input.getHasMin() && amount < input.getMinAmount()) {
                continue;
            }
            if (input.getHasMax() && amount > input.getMaxAmount()) {
                continue;
            }
            result.addTransactions(t);
        }
        return result.build();
    }
}
