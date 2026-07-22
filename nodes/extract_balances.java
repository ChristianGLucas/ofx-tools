package nodes;

import axiom.AxiomContext;
import gen.Messages.AccountBalance;
import gen.Messages.ExtractBalancesResult;
import gen.Messages.OfxDocument;
import gen.Messages.Statement;
import java.util.Map;

public class ExtractBalances {

    /**
     * List the ledger balance and available balance (amount plus as-of
     * date) declared for every statement in a parsed OfxDocument. A balance
     * that the source statement did not report has present=false, distinct
     * from an explicitly reported zero balance.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A document produced by ParseDocument.
     */
    public static ExtractBalancesResult extractBalances(AxiomContext ax, OfxDocument input) {
        ax.log().info("extractBalances handling", Map.of());
        ExtractBalancesResult.Builder result = ExtractBalancesResult.newBuilder().setError(OfxSupport.okError());
        for (Statement st : input.getStatementsList()) {
            result.addBalances(AccountBalance.newBuilder()
                    .setStatementKind(st.getStatementKind())
                    .setAccountId(st.getAccount().getAccountId())
                    .setLedgerBalance(st.getLedgerBalance())
                    .setAvailableBalance(st.getAvailableBalance())
                    .build());
        }
        return result.build();
    }
}
