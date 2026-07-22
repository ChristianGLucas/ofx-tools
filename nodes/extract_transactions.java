package nodes;

import axiom.AxiomContext;
import gen.Messages.OfxDocument;
import gen.Messages.Statement;
import gen.Messages.TransactionList;
import java.util.Map;

public class ExtractTransactions {

    /**
     * Flatten every banking and credit-card transaction across all
     * statements in a parsed OfxDocument into a single structured
     * TransactionList (type, date posted, amount, name/payee, memo, check
     * number, FITID). Investment transactions are intentionally excluded —
     * use ExtractInvestmentTransactions for those.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A document produced by ParseDocument.
     */
    public static TransactionList extractTransactions(AxiomContext ax, OfxDocument input) {
        ax.log().info("extractTransactions handling", Map.of());
        TransactionList.Builder result = TransactionList.newBuilder();
        for (Statement st : input.getStatementsList()) {
            if (!"INVESTMENT".equals(st.getStatementKind())) {
                result.addAllTransactions(st.getTransactionsList());
            }
        }
        return result.build();
    }
}
