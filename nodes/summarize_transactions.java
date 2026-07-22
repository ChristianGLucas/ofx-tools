package nodes;

import axiom.AxiomContext;
import gen.Messages.SummarizeTransactionsResult;
import gen.Messages.Transaction;
import gen.Messages.TransactionList;
import gen.Messages.TypeCount;
import java.util.LinkedHashMap;
import java.util.Map;

public class SummarizeTransactions {

    /**
     * Summarize a TransactionList: total count, count broken down by
     * transaction type (in first-seen order), total money in (sum of
     * positive amounts), total money out (sum of the absolute value of
     * negative amounts), and net.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A TransactionList, e.g. from ExtractTransactions.
     */
    public static SummarizeTransactionsResult summarizeTransactions(AxiomContext ax, TransactionList input) {
        ax.log().info("summarizeTransactions handling", Map.of());
        LinkedHashMap<String, Integer> countByType = new LinkedHashMap<>();
        double totalIn = 0.0;
        double totalOut = 0.0;

        for (Transaction t : input.getTransactionsList()) {
            String type = t.getType() == null || t.getType().isEmpty() ? "UNKNOWN" : t.getType();
            countByType.merge(type, 1, Integer::sum);
            double amount = t.getAmount();
            if (amount > 0) {
                totalIn += amount;
            } else if (amount < 0) {
                totalOut += -amount;
            }
        }

        SummarizeTransactionsResult.Builder result = SummarizeTransactionsResult.newBuilder()
                .setCount(input.getTransactionsCount())
                .setTotalIn(totalIn)
                .setTotalOut(totalOut)
                .setNet(totalIn - totalOut)
                .setError(OfxSupport.okError());
        for (Map.Entry<String, Integer> e : countByType.entrySet()) {
            result.addCountByType(TypeCount.newBuilder().setType(e.getKey()).setCount(e.getValue()).build());
        }
        return result.build();
    }
}
