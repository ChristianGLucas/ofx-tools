package nodes;

import axiom.AxiomContext;
import gen.Messages.DetectDuplicatesResult;
import gen.Messages.DuplicateGroup;
import gen.Messages.Transaction;
import gen.Messages.TransactionList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DetectDuplicateTransactions {

    /**
     * Find transactions that share the same FITID (the OFX unique
     * transaction identifier) — the standard signal that two overlapping
     * downloads of a date range contain the same transaction twice.
     * Transactions with a blank FITID are never reported as duplicates of
     * each other (a blank FITID means the source did not provide one, not
     * that they are the same transaction). duplicate_transaction_count
     * counts every transaction that belongs to some duplicate group (not
     * just the "extra" copies).
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A TransactionList, e.g. from ExtractTransactions.
     */
    public static DetectDuplicatesResult detectDuplicateTransactions(AxiomContext ax, TransactionList input) {
        ax.log().info("detectDuplicateTransactions handling", Map.of());
        LinkedHashMap<String, List<Integer>> byFitid = new LinkedHashMap<>();
        List<Transaction> txs = input.getTransactionsList();
        for (int i = 0; i < txs.size(); i++) {
            String fitid = txs.get(i).getFitid();
            if (fitid == null || fitid.isEmpty()) {
                continue;
            }
            byFitid.computeIfAbsent(fitid, k -> new ArrayList<>()).add(i);
        }

        DetectDuplicatesResult.Builder result = DetectDuplicatesResult.newBuilder().setError(OfxSupport.okError());
        int duplicateTxCount = 0;
        for (Map.Entry<String, List<Integer>> e : byFitid.entrySet()) {
            List<Integer> indices = e.getValue();
            if (indices.size() > 1) {
                result.addDuplicateGroups(DuplicateGroup.newBuilder()
                        .setFitid(e.getKey())
                        .setCount(indices.size())
                        .addAllIndices(indices)
                        .build());
                duplicateTxCount += indices.size();
            }
        }
        result.setDuplicateTransactionCount(duplicateTxCount);
        return result.build();
    }
}
