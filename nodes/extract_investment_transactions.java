package nodes;

import axiom.AxiomContext;
import gen.Messages.InvestmentTransactionList;
import gen.Messages.OfxDocument;
import gen.Messages.Statement;
import java.util.Map;

public class ExtractInvestmentTransactions {

    /**
     * Flatten every investment transaction (buy/sell/income/reinvest/
     * transfer/split/etc.) across all investment statements in a parsed
     * OfxDocument into a single structured list. Full monetary detail
     * (units/price/commission/fees/total/security) is populated for the
     * buy, sell, and income transaction families; other investment
     * transaction types report type/FITID/dates/memo only — see the
     * package README for why.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A document produced by ParseDocument.
     */
    public static InvestmentTransactionList extractInvestmentTransactions(AxiomContext ax, OfxDocument input) {
        ax.log().info("extractInvestmentTransactions handling", Map.of());
        InvestmentTransactionList.Builder result =
                InvestmentTransactionList.newBuilder().setError(OfxSupport.okError());
        for (Statement st : input.getStatementsList()) {
            result.addAllTransactions(st.getInvestmentTransactionsList());
        }
        return result.build();
    }
}
