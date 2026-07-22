package nodes;

import axiom.AxiomContext;
import gen.Messages.AccountCurrency;
import gen.Messages.ExtractCurrencyResult;
import gen.Messages.OfxDocument;
import gen.Messages.Statement;
import java.util.Map;

public class ExtractCurrency {

    /**
     * List the declared currency (CURDEF, ISO 4217, e.g. "USD") for every
     * statement in a parsed OfxDocument.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A document produced by ParseDocument.
     */
    public static ExtractCurrencyResult extractCurrency(AxiomContext ax, OfxDocument input) {
        ax.log().info("extractCurrency handling", Map.of());
        ExtractCurrencyResult.Builder result = ExtractCurrencyResult.newBuilder().setError(OfxSupport.okError());
        for (Statement st : input.getStatementsList()) {
            result.addCurrencies(AccountCurrency.newBuilder()
                    .setStatementKind(st.getStatementKind())
                    .setAccountId(st.getAccount().getAccountId())
                    .setCurrency(st.getCurrency())
                    .build());
        }
        return result.build();
    }
}
