package nodes;

import axiom.AxiomContext;
import gen.Messages.Account;
import gen.Messages.ExtractAccountsResult;
import gen.Messages.OfxDocument;
import gen.Messages.Statement;
import java.util.Map;

public class ExtractAccounts {

    /**
     * List every account (bank id, account id, account type, branch/routing
     * info) declared across all statements in a parsed OfxDocument — one
     * entry per statement, in statement order (not deduplicated: a document
     * with two statement responses for the same account reports it twice,
     * matching what the file actually declared).
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A document produced by ParseDocument.
     */
    public static ExtractAccountsResult extractAccounts(AxiomContext ax, OfxDocument input) {
        ax.log().info("extractAccounts handling", Map.of());
        ExtractAccountsResult.Builder result = ExtractAccountsResult.newBuilder().setError(OfxSupport.okError());
        for (Statement st : input.getStatementsList()) {
            if (st.hasAccount()) {
                Account acct = st.getAccount();
                if (!acct.getAccountId().isEmpty() || !acct.getBankId().isEmpty()) {
                    result.addAccounts(acct);
                }
            }
        }
        return result.build();
    }
}
