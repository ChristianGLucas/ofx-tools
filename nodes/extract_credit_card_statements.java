package nodes;

import axiom.AxiomContext;
import gen.Messages.CreditCardStatementDetail;
import gen.Messages.ExtractCreditCardStatementsResult;
import gen.Messages.OfxDocument;
import gen.Messages.Statement;
import java.util.Map;

public class ExtractCreditCardStatements {

    /**
     * List every credit-card statement (CCSTMTRS) in a parsed OfxDocument
     * with its account, currency, statement period, transactions, and
     * ledger/available balance.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A document produced by ParseDocument.
     */
    public static ExtractCreditCardStatementsResult extractCreditCardStatements(AxiomContext ax, OfxDocument input) {
        ax.log().info("extractCreditCardStatements handling", Map.of());
        ExtractCreditCardStatementsResult.Builder result =
                ExtractCreditCardStatementsResult.newBuilder().setError(OfxSupport.okError());
        for (Statement st : input.getStatementsList()) {
            if (!"CREDITCARD".equals(st.getStatementKind())) {
                continue;
            }
            result.addStatements(CreditCardStatementDetail.newBuilder()
                    .setAccount(st.getAccount())
                    .setCurrency(st.getCurrency())
                    .setStartDateIso(st.getStartDateIso())
                    .setEndDateIso(st.getEndDateIso())
                    .addAllTransactions(st.getTransactionsList())
                    .setLedgerBalance(st.getLedgerBalance())
                    .setAvailableBalance(st.getAvailableBalance())
                    .build());
        }
        return result.build();
    }
}
