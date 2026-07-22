package nodes;

import axiom.AxiomContext;
import gen.Messages.ExtractStatementPeriodsResult;
import gen.Messages.OfxDocument;
import gen.Messages.Statement;
import gen.Messages.StatementPeriod;
import java.util.Map;

public class ExtractStatementPeriods {

    /**
     * List the declared statement period (start/end date, exactly as
     * reported by the statement itself — never inferred from the earliest/
     * latest transaction date) for every statement in a parsed OfxDocument.
     *
     * @param ax    The AxiomContext: logging, secrets, reflection, mutation.
     * @param input A document produced by ParseDocument.
     */
    public static ExtractStatementPeriodsResult extractStatementPeriods(AxiomContext ax, OfxDocument input) {
        ax.log().info("extractStatementPeriods handling", Map.of());
        ExtractStatementPeriodsResult.Builder result =
                ExtractStatementPeriodsResult.newBuilder().setError(OfxSupport.okError());
        for (Statement st : input.getStatementsList()) {
            result.addPeriods(StatementPeriod.newBuilder()
                    .setStatementKind(st.getStatementKind())
                    .setAccountId(st.getAccount().getAccountId())
                    .setStartDateIso(st.getStartDateIso())
                    .setEndDateIso(st.getEndDateIso())
                    .build());
        }
        return result.build();
    }
}
