package nodes;

import axiom.AxiomContext;
import gen.Messages.ComputeTotalsResult;
import gen.Messages.Transaction;
import gen.Messages.TransactionList;
import java.util.Map;

public class ComputeTransactionTotals {

    /**
     * Pure aggregation over a TransactionList: the sum of credit (positive
     * amount) transactions, the sum of debit (negative amount) transactions
     * reported as a non-negative number, and the net (credits minus
     * debits) — the OFX-signed-amount convention.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A TransactionList, e.g. from ExtractTransactions.
     */
    public static ComputeTotalsResult computeTransactionTotals(AxiomContext ax, TransactionList input) {
        ax.log().info("computeTransactionTotals handling", Map.of());
        double credits = 0.0;
        double debits = 0.0;
        for (Transaction t : input.getTransactionsList()) {
            double amount = t.getAmount();
            if (amount > 0) {
                credits += amount;
            } else if (amount < 0) {
                debits += -amount;
            }
        }
        return ComputeTotalsResult.newBuilder()
                .setSumCredits(credits)
                .setSumDebits(debits)
                .setNet(credits - debits)
                .setTransactionCount(input.getTransactionsCount())
                .setError(OfxSupport.okError())
                .build();
    }
}
